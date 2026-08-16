package com.mugunghwa.goodquestion.admin.database.dto;

import java.util.List;
import java.util.Map;

public final class DatabaseDtos {

    private DatabaseDtos() {
    }

    /**
     * 목록 한 줄.
     *
     * @param estimatedRows 대략적인 행 수. PostgreSQL 이 통계로 들고 있는 값이라
     *                      정확하지 않다. 39개 테이블에 각각 count(*) 를 돌리면
     *                      큰 테이블에서 목록이 눈에 띄게 느려지므로 여기서는
     *                      추정값을 쓰고, 테이블을 열 때 정확한 값을 센다.
     *                      한 번도 통계를 낸 적 없는 테이블은 null 이다.
     */
    public record TableSummary(
            String name,
            String comment,
            String group,
            int columnCount,
            Long estimatedRows,
            boolean containsPersonalData
    ) {
    }

    /**
     * 컬럼 하나.
     *
     * @param masked      값을 가려서 내보내는 컬럼인지. 비밀번호와 토큰이 해당한다
     * @param referencesTable  외래키가 가리키는 테이블. 없으면 null
     */
    public record ColumnInfo(
            int position,
            String name,
            String type,
            boolean nullable,
            String defaultValue,
            String comment,
            boolean primaryKey,
            boolean masked,
            String referencesTable,
            String referencesColumn
    ) {
    }

    public record IndexInfo(String name, String definition, boolean unique, boolean primaryKey) {
    }

    public record TableDetail(
            String name,
            String comment,
            String group,
            long rowCount,
            boolean containsPersonalData,
            List<ColumnInfo> columns,
            List<IndexInfo> indexes
    ) {
    }

    /**
     * 행 조회 결과.
     *
     * @param columns 화면이 열 순서와 가림 여부를 알아야 해서 함께 내린다.
     *                행 데이터만 주면 값이 없는 컬럼은 화면에서 사라진다.
     * @param rows    컬럼 이름을 키로 하는 값 묶음. 가려진 컬럼은 마스킹 문자열이 들어 있다
     */
    public record RowPage(
            List<ColumnInfo> columns,
            List<Map<String, Object>> rows,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
