package com.mugunghwa.goodquestion.admin.database;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 테이블을 사람이 찾기 좋게 분류하고, 다루기 조심스러운 것에 표시를 붙인다.
 *
 * <p>이 정보를 DB 설명(COMMENT ON)에 넣지 않고 코드에 둔 이유가 있다. 분류와 주의
 * 표시는 <b>이 화면이 어떻게 보여줄지</b>에 대한 것이지 스키마의 성질이 아니다.
 * DB 설명은 psql 이나 다른 도구도 읽으므로 그쪽에는 컬럼의 뜻만 남긴다.
 */
final class TableCatalog {

    private TableCatalog() {
    }

    /**
     * 값을 가려서 내보내는 컬럼.
     *
     * <p>비밀번호 해시와 토큰 해시는 <b>어떤 경우에도 화면에 띄우지 않는다.</b>
     * 스키마를 이해하는 데 값이 전혀 필요 없고, 한 번 새어 나가면 계정을 통째로
     * 넘겨주는 것과 같다. 컬럼이 있다는 사실과 뜻은 그대로 보여주되 값만 가린다.
     *
     * <p>기기 토큰(device_tokens.token)도 같다. 그 값이면 남의 기기로 푸시를 보낼 수 있다.
     */
    static final Set<String> MASKED_COLUMNS = Set.of(
            "password_hash", "token_hash", "token", "idempotency_key");

    /** 가려진 값 자리에 넣는 문자열. */
    static final String MASK = "(가려짐)";

    /**
     * 개인정보가 들어 있는 테이블.
     *
     * <p>막지는 않는다. 관리자는 사용자 관리 화면에서 이미 이름과 이메일을 본다.
     * 다만 아이 발화 원문처럼 무게가 다른 값이 섞여 있으므로, 열기 전에 무엇을
     * 보게 되는지 화면이 알려 준다.
     */
    static final Set<String> PERSONAL_DATA_TABLES = Set.of(
            "parents", "children", "child_consents", "daily_visits",
            "refresh_tokens", "password_reset_tokens", "device_tokens",
            "messages", "utterance_analyses", "reports", "wordbook", "word_practices",
            "post_activity_results", "inquiries", "inquiry_answers", "notifications",
            "admin_accounts", "admin_refresh_tokens", "admin_audit_logs",
            "admin_inquiry_notes", "admin_inquiry_assignees",
            "idempotent_requests");

    /**
     * 테이블 분류. 목록을 이름순으로만 늘어놓으면 39개 중에서 찾으려는 것을
     * 짚어내기 어렵다. 서비스의 도메인 경계와 같은 순서로 묶는다.
     */
    private static final List<Map.Entry<String, List<String>>> GROUPS = List.of(
            Map.entry("사용자", List.of(
                    "parents", "children", "child_consents",
                    "refresh_tokens", "password_reset_tokens", "daily_visits")),
            Map.entry("콘텐츠", List.of(
                    "stories", "topics", "story_topics", "characters",
                    "story_scenes", "scene_audio", "items")),
            Map.entry("진행 기록", List.of(
                    "story_sessions", "messages", "utterance_analyses",
                    "mission_results", "post_activity_results", "child_story_play_counts")),
            Map.entry("학습 결과", List.of("reports", "wordbook", "word_practices")),
            Map.entry("보상", List.of(
                    "stardust_wallets", "stardust_transactions", "child_items",
                    "planets", "planet_items")),
            Map.entry("고객 지원", List.of(
                    "notices", "guides", "inquiries", "inquiry_answers",
                    "notifications", "device_tokens",
                    "admin_reply_templates", "admin_inquiry_notes",
                    "admin_inquiry_assignees",
                    "admin_notice_schedules", "admin_notice_revisions")),
            Map.entry("관리자", List.of(
                    "admin_accounts", "admin_refresh_tokens", "admin_audit_logs")),
            Map.entry("시스템", List.of(
                    "idempotent_requests", "flyway_schema_history", "flyway_schema_history_admin")));

    /** 분류에 없는 테이블이 들어갈 자리. 서비스가 테이블을 새로 만들어도 목록에서 사라지지 않는다. */
    private static final String UNGROUPED = "기타";

    static String groupOf(String tableName) {
        return GROUPS.stream()
                .filter(group -> group.getValue().contains(tableName))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(UNGROUPED);
    }

    /** 화면이 그리는 분류 순서. 여기 없는 이름은 뒤에 붙는다. */
    static int groupOrder(String group) {
        for (int i = 0; i < GROUPS.size(); i++) {
            if (GROUPS.get(i).getKey().equals(group)) {
                return i;
            }
        }
        return GROUPS.size();
    }

    static boolean isMasked(String columnName) {
        return MASKED_COLUMNS.contains(columnName);
    }

    static boolean hasPersonalData(String tableName) {
        return PERSONAL_DATA_TABLES.contains(tableName);
    }
}
