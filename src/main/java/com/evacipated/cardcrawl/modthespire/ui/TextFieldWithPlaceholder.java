package com.evacipated.cardcrawl.modthespire.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class TextFieldWithPlaceholder extends JTextField {
    private String placeholder;

    public TextFieldWithPlaceholder() {
        this.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                TextFieldWithPlaceholder.this.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                TextFieldWithPlaceholder.this.repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (getText().isEmpty() && !isFocusOwner()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(ThemeManager.TEXT_DISABLED);
            g2.setFont(getFont().deriveFont(Font.ITALIC));

            // 计算垂直居中位置
            FontMetrics fm = g2.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            int x = getInsets().left + 2;

            g2.drawString(placeholder, x, y);
            g2.dispose();
        }
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        // 同时设置 FlatLaf 的 placeholder 属性
        putClientProperty("JTextField.placeholderText", placeholder);
        this.revalidate();
        this.repaint();
    }
}
