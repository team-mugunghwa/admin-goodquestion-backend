package com.mugunghwa.goodquestion.admin.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    /** 발송 대상. 거절당해 비활성이 된 토큰은 제외한다. */
    List<DeviceToken> findAllByParentIdAndDisabledAtIsNull(UUID parentId);
}
