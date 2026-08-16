package com.mugunghwa.goodquestion.admin.database;

import com.mugunghwa.goodquestion.admin.database.dto.DatabaseDtos.ColumnInfo;
import com.mugunghwa.goodquestion.admin.database.dto.DatabaseDtos.IndexInfo;
import com.mugunghwa.goodquestion.admin.database.dto.DatabaseDtos.RowPage;
import com.mugunghwa.goodquestion.admin.database.dto.DatabaseDtos.TableDetail;
import com.mugunghwa.goodquestion.admin.database.dto.DatabaseDtos.TableSummary;
import com.mugunghwa.goodquestion.admin.global.audit.AuditAction;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogger;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DB 구조와 실제 값을 읽어 온다. 백엔드를 보지 않는 팀원이 스키마를 이해하는 데 쓴다.
 *
 * <h2>읽기 전용이다</h2>
 *
 * 이 클래스는 select 만 만든다. insert/update/delete 를 만드는 경로가 없고, 임의
 * SQL 을 받는 입구도 두지 않았다. 관리자 콘솔은 서비스와 <b>같은 운영 DB</b>를 보므로,
 * 여기서 잘못 나간 문장 하나가 사용자 서비스를 멈출 수 있다.
 *
 * <h2>SQL 조립 규칙</h2>
 *
 * 테이블과 컬럼 이름은 SQL 문자열에 직접 들어간다. 그 자리는 바인딩 파라미터로 바꿀
 * 수 없기 때문이다. 그래서 <b>넘어온 이름을 information_schema 에서 먼저 찾아보고,
 * 실제로 있는 것만</b> 큰따옴표로 감싸 쓴다. 값은 전부 바인딩 파라미터로 넘긴다.
 * 이 두 가지를 지키면 이름 자리로 문장을 끼워 넣을 수 없다.
 *
 * <h2>느린 질의를 막는 장치</h2>
 *
 * 자체 JdbcTemplate 에 질의 타임아웃을 걸었다. 공유 빈에 걸면 다른 기능까지 영향을
 * 받는다. 목록의 행 수는 통계 기반 추정값을 쓰고, 정확한 수는 테이블 하나를 열 때만 센다.
 */
@Slf4j
@Service
public class DatabaseService {

    /** 이 시간을 넘기는 질의는 끊는다. 운영 DB를 공유하므로 오래 도는 문장을 두지 않는다. */
    private static final int QUERY_TIMEOUT_SECONDS = 5;

    /** 한 번에 가져오는 최대 행 수. 화면이 감당하지 못할 양을 요청하지 못하게 막는다. */
    private static final int MAX_PAGE_SIZE = 200;

    private static final String TARGET_TYPE = "DATABASE";

    /** psql 이 보여주는 긴 타입 이름을 개발자가 쓰는 짧은 이름으로 바꾼다. */
    private static final Map<String, String> TYPE_ALIASES = Map.of(
            "character varying", "varchar",
            "timestamp with time zone", "timestamptz",
            "timestamp without time zone", "timestamp",
            "double precision", "float8",
            "character", "char");

    private final JdbcTemplate jdbcTemplate;
    private final AuditLogger auditLogger;

