package com.mugunghwa.goodquestion.admin.member;

import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.member.dto.MemberDtos.MemberDetail;
import com.mugunghwa.goodquestion.admin.support.IntegrationTest;
import com.mugunghwa.goodquestion.admin.support.TestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Transactional
class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired TestFixture fixture;
    @Autowired JdbcTemplate jdbcTemplate;

    private AdminPrincipal admin;
    private UUID parentId;

    @BeforeEach
    void setUp() {
        admin = fixture.createAdmin();
        parentId = fixture.createParent("김보호자");
    }

    /** 서비스 백엔드가 로그인 때 만드는 행. 관리자 쪽에는 생성 경로가 없어 직접 넣는다. */
    private void createLoginSession() {
        jdbcTemplate.update("""
                insert into refresh_tokens (id, parent_id, token_hash, expires_at)
                values (?, ?, ?, now() + interval '14 days')
                """, UUID.randomUUID(), parentId, "hash-%s".formatted(System.nanoTime()));
    }

    @Test
    @DisplayName("정지시키면 살아 있던 로그인 세션도 함께 끊긴다")
    void suspendRevokesLoginSessions() {
        createLoginSession();
        createLoginSession();

        MemberDetail detail = memberService.suspend(admin, parentId, "이용약관 위반 신고 접수");

        assertThat(detail.status()).isEqualTo(ParentStatus.SUSPENDED);
        assertThat(detail.suspendedReason()).isEqualTo("이용약관 위반 신고 접수");
        // 정지했는데 로그인된 기기가 그대로 쓸 수 있으면 정지가 아니다.
        assertThat(detail.loginSessions()).allMatch(session -> !session.active());
    }

    @Test
    @DisplayName("정지를 풀면 로그인 실패 잠금도 함께 풀린다")
    void restoreClearsLoginLock() {
        jdbcTemplate.update("update parents set locked_until = now() + interval '1 hour' where id = ?",
                parentId);
        memberService.suspend(admin, parentId, "확인 필요");

        MemberDetail restored = memberService.restore(admin, parentId);

        assertThat(restored.status()).isEqualTo(ParentStatus.ACTIVE);
        // 둘이 겹쳐 있으면 관리자는 풀었다고 생각하는데 사용자는 여전히 못 들어온다.
        assertThat(restored.locked()).isFalse();
    }

    @Test
    @DisplayName("로그인 세션만 끊는 것은 계정 상태를 바꾸지 않는다")
    void revokeSessionsKeepsAccountActive() {
        createLoginSession();

        memberService.revokeLoginSessions(admin, parentId);

        MemberDetail detail = memberService.get(parentId);
        assertThat(detail.status()).isEqualTo(ParentStatus.ACTIVE);
        assertThat(detail.loginSessions()).allMatch(session -> !session.active());
    }

    @Test
    @DisplayName("아이가 없는 보호자의 학습 세션 목록은 비어 있다")
    void sessionsEmptyWithoutChildren() {
        var page = memberService.listSessions(parentId, 0, 20);
        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    /** 값을 채우는 것은 서비스 백엔드다. 관리자 쪽에 쓰기 경로가 없어 직접 넣는다. */
    @Test
    @DisplayName("마지막 접속 시각이 목록과 상세에 함께 실린다")
    void lastLoginAtIsExposed() {
        jdbcTemplate.update("update parents set last_login_at = now() where id = ?", parentId);

        var summary = memberService.list(null, "김보호자", 0, 20).content().getFirst();
        MemberDetail detail = memberService.get(parentId);

        assertThat(summary.lastLoginAt()).isNotNull();
        assertThat(detail.lastLoginAt()).isNotNull();
    }

    /**
     * 접속한 적 없는 계정은 비어 있어야 한다. 가입 시각으로 대신 채우면
     * "한 번도 안 온 사람"이 목록에서 방금 다녀간 사람처럼 보인다.
     */
    @Test
    @DisplayName("접속 기록이 없으면 마지막 접속 시각은 비어 있다")
    void lastLoginAtIsNullWithoutLogin() {
        MemberDetail detail = memberService.get(parentId);

        assertThat(detail.lastLoginAt()).isNull();
        assertThat(detail.createdAt()).isNotNull();
    }
}
