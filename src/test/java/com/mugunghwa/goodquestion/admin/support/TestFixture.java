package com.mugunghwa.goodquestion.admin.support;

import com.mugunghwa.goodquestion.admin.auth.AdminAccount;
import com.mugunghwa.goodquestion.admin.auth.AdminAccountRepository;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.AdminRole;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

/**
 * 테스트 데이터를 만든다.
 *
 * <p>관리자 계정은 엔티티로 만들지만 보호자, 문의, 기기 토큰은 SQL로 넣는다.
 * 그것들을 만드는 것은 서비스 백엔드이고 관리자 콘솔에는 생성 경로 자체가 없다 -
 * 테스트를 위해 엔티티에 public 생성자를 열면 그 사실이 흐려진다.
 */
@TestComponent
@RequiredArgsConstructor
public class TestFixture {

    private final AdminAccountRepository accountRepository;
    private final JdbcTemplate jdbcTemplate;

    public AdminPrincipal createAdmin() {
        return createAdmin(AdminRole.ADMIN);
    }

    public AdminPrincipal createAdmin(AdminRole role) {
        AdminAccount account = accountRepository.save(AdminAccount.builder()
                // 테스트끼리 이메일이 겹치면 유니크 제약에 걸린다. 트랜잭션이 롤백되더라도
                // 같은 클래스 안에서 두 번 부르는 경우가 있어 매번 다르게 만든다.
                .email("fixture-%s@goodquestion.kr".formatted(System.nanoTime()))
                .passwordHash("$2y$10$fixture.hash.not.used.for.login.abcdefghijklmnopqrstuv")
                .name("테스트관리자")
                .role(role)
                .build());
        return account.toPrincipal();
    }

    public UUID createParent(String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into parents (id, email, password_hash, provider, name)
                values (?, ?, ?, 'LOCAL', ?)
                """, id, "parent-%s@example.com".formatted(System.nanoTime()), "hash", name);
        return id;
    }

    public UUID createInquiry(UUID parentId, String title) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into inquiries (id, parent_id, category, title, content, status)
                values (?, ?, 'ETC', ?, '문의 본문', 'PENDING')
                """, id, parentId, title);
        return id;
    }

    public UUID createDeviceToken(UUID parentId, String token) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into device_tokens (id, parent_id, token, platform)
                values (?, ?, ?, 'ANDROID')
                """, id, parentId, token);
        return id;
    }
}
