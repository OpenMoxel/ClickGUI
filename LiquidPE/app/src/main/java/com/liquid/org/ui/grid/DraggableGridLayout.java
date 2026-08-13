/*
 * LiquidPE 开源作者
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.liquid.org.ui.grid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 可自由编辑的网格容器。子 View 的点击仍由 Android 正常分发；只有长按顶层面板的空白区域后，
 * 容器才开始拦截后续事件并负责拖拽、吸附和碰撞处理。
 */
public class DraggableGridLayout extends ViewGroup {
    public static class LayoutParams extends MarginLayoutParams {
        public PanelLayoutInfo panelInfo;

        public LayoutParams(Context context, AttributeSet attrs) { super(context, attrs); }
        public LayoutParams(int width, int height) { super(width, height); }
        public LayoutParams(ViewGroup.LayoutParams source) { super(source); }
        public LayoutParams(MarginLayoutParams source) { super(source); }
    }

    private final int gapPx;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int touchSlop;
    private final Runnable longPressRunnable = new Runnable() {
        @Override public void run() {
            if (pressedPanel != null && !pointerMoved && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                beginDragging(pressedPanel, downX, downY);
            }
        }
    };

    private final PanelLayoutStorage storage;
    private int columnCount = 4;
    private int cellSizePx;
    private boolean isEditMode;
    private View draggingPanel;
    private View pressedPanel;
    private boolean hasMoved;
    private boolean pointerMoved;
    private int originalGridX;
    private int originalGridY;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private float downX;
    private float downY;
    private float dragOffsetX;
    private float dragOffsetY;
    private int previewGridX = -1;
    private int previewGridY = -1;
    private boolean previewConflicted;
    private boolean pendingAutoRestore = true;

    public DraggableGridLayout(Context context) { this(context, null); }
    public DraggableGridLayout(Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public DraggableGridLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        gapPx = dp(8);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        storage = new PanelLayoutStorage(context);
        setWillNotDraw(false);
        setClipChildren(false);
    }

    /** 默认四列，可按业务需要覆盖；列数变化后仍以逻辑坐标重新布局。 */
    public void setColumnCount(int columns) {
        int valid = Math.max(1, columns);
        if (columnCount == valid) return;
        columnCount = valid;
        requestLayout();
    }

    public int getColumnCount() { return columnCount; }

    public boolean isEditMode() { return isEditMode; }

    public void enterEditMode() {
        isEditMode = true;
        invalidate();
    }

    public void exitEditMode() {
        cancelDragLongPress();
        if (draggingPanel != null) finishDragging(false);
        isEditMode = false;
        saveLayout();
        invalidate();
    }

    /** 添加一个具备稳定 id 的面板。重复 id 会被忽略，避免损坏布局数据。 */
    public boolean addPanel(View panel, String panelId, int spanX, int spanY) {
        if (panel == null || panelId == null || panelId.trim().isEmpty() || findPanelById(panelId) != null) return false;
        LayoutParams params = panel.getLayoutParams() instanceof LayoutParams
                ? (LayoutParams) panel.getLayoutParams() : new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.panelInfo = new PanelLayoutInfo(panelId.trim(), -1, -1, spanX, spanY);
        addView(panel, params);
        return true;
    }

    public void restoreLayout() {
        Map<String, PanelLayoutInfo> saved = storage.load();
        Set<String> occupiedIds = new HashSet<>();
        forEachPanel(new PanelVisitor() {
            @Override public void visit(View child, LayoutParams params) {
                PanelLayoutInfo persisted = saved.get(params.panelInfo.panelId);
                if (persisted != null) {
                    params.panelInfo.gridX = persisted.gridX;
                    params.panelInfo.gridY = persisted.gridY;
                    params.panelInfo.spanX = Math.min(columnCount, Math.max(1, persisted.spanX));
                    params.panelInfo.spanY = Math.max(1, persisted.spanY);
                    params.panelInfo.visible = persisted.visible;
                }
                if (!isValidAndFree(params.panelInfo, null)) placeFirstFree(params.panelInfo, null);
                child.setVisibility(params.panelInfo.visible ? VISIBLE : GONE);
                occupiedIds.add(params.panelInfo.panelId);
            }
        });
        // 保存自动修复后的版本，避免下一次启动再次读取无效数据。
        saveLayout();
        requestLayout();
    }

