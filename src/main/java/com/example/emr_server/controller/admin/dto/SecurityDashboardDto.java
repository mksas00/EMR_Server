package com.example.emr_server.controller.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SecurityDashboardDto(
        Stats stats,
        List<SeverityCount> incidentsBySeverity,
        List<TimeBucket> loginTimeline,
        List<TopItem> topFailingIps,
        List<TopItem> topActiveUsers
) {
    public record Stats(
            long activeSessions,
            long openIncidents,
            long failedLogins24h,
            long successfulLogins24h,
            long lockedAccounts,
            long totalUsers
    ) {}

    public record SeverityCount(String severity, long count) {}
    public record TimeBucket(Instant time, long success, long failed) {}
    public record TopItem(String key, long count) {}
}

