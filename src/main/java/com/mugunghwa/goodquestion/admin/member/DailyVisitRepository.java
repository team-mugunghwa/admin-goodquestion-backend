package com.mugunghwa.goodquestion.admin.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyVisitRepository extends JpaRepository<DailyVisit, DailyVisit.Pk> {

    /**
     * 그날 다녀간 순 방문자 수.
     *
     * <p>(보호자, 날짜)가 기본키라 행 수를 세는 것이 곧 순 방문자 수다. 방문 이벤트를
     * 그대로 쌓았다면 집계할 때마다 distinct가 필요했을 것이다.
     */
    @Query("select count(v) from DailyVisit v where v.visitDate = :date")
    long countVisitors(@Param("date") LocalDate date);

    /** 방문자 추이 그래프용. 방문이 없던 날은 행 자체가 없으므로 호출부가 0으로 채운다. */
    @Query("""
            select v.visitDate as date, count(v) as visitors
            from DailyVisit v
            where v.visitDate >= :from
            group by v.visitDate
            order by v.visitDate asc
            """)
    List<DailyVisitCount> countDailyFrom(@Param("from") LocalDate from);

    /** 스프링 데이터 프로젝션. */
    interface DailyVisitCount {
        LocalDate getDate();

        long getVisitors();
    }
}
