/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.notification;

public enum NotificationType {
    /** Enabled: green → blue */
    SUCCESS((int) 0xFF00FF88L, (int) 0xFF00BFFFL),
    /** Disabled: orange → dark */
    WARNING((int) 0xFFFF7A00L, (int) 0xFF101010L),
    INFO((int) 0xFF00BFFFL, (int) 0xFF4F7DB2L),
    ERROR((int) 0xFFFF5555L, (int) 0xFF101010L);

    private final int gradientStart;
    private final int gradientEnd;

    NotificationType(int gradientStart, int gradientEnd) {
        this.gradientStart = gradientStart;
        this.gradientEnd = gradientEnd;
    }

    public int getGradientStart() {
        return gradientStart;
    }

    public int getGradientEnd() {
        return gradientEnd;
    }
}
