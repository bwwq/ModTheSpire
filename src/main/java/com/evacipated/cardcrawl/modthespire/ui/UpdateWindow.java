package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.DownloadAndRestarter;
import com.evacipated.cardcrawl.modthespire.ModUpdate;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

class UpdateWindow extends JDialog
{
    UpdateWindow(JFrame parent)
    {
        super(parent);
        setModal(true);
        initUI();
    }

    private void initUI()
    {
        if (ModSelectWindow.MODUPDATES.size() == 1) {
            setTitle("Update Available");
        } else {
            setTitle(ModSelectWindow.MODUPDATES.size() + " Updates Available");
        }
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(true);

        getContentPane().setPreferredSize(new Dimension(400, 300));

        rootPane.setBorder(new EmptyBorder(12, 12, 12, 12));

        setLayout(new BorderLayout(0, 12));

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        list.setCellRenderer(new ModUpdateCellRenderer());
        JScrollPane modScroller = new JScrollPane(list);
        modScroller.setBorder(BorderFactory.createLineBorder(ThemeManager.BORDER_DEFAULT));
        getContentPane().add(modScroller, BorderLayout.CENTER);

        for (ModUpdate update : ModSelectWindow.MODUPDATES) {
            model.addElement(update.info.Name);
        }

        String tmp;
        if (ModSelectWindow.MODUPDATES.size() == 1) {
            tmp = "The following mod has an update available:";
        } else {
            tmp = "The following mods have updates available:";
        }
        JLabel headerLabel = new JLabel(tmp);
        headerLabel.setIcon(ModSelectWindow.ICON_WARNING);
        headerLabel.setForeground(ThemeManager.TEXT_PRIMARY);
        getContentPane().add(headerLabel, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        JButton downloadBtn = new JButton("Download Updates and Restart ModTheSpire");
        downloadBtn.putClientProperty("JButton.buttonType", "default");
        JButton browserBtn = new JButton("Open Releases in Browser");
        btnPanel.add(downloadBtn);
        btnPanel.add(browserBtn);
        getContentPane().add(btnPanel, BorderLayout.SOUTH);

        // Open each update's release url in browser
        browserBtn.addActionListener((ActionEvent event) -> {
            if (Desktop.isDesktopSupported()) {
                for (ModUpdate update : ModSelectWindow.MODUPDATES) {
                    try {
                        Desktop.getDesktop().browse(update.releaseURL.toURI());
                    } catch (IOException | URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // Download each update
        downloadBtn.addActionListener((ActionEvent event) -> {
            getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            URL[] downloadURLs = new URL[ModSelectWindow.MODUPDATES.size()];
            for (int i=0; i<ModSelectWindow.MODUPDATES.size(); ++i) {
                downloadURLs[i] = ModSelectWindow.MODUPDATES.get(i).downloadURL;
            }
            try {
                DownloadAndRestarter.downloadAndRestart(downloadURLs);
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        });

        pack();
        setLocationRelativeTo(getParent());
    }

    private static class ModUpdateCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setIcon(ModSelectWindow.ICON_UPDATE);
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            return this;
        }
    }
}
