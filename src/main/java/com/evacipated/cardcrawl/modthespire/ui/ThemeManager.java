package com.evacipated.cardcrawl.modthespire.ui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;

/**
 * ModTheSpire 主题管理器
 * 负责初始化 FlatLaf 暗色主题并自定义颜色
 */
public final class ThemeManager {

    // 品牌色 - 紫色（呼应杀戮尖塔的紫色主题）
    public static final Color ACCENT_COLOR = new Color(156, 136, 255);
    public static final Color ACCENT_HOVER = new Color(176, 156, 255);
    public static final Color ACCENT_PRESSED = new Color(136, 116, 235);

    // 状态色
    public static final Color STATUS_ERROR = new Color(255, 107, 107);
    public static final Color STATUS_WARNING = new Color(255, 193, 107);
    public static final Color STATUS_SUCCESS = new Color(107, 255, 153);
    public static final Color STATUS_INFO = new Color(107, 193, 255);

    // 背景色
    public static final Color BG_PRIMARY = new Color(43, 43, 43);
    public static final Color BG_SECONDARY = new Color(60, 63, 65);
    public static final Color BG_TERTIARY = new Color(69, 73, 74);
    public static final Color BG_SELECTED = new Color(75, 110, 175);

    // 文本色
    public static final Color TEXT_PRIMARY = new Color(220, 220, 220);
    public static final Color TEXT_SECONDARY = new Color(160, 160, 160);
    public static final Color TEXT_DISABLED = new Color(100, 100, 100);

    // 边框色
    public static final Color BORDER_DEFAULT = new Color(80, 80, 80);
    public static final Color BORDER_FOCUS = ACCENT_COLOR;

    // 缩放设置键名
    public static final String SCALE_CONFIG_KEY = "ui-scale";

    private static boolean flatLafInitialized = false;
    private static float scaleFactor = 1.0f;
    private static float userScaleFactor = -1.0f; // -1 表示自动

    private ThemeManager() {}

    /**
     * 检查 FlatLaf 是否成功初始化
     */
    public static boolean isFlatLafInitialized() {
        return flatLafInitialized;
    }

    /**
     * 获取当前缩放因子
     */
    public static float getScaleFactor() {
        return scaleFactor;
    }

    /**
     * 设置用户自定义缩放因子
     * @param scale 缩放因子，-1 表示自动检测
     */
    public static void setUserScaleFactor(float scale) {
        userScaleFactor = scale;
    }

    /**
     * 获取用户设置的缩放因子
     * @return 缩放因子，-1 表示自动
     */
    public static float getUserScaleFactor() {
        return userScaleFactor;
    }

    /**
     * 根据缩放因子调整尺寸
     */
    public static int scale(int value) {
        return Math.round(value * scaleFactor);
    }

    /**
     * 根据缩放因子调整浮点尺寸
     */
    public static float scaleF(float value) {
        return value * scaleFactor;
    }

    /**
     * 初始化 FlatLaf 暗色主题
     * 必须在创建任何 Swing 组件之前调用
     */
    public static void initialize() {
        try {
            // 检测和设置缩放
            detectAndSetScaleFactor();

            // 使用 FlatLaf.setup() 进行初始化
            if (FlatDarkLaf.setup()) {
                flatLafInitialized = true;

                // 设置全局字体
                setGlobalFont();

                // 自定义 UI 属性
                UIManager.put("Component.focusWidth", 1);
                UIManager.put("Component.innerFocusWidth", 0);
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arc", 8);
                UIManager.put("TextComponent.arc", 8);
                UIManager.put("ScrollBar.width", scale(12));
                UIManager.put("ScrollBar.thumbArc", 999);
                UIManager.put("ScrollBar.trackArc", 999);

                // 自定义颜色
                UIManager.put("Component.accentColor", ACCENT_COLOR);
                UIManager.put("Button.default.focusColor", ACCENT_COLOR);
                UIManager.put("Button.default.borderColor", ACCENT_COLOR);
                UIManager.put("CheckBox.icon.focusColor", ACCENT_COLOR);
                UIManager.put("List.selectionBackground", BG_SELECTED);
                UIManager.put("List.selectionInactiveBackground", new Color(60, 80, 110));

                // 标题边框颜色
                UIManager.put("TitledBorder.titleColor", TEXT_PRIMARY);

                System.out.println("FlatLaf 主题初始化成功，缩放因子: " + scaleFactor);
            } else {
                System.err.println("FlatLaf.setup() 返回 false，回退到系统主题");
                fallbackToSystemLookAndFeel();
            }
        } catch (Throwable t) {
            System.err.println("FlatLaf 初始化失败: " + t.getMessage());
            t.printStackTrace();
            fallbackToSystemLookAndFeel();
        }
    }

    /**
     * 设置全局字体
     */
    private static void setGlobalFont() {
        // 基础字体大小 13pt，根据缩放因子调整
        int scaledFontSize = Math.round(13 * scaleFactor);

        Font defaultFont = new Font("Microsoft YaHei UI", Font.PLAIN, scaledFontSize);
        Font boldFont = new Font("Microsoft YaHei UI", Font.BOLD, scaledFontSize);

        FontUIResource fontResource = new FontUIResource(defaultFont);
        FontUIResource boldFontResource = new FontUIResource(boldFont);

        // 只设置必要的字体属性
        UIManager.put("defaultFont", fontResource);

        System.out.println("设置全局字体大小: " + scaledFontSize + "pt");
    }

    /**
     * 检测系统 DPI 并设置缩放因子
     */
    private static void detectAndSetScaleFactor() {
        // 如果用户设置了自定义缩放
        if (userScaleFactor > 0) {
            scaleFactor = userScaleFactor;
            System.out.println("使用用户设置的缩放因子: " + scaleFactor);
            return;
        }

        try {
            // 获取屏幕 DPI
            int screenDpi = Toolkit.getDefaultToolkit().getScreenResolution();
            // 标准 DPI 是 96
            float detectedScale = screenDpi / 96.0f;

            // 也检查系统缩放设置
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();

            double scaleX = gc.getDefaultTransform().getScaleX();
            double scaleY = gc.getDefaultTransform().getScaleY();
            float transformScale = (float) Math.max(scaleX, scaleY);

            // 使用较大的缩放因子
            scaleFactor = Math.max(detectedScale, transformScale);

            // 确保缩放因子在合理范围内
            scaleFactor = Math.max(1.0f, Math.min(4.0f, scaleFactor));

            System.out.println("检测到屏幕 DPI: " + screenDpi + ", 缩放因子: " + scaleFactor);

        } catch (Exception e) {
            System.err.println("检测 DPI 缩放失败: " + e.getMessage());
            scaleFactor = 1.0f;
        }
    }

    /**
     * 回退到系统默认外观
     */
    private static void fallbackToSystemLookAndFeel() {
        flatLafInitialized = false;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            System.out.println("已回退到系统默认主题");
        } catch (Exception ex) {
            System.err.println("系统主题设置也失败: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
