package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.*;
import com.evacipated.cardcrawl.modthespire.steam.SteamSearch;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ModSelectWindow extends JFrame
{
    private static final long serialVersionUID = -8232997068791248057L;
    private static final int BASE_WIDTH = 900;
    private static final int BASE_HEIGHT = 560;

    private static int getDefaultWidth() {
        return ThemeManager.scale(BASE_WIDTH);
    }

    private static int getDefaultHeight() {
        return ThemeManager.scale(BASE_HEIGHT);
    }
    private static final String DEBUG_OPTION = "Debug";
    private static final String PLAY_OPTION = "Play";
    private static final String JAR_DUMP_OPTION = "Dump Patched Jar";
    private static final String PACKAGE_OPTION = "Package";

    static final Image APP_ICON = Toolkit.getDefaultToolkit().createImage(ModSelectWindow.class.getResource("/assets/icon.png"));
    static final Icon ICON_UPDATE   = new ImageIcon(ModSelectWindow.class.getResource("/assets/update.gif"));
    static final Icon ICON_LOAD     = new ImageIcon(ModSelectWindow.class.getResource("/assets/ajax-loader.gif"));
    static final Icon ICON_GOOD     = new ImageIcon(ModSelectWindow.class.getResource("/assets/good.gif"));
    static final Icon ICON_WARNING  = new ImageIcon(ModSelectWindow.class.getResource("/assets/warning.gif"));
    static final Icon ICON_ERROR    = new ImageIcon(ModSelectWindow.class.getResource("/assets/error.gif"));
    static final Icon ICON_WORKSHOP = new ImageIcon(ModSelectWindow.class.getResource("/assets/workshop.gif"));

    private ModInfo[] info;
    private boolean showingLog = false;
    private boolean isMaximized = false;
    private boolean isCentered = false;
    private Rectangle location;
    private JButton playBtn;

    private JModPanelCheckBoxList modList;

    private ModInfo currentModInfo;
    private TitledBorder name;
    private JTextArea authors;
    private JLabel modVersion;
    private JTextArea status;
    private JLabel mtsVersion;
    private JLabel stsVersion;
    private JTextArea description;
    private JTextArea credits;

    private JPanel bannerNoticePanel;
    private JLabel mtsUpdateBanner;
    private JLabel betaWarningBanner;

    private JPanel modBannerNoticePanel;
    private JLabel modUpdateBanner;

    static List<ModUpdate> MODUPDATES;

    public enum UpdateIconType
    {
        NONE, CAN_CHECK, CHECKING, UPDATE_AVAILABLE, UPTODATE, WORKSHOP
    }

    public static Properties getDefaults()
    {
        Properties properties = new Properties();
        properties.setProperty("x", "center");
        properties.setProperty("y", "center");
        properties.setProperty("width", Integer.toString(BASE_WIDTH));
        properties.setProperty("height", Integer.toString(BASE_HEIGHT));
        properties.setProperty("maximize", Boolean.toString(false));
        return properties;
    }

    public ModSelectWindow(ModInfo[] modInfos, boolean skipLauncher)
    {
        // FlatLaf 主题已在 Loader.main() 中初始化

        setIconImage(APP_ICON);

        info = modInfos;
        readWindowPosSize();
        setupDetectMaximize();
        initUI(skipLauncher);
        if (Loader.MTS_CONFIG.getBool("maximize")) {
            isMaximized = true;
            this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        }
    }

    private void readWindowPosSize()
    {
        // Sanity check values
        if (Loader.MTS_CONFIG.getInt("width") < getDefaultWidth()) {
            Loader.MTS_CONFIG.setInt("width", getDefaultWidth());
        }
        if (Loader.MTS_CONFIG.getInt("height") < getDefaultHeight()) {
            Loader.MTS_CONFIG.setInt("height", getDefaultHeight());
        }
        location = new Rectangle();
        location.width = Loader.MTS_CONFIG.getInt("width");
        location.height = Loader.MTS_CONFIG.getInt("height");
        if (Loader.MTS_CONFIG.getString("x").equals("center") || Loader.MTS_CONFIG.getString("y").equals("center")) {
            isCentered = true;
        } else {
            isCentered = false;
            location.x = Loader.MTS_CONFIG.getInt("x");
            location.y = Loader.MTS_CONFIG.getInt("y");
            if (!isInScreenBounds(location)) {
                Loader.MTS_CONFIG.setString("x", "center");
                Loader.MTS_CONFIG.setString("y", "center");
                isCentered = true;
            }
        }

        try {
            Loader.MTS_CONFIG.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupDetectMaximize()
    {
        ModSelectWindow tmpthis = this;
        this.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                super.componentResized(e);

                if (!showingLog) {
                    Dimension d = tmpthis.getContentPane().getSize();
                    if (!isMaximized) {
                        saveWindowDimensions(d);
                    }
                }
            }

            int skipMoves = 2;

            @Override
            public void componentMoved(ComponentEvent e)
            {
                super.componentMoved(e);

                if (!showingLog && skipMoves == 0) {
                    if (isInScreenBounds(getLocationOnScreen(), getBounds())) {
                        saveWindowLocation();
                    }
                    isCentered = false;
                } else if (skipMoves > 0) {
                    --skipMoves;
                }
            }
        });
        this.addWindowStateListener(new WindowAdapter()
        {
            @Override
            public void windowStateChanged(WindowEvent e)
            {
                super.windowStateChanged(e);

                if (!showingLog) {
                    if ((e.getNewState() & Frame.MAXIMIZED_BOTH) != 0) {
                        isMaximized = true;
                        saveWindowMaximize();
                    } else {
                        isMaximized = false;
                        saveWindowMaximize();
                    }
                }
            }
        });
    }

    private void initUI(boolean skipLauncher)
    {
        setTitle("ModTheSpire " + Loader.MTS_VERSION);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(true);

        rootPane.setBorder(new EmptyBorder(ThemeManager.scale(8), ThemeManager.scale(8), ThemeManager.scale(8), ThemeManager.scale(8)));

        setLayout(new BorderLayout(6, 6));
        getContentPane().setPreferredSize(new Dimension(location.width, location.height));

        // 使用 JSplitPane 支持用户调整左右比例
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(makeModListPanel());
        splitPane.setRightComponent(makeInfoPanel());
        splitPane.setDividerLocation(280);
        splitPane.setDividerSize(6);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(makeTopPanel(), BorderLayout.NORTH);

        pack();
        if (isCentered) {
            setLocationRelativeTo(null);
        } else {
            setLocation(location.getLocation());
        }

        if (skipLauncher) {
            playBtn.doClick();
        } else {
            // Default focus Play button
            JRootPane rootPane = SwingUtilities.getRootPane(playBtn);
            rootPane.setDefaultButton(playBtn);
            EventQueue.invokeLater(playBtn::requestFocusInWindow);
        }
    }

    private JPanel makeModListPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 6));
        panel.setMinimumSize(new Dimension(ThemeManager.scale(250), ThemeManager.scale(300)));
        panel.setPreferredSize(new Dimension(ThemeManager.scale(280), ThemeManager.scale(300)));

        // Mod List
        DefaultListModel<ModPanel> model = new DefaultListModel<>();
        modList = new JModPanelCheckBoxList(this, model);
        ModList mods = ModList.loadModLists();
        mods.loadModsInOrder(model, info, modList);
        modList.publishBoxChecked();

        JScrollPane modScroller = new JScrollPane(modList);
        modScroller.setBorder(BorderFactory.createLineBorder(ThemeManager.BORDER_DEFAULT));
        modScroller.getVerticalScrollBar().setUnitIncrement(ThemeManager.scale(16));
        panel.add(modScroller, BorderLayout.CENTER);

        // Play button
        playBtn = new JButton(
            Loader.PACKAGE ? PACKAGE_OPTION :
                Loader.OUT_JAR ? JAR_DUMP_OPTION :
                PLAY_OPTION
        );
        playBtn.setFont(playBtn.getFont().deriveFont(Font.BOLD, (float) ThemeManager.scale(14)));
        playBtn.setPreferredSize(new Dimension(0, ThemeManager.scale(40)));
        playBtn.putClientProperty("JButton.buttonType", "default");
        playBtn.addActionListener((ActionEvent event) -> {
            showingLog = true;
            playBtn.setEnabled(false);

            this.getContentPane().removeAll();

            JTextArea textArea = new JTextArea();
            textArea.setLineWrap(true);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, ThemeManager.scale(12)));
            textArea.setBackground(ThemeManager.BG_PRIMARY);
            textArea.setForeground(ThemeManager.TEXT_PRIMARY);
            textArea.setCaretColor(ThemeManager.TEXT_PRIMARY);
            JScrollPane logScroller = new JScrollPane(textArea);
            this.getContentPane().add(logScroller, BorderLayout.CENTER);
            MessageConsole mc = new MessageConsole(textArea);
            mc.redirectOut(null, System.out);
            mc.redirectErr(ThemeManager.STATUS_ERROR, System.err);

            setResizable(true);
            pack();
            if (isCentered) {
                setLocationRelativeTo(null);
            }

            Thread tCfg = new Thread(() -> {
                // Save new load order cfg
                ModList.save(ModList.getDefaultList(), modList.getCheckedMods());
            });
            tCfg.start();

            Thread t = new Thread(() -> {
                // Build array of selected mods
                File[] selectedMods;
                if (Loader.manualModIds != null) {
                    selectedMods = modList.getAllMods();
                } else {
                    selectedMods = modList.getCheckedMods();
                }

                Loader.runMods(selectedMods);
                if (Loader.CLOSE_WHEN_FINISHED) {
                    Loader.closeWindow();
                }
            });
            t.start();
        });
        if (Loader.STS_BETA && !Loader.allowBeta) {
            playBtn.setEnabled(false);
        }

        // Open mod directory
        JButton openFolderBtn = new JButton(UIManager.getIcon("FileView.directoryIcon"));
        openFolderBtn.setToolTipText("Open Mods Directory");
        openFolderBtn.addActionListener((ActionEvent event) -> {
            try {
                File file = new File(Loader.MOD_DIR);
                if (!file.exists()) {
                    file.mkdir();
                }
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        // Check for Updates button
        JButton updatesBtn = new JButton(ICON_UPDATE);
        updatesBtn.setToolTipText("Check for Mod Updates");
        updatesBtn.addActionListener(event -> {
            startCheckingForModUpdates(updatesBtn);
        });
        // Toggle all button
        JButton toggleAllBtn = new JButton(UIManager.getIcon("Tree.collapsedIcon"));
        toggleAllBtn.setToolTipText("Toggle all mods On/Off");
        toggleAllBtn.addActionListener((ActionEvent event) -> {
            modList.toggleAllMods();
            repaint();
        });

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(null);
        toolbar.add(updatesBtn);
        toolbar.add(openFolderBtn);
        toolbar.addSeparator();
        toolbar.add(toggleAllBtn);

        JComboBox<String> profilesList = new JComboBox<>(ModList.getAllModListNames().toArray(new String[0]));
        JButton addProfile = new JButton("+");
        JButton delProfile = new JButton("-");

        TextFieldWithPlaceholder filter = new TextFieldWithPlaceholder();
        filter.setPlaceholder("Filter...");

        profilesList.addActionListener((ActionEvent event) -> {
            String profileName = (String) profilesList.getSelectedItem();
            delProfile.setEnabled(!ModList.DEFAULT_LIST.equals(profileName));
            ModList newList = new ModList(profileName);
            DefaultListModel<ModPanel> newModel = (DefaultListModel<ModPanel>) modList.getModel();
            newList.loadModsInOrder(newModel, info, modList);
            filter.setText("");

            Thread tCfg = new Thread(() -> {
                // Save new load order cfg
                ModList.save(profileName, modList.getCheckedMods());
            });
            tCfg.start();
        });
        if (Loader.profileArg != null) {
            profilesList.setSelectedItem(Loader.profileArg);
        } else {
            profilesList.setSelectedItem(ModList.getDefaultList());
        }

        JPanel profilesPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.9;
        profilesPanel.add(profilesList, c);
        c.weightx = 0;
        // Add profile button
        addProfile.setToolTipText("Add new profile");
        addProfile.addActionListener((ActionEvent event) -> {
            String s = JOptionPane.showInputDialog(
                this,
                "Profile Name:",
                "New Profile",
                JOptionPane.PLAIN_MESSAGE
            );
            if (s != null && !s.isEmpty()) {
                profilesList.addItem(s);
                profilesList.setSelectedIndex(profilesList.getItemCount() - 1);
            }
        });
        profilesPanel.add(addProfile, c);
        // Delete profile button
        delProfile.setToolTipText("Delete profile");
        delProfile.addActionListener((ActionEvent event) -> {
            String profileName = (String) profilesList.getSelectedItem();

            int n = JOptionPane.showConfirmDialog(
                this,
                "Are you sure?\nThis action cannot be undone.",
                "Delete Profile \"" + profileName + "\"",
                JOptionPane.YES_NO_OPTION
            );
            if (n == 0) {
                profilesList.removeItem(profileName);
                profilesList.setSelectedItem(ModList.DEFAULT_LIST);
                ModList.delete(profileName);
            }
        });
        profilesPanel.add(delProfile, c);

        Runnable filterModList = () -> {
            String filterText = filter.getText().trim().toLowerCase();
            String[] filterKeys = filterText.length() == 0 ? null : filterText.split("\\s+");
            for (int i = 0; i < model.size(); i++) {
                ModPanel modPanel = model.getElementAt(i);
                modPanel.filter(filterKeys);
            }
            modList.updateUI();
        };
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterModList.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterModList.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterModList.run();
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout(0, 6));
        topPanel.add(toolbar, BorderLayout.NORTH);
        topPanel.add(profilesPanel, BorderLayout.CENTER);
        topPanel.add(filter, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

        // South panel with status and play button
        JPanel southPanel = new JPanel(new BorderLayout(0, 6));
        southPanel.add(makeStatusPanel(), BorderLayout.NORTH);
        southPanel.add(playBtn, BorderLayout.SOUTH);
        panel.add(southPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel makeInfoPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 6));

        // Top mod banner panel
        panel.add(makeModBannerPanel(), BorderLayout.NORTH);

        // Main info panel
        JPanel infoPanel = new JPanel();
        name = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ThemeManager.BORDER_DEFAULT),
            "Mod Info"
        );
        name.setTitleFont(name.getTitleFont().deriveFont(Font.BOLD, (float) ThemeManager.scale(14)));
        name.setTitleColor(ThemeManager.TEXT_PRIMARY);
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            name,
            BorderFactory.createEmptyBorder(ThemeManager.scale(10), ThemeManager.scale(10), ThemeManager.scale(10), ThemeManager.scale(10))
        ));
        infoPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(ThemeManager.scale(4), 0, ThemeManager.scale(4), ThemeManager.scale(8));

        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.4;

        authors = makeInfoTextAreaField("Author(s)", " ");
        infoPanel.add(authors, c);

        c.gridy = 1;
        modVersion = makeInfoLabelField("ModVersion", " ");
        infoPanel.add(modVersion, c);

        c.gridy = 2;
        mtsVersion = makeInfoLabelField("ModTheSpire Version", " ");
        infoPanel.add(mtsVersion, c);

        c.gridy = 3;
        stsVersion = makeInfoLabelField("Slay the Spire Version", " ");
        infoPanel.add(stsVersion, c);

        c.gridy = 4;
        credits = makeInfoTextAreaField("Additional Credits", " ");
        infoPanel.add(credits, c);

        c.gridy = 5;
        c.weighty = 1;
        status = makeInfoTextAreaField("Status", " ");
        infoPanel.add(status, c);

        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 6;
        c.weightx = 0.6;
        c.weighty = 1;
        c.insets = new Insets(ThemeManager.scale(4), 0, ThemeManager.scale(4), ThemeManager.scale(12));
        description = makeInfoTextAreaField("Description", " ");
        JScrollPane descScroller = new JScrollPane(description);
        descScroller.setBorder(description.getBorder());
        descScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        description.setBorder(null);
        infoPanel.add(descScroller, c);

        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    private JLabel makeInfoLabelField(String title, String value)
    {
        JLabel label = new JLabel(value);

        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ThemeManager.BORDER_DEFAULT),
            title
        );
        border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD, (float) ThemeManager.scale(12)));
        border.setTitleColor(ThemeManager.TEXT_SECONDARY);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, (float) ThemeManager.scale(12)));
        label.setForeground(ThemeManager.TEXT_PRIMARY);
        label.setBorder(BorderFactory.createCompoundBorder(
            border,
            BorderFactory.createEmptyBorder(ThemeManager.scale(6), ThemeManager.scale(8), ThemeManager.scale(6), ThemeManager.scale(8))
        ));

        return label;
    }

    private JTextArea makeInfoTextAreaField(String title, String value)
    {
        JTextArea label = new JTextArea(value);

        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ThemeManager.BORDER_DEFAULT),
            title
        );
        border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD, (float) ThemeManager.scale(12)));
        border.setTitleColor(ThemeManager.TEXT_SECONDARY);
        label.setBorder(BorderFactory.createCompoundBorder(
            border,
            BorderFactory.createEmptyBorder(ThemeManager.scale(6), ThemeManager.scale(8), ThemeManager.scale(6), ThemeManager.scale(8))
        ));
        label.setEditable(false);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setLineWrap(true);
        label.setWrapStyleWord(true);
        label.setOpaque(false);
        label.setForeground(ThemeManager.TEXT_PRIMARY);
        label.setFont(border.getTitleFont().deriveFont(Font.PLAIN, (float) ThemeManager.scale(11)));

        return label;
    }

    private JPanel makeModBannerPanel()
    {
        modBannerNoticePanel = new JPanel();
        modBannerNoticePanel.setLayout(new GridLayout(0, 1));
        modBannerNoticePanel.setBorder(BorderFactory.createEmptyBorder(ThemeManager.scale(1), 0, ThemeManager.scale(2), 0));

        modUpdateBanner = new JLabel();
        modUpdateBanner.setIcon(ICON_WARNING);
        modUpdateBanner.setText("<html>" +
            "An update is available for this mod." +
            "</html>");
        modUpdateBanner.setHorizontalAlignment(JLabel.CENTER);
        modUpdateBanner.setOpaque(true);
        modUpdateBanner.setBackground(ThemeManager.STATUS_WARNING);
        modUpdateBanner.setForeground(ThemeManager.BG_PRIMARY);
        modUpdateBanner.setBorder(new EmptyBorder(ThemeManager.scale(8), ThemeManager.scale(8), ThemeManager.scale(8), ThemeManager.scale(8)));

        return modBannerNoticePanel;
    }

    private JPanel makeStatusPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(ThemeManager.scale(10), 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, ThemeManager.BORDER_DEFAULT),
            BorderFactory.createEmptyBorder(ThemeManager.scale(6), 0, 0, 0)
        ));

        // StS version
        JLabel sts_version = new JLabel("Slay the Spire: " + Loader.STS_VERSION);
        sts_version.setForeground(ThemeManager.TEXT_SECONDARY);
        if (Loader.STS_BETA) {
            sts_version.setText(sts_version.getText() + " BETA");
            sts_version.setForeground(ThemeManager.STATUS_WARNING);
        }
        sts_version.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(sts_version, BorderLayout.EAST);

        // Debug checkbox
        JCheckBox debugCheck = new JCheckBox(DEBUG_OPTION);
        debugCheck.setForeground(ThemeManager.TEXT_SECONDARY);
        if (Loader.DEBUG) {
            debugCheck.setSelected(true);
        }
        debugCheck.addActionListener((ActionEvent event) -> {
            Loader.DEBUG = debugCheck.isSelected();
            Loader.MTS_CONFIG.setBool("debug", Loader.DEBUG);
            try {
                Loader.MTS_CONFIG.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // UI Scale selector
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, ThemeManager.scale(10), 0));
        leftPanel.setOpaque(false);
        leftPanel.add(debugCheck);

        JLabel scaleLabel = new JLabel("缩放:");
        scaleLabel.setForeground(ThemeManager.TEXT_SECONDARY);
        leftPanel.add(scaleLabel);

        String[] scaleOptions = {"自动", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x", "2.5x", "3.0x"};
        float[] scaleValues = {-1f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f};
        JComboBox<String> scaleCombo = new JComboBox<>(scaleOptions);
        scaleCombo.setForeground(ThemeManager.TEXT_PRIMARY);

        // 设置当前选中项
        float currentScale = ThemeManager.getUserScaleFactor();
        int selectedIndex = 0;
        for (int i = 0; i < scaleValues.length; i++) {
            if (Math.abs(scaleValues[i] - currentScale) < 0.01f) {
                selectedIndex = i;
                break;
            }
        }
        scaleCombo.setSelectedIndex(selectedIndex);

        scaleCombo.addActionListener((ActionEvent event) -> {
            int idx = scaleCombo.getSelectedIndex();
            float newScale = scaleValues[idx];
            Loader.MTS_CONFIG.setString("ui-scale", String.valueOf(newScale));
            try {
                Loader.MTS_CONFIG.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
            JOptionPane.showMessageDialog(this,
                "缩放设置已保存。\n重启 ModTheSpire 后生效。",
                "提示", JOptionPane.INFORMATION_MESSAGE);
        });
        leftPanel.add(scaleCombo);

        panel.add(leftPanel, BorderLayout.WEST);

        return panel;
    }

    private JPanel makeTopPanel()
    {
        bannerNoticePanel = new JPanel();
        bannerNoticePanel.setLayout(new BoxLayout(bannerNoticePanel, BoxLayout.Y_AXIS));

        if (Loader.STS_BETA) {
            betaWarningBanner = new JLabel();
            betaWarningBanner.setIcon(ICON_ERROR);
            betaWarningBanner.setText("<html>" +
                "You are on the Slay the Spire beta branch.<br/>" +
                "If mods are not working correctly,<br/>" +
                "switch to the main branch for best results." +
                "</html>");
            betaWarningBanner.setHorizontalAlignment(JLabel.CENTER);
            betaWarningBanner.setOpaque(true);
            betaWarningBanner.setBackground(ThemeManager.STATUS_ERROR);
            betaWarningBanner.setForeground(ThemeManager.BG_PRIMARY);
            betaWarningBanner.setBorder(new EmptyBorder(ThemeManager.scale(8), ThemeManager.scale(8), ThemeManager.scale(8), ThemeManager.scale(8)));
            betaWarningBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, betaWarningBanner.getPreferredSize().height));
            bannerNoticePanel.add(betaWarningBanner);
            bannerNoticePanel.add(Box.createVerticalStrut(ThemeManager.scale(4)));
        }

        mtsUpdateBanner = new JLabel();
        mtsUpdateBanner.setIcon(ICON_WARNING);
        mtsUpdateBanner.setText("<html>" +
            "An update for ModTheSpire is available.<br/>" +
            "Click here to open the download page." +
            "</html>");
        mtsUpdateBanner.setHorizontalAlignment(JLabel.CENTER);
        mtsUpdateBanner.setOpaque(true);
        mtsUpdateBanner.setBackground(ThemeManager.STATUS_WARNING);
        mtsUpdateBanner.setForeground(ThemeManager.BG_PRIMARY);
        mtsUpdateBanner.setBorder(new EmptyBorder(ThemeManager.scale(8), ThemeManager.scale(8), ThemeManager.scale(8), ThemeManager.scale(8)));
        mtsUpdateBanner.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mtsUpdateBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, mtsUpdateBanner.getPreferredSize().height));

        return bannerNoticePanel;
    }

    private void setMTSUpdateAvailable(URL url)
    {
        bannerNoticePanel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(url.toURI());
                    } catch (IOException | URISyntaxException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        bannerNoticePanel.add(mtsUpdateBanner);
        pack();
        repaint();
    }

    void saveWindowDimensions(Dimension d)
    {
        Loader.MTS_CONFIG.setInt("width", d.width);
        Loader.MTS_CONFIG.setInt("height", d.height);
        try {
            Loader.MTS_CONFIG.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void saveWindowMaximize()
    {
        Loader.MTS_CONFIG.setBool("maximize", isMaximized);
        try {
            Loader.MTS_CONFIG.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void saveWindowLocation()
    {
        Point loc = getLocationOnScreen();
        Loader.MTS_CONFIG.setInt("x", loc.x);
        Loader.MTS_CONFIG.setInt("y", loc.y);
        try {
            Loader.MTS_CONFIG.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean isInScreenBounds(Point location, Rectangle size)
    {
        size.setLocation(location);
        return isInScreenBounds(size);
    }

    boolean isInScreenBounds(Rectangle location)
    {
        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle bounds = gd.getDefaultConfiguration().getBounds();
            // Expand screen bounds slightly
            bounds.x -= 10;
            bounds.width += 20;
            bounds.y -= 10;
            bounds.height += 20;
            if (bounds.contains(location)) {
                return true;
            }
        }
        return false;
    }

    void setModInfo(ModInfo info)
    {
        currentModInfo = info;

        name.setTitle(info.Name);
        authors.setText(String.join(", ", info.Authors));
        if (info.ModVersion != null) {
            modVersion.setText(info.ModVersion.toString());
        } else {
            modVersion.setText(" ");
        }
        if (info.MTS_Version != null) {
            mtsVersion.setText(info.MTS_Version + "+");
        } else {
            mtsVersion.setText(" ");
        }
        if (info.STS_Version != null && !info.STS_Version.isEmpty()) {
            stsVersion.setText(info.STS_Version);
        } else {
            stsVersion.setText(" ");
        }
        description.setText(info.Description);
        credits.setText(info.Credits);

        status.setText(info.statusMsg);

        setModUpdateBanner(info);

        repaint();
    }

    synchronized void setModUpdateBanner(ModInfo info)
    {
        if (currentModInfo != null && currentModInfo.equals(info)) {
            boolean needsUpdate = false;
            if (MODUPDATES != null) {
                for (ModUpdate modUpdate : MODUPDATES) {
                    if (modUpdate.info.equals(info)) {
                        needsUpdate = true;
                        break;
                    }
                }
            }
            if (needsUpdate) {
                modBannerNoticePanel.add(modUpdateBanner);
            } else {
                modBannerNoticePanel.remove(modUpdateBanner);
            }
        }
    }

    public void startCheckingForMTSUpdate()
    {
        new Thread(() -> {
            try {
                // Check for ModTheSpire updates
                UpdateChecker updateChecker = new GithubUpdateChecker("kiooeht", "ModTheSpire");
                if (updateChecker.isNewerVersionAvailable(Loader.MTS_VERSION)) {
                    URL latestReleaseURL = updateChecker.getLatestReleaseURL();
                    setMTSUpdateAvailable(latestReleaseURL);
                    return;
                }
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: ModTheSpire: " + e.getMessage());
            } catch (IOException e) {
                // NOP
            }
        }).start();
    }

    public void startCheckingForModUpdates(JButton updatesBtn)
    {
        updatesBtn.setIcon(ICON_LOAD);

        new Thread(() -> {
            // Set all icons to checking
            for (int i=0; i<info.length; ++i) {
                if (info[i].UpdateJSON == null || info[i].UpdateJSON.isEmpty()) {
                    continue;
                }

                modList.setUpdateIcon(info[i], UpdateIconType.CHECKING);
            }

            // Check for mod updates
            boolean anyNeedUpdates = false;
            MODUPDATES = new ArrayList<>();
            for (int i=0; i<info.length; ++i) {
                if (info[i].UpdateJSON == null || info[i].UpdateJSON.isEmpty()) {
                    continue;
                }
                try {
                    UpdateChecker updateChecker = new GithubUpdateChecker(info[i].UpdateJSON);
                    if (updateChecker.isNewerVersionAvailable(info[i].ModVersion)) {
                        anyNeedUpdates = true;
                        MODUPDATES.add(new ModUpdate(info[i], updateChecker.getLatestReleaseURL(), updateChecker.getLatestDownloadURL()));
                        setModUpdateBanner(info[i]);
                        revalidate();
                        repaint();
                        modList.setUpdateIcon(info[i], UpdateIconType.UPDATE_AVAILABLE);
                    } else {
                        modList.setUpdateIcon(info[i], UpdateIconType.UPTODATE);
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("ERROR: " + info[i].Name + ": " + e.getMessage());
                } catch (IOException e) {
                    // NOP
                    System.out.println(e);
                }
            }

            if (anyNeedUpdates) {
                updatesBtn.setIcon(ICON_WARNING);
                updatesBtn.setToolTipText("Mod updates are available.");
                for (ActionListener listener : updatesBtn.getActionListeners()) {
                    updatesBtn.removeActionListener(listener);
                }
                updatesBtn.addActionListener(e -> {
                    UpdateWindow win = new UpdateWindow(this);
                    win.setVisible(true);
                });
            } else {
                updatesBtn.setIcon(ICON_UPDATE);
            }
        }).start();
    }

    public void warnAboutMissingVersions()
    {
        for (ModInfo modInfo : info) {
            if (modInfo.ModVersion == null) {
                JOptionPane.showMessageDialog(null,
                    modInfo.Name + " has a missing or bad version number.\nGo yell at the author to fix it.",
                    "Warning", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}
