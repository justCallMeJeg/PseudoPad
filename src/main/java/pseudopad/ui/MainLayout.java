package pseudopad.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import pseudopad.app.ActionController;
import pseudopad.app.AppConstants;
import pseudopad.app.MainFrame;
import pseudopad.app.WindowManager;
import pseudopad.core.AST.ProgramNode;
import pseudopad.editor.EditorTabbedPane;
import pseudopad.editor.FileTabPane;
import pseudopad.editor.ProblemsPanel;
import pseudopad.editor.explorer.FileExplorer;
import pseudopad.editor.statusbar.CursorPositionWidget;
import pseudopad.editor.statusbar.MemoryUsageWidget;
import pseudopad.editor.statusbar.ReadOnlyWidget;
import pseudopad.editor.statusbar.StatusBar;
import pseudopad.editor.FileOutlinePanel;
import pseudopad.editor.terminal.SimpleTerminalBackend;
import pseudopad.editor.terminal.TerminalPane;
import pseudopad.ui.components.ActivityBar;
import pseudopad.ui.components.AppMenuBar;
import pseudopad.ui.components.AppToolBar;
import pseudopad.ui.components.RecentProjectsPanel;
import pseudopad.ui.components.TabbedPane;
import pseudopad.ui.components.TextPane;
import pseudopad.utils.IconManager;

import java.awt.Component;

/**
 * Manages the UI layout and components for the MainFrame.
 * 
 * @author Geger John Paul Gabayeron
 */
public class MainLayout extends JPanel {
    private final MainFrame mainFrame;
    private final ActionController appActions;

    private JMenuBar menuBar;
    private JSplitPane mainSplitPane;
    private JSplitPane navigationSplitPane;
    private JSplitPane editorSplitPane;
    private TabbedPane topNavigationTabbedPane;
    private TabbedPane bottomNavigationTabbedPane;
    private FileExplorer fileExplorer;
    private RecentProjectsPanel projectExplorer;
    private EditorTabbedPane editorTabbedPane;
    private TabbedPane bottomEditorTabbedPane;

    private ActivityBar activityBar;

    private TerminalPane terminalTextPane;
    private TextPane logTextPane;
    private StatusBar statusBar;
    private JScrollPane logScrollPane;
    private JScrollPane terminalScrollPane;

    private CursorPositionWidget cursorWidget;
    private ReadOnlyWidget readOnlyWidget;
    private MemoryUsageWidget memoryWidget;

    private ProblemsPanel problemsPanel;

    private FileOutlinePanel fileOutlinePanel;

    public MainLayout(MainFrame mainFrame, ActionController appActions) {
        this.mainFrame = mainFrame;
        this.appActions = appActions;
        setLayout(new BorderLayout());
        initUIComponents();
    }

