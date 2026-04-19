package com.repm.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    private String source;

    private String type;

    private boolean isRead = false;

    private String senderUsername;

    private Long parentId;

    private LocalDateTime timestamp = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Long getId() { return id; }

    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

    public String getSource() { return source; }

    public void setSource(String source) { this.source = source; }

    public LocalDateTime getTimestamp() { return timestamp; }

    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }

    public void setRead(boolean read) { isRead = read; }

    public String getSenderUsername() { return senderUsername; }

    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public Long getParentId() { return parentId; }

    public void setParentId(Long parentId) { this.parentId = parentId; }
}
