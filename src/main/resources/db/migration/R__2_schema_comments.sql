-- ============================================================
-- 테이블과 컬럼 설명
--
-- 이 DB에는 설명이 하나도 없었다. 컬럼의 뜻이 마이그레이션 파일의 SQL 주석과
-- 문서에만 있어서, 백엔드를 보지 않는 팀원은 확인할 방법이 없었다.
-- 관리자 콘솔의 데이터베이스 메뉴가 여기 심은 설명을 읽어 보여준다.
--
-- COMMENT ON 으로 넣는 이유는 이 콘솔 말고도 읽는 곳이 많기 때문이다.
-- psql 의 \d+, DBeaver, DataGrip, 스키마 문서 생성기가 전부 같은 자리를 본다.
-- 별도 설명 테이블을 만들면 그 도구들에서는 여전히 안 보인다.
--
-- 반복 마이그레이션(R__)이라 파일을 고치면 다음 기동에 다시 적용된다.
-- 설명은 코드보다 자주 손보게 되므로 버전 마이그레이션으로 두지 않았다.
--
-- 없는 테이블/컬럼은 조용히 건너뛴다. 서비스 백엔드가 컬럼을 지우거나
-- 이름을 바꿔도 이 파일 때문에 기동이 막히면 안 된다.
-- ============================================================

do $$
declare
    entry record;
