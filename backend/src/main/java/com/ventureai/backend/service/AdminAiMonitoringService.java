package com.ventureai.backend.service;

import com.ventureai.backend.dto.*;
import com.ventureai.backend.entity.AiErrorCategory;
import com.ventureai.backend.entity.AiExecution;
import com.ventureai.backend.entity.AiExecutionOutcome;
import com.ventureai.backend.entity.AiTaskType;
import com.ventureai.backend.entity.FeedbackCategory;
import com.ventureai.backend.repository.AiExecutionRepository;
import com.ventureai.backend.repository.AiFeedbackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminAiMonitoringService {

    private final AiExecutionRepository executionRepository;
    private final AiFeedbackRepository feedbackRepository;

    public AdminAiMonitoringService(AiExecutionRepository executionRepository,
                                    AiFeedbackRepository feedbackRepository) {
        this.executionRepository = executionRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional(readOnly = true)
    public AdminAiMonitoringResponse getMonitoring(int trendDays) {
        int days = Math.max(1, Math.min(trendDays, 90));
        LocalDateTime since = LocalDate.now().minusDays(days - 1L).atStartOfDay();

        long total = executionRepository.count();
        long successes = executionRepository.countByOutcome(AiExecutionOutcome.SUCCESS);
        long failures = executionRepository.countByOutcome(AiExecutionOutcome.FAILED);
        double successRate = total > 0 ? round2(100.0 * successes / total) : 0.0;

        List<TaskExecutionStats> byTask = new ArrayList<>();
        for (AiTaskType task : AiTaskType.values()) {
            long tTotal = executionRepository.countByTask(task);
            if (tTotal == 0) continue;
            long tSucc = executionRepository.countByTaskAndOutcome(task, AiExecutionOutcome.SUCCESS);
            long tFail = executionRepository.countByTaskAndOutcome(task, AiExecutionOutcome.FAILED);
            Double avgLat = executionRepository.avgLatencyByTask(task);
            Long maxLat = executionRepository.maxLatencyByTask(task);
            double tRate = round2(100.0 * tSucc / tTotal);
            byTask.add(new TaskExecutionStats(
                    task.name(), tTotal, tSucc, tFail, tRate,
                    avgLat == null ? null : round2(avgLat), maxLat));
        }

        List<ModelVersionStats> byModel = executionRepository
                .countGroupedByModelVersion().stream()
                .map(r -> new ModelVersionStats((String) r[0], ((Number) r[1]).longValue()))
                .toList();

        List<ErrorCategoryStats> errorsByCategory = executionRepository
                .countGroupedByErrorCategory().stream()
                .map(r -> new ErrorCategoryStats(
                        ((AiErrorCategory) r[0]).name(), ((Number) r[1]).longValue()))
                .toList();

        long totalFeedback = feedbackRepository.count();
        Double avgRating = feedbackRepository.averageRating();

        List<FeedbackCategoryStats> feedbackByCategory = feedbackRepository
                .countGroupedByCategory().stream()
                .map(r -> new FeedbackCategoryStats(
                        ((FeedbackCategory) r[0]).name(), ((Number) r[1]).longValue()))
                .toList();

        // Bounded trend aggregation: pull the window's executions and bucket in memory.
        List<AiExecution> window = executionRepository.findAllSince(since);
        Map<LocalDate, long[]> buckets = new TreeMap<>();
        for (AiExecution e : window) {
            LocalDate d = e.getCreatedAt().toLocalDate();
            long[] b = buckets.computeIfAbsent(d, k -> new long[]{0L, 0L, 0L});
            b[0]++;
            if (e.getOutcome() == AiExecutionOutcome.SUCCESS) b[1]++;
            else b[2]++;
        }
        List<DailyExecutionPoint> dailyTrend = buckets.entrySet().stream()
                .map(en -> new DailyExecutionPoint(
                        en.getKey().toString(),
                        en.getValue()[0],
                        en.getValue()[1],
                        en.getValue()[2]))
                .collect(Collectors.toList());

        String note;
        if (total == 0) {
            note = "No AI executions recorded yet.";
        } else {
            note = "These are operational metrics only. They are NOT model accuracy "
                    + "measurements. Accuracy, precision, and recall require labeled "
                    + "evaluation data and are not derivable from execution logs or "
                    + "subjective user feedback.";
        }

        return new AdminAiMonitoringResponse(
                total, successes, failures, successRate,
                totalFeedback,
                avgRating == null ? null : round2(avgRating),
                byTask, byModel, errorsByCategory, feedbackByCategory, dailyTrend,
                note);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}