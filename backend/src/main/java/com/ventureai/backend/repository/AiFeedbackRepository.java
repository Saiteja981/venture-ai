package com.ventureai.backend.repository;

import com.ventureai.backend.entity.AiFeedback;
import com.ventureai.backend.entity.AiTaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    boolean existsByExecutionIdAndUserId(Long executionId, Long userId);

    Optional<AiFeedback> findByExecutionIdAndUserId(Long executionId, Long userId);

    @Query("SELECT AVG(f.rating) FROM AiFeedback f")
    Double averageRating();

    @Query("SELECT AVG(f.rating) FROM AiFeedback f WHERE f.execution.task = :task")
    Double averageRatingByTask(@Param("task") AiTaskType task);

    @Query("SELECT f.category, COUNT(f) FROM AiFeedback f " +
            "GROUP BY f.category ORDER BY COUNT(f) DESC")
    List<Object[]> countGroupedByCategory();
}