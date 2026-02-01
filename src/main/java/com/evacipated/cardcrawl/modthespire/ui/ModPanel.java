package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.steam.SteamSearch;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class ModPanel extends JPanel
{
    // 暗色主题下的状态背景色
    private static final Color ERROR_BG = new Color(80, 40, 40);
    private static final Color WARNING_BG = new Color(80, 60, 30);

    public ModInfo info;
    public File modFile;
    public JCheckBox checkBox;
    InfoPanel infoPanel;
    private JLabel update = new JLabel();
    private boolean isFilteredOut = false;

    private static boolean dependenciesChecked(ModInfo info, JModPanelCheckBoxList parent) {
        String[] dependencies = info.Dependencies;
        boolean[] checked = new boolean[dependencies.length]; // initializes to false
        for (int i = 0; i < parent.getModel().getSize(); i++) {
            ModPanel panel = parent.getModel().getElementAt(i);
            for (int j = 0; j < dependencies.length; j++) {
                if (panel.info != null && panel.info.ID != null && panel.info.ID.equals(dependencies[j]) && panel.checkBox.isSelected()) {
                    checked[j] = true;
                }
            }
        }
        boolean allChecked = true;
        for (int i = 0; i < checked.length; i++) {
            if (!checked[i]) {
                allChecked = false;
            }
        }

        return allChecked;
    }

    private static String[] missingDependencies(ModInfo info, JModPanelCheckBoxList parent) {
        String[] dependencies = info.Dependencies;
        boolean[] checked = new boolean[dependencies.length]; // initializes to false
        for (int i = 0; i < parent.getModel().getSize(); i++) {
            ModPanel panel = parent.getModel().getElementAt(i);
            for (int j = 0; j < dependencies.length; j++) {
                if (panel.info != null && panel.info.ID != null && panel.info.ID.equals(dependencies[j]) && panel.checkBox.isSelected()) {
                    checked[j] = true;
                }
            }
        }
        java.util.List<String> missing = new ArrayList<String>();
        for (int i = 0; i < checked.length; i++) {
            if (!checked[i]) {
                missing.add(dependencies[i]);
            }
        }
        String[] returnType = new String[missing.size()];
        return missing.toArray(returnType);
    }

    public ModPanel(ModInfo info, File modFile, JModPanelCheckBoxList parent) {
        this.info = info;
        this.modFile = modFile;
        checkBox = new JCheckBox();
        checkBox.setOpaque(false);

        setLayout(new BorderLayout(6, 0));
        setOpaque(true);
        setBackground(ThemeManager.BG_SECONDARY);
        setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, ThemeManager.BORDER_DEFAULT),
            new EmptyBorder(6, 8, 6, 8)
        ));

        infoPanel = new InfoPanel();

        add(checkBox, BorderLayout.WEST);
        add(infoPanel, BorderLayout.CENTER);

        // Update icon
        update.setHorizontalAlignment(JLabel.CENTER);
        update.setVerticalAlignment(JLabel.CENTER);
        update.setOpaque(false);
        update.setBorder(new EmptyBorder(0, 4, 0, 0));
        if (info.isWorkshop) {
            setUpdateIcon(ModSelectWindow.UpdateIconType.WORKSHOP);
        } else if (info.UpdateJSON != null && !info.UpdateJSON.isEmpty()) {
            setUpdateIcon(ModSelectWindow.UpdateIconType.CAN_CHECK);
        } else {
            setUpdateIcon(ModSelectWindow.UpdateIconType.NONE);
        }
        add(update, BorderLayout.EAST);

        checkBox.addItemListener((event) -> {
            parent.publishBoxChecked();
        });
        parent.publishBoxChecked();
    }

    public void recalcModWarnings(JModPanelCheckBoxList parent)
    {
        info.statusMsg = " ";
        setBackground(ThemeManager.BG_SECONDARY);
        infoPanel.resetColors();

        if (info.MTS_Version == null) {
            checkBox.setEnabled(false);
            setBackground(ERROR_BG);
            infoPanel.setWarningColors(ThemeManager.STATUS_ERROR);
            info.statusMsg = "This mod is missing a valid ModTheSpire version number.";
            return;
        }
        if (info.MTS_Version.compareTo(Loader.MTS_VERSION) > 0) {
            checkBox.setEnabled(false);
            setBackground(ERROR_BG);
            infoPanel.setWarningColors(ThemeManager.STATUS_ERROR);
            info.statusMsg = "This mod requires ModTheSpire v" + info.MTS_Version + " or higher.";
            return;
        }

        if (checkBox.isSelected() && !dependenciesChecked(info, parent)) {
            setBackground(WARNING_BG);
            infoPanel.setWarningColors(ThemeManager.STATUS_WARNING);
            String[] missingDependencies = missingDependencies(info, parent);
            StringBuilder tooltip = new StringBuilder();
            tooltip.append("Missing dependencies: [");
            tooltip.append(String.join(", ", missingDependencies));
            tooltip.append("]");
            info.statusMsg = tooltip.toString();
        }
        if (Loader.STS_VERSION != null && info.STS_Version != null && !Loader.STS_VERSION.equals(info.STS_Version)) {
            if (info.statusMsg == " ") {
                info.statusMsg = "This mod explicitly supports StS " + info.STS_Version + ".\n" +
                    "You are running StS " + Loader.STS_VERSION + ".\n" +
                    "You may encounter problems running it.";
            }
        }
    }

    public boolean isSelected()
    {
        return checkBox.isEnabled() && checkBox.isSelected();
    }

    public void setSelected(boolean b)
    {
        if (checkBox.isEnabled()) {
            checkBox.setSelected(b);
        }
    }

    public synchronized void setUpdateIcon(ModSelectWindow.UpdateIconType type)
    {
        switch (type) {
            case NONE:
                update.setIcon(null);
                break;
            case CAN_CHECK:
                update.setIcon(ModSelectWindow.ICON_UPDATE);
                break;
            case CHECKING:
                update.setIcon(ModSelectWindow.ICON_LOAD);
                break;
            case UPDATE_AVAILABLE:
                update.setIcon(ModSelectWindow.ICON_WARNING);
                break;
            case UPTODATE:
                update.setIcon(ModSelectWindow.ICON_GOOD);
                break;
            case WORKSHOP:
                update.setIcon(ModSelectWindow.ICON_WORKSHOP);
        }
    }

    public void filter(String[] filterKeys) {
        if (filterKeys == null) {
            isFilteredOut = false;
            return;
        }

        String workshopInfoKey = "";

        try {
            if (info.isWorkshop) {
                // WorkshopId is in hex while workshop mod folder name is in dec.
                String workshopId = Long.toString(Long.parseLong(this.modFile.getParentFile().getName()), 16);
                SteamSearch.WorkshopInfo workshopInfo = Loader.getWorkshopInfos().stream().filter(i -> i.getID().equals(workshopId)).findFirst().orElse(null);
                if (workshopInfo != null) {
                    workshopInfoKey = String.format("%s %s", workshopInfo.getTitle(), String.join(" ", workshopInfo.getTags()));
                }
            }
        } catch (Exception ex) {
            System.out.println("ModPanel.filter failed to get workshop info of " + info.ID + ": " + ex);
        }

        String modInfoKey = String.format("%s %s %s %s", info.ID, info.Name, String.join(" ", info.Authors), workshopInfoKey).toLowerCase();
        boolean isFilteredOut = false;
        for (String filterKey : filterKeys) {
            if (!modInfoKey.contains(filterKey)) {
                isFilteredOut = true;
                break;
            }
        }
        this.isFilteredOut = isFilteredOut;
    }

    public boolean isFilteredOut() {
        return isFilteredOut;
    }

    public class InfoPanel extends JPanel
    {
        JLabel name = new JLabel();
        JLabel version = new JLabel();

        public InfoPanel()
        {
            setLayout(new BorderLayout(0, 2));
            setOpaque(false);

            name.setOpaque(false);
            name.setText(info.Name);
            name.setFont(name.getFont().deriveFont(Font.BOLD, (float) ThemeManager.scale(13)));
            name.setForeground(ThemeManager.TEXT_PRIMARY);
            add(name, BorderLayout.CENTER);

            version.setOpaque(false);
            version.setFont(version.getFont().deriveFont(Font.PLAIN, (float) ThemeManager.scale(11)));
            version.setForeground(ThemeManager.TEXT_SECONDARY);
            if (info.ModVersion != null) {
                version.setText("v" + info.ModVersion.toString());
            } else {
                version.setText("missing version");
                version.setForeground(ThemeManager.STATUS_WARNING);
            }
            add(version, BorderLayout.SOUTH);
        }

        public void resetColors() {
            name.setForeground(ThemeManager.TEXT_PRIMARY);
            if (info.ModVersion != null) {
                version.setForeground(ThemeManager.TEXT_SECONDARY);
            } else {
                version.setForeground(ThemeManager.STATUS_WARNING);
            }
        }

        public void setWarningColors(Color textColor) {
            name.setForeground(textColor);
            version.setForeground(textColor.darker());
        }
    }

}
