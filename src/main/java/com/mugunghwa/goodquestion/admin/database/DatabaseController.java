package com.mugunghwa.goodquestion.admin.database;

import com.mugunghwa.goodquestion.admin.database.dto.DatabaseDtos.RowPage;
import com.mugunghwa.goodquestion.admin.database.dto.DatabaseDtos.SchemaGraph;
import com.mugunghwa.goodquestion.admin.database.dto.DatabaseDtos.TableDetail;
import com.mugunghwa.goodquestion.admin.database.dto.DatabaseDtos.TableSummary;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.CurrentAdmin;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * DB 구조와 값 조회.
 *
 * <p>백엔드를 보지 않는 팀원이 "이 컬럼은 무슨 뜻이고 실제로 어떤 값이 들어 있는가"를
 * 확인하는 화면이 쓴다.
 *
 * <p><b>읽기만 한다.</b> 쓰기 엔드포인트가 없고 임의 SQL 을 받는 입구도 없다.
 * 관리자 콘솔은 서비스와 같은 운영 DB 를 보므로 여기서 나간 문장 하나가 사용자
 * 서비스를 멈출 수 있다. 조회 조건은 테이블/컬럼 이름과 검색어까지로 제한한다.
 */
@RestController
@RequestMapping("/api/admin/database")
@RequiredArgsConstructor
public class DatabaseController {

    private final DatabaseService databaseService;

    /** 테이블 목록. 도메인 분류로 묶여 있고 행 수는 추정값이다. */
    @GetMapping("/tables")
    public List<TableSummary> listTables() {
        return databaseService.listTables();
    }

    /**
     * 관계도. 상자와 선을 한 번에 내린다.
     *
     * <p>배치는 화면이 정한다. 서버가 좌표까지 정하면 화면 크기나 접힌 분류에 따라
     * 다시 계산해야 할 때마다 왕복이 생긴다.
     */
    @GetMapping("/relations")
    public SchemaGraph getRelations() {
        return databaseService.getSchemaGraph();
    }

    /** 테이블 하나의 컬럼, 인덱스, 정확한 행 수. */
    @GetMapping("/tables/{tableName}")
    public TableDetail getTable(@PathVariable String tableName) {
        return databaseService.getTable(tableName);
    }

    /**
     * 실제 저장된 값.
     *
     * @param sortColumn    정렬 기준 컬럼. 비우면 created_at 최신순, 없으면 기본키순
     * @param filterColumn  검색할 컬럼. keyword 와 함께 보낸다
     * @param keyword       검색어. 컬럼을 text 로 바꿔 부분 일치로 찾는다
     */
    @GetMapping("/tables/{tableName}/rows")
    public RowPage getRows(@CurrentAdmin AdminPrincipal admin,
                           @PathVariable String tableName,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "50") int size,
                           @RequestParam(required = false) String sortColumn,
                           @RequestParam(required = false) String sortDirection,
                           @RequestParam(required = false) String filterColumn,
                           @RequestParam(required = false) String keyword) {
        return databaseService.getRows(admin, tableName, page, size,
                sortColumn, sortDirection, filterColumn, keyword);
    }
}
