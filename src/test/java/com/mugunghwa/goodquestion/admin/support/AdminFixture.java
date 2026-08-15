package com.mugunghwa.goodquestion.admin.support;

import com.mugunghwa.goodquestion.admin.auth.AdminAccount;
import com.mugunghwa.goodquestion.admin.auth.AdminAccountRepository;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.AdminRole;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;

/**
 * 테스트용 관리자 계정을 만든다.
 *
 * <p>서비스가 조작마다 감사 로그를 남기고 그 로그의 admin_id가 admin_accounts를
 * 참조하므로, 아무 UUID나 담은 {@link AdminPrincipal}로는 쓰기 API를 부를 수 없다.
 * 실제로 존재하는 계정을 만들어 그 principal을 쓴다.
 */
@TestComponent
@RequiredArgsConstructor
public class AdminFixture {

    private final AdminAccountRepository accountRepository;

    public AdminPrincipal createAdmin() {
        return createAdmin(AdminRole.ADMIN);
    }

    public AdminPrincipal createAdmin(AdminRole role) {
        AdminAccount account = accountRepository.save(AdminAccount.builder()
                // 테스트끼리 이메일이 겹치면 유니크 제약에 걸린다. 트랜잭션 롤백이
                // 되더라도 같은 클래스 안에서 두 번 부르는 경우가 있어 매번 다르게 만든다.
                .email("fixture-%s@goodquestion.kr".formatted(System.nanoTime()))
                .passwordHash("$2y$10$fixture.hash.not.used.for.login.abcdefghijklmnopqrstuv")
                .name("테스트관리자")
                .role(role)
                .build());
        return account.toPrincipal();
    }
}
