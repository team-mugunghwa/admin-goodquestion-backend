package com.mugunghwa.goodquestion.admin.database;

import com.mugunghwa.goodquestion.admin.database.dto.DatabaseDtos.ColumnInfo;
import com.mugunghwa.goodquestion.admin.database.dto.DatabaseDtos.RowPage;
import com.mugunghwa.goodquestion.admin.database.dto.DatabaseDtos.TableDetail;
import com.mugunghwa.goodquestion.admin.database.dto.DatabaseDtos.TableSummary;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.support.IntegrationTest;
import com.mugunghwa.goodquestion.admin.support.TestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 데이터 탐색 기능.
 *
 * <p>{@code @Transactional} 을 붙이지 않았다. 이 서비스는 조회만 하고, 감사 로그가
 * 남는지 확인할 때 별도 트랜잭션 없이 그대로 읽는 편이 실제 동작에 가깝다.
 */
@IntegrationTest
class DatabaseServiceTest {

    @Autowired DatabaseService databaseService;
    @Autowired TestFixture fixture;
    @Autowired JdbcTemplate jdbcTemplate;

    private AdminPrincipal admin;

    @BeforeEach
    void setUp() {
        admin = fixture.createAdmin();
    }

    @Test
    @DisplayName("테이블 목록이 도메인 분류로 묶여 나온다")
    void listsTablesGroupedByDomain() {
        List<TableSummary> tables = databaseService.listTables();

        assertThat(tables).isNotEmpty();
        assertThat(tables).extracting(TableSummary::name).contains("parents", "stories", "notices");
        assertThat(tables).filteredOn(t -> t.name().equals("parents"))
                .allMatch(t -> t.group().equals("사용자"));
        // 분류가 붙지 않은 테이블도 목록에서 사라지지 않아야 한다.
        assertThat(tables).allMatch(t -> t.group() != null && !t.group().isBlank());
    }

    @Test
    @DisplayName("마이그레이션으로 심은 테이블/컬럼 설명이 그대로 읽힌다")
    void readsSchemaComments() {
        TableDetail detail = databaseService.getTable("children");

        assertThat(detail.comment()).contains("아이 프로필");
        assertThat(detail.columns()).extracting(ColumnInfo::name)
                .contains("id", "parent_id", "name", "birth_year", "created_at");
        // 이 기능의 존재 이유다. 설명이 비면 화면이 컬럼 이름만 나열하게 된다.
        assertThat(detail.columns()).allMatch(c -> c.comment() != null && !c.comment().isBlank());
    }

    @Test
    @DisplayName("기본키와 외래키, 타입이 함께 나온다")
    void describesKeysAndTypes() {
        TableDetail detail = databaseService.getTable("children");

        ColumnInfo id = column(detail, "id");
        assertThat(id.primaryKey()).isTrue();
        assertThat(id.type()).isEqualTo("uuid");

        ColumnInfo parentId = column(detail, "parent_id");
        assertThat(parentId.referencesTable()).isEqualTo("parents");
        assertThat(parentId.nullable()).isFalse();

        // 긴 타입 이름은 개발자가 쓰는 짧은 이름으로 바꿔서 내려야 표에서 읽힌다.
        assertThat(column(detail, "name").type()).startsWith("varchar");
        assertThat(column(detail, "created_at").type()).isEqualTo("timestamptz");
    }

    @Test
    @DisplayName("비밀번호 해시는 값이 가려진 채로 나온다")
    void masksSecretColumns() {
        RowPage page = databaseService.getRows(admin, "admin_accounts", 0, 10,
                null, null, null, null);

        assertThat(page.rows()).isNotEmpty();
        // 컬럼이 있다는 사실과 뜻은 보여주되 값만 가린다.
        assertThat(page.columns()).filteredOn(c -> c.name().equals("password_hash"))
                .allMatch(ColumnInfo::masked);
        assertThat(page.rows()).allSatisfy(row ->
                assertThat(row.get("password_hash")).isEqualTo(TableCatalog.MASK));
        // 가리지 않아도 되는 컬럼까지 가려 버리면 화면이 쓸모없어진다.
        assertThat(page.rows()).anySatisfy(row ->
                assertThat(row.get("email")).asString().contains("@"));
    }

