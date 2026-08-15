package com.mugunghwa.goodquestion.admin.notification.push;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * FCM HTTP v1로 보내는 발송기.
 *
 * <p>firebase-admin SDK 대신 REST를 직접 부른다. SDK는 gRPC와 Guava까지 끌고 오는데
 * 우리가 쓰는 기능은 "토큰 하나에 알림 한 건"뿐이고, 인증 라이브러리만 있으면 그건
 * POST 한 번이다. 대신 액세스 토큰 발급은 직접 만들지 않고
 * {@code google-auth-library}에 맡긴다 - 서비스 계정 키로 JWT를 서명해 교환하는
 * 과정은 직접 구현할 값어치가 없다.
 *
 * <p>액세스 토큰은 라이브러리가 캐시하고 만료 전에 알아서 갱신한다
 * ({@code getRequestMetadata}). 우리가 유효기간을 따로 관리하지 않는다.
 */
@Slf4j
public class FcmPushSender implements PushSender {

    private static final String SEND_URL = "https://fcm.googleapis.com/v1/projects/%s/messages:send";

    private final WebClient webClient;
    private final GoogleCredentials credentials;
    private final String projectId;

    public FcmPushSender(WebClient webClient, GoogleCredentials credentials, String projectId) {
        this.webClient = webClient;
        this.credentials = credentials;
        this.projectId = projectId;
    }

    @Override
    public PushResult send(String deviceToken, PushMessage message) {
        try {
            webClient.post()
                    .uri(SEND_URL.formatted(projectId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("message", buildMessage(deviceToken, message)))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return PushResult.SENT;
        } catch (WebClientResponseException e) {
            return handleVendorError(deviceToken, e);
        } catch (Exception e) {
            // 연결 실패, 타임아웃, 토큰 발급 실패. 토큰 자체는 멀쩡하므로 손대지 않는다.
            log.warn("FCM 발송 실패: {}", e.getMessage());
            return PushResult.FAILED;
        }
    }

    /**
     * 응답 본문은 로그에만 남긴다. 프로젝트 id와 서비스 계정 힌트가 실려 오는데
     * 그것이 관리자 화면까지 나갈 이유가 없다.
     */
    private PushResult handleVendorError(String deviceToken, WebClientResponseException e) {
        int status = e.getStatusCode().value();
        // 404 NOT_FOUND(UNREGISTERED)는 앱 삭제·재설치로 토큰이 죽은 경우다.
        // 400 INVALID_ARGUMENT는 토큰 형식 자체가 틀린 경우로, 다시 보내도 결과가 같다.
        if (status == 404 || status == 400) {
            log.info("FCM이 토큰을 거절해 비활성 처리한다. status={} token={}...",
                    status, deviceToken.length() > 12 ? deviceToken.substring(0, 12) : deviceToken);
            return PushResult.TOKEN_INVALID;
        }
        log.warn("FCM 응답 오류 status={} body={}", status, e.getResponseBodyAsString());
        return PushResult.FAILED;
    }

    private String accessToken() throws IOException {
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }

    private Map<String, Object> buildMessage(String deviceToken, PushMessage message) {
        Map<String, String> data = new HashMap<>(message.data());
        if (message.linkPath() != null) {
            // 앱은 이 값을 보고 알림을 눌렀을 때 어느 화면으로 갈지 정한다.
            data.put("linkPath", message.linkPath());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("token", deviceToken);
        payload.put("notification", Map.of("title", message.title(), "body", message.body()));
        if (!data.isEmpty()) {
            payload.put("data", data);
        }
        return payload;
    }
}
