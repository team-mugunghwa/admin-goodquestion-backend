package com.mugunghwa.goodquestion.admin.dashboard;

import com.mugunghwa.goodquestion.admin.dashboard.dto.DashboardDtos.DashboardSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대시보드.
 *
 * <p>한 번의 호출로 화면 전체를 채운다. 카드마다 엔드포인트를 나누면 첫 화면에서
 * 요청이 예닐곱 개 나가고, 그중 하나가 느리면 화면이 조각조각 채워진다.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardSummary summary() {
        return dashboardService.summary();
    }
}
