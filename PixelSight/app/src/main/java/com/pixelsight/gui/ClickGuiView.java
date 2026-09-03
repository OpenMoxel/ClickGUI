package com.pixelsight.gui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
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

import com.pixelsight.hud.DynamicIslandView;
import com.pixelsight.hud.NotificationView;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiView extends View {

    public static ClickGuiView instance; 

    private Paint paint, textPaint, iconPaint, glassHighlightPaint;
    private RectF panelRect, sidebarRect, searchRect, langRect, fullScreenHitbox, gearHitbox;
    public boolean isVisible = false;

    public static boolean isEnglish = true; 
    public static boolean isLightTheme = false; 
    public static int triggerMode = 0; 
    public static int shortcutSize = 1; 
    public static int guiMode = 0; 
    public static Runnable onTriggerChanged; 
    public static Runnable onGuiModeSwitched; 

    private boolean isFullscreen = false;
    private float fullscreenAnim = 0f; 
    private boolean isGridLayout = false;
    private boolean isPanelSettings = false; 
    private float layoutAnim = 1f; 
    private boolean isLiquidGlass = true; 

    private int colorBgPanel, colorBgSidebar, colorCardBg, colorAccent, colorTextMain, colorTextSub, colorStroke, colorSwitchBg;

    private boolean isGlobalSettingsOpen = false;
    private float globalSettingsAnim = 0f;
    private RectF globalSettingsRect = new RectF();
    private RectF btnListRect = new RectF(), btnGridRect = new RectF();
    private RectF btnDrawerRect = new RectF(), btnPanelRect = new RectF();
    private RectF btnDarkRect = new RectF(), btnLightRect = new RectF();
    private RectF btnSolidRect = new RectF(), btnGlassRect = new RectF();
    private RectF btnFloatRect = new RectF(), btnVolRect = new RectF(), btnIslandRect = new RectF();
    private RectF btnShortSmall = new RectF(), btnShortMid = new RectF(), btnShortLarge = new RectF();
    private RectF btnGuiAui = new RectF(), btnGuiBui = new RectF();

    private Module activePanelModule = null;
    private float rightPanelAnim = 0f;
    private RectF rightPanelRect = new RectF();
    private float rightPanelScrollY = 0f, maxRightPanelScrollY = 0f;

    private String searchText = "";
    private Runnable onSearchRequest;
    private final String[] CAT_EN = {"Combat", "Movement", "World", "Player", "Misc", "Render"};
    private final String[] CAT_ZH = {"战斗", "移动", "世界", "玩家", "杂项", "渲染"};
    private int currentCategoryIndex = 0; 
    private RectF[] categoryRects = new RectF[CAT_EN.length];

    private float listAlpha = 1f, listOffsetY = 0f; 
    private float scrollY = 0f, maxScrollY = 0f;
    
    private float initialTouchX = 0f, initialTouchY = 0f, lastTouchY = 0f;
    private boolean isScrolling = false;
    private boolean hasLongPressed = false;
    private Setting draggingSetting = null; 
    private int touchSlop;

    private Path pathCombat, pathMovement, pathWorld, pathPlayer, pathMisc, pathRender;
    
    public static List<Module> allModules = new ArrayList<>();
    public static Runnable onModuleToggled;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Module longPressedModule = null;
    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (longPressedModule != null && longPressedModule.hasSettings && !isScrolling && draggingSetting == null) {
                hasLongPressed = true; 
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                if (isPanelSettings || isGridLayout) openRightPanel(longPressedModule);
                else {
                    longPressedModule.isDrawerExpanded = !longPressedModule.isDrawerExpanded;
                    animateDrawer(longPressedModule, longPressedModule.isDrawerExpanded);
                }
            }
        }
    };

    public ClickGuiView(Context context) {
        super(context);
        instance = this; 
        init();
        if (allModules.isEmpty()) initModules(); 
        applyTheme();
    }

    public static Module getModule(String nameEn) {
        for (Module m : allModules) if (m.nameEn.equals(nameEn)) return m; return null;
    }

    public static void toggleModule(Module m) {
        if (m == null) return;
        m.enabled = !m.enabled;
        
        if (instance != null) {
            instance.animateSwitch(m);
            instance.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
        
        if (onModuleToggled != null) onModuleToggled.run();
        DynamicIslandView.push(m.nameEn, m.enabled);
        NotificationView.show(m.nameEn, m.nameZh, m.enabled);
    }

    public void animateSwitch(Module m) { 
        ValueAnimator va = ValueAnimator.ofFloat(m.animProgress, m.enabled ? 1f : 0f); 
        va.setDuration(200); 
        va.addUpdateListener(a -> { 
            m.animProgress = (float) a.getAnimatedValue(); 
            invalidate(); 
            if (SolsticeGuiView.instance != null) SolsticeGuiView.instance.invalidate();
        }); 
        va.start(); 
    }

    private void applyTheme() {
        if (isLightTheme) {
            colorBgPanel = isLiquidGlass ? Color.parseColor("#DDF5F5F7") : Color.parseColor("#F5F5F7"); 
            colorBgSidebar = isLiquidGlass ? Color.parseColor("#DDBBBBBB") : Color.parseColor("#FFFFFF");
            colorCardBg = isLiquidGlass ? Color.parseColor("#EEFFFFFF") : Color.parseColor("#FFFFFF");
            colorTextMain = Color.parseColor("#1C1C1E"); colorTextSub = Color.parseColor("#55555A"); 
            colorAccent = Color.parseColor("#007AFF"); colorStroke = isLiquidGlass ? Color.parseColor("#40FFFFFF") : Color.parseColor("#E5E5EA");
            colorSwitchBg = Color.parseColor("#D1D1D6"); 
        } else {
            colorBgPanel = isLiquidGlass ? Color.parseColor("#B31D1D21") : Color.parseColor("#1D1D21");
            colorBgSidebar = isLiquidGlass ? Color.parseColor("#B3161618") : Color.parseColor("#161618");
            colorCardBg = isLiquidGlass ? Color.parseColor("#99242529") : Color.parseColor("#242529");
            colorTextMain = Color.parseColor("#FFFFFF"); colorTextSub = Color.parseColor("#808084");
            colorAccent = Color.parseColor("#4A80F6"); colorStroke = isLiquidGlass ? Color.parseColor("#20FFFFFF") : Color.parseColor("#26272B");
            colorSwitchBg = Color.parseColor("#323338");
        }
    }

    private void init() {
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG); textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG); iconPaint.setStyle(Paint.Style.STROKE); iconPaint.setStrokeWidth(3.5f); iconPaint.setStrokeCap(Paint.Cap.ROUND); iconPaint.setStrokeJoin(Paint.Join.ROUND);
        glassHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG); glassHighlightPaint.setStyle(Paint.Style.STROKE); glassHighlightPaint.setStrokeWidth(1.5f);

        panelRect = new RectF(); sidebarRect = new RectF(); searchRect = new RectF(); langRect = new RectF(); fullScreenHitbox = new RectF(); gearHitbox = new RectF();
        for (int i = 0; i < categoryRects.length; i++) categoryRects[i] = new RectF();

        setAlpha(0f); setScaleX(0f); setScaleY(0f); setVisibility(View.GONE); buildIcons();
    }

    private void initModules() {
        allModules.clear();
        addMod("KillAura", "杀戮光环", "Attack automatically.", "自动攻击范围内实体。", 0, true);
        getModule("KillAura").settings.add(new Mode("Mode", "模式", new String[]{"Single", "Switch", "Multi"}, new String[]{"单体", "切换", "多目标"}, 0));
        getModule("KillAura").settings.add(new Slider("Range", "攻击范围", 10, 60, 30));

        addMod("CPS", "点击速度", "Limit clicks per second.", "限制每秒自动点击上限。", 0, true);
        getModule("CPS").settings.add(new Slider("Speed", "速度", 1, 20, 12));

        addMod("Velocity", "防击退", "Modify knockback taken.", "修改受到攻击时的击退距离。", 0, false);
        getModule("Velocity").settings.add(new Slider("Horizontal", "水平击退", 0, 100, 0));
        getModule("Velocity").settings.add(new Slider("Vertical", "垂直击退", 0, 100, 0));

        addMod("Criticals", "刀刀暴击", "Always do critical hits.", "每次攻击强制触发暴击伤害。", 0, false);
        getModule("Criticals").settings.add(new Mode("Mode", "模式", new String[]{"Packet", "Jump", "Mini"}, new String[]{"发包", "跳跃", "微跳"}, 0));

        addMod("Reach", "极限距离", "Increase attack reach.", "增加合法攻击判定距离。", 0, false);
        getModule("Reach").settings.add(new Slider("Distance", "距离", 30, 60, 42));

        addMod("HitBox", "扩大体积", "Expand entity hitboxes.", "扩大实体受击体积箱。", 0, false);
        getModule("HitBox").settings.add(new Slider("Expand", "扩大倍数", 1, 10, 2));

        addMod("AimAssist", "辅助瞄准", "Helps you aim.", "自动瞄准实体。", 0, true);
        addMod("TargetStrafe", "环绕攻击", "Strafe around targets.", "攻击时自动环绕目标走位。", 0, false);
        
        addMod("Sprint", "自动疾跑", "Automatically sprint.", "无需双击自动疾跑。", 1, true);
        getModule("Sprint").settings.add(new Mode("Dir", "方向", new String[]{"All", "Forward"}, new String[]{"全向", "仅向前"}, 0));

        addMod("Fly", "飞行模式", "Fly like creative.", "像创造模式一样飞行。", 1, false);
        getModule("Fly").settings.add(new Slider("Speed", "飞行速度", 1, 50, 10));

        addMod("Timer", "变速齿轮", "Change game tick speed.", "改变游戏全局运行速度。", 1, false);
        getModule("Timer").settings.add(new Slider("Multiplier", "倍率", 1, 50, 20));

        addMod("Speed", "连跳加速", "Move faster.", "自动连跳并增加移动速度。", 1, false);
        getModule("Speed").settings.add(new Mode("Mode", "模式", new String[]{"Bhop", "Strafe"}, new String[]{"连跳", "空移"}, 0));

        addMod("HighJump", "极高跳跃", "Jump higher than normal.", "突破原版跳跃高度限制。", 1, false);
        addMod("Step", "自动跃上", "Step up blocks.", "遇到方块时自动跃上。", 1, false);

        addMod("Scaffold", "自动搭桥", "Place blocks under you.", "在脚下虚空自动放置方块。", 2, false);
        getModule("Scaffold").settings.add(new Slider("Delay", "延迟", 0, 20, 10));

        addMod("FastPlace", "快速放置", "Remove place delay.", "移除右键延迟。", 2, true);
        addMod("Nuke", "核弹挖矿", "Break all blocks around.", "瞬间摧毁周围所有特定方块。", 2, false);

        addMod("NoFall", "无掉落伤", "Prevent fall damage.", "防止从高处掉落受到伤害。", 3, true);
        addMod("InvManager", "背包管理", "Manage inventory.", "自动清理无用物品并整理背包。", 3, false);
        addMod("AutoArmor", "自动穿甲", "Equips best armor.", "自动穿戴最好的装备。", 3, true);
        addMod("AutoPot", "自动丢药", "Throws potions.", "血量低时自动丢治疗药水。", 3, false);
        addMod("ChestStealer", "自动偷箱", "Steal items from chests.", "一键拿走箱子里的所有物品。", 3, false);
        addMod("Blink", "瞬移卡顿", "Hold packets to blink.", "拦截发包，在别人眼里像瞬移。", 3, false);

        addMod("AutoRespawn", "自动重生", "Respawn instantly.", "死亡后瞬间复活。", 4, true);
        addMod("Freecam", "灵魂出窍", "Move camera freely.", "本体留在原地，视角自由飞行。", 4, false);
        addMod("Spammer", "自动喊话", "Spam chat.", "自动喊话刷屏。", 4, false);

        Module island = new Module("DynamicIsland", "灵动岛", "Show animated top pill.", "屏幕顶端显示灵动岛。", 5, true);
        island.settings.add(new Mode("Mode", "模式", new String[]{"Classic", "HUD"}, new String[]{"经典", "状态栏"}, 1));
        island.settings.add(new Mode("Alerts", "功能提示", new String[]{"Off", "On"}, new String[]{"关闭", "开启"}, 1));
        allModules.add(island);

        Module notif = new Module("Notifications", "模块提示", "Show HUD toggle alerts.", "屏幕右下角显示模块开关提示。", 5, true);
        notif.settings.add(new Mode("Style", "风格", new String[]{"Classic", "Vape", "Outline"}, new String[]{"经典", "Vape", "描边"}, 1));
        notif.settings.add(new Slider("Scale", "缩放大小", 50, 150, 100));
        allModules.add(notif);

        Module arrayList = new Module("ArrayList", "功能列表", "Display enabled modules.", "在屏幕右上角显示已开启功能。", 5, true);
        arrayList.settings.add(new Mode("Style", "风格", new String[]{"Classic", "Glow", "Neon", "Vape"}, new String[]{"经典", "辉光", "霓虹", "Vape"}, 1));
        arrayList.settings.add(new Mode("Color", "颜色", new String[]{"Rainbow", "Blue", "Custom"}, new String[]{"彩虹", "原版蓝", "自定义"}, 0));
        arrayList.settings.add(new ColorPicker("Hue", "色相(自定义)", 200)); 
        arrayList.settings.add(new Slider("Pos X", "X轴", 0, 100, 100));
        arrayList.settings.add(new Slider("Pos Y", "Y轴", 0, 100, 0));
        arrayList.settings.add(new Slider("Scale", "缩放比例", 50, 150, 100));
        arrayList.settings.add(new Slider("Glow", "辉光强度", 0, 100, 60));
        arrayList.settings.add(new Mode("Font", "字体", new String[]{"Normal", "Serif", "Mono", "Pixel"}, new String[]{"默认", "衬线", "等宽", "像素"}, 0));
        arrayList.settings.add(new Mode("Tags", "详细后缀", new String[]{"None", "Simple"}, new String[]{"隐藏", "简略"}, 1));
        arrayList.settings.add(new Mode("Lang", "语言", new String[]{"English", "Chinese"}, new String[]{"英文", "中文"}, 0));
        allModules.add(arrayList);

        addMod("ESP", "透视光环", "See entities through walls.", "隔墙高亮显示实体位置。", 5, true);
        getModule("ESP").settings.add(new Mode("Mode", "样式", new String[]{"Box", "Outline", "Chams"}, new String[]{"方框", "描边", "着色"}, 0));

        addMod("Fullbright", "夜视模式", "See in the dark.", "修改亮度以在黑暗中视物。", 5, true);
        addMod("Tracers", "玩家追踪", "Draw lines to players.", "绘制连接玩家的线条。", 5, false);

        for(Module m : allModules) m.calcHeights();
    }

    private void addMod(String nEn, String nZh, String dEn, String dZh, int cat, boolean e) { allModules.add(new Module(nEn, nZh, dEn, dZh, cat, e)); }
    private String t(String en, String zh) { return isEnglish ? en : zh; }

    private void buildIcons() {
        pathCombat = new Path(); pathCombat.moveTo(-7, 7); pathCombat.lineTo(5, -5); pathCombat.moveTo(-2, 7); pathCombat.lineTo(-7, 2); pathCombat.moveTo(1, -7); pathCombat.lineTo(7, -1);
        pathMovement = new Path(); pathMovement.addCircle(2, -7, 2.5f, Path.Direction.CW); pathMovement.moveTo(1, -3); pathMovement.lineTo(-1, 2); pathMovement.moveTo(1, -3); pathMovement.lineTo(-4, 0); pathMovement.moveTo(1, -3); pathMovement.lineTo(5, -1); pathMovement.moveTo(-1, 2); pathMovement.lineTo(-5, 6); pathMovement.moveTo(-1, 2); pathMovement.lineTo(4, 3); pathMovement.lineTo(3, 7);
        pathWorld = new Path(); float s = 6.5f, h = (float)(s * Math.sqrt(3)/2); pathWorld.moveTo(0, -s); pathWorld.lineTo(h, -s/2); pathWorld.lineTo(h, s/2); pathWorld.lineTo(0, s); pathWorld.lineTo(-h, s/2); pathWorld.lineTo(-h, -s/2); pathWorld.close(); pathWorld.moveTo(0, 0); pathWorld.lineTo(0, s); pathWorld.moveTo(0, 0); pathWorld.lineTo(-h, -s/2); pathWorld.moveTo(0, 0); pathWorld.lineTo(h, -s/2);
        pathPlayer = new Path(); pathPlayer.addCircle(0, -4, 4f, Path.Direction.CW); pathPlayer.moveTo(-8, 8); pathPlayer.cubicTo(-8, 0, 8, 0, 8, 8);
        pathMisc = new Path(); pathMisc.addCircle(0, 0, 3.5f, Path.Direction.CW); for(int i=0; i<8; i++) { float angle = (float) (i * Math.PI / 4); pathMisc.moveTo((float) (Math.cos(angle) * 3.5f), (float) (Math.sin(angle) * 3.5f)); pathMisc.lineTo((float) (Math.cos(angle) * 6.5f), (float) (Math.sin(angle) * 6.5f)); }
        pathRender = new Path(); pathRender.addOval(new RectF(-8, -6, 8, 6), Path.Direction.CW); pathRender.addCircle(4, 1.5f, 1.5f, Path.Direction.CW); pathRender.moveTo(-4, -2); pathRender.lineTo(-4, -2.1f); pathRender.moveTo(0, -3); pathRender.lineTo(0, -3.1f);
    }

    public void setSearchText(String text) { this.searchText = text.toLowerCase(); scrollY = 0f; switchCategory(currentCategoryIndex); }
    public void setOnSearchRequestListener(Runnable listener) { this.onSearchRequest = listener; }
    
    public void toggle(float x, float y) { if (isVisible) closeGui(); else openGui(x, y); }
    public void toggle() { toggle(0, 0); } 

    public void openGui(float x, float y) {
        isGlobalSettingsOpen = false; globalSettingsAnim = 0f; draggingSetting = null; isScrolling = false; hasLongPressed = false;
        
        isVisible = true; setVisibility(View.VISIBLE);
        if (x != 0 && y != 0) { setPivotX(x); setPivotY(y); } 
        else { setPivotX(getWidth()/2f); setPivotY(getHeight()/2f); }
        
        animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(350).setInterpolator(new OvershootInterpolator(1.1f)).start();
        if (activePanelModule != null && (isPanelSettings || isGridLayout)) rightPanelAnim = 1f; 
        switchCategory(currentCategoryIndex);
    }

    public void closeGui() {
        isVisible = false; isGlobalSettingsOpen = false; globalSettingsAnim = 0f; draggingSetting = null; isScrolling = false;
        animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(250).setInterpolator(new DecelerateInterpolator()).withEndAction(() -> setVisibility(View.GONE)).start();
    }

    private void switchCategory(int index) {
        currentCategoryIndex = index; scrollY = 0f; 
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(250); va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(a -> { listAlpha = (float) a.getAnimatedValue(); listOffsetY = 30f * (1f - listAlpha); invalidate(); }); va.start();
    }

    private void switchLayoutMode() {
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(350); va.setInterpolator(new OvershootInterpolator(1.2f));
        va.addUpdateListener(a -> { layoutAnim = (float) a.getAnimatedValue(); invalidate(); }); va.start();
    }

    private void toggleTheme() { isLightTheme = !isLightTheme; applyTheme(); invalidate(); }
    private void toggleGlass() { isLiquidGlass = !isLiquidGlass; applyTheme(); invalidate(); }
    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        ValueAnimator va = ValueAnimator.ofFloat(fullscreenAnim, isFullscreen ? 1f : 0f);
        va.setDuration(350); va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(a -> { fullscreenAnim = (float) a.getAnimatedValue(); scrollY = Math.max(maxScrollY, Math.min(0f, scrollY)); invalidate(); }); va.start();
    }

    private void toggleGlobalSettings() {
        isGlobalSettingsOpen = !isGlobalSettingsOpen;
        if (isGlobalSettingsOpen && activePanelModule != null) { activePanelModule = null; rightPanelAnim = 0f; } 
        ValueAnimator va = ValueAnimator.ofFloat(globalSettingsAnim, isGlobalSettingsOpen ? 1f : 0f);
        va.setDuration(300); va.setInterpolator(new OvershootInterpolator(1.2f));
        va.addUpdateListener(a -> { globalSettingsAnim = (float) a.getAnimatedValue(); invalidate(); }); va.start();
    }

    private void openRightPanel(Module m) {
        if (activePanelModule == m) { closeRightPanel(); return; }
        activePanelModule = m; rightPanelScrollY = 0f; 
        if (rightPanelAnim == 1f) { invalidate(); return; } 
        ValueAnimator va = ValueAnimator.ofFloat(rightPanelAnim, 1f);
        va.setDuration(300); va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(a -> { rightPanelAnim = (float) a.getAnimatedValue(); invalidate(); }); va.start();
    }

    private void closeRightPanel() {
        if (activePanelModule == null) return;
        ValueAnimator va = ValueAnimator.ofFloat(rightPanelAnim, 0f);
        va.setDuration(250); va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(a -> { rightPanelAnim = (float) a.getAnimatedValue(); if (rightPanelAnim == 0f) activePanelModule = null; invalidate(); }); va.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float viewW = getWidth(), viewH = getHeight();
        float normW = viewW * 0.78f, normH = viewH * 0.90f;
        float fullW = viewW - 50f, fullH = viewH - 50f;
        
        float curW = normW + (fullW - normW) * fullscreenAnim;
        float curH = normH + (fullH - normH) * fullscreenAnim;
        float left = (viewW - curW) / 2f, top = (viewH - curH) / 2f;
        
        panelRect.set(left, top, left + curW, top + curH);
        sidebarRect.set(left, top, left + (curW * 0.20f), top + curH);
        float cornerRadius = 30f - (15f * fullscreenAnim);

        paint.setColor(colorBgPanel); 
        if(isLightTheme) paint.setShadowLayer(30f, 0, 15f, Color.parseColor("#25000000"));
        canvas.drawRoundRect(panelRect, cornerRadius, cornerRadius, paint); paint.clearShadowLayer();
        drawGlassHighlight(canvas, panelRect, cornerRadius);

        canvas.save(); canvas.clipRect(sidebarRect);
        paint.setColor(colorBgSidebar); 
        canvas.drawRoundRect(new RectF(sidebarRect.left, sidebarRect.top, sidebarRect.right + cornerRadius, sidebarRect.bottom), cornerRadius, cornerRadius, paint);
        canvas.restore();
        drawGlassHighlight(canvas, new RectF(sidebarRect.left, sidebarRect.top, sidebarRect.right, sidebarRect.bottom), cornerRadius);

        paint.setColor(isLiquidGlass ? Color.parseColor("#15FFFFFF") : colorStroke); paint.setStrokeWidth(2f);
        canvas.drawLine(sidebarRect.right, sidebarRect.top + 30f, sidebarRect.right, sidebarRect.bottom - 30f, paint);

        drawSidebar(canvas);
        drawTopBar(canvas); 

        float listTop = panelRect.top + 130f;
        float listBottom = panelRect.bottom - 20f;
        
        Path mainClip = new Path();
        mainClip.addRoundRect(panelRect, cornerRadius, cornerRadius, Path.Direction.CW);
        canvas.save(); canvas.clipPath(mainClip); 

        canvas.save(); canvas.clipRect(sidebarRect.right, listTop, panelRect.right, listBottom);
        canvas.translate(0, scrollY + listOffsetY);
        textPaint.setAlpha((int)(255 * listAlpha)); paint.setAlpha((int)(255 * listAlpha));
        drawModules(canvas);
        canvas.restore();

        if (rightPanelAnim > 0f) drawRightPanel(canvas, cornerRadius, listTop, listBottom);
        if (globalSettingsAnim > 0f) drawGlobalSettings(canvas);
        
        canvas.restore(); 
        drawBottomStatus(canvas); 
    }
    
    private void drawGlassHighlight(Canvas canvas, RectF rect, float radius) {
        if (!isLiquidGlass) return;
        glassHighlightPaint.setColor(isLightTheme ? Color.parseColor("#60FFFFFF") : Color.parseColor("#25FFFFFF"));
        canvas.drawRoundRect(rect.left + 1, rect.top + 1, rect.right - 1, rect.bottom - 1, radius, radius, glassHighlightPaint);
    }

    private void drawTopBar(Canvas canvas) {
        float startX = sidebarRect.right + 35f; float startY = panelRect.top + 70f;
        
        textPaint.setColor(colorTextMain); textPaint.setTextSize(42f); textPaint.setTypeface(Typeface.DEFAULT_BOLD); textPaint.setAlpha(255); 
        canvas.drawText(searchText.isEmpty() ? t(CAT_EN[currentCategoryIndex], CAT_ZH[currentCategoryIndex]) : t("Search Results", "搜索结果"), startX, startY, textPaint);

        float rx = panelRect.right - 55f; float gearX = rx - 65f; float langX = gearX - 130f; 
        
        langRect.set(langX, startY - 35f, gearX - 35f, startY + 10f);
        paint.setColor(isLiquidGlass ? (isLightTheme ? Color.parseColor("#CCFFFFFF") : Color.parseColor("#602D2E33")) : (isLightTheme ? Color.WHITE : Color.parseColor("#2D2E33"))); 
        paint.setAlpha(255); 
        if(isLightTheme && !isLiquidGlass) paint.setShadowLayer(5f,0,2f,Color.parseColor("#15000000"));
        canvas.drawRoundRect(langRect, 12f, 12f, paint); paint.clearShadowLayer();
        
        textPaint.setColor(colorTextMain); textPaint.setTextSize(24f); textPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics ffm = textPaint.getFontMetrics();
        canvas.drawText(isEnglish ? "EN | 中" : "中 | EN", langRect.centerX(), langRect.centerY() - (ffm.descent + ffm.ascent)/2f, textPaint); textPaint.setTextAlign(Paint.Align.LEFT);

        searchRect.set(startX + 220f, startY - 35f, langRect.left - 20f, startY + 12f);
        paint.setColor(isLiquidGlass ? (isLightTheme ? Color.parseColor("#99FFFFFF") : Color.parseColor("#40242529")) : (isLightTheme ? Color.WHITE : Color.parseColor("#242529")));
        if(isLightTheme && !isLiquidGlass) paint.setShadowLayer(5f,0,2f,Color.parseColor("#10000000"));
        canvas.drawRoundRect(searchRect, 15f, 15f, paint); paint.clearShadowLayer();
        
        float sCx = searchRect.left + 25f; float sCy = searchRect.centerY();
        iconPaint.setColor(colorTextSub); iconPaint.setStrokeWidth(3f);
        canvas.drawCircle(sCx - 2f, sCy - 2f, 6f, iconPaint); canvas.drawLine(sCx + 3f, sCy + 3f, sCx + 9f, sCy + 9f, iconPaint);
        
        textPaint.setColor(searchText.isEmpty() ? colorTextSub : colorTextMain); textPaint.setTextSize(24f); textPaint.setTypeface(Typeface.DEFAULT);
        Paint.FontMetrics fm = textPaint.getFontMetrics(); float textY = sCy - (fm.descent + fm.ascent) / 2f;
        
        canvas.save(); canvas.clipRect(searchRect.left + 45f, searchRect.top, searchRect.right - 10f, searchRect.bottom);
        canvas.drawText(searchText.isEmpty() ? t("Search...", "搜索...") : searchText + "|", searchRect.left + 45f, textY, textPaint);
        canvas.restore();

        iconPaint.setColor(colorTextMain); fullScreenHitbox.set(rx - 25f, startY - 35f, rx + 25f, startY + 15f); 
        canvas.save(); canvas.translate(rx, startY - 12f); 
        float cornerOffset = 8f - 5f * fullscreenAnim; float armLength = 5f * (1f - 2f * fullscreenAnim); 
        for (int i = 0; i < 4; i++) {
            float signX = (i == 0 || i == 3) ? -1 : 1; float signY = (i == 0 || i == 1) ? -1 : 1;
            canvas.drawLine(signX * cornerOffset, signY * cornerOffset, signX * cornerOffset - signX * armLength, signY * cornerOffset, iconPaint); 
            canvas.drawLine(signX * cornerOffset, signY * cornerOffset, signX * cornerOffset, signY * cornerOffset - signY * armLength, iconPaint);
        }
        canvas.restore();

        gearHitbox.set(gearX - 25f, startY - 35f, gearX + 25f, startY + 15f);
        canvas.save(); canvas.rotate(globalSettingsAnim * 90f, gearX, startY - 12f); canvas.drawCircle(gearX, startY - 12f, 5f, iconPaint); 
        for(int i=0; i<6; i++) { canvas.save(); canvas.rotate(i*60, gearX, startY - 12f); canvas.drawLine(gearX, startY - 20f, gearX, startY - 17f, iconPaint); canvas.restore(); }
        canvas.restore();
    }

    private void drawSidebar(Canvas canvas) {
        float x = sidebarRect.left + 25f; float y = sidebarRect.top + 80f;
        textPaint.setColor(colorTextMain); textPaint.setTextSize(46f); textPaint.setTypeface(Typeface.DEFAULT_BOLD); canvas.drawText("PixelSight", x, y, textPaint); 
        textPaint.setColor(colorTextSub); textPaint.setTextSize(16f); textPaint.setTypeface(Typeface.DEFAULT); canvas.drawText(t("Advanced Minecraft Utility", "高级我的世界实用工具"), x, y + 30f, textPaint);

        y += 100f; 
        for (int i = 0; i < CAT_EN.length; i++) {
            boolean isSelected = (i == currentCategoryIndex) && searchText.isEmpty();
            categoryRects[i].set(x, y - 35f, sidebarRect.right - 15f, y + 30f);
            if (isSelected) { 
                paint.setColor(isLightTheme ? (isLiquidGlass?Color.parseColor("#B3FFFFFF"):Color.parseColor("#E5E5EA")) : (isLiquidGlass?Color.parseColor("#602D2E33"):Color.parseColor("#2D2E33"))); 
                if(isLightTheme && !isLiquidGlass) paint.setShadowLayer(5f,0,2f,Color.parseColor("#15000000"));
                canvas.drawRoundRect(categoryRects[i], 16f, 16f, paint); paint.clearShadowLayer();
            }
            
            textPaint.setColor(isSelected ? colorTextMain : colorTextSub); textPaint.setTextSize(24f); textPaint.setTypeface(isSelected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            canvas.drawText(t(CAT_EN[i], CAT_ZH[i]), x + 60f, y + 8f, textPaint);

            iconPaint.setColor(isSelected ? colorTextMain : colorTextSub);
            canvas.save(); canvas.translate(x + 25f, y - 2f); Path iconPath = getPathForCategory(i); if (iconPath != null) canvas.drawPath(iconPath, iconPaint); canvas.restore();
            y += 80f;
        }
    }

    private Path getPathForCategory(int index) {
        switch (index) { case 0: return pathCombat; case 1: return pathMovement; case 2: return pathWorld; case 3: return pathPlayer; case 4: return pathMisc; case 5: return pathRender; } return null;
    }

    private void drawBottomStatus(Canvas canvas) {
        float bY = panelRect.bottom - 45f;
        paint.setColor(colorAccent); canvas.drawCircle(sidebarRect.left + 45f, bY, 20f, paint); 
        textPaint.setColor(Color.WHITE); textPaint.setTextSize(24f); textPaint.setTypeface(Typeface.DEFAULT_BOLD); textPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = textPaint.getFontMetrics(); canvas.drawText("P", sidebarRect.left + 45f, bY - (fm.descent + fm.ascent)/2f, textPaint); textPaint.setTextAlign(Paint.Align.LEFT);
        
        textPaint.setColor(colorTextMain); textPaint.setTextSize(22f); canvas.drawText("PixelSight", sidebarRect.left + 75f, bY - 4f, textPaint);
        textPaint.setColor(colorTextSub); textPaint.setTextSize(14f); canvas.drawText(t("Admin User", "管理员用户"), sidebarRect.left + 75f, bY + 15f, textPaint);

        int activeCount = 0; for (Module m : allModules) if (m.enabled) activeCount++;
        String countStr = t("Enabled: ", "已开启: ") + activeCount;

        float pX = panelRect.right - 80f; 
        textPaint.setColor(colorAccent); textPaint.setTextSize(22f); textPaint.setTypeface(Typeface.DEFAULT_BOLD); 
        if(!isLightTheme) textPaint.setShadowLayer(8f, 0, 0, Color.parseColor("#404A80F6")); 
        canvas.drawText(countStr, pX - textPaint.measureText(countStr) - 30f, bY + 8f, textPaint); textPaint.clearShadowLayer();

        paint.setColor(Color.parseColor("#4A80F6")); canvas.drawCircle(pX, bY, 6f, paint); paint.setColor(Color.parseColor("#B026FF")); canvas.drawCircle(pX + 22f, bY, 6f, paint); paint.setColor(Color.parseColor("#502280")); canvas.drawCircle(pX + 44f, bY, 6f, paint);
    }

    private void drawModules(Canvas canvas) {
        float startX = sidebarRect.right + 30f; float currentY = panelRect.top + 130f; 
        float availableW = panelRect.right - startX - 30f;
        float pW = availableW * 0.60f; 
        float listTargetW = availableW * 0.40f - 20f; 
        float listWidth = availableW - (availableW - listTargetW) * rightPanelAnim; 
        
        float baseCardHeight = 125f; 
        int cols = isGridLayout ? (rightPanelAnim > 0.1f ? 1 : 2) : 1;
        float spacing = 30f;
        float gridCardWidth = (listWidth - spacing * (cols - 1)) / cols;
        int visibleIndex = 0;

        for (Module m : allModules) {
            boolean matchSearch = !searchText.isEmpty() && (m.nameEn.toLowerCase().contains(searchText) || m.nameZh.toLowerCase().contains(searchText));
            boolean matchCat = searchText.isEmpty() && m.categoryIndex == currentCategoryIndex;
            
            if (matchSearch || matchCat) {
                float x, y, w, h;
                
                if (isGridLayout) {
                    int col = visibleIndex % cols; int row = visibleIndex / cols;
                    x = startX + col * (gridCardWidth + spacing); y = currentY + row * (baseCardHeight + spacing);
                    w = gridCardWidth; h = baseCardHeight;
                } else {
                    x = startX; y = currentY; w = listWidth; 
                    h = baseCardHeight + (isPanelSettings ? 0 : m.currentDrawerAnim);
                }
                
                m.cardHitbox.set(x, y + scrollY, x + w, y + scrollY + h);
                paint.setColor(colorCardBg); 
                if(isLightTheme && !isLiquidGlass) paint.setShadowLayer(10f, 0, 5f, Color.parseColor("#15000000"));
                canvas.drawRoundRect(new RectF(x, y, x + w, y + h), 22f, 22f, paint); paint.clearShadowLayer();
                drawGlassHighlight(canvas, new RectF(x, y, x + w, y + h), 22f);

                canvas.save(); canvas.clipRect(x, y, x + w, y + h);

                float scaleRatio = isGridLayout ? Math.max(0.6f, Math.min(1f, w / 350f)) : 1f;

                textPaint.setColor(colorTextMain); textPaint.setTextSize(32f * scaleRatio); textPaint.setTypeface(Typeface.DEFAULT_BOLD); 
                canvas.drawText(t(m.nameEn, m.nameZh), x + 30f, y + 55f * scaleRatio, textPaint);

                textPaint.setColor(colorTextSub); textPaint.setTextSize(22f * scaleRatio); textPaint.setTypeface(Typeface.DEFAULT); 
                String desc = t(m.descEn, m.descZh);
                if (isGridLayout && textPaint.measureText(desc) > w - 40f) desc = desc.substring(0, Math.min(desc.length(), 10)) + "...";
                canvas.drawText(desc, x + 30f, y + 95f * scaleRatio, textPaint);

                float sW = isGridLayout ? 75f : 96f, sH = isGridLayout ? 40f : 50f;
                float sRight = x + w - 25f, sTop = y + (baseCardHeight - sH) / 2f; 
                m.switchHitbox.set(sRight - sW, sTop + scrollY, sRight, sTop + sH + scrollY); 
                
                paint.setColor(blendColors(colorSwitchBg, colorAccent, m.animProgress)); 
                if(isLightTheme && m.animProgress < 0.5f) paint.setShadowLayer(5f,0,2f,Color.parseColor("#15000000"));
                canvas.drawRoundRect(new RectF(sRight - sW, sTop, sRight, sTop + sH), sH/2, sH/2, paint); paint.clearShadowLayer();
                
                float circleX = (sRight - sW) + sH/2 + (sW - sH) * m.animProgress;
                float circleY = sTop + sH/2;
                paint.setColor(Color.WHITE); paint.setShadowLayer(4f, 0, 2f, Color.parseColor("#40000000"));
                canvas.drawCircle(circleX, circleY, sH/2 - 6f, paint); paint.clearShadowLayer();

                if (!isGridLayout && !isPanelSettings && m.currentDrawerAnim > 10f) {
                    drawModuleSettings(canvas, m, x, y + baseCardHeight, w, false);
                }
                canvas.restore();

                if (isGridLayout) visibleIndex++; else currentY += h + 18f;
            }
        }
        
        float contentHeight = isGridLayout ? (((visibleIndex - 1) / cols) + 1) * (baseCardHeight + spacing) : (currentY - (panelRect.top + 130f));
        float visibleHeight = panelRect.bottom - 40f - (panelRect.top + 130f);
        maxScrollY = contentHeight > visibleHeight ? -(contentHeight - visibleHeight) : 0;
    }

    private void drawGlobalSettings(Canvas canvas) {
        float pW = 450f, pH = 700f; 
        float pTop = panelRect.top + 110f;
        float pRight = panelRect.right - 45f;
        
        globalSettingsRect.set(pRight - pW, pTop, pRight, pTop + pH * globalSettingsAnim);
        if (globalSettingsAnim == 0f) return; 
        
        canvas.save(); canvas.clipRect(globalSettingsRect);
        
        paint.setColor(isLightTheme ? Color.parseColor("#E6F5F5F7") : Color.parseColor("#E6161618")); 
        if(isLightTheme && !isLiquidGlass) paint.setShadowLayer(20f,0,10f,Color.parseColor("#20000000"));
        canvas.drawRoundRect(new RectF(globalSettingsRect.left, globalSettingsRect.top, globalSettingsRect.right, pTop + pH), 25f, 25f, paint); paint.clearShadowLayer();
        drawGlassHighlight(canvas, new RectF(globalSettingsRect.left, globalSettingsRect.top, globalSettingsRect.right, pTop + pH), 25f);

        float x = globalSettingsRect.left + 35f; float y = pTop + 60f;
        textPaint.setColor(colorTextMain); textPaint.setTextSize(32f); textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText(t("GUI Settings", "界面设置"), x, y, textPaint);

        y += 70f; textPaint.setColor(colorTextSub); textPaint.setTextSize(24f); textPaint.setTypeface(Typeface.DEFAULT); canvas.drawText(t("GUI Mode", "客户端风格"), x, y, textPaint);
        btnGuiAui.set(globalSettingsRect.right - 190f, y - 30f, globalSettingsRect.right - 110f, y + 15f);
        btnGuiBui.set(globalSettingsRect.right - 100f, y - 30f, globalSettingsRect.right - 20f, y + 15f);
        drawToggleBtn(canvas, btnGuiAui, "Aui", guiMode == 0, false); drawToggleBtn(canvas, btnGuiBui, "Bui", guiMode == 1, false);

        y += 80f; canvas.drawText(t("Theme", "界面主题"), x, y, textPaint);
        btnDarkRect.set(globalSettingsRect.right - 190f, y - 30f, globalSettingsRect.right - 110f, y + 15f);
        btnLightRect.set(globalSettingsRect.right - 100f, y - 30f, globalSettingsRect.right - 20f, y + 15f);
        drawToggleBtn(canvas, btnDarkRect, t("Dark", "暗黑"), !isLightTheme, false); drawToggleBtn(canvas, btnLightRect, t("Light", "明亮"), isLightTheme, false);

        y += 80f; canvas.drawText(t("Style", "界面质感"), x, y, textPaint);
        btnSolidRect.set(globalSettingsRect.right - 190f, y - 30f, globalSettingsRect.right - 110f, y + 15f);
        btnGlassRect.set(globalSettingsRect.right - 100f, y - 30f, globalSettingsRect.right - 20f, y + 15f);
        drawToggleBtn(canvas, btnSolidRect, t("Solid", "纯色"), !isLiquidGlass, false); drawToggleBtn(canvas, btnGlassRect, t("Glass", "毛玻璃"), isLiquidGlass, false);

        y += 80f; canvas.drawText(t("Trigger", "唤出方式"), x, y, textPaint);
        btnFloatRect.set(globalSettingsRect.right - 270f, y - 30f, globalSettingsRect.right - 190f, y + 15f);
        btnVolRect.set(globalSettingsRect.right - 180f, y - 30f, globalSettingsRect.right - 100f, y + 15f);
        btnIslandRect.set(globalSettingsRect.right - 90f, y - 30f, globalSettingsRect.right - 10f, y + 15f);
        drawToggleBtn(canvas, btnFloatRect, t("Float", "悬浮球"), triggerMode == 0, false); 
        drawToggleBtn(canvas, btnVolRect, t("Vol", "音量键"), triggerMode == 1, false);
        drawToggleBtn(canvas, btnIslandRect, t("Island", "灵动岛"), triggerMode == 2, false);

        y += 80f; canvas.drawText(t("Layout", "模块布局"), x, y, textPaint);
        btnListRect.set(globalSettingsRect.right - 190f, y - 30f, globalSettingsRect.right - 110f, y + 15f);
        btnGridRect.set(globalSettingsRect.right - 100f, y - 30f, globalSettingsRect.right - 20f, y + 15f);
        drawToggleBtn(canvas, btnListRect, t("List", "列表"), !isGridLayout, false); drawToggleBtn(canvas, btnGridRect, t("Grid", "网格"), isGridLayout, false);

        y += 80f; canvas.drawText(t("Settings", "参数呼出"), x, y, textPaint);
        btnDrawerRect.set(globalSettingsRect.right - 190f, y - 30f, globalSettingsRect.right - 110f, y + 15f);
        btnPanelRect.set(globalSettingsRect.right - 100f, y - 30f, globalSettingsRect.right - 20f, y + 15f);
        boolean forcePanel = isGridLayout; 
        drawToggleBtn(canvas, btnDrawerRect, t("Drawer", "抽屉"), !isPanelSettings && !forcePanel, forcePanel); drawToggleBtn(canvas, btnPanelRect, t("Panel", "侧板"), isPanelSettings || forcePanel, false);

        y += 80f; canvas.drawText(t("Shortcuts", "快捷键大小"), x, y, textPaint);
        btnShortSmall.set(globalSettingsRect.right - 270f, y - 30f, globalSettingsRect.right - 190f, y + 15f);
        btnShortMid.set(globalSettingsRect.right - 180f, y - 30f, globalSettingsRect.right - 100f, y + 15f);
        btnShortLarge.set(globalSettingsRect.right - 90f, y - 30f, globalSettingsRect.right - 10f, y + 15f);
        drawToggleBtn(canvas, btnShortSmall, t("Small", "小号"), shortcutSize == 0, false); 
        drawToggleBtn(canvas, btnShortMid, t("Mid", "中号"), shortcutSize == 1, false);
        drawToggleBtn(canvas, btnShortLarge, t("Large", "大号"), shortcutSize == 2, false);

        canvas.restore();
    }

    private void drawToggleBtn(Canvas canvas, RectF rect, String text, boolean isActive, boolean isLocked) {
        int bgCol = isLiquidGlass ? (isLightTheme ? Color.parseColor("#B3FFFFFF") : Color.parseColor("#4026272B")) : (isLightTheme ? Color.WHITE : Color.parseColor("#26272B"));
        int lockCol = isLightTheme ? Color.parseColor("#E5E5EA") : Color.parseColor("#101010");
        paint.setColor(isActive ? colorAccent : (isLocked ? lockCol : bgCol));
        if(isLightTheme && !isActive && !isLocked && !isLiquidGlass) paint.setShadowLayer(5f,0,2f,Color.parseColor("#15000000"));
        canvas.drawRoundRect(rect, 10f, 10f, paint); paint.clearShadowLayer();
        
        int textCol = isLightTheme ? Color.parseColor("#55555A") : colorTextSub;
        textPaint.setColor(isActive ? Color.WHITE : (isLocked ? Color.parseColor("#A0A0A0") : textCol));
        textPaint.setTextAlign(Paint.Align.CENTER); textPaint.setTextSize(22f);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        canvas.drawText(text, rect.centerX(), rect.centerY() - (fm.descent + fm.ascent)/2f, textPaint); textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawRightPanel(Canvas canvas, float mainCornerRadius, float listTop, float listBottom) {
        float availableW = panelRect.right - sidebarRect.right - 35f;
        float pW = availableW * 0.6f; 
        float rpLeft = panelRect.right - 30f - pW * rightPanelAnim; 
        rightPanelRect.set(rpLeft, listTop, rpLeft + pW, listBottom);
        
        paint.setColor(isLightTheme ? (isLiquidGlass?Color.parseColor("#E6FFFFFF"):Color.parseColor("#FDFDFD")) : (isLiquidGlass?Color.parseColor("#E61D1D21"):colorCardBg)); 
        canvas.drawRoundRect(rightPanelRect, 22f, 22f, paint);
        drawGlassHighlight(canvas, rightPanelRect, 22f);
        
        iconPaint.setColor(isLiquidGlass ? Color.parseColor("#20FFFFFF") : colorStroke); 
        canvas.drawLine(rightPanelRect.left, rightPanelRect.top, rightPanelRect.left, rightPanelRect.bottom, iconPaint);

        if (activePanelModule != null) {
            float x = rightPanelRect.left + 30f; float y = rightPanelRect.top + 60f;
            textPaint.setColor(colorTextMain); textPaint.setTextSize(40f); textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(t(activePanelModule.nameEn, activePanelModule.nameZh), x, y, textPaint);
            
            textPaint.setColor(colorAccent); textPaint.setTextSize(20f); textPaint.setTypeface(Typeface.DEFAULT);
            canvas.drawText(t("Configuration", "模块参数配置"), x, y + 30f, textPaint);

            canvas.save();
            canvas.clipRect(rightPanelRect.left, y + 40f, rightPanelRect.right, rightPanelRect.bottom - 20f);
            canvas.translate(0, rightPanelScrollY);
            drawModuleSettings(canvas, activePanelModule, rightPanelRect.left - 10f, y + 40f, pW, true);
            canvas.restore();
        }
    }

    private void drawModuleSettings(Canvas canvas, Module m, float x, float y, float w, boolean inSidePanel) {
        float curY = y + 10f;
        float contentX = inSidePanel ? x + 40f : x + 35f;
        float contentW = inSidePanel ? w - 80f : w - 70f;
        
        for (Setting s : m.settings) {
            if (s instanceof Slider || s instanceof ColorPicker) {
                float sliderY = curY + 20f; float sEndX = contentX + contentW;
                
                s.hitbox.set(contentX - 40f, sliderY - 30f + (inSidePanel ? rightPanelScrollY : scrollY), sEndX + 40f, sliderY + 50f + (inSidePanel ? rightPanelScrollY : scrollY));

                textPaint.setColor(colorTextSub); textPaint.setTextSize(22f); 
                canvas.drawText(t(s.nameEn, s.nameZh), contentX, sliderY, textPaint);

                float trackTop = sliderY + 8f; float trackBottom = sliderY + 26f; 
                float valBoxWidth = 70f; float trackEndX = sEndX - valBoxWidth - 15f;
                Paint.FontMetrics fm = textPaint.getFontMetrics();
                float valTextY = sliderY + 18f - (fm.descent + fm.ascent)/2f;

                if (s instanceof ColorPicker) {
                    ColorPicker cp = (ColorPicker) s;
                    paint.setColor(isLightTheme ? Color.parseColor("#E5E5EA") : Color.parseColor("#1B1B1E"));
                    canvas.drawRoundRect(new RectF(sEndX - valBoxWidth, sliderY, sEndX, sliderY + 36f), 8f, 8f, paint);
                    String valStr = String.valueOf((int)cp.hue);
                    textPaint.setColor(colorTextMain); textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                    canvas.drawText(valStr, sEndX - valBoxWidth/2f - textPaint.measureText(valStr)/2f, valTextY, textPaint);

                    LinearGradient lg = new LinearGradient(contentX, trackTop, trackEndX, trackTop, new int[]{Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED}, null, Shader.TileMode.CLAMP);
                    paint.setShader(lg); paint.setStrokeWidth(16f); paint.setStrokeCap(Paint.Cap.ROUND);
                    canvas.drawLine(contentX+8f, trackTop+8f, trackEndX-8f, trackTop+8f, paint); paint.setShader(null);
                    
                    float pX = contentX + ((trackEndX - contentX) * (cp.hue / 360f));
                    paint.setColor(Color.WHITE); paint.setShadowLayer(4f, 0, 2f, Color.parseColor("#80000000"));
                    canvas.drawRoundRect(new RectF(pX - 4f, trackTop - 3f, pX + 4f, trackBottom + 3f), 4f, 4f, paint); paint.clearShadowLayer();
                } else {
                    Slider sl = (Slider) s;
                    paint.setColor(isLightTheme ? Color.parseColor("#E5E5EA") : Color.parseColor("#38393F"));
                    canvas.drawRoundRect(new RectF(sEndX - valBoxWidth, sliderY, sEndX, sliderY + 36f), 8f, 8f, paint);
                    String valStr = String.valueOf(sl.val);
                    textPaint.setColor(colorTextMain); textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                    canvas.drawText(valStr, sEndX - valBoxWidth/2f - textPaint.measureText(valStr)/2f, valTextY, textPaint);

                    paint.setColor(isLightTheme ? Color.parseColor("#E5E5EA") : Color.parseColor("#1B1B1E")); 
                    canvas.drawRoundRect(new RectF(contentX, trackTop, trackEndX, trackBottom), 6f, 6f, paint);
                    
                    float pX = contentX + ((trackEndX - contentX) * (sl.val / (float)sl.max));
                    if (pX > contentX) {
                        paint.setColor(colorAccent); 
                        canvas.drawRoundRect(new RectF(contentX, trackTop, pX, trackBottom), 6f, 6f, paint);
                    }
                    paint.setColor(Color.parseColor("#F5F5F7"));
                    paint.setShadowLayer(4f, 0, 0, Color.parseColor("#60000000"));
                    canvas.drawRoundRect(new RectF(pX - 4f, trackTop - 3f, pX + 4f, trackBottom + 3f), 4f, 4f, paint);
                    paint.clearShadowLayer();
                }
                curY += 55f; 
            } else if (s instanceof Mode) {
                Mode md = (Mode) s;
                float mY = curY + 20f;
                textPaint.setColor(colorTextSub); textPaint.setTextSize(22f); textPaint.setTypeface(Typeface.DEFAULT);
                canvas.drawText(t(md.nameEn, md.nameZh), contentX, mY, textPaint);

                int cols = inSidePanel ? 2 : 3;
                float btnW = (contentW - (cols - 1) * 15f) / cols;
                float bX = contentX; float btnY = mY + 15f;

                for (int i = 0; i < md.modesEn.length; i++) {
                    int row = i / cols; int col = i % cols;
                    float rX = bX + col * (btnW + 15f);
                    float rY = btnY + row * 55f;
                    
                    md.modeRects[i].set(rX, rY + (inSidePanel ? rightPanelScrollY : scrollY), rX + btnW, rY + 45f + (inSidePanel ? rightPanelScrollY : scrollY));
                    
                    int bgBtnColor = isLiquidGlass ? (isLightTheme ? Color.parseColor("#B3FFFFFF") : Color.parseColor("#4026272B")) : (isLightTheme ? Color.WHITE : Color.parseColor("#1B1B1E"));
                    paint.setColor(i == md.current ? colorAccent : bgBtnColor); 
                    if(isLightTheme && i != md.current && !isLiquidGlass) paint.setShadowLayer(5f,0,2f,Color.parseColor("#15000000"));
                    canvas.drawRoundRect(new RectF(rX, rY, rX + btnW, rY + 45f), 12f, 12f, paint); paint.clearShadowLayer();
                    
                    textPaint.setColor(i == md.current ? Color.WHITE : (isLightTheme ? Color.parseColor("#55555A") : colorTextSub)); 
                    textPaint.setTextAlign(Paint.Align.CENTER); 
                    Paint.FontMetrics fm = textPaint.getFontMetrics();
                    canvas.drawText(t(md.modesEn[i], md.modesZh[i]), rX + btnW / 2f, rY + 22.5f - (fm.descent+fm.ascent)/2f, textPaint); textPaint.setTextAlign(Paint.Align.LEFT);
                }
                int rows = (md.modesEn.length - 1) / cols + 1;
                curY += 35f + rows * 55f;
            }
        }
        
        // 核心恢复：Aui 中找回完美的快捷键按钮
        float sy = curY + 20f;
        m.shortcutHitbox.set(contentX, sy - 15f + (inSidePanel ? rightPanelScrollY : scrollY), contentX + contentW, sy + 35f + (inSidePanel ? rightPanelScrollY : scrollY));
        textPaint.setColor(colorTextSub); textPaint.setTextSize(22f); textPaint.setTypeface(Typeface.DEFAULT);
        canvas.drawText(t("Shortcut Key", "桌面悬浮快捷键"), contentX, sy + 15f, textPaint);

        float sW = 75f, sH = 36f; float sRight = contentX + contentW;
        paint.setColor(m.showShortcut ? colorAccent : (isLightTheme ? Color.parseColor("#E5E5EA") : colorSwitchBg));
        canvas.drawRoundRect(new RectF(sRight - sW, sy - sH/2 + 10f, sRight, sy + sH/2 + 10f), sH/2, sH/2, paint);
        paint.setColor(Color.WHITE); paint.setShadowLayer(4f, 0, 2f, Color.parseColor("#40000000"));
        float cX = m.showShortcut ? (sRight - sH/2) : (sRight - sW + sH/2);
        canvas.drawCircle(cX, sy + 10f, sH/2 - 6f, paint); paint.clearShadowLayer();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isVisible) return false;
        float tx = event.getX(), ty = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = tx; initialTouchY = ty; lastTouchY = ty; isScrolling = false; draggingSetting = null; hasLongPressed = false;

                if (!panelRect.contains(tx, ty) && !searchRect.contains(tx, ty) && !langRect.contains(tx, ty) && !gearHitbox.contains(tx, ty) && !fullScreenHitbox.contains(tx, ty)) {
                    closeGui(); return false; 
                }

                if (isGlobalSettingsOpen && !globalSettingsRect.contains(tx, ty)) { toggleGlobalSettings(); return true; }

                if (rightPanelAnim > 0f && rightPanelRect.contains(tx, ty)) {
                    if (activePanelModule != null) {
                        for (Setting s : activePanelModule.settings) {
                            if ((s instanceof Slider || s instanceof ColorPicker) && s.hitbox.contains(tx, ty)) { 
                                draggingSetting = s; updateSlider(s, tx); return true; 
                            }
                        }
                    }
                    return true;
                }

                for (Module m : allModules) {
                    if (m.cardHitbox.bottom < panelRect.top || m.cardHitbox.top > panelRect.bottom) continue; 
                    if (!isGridLayout && !isPanelSettings && m.isDrawerExpanded) {
                        for (Setting s : m.settings) {
                            if ((s instanceof Slider || s instanceof ColorPicker) && s.hitbox.contains(tx, ty)) { 
                                draggingSetting = s; updateSlider(s, tx); return true; 
                            }
                        }
                    }
                    if (m.cardHitbox.contains(tx, ty) && !m.switchHitbox.contains(tx, ty) && m.hasSettings) {
                        longPressedModule = m; handler.postDelayed(longPressRunnable, 350); return true;
                    }
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(tx - initialTouchX); float dy = Math.abs(ty - initialTouchY);
                
                if (draggingSetting != null) {
                    updateSlider(draggingSetting, tx);
                    return true; 
                }
                
                float moveY = ty - lastTouchY;
                if (!isScrolling && Math.hypot(dx, dy) > 15f) { isScrolling = true; handler.removeCallbacks(longPressRunnable); } 
                if (isScrolling) { 
                    if (rightPanelAnim > 0f && rightPanelRect.contains(tx, ty)) {
                        rightPanelScrollY += moveY;
                        float pH = activePanelModule != null ? activePanelModule.panelHeightMax : 0f;
                        float vH = rightPanelRect.height() - 40f; 
                        maxRightPanelScrollY = pH > vH ? -(pH - vH) : 0f;
                        rightPanelScrollY = Math.max(maxRightPanelScrollY, Math.min(0f, rightPanelScrollY));
                    } else { scrollY += moveY; validateScroll(); }
                    lastTouchY = ty; invalidate(); 
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean isTap = Math.abs(tx - initialTouchX) < 15f && Math.abs(ty - initialTouchY) < 15f;
                handler.removeCallbacks(longPressRunnable);
                if (draggingSetting != null) { draggingSetting = null; return true; }

                if (!isScrolling && event.getAction() == MotionEvent.ACTION_UP && isTap) {
                    
                    if (rightPanelAnim > 0f && rightPanelRect.contains(tx, ty)) {
                        if (activePanelModule != null) {
                            if (activePanelModule.shortcutHitbox.contains(tx, ty)) {
                                activePanelModule.showShortcut = !activePanelModule.showShortcut;
                                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                                if (onModuleToggled != null) onModuleToggled.run(); invalidate(); return true;
                            }
                            for (Setting s : activePanelModule.settings) {
                                if (s instanceof Mode) {
                                    Mode md = (Mode) s;
                                    for (int i=0; i<md.modesEn.length; i++) if (md.modeRects[i].contains(tx, ty)) { md.current = i; performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); if (onModuleToggled != null) onModuleToggled.run(); invalidate(); return true; }
                                }
                            }
                        }
                        return true; 
                    }

                    if (isGlobalSettingsOpen && globalSettingsAnim > 0f) {
                        if (btnDarkRect.contains(tx, ty) && isLightTheme) { toggleTheme(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); }
                        if (btnLightRect.contains(tx, ty) && !isLightTheme) { toggleTheme(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); }
                        if (btnSolidRect.contains(tx, ty) && isLiquidGlass) { toggleGlass(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); }
                        if (btnGlassRect.contains(tx, ty) && !isLiquidGlass) { toggleGlass(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); }
                        if (btnListRect.contains(tx, ty) && isGridLayout) { isGridLayout = false; switchLayoutMode(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); validateScroll(); }
                        if (btnGridRect.contains(tx, ty) && !isGridLayout) { isGridLayout = true; switchLayoutMode(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); validateScroll(); }
                        if (btnDrawerRect.contains(tx, ty) && isPanelSettings && !isGridLayout) { isPanelSettings = false; performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); invalidate(); }
                        if (btnPanelRect.contains(tx, ty) && (!isPanelSettings || isGridLayout)) { isPanelSettings = true; performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); invalidate(); }
                        if (btnGuiAui.contains(tx, ty) && guiMode != 0) { guiMode = 0; if(onGuiModeSwitched!=null)onGuiModeSwitched.run(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); }
                        if (btnGuiBui.contains(tx, ty) && guiMode != 1) { guiMode = 1; if(onGuiModeSwitched!=null)onGuiModeSwitched.run(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); }
                        if (btnFloatRect.contains(tx, ty) && triggerMode != 0) { triggerMode = 0; if(onTriggerChanged!=null)onTriggerChanged.run(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); invalidate(); }
                        if (btnVolRect.contains(tx, ty) && triggerMode != 1) { triggerMode = 1; if(onTriggerChanged!=null)onTriggerChanged.run(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); invalidate(); }
                        if (btnIslandRect.contains(tx, ty) && triggerMode != 2) { triggerMode = 2; if(onTriggerChanged!=null)onTriggerChanged.run(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); invalidate(); }
                        
                        if (btnShortSmall.contains(tx, ty) && shortcutSize != 0) { shortcutSize = 0; if(onModuleToggled!=null)onModuleToggled.run(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); invalidate(); }
                        if (btnShortMid.contains(tx, ty) && shortcutSize != 1) { shortcutSize = 1; if(onModuleToggled!=null)onModuleToggled.run(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); invalidate(); }
                        if (btnShortLarge.contains(tx, ty) && shortcutSize != 2) { shortcutSize = 2; if(onModuleToggled!=null)onModuleToggled.run(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); invalidate(); }
                        return true;
                    }

                    if (gearHitbox.contains(tx, ty)) { toggleGlobalSettings(); performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); return true; }
                    if (fullScreenHitbox.contains(tx, ty)) { toggleFullscreen(); performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); return true; }
                    if (langRect.contains(tx, ty)) { isEnglish = !isEnglish; performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); invalidate(); if(onModuleToggled!=null)onModuleToggled.run(); return true; }
                    if (searchRect.contains(tx, ty) && onSearchRequest != null) { onSearchRequest.run(); return true; }
                    
                    for (int i = 0; i < CAT_EN.length; i++) {
                        if (categoryRects[i].contains(tx, ty) && i != currentCategoryIndex) { searchText = ""; switchCategory(i); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); return true; }
                    }

                    for (Module m : allModules) {
                        boolean matchSearch = !searchText.isEmpty() && (m.nameEn.toLowerCase().contains(searchText) || m.nameZh.toLowerCase().contains(searchText));
                        boolean matchCat = searchText.isEmpty() && m.categoryIndex == currentCategoryIndex;
                        if (!(matchSearch || matchCat) || m.cardHitbox.bottom < panelRect.top || m.cardHitbox.top > panelRect.bottom) continue; 

                        if (!isGridLayout && !isPanelSettings && m.isDrawerExpanded) {
                            if (m.shortcutHitbox.contains(tx, ty)) {
                                m.showShortcut = !m.showShortcut; performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                                if (onModuleToggled != null) onModuleToggled.run(); invalidate(); return true;
                            }
                            for (Setting s : m.settings) {
                                if (s instanceof Mode) {
                                    Mode md = (Mode) s;
                                    for (int i=0; i<md.modesEn.length; i++) if (md.modeRects[i].contains(tx, ty)) { md.current = i; performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); if (onModuleToggled != null) onModuleToggled.run(); 
                                    if(m.nameEn.equals("Notifications")||m.nameEn.equals("ArrayList")||m.nameEn.equals("DynamicIsland")) NotificationView.show("Preview", "Settings Preview...", true);
                                    invalidate(); return true; }
                                }
                            }
                        }

                        if (m.cardHitbox.contains(tx, ty)) {
                            if (m.switchHitbox.contains(tx, ty)) {
                                toggleModule(m); return true;
                            } 
                            else if (!hasLongPressed && m.hasSettings) { 
                                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                                if (isPanelSettings || isGridLayout) openRightPanel(m); 
                                else { m.isDrawerExpanded = !m.isDrawerExpanded; animateDrawer(m, m.isDrawerExpanded); }
                            }
                            return true;
                        }
                    }
                }
                isScrolling = false; break;
        }
        return true; 
    }

    private void updateSlider(Setting s, float x) {
        float trackLeft = s.hitbox.left + 8f; float trackRight = s.hitbox.right - 85f;
        float r = Math.max(0f, Math.min(1f, (x - trackLeft) / (trackRight - trackLeft))); 
        if (s instanceof Slider) { Slider sl = (Slider) s; sl.val = Math.round(r * sl.max); } 
        else if (s instanceof ColorPicker) { ColorPicker cp = (ColorPicker) s; cp.hue = r * 360f; }
        if (onModuleToggled != null) onModuleToggled.run();
        invalidate();
    }
    
    private void animateDrawer(Module m, boolean expand) { ValueAnimator va = ValueAnimator.ofFloat(m.currentDrawerAnim, expand ? m.drawerHeightMax : 0f); va.setDuration(300); va.setInterpolator(new DecelerateInterpolator()); va.addUpdateListener(a -> { m.currentDrawerAnim = (float) a.getAnimatedValue(); validateScroll(); invalidate(); }); va.start(); }
    private void validateScroll() { scrollY = Math.max(maxScrollY, Math.min(0f, scrollY)); }
    private int blendColors(int c1, int c2, float r) { return Color.argb((int)(Color.alpha(c1)+(Color.alpha(c2)-Color.alpha(c1))*r), (int)(Color.red(c1)+(Color.red(c2)-Color.red(c1))*r), (int)(Color.green(c1)+(Color.green(c2)-Color.green(c1))*r), (int)(Color.blue(c1)+(Color.blue(c2)-Color.blue(c1))*r)); }

    public static abstract class Setting { String nameEn, nameZh; RectF hitbox = new RectF(); Setting(String nE, String nZ) { nameEn=nE; nameZh=nZ; } }
    public static class Slider extends Setting { public int val, max; public boolean isDragging = false; public Slider(String nE, String nZ, int min, int max, int val) { super(nE, nZ); this.max=max; this.val=val; } }
    public static class Mode extends Setting { public String[] modesEn, modesZh; public int current; public RectF[] modeRects = new RectF[10]; public Mode(String nE, String nZ, String[] mE, String[] mZ, int def) { super(nE, nZ); modesEn=mE; modesZh=mZ; current=def; for(int i=0;i<10;i++) modeRects[i]=new RectF(); } }
    public static class ColorPicker extends Setting { public float hue; public ColorPicker(String nE, String nZ, float h) { super(nE, nZ); hue=h; } }

    public static class Module {
        public String nameEn, nameZh, descEn, descZh; public int categoryIndex; public boolean enabled; public float animProgress; 
        public RectF cardHitbox = new RectF(), switchHitbox = new RectF(), shortcutHitbox = new RectF();
        public boolean hasSettings; public boolean isDrawerExpanded = false, showShortcut = false; 
        public float shortcutX = -1f, shortcutY = -1f; 
        public float currentDrawerAnim = 0f, drawerHeightMax = 0f, panelHeightMax = 0f;
        public List<Setting> settings = new ArrayList<>();

        public Module(String nEn, String nZh, String dEn, String dZh, int cIdx, boolean e) {
            nameEn = nEn; nameZh = nZh; descEn = dEn; descZh = dZh; categoryIndex = cIdx; enabled = e; animProgress = e ? 1f : 0f;
        }

        public void calcHeights() {
            drawerHeightMax = 20f; panelHeightMax = 20f;
            for(Setting s : settings) {
                if (s instanceof Slider || s instanceof ColorPicker) { drawerHeightMax += 70f; panelHeightMax += 70f; }
                else if (s instanceof Mode) {
                    int dRows = (((Mode)s).modesEn.length - 1) / 3 + 1; 
                    int pRows = (((Mode)s).modesEn.length - 1) / 2 + 1; 
                    drawerHeightMax += 35f + dRows * 55f;
                    panelHeightMax += 35f + pRows * 55f;
                }
            }
            drawerHeightMax += 20f; 
            panelHeightMax += 150f; 
            hasSettings = !settings.isEmpty();
        }
    }
}
