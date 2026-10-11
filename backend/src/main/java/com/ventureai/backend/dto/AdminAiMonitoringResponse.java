package com.ventureai.backend.dto;

import java.util.List;

public record AdminAiMonitoringResponse(
        long totalExecutions,
        long totalSuccessfulExecutions,
        long totalFailedExecutions,
        double overallSuccessRatePercent,

        long totalFeedbackSubmissions,
        Double averageFeedbackRating,

        List<TaskExecutionStats> byTask,
        List<ModelVersionStats> byModelVersion,
        List<ErrorCategoryStats> errorsByCategory,
        List<FeedbackCategoryStats> feedbackByCategory,
        List<DailyExecutionPoint> dailyTrend,

        String note
) {}