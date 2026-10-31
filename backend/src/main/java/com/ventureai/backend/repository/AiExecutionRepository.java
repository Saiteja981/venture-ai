package com.ventureai.backend.repository;

import com.ventureai.backend.entity.AiExecution;
import com.ventureai.backend.entity.AiExecutionOutcome;
import com.ventureai.backend.entity.AiTaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AiExecutionRepository extends JpaRepository<AiExecution, Long>,
        JpaSpecificationExecutor<AiExecution> {

    List<AiExecution> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByOutcome(AiExecutionOutcome outcome);

    long countByTask(AiTaskType task);

    long countByTaskAndOutcome(AiTaskType task, AiExecutionOutcome outcome);

    @Query("SELECT AVG(e.latencyMs) FROM AiExecution e " +
            "WHERE e.task = :task AND e.outcome = com.ventureai.backend.entity.AiExecutionOutcome.SUCCESS")
    Double avgLatencyByTask(@Param("task") AiTaskType task);

    @Query("SELECT MAX(e.latencyMs) FROM AiExecution e " +
            "WHERE e.task = :task AND e.outcome = com.ventureai.backend.entity.AiExecutionOutcome.SUCCESS")
    Long maxLatencyByTask(@Param("task") AiTaskType task);

    @Query("SELECT e.modelVersion, COUNT(e) FROM AiExecution e " +
            "WHERE e.modelVersion IS NOT NULL GROUP BY e.modelVersion ORDER BY COUNT(e) DESC")
    List<Object[]> countGroupedByModelVersion();

    @Query("SELECT e.errorCategory, COUNT(e) FROM AiExecution e " +
            "WHERE e.errorCategory IS NOT NULL GROUP BY e.errorCategory ORDER BY COUNT(e) DESC")
    List<Object[]> countGroupedByErrorCategory();

    @Query("SELECT e FROM AiExecution e WHERE e.createdAt >= :since ORDER BY e.createdAt ASC")
    List<AiExecution> findAllSince(@Param("since") LocalDateTime since);
}