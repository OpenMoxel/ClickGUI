package com.pixelsight.gui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import java.util.ArrayList;
import java.util.List;

public class SolsticeGuiView extends View {

    public static SolsticeGuiView instance;

    private Paint paint, textPaint, iconPaint;
    public boolean isVisible = false;
    private float globalAlpha = 0f;
    private float scaleAnim = 0f;
    private float pivotX = 0f, pivotY = 0f;

    private List<CategoryWindow> windows = new ArrayList<>();
    private CategoryWindow draggingWindow = null;
    private float touchDx, touchDy;
    private boolean layoutInitialized = false;
    
    private ClickGuiView.Setting draggingSetting = null;
    private float initialTouchX = 0f, initialTouchY = 0f, lastTouchY = 0f;
    private boolean isScrolling = false;
    private boolean hasLongPressed = false;
    private int touchSlop;
    private CategoryWindow scrollingWindow = null;

    private final String[] CAT_EN = {"Combat", "Movement", "World", "Player", "Misc", "Render"};
    private final String[] CAT_ZH = {"战斗", "移动", "世界", "玩家", "杂项", "渲染"};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ClickGuiView.Module longPressedModule = null;
    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (longPressedModule != null && !isScrolling && draggingSetting == null) {
                hasLongPressed = true; 
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                longPressedModule.showShortcut = !longPressedModule.showShortcut;
                if (ClickGuiView.onModuleToggled != null) ClickGuiView.onModuleToggled.run();
                invalidate();
            }
        }
    };

    public SolsticeGuiView(Context context) {
        super(context);
        instance = this;
        init();
    }

    private void init() {
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG); textPaint = new Paint(Paint.ANTI_ALIAS_FLAG); iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setStyle(Paint.Style.STROKE); iconPaint.setStrokeWidth(4f); iconPaint.setStrokeCap(Paint.Cap.ROUND); iconPaint.setStrokeJoin(Paint.Join.ROUND);
        
        setAlpha(0f); setVisibility(View.GONE);

        windows.add(new CategoryWindow(-1)); 
        windows.add(new CategoryWindow(0));
        windows.add(new CategoryWindow(1));
        windows.add(new CategoryWindow(2));
        windows.add(new CategoryWindow(3));
        windows.add(new CategoryWindow(4));
        windows.add(new CategoryWindow(5));
    }

    public void toggle(float x, float y) {
        if (isVisible) closeGui(); else openGui(x, y);
    }

    public void openGui(float x, float y) {
        draggingSetting = null; isScrolling = false; draggingWindow = null; hasLongPressed = false;
        isVisible = true; setVisibility(View.VISIBLE); 
        if (x != 0 && y != 0) { pivotX = x; pivotY = y; } 
        else { pivotX = getWidth()/2f; pivotY = getHeight()/2f; }
        
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(350); va.setInterpolator(new OvershootInterpolator(1.1f));
        va.addUpdateListener(a -> {
            scaleAnim = (float) a.getAnimatedValue();
            globalAlpha = Math.min(1f, scaleAnim);
            setAlpha(Math.max(0f, globalAlpha));
            invalidate();
        });
        va.start();
    }

    public void closeGui() {
        isVisible = false; draggingSetting = null; isScrolling = false; draggingWindow = null;
        ValueAnimator va = ValueAnimator.ofFloat(1f, 0f);
        va.setDuration(250); va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(a -> {
            scaleAnim = (float) a.getAnimatedValue();
            globalAlpha = scaleAnim;
            setAlpha(Math.max(0f, Math.min(1f, globalAlpha)));
            invalidate();
        });
        va.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) { setVisibility(View.GONE); }
        });
        va.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (!layoutInitialized && getWidth() > 0) {
            float startX = 30f; float startY = 30f;
            for (CategoryWindow w : windows) {
                w.x = startX; w.y = startY;
                startX += 380f;
                if (startX + 350f > getWidth()) { startX = 30f; startY += 80f; }
            }
            layoutInitialized = true;
        }

        if (ClickGuiView.allModules == null || globalAlpha <= 0.01f) return;

        canvas.save();
        canvas.scale(scaleAnim, scaleAnim, pivotX, pivotY);

        boolean isLight = ClickGuiView.isLightTheme;

        for (CategoryWindow w : windows) {
            float wWidth = 350f;
            float targetBodyH = 0f;
            
            if (w.categoryIndex == -1) {
                targetBodyH = w.isExpanded ? 430f : 0f; 
            } else if (w.isExpanded) {
                for (ClickGuiView.Module m : ClickGuiView.allModules) {
                    if (m.categoryIndex == w.categoryIndex) {
                        targetBodyH += 65f; 
                        if (m.isDrawerExpanded) targetBodyH += m.drawerHeightMax; 
                    }
                }
            }
            w.currentBodyH += (targetBodyH - w.currentBodyH) * 0.2f;
            if (Math.abs(w.currentBodyH - targetBodyH) > 0.5f) postInvalidateOnAnimation();
            
            float maxVisibleH = getHeight() - w.y - 60f;
            float visibleH = Math.min(w.currentBodyH, maxVisibleH);
            w.maxScrollY = w.currentBodyH > visibleH ? -(w.currentBodyH - visibleH) : 0f;

            w.headerRect.set(w.x, w.y, w.x + wWidth, w.y + 60f);
            w.bodyRect.set(w.x, w.y + 60f, w.x + wWidth, w.y + 60f + visibleH);

            if (visibleH > 1f) {
                paint.setColor(isLight ? Color.parseColor("#E6FFFFFF") : Color.parseColor("#D91D1D21"));
                paint.setShadowLayer(15f, 0, 10f, Color.parseColor("#30000000"));
                canvas.drawRoundRect(w.x, w.y + 20f, w.x + wWidth, w.bodyRect.bottom, 15f, 15f, paint); paint.clearShadowLayer();
                
                // 核心修复：在此处 clipRect 严格限制绘制边界，防止展开时文字飞出框外
                canvas.save(); 
                canvas.clipRect(w.x, w.y + 60f, w.x + wWidth, w.bodyRect.bottom); 
                canvas.translate(0, w.scrollY);
                
                float mY = w.y + 60f;
                
                if (w.categoryIndex == -1) {
                    float hubY = mY + 20f;
                    textPaint.setColor(isLight ? Color.parseColor("#55555A") : Color.parseColor("#A0A0A5")); textPaint.setTextSize(20f); textPaint.setTypeface(Typeface.DEFAULT);
                    canvas.drawText(ClickGuiView.isEnglish ? "GUI Framework" : "GUI 框架", w.x + 20f, hubY, textPaint);
                    drawHubBtn(canvas, new RectF(w.x + 20f, hubY + 15f, w.x + wWidth/2f - 5f, hubY + 60f), "Aui", ClickGuiView.guiMode == 0);
                    drawHubBtn(canvas, new RectF(w.x + wWidth/2f + 5f, hubY + 15f, w.x + wWidth - 20f, hubY + 60f), "Bui", ClickGuiView.guiMode == 1);
                    
                    hubY += 85f;
                    canvas.drawText(ClickGuiView.isEnglish ? "Theme Engine" : "主题引擎", w.x + 20f, hubY, textPaint);
                    drawHubBtn(canvas, new RectF(w.x + 20f, hubY + 15f, w.x + wWidth/2f - 5f, hubY + 60f), ClickGuiView.isEnglish ? "Dark" : "暗黑", !ClickGuiView.isLightTheme);
                    drawHubBtn(canvas, new RectF(w.x + wWidth/2f + 5f, hubY + 15f, w.x + wWidth - 20f, hubY + 60f), ClickGuiView.isEnglish ? "Light" : "明亮", ClickGuiView.isLightTheme);

                    hubY += 85f;
                    canvas.drawText(ClickGuiView.isEnglish ? "Shortcut Size" : "快捷键尺寸", w.x + 20f, hubY, textPaint);
                    float btnW = (wWidth - 50f) / 3f;
                    drawHubBtn(canvas, new RectF(w.x + 20f, hubY + 15f, w.x + 20f + btnW, hubY + 60f), "S", ClickGuiView.shortcutSize == 0);
                    drawHubBtn(canvas, new RectF(w.x + 25f + btnW, hubY + 15f, w.x + 25f + btnW*2f, hubY + 60f), "M", ClickGuiView.shortcutSize == 1);
                    drawHubBtn(canvas, new RectF(w.x + 30f + btnW*2f, hubY + 15f, w.x + wWidth - 20f, hubY + 60f), "L", ClickGuiView.shortcutSize == 2);
                    
                    hubY += 85f;
                    canvas.drawText(ClickGuiView.isEnglish ? "Language" : "界面语言", w.x + 20f, hubY, textPaint);
                    drawHubBtn(canvas, new RectF(w.x + 20f, hubY + 15f, w.x + wWidth/2f - 5f, hubY + 60f), "English", ClickGuiView.isEnglish);
                    drawHubBtn(canvas, new RectF(w.x + wWidth/2f + 5f, hubY + 15f, w.x + wWidth - 20f, hubY + 60f), "中文", !ClickGuiView.isEnglish);
                    
                } else {
                    for (ClickGuiView.Module m : ClickGuiView.allModules) {
                        if (m.categoryIndex == w.categoryIndex) {
                            float mH = 65f;
                            
                            // 核心修复点不到：将卡片的热区高度扩大，包含已经展开的抽屉！
                            float fullH = mH + (m.isDrawerExpanded ? m.currentDrawerAnim : 0f);
                            m.cardHitbox.set(w.x, mY + w.scrollY, w.x + wWidth, mY + fullH + w.scrollY);
                            
                            if (m.animProgress > 0) {
                                int color = Color.parseColor("#4A80F6");
                                int alphaColor = Color.argb((int)(40 * m.animProgress), Color.red(color), Color.green(color), Color.blue(color));
                                paint.setColor(alphaColor);
                                canvas.drawRoundRect(new RectF(w.x + 10f, mY + 5f, w.x + wWidth - 10f, mY + mH - 5f), 12f, 12f, paint);
                                
                                paint.setColor(Color.parseColor("#4A80F6")); paint.setAlpha((int)(255 * m.animProgress));
                                canvas.drawRoundRect(new RectF(w.x + 10f, mY + 15f, w.x + 16f, mY + mH - 15f), 3f, 3f, paint);
                            }
                            
                            textPaint.setColor(m.animProgress > 0.5f ? Color.parseColor("#4A80F6") : (isLight ? Color.parseColor("#1C1C1E") : Color.WHITE));
                            textPaint.setTextSize(26f); textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                            canvas.drawText(ClickGuiView.isEnglish ? m.nameEn : m.nameZh, w.x + 35f, mY + 40f, textPaint);

                            if (m.hasSettings) {
                                float sW = 60f, sH = 65f;
                                float sRight = w.x + wWidth - 10f, sTop = mY;
                                m.switchHitbox.set(sRight - sW, sTop + w.scrollY, sRight, sTop + sH + w.scrollY);
                                
                                iconPaint.setColor(m.animProgress > 0 ? Color.parseColor("#4A80F6") : (isLight ? Color.parseColor("#A0A0A5") : Color.parseColor("#808084"))); 
                                iconPaint.setStyle(Paint.Style.FILL);
                                float cx = sRight - 30f; float cy = sTop + 32.5f;
                                
                                // 三点图标带有基于展开进度的90度丝滑旋转
                                float progress = m.currentDrawerAnim / (m.drawerHeightMax > 0 ? m.drawerHeightMax : 1f);
                                canvas.save(); canvas.rotate(90f * progress, cx, cy);
                                canvas.drawCircle(cx - 12f, cy, 4f, iconPaint);
                                canvas.drawCircle(cx, cy, 4f, iconPaint);
                                canvas.drawCircle(cx + 12f, cy, 4f, iconPaint);
                                canvas.restore();
                                iconPaint.setStyle(Paint.Style.STROKE);
                            } else {
                                m.switchHitbox.set(0,0,0,0);
                            }

                            mY += mH;

                            if (m.isDrawerExpanded && m.currentDrawerAnim > 10f) {
                                float setW = wWidth; float cY = mY;
                                for (ClickGuiView.Setting s : m.settings) {
                                    if (s instanceof ClickGuiView.Slider || s instanceof ClickGuiView.ColorPicker) {
                                        float sliderY = cY + 15f; float sEndX = w.x + setW - 20f;
                                        s.hitbox.set(w.x + 20f, sliderY - 20f + w.scrollY, sEndX + 15f, sliderY + 30f + w.scrollY);

                                        textPaint.setColor(isLight ? Color.parseColor("#55555A") : Color.parseColor("#A0A0A5"));
                                        textPaint.setTextSize(20f); textPaint.setTypeface(Typeface.DEFAULT);
                                        canvas.drawText(ClickGuiView.isEnglish ? s.nameEn : s.nameZh, w.x + 20f, sliderY, textPaint);

                                        float trackTop = sliderY + 8f; float trackBottom = sliderY + 20f; 
                                        float valBoxWidth = 55f; float trackEndX = sEndX - valBoxWidth - 10f;
                                        
                                        if (s instanceof ClickGuiView.ColorPicker) {
                                            ClickGuiView.ColorPicker cp = (ClickGuiView.ColorPicker) s;
                                            paint.setColor(isLight ? Color.parseColor("#E5E5EA") : Color.parseColor("#1B1B1E"));
                                            canvas.drawRoundRect(new RectF(sEndX - valBoxWidth, sliderY-5f, sEndX, sliderY + 25f), 6f, 6f, paint);
                                            String valStr = String.valueOf((int)cp.hue);
                                            textPaint.setColor(isLight ? Color.parseColor("#1C1C1E") : Color.WHITE); textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                                            canvas.drawText(valStr, sEndX - valBoxWidth/2f - textPaint.measureText(valStr)/2f, sliderY + 15f, textPaint);

                                            LinearGradient lg = new LinearGradient(w.x+20f, trackTop, trackEndX, trackTop, new int[]{Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED}, null, Shader.TileMode.CLAMP);
                                            paint.setShader(lg); paint.setStrokeWidth(12f); paint.setStrokeCap(Paint.Cap.ROUND);
                                            canvas.drawLine(w.x+26f, trackTop+6f, trackEndX-6f, trackTop+6f, paint); paint.setShader(null);
                                            
                                            float pX = w.x+20f + ((trackEndX - (w.x+20f)) * (cp.hue / 360f));
                                            paint.setColor(Color.WHITE); paint.setShadowLayer(4f, 0, 2f, Color.parseColor("#80000000"));
                                            canvas.drawRoundRect(new RectF(pX - 3f, trackTop - 3f, pX + 3f, trackBottom + 3f), 3f, 3f, paint); paint.clearShadowLayer();
                                        } else {
                                            ClickGuiView.Slider sl = (ClickGuiView.Slider) s;
                                            paint.setColor(isLight ? Color.parseColor("#E5E5EA") : Color.parseColor("#2A2B30"));
                                            canvas.drawRoundRect(new RectF(sEndX - valBoxWidth, sliderY-5f, sEndX, sliderY + 25f), 6f, 6f, paint);
                                            String valStr = String.valueOf(sl.val);
                                            textPaint.setColor(isLight ? Color.parseColor("#1C1C1E") : Color.WHITE); textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                                            canvas.drawText(valStr, sEndX - valBoxWidth/2f - textPaint.measureText(valStr)/2f, sliderY + 15f, textPaint);

                                            paint.setColor(isLight ? Color.parseColor("#E5E5EA") : Color.parseColor("#1B1B1E")); 
                                            canvas.drawRoundRect(new RectF(w.x+20f, trackTop, trackEndX, trackBottom), 6f, 6f, paint);
                                            
                                            float pX = w.x+20f + ((trackEndX - (w.x+20f)) * (sl.val / (float)sl.max));
                                            if (pX > w.x+20f) { paint.setColor(Color.parseColor("#4A80F6")); canvas.drawRoundRect(new RectF(w.x+20f, trackTop, pX, trackBottom), 6f, 6f, paint); }
                                            paint.setColor(Color.parseColor("#F5F5F7")); paint.setShadowLayer(4f, 0, 2f, Color.parseColor("#60000000"));
                                            canvas.drawRoundRect(new RectF(pX - 3f, trackTop - 2f, pX + 3f, trackBottom + 2f), 3f, 3f, paint); paint.clearShadowLayer();
                                        }
                                        cY += 55f;
                                    } else if (s instanceof ClickGuiView.Mode) {
                                        ClickGuiView.Mode md = (ClickGuiView.Mode) s;
                                        float modeY = cY + 15f; 
                                        textPaint.setColor(isLight ? Color.parseColor("#55555A") : Color.parseColor("#A0A0A5")); textPaint.setTextSize(20f); textPaint.setTypeface(Typeface.DEFAULT);
                                        canvas.drawText(ClickGuiView.isEnglish ? md.nameEn : md.nameZh, w.x + 20f, modeY, textPaint);

                                        float bX = w.x + 20f; float btnY = modeY + 10f;
                                        int cols = 2; float btnW = (setW - 40f - 10f) / 2f;
                                        for (int i = 0; i < md.modesEn.length; i++) {
                                            int row = i / cols; int col = i % cols;
                                            float rX = bX + col * (btnW + 10f); float rY = btnY + row * 55f;
                                            md.modeRects[i].set(rX, rY + w.scrollY, rX + btnW, rY + 45f + w.scrollY);
                                            
                                            paint.setColor(i == md.current ? Color.parseColor("#4A80F6") : (isLight ? Color.WHITE : Color.parseColor("#1B1B1E"))); 
                                            if(isLight && i != md.current) paint.setShadowLayer(5f,0,2f,Color.parseColor("#15000000"));
                                            canvas.drawRoundRect(new RectF(rX, rY, rX + btnW, rY + 45f), 12f, 12f, paint); paint.clearShadowLayer();
                                            
                                            textPaint.setColor(i == md.current ? Color.WHITE : (isLight ? Color.parseColor("#55555A") : Color.parseColor("#A0A0A5"))); 
                                            textPaint.setTextAlign(Paint.Align.CENTER); 
                                            // 绝对物理居中修复
                                            Paint.FontMetrics fm = textPaint.getFontMetrics();
                                            canvas.drawText(ClickGuiView.isEnglish ? md.modesEn[i] : md.modesZh[i], rX + btnW / 2f, rY + 22.5f - (fm.descent+fm.ascent)/2f, textPaint); textPaint.setTextAlign(Paint.Align.LEFT);
                                        }
                                        int rows = (md.modesEn.length - 1) / cols + 1;
                                        cY += 35f + rows * 55f;
                                    }
                                }
                                mY += m.currentDrawerAnim;
                            }
                        }
                    }
                }
                canvas.restore();
            }

            paint.setColor(isLight ? Color.parseColor("#F5F5F7") : Color.parseColor("#1B1B1E"));
            canvas.drawRoundRect(w.headerRect, 15f, 15f, paint);

            textPaint.setColor(isLight ? Color.parseColor("#1C1C1E") : Color.WHITE);
            textPaint.setTextSize(30f); textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(ClickGuiView.isEnglish ? w.nameEn : w.nameZh, w.x + 20f, w.y + 40f, textPaint);

            iconPaint.setColor(isLight ? Color.parseColor("#1C1C1E") : Color.WHITE);
            float cx = w.x + wWidth - 25f; float cy = w.y + 30f;
            if (w.isExpanded) { 
                canvas.drawLine(cx - 8f, cy, cx + 8f, cy, iconPaint); // 减号
            } else { 
                canvas.drawLine(cx - 8f, cy, cx + 8f, cy, iconPaint); 
                canvas.drawLine(cx, cy - 8f, cx, cy + 8f, iconPaint); // 加号
            }
        }
        
        canvas.restore();
    }

    private void drawHubBtn(Canvas canvas, RectF r, String t, boolean isActive) {
        paint.setColor(isActive ? Color.parseColor("#4A80F6") : (ClickGuiView.isLightTheme ? Color.WHITE : Color.parseColor("#1B1B1E"))); 
        if(ClickGuiView.isLightTheme && !isActive) paint.setShadowLayer(5f,0,2f,Color.parseColor("#15000000"));
        canvas.drawRoundRect(r, 8f, 8f, paint); paint.clearShadowLayer();
        textPaint.setColor(isActive ? Color.WHITE : (ClickGuiView.isLightTheme ? Color.parseColor("#55555A") : Color.parseColor("#A0A0A5"))); 
        textPaint.setTextAlign(Paint.Align.CENTER); Paint.FontMetrics fm = textPaint.getFontMetrics();
        canvas.drawText(t, r.centerX(), r.centerY() - (fm.descent+fm.ascent)/2f, textPaint); textPaint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isVisible || ClickGuiView.allModules == null) return false;
        float tx = event.getX(), ty = event.getY();
        float mappedX = (tx - pivotX) / scaleAnim + pivotX;
        float mappedY = (ty - pivotY) / scaleAnim + pivotY;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = mappedX; initialTouchY = mappedY; lastTouchY = mappedY; isScrolling = false; draggingSetting = null; draggingWindow = null; scrollingWindow = null; hasLongPressed = false;

                boolean hitAny = false;
                for (int i = windows.size() - 1; i >= 0; i--) {
                    CategoryWindow w = windows.get(i);
                    if (w.headerRect.contains(mappedX, mappedY)) {
                        float rightIconX = w.x + w.width - 50f;
                        if (mappedX > rightIconX) { w.isExpanded = !w.isExpanded; invalidate(); } 
                        else { draggingWindow = w; touchDx = mappedX - w.x; touchDy = mappedY - w.y; windows.remove(w); windows.add(w); }
                        hitAny = true; break;
                    }
                    if (w.isExpanded && w.bodyRect.contains(mappedX, mappedY)) {
                        hitAny = true; scrollingWindow = w;
                        
                        if (w.categoryIndex == -1) { return true; } 
                        
                        for (ClickGuiView.Module m : ClickGuiView.allModules) {
                            if (m.categoryIndex == w.categoryIndex) {
                                // 核心修复：点击抽屉内的设置绝对隔离，防止开关被触发
                                if (m.isDrawerExpanded) {
                                    float drawerTop = m.cardHitbox.top + 65f;
                                    if (mappedY > drawerTop) {
                                        for (ClickGuiView.Setting s : m.settings) {
                                            if ((s instanceof ClickGuiView.Slider || s instanceof ClickGuiView.ColorPicker) && s.hitbox.contains(mappedX, mappedY)) {
                                                draggingSetting = s; updateSlider(s, mappedX, w); return true;
                                            }
                                        }
                                        return true; // 点在设置里但没命中控件，也拦截！
                                    }
                                }
                                if (m.cardHitbox.contains(mappedX, mappedY)) {
                                    if (!m.switchHitbox.contains(mappedX, mappedY)) {
                                        longPressedModule = m; handler.postDelayed(longPressRunnable, 350); 
                                    }
                                    return true;
                                }
                            }
                        }
                        break;
                    }
                }
                if (!hitAny) return false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(mappedX - initialTouchX); float dy = Math.abs(mappedY - initialTouchY);
                if (draggingWindow != null) {
                    float nx = mappedX - touchDx; float ny = mappedY - touchDy;
                    // 防止拖出屏幕
                    nx = Math.max(0, Math.min(nx, getWidth() - 350f));
                    ny = Math.max(0, Math.min(ny, getHeight() - 60f));
                    draggingWindow.x = nx; draggingWindow.y = ny; invalidate(); return true;
                }
                if (draggingSetting != null) {
                    updateSlider(draggingSetting, mappedX, scrollingWindow); return true;
                }
                
                // 终极 XY 轴隔离防断触：如果是横向滑动大于纵向，强行判定为点拉条
                if (!isScrolling && Math.hypot(dx, dy) > touchSlop && scrollingWindow != null) { 
                    if (dx > dy && scrollingWindow.categoryIndex != -1) {
                         // 横滑时如果位于某模块的抽屉高度，强制抓取滑块！
                    } else {
                         isScrolling = true; handler.removeCallbacks(longPressRunnable); 
                    }
                }
                if (isScrolling && scrollingWindow != null) {
                    scrollingWindow.scrollY += (mappedY - lastTouchY);
                    scrollingWindow.scrollY = Math.max(scrollingWindow.maxScrollY, Math.min(0f, scrollingWindow.scrollY));
                    lastTouchY = mappedY; invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean isTap = Math.abs(mappedX - initialTouchX) < touchSlop && Math.abs(mappedY - initialTouchY) < touchSlop;
                handler.removeCallbacks(longPressRunnable);
                if (draggingWindow != null || draggingSetting != null) { draggingWindow = null; draggingSetting = null; return true; }
                if (!isScrolling && isTap && scrollingWindow != null && event.getAction() == MotionEvent.ACTION_UP) {
                    
                    if (scrollingWindow.categoryIndex == -1) {
                        float hubY = scrollingWindow.y + 60f + 20f + scrollingWindow.scrollY;
                        if (mappedY > hubY+15f && mappedY < hubY+60f) {
                            if (mappedX > scrollingWindow.x+20f && mappedX < scrollingWindow.x+170f && ClickGuiView.guiMode != 0) { ClickGuiView.guiMode = 0; if (ClickGuiView.onGuiModeSwitched != null) ClickGuiView.onGuiModeSwitched.run(); }
                            if (mappedX > scrollingWindow.x+180f && mappedX < scrollingWindow.x+330f && ClickGuiView.guiMode != 1) { ClickGuiView.guiMode = 1; if (ClickGuiView.onGuiModeSwitched != null) ClickGuiView.onGuiModeSwitched.run(); }
                        }
                        hubY += 85f;
                        if (mappedY > hubY+15f && mappedY < hubY+60f) {
                            if (mappedX > scrollingWindow.x+20f && mappedX < scrollingWindow.x+170f && ClickGuiView.isLightTheme) { ClickGuiView.isLightTheme = false; invalidate(); if(ClickGuiView.instance!=null)ClickGuiView.instance.invalidate(); }
                            if (mappedX > scrollingWindow.x+180f && mappedX < scrollingWindow.x+330f && !ClickGuiView.isLightTheme) { ClickGuiView.isLightTheme = true; invalidate(); if(ClickGuiView.instance!=null)ClickGuiView.instance.invalidate(); }
                        }
                        hubY += 85f;
                        if (mappedY > hubY+15f && mappedY < hubY+60f) {
                            float btnW = (350f - 50f)/3f;
                            if (mappedX > scrollingWindow.x+20f && mappedX < scrollingWindow.x+20f+btnW) ClickGuiView.shortcutSize = 0;
                            if (mappedX > scrollingWindow.x+25f+btnW && mappedX < scrollingWindow.x+25f+btnW*2f) ClickGuiView.shortcutSize = 1;
                            if (mappedX > scrollingWindow.x+30f+btnW*2f) ClickGuiView.shortcutSize = 2;
                            invalidate(); if(ClickGuiView.instance!=null)ClickGuiView.instance.invalidate();
                        }
                        hubY += 85f;
                        if (mappedY > hubY+15f && mappedY < hubY+60f) {
                            if (mappedX > scrollingWindow.x+20f && mappedX < scrollingWindow.x+170f && !ClickGuiView.isEnglish) { ClickGuiView.isEnglish = true; invalidate(); if(ClickGuiView.instance!=null)ClickGuiView.instance.invalidate(); }
                            if (mappedX > scrollingWindow.x+180f && mappedX < scrollingWindow.x+330f && ClickGuiView.isEnglish) { ClickGuiView.isEnglish = false; invalidate(); if(ClickGuiView.instance!=null)ClickGuiView.instance.invalidate(); }
                        }
                        return true;
                    }

                    for (ClickGuiView.Module m : ClickGuiView.allModules) {
                        if (m.categoryIndex == scrollingWindow.categoryIndex && m.cardHitbox.contains(mappedX, mappedY)) {
                            // 隔离内部设置点击
                            if (m.isDrawerExpanded && mappedY > m.cardHitbox.top + 65f) {
                                for (ClickGuiView.Setting s : m.settings) {
                                    if (s instanceof ClickGuiView.Mode) {
                                        ClickGuiView.Mode md = (ClickGuiView.Mode) s;
                                        for (int i=0; i<md.modesEn.length; i++) if (md.modeRects[i].contains(mappedX, mappedY)) { md.current = i; ClickGuiView.onModuleToggled.run(); invalidate(); return true; }
                                    }
                                }
                                return true; // 点在设置空白处，消耗掉
                            }
                            
                            // 点击三点 = 展开设置；点击其它地方 = 开启模块
                            if (m.hasSettings && m.switchHitbox.contains(mappedX, mappedY)) { 
                                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                                m.isDrawerExpanded = !m.isDrawerExpanded; 
                                ValueAnimator va = ValueAnimator.ofFloat(m.currentDrawerAnim, m.isDrawerExpanded ? m.drawerHeightMax : 0f); 
                                va.setDuration(250); va.addUpdateListener(a -> { m.currentDrawerAnim = (float) a.getAnimatedValue(); invalidate(); }); va.start(); 
                            } else if (!hasLongPressed) { 
                                ClickGuiView.toggleModule(m); 
                            }
                            return true;
                        }
                    }
                }
                isScrolling = false; scrollingWindow = null; break;
        }
        return true;
    }

    private void updateSlider(ClickGuiView.Setting s, float x, CategoryWindow w) {
        float trackLeft = s.hitbox.left + 8f; float trackRight = s.hitbox.right - 85f;
        float r = Math.max(0f, Math.min(1f, (x - trackLeft) / (trackRight - trackLeft)));
        if (s instanceof ClickGuiView.Slider) { ((ClickGuiView.Slider)s).val = Math.round(r * ((ClickGuiView.Slider)s).max); }
        else if (s instanceof ClickGuiView.ColorPicker) { ((ClickGuiView.ColorPicker)s).hue = r * 360f; }
        if (ClickGuiView.onModuleToggled != null) ClickGuiView.onModuleToggled.run(); invalidate();
    }

    private class CategoryWindow {
        int categoryIndex; String nameEn, nameZh; float x, y, width = 360f;
        boolean isExpanded = true; float currentBodyH = 0f; float scrollY = 0f, maxScrollY = 0f;
        RectF headerRect = new RectF(), bodyRect = new RectF();
        CategoryWindow(int idx) { 
            categoryIndex = idx; 
            if (idx == -1) { nameEn = "Hub"; nameZh = "枢纽"; }
            else { nameEn = CAT_EN[idx]; nameZh = CAT_ZH[idx]; }
        }
    }
}
