package com.notificationservice.repository;

import com.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByTimestampDesc();

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.triggerId = :triggerId AND n.timestamp > :since")
    List<Notification> findRecentByUserAndTrigger(
            @Param("userId") String userId,
            @Param("triggerId") String triggerId,
            @Param("since") Instant since);
}