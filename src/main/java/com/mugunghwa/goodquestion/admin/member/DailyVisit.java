package com.mugunghwa.goodquestion.admin.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * "이 보호자가 이 날 다녀갔다" 한 줄.
 *
 * <p>방문 이벤트를 그대로 쌓지 않고 (보호자, 날짜)를 기본키로 둔 이유는 대시보드가 묻는
 * 것이 "오늘 방문자 수"이기 때문이다. 이벤트를 쌓으면 집계할 때마다 distinct가 필요하고,
 * 하루에 스무 번 들어오는 사용자 때문에 행이 빠르게 는다. 이 형태면 행을 세는 것이 곧
 * 순 방문자 수다.
 *
 * <p>기록은 서비스 백엔드가 남긴다. 관리자는 읽기만 한다.
 */
@Entity
@Table(name = "daily_visits")
@IdClass(DailyVisit.Pk.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyVisit {

    @Id
    @Column(name = "parent_id")
    private UUID parentId;

    @Id
    @Column(name = "visit_date")
    private LocalDate visitDate;

    /** 그날 몇 번 들어왔는지. 방문자 수 집계에는 쓰이지 않고 이용 빈도를 볼 때 쓴다. */
    @Column(name = "visit_count", nullable = false)
    private int visitCount;

    @Column(name = "last_seen_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime lastSeenAt;

    @NoArgsConstructor
    public static class Pk implements Serializable {
        private UUID parentId;
        private LocalDate visitDate;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(parentId, pk.parentId) && Objects.equals(visitDate, pk.visitDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(parentId, visitDate);
        }
    }
}