    public void saveLayout() {
        List<PanelLayoutInfo> infos = new ArrayList<>();
        forEachPanel(new PanelVisitor() {
            @Override public void visit(View child, LayoutParams params) { infos.add(params.panelInfo.copy()); }
        });
        storage.save(infos);
    }

    /** 清除保存结果，按子 View 加入顺序重新放到第一个可用位置。 */
    public void resetLayout() {
        storage.clear();
        forEachPanel(new PanelVisitor() {
            @Override public void visit(View child, LayoutParams params) {
                params.panelInfo.gridX = -1;
                params.panelInfo.gridY = -1;
                params.panelInfo.visible = true;
                child.setVisibility(VISIBLE);
                placeFirstFree(params.panelInfo, null);
            }
        });
        saveLayout();
        requestLayout();
    }

    @Override public void onViewAdded(View child) {
        super.onViewAdded(child);
        if (child.getLayoutParams() instanceof LayoutParams) {
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            if (params.panelInfo != null && pendingAutoRestore) post(new Runnable() {
                @Override public void run() { if (pendingAutoRestore) { pendingAutoRestore = false; restoreLayout(); } }
            });
        }
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int availableWidth = Math.max(1, width - getPaddingLeft() - getPaddingRight() - gapPx * (columnCount - 1));
        cellSizePx = Math.max(dp(48), availableWidth / columnCount);
        final int[] maxRow = {0};
        forEachPanel(new PanelVisitor() {
            @Override public void visit(View child, LayoutParams params) {
                PanelLayoutInfo info = params.panelInfo;
                if (info == null) return;
                info.spanX = Math.min(columnCount, Math.max(1, info.spanX));
                info.spanY = Math.max(1, info.spanY);
                if (!isValidAndFree(info, child)) placeFirstFree(info, child);
                int childWidth = spanToPixels(info.spanX);
                int childHeight = spanToPixels(info.spanY);
                child.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
                if (child.getVisibility() != GONE) maxRow[0] = Math.max(maxRow[0], info.gridY + info.spanY);
            }
        });
        int wantedHeight = getPaddingTop() + getPaddingBottom();
        if (maxRow[0] > 0) wantedHeight += maxRow[0] * cellSizePx + Math.max(0, maxRow[0] - 1) * gapPx;
        int measuredHeight = resolveSize(Math.max(getSuggestedMinimumHeight(), wantedHeight), heightMeasureSpec);
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), measuredHeight);
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        forEachPanel(new PanelVisitor() {
            @Override public void visit(View child, LayoutParams params) {
                if (child.getVisibility() == GONE || params.panelInfo == null) return;
                int childLeft = gridToPixelX(params.panelInfo.gridX);
                int childTop = gridToPixelY(params.panelInfo.gridY);
                child.layout(childLeft, childTop, childLeft + child.getMeasuredWidth(), childTop + child.getMeasuredHeight());
            }
        });
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activePointerId = event.getPointerId(0);
                downX = event.getX(); downY = event.getY(); pointerMoved = false; hasMoved = false;
                pressedPanel = findDirectPanelUnder(downX, downY);
                if (pressedPanel != null && !hasClickableDescendantAt(pressedPanel, downX, downY)) {
                    handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
                } else pressedPanel = null;
                return false;
            case MotionEvent.ACTION_MOVE:
                if (activePointerId == MotionEvent.INVALID_POINTER_ID) return false;
                int index = event.findPointerIndex(activePointerId);
                if (index < 0) return false;
                if (Math.hypot(event.getX(index) - downX, event.getY(index) - downY) > touchSlop) {
                    pointerMoved = true;
                    cancelDragLongPress();
                }
                return draggingPanel != null;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelDragLongPress();
                return draggingPanel != null;
            case MotionEvent.ACTION_POINTER_UP:
                int pointerIndex = event.getActionIndex();
                if (event.getPointerId(pointerIndex) == activePointerId) {
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    cancelDragLongPress();
                }
                return draggingPanel != null;
            default:
                return draggingPanel != null;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (draggingPanel == null) return true;
        int index = event.findPointerIndex(activePointerId);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                if (index >= 0) updateDragging(event.getX(index), event.getY(index));
                return true;
            case MotionEvent.ACTION_UP:
                if (index >= 0) updateDragging(event.getX(index), event.getY(index));
                finishDragging(true);
                return true;
            case MotionEvent.ACTION_CANCEL:
                finishDragging(false);
                return true;
            default:
                return true;
        }
    }

    private void beginDragging(View panel, float x, float y) {
        LayoutParams params = panel == null ? null : (LayoutParams) panel.getLayoutParams();
        if (params == null || params.panelInfo == null) return;
        draggingPanel = panel;
        originalGridX = params.panelInfo.gridX;
        originalGridY = params.panelInfo.gridY;
        dragOffsetX = x - panel.getLeft() - panel.getTranslationX();
        dragOffsetY = y - panel.getTop() - panel.getTranslationY();
        panel.animate().cancel();
        panel.animate().scaleX(1.04f).scaleY(1.04f).alpha(.90f).setDuration(120).start();
        panel.setElevation(dp(12));
        requestDisallowInterceptTouchEvent(true);
        updateDragging(x, y);
        invalidate();
    }

    private void updateDragging(float x, float y) {
        if (draggingPanel == null) return;
        float desiredLeft = clamp(x - dragOffsetX, getPaddingLeft(), getWidth() - getPaddingRight() - draggingPanel.getWidth());
        float desiredTop = clamp(y - dragOffsetY, getPaddingTop(), getHeight() - getPaddingBottom() - draggingPanel.getHeight());
        draggingPanel.setTranslationX(desiredLeft - draggingPanel.getLeft());
        draggingPanel.setTranslationY(desiredTop - draggingPanel.getTop());
        LayoutParams params = (LayoutParams) draggingPanel.getLayoutParams();
        int rawX = pixelToGridX(desiredLeft);
        int rawY = pixelToGridY(desiredTop);
        rawX = Math.max(0, Math.min(columnCount - params.panelInfo.spanX, rawX));
        rawY = Math.max(0, rawY);
        previewConflicted = isOccupied(rawX, rawY, params.panelInfo.spanX, params.panelInfo.spanY, draggingPanel);
        PanelLayoutInfo resolved = findNearestFree(rawX, rawY, params.panelInfo.spanX, params.panelInfo.spanY, draggingPanel);
        previewGridX = resolved == null ? originalGridX : resolved.gridX;
        previewGridY = resolved == null ? originalGridY : resolved.gridY;
        hasMoved = true;
        invalidate();
    }

    private void finishDragging(boolean commit) {
        if (draggingPanel == null) return;
        final View panel = draggingPanel;
        LayoutParams params = (LayoutParams) panel.getLayoutParams();
        if (commit && previewGridX >= 0 && previewGridY >= 0) {
            params.panelInfo.gridX = previewGridX;
            params.panelInfo.gridY = previewGridY;
        } else {
            params.panelInfo.gridX = originalGridX;
            params.panelInfo.gridY = originalGridY;
        }
        final float previousX = panel.getLeft() + panel.getTranslationX();
        final float previousY = panel.getTop() + panel.getTranslationY();
        panel.setTranslationX(0); panel.setTranslationY(0);
        panel.animate().cancel();
        requestLayout();
        panel.post(new Runnable() {
            @Override public void run() {
                panel.setTranslationX(previousX - panel.getLeft());
                panel.setTranslationY(previousY - panel.getTop());
                panel.animate().translationX(0).translationY(0).scaleX(1).scaleY(1).alpha(1)
                        .setDuration(180).setListener(new AnimatorListenerAdapter() {
                            @Override public void onAnimationEnd(Animator animation) { panel.setElevation(0); }
                        }).start();
            }
        });
        draggingPanel = null;
        pressedPanel = null;
        activePointerId = MotionEvent.INVALID_POINTER_ID;
        previewGridX = previewGridY = -1;
        requestDisallowInterceptTouchEvent(false);
        saveLayout();
        invalidate();
    }

    private PanelLayoutInfo findNearestFree(int targetX, int targetY, int spanX, int spanY, View ignored) {
        int maxRows = Math.max(1, Math.max(occupiedBottomRow(ignored) + spanY + 1, targetY + spanY + 1));
        PanelLayoutInfo best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int y = 0; y <= maxRows; y++) for (int x = 0; x <= columnCount - spanX; x++) {
            if (isOccupied(x, y, spanX, spanY, ignored)) continue;
            int distance = Math.abs(x - targetX) + Math.abs(y - targetY);
            if (distance < bestDistance) { best = new PanelLayoutInfo("", x, y, spanX, spanY); bestDistance = distance; }
        }
        return best;
    }

    private void placeFirstFree(PanelLayoutInfo info, View ignored) {
        PanelLayoutInfo free = findNearestFree(0, 0, info.spanX, info.spanY, ignored);
        if (free != null) { info.gridX = free.gridX; info.gridY = free.gridY; }
    }

    private boolean isValidAndFree(PanelLayoutInfo info, View ignored) {
        return info != null && info.gridX >= 0 && info.gridY >= 0 && info.gridX + info.spanX <= columnCount
                && !isOccupied(info.gridX, info.gridY, info.spanX, info.spanY, ignored);
    }

    /** 独立碰撞检测：两个网格矩形只要有交集就不能共存。 */
    private boolean isOccupied(int x, int y, int spanX, int spanY, View ignored) {
        final boolean[] occupied = {false};
        forEachPanel(new PanelVisitor() {
            @Override public void visit(View child, LayoutParams params) {
                if (occupied[0] || child == ignored || child.getVisibility() == GONE || params.panelInfo == null) return;
                PanelLayoutInfo other = params.panelInfo;
                if (x < other.gridX + other.spanX && x + spanX > other.gridX
                        && y < other.gridY + other.spanY && y + spanY > other.gridY) occupied[0] = true;
            }
        });
        return occupied[0];
    }

    private int occupiedBottomRow(View ignored) {
        final int[] result = {0};
        forEachPanel(new PanelVisitor() {
            @Override public void visit(View child, LayoutParams params) {
                if (child != ignored && child.getVisibility() != GONE && params.panelInfo != null) {
                    result[0] = Math.max(result[0], params.panelInfo.gridY + params.panelInfo.spanY);
                }
            }
        });
        return result[0];
    }

    private View findPanelById(String id) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getLayoutParams() instanceof LayoutParams) {
                PanelLayoutInfo info = ((LayoutParams) child.getLayoutParams()).panelInfo;
                if (info != null && id.equals(info.panelId)) return child;
            }
        }
        return null;
    }

    private View findDirectPanelUnder(float x, float y) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE && x >= child.getLeft() && x <= child.getRight()
                    && y >= child.getTop() && y <= child.getBottom() && child.getLayoutParams() instanceof LayoutParams) return child;
        }
        return null;
    }

    private boolean hasClickableDescendantAt(View root, float x, float y) {
        if (!(root instanceof ViewGroup)) return false;
        ViewGroup group = (ViewGroup) root;
        for (int i = group.getChildCount() - 1; i >= 0; i--) {
            View child = group.getChildAt(i);
            float childX = x - root.getLeft() - child.getLeft();
            float childY = y - root.getTop() - child.getTop();
            if (childX < 0 || childX > child.getWidth() || childY < 0 || childY > child.getHeight()) continue;
            if (child.isClickable() || child.isLongClickable()) return true;
            if (hasClickableDescendantAt(child, x - root.getLeft(), y - root.getTop())) return true;
        }
        return false;
    }

    private interface PanelVisitor { void visit(View child, LayoutParams params); }
    private void forEachPanel(PanelVisitor visitor) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getLayoutParams() instanceof LayoutParams) visitor.visit(child, (LayoutParams) child.getLayoutParams());
        }
    }
    private int spanToPixels(int span) { return span * cellSizePx + Math.max(0, span - 1) * gapPx; }
    private int gridToPixelX(int x) { return getPaddingLeft() + x * (cellSizePx + gapPx); }
    private int gridToPixelY(int y) { return getPaddingTop() + y * (cellSizePx + gapPx); }
    private int pixelToGridX(float x) { return Math.round((x - getPaddingLeft()) / Math.max(1f, cellSizePx + gapPx)); }
    private int pixelToGridY(float y) { return Math.round((y - getPaddingTop()) / Math.max(1f, cellSizePx + gapPx)); }
    private float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private void cancelDragLongPress() { handler.removeCallbacks(longPressRunnable); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected boolean checkLayoutParams(ViewGroup.LayoutParams p) { return p instanceof LayoutParams; }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT); }
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }
    @Override protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) { return p instanceof MarginLayoutParams ? new LayoutParams((MarginLayoutParams) p) : new LayoutParams(p); }
}