    private void initUIComponents() {
        initComponents();

        Border lineBorder = BorderFactory.createLineBorder(UIManager.getColor("Panel.foreground").darker(), 1);

        this.menuBar = new AppMenuBar(this.appActions);
        mainFrame.setJMenuBar(menuBar);

        AppToolBar toolBar = new AppToolBar(this.appActions);
        add(toolBar, BorderLayout.NORTH);

        // Add Activity Bar (West of main layout)
        add(activityBar, BorderLayout.WEST);

        this.mainSplitPane.setResizeWeight(AppConstants.MAIN_SPLIT_RESIZE_WEIGHT);
        this.mainSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        add(mainSplitPane, BorderLayout.CENTER);

        // Add Status Bar
        add(statusBar, BorderLayout.SOUTH);

        this.navigationSplitPane.setBorder(lineBorder);
        this.navigationSplitPane.setResizeWeight(AppConstants.NAV_SPLIT_RESIZE_WEIGHT);
        this.navigationSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

        this.fileExplorer.setOnFileOpenListener((File file) -> {
            editorTabbedPane.openFileTab(file);
        });

        this.fileExplorer.setOnFileRenamedListener((File oldFile, File newFile) -> {
            if (editorTabbedPane != null) {
                editorTabbedPane.handleFileRename(oldFile, newFile);
            }
        });

        this.fileExplorer.setOnFileDeletedListener((File file) -> {
            if (editorTabbedPane != null) {
                editorTabbedPane.closeFileTab(file);
            }
        });

        this.topNavigationTabbedPane.setMinimumSize(new Dimension(200, 100));
        // Tabs added dynamically now via WindowManager

        this.topNavigationTabbedPane.setMinimizeAction(e -> {
            if (this.navigationSplitPane.getDividerLocation() < 50) {
                this.navigationSplitPane.setDividerLocation(0.5);
            } else {
                if (this.navigationSplitPane
                        .getDividerLocation() >= this.navigationSplitPane.getMaximumDividerLocation() - 50) {
                    this.mainSplitPane.setDividerLocation(0.0);
                } else {
                    this.navigationSplitPane.setDividerLocation(0.0);
                }
            }
        });

        this.bottomNavigationTabbedPane.setMinimumSize(new Dimension(200, 100));
        // Tabs added dynamically now via WindowManager

        this.bottomNavigationTabbedPane.setMinimizeAction(e -> {
            if (this.navigationSplitPane.getDividerLocation() >= this.navigationSplitPane.getMaximumDividerLocation()
                    - 50) {
                this.navigationSplitPane.setDividerLocation(0.5);
            } else {
                if (this.navigationSplitPane.getDividerLocation() < 50) {
                    this.mainSplitPane.setDividerLocation(0.0);
                } else {
                    this.navigationSplitPane.setDividerLocation(1.0);
                }
            }
        });

        this.navigationSplitPane.setTopComponent(this.topNavigationTabbedPane);
        this.navigationSplitPane.setBottomComponent(this.bottomNavigationTabbedPane);

        this.mainSplitPane.setLeftComponent(this.navigationSplitPane);

        this.editorSplitPane.setBorder(lineBorder);
        this.editorSplitPane.setResizeWeight(AppConstants.EDITOR_SPLIT_RESIZE_WEIGHT);
        this.editorSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        this.editorSplitPane.setTopComponent(editorTabbedPane);

        this.logTextPane.setEditable(false);
        // this.terminalTextPane.setEditable(false);

        logScrollPane = new JScrollPane(logTextPane);
        terminalScrollPane = new JScrollPane(terminalTextPane);

        // Tabs added dynamically now via WindowManager

        this.editorSplitPane.setBottomComponent(bottomEditorTabbedPane);

        this.mainSplitPane.setRightComponent(this.editorSplitPane);

        this.bottomEditorTabbedPane.setMinimizeAction(e -> {
            if (this.editorSplitPane.getDividerLocation() >= this.editorSplitPane.getMaximumDividerLocation() - 50) {
                this.editorSplitPane.setDividerLocation(0.7);
            } else {
                this.editorSplitPane.setDividerLocation(1.0);
            }
        });

        // Enable double-click to reset dividers
        enableDividerDoubleClickListener(mainSplitPane, 0.25);
        enableDividerDoubleClickListener(navigationSplitPane, 0.5);
        enableDividerDoubleClickListener(editorSplitPane, 0.75);

        statusBar.addRightComponent(cursorWidget);
        statusBar.addSeparator();
        statusBar.addRightComponent(readOnlyWidget);
        statusBar.addSeparator();
        statusBar.addRightComponent(memoryWidget);

        editorTabbedPane.addChangeListener(e -> {
            updateStatusBarWidgets(cursorWidget, readOnlyWidget);
            updateFileOutline();
        });

        updateStatusBarWidgets(cursorWidget, readOnlyWidget);
        updateFileOutline();

        // Final Step: Wire up visibility listeners and init state
        setupPanelVisibilityListeners();

        // Wire close callbacks so clicking X triggers WindowManager
        setupTabCloseCallbacks();
    }

    private void setupTabCloseCallbacks() {
        BiConsumer<JTabbedPane, Integer> topNavCallback = (tabbedPane, tabIndex) -> {
            Component comp = tabbedPane.getComponentAt(tabIndex);
            String panelId = TabPaneUtils.getPanelIdForComponent(comp);
            if (panelId != null) {
                WindowManager.getInstance().hidePanel(panelId);
            }
        };

        BiConsumer<JTabbedPane, Integer> bottomNavCallback = (tabbedPane, tabIndex) -> {
            Component comp = tabbedPane.getComponentAt(tabIndex);
            String panelId = TabPaneUtils.getPanelIdForComponent(comp);
            if (panelId != null) {
                WindowManager.getInstance().hidePanel(panelId);
            }
        };

        BiConsumer<JTabbedPane, Integer> outputCallback = (tabbedPane, tabIndex) -> {
            Component comp = tabbedPane.getComponentAt(tabIndex);
            String panelId = TabPaneUtils.getPanelIdForComponent(comp);
            if (panelId != null) {
                WindowManager.getInstance().hidePanel(panelId);
            }
        };

        // Set FlatLaf property directly to override constructor's default
        topNavigationTabbedPane.putClientProperty(
                com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK, topNavCallback);
        bottomNavigationTabbedPane.putClientProperty(
                com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK, bottomNavCallback);
        bottomEditorTabbedPane.putClientProperty(
                com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK, outputCallback);
    }

