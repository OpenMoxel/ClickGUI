/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.notification;

import java.util.Objects;

public final class NotificationData {
    private final long id;
    private final String title;
    private final String message;
    private final NotificationType type;
    private final long createdAt;

    public NotificationData(long id, String title, String message, NotificationType type, long createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.createdAt = createdAt;
    }

    public NotificationData(long id, String title, String message, NotificationType type) {
        this(id, title, message, type, System.currentTimeMillis());
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationData)) return false;
        NotificationData other = (NotificationData) o;
        return id == other.id
                && title.equals(other.title)
                && message.equals(other.message)
                && type == other.type
                && createdAt == other.createdAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, message, type, createdAt);
    }

    @Override
    public String toString() {
        return "NotificationData(id=" + id + ", title=" + title + ", message=" + message
                + ", type=" + type + ", createdAt=" + createdAt + ")";
    }
}
