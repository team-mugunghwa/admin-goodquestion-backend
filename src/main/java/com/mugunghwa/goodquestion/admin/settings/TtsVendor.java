package com.mugunghwa.goodquestion.admin.settings;

/** 본 서버의 실시간 합성 벤더. 본 서버의 ai.tts.TtsVendor와 이름을 맞춘다. */
public enum TtsVendor {
    /** gpt-4o-mini-tts. 키만 있으면 항상 가용 - 기본값 */
    OPENAI,
    /** gemini-2.5-flash-preview-tts. 사전 렌더와 같은 화자, 무료 등급 한도 빠듯 */
    GEMINI,
    /** Cloud TTS Chirp 3: HD. 월 100만 자 무료 - 테스트 기간용 */
    CHIRP3
}
