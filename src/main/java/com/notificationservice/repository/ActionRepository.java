package com.notificationservice.repository;

import com.notificationservice.model.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActionRepository extends JpaRepository<Action, Long> {

    List<Action> findByUserIdAndActionTypeAndTimestampAfterOrderByTimestampDesc(
            String userId, String actionType, Instant timestamp);

    List<Action> findByUserIdAndTimestampAfterOrderByTimestampDesc(
            String userId, Instant timestamp);

    @Query("SELECT COUNT(a) FROM Action a WHERE a.userId = :userId AND a.actionType = :actionType AND a.timestamp BETWEEN :start AND :end")
    long countByUserAndActionTypeAndTimeWindow(
            @Param("userId") String userId,
            @Param("actionType") String actionType,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT a FROM Action a WHERE a.userId = :userId AND a.actionType = :actionType AND a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    List<Action> findActionsInTimeWindow(
            @Param("userId") String userId,
            @Param("actionType") String actionType,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT COUNT(a) FROM Action a WHERE a.userId = :userId AND a.actionType = :actionType " +
            "AND DATE(a.timestamp) = :date")
    long countByUserAndActionTypeAndDate(
            @Param("userId") String userId,
            @Param("actionType") String actionType,
            @Param("date") LocalDateTime date);

    @Query(value = "SELECT COUNT(a) FROM notification_service.actions a WHERE a.user_id = :userId AND a.action_type = :actionType " +
            "AND a.timestamp BETWEEN :start AND :end " +
            "AND EXTRACT(DOW FROM a.timestamp) IN (0, 6)", nativeQuery = true)
    // 0=Sunday, 6=Saturday
    long countWeekendActions(
            @Param("userId") String userId,
            @Param("actionType") String actionType,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query(value = "SELECT COUNT(a) FROM notification_service.actions a WHERE a.user_id = :userId AND a.action_type = :actionType " +
            "AND a.timestamp BETWEEN :start AND :end " +
            "AND EXTRACT(DOW FROM a.timestamp) BETWEEN 1 AND 5", nativeQuery = true)
        // 1=Monday to 5=Friday
    long countWeekdayActions(
            @Param("userId") String userId,
            @Param("actionType") String actionType,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT COUNT(a) FROM Action a WHERE a.userId = :userId " +
            "AND DATE(a.timestamp) = :date")
    long countByUserAndActionTypeAndDate(
            @Param("userId") String userId,
            @Param("date") LocalDateTime date);
}