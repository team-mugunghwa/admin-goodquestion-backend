package com.mugunghwa.goodquestion.admin.notification.push;

import com.google.auth.oauth2.GoogleCredentials;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * 푸시 발송기를 고른다.
 *
 * <p>{@code push.fcm.credentials}가 비어 있으면 로그만 남기는 구현이 뜬다. 로컬과 CI에
 * Firebase 키를 두지 않으면서도 앱이 뜨고 테스트가 돌게 하려는 것이다. 자격증명이
 * 잘못됐을 때는 뜨지 않게 한다 - 키를 넣었는데 조용히 안 나가는 상태가 제일 나쁘다.
 */
@Slf4j
@Configuration
public class PushConfig {

    /** FCM HTTP v1 호출에 필요한 권한. */
    private static final String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    @Bean
    public PushSender pushSender(
            @Value("${push.fcm.credentials:}") String credentialsProperty,
            @Value("${push.fcm.project-id:}") String configuredProjectId,
            @Value("${push.fcm.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${push.fcm.response-timeout-ms:5000}") long responseTimeoutMs) {

        if (!StringUtils.hasText(credentialsProperty)) {
            return new LoggingPushSender();
        }

        try (InputStream stream = openCredentials(credentialsProperty.trim())) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(List.of(MESSAGING_SCOPE));
            String projectId = StringUtils.hasText(configuredProjectId)
                    ? configuredProjectId
                    : resolveProjectId(credentials);

            log.info("FCM 푸시 발송기를 사용합니다. projectId={}", projectId);
            return new FcmPushSender(fcmWebClient(connectTimeoutMs, responseTimeoutMs),
                    credentials, projectId);
        } catch (IOException e) {
            // 자격증명을 넣었는데 못 읽는 상태로 기동시키지 않는다. 그대로 뜨면
            // 답변은 등록되는데 푸시만 조용히 안 나가고, 그 사실을 알아채기 어렵다.
            throw new IllegalStateException(
                    "FCM 자격증명을 읽지 못했습니다. 파일 경로나 JSON 원문을 확인하세요: " + e.getMessage(), e);
        }
    }

    /** 값이 파일 경로면 그 파일을, 아니면 JSON 원문으로 본다. PaaS는 시크릿을 환경변수로 주는 곳이 많다. */
    private InputStream openCredentials(String value) throws IOException {
        Path path = Path.of(value);
        if (Files.isRegularFile(path)) {
            return Files.newInputStream(path);
        }
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private String resolveProjectId(GoogleCredentials credentials) {
        if (credentials instanceof com.google.auth.oauth2.ServiceAccountCredentials serviceAccount) {
            return serviceAccount.getProjectId();
        }
        throw new IllegalStateException(
                "FCM_PROJECT_ID가 필요합니다. 서비스 계정 키가 아니면 project_id를 자동으로 알 수 없습니다.");
    }

    /**
     * FCM 전용 WebClient.
     *
     * <p>타임아웃이 없으면 벤더가 응답하지 않는 동안 호출 스레드가 무한정 매달린다.
     * 발송은 답변 등록이 끝난 뒤 비동기로 도는데, 그렇더라도 스레드를 쥔 채 쌓이면
     * 결국 다른 작업까지 밀린다.
     */
    private WebClient fcmWebClient(int connectTimeoutMs, long responseTimeoutMs) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