    public DatabaseService(DataSource dataSource, AuditLogger auditLogger) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
        this.auditLogger = auditLogger;
    }

    // ------------------------------------------------------------------ 목록

    @Transactional(readOnly = true)
    public List<TableSummary> listTables() {
        List<TableSummary> tables = jdbcTemplate.query("""
                select c.relname as name,
                       obj_description(c.oid) as comment,
                       (select count(*) from pg_attribute a
                        where a.attrelid = c.oid and a.attnum > 0 and not a.attisdropped)
                           as column_count,
                       case when c.reltuples < 0 then null else c.reltuples::bigint end
                           as estimated_rows
                from pg_class c
                join pg_namespace n on n.oid = c.relnamespace
                where n.nspname = 'public' and c.relkind = 'r'
                """, (rs, rowNum) -> {
            String name = rs.getString("name");
            Long estimated = rs.getObject("estimated_rows", Long.class);
            return new TableSummary(
                    name,
                    rs.getString("comment"),
                    TableCatalog.groupOf(name),
                    rs.getInt("column_count"),
                    estimated,
                    TableCatalog.hasPersonalData(name));
        });

        // 분류 순서로 묶고 그 안에서는 이름순. 화면이 정렬을 다시 하지 않아도 되게 한다.
        return tables.stream()
                .sorted(Comparator
                        .comparingInt((TableSummary t) -> TableCatalog.groupOrder(t.group()))
                        .thenComparing(TableSummary::name))
                .toList();
    }

    // ------------------------------------------------------------------ 상세

    @Transactional(readOnly = true)
    public TableDetail getTable(String tableName) {
        requireTable(tableName);

        String comment = jdbcTemplate.queryForObject("""
                select obj_description(c.oid)
                from pg_class c join pg_namespace n on n.oid = c.relnamespace
                where n.nspname = 'public' and c.relname = ?
                """, String.class, tableName);

        return new TableDetail(
                tableName,
                comment,
                TableCatalog.groupOf(tableName),
                countRows(tableName),
                TableCatalog.hasPersonalData(tableName),
                readColumns(tableName),
                readIndexes(tableName));
    }

    // ------------------------------------------------------------------ 행 조회

    /**
     * 테이블의 실제 값을 페이지 단위로 읽는다.
     *
     * <p>개인정보가 든 테이블을 열면 감사 로그에 남긴다. 다른 곳에서는 조회를 남기지
     * 않는다는 규칙을 여기서만 깨는데, 목록 화면을 여는 것과 아이 발화 원문을 훑는
     * 것은 무게가 다르기 때문이다. 나중에 누가 무엇을 봤는지 물을 수 있어야 한다.
     *
     * <p><b>이 메서드에는 {@code @Transactional(readOnly = true)} 를 붙이지 않는다.</b>
     * 붙이면 감사 로그가 조용히 사라진다. 읽기 전용 트랜잭션은 Hibernate 의 플러시를
     * 꺼 두기 때문에 로그 저장이 끝까지 나가지 않는데, 예외도 뜨지 않아 알아채기 어렵다.
     * 여기서는 트랜잭션을 열지 않고, 조회는 JdbcTemplate 이 각자 처리하고 로그 저장은
     * 리포지터리가 자기 트랜잭션을 연다. 조회 기록은 되돌릴 일이 없으므로 읽기와 한
     * 트랜잭션으로 묶을 이유도 없다.
     */
    public RowPage getRows(AdminPrincipal admin, String tableName, int page, int size,
                           String sortColumn, String sortDirection,
                           String filterColumn, String keyword) {
        requireTable(tableName);
        List<ColumnInfo> columns = readColumns(tableName);
        Set<String> columnNames = columns.stream().map(ColumnInfo::name)
                .collect(java.util.stream.Collectors.toSet());

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(filterColumn) && StringUtils.hasText(keyword)) {
            requireColumn(columnNames, tableName, filterColumn);
            // 어떤 타입이든 찾을 수 있게 text 로 바꿔서 비교한다. uuid 나 숫자 컬럼도
            // 그대로 검색되므로 화면에서 타입을 따질 필요가 없다.
            where.append(" where cast(").append(quote(filterColumn))
                    .append(" as text) ilike ?");
            params.add("%" + keyword.trim() + "%");
        }

        Long total = jdbcTemplate.queryForObject(
                "select count(*) from " + qualified(tableName) + where,
                Long.class, params.toArray());
        long totalElements = total == null ? 0 : total;

        String orderBy = buildOrderBy(columns, columnNames, tableName, sortColumn, sortDirection);

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(safeSize);
        pageParams.add((long) safePage * safeSize);

        List<Map<String, Object>> rows = jdbcTemplate.query(
                "select * from " + qualified(tableName) + where + orderBy + " limit ? offset ?",
                this::mapRow, pageParams.toArray());

        if (TableCatalog.hasPersonalData(tableName)) {
            auditLogger.log(admin, AuditAction.READ_DATA, TARGET_TYPE, tableName,
                    "개인정보 테이블 조회: %s (%d페이지)".formatted(tableName, safePage + 1));
        }

        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        return new RowPage(columns, rows, safePage, safeSize, totalElements, totalPages);
    }

    // ------------------------------------------------------------------ 내부

    /**
     * 정렬 기준을 정한다.
     *
     * <p>지정이 없으면 created_at 이 있는 테이블은 최신순으로 둔다. 값을 확인하러 오는
     * 사람이 가장 자주 찾는 것이 방금 들어온 행이기 때문이다. created_at 이 없으면
     * 기본키로 정렬한다 - 정렬을 아예 빼면 페이지를 넘길 때마다 순서가 달라져 같은
     * 행이 두 번 보이거나 아예 안 보이는 일이 생긴다.
     */
    private String buildOrderBy(List<ColumnInfo> columns, Set<String> columnNames,
                                String tableName, String sortColumn, String sortDirection) {
        String column = sortColumn;
        if (StringUtils.hasText(column)) {
            requireColumn(columnNames, tableName, column);
        } else if (columnNames.contains("created_at")) {
            column = "created_at";
        } else {
            column = columns.stream().filter(ColumnInfo::primaryKey).findFirst()
                    .map(ColumnInfo::name)
                    .orElse(columns.isEmpty() ? null : columns.getFirst().name());
        }
        if (column == null) {
            return "";
        }
        boolean ascending = "asc".equalsIgnoreCase(sortDirection)
                || (!StringUtils.hasText(sortDirection) && !"created_at".equals(column));
        return " order by " + quote(column) + (ascending ? " asc" : " desc");
    }

    private long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from " + qualified(tableName), Long.class);
        return count == null ? 0 : count;
    }

    private List<ColumnInfo> readColumns(String tableName) {
        Set<String> primaryKeys = Set.copyOf(jdbcTemplate.queryForList("""
                select a.attname
                from pg_index x
                join pg_class c on c.oid = x.indrelid
                join pg_namespace n on n.oid = c.relnamespace
                join pg_attribute a on a.attrelid = c.oid and a.attnum = any(x.indkey)
                where n.nspname = 'public' and c.relname = ? and x.indisprimary
                """, String.class, tableName));

        Map<String, String[]> foreignKeys = new HashMap<>();
        jdbcTemplate.query("""
                select kcu.column_name, ccu.table_name as ref_table, ccu.column_name as ref_column
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                     on kcu.constraint_name = tc.constraint_name
                    and kcu.table_schema = tc.table_schema
                join information_schema.constraint_column_usage ccu
                     on ccu.constraint_name = tc.constraint_name
                    and ccu.table_schema = tc.table_schema
                where tc.constraint_type = 'FOREIGN KEY'
                  and tc.table_schema = 'public' and tc.table_name = ?
                """, rs -> {
            foreignKeys.put(rs.getString("column_name"),
                    new String[]{rs.getString("ref_table"), rs.getString("ref_column")});
        }, tableName);

        return jdbcTemplate.query("""
                select a.attnum, a.attname,
                       format_type(a.atttypid, a.atttypmod) as type,
                       a.attnotnull,
                       pg_get_expr(d.adbin, d.adrelid) as default_value,
                       col_description(c.oid, a.attnum) as comment
                from pg_attribute a
                join pg_class c on c.oid = a.attrelid
                join pg_namespace n on n.oid = c.relnamespace
                left join pg_attrdef d on d.adrelid = c.oid and d.adnum = a.attnum
                where n.nspname = 'public' and c.relname = ?
                  and a.attnum > 0 and not a.attisdropped
                order by a.attnum
                """, (rs, rowNum) -> {
            String name = rs.getString("attname");
            String[] reference = foreignKeys.get(name);
            return new ColumnInfo(
                    rs.getInt("attnum"),
                    name,
                    shortenType(rs.getString("type")),
                    !rs.getBoolean("attnotnull"),
                    rs.getString("default_value"),
                    rs.getString("comment"),
                    primaryKeys.contains(name),
                    TableCatalog.isMasked(name),
                    reference == null ? null : reference[0],
                    reference == null ? null : reference[1]);
        }, tableName);
    }

    private List<IndexInfo> readIndexes(String tableName) {
        return jdbcTemplate.query("""
                select i.relname as name,
                       pg_get_indexdef(x.indexrelid) as definition,
                       x.indisunique, x.indisprimary
                from pg_index x
                join pg_class c on c.oid = x.indrelid
                join pg_class i on i.oid = x.indexrelid
                join pg_namespace n on n.oid = c.relnamespace
                where n.nspname = 'public' and c.relname = ?
                order by x.indisprimary desc, i.relname
                """, (rs, rowNum) -> new IndexInfo(
                rs.getString("name"),
                rs.getString("definition"),
                rs.getBoolean("indisunique"),
                rs.getBoolean("indisprimary")), tableName);
    }

    /** 컬럼 순서를 유지해야 화면의 열 순서가 스키마와 같아진다. */
    private Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String name = meta.getColumnLabel(i);
            Object value = rs.getObject(i);
            row.put(name, TableCatalog.isMasked(name) && value != null
                    ? TableCatalog.MASK
                    : renderValue(value));
        }
        return row;
    }

    /**
     * 값을 화면이 그대로 쓸 수 있는 모양으로 바꾼다.
     *
     * <p>배열과 jsonb 는 JDBC 가 드라이버 전용 객체로 돌려줘서 그대로 직렬화하면
     * 화면에 클래스 이름이 찍힌다. 시각은 드라이버 기본 표현을 쓰면 숫자로 나가
     * 사람이 읽을 수 없으므로 문자열로 바꾼다.
     */
    private Object renderValue(Object value) throws SQLException {
        return switch (value) {
            case null -> null;
            case java.sql.Array array -> {
                Object elements = array.getArray();
                yield elements instanceof Object[] items ? List.of(items) : String.valueOf(elements);
            }
            case Timestamp timestamp -> timestamp.toInstant().toString();
            case java.sql.Date date -> date.toLocalDate().toString();
            case Temporal temporal -> temporal.toString();
            case byte[] bytes -> "(바이너리 %d바이트)".formatted(bytes.length);
            case String text -> text;
            case Number number -> number;
            case Boolean bool -> bool;
            // jsonb 등 드라이버 전용 객체. 문자열 표현이 곧 저장된 값이다.
            default -> value.toString();
        };
    }

    private String shortenType(String type) {
        for (Map.Entry<String, String> alias : TYPE_ALIASES.entrySet()) {
            if (type.startsWith(alias.getKey())) {
                return alias.getValue() + type.substring(alias.getKey().length());
            }
        }
        return type;
    }

    /**
     * 넘어온 이름이 실제로 있는 테이블인지 확인한다.
     *
     * <p>이 검사가 SQL 조립의 안전장치다. 이름은 바인딩 파라미터로 넘길 수 없어
     * 문자열에 직접 들어가는데, information_schema 에 있는 값만 통과시키면
     * 이름 자리로 다른 문장을 끼워 넣을 수 없다.
     */
    private void requireTable(String tableName) {
        Boolean exists = jdbcTemplate.queryForObject("""
                select exists(select 1 from information_schema.tables
                              where table_schema = 'public' and table_name = ?)
                """, Boolean.class, tableName);
        if (!Boolean.TRUE.equals(exists)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "테이블을 찾을 수 없습니다: " + tableName);
        }
    }

    private void requireColumn(Set<String> columnNames, String tableName, String columnName) {
        if (!columnNames.contains(columnName)) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    "컬럼을 찾을 수 없습니다: %s.%s".formatted(tableName, columnName));
        }
    }

    private String qualified(String tableName) {
        return "public." + quote(tableName);
    }

    /** 큰따옴표를 두 번 쓰는 것이 PostgreSQL 의 이스케이프다. 검증을 통과한 이름이라도 그대로 지킨다. */
    private String quote(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }
}