    @Test
    @DisplayName("없는 테이블 이름은 찾을 수 없다고 답한다")
    void rejectsUnknownTable() {
        assertThatThrownBy(() -> databaseService.getTable("존재하지_않는_테이블"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("테이블 이름 자리에 SQL 을 넣어도 실행되지 않는다")
    void rejectsSqlInTableName() {
        // 이름은 바인딩 파라미터로 넘길 수 없어 SQL 문자열에 직접 들어간다.
        // information_schema 에 있는 이름만 통과시키는 것이 그 자리의 유일한 방어다.
        assertThatThrownBy(() -> databaseService.getRows(admin,
                "parents\"; drop table children; --", 0, 10, null, null, null, null))
                .isInstanceOf(BusinessException.class);

        // 방어가 뚫렸다면 이 테이블이 사라졌을 것이다.
        Boolean stillThere = jdbcTemplate.queryForObject("""
                select exists(select 1 from information_schema.tables
                              where table_schema = 'public' and table_name = 'children')
                """, Boolean.class);
        assertThat(stillThere).isTrue();
    }

    @Test
    @DisplayName("정렬과 검색 컬럼도 실제 있는 것만 받는다")
    void rejectsUnknownColumns() {
        assertThatThrownBy(() -> databaseService.getRows(admin, "parents", 0, 10,
                "1; drop table children", null, null, null))
                .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> databaseService.getRows(admin, "parents", 0, 10,
                null, null, "없는컬럼", "값"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("검색은 타입과 상관없이 부분 일치로 찾는다")
    void searchesAnyColumnAsText() {
        String name = "검색대상-%s".formatted(System.nanoTime());
        fixture.createParent(name);

        RowPage page = databaseService.getRows(admin, "parents", 0, 20,
                null, null, "name", name);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.rows().getFirst().get("name")).isEqualTo(name);
    }

    @Test
    @DisplayName("한 번에 가져오는 행 수에 상한이 있다")
    void capsPageSize() {
        RowPage page = databaseService.getRows(admin, "parents", 0, 100000,
                null, null, null, null);

        assertThat(page.size()).isEqualTo(200);
    }

    @Test
    @DisplayName("개인정보 테이블을 열면 감사 로그에 남는다")
    void logsPersonalDataAccess() {
        long before = countAuditLogs();

        databaseService.getRows(admin, "children", 0, 10, null, null, null, null);
        assertThat(countAuditLogs()).isEqualTo(before + 1);

        // 개인정보가 없는 테이블은 남기지 않는다. 전부 남기면 정작 봐야 할 기록이 묻힌다.
        databaseService.getRows(admin, "topics", 0, 10, null, null, null, null);
        assertThat(countAuditLogs()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("배열과 jsonb 값이 사람이 읽을 수 있는 모양으로 나온다")
    void rendersArrayAndJsonValues() {
        RowPage page = databaseService.getRows(admin, "story_scenes", 0, 5,
                null, null, null, null);

        if (page.rows().isEmpty()) {
            return; // 시드에 장면이 없는 환경에서는 확인할 것이 없다
        }
        Map<String, Object> row = page.rows().getFirst();
        // 드라이버 전용 객체가 그대로 나가면 화면에 클래스 이름이 찍힌다.
        assertThat(row.get("proper_nouns")).isNotInstanceOf(java.sql.Array.class);
        assertThat(String.valueOf(row.get("element_criteria"))).doesNotContain("PGobject");
    }

    private long countAuditLogs() {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from admin_audit_logs where action = 'READ_DATA'", Long.class);
        return count == null ? 0 : count;
    }

    private ColumnInfo column(TableDetail detail, String name) {
        return detail.columns().stream().filter(c -> c.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("컬럼이 없습니다: " + name));
    }
}