begin
    for entry in
        select * from (values

-- ------------------------------------------------------------
-- 관리자 콘솔
-- ------------------------------------------------------------
('admin_accounts', null, '관리자 콘솔 로그인 계정. 보호자(parents)와 완전히 분리돼 있다. 한 테이블에 역할 컬럼만 얹으면 보호자 로그인 경로의 실수가 곧바로 관리자 권한 문제가 되기 때문이다.'),
('admin_accounts', 'id', '관리자 식별자'),
('admin_accounts', 'email', '로그인 이메일. 중복 불가'),
('admin_accounts', 'password_hash', 'BCrypt 해시. 원문 비밀번호는 어디에도 저장하지 않는다'),
('admin_accounts', 'name', '화면에 표시되는 이름. 문의 답변의 작성자로도 쓰인다'),
('admin_accounts', 'role', 'ADMIN(콘텐츠/고객센터) 또는 SUPER_ADMIN(관리자 계정 관리까지)'),
('admin_accounts', 'status', 'ACTIVE 또는 SUSPENDED. 정지된 계정은 로그인이 거부된다'),
('admin_accounts', 'failed_login_attempts', '연속 로그인 실패 횟수. 성공하면 0으로 돌아간다'),
('admin_accounts', 'locked_until', '잠금 해제 시각. 5회 실패하면 15분 잠긴다. null 이거나 과거면 로그인할 수 있다'),
('admin_accounts', 'last_login_at', '마지막 로그인 시각'),
('admin_accounts', 'last_login_ip', '마지막 로그인 IP. IPv6 최대 45자'),
('admin_accounts', 'created_at', '계정 생성 시각'),
('admin_accounts', 'updated_at', '마지막 수정 시각'),

('admin_refresh_tokens', null, '관리자 리프레시 토큰. 쓸 때마다 폐기하고 새로 발급한다(회전). 같은 토큰이 두 번 오면 그중 하나는 탈취된 것이므로 거절로 드러난다.'),
('admin_refresh_tokens', 'id', '토큰 행 식별자'),
('admin_refresh_tokens', 'admin_id', '이 토큰을 발급받은 관리자'),
('admin_refresh_tokens', 'token_hash', '토큰 원문의 SHA-256 해시. 원문은 저장하지 않아 DB가 유출돼도 그 값으로 재발급받을 수 없다'),
('admin_refresh_tokens', 'expires_at', '만료 시각. 발급 후 12시간'),
('admin_refresh_tokens', 'revoked_at', '폐기 시각. 값이 있으면 더는 쓸 수 없다'),
('admin_refresh_tokens', 'created_at', '발급 시각'),

('admin_audit_logs', null, '관리자 조작 기록. 상태를 바꾼 것만 남기고 조회는 남기지 않는다. 조회까지 남기면 목록 한 번 여는 것으로 수십 건이 쌓여 정작 확인할 삭제와 정지가 묻힌다.'),
('admin_audit_logs', 'id', '기록 식별자'),
('admin_audit_logs', 'admin_id', '조작한 관리자. 계정이 지워지면 null 이 된다'),
('admin_audit_logs', 'admin_email', '조작 당시의 관리자 이메일. 계정이 지워져도 누구였는지 남기려고 따로 저장한다'),
('admin_audit_logs', 'action', 'LOGIN, CREATE, UPDATE, DELETE, PUBLISH, SUSPEND, RESTORE, REVOKE_SESSION, ANSWER 등'),
('admin_audit_logs', 'target_type', '조작 대상 종류. NOTICE, STORY, PARENT, INQUIRY 같은 값'),
('admin_audit_logs', 'target_id', '조작 대상 식별자'),
('admin_audit_logs', 'summary', '사람이 읽을 한 줄 요약. 목록에서 이 문장만 보고 무슨 일이 있었는지 알 수 있어야 한다'),
('admin_audit_logs', 'ip', '조작한 관리자의 IP'),
('admin_audit_logs', 'created_at', '조작 시각'),

-- ------------------------------------------------------------
-- 사용자 (보호자와 아이)
-- ------------------------------------------------------------
('parents', null, '보호자 계정. 로그인의 주체이고 아이는 이 아래에 딸린다. LOCAL 은 이메일과 비밀번호로, KAKAO/GOOGLE 은 제공자 식별자로 구분한다.'),
('parents', 'id', '보호자 식별자. 토큰의 sub 값이 이것이다'),
('parents', 'email', '이메일. 소셜 로그인은 제공자가 안 주면 비어 있을 수 있다'),
('parents', 'password_hash', 'BCrypt 해시. 소셜 계정은 비어 있다'),
('parents', 'provider', 'LOCAL, KAKAO, GOOGLE 중 하나'),
('parents', 'provider_id', '소셜 제공자가 준 사용자 식별자. LOCAL 은 비어 있다'),
('parents', 'name', '보호자 이름'),
('parents', 'created_at', '가입 시각'),
('parents', 'failed_login_attempts', '연속 로그인 실패 횟수'),
('parents', 'locked_until', '잠금 해제 시각. 5회 실패부터 잠기고 이후 실패마다 시간이 두 배로 늘어난다(최대 24시간)'),
('parents', 'last_login_ip', '마지막 로그인 IP'),
('parents', 'status', 'ACTIVE 또는 SUSPENDED. 관리자가 정지하면 로그인이 거부된다'),
('parents', 'suspended_at', '정지 시각'),
('parents', 'suspended_reason', '정지 사유. 나중에 왜 막았는지 확인할 근거다'),

('children', null, '아이 프로필. 한 보호자에 여러 명을 둘 수 있고 진행 기록과 보상은 아이별로 따로 관리된다.'),
('children', 'id', '아이 식별자'),
('children', 'parent_id', '보호자. 보호자가 지워지면 함께 지워진다'),
('children', 'name', '아이 이름. 화면에서 부르는 이름이다'),
('children', 'birth_year', '태어난 해. 나이 대신 연도로 두어 해가 바뀌어도 갱신할 필요가 없다'),
('children', 'created_at', '등록 시각'),

('child_consents', null, '아동 개인정보 처리 동의 기록. 아이가 서비스를 쓰려면 유효한 동의가 있어야 하고, 없으면 이야기 진입이 막힌다.'),
('child_consents', 'id', '동의 기록 식별자'),
('child_consents', 'child_id', '동의 대상 아이'),
('child_consents', 'consent_version', '동의한 약관 버전. 약관이 바뀌면 다시 받아야 한다'),
('child_consents', 'verification_method', 'AUTHENTICATED_PARENT(로그인한 보호자), INSTITUTION_PAPER(기관 서면), MOBILE_VERIFICATION(본인인증)'),
('child_consents', 'consented_at', '동의 시각'),
('child_consents', 'withdrawn_at', '철회 시각. 값이 있으면 유효한 동의가 아니다'),

('refresh_tokens', null, '사용자 앱의 리프레시 토큰. 관리자가 로그인 세션을 강제 종료하면 여기에 폐기 시각이 찍힌다.'),
('refresh_tokens', 'id', '토큰 행 식별자'),
('refresh_tokens', 'parent_id', '이 토큰을 발급받은 보호자'),
('refresh_tokens', 'token_hash', '토큰 원문의 해시. 원문은 저장하지 않는다'),
('refresh_tokens', 'expires_at', '만료 시각. 발급 후 14일'),
('refresh_tokens', 'revoked_at', '폐기 시각. 관리자가 세션을 끊거나 사용자가 로그아웃하면 찍힌다'),
('refresh_tokens', 'created_at', '발급 시각'),

('password_reset_tokens', null, '비밀번호 재설정 링크의 토큰. 메일로 보낸 링크가 이 값을 들고 온다.'),
('password_reset_tokens', 'id', '토큰 행 식별자'),
('password_reset_tokens', 'parent_id', '재설정 대상 보호자'),
('password_reset_tokens', 'token_hash', '토큰 원문의 해시'),
('password_reset_tokens', 'expires_at', '만료 시각. 발급 후 30분'),
('password_reset_tokens', 'consumed_at', '사용 시각. 값이 있으면 이미 쓴 링크라 재사용할 수 없다'),
('password_reset_tokens', 'created_at', '발급 시각'),

('daily_visits', null, '일자별 방문 기록. 관리자 대시보드의 오늘 방문자 수가 이 행 수를 센다. (보호자, 날짜)가 기본키라 하루에 몇 번 들어와도 1명으로 집계된다.'),
('daily_visits', 'parent_id', '방문한 보호자'),
('daily_visits', 'visit_date', '방문 날짜. 한국 시간 기준'),
('daily_visits', 'visit_count', '그날 들어온 횟수. 방문자 수 집계에는 쓰지 않고 이용 빈도를 볼 때 쓴다'),
('daily_visits', 'last_seen_at', '그날 마지막으로 요청을 보낸 시각'),

-- ------------------------------------------------------------
-- 콘텐츠 (이야기, 장면, 캐릭터)
-- ------------------------------------------------------------
('stories', null, '이야기. 아이가 고르는 콘텐츠 한 편이다. PUBLISHED 상태만 사용자 앱 목록에 나간다.'),
('stories', 'id', '이야기 식별자'),
('stories', 'title', '이야기 제목'),
('stories', 'summary', '목록과 상세에 보이는 줄거리'),
('stories', 'child_role', '아이가 맡는 역할. 상세 화면에서 너는 무엇이라고 알려 주는 문구'),
('stories', 'intro', '시작 전에 들려주는 도입 상황 설명'),
('stories', 'image_url', '대표 이미지 경로'),
('stories', 'difficulty', 'EASY, NORMAL, HARD. 화면에는 숫자가 아니라 점 세 개로 표시한다'),
('stories', 'estimated_minutes', '예상 소요 시간(분)'),
('stories', 'post_activity_config', '이야기를 마친 뒤 활동 설정. 카드 내용과 정답 순서, 재구성 키워드가 들어 있다'),
('stories', 'status', 'DRAFT(작성 중), PUBLISHED(공개), ARCHIVED(내림)'),
('stories', 'created_at', '등록 시각'),
('stories', 'updated_at', '마지막 수정 시각. 관리자 콘솔이 추가한 컬럼이다'),

('topics', null, '주제 마스터. 사용자 앱 이야기 목록의 필터 칩이 이 순서대로 그려진다.'),
('topics', 'id', '주제 식별자'),
('topics', 'name', '주제 이름. 중복 불가'),
('topics', 'display_order', '노출 순서. 작을수록 앞'),
('topics', 'created_at', '등록 시각'),

('story_topics', null, '이야기와 주제의 연결. 한 이야기에 주제가 여러 개 붙을 수 있다.'),
('story_topics', 'story_id', '이야기'),
('story_topics', 'topic_id', '주제'),

('characters', null, '이야기에 등장하는 캐릭터. 성격과 목소리를 여기 모아 두어, 같은 캐릭터가 장면마다 다른 사람처럼 들리는 것을 막는다.'),
('characters', 'id', '캐릭터 식별자'),
('characters', 'story_id', '소속 이야기'),
('characters', 'character_key', '표정 이미지 파일명의 앞부분. {키}_{표정}.png 규칙이라 바꾸면 이미 올린 이미지가 안 붙는다'),
('characters', 'name', '화면에 표시되는 이름'),
('characters', 'personality', '성격과 말투. 캐릭터 대사를 만드는 모델이 그대로 참고한다'),
('characters', 'guidance_style', '아이가 막혔을 때 이 캐릭터가 도와주는 방식'),
('characters', 'tts_voice', '음성 합성에 쓰는 보이스 이름'),
('characters', 'tts_style', '연기 지시문. 보이스 이름만으로는 성별이 정해지지 않아 성별과 연령을 반드시 적는다'),
('characters', 'tts_gender', '합성 결과의 성별 기대값. 검증용이고 null 이면 검사하지 않는다'),
('characters', 'expression_keys', '이 캐릭터가 실제로 가진 표정 목록. 없는 표정을 요구하면 기본 표정으로 대체된다'),
('characters', 'created_at', '등록 시각'),

('story_scenes', null, '장면. 이야기를 이루는 단위이고 순서대로 진행된다. STORY 는 듣기만 하는 내레이션, DIALOGUE 는 아이가 말하는 대화 장면이다.'),
('story_scenes', 'id', '장면 식별자'),
('story_scenes', 'story_id', '소속 이야기'),
('story_scenes', 'scene_order', '진행 순서. (이야기, 순서)가 유일해야 한다'),
('story_scenes', 'scene_type', 'STORY(내레이션) 또는 DIALOGUE(대화)'),
('story_scenes', 'scene_description', '내레이션이면 들려줄 본문, 대화면 상황 설명. 대화 장면에서는 발화를 분석할 때 맥락으로 쓰인다'),
('story_scenes', 'conflict', '이 장면에서 벌어지는 갈등'),
('story_scenes', 'image_url', '장면 배경 이미지 경로'),
('story_scenes', 'character_id', '이 장면에서 대화하는 캐릭터'),
('story_scenes', 'character_name', '화면 표시용 캐릭터 이름. 캐릭터를 지정하면 그 이름으로 맞춰 둔다'),
('story_scenes', 'scene_stance', '이 장면에서의 캐릭터 입장. 같은 캐릭터라도 장면마다 태도가 다르다'),
('story_scenes', 'proper_nouns', '음성 인식 힌트로 넘길 고유명사. 아이 발화는 고유명사 오인식이 가장 많다'),
('story_scenes', 'character_opening', '장면이 시작될 때 캐릭터가 먼저 건네는 말'),
('story_scenes', 'character_closing', '장면을 닫을 때 쓰는 고정 대사. 최대 턴에 닿아도 이 대사로 마무리한다'),
('story_scenes', 'scene_goal', '이 장면에서 아이가 말해 봤으면 하는 것'),
('story_scenes', 'required_elements', '확인할 생각 요소. DECISION(선택), REASON(이유), PERSPECTIVE(상대 입장), SOLUTION, RESULT, EMOTION, EMPATHY, REQUEST'),
('story_scenes', 'element_criteria', '요소별 인정 기준. 발화 분석 모델의 입력으로 들어간다'),
('story_scenes', 'remaining_worries', '요소별로 캐릭터가 아직 품고 있는 걱정. 유도할 때 대사 생성에 쓰인다'),
('story_scenes', 'mission_config', '장면 안 미션 설정. 미션이 없는 장면은 비어 있다'),
('story_scenes', 'preferred_turns', '이만큼 말하면 장면 목표를 채운 것으로 본다'),
('story_scenes', 'max_turns', '여기 닿으면 목표와 무관하게 장면을 닫는다'),

('scene_audio', null, '미리 만들어 둔 장면 음성. 매번 합성하면 느리고 비싸서 고정 대사는 파일로 만들어 둔다.'),
('scene_audio', 'id', '음성 식별자'),
('scene_audio', 'scene_id', '대상 장면'),
('scene_audio', 'slot', '장면 안에서의 자리. 오프닝, 내레이션, 클로징 등'),
('scene_audio', 'child_id', '아이별로 다르게 만든 음성일 때의 대상 아이. 공용이면 비어 있다'),
('scene_audio', 'storage_path', '음성 파일 경로'),
('scene_audio', 'text_hash', '원본 문장의 해시. 대사가 바뀌면 값이 달라져 다시 만들어야 하는 것을 알 수 있다'),
('scene_audio', 'engine', '합성에 쓴 벤더. openai 또는 gemini'),
('scene_audio', 'voice', '합성에 쓴 보이스 이름'),
('scene_audio', 'style_prompt', '합성할 때 넘긴 연기 지시문'),
('scene_audio', 'speaking_rate', '말하기 속도'),
('scene_audio', 'duration_ms', '음성 길이(밀리초)'),
('scene_audio', 'sentence_timings', '문장별 시작과 끝 시각. 자막을 음성에 맞춰 띄우는 데 쓴다'),
('scene_audio', 'created_at', '생성 시각'),

('items', null, '내 행성 꾸미기 아이템 마스터. 아이가 별가루로 산다.'),
('items', 'id', '아이템 식별자'),
('items', 'name', '아이템 이름'),
('items', 'category', 'TERRAIN_PROP(지형), PLANT(식물), STRUCTURE(건물), ANIMAL(동물)'),
('items', 'price', '가격(별가루)'),
('items', 'unlock_type', '해금 조건 종류. 항상 열림, 이야기 완주, 별가루 누적 중 하나'),
('items', 'unlock_story_id', '이야기 완주로 해금될 때의 대상 이야기'),
('items', 'unlock_stardust_total', '별가루 누적으로 해금될 때의 기준 누적량'),
('items', 'model_url', '3D 모델 파일 경로'),
('items', 'thumbnail_url', '상점 목록에 보이는 썸네일 경로'),
('items', 'display_order', '상점 노출 순서'),
('items', 'status', '판매 상태'),
('items', 'created_at', '등록 시각'),

-- ------------------------------------------------------------
-- 진행 기록 (세션과 대화)
-- ------------------------------------------------------------
('story_sessions', null, '이야기 한 편의 진행 기록. 아이가 이야기를 시작하면 만들어지고 장면을 넘길 때마다 갱신된다. 대부분의 컬럼은 다음에 무엇을 할지 판단하는 데 쓰이는 진행 상태다.'),
('story_sessions', 'id', '세션 식별자'),
('story_sessions', 'child_id', '진행한 아이'),
('story_sessions', 'story_id', '진행 중인 이야기'),
('story_sessions', 'current_scene_id', '지금 머물러 있는 장면'),
('story_sessions', 'current_child_turn_count', '현재 장면에서 아이가 말한 횟수. 장면을 옮기면 0으로 돌아간다'),
('story_sessions', 'accumulated_elements', '현재 장면에서 지금까지 확인된 생각 요소. 장면을 옮기면 비워진다'),
('story_sessions', 'last_detected_elements', '마지막 발화에서 새로 확인된 요소'),
('story_sessions', 'last_response_mode', 'NORMAL(평범한 대화), GUIDED(막혀서 유도), CLOSING(마무리)'),
('story_sessions', 'last_guidance_target', '유도할 때 목표로 잡은 생각 요소'),
('story_sessions', 'turns_without_new_element', '새 요소 없이 흘러간 연속 턴 수. 기준을 넘으면 유도로 전환한다'),
('story_sessions', 'consecutive_low_information_turns', '내용이 거의 없는 발화가 연속으로 나온 횟수'),
('story_sessions', 'scene_goal_met', '현재 장면의 목표를 채웠는지'),
('story_sessions', 'scene_end_reason', 'GOAL_MET(목표 달성) 또는 MAX_TURNS(턴 소진)'),
('story_sessions', 'guided_used_in_scene', '현재 장면에서 유도를 한 번이라도 썼는지. 유도 없이 통과하면 별가루 보너스 대상이다'),
('story_sessions', 'mission_exposed', '현재 장면의 미션을 보여줬는지'),
('story_sessions', 'mission_completed', '현재 장면의 미션을 마쳤는지'),
('story_sessions', 'safety_flagged', '아이 발화에서 위험 신호가 감지된 세션인지. 확인이 필요하다'),
('story_sessions', 'safety_categories', '감지된 위험 범주. 아이 발화 원문은 여기 남기지 않는다'),
('story_sessions', 'safety_flagged_at', '위험 신호가 감지된 시각'),
('story_sessions', 'status', 'IN_PROGRESS(진행 중), POST_ACTIVITY(후속 활동), COMPLETED(완료), STOPPED(중단)'),
('story_sessions', 'version', '낙관적 락 번호. 아이가 연타해 같은 세션에 두 턴이 겹치면 나중 것을 거절하는 데 쓴다'),
('story_sessions', 'started_at', '시작 시각'),
('story_sessions', 'completed_at', '완료 시각'),
('story_sessions', 'last_activity_at', '마지막 활동 시각. 홈의 이어하기가 이 순서로 고른다'),

('messages', null, '대화 기록. 아이와 캐릭터가 주고받은 말이 순서대로 쌓인다. 아이 발화 원문이 들어 있으므로 열람에 주의한다.'),
('messages', 'id', '메시지 식별자'),
('messages', 'session_id', '소속 세션'),
('messages', 'scene_id', '이 말이 오간 장면'),
('messages', 'speaker_type', '말한 쪽. 아이인지 캐릭터인지'),
('messages', 'turn_order', '장면 안에서의 순서'),
('messages', 'text', '실제 말한 내용. 아이 발화는 음성 인식 결과를 다듬은 값이다'),
('messages', 'stt_raw_text', '음성 인식이 돌려준 원문. 다듬기 전 값이라 오인식을 확인할 때 쓴다'),
('messages', 'stt_confidence', '음성 인식 신뢰도. 토큰 확률의 평균에 지수를 취한 값이다'),
('messages', 'stt_low_confidence', '신뢰도가 기준(0.5) 아래인지. 아이 음성은 분포가 낮게 깔려 성인 기준을 그대로 쓰지 않는다'),
('messages', 'stt_retry_count', '못 알아들어 다시 말해 달라고 한 횟수'),
('messages', 'character_emotion', '캐릭터 발화일 때의 표정. 아이 발화에는 없다'),
('messages', 'created_at', '기록 시각'),

('utterance_analyses', null, '아이 발화 한 건의 분석 결과. 어떤 생각 요소가 담겼는지 판단해 진행 여부를 정하는 근거가 된다.'),
('utterance_analyses', 'id', '분석 식별자'),
('utterance_analyses', 'message_id', '분석 대상 발화'),
('utterance_analyses', 'child_intent', '아이가 무엇을 하려던 말인지'),
('utterance_analyses', 'main_point', '발화의 핵심을 한 줄로 정리한 것'),
('utterance_analyses', 'detected_elements', '이 발화에서 확인된 생각 요소와 근거'),
('utterance_analyses', 'utterance_validity', '분석에 쓸 만한 발화인지. 무의미하거나 잘못 인식된 경우를 걸러낸다'),
('utterance_analyses', 'analysis_version', '분석 규칙 버전. 규칙이 바뀌면 예전 결과와 섞이지 않게 구분한다'),
('utterance_analyses', 'model_id', '분석에 쓴 모델 이름'),
('utterance_analyses', 'dropped_evidence', '요소로 인정하지 않고 버린 근거. 왜 인정 안 됐는지 확인할 때 본다'),
('utterance_analyses', 'created_at', '분석 시각'),

('mission_results', null, '장면 안 미션의 수행 결과. 세션과 미션당 한 건이라 중복 제출이 막힌다.'),
('mission_results', 'id', '결과 식별자'),
('mission_results', 'session_id', '소속 세션'),
('mission_results', 'scene_id', '미션이 있던 장면'),
('mission_results', 'mission_id', '미션 식별자'),
('mission_results', 'mission_type', 'PROBLEM_SOLVING(문제 해결) 또는 PERSPECTIVE_SHIFT(입장 바꾸기)'),
('mission_results', 'result', '아이가 고른 값. 미션 종류마다 구조가 다르다'),
('mission_results', 'created_at', '제출 시각'),

('post_activity_results', null, '이야기를 마친 뒤 활동의 결과. 장면 카드를 순서대로 놓고 이야기를 다시 말해 보는 단계다.'),
('post_activity_results', 'id', '결과 식별자'),
('post_activity_results', 'session_id', '소속 세션'),
('post_activity_results', 'card_order_seed', '카드를 섞은 기준값. 같은 값이면 같은 순서로 다시 섞인다'),
('post_activity_results', 'submitted_order', '아이가 놓은 카드 순서'),
('post_activity_results', 'is_order_correct', '순서를 맞췄는지'),
('post_activity_results', 'attempt_count', '시도 횟수'),
('post_activity_results', 'retelling_text', '아이가 다시 말한 이야기'),
('post_activity_results', 'completed_at', '완료 시각'),

('child_story_play_counts', null, '아이가 같은 이야기를 몇 번 완주했는지. 반복 완주에 보상 상한을 두는 데 쓴다.'),
('child_story_play_counts', 'child_id', '아이'),
('child_story_play_counts', 'story_id', '이야기'),
('child_story_play_counts', 'play_count', '완주 횟수'),
('child_story_play_counts', 'updated_at', '마지막 완주 시각'),

-- ------------------------------------------------------------
-- 학습 결과
-- ------------------------------------------------------------
('reports', null, '보호자 리포트. 아이가 이야기를 마칠 때마다 대화를 바탕으로 만들어진다. 잘한 점을 먼저 짚는 순서가 화면에 고정돼 있다.'),
('reports', 'id', '리포트 식별자'),
('reports', 'session_id', '대상 세션'),
('reports', 'summary', '이번 활동 요약'),
('reports', 'strengths', '잘한 점과 근거가 된 아이 발화'),
('reports', 'next_focus', '다음에 함께 해 보면 좋을 것'),
('reports', 'created_at', '생성 시각'),
('reports', 'analysis', '리포트를 만들 때 쓴 분석 원본. 화면에는 내보내지 않는다'),

('wordbook', null, '아이가 담은 단어. 이야기를 하다 어려운 낱말을 만나면 저장해 두고 나중에 다시 듣는다.'),
('wordbook', 'id', '단어 식별자'),
('wordbook', 'child_id', '담은 아이'),
('wordbook', 'word', '단어'),
('wordbook', 'meaning', '뜻. 사용자가 안 적으면 모델이 만들어 채운다'),
('wordbook', 'example_sentence', '예문'),
('wordbook', 'entry_type', '어떻게 담겼는지. 이야기 중 저장인지 직접 입력인지'),
('wordbook', 'source_scene_id', '이 단어를 만난 장면'),
('wordbook', 'created_at', '저장 시각'),

('word_practices', null, '담은 단어를 소리 내어 말해 본 기록. 단어당 하루 정해진 횟수까지 별가루를 준다.'),
('word_practices', 'id', '연습 식별자'),
('word_practices', 'wordbook_id', '연습한 단어'),
('word_practices', 'child_id', '연습한 아이'),
('word_practices', 'spoken_text', '아이가 말한 내용의 음성 인식 결과'),
('word_practices', 'created_at', '연습 시각'),

-- ------------------------------------------------------------
-- 보상 (별가루와 내 행성)
-- ------------------------------------------------------------
('stardust_wallets', null, '아이별 별가루 지갑. 아이를 만들면 자동으로 함께 생긴다.'),
('stardust_wallets', 'id', '지갑 식별자'),
('stardust_wallets', 'child_id', '지갑 주인'),
('stardust_wallets', 'balance', '지금 남은 별가루'),
('stardust_wallets', 'total_earned', '지금까지 번 별가루 총합. 누적 기준 해금에 쓰인다'),
('stardust_wallets', 'created_at', '생성 시각'),

('stardust_transactions', null, '별가루가 들고 난 기록. 잔액은 이 기록의 합과 맞아야 한다.'),
('stardust_transactions', 'id', '거래 식별자'),
('stardust_transactions', 'wallet_id', '대상 지갑'),
('stardust_transactions', 'amount', '증감량. 버는 것은 양수, 쓰는 것은 음수'),
('stardust_transactions', 'reason', '사유. 이야기 완주, 장면 보너스, 아이템 구매, 가입 환영 등'),
('stardust_transactions', 'session_id', '이야기 진행으로 생긴 거래일 때의 세션'),
('stardust_transactions', 'scene_id', '장면 보너스일 때의 장면'),
('stardust_transactions', 'item_id', '아이템 구매일 때의 아이템'),
('stardust_transactions', 'acknowledged', '아이에게 지급 연출을 보여줬는지'),
('stardust_transactions', 'created_at', '거래 시각'),

('child_items', null, '아이가 산 아이템. 여기 있어야 행성에 놓을 수 있다.'),
('child_items', 'id', '보유 식별자'),
('child_items', 'child_id', '보유한 아이'),
('child_items', 'item_id', '아이템'),
('child_items', 'acquired_at', '구매 시각'),

('planets', null, '아이의 행성. 아이를 만들면 자동으로 함께 생긴다.'),
('planets', 'id', '행성 식별자'),
('planets', 'child_id', '행성 주인'),
('planets', 'name', '행성 이름'),
('planets', 'tutorial_completed', '꾸미기 안내를 봤는지'),
('planets', 'created_at', '생성 시각'),

('planet_items', null, '행성에 놓인 아이템의 위치. 한 칸에 하나만 놓을 수 있다.'),
('planet_items', 'id', '배치 식별자'),
('planet_items', 'planet_id', '대상 행성'),
('planet_items', 'child_item_id', '놓은 아이템'),
('planet_items', 'placed_q', '가로 좌표. 원점 기준이라 음수도 유효하다'),
('planet_items', 'placed_r', '세로 좌표. 원점 기준이라 음수도 유효하다'),
('planet_items', 'placed_at', '배치 시각'),

-- ------------------------------------------------------------
-- 고객 지원 (관리자 콘솔이 쓰고 사용자 앱이 읽는다)
-- ------------------------------------------------------------
('notices', null, '공지사항. 관리자가 쓰고 사용자 앱이 읽는다. PUBLISHED 만 사용자에게 나간다.'),
('notices', 'id', '공지 식별자'),
('notices', 'title', '제목'),
('notices', 'content', '본문. 줄바꿈이 그대로 화면에 반영된다'),
('notices', 'category', 'GENERAL(일반), UPDATE(업데이트), EVENT(이벤트), MAINTENANCE(점검)'),
('notices', 'pinned', '목록 맨 위 고정 여부'),
('notices', 'status', 'DRAFT(비공개), PUBLISHED(공개), ARCHIVED(보관)'),
('notices', 'published_at', '공개 시각. 비공개에서 공개로 바뀌는 순간에만 찍고 이후 수정에는 갱신하지 않는다'),
('notices', 'view_count', '조회수. 상세를 열 때마다 오른다'),
('notices', 'author_name', '작성 당시의 관리자 이름'),
('notices', 'created_at', '작성 시각'),
('notices', 'updated_at', '마지막 수정 시각'),

('guides', null, '이용안내 문서. 공지와 달리 시간순이 아니라 관리자가 정한 순서로 노출된다.'),
('guides', 'id', '문서 식별자'),
('guides', 'category', 'BASIC(서비스 소개), ACCOUNT(계정), PLAY(이야기 진행), REWARD(별가루), TROUBLE(문제 해결)'),
('guides', 'title', '제목. 사용자가 목록에서 보는 질문 형태로 적는다'),
('guides', 'content', '본문'),
('guides', 'display_order', '분류 안에서의 노출 순서. 작을수록 위'),
('guides', 'status', 'DRAFT(비공개), PUBLISHED(공개), ARCHIVED(보관)'),
('guides', 'created_at', '작성 시각'),
('guides', 'updated_at', '마지막 수정 시각'),

('inquiries', null, '고객센터 문의. 사용자 앱이 만들고 관리자가 답변한다.'),
('inquiries', 'id', '문의 식별자'),
('inquiries', 'parent_id', '문의한 보호자'),
('inquiries', 'category', 'ACCOUNT, PAYMENT, CONTENT, BUG, SUGGESTION, ETC'),
('inquiries', 'title', '제목'),
('inquiries', 'content', '문의 내용'),
('inquiries', 'status', 'PENDING(답변 대기), ANSWERED(답변 완료), CLOSED(종료)'),
('inquiries', 'answered_at', '처음 답변이 등록된 시각. 답변을 고쳐도 갱신하지 않는다'),
('inquiries', 'created_at', '접수 시각'),
('inquiries', 'updated_at', '마지막 수정 시각'),

('inquiry_answers', null, '문의 답변. 문의당 한 건으로 제한된다. 여러 건을 허용하면 사용자 화면이 어느 것이 최종 답변인지 판단해야 한다.'),
('inquiry_answers', 'id', '답변 식별자'),
('inquiry_answers', 'inquiry_id', '대상 문의. 중복 불가'),
('inquiry_answers', 'admin_id', '답변한 관리자. 계정이 지워지면 null 이 된다'),
('inquiry_answers', 'admin_name', '답변자 이름. 사용자 화면에 이 이름이 보인다'),
('inquiry_answers', 'content', '답변 내용'),
('inquiry_answers', 'created_at', '최초 답변 시각'),
('inquiry_answers', 'updated_at', '마지막 수정 시각. 사용자가 보는 것은 최신 내용이라 화면에는 이 값을 쓴다'),

('notifications', null, '사용자 알림함. 푸시가 도착하지 않아도 여기 남아 있어야 사용자가 답변을 확인할 수 있다. 푸시는 알리는 수단이지 전달 경로가 아니다.'),
('notifications', 'id', '알림 식별자'),
('notifications', 'parent_id', '받는 보호자'),
('notifications', 'type', 'INQUIRY_ANSWERED(문의 답변), NOTICE(공지), SYSTEM(운영)'),
('notifications', 'title', '알림 제목'),
('notifications', 'body', '알림 내용. 잠금 화면에도 뜨므로 답변 본문은 싣지 않는다'),
('notifications', 'link_path', '누르면 이동할 앱 안의 경로. 예를 들어 /support/{문의id}'),
('notifications', 'read_at', '읽은 시각. 비어 있으면 안 읽은 알림이다'),
('notifications', 'created_at', '생성 시각'),

('device_tokens', null, '푸시를 받을 기기. 앱이 뜰 때마다 등록한다. 토큰은 재설치나 장기 미사용으로 바뀐다.'),
('device_tokens', 'id', '기기 식별자'),
('device_tokens', 'parent_id', '기기 주인. 같은 토큰이 다른 사람으로 다시 등록되면 주인이 바뀐다'),
('device_tokens', 'token', 'FCM 등록 토큰. 중복 불가'),
('device_tokens', 'platform', 'ANDROID, IOS, WEB'),
('device_tokens', 'disabled_at', '발송이 등록되지 않은 토큰으로 거절된 시각. 바로 지우지 않는 이유는 왜 안 갔는지 확인할 근거를 남기려는 것이다'),
('device_tokens', 'created_at', '등록 시각'),
('device_tokens', 'updated_at', '마지막 갱신 시각'),

-- ------------------------------------------------------------
-- 시스템
-- ------------------------------------------------------------
('idempotent_requests', null, '같은 요청이 두 번 처리되는 것을 막는 기록. 타임아웃 뒤 재시도가 발화를 두 번 처리하거나 별가루를 두 번 빼는 것을 막는다.'),
('idempotent_requests', 'id', '기록 식별자'),
('idempotent_requests', 'endpoint', '대상 엔드포인트. 발화 제출과 아이템 구매에 적용한다'),
('idempotent_requests', 'scope_id', '키가 유효한 범위. 발화는 세션, 구매는 아이'),
('idempotent_requests', 'parent_id', '요청한 보호자. 응답을 재생할 때 소유자를 확인한다'),
('idempotent_requests', 'idempotency_key', '클라이언트가 보낸 키. 같은 키가 다시 오면 재전송으로 본다'),
('idempotent_requests', 'status', 'IN_PROGRESS(처리 중) 또는 COMPLETED(완료)'),
('idempotent_requests', 'response', '완료된 응답 본문. 같은 키가 다시 오면 이 값을 그대로 돌려준다'),
('idempotent_requests', 'created_at', '기록 시각. 24시간 지난 행이 청소 대상이다'),
('idempotent_requests', 'completed_at', '완료 시각'),

('flyway_schema_history', null, '서비스 백엔드(goodquestion-backend)가 적용한 DB 변경 이력. Flyway 가 직접 관리하므로 손으로 고치지 않는다.'),
('flyway_schema_history_admin', null, '관리자 콘솔(admin-goodquestion-backend)이 적용한 DB 변경 이력. 서비스와 이력을 나눠 두어야 서로의 마이그레이션을 다시 돌리려 들지 않는다.')

        ) as t(tbl, col, note)
    loop
        if entry.col is null then
            if exists (select 1 from information_schema.tables
                       where table_schema = 'public' and table_name = entry.tbl) then
                execute format('comment on table public.%I is %L', entry.tbl, entry.note);
            end if;
        else
            if exists (select 1 from information_schema.columns
                       where table_schema = 'public'
                         and table_name = entry.tbl and column_name = entry.col) then
                execute format('comment on column public.%I.%I is %L',
                               entry.tbl, entry.col, entry.note);
            end if;
        end if;
    end loop;
end $$;
