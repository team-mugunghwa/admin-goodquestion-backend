package com.mugunghwa.goodquestion.admin.story;

/**
 * 이야기 노출 상태. 서비스 백엔드의 같은 이름 enum과 값이 일치해야 한다
 * (같은 컬럼에 문자열로 저장된다).
 */
public enum StoryStatus {
    /** 작성 중. 사용자 목록에 나오지 않는다. */
    DRAFT,
    /** 공개. 사용자가 고를 수 있다. */
    PUBLISHED,
    /** 내림. 이미 진행한 세션은 그대로 두고 새로 고르지만 못하게 한다. */
    ARCHIVED
}
