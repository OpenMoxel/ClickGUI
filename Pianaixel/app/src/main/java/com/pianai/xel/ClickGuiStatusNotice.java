/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */
package com.pianai.xel;

/** Runtime data for one queued enable/disable status notice. */
final class ClickGuiStatusNotice {

    final boolean enabled;
    final long createdAtMs;
    float displayedY = Float.NaN;
    long lastFrameAtMs;

    ClickGuiStatusNotice(boolean enabled, long createdAtMs) {
        this.enabled = enabled;
        this.createdAtMs = createdAtMs;
    }

    float resolveY(float targetY, long nowMs) {
        if (Float.isNaN(displayedY)) {
            displayedY = targetY;
        } else {
            long elapsedMs = Math.max(0L, nowMs - lastFrameAtMs);
            float movement = 1f - (float) Math.exp(-elapsedMs / 105f);
            displayedY += (targetY - displayedY) * movement;
        }
        lastFrameAtMs = nowMs;
        return displayedY;
    }
}
