/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationQueue {

    private final int maxSize;
    private final List<NotificationData> items = new ArrayList<>();
    private long idSeq = 0L;

    public NotificationQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    public NotificationQueue() {
        this(5);
    }

    public NotificationData enqueue(String title, String message, NotificationType type) {
        NotificationData data = new NotificationData(idSeq++, title, message, type);
        items.add(data);
        if (items.size() > maxSize) items.remove(0);
        return data;
    }

    public void remove(NotificationData data) {
        items.remove(data);
    }

    public NotificationData peek() {
        return items.isEmpty() ? null : items.get(0);
    }

    public int size() {
        return items.size();
    }
}
