package com.mugunghwa.goodquestion.admin.global.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import java.util.List;

/**
 * 빈을 만들기 전에 환경변수를 확인하고, 빠진 것이 있으면 기동을 세운다.
 *
 * <h2>왜 리스너인가</h2>
 *
 * 값이 없으면 빈 생성 중에 죽는데, 그때 나오는 문장은 원인을 가린다
 * ({@link RequiredEnvironment} 참고). 환경이 준비된 직후 - 빈이 만들어지기 전에
 * 끼어들어야 원인만 깔끔하게 보여 줄 수 있다.
 *
 * <h2>왜 spring.factories 에 등록하지 않나</h2>
 *
 * {@code main()} 에서 직접 붙인다. 그러면 <b>실제로 앱을 띄울 때만</b> 돈다.
 * 테스트는 Testcontainers 가 접속 정보를 넣어 주므로 {@code DB_URL} 환경변수가
 * 없는데, 자동 등록하면 이 검사가 모든 테스트를 막는다.
 */
public class RequiredEnvironmentListener
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        List<String> problems = RequiredEnvironment.check(
                name -> rawProperty(event.getEnvironment(), name));
        if (problems.isEmpty()) {
            return;
        }

        // 로거는 아직 준비되기 전이라 표준 오류로 직접 쓴다. Railway 의 Deploy Logs 는
        // 표준 출력과 표준 오류를 함께 보여 준다.
        System.err.print(RequiredEnvironment.describe(problems));

        // 예외를 던지지 않고 여기서 끝낸다. 예외로 두면 스무 줄짜리 스택트레이스가
        // 뒤에 붙어서, 배포 로그를 여는 사람이 정작 위 안내를 지나쳐 버린다.
        // 아직 아무것도 초기화되기 전이라 정리할 자원도 없다.
        System.exit(1);
    }

    /**
     * 값을 <b>치환하지 않고</b> 그대로 읽는다.
     *
     * <p>{@code Environment.getProperty} 를 쓰면 안 된다. Railway 표기
     * {@code ${{ Postgres.PGHOST }}} 가 스프링 눈에는 자기 자리표시자로 보여서,
     * 값을 돌려주는 대신 그 자리에서 예외를 던진다. 그러면 우리가 준비한 안내는
     * 나오지도 못하고 원래의 알아보기 힘든 오류가 다시 나온다. 여기서 확인하려는
     * 것은 사람이 넣은 원래 값이므로 원본을 그대로 꺼낸다.
     */
    private static String rawProperty(ConfigurableEnvironment environment, String name) {
        for (PropertySource<?> source : environment.getPropertySources()) {
            Object value = source.getProperty(name);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }
}
