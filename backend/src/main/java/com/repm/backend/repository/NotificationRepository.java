package com.repm.backend.repository;

import com.repm.backend.entity.Notification;
import com.repm.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser(User user);
    List<Notification> findByUserAndType(User user, String type);
    List<Notification> findByType(String type);
    List<Notification> findBySenderUsernameAndType(String senderUsername, String type);
    List<Notification> findByUserAndTypeOrderByTimestampDesc(User user, String type);
}