    private void updateFileOutline() {
        Component selected = editorTabbedPane.getSelectedComponent();
        if (selected instanceof FileTabPane fileTab) {
            ProgramNode ast = fileTab.getCachedAST();
            fileOutlinePanel.updateOutline(ast);
        } else {
            fileOutlinePanel.updateOutline(null);
        }
    }

    private void enableDividerDoubleClickListener(JSplitPane splitPane, double defaultLocation) {
        if (splitPane.getUI() instanceof javax.swing.plaf.basic.BasicSplitPaneUI ui) {
            java.awt.Container divider = ui.getDivider();
            divider.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        splitPane.setDividerLocation(defaultLocation);
                    }
                }
            });
        }
    }

    private void setupPanelVisibilityListeners() {
        WindowManager wm = WindowManager.getInstance();

        // Define handling logic - execute synchronously since we're called from EDT
        java.util.function.BiConsumer<String, Boolean> handler = (panelId, isVisible) -> {
            if (isVisible) {
                restorePanel(panelId);
            } else {
                removePanel(panelId);
            }
        };

        // Add listener
        wm.addPanelChangeListener(handler);

        // Initialize state (trigger logic for default or persisted values)
        // Important: We call this AFTER components are created but BEFORE window shows
        // ideally,
        // to setup tabs.
        restorePanel(WindowManager.PANEL_PROJECTS);
        restorePanel(WindowManager.PANEL_FILES);
        restorePanel(WindowManager.PANEL_FILE_OUTLINE);
        restorePanel(WindowManager.PANEL_OUTPUT);
        restorePanel(WindowManager.PANEL_PROBLEMS);
        restorePanel(WindowManager.PANEL_LOGS);
    }

    private void removePanel(String panelId) {
        String name = WindowManager.getPanelName(panelId);
        Component comp = getComponentForPanel(panelId);

        if (WindowManager.isNavigationPanel(panelId)) {
            // Remove from TabPane by component (titles may change)
            if (comp != null) {
                TabPaneUtils.removeTabByComponent(topNavigationTabbedPane, comp);
                TabPaneUtils.removeTabByComponent(bottomNavigationTabbedPane, comp);
            }

            // Add to Activity Bar
            String iconName = getIconNameForPanel(panelId);
            javax.swing.Icon icon = IconManager.get(iconName);

            activityBar.showRestoreButton(panelId, icon,
                    name, e -> WindowManager.getInstance().showPanel(panelId));

            // Auto-collapse if empty
            checkAndCollapseNavigationPanes();

        } else if (WindowManager.isOutputPanel(panelId)) {
            // Remove from Bottom Editor Tabs by component (titles may change, e.g.
            // "Problems [ 5 ]")
            if (comp != null) {
                TabPaneUtils.removeTabByComponent(bottomEditorTabbedPane, comp);
            }

            // Add to Activity Bar (consistent with navigation panels)
            String iconName = getIconNameForPanel(panelId);
            javax.swing.Icon icon = IconManager.get(iconName);

            activityBar.showRestoreButton(panelId, icon,
                    name, e -> WindowManager.getInstance().showPanel(panelId));

            // Auto-collapse if empty
            checkAndCollapseOutputPane();
        }
    }

    private void checkAndCollapseNavigationPanes() {
        // If top nav is empty, collapse it to bottom
        if (topNavigationTabbedPane.getTabCount() == 0 && bottomNavigationTabbedPane.getTabCount() > 0) {
            navigationSplitPane.setDividerLocation(0.0);
        }
        // If bottom nav is empty, collapse it to top
        if (bottomNavigationTabbedPane.getTabCount() == 0 && topNavigationTabbedPane.getTabCount() > 0) {
            navigationSplitPane.setDividerLocation(1.0);
        }
        // If both are empty, collapse entire nav
        if (topNavigationTabbedPane.getTabCount() == 0 && bottomNavigationTabbedPane.getTabCount() == 0) {
            mainSplitPane.setDividerLocation(0.0);
        }
    }

    private void checkAndCollapseOutputPane() {
        // If output tabbed pane is empty, collapse output area
        if (bottomEditorTabbedPane.getTabCount() == 0) {
            editorSplitPane.setDividerLocation(1.0);
        }
    }

    private void restorePanel(String panelId) {
        String name = WindowManager.getPanelName(panelId);
        Component comp = getComponentForPanel(panelId);

        if (comp == null)
            return;

        if (WindowManager.isNavigationPanel(panelId)) {
            // Remove from Activity Bar
            activityBar.hideRestoreButton(panelId);

            // Add to appropriate TabPane at correct position
            if (WindowManager.PANEL_FILE_OUTLINE.equals(panelId)) {
                TabPaneUtils.addTabAtCorrectPosition(bottomNavigationTabbedPane, name, comp, panelId);
            } else {
                // Projects and Files go to Top Nav
                TabPaneUtils.addTabAtCorrectPosition(topNavigationTabbedPane, name, comp, panelId);
            }

            // Check if we need to expand the navigation split
            checkAndExpandNavigationSplit();

        } else if (WindowManager.isOutputPanel(panelId)) {
            // Remove from Activity Bar
            activityBar.hideRestoreButton(panelId);

            // Add to Bottom Editor Tabs at correct position
            TabPaneUtils.addTabAtCorrectPosition(bottomEditorTabbedPane, name, comp, panelId);

            // Special case: Problems panel needs to update its title to show error count
            if (WindowManager.PANEL_PROBLEMS.equals(panelId) && comp instanceof ProblemsPanel pp) {
                SwingUtilities.invokeLater(pp::updateTabTitleCount);
            }

            // Check if we need to expand the editor split
            checkAndExpandEditorSplit();
        }
    }

    private void checkAndExpandNavigationSplit() {
        // If nav was collapsed, expand it
        if (mainSplitPane.getDividerLocation() < 50) {
            mainSplitPane.setDividerLocation(0.25);
        }
        // Also check inner nav split
        if (topNavigationTabbedPane.getTabCount() > 0 && navigationSplitPane.getDividerLocation() < 50) {
            navigationSplitPane.setDividerLocation(0.5);
        }
        if (bottomNavigationTabbedPane.getTabCount() > 0 &&
                navigationSplitPane.getDividerLocation() > navigationSplitPane.getMaximumDividerLocation() - 50) {
            navigationSplitPane.setDividerLocation(0.5);
        }
    }

    private void checkAndExpandEditorSplit() {
        // If output was collapsed, expand it
        if (editorSplitPane.getDividerLocation() > editorSplitPane.getMaximumDividerLocation() - 50) {
            editorSplitPane.setDividerLocation(0.75);
        }
    }

    private Component getComponentForPanel(String panelId) {
        return switch (panelId) {
            case WindowManager.PANEL_PROJECTS -> projectExplorer;
            case WindowManager.PANEL_FILES -> fileExplorer;
            case WindowManager.PANEL_FILE_OUTLINE -> fileOutlinePanel;
            case WindowManager.PANEL_OUTPUT -> terminalScrollPane;
            case WindowManager.PANEL_PROBLEMS -> problemsPanel;
            case WindowManager.PANEL_LOGS -> logScrollPane;
            default -> null;
        };
    }

    private String getIconNameForPanel(String panelId) {
        return switch (panelId) {
            case WindowManager.PANEL_PROJECTS -> "project"; // Custom PseudoPad project icon
            case WindowManager.PANEL_FILES -> "files"; // File multiple icon (not folder)
            case WindowManager.PANEL_FILE_OUTLINE -> "outline";
            case WindowManager.PANEL_OUTPUT -> "terminal";
            case WindowManager.PANEL_PROBLEMS -> "warning";
            case WindowManager.PANEL_LOGS -> "log";
            default -> "file";
        };
    }

    // Internal helper for tab management
    private static class TabPaneUtils {
        /**
         * Adds a tab at the correct position based on panel order.
         * 
         * @param tabPane The target TabbedPane
         * @param title   The tab title
         * @param comp    The component
         * @param panelId The panel ID (for ordering)
         */
        static void addTabAtCorrectPosition(TabbedPane tabPane, String title, Component comp, String panelId) {
            // Check if already exists
            for (int i = 0; i < tabPane.getTabCount(); i++) {
                if (tabPane.getComponentAt(i) == comp) {
                    return; // Already exists
                }
            }

            int targetOrder = WindowManager.getDefaultPanelOrder(panelId);
            int insertIndex = tabPane.getTabCount(); // Default: add at end

            // Find the correct insertion point
            for (int i = 0; i < tabPane.getTabCount(); i++) {
                Component existingComp = tabPane.getComponentAt(i);
                String existingPanelId = getPanelIdForComponent(existingComp);
                if (existingPanelId != null) {
                    int existingOrder = WindowManager.getDefaultPanelOrder(existingPanelId);
                    if (targetOrder < existingOrder) {
                        insertIndex = i;
                        break;
                    }
                }
            }

            tabPane.insertTab(title, null, comp, null, insertIndex);
            // Tabs are closable - wiring is done via TabbedPane's close callback
        }

        static void addTabIfNotExists(TabbedPane tabPane, String title, Component comp) {
            for (int i = 0; i < tabPane.getTabCount(); i++) {
                if (tabPane.getComponentAt(i) == comp) {
                    return;
                }
            }
            tabPane.addTab(title, comp);
        }

        static void removeTabByComponent(TabbedPane tabPane, Component comp) {
            int index = tabPane.indexOfComponent(comp);
            if (index >= 0) {
                tabPane.removeTabAt(index);
            }
        }

        // Legacy method - kept for compatibility but prefer removeTabByComponent
        static void removeTab(TabbedPane tabPane, String title) {
            int index = tabPane.indexOfTab(title);
            if (index >= 0) {
                tabPane.removeTabAt(index);
            }
        }

        private static String getPanelIdForComponent(Component comp) {
            // Reverse lookup
            if (comp instanceof RecentProjectsPanel)
                return WindowManager.PANEL_PROJECTS;
            if (comp instanceof FileExplorer)
                return WindowManager.PANEL_FILES;
            if (comp instanceof FileOutlinePanel)
                return WindowManager.PANEL_FILE_OUTLINE;
            if (comp instanceof JScrollPane scroll) {
                Component view = scroll.getViewport().getView();
                if (view instanceof TerminalPane)
                    return WindowManager.PANEL_OUTPUT;
                if (view instanceof TextPane)
                    return WindowManager.PANEL_LOGS;
            }
            if (comp instanceof ProblemsPanel)
                return WindowManager.PANEL_PROBLEMS;
            return null;
        }
    }

    private void initComponents() {
        this.mainSplitPane = new JSplitPane();
        this.navigationSplitPane = new JSplitPane();
        this.editorSplitPane = new JSplitPane();
        this.topNavigationTabbedPane = new TabbedPane();
        this.bottomNavigationTabbedPane = new TabbedPane();
        this.fileExplorer = new FileExplorer();
        this.fileOutlinePanel = new pseudopad.editor.FileOutlinePanel();
        this.projectExplorer = new RecentProjectsPanel(mainFrame);
        this.editorTabbedPane = new EditorTabbedPane(mainFrame);
        this.bottomEditorTabbedPane = new TabbedPane();

        // New Activity Bar
        this.activityBar = new ActivityBar();

        SimpleTerminalBackend backend = new SimpleTerminalBackend();
        backend.setCodeProvider(() -> {
            if (editorTabbedPane != null) {
                return editorTabbedPane.getActiveFileContent();
            }
            return null;
        });

        this.terminalTextPane = new TerminalPane(backend);
        this.logTextPane = new TextPane();

        this.statusBar = new StatusBar();

        this.cursorWidget = new CursorPositionWidget();
        this.readOnlyWidget = new ReadOnlyWidget();
        this.memoryWidget = new MemoryUsageWidget();

        this.problemsPanel = new pseudopad.editor.ProblemsPanel();
    }

    private void updateStatusBarWidgets(CursorPositionWidget cursorWidget, ReadOnlyWidget readOnlyWidget) {
        java.awt.Component selected = editorTabbedPane.getSelectedComponent();

        if (selected instanceof FileTabPane fileTab) {
            // Update Read-Only Status
            File file = fileTab.getFile();
            readOnlyWidget.setReadOnly(file != null && !file.canWrite());

            // Update Cursor Position
            TextPane textPane = fileTab.getTextPane();
            updateCursorPosition(textPane, cursorWidget);

            // Update Outline explicitly? Maybe via FileTabPane itself?
            // Actually FileTabPane will analyze and update when focused or changed.
            // But if we switch tabs, we might want to refresh the outline immediately with
            // cached AST.

            // Add Caret Listener
            for (javax.swing.event.CaretListener l : textPane.getCaretListeners()) {
                // Remove existing listeners to avoid duplicates (simplistic approach)
                // In real app, cleaner listener management is needed
                if (l instanceof javax.swing.event.CaretListener) {

                }
            }

            // Let's use a client property on the textPane to store the listener
            javax.swing.event.CaretListener existingListener = (javax.swing.event.CaretListener) textPane
                    .getClientProperty("StatusBarCaretListener");
            if (existingListener == null) {
                javax.swing.event.CaretListener listener = e -> updateCursorPosition(textPane, cursorWidget);
                textPane.addCaretListener(listener);
                textPane.putClientProperty("StatusBarCaretListener", listener);
            }

        } else

        {
            readOnlyWidget.setReadOnly(false);
            cursorWidget.updatePosition(1, 1);
        }
    }

    private void updateCursorPosition(TextPane textPane, CursorPositionWidget cursorWidget) {
        try {
            int caretPos = textPane.getCaretPosition();
            int line = getLineOfOffset(textPane, caretPos);
            int column = getColumnOfOffset(textPane, caretPos);
            cursorWidget.updatePosition(line, column);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private int getLineOfOffset(javax.swing.JTextPane textPane, int offset) throws BadLocationException {
        javax.swing.text.Element map = textPane.getDocument().getDefaultRootElement();
        return map.getElementIndex(offset) + 1;
    }

    private int getColumnOfOffset(javax.swing.JTextPane textPane, int offset) throws BadLocationException {
        javax.swing.text.Element map = textPane.getDocument().getDefaultRootElement();
        int line = map.getElementIndex(offset);
        javax.swing.text.Element lineElem = map.getElement(line);
        return offset - lineElem.getStartOffset() + 1;
    }

    public void appendLog(String message, Color color) {
        if (logTextPane != null) {
            StyledDocument doc = logTextPane.getStyledDocument();
            Style style = logTextPane.addStyle("LogStyle", null);
            StyleConstants.setForeground(style, color != null ? color : UIManager.getColor("Panel.foreground"));

            try {
                doc.insertString(doc.getLength(), message + "\n", style);
                SwingUtilities.invokeLater(() -> {
                    logTextPane.setCaretPosition(doc.getLength());
                });
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    public void resetLayout() {
        SwingUtilities.invokeLater(() -> {
            // Hide Navigation
            mainSplitPane.setDividerLocation(0.0);

            // Hide Output
            editorSplitPane.setDividerLocation(1.0);

            // Default Nav Split
            navigationSplitPane.setDividerLocation(0.5);
        });
    }

    // Getters
    public JSplitPane getMainSplitPane() {
        return mainSplitPane;
    }

    public JSplitPane getNavigationSplitPane() {
        return navigationSplitPane;
    }

    public JSplitPane getEditorSplitPane() {
        return editorSplitPane;
    }

    public TabbedPane getTopNavigationTabbedPane() {
        return topNavigationTabbedPane;
    }

    public FileExplorer getFileExplorer() {
        return fileExplorer;
    }

    public EditorTabbedPane getEditorTabbedPane() {
        return editorTabbedPane;
    }

    public TextPane getLogTextPane() {
        return logTextPane;
    }

    public StatusBar getStatusBar() {
        return statusBar;
    }

    public void runTerminalCommand(String command) {
        if (terminalTextPane != null) {
            terminalTextPane.runCommand(command);
        }
    }

    public void setTerminalProjectName(String projectName) {
        if (terminalTextPane != null) {
            terminalTextPane.setProjectName(projectName);
        }
    }

    public pseudopad.editor.ProblemsPanel getProblemsPanel() {
        return problemsPanel;
    }

    public pseudopad.editor.FileOutlinePanel getFileOutlinePanel() {
        return fileOutlinePanel;
    }

    public TabbedPane getBottomEditorTabbedPane() {
        return bottomEditorTabbedPane;
    }

    public RecentProjectsPanel getRecentProjectsPanel() {
        return projectExplorer;
    }
}
