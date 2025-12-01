package pseudopad.app;

import javax.swing.border.Border;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Taskbar;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import pseudopad.ui.components.AppMenuBar;
import pseudopad.ui.components.AppToolBar;
import pseudopad.ui.components.EditorTabbedPane;
import pseudopad.ui.components.FileExplorer;
import pseudopad.ui.components.TabbedPane;
import pseudopad.ui.components.TextPane;
import pseudopad.ui.dialogs.NewProjectDialog;
import pseudopad.ui.dialogs.OpenProjectDialog;
import pseudopad.utils.AppActionsManager;
import pseudopad.utils.PreferenceManager;
import pseudopad.utils.ThemeManager;
import pseudopad.utils.AppConstants;
import pseudopad.utils.ProjectFileWatcher;
import pseudopad.utils.ProjectManager;
import pseudopad.config.ProjectConfig;
import java.awt.KeyboardFocusManager;
import java.awt.Component;
import java.awt.Color;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.BadLocationException;
import pseudopad.utils.AppLogger;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class MainFrame extends JFrame {
    private static MainFrame INSTANCE;

    private File currentProjectPath;
    // private final ThemeManager themeManager = ThemeManager.getInstance(); //
    // Unused
    // private final PreferenceManager AppPref = PreferenceManager.getInstance(); //
    // Unused
    private final AppActionsManager AppActions = new AppActionsManager(this);
    private ProjectFileWatcher projectFileWatcher;
    private File pendingProjectToLoad;

    public MainFrame() {
        INSTANCE = this;
        initUIComponents();

        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName())) {
                Boolean isDark = ThemeManager.getInstance().isDarkMode();
                setupAppIcon(isDark);
            }
        });
    }

    @Override
    public void dispose() {
        saveWindowState();
        if (projectFileWatcher != null) {
            projectFileWatcher.stop();
        }
        super.dispose();
    }

    private void saveWindowState() {
        int width = getWidth();
        int height = getHeight();
        int x = getX();
        int y = getY();
        int state = getExtendedState();

        int divMain = mainSplitPane.getDividerLocation();
        int divEditor = editorSplitPane.getDividerLocation();
        int divNav = navigationSplitPane.getDividerLocation();

        if (currentProjectPath != null) {
            ProjectConfig config = ProjectManager.loadConfig(currentProjectPath);
            if (config == null)
                config = new ProjectConfig(currentProjectPath.getName());

            config.windowWidth = width;
            config.windowHeight = height;
            config.windowX = x;
            config.windowY = y;
            config.windowState = state;

            config.dividerMain = divMain;
            config.dividerEditor = divEditor;
            config.dividerNav = divNav;

            // Save Session State
            if (editorTabbedPane != null) {
                config.openFiles = editorTabbedPane.getOpenFiles();
                config.activeFile = editorTabbedPane.getActiveFile();
            }

            ProjectManager.saveConfig(currentProjectPath, config);
        } else {
            PreferenceManager.getInstance().saveWindowState(width, height, x, y, state);
            PreferenceManager.getInstance().saveDividerLocations(divMain, divEditor, divNav);
        }
    }

    public void setupAppIcon(Boolean isDark) {
        Image icon = new ImageIcon(
                MainFrame.class.getResource(isDark ? AppConstants.ICON_PATH_DARK : AppConstants.ICON_PATH_LIGHT))
                .getImage();

        // macOS app icon setup
        // Check if the Taskbar API is supported on the current OS
        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            try {
                // Set the icon for the application's global taskbar/dock icon
                taskbar.setIconImage(icon);
            } catch (UnsupportedOperationException e) {
                System.err.println("The taskbar setIconImage feature is not supported on this platform.");
            }
        }

        // Windows/Linux app icon setup
        setIconImage(icon);
    }

    public void setPendingProject(File project) {
        this.pendingProjectToLoad = project;
    }

    public void launchAppInstance(File projectPath) {
        // If no specific project passed, check pending
        if (projectPath == null && this.pendingProjectToLoad != null) {
            projectPath = this.pendingProjectToLoad;
            this.pendingProjectToLoad = null; // Consume it
        }

        this.currentProjectPath = projectPath;

        // Determine initial bounds and state
        int w = 1280, h = 720, x = -1, y = -1, state = JFrame.MAXIMIZED_BOTH; // Default to Maximized
        int divMain = -1, divEditor = -1, divNav = -1;

        if (currentProjectPath != null) {
            ProjectConfig config = ProjectManager.loadConfig(currentProjectPath);
            if (config != null && config.windowWidth > 0 && config.windowHeight > 0) {
                w = config.windowWidth;
                h = config.windowHeight;
                x = config.windowX;
                y = config.windowY;
                state = config.windowState;
                divMain = config.dividerMain;
                divEditor = config.dividerEditor;
                divNav = config.dividerNav;
            } else {
                // Config invalid or missing, try global fallback for window size/pos
                // But for dividers, we might want defaults.
                int[] winState = PreferenceManager.getInstance().loadWindowState();
                if (winState[0] > 0) {
                    w = winState[0];
                    h = winState[1];
                    x = winState[2];
                    y = winState[3];
                    state = winState[4];
                }
            }
        } else {
            int[] winState = PreferenceManager.getInstance().loadWindowState();
            w = winState[0];
            h = winState[1];
            x = winState[2];
            y = winState[3];
            state = winState[4];

            int[] divs = PreferenceManager.getInstance().loadDividerLocations();
            divMain = divs[0];
            divEditor = divs[1];
            divNav = divs[2];
        }

        // Apply Window State
        if (x != -1 && y != -1)
            setLocation(x, y);
        else
            setLocationRelativeTo(null);

        setSize(w > 0 ? w : 1280, h > 0 ? h : 720);
        setExtendedState(state);

        // 1. Show the window FIRST so components get their sizes
        this.setVisible(true);

        // 2. Configure components
        if (currentProjectPath != null) {
            AppLogger.info("Launching project: " + currentProjectPath.getAbsolutePath());
            this.setTitle(this.currentProjectPath.getName() + " - " + AppConstants.APP_TITLE);
            this.fileExplorer.openProject(projectPath);

            // Start File Watcher
            if (projectFileWatcher != null) {
                projectFileWatcher.stop();
            }
            projectFileWatcher = new ProjectFileWatcher(projectPath);
            projectFileWatcher.setFileChangeListener(new ProjectFileWatcher.FileChangeListener() {
                @Override
                public void onFileCreated(File file) {
                    // Do nothing specific, refresh handles it
                }

                @Override
                public void onFileDeleted(File file) {
                    SwingUtilities.invokeLater(() -> {
                        if (editorTabbedPane != null) {
                            editorTabbedPane.closeFileTab(file);
                        }
                    });
                }

                @Override
                public void onFileRenamed(File oldFile, File newFile) {
                    SwingUtilities.invokeLater(() -> {
                        if (editorTabbedPane != null) {
                            editorTabbedPane.handleFileRename(oldFile, newFile);
                        }
                    });
                }
            });
            projectFileWatcher.start();

            // this.editorSplitPane.setTopComponent(editorTabbedPane);

            // Save as Last Project
            PreferenceManager.getInstance().saveLastProject(currentProjectPath);
        } else {
            this.setTitle(AppConstants.APP_TITLE);
            // this.editorSplitPane.setTopComponent(new FallbackPanel(INSTANCE));
        }

        if (this.editorTabbedPane != null) {
            this.editorTabbedPane.refreshFallbackState();
        }

        // Restore Session State (Open Files)
        if (currentProjectPath != null) {
            ProjectConfig config = ProjectManager.loadConfig(currentProjectPath);
            if (config != null && config.openFiles != null) {
                for (String filePath : config.openFiles) {
                    File f = new File(filePath);
                    if (f.exists()) {
                        this.editorTabbedPane.openFileTab(f);
                    }
                }
                if (config.activeFile != null) {
                    File f = new File(config.activeFile);
                    if (f.exists()) {
                        this.editorTabbedPane.openFileTab(f); // Selects it
                    }
                }
            }
            // Select "Files" tab in Top Navigation
            if (topNavigationTabbedPane.getTabCount() > 1) {
                topNavigationTabbedPane.setSelectedIndex(1);
            }
        }

        // 3. Set Divider Locations LAST, inside invokeLater
        // This places the request at the end of the Event Queue, ensuring
        // the window is fully drawn before the dividers try to move.
        final int fDivMain = divMain;
        final int fDivEditor = divEditor;
        final int fDivNav = divNav;

        SwingUtilities.invokeLater(() -> {
            // Set Main Split (Navigation vs Editor)
            if (currentProjectPath == null) {
                this.mainSplitPane.setDividerLocation(0.0); // Enforce hidden if no project
            } else if (fDivMain > 0) { // Check > 0 to avoid collapsing if uninitialized
                this.mainSplitPane.setDividerLocation(fDivMain);
            } else {
                this.mainSplitPane.setDividerLocation(0.25); // Default show
            }

            // Set Editor Split (Editor vs Console)
            if (fDivEditor > 0) // Check > 0
                this.editorSplitPane.setDividerLocation(fDivEditor);
            else {
                if (currentProjectPath != null) {
                    this.editorSplitPane.setDividerLocation(0.75); // Show output panel
                } else {
                    this.editorSplitPane.setDividerLocation(1.0); // Hide output panel
                }
            }

            // Set Navigation Split (Files vs Outline)
            if (fDivNav > 0)
                this.navigationSplitPane.setDividerLocation(fDivNav);
            else
                this.navigationSplitPane.setDividerLocation(0.5);

            // Ensure window is on top and focused after UI setup
            this.toFront();
            this.requestFocus();
        });
    }

    // ----- App Action Delegation Logic -----

    public void newProject() {
        NewProjectDialog newProject = new NewProjectDialog(this);
        newProject.setLocationRelativeTo(this);
        newProject.setVisible(true);
    }

    public void openProject() {
        OpenProjectDialog chooser = new OpenProjectDialog();
        chooser.showOpenDialog(this);
    }

    public void openProject(File projectPath) {
        if (this.currentProjectPath == null) {
            // Case 1: Current window is empty -> Use it
            AppLogger.info("Opening project in current window: " + projectPath.getAbsolutePath());
            launchAppInstance(projectPath);
        } else {
            // Case 2: Current window is busy -> Create new window
            AppLogger.info("Opening project in new window: " + projectPath.getAbsolutePath());
            MainFrame newFrame = new MainFrame();
            newFrame.setupAppIcon(ThemeManager.getInstance().isDarkMode());
            newFrame.launchAppInstance(projectPath);
        }
    }

    public void saveCurrentFile() {
        if (editorTabbedPane != null) {
            AppLogger.info("Saving current file...");
            // Delegate deeper to the specific component
            editorTabbedPane.saveActiveTab();
        }
    }

    public void undoAction() {
        if (editorTabbedPane != null)
            editorTabbedPane.undo();
    }

    public void redoAction() {
        if (editorTabbedPane != null)
            editorTabbedPane.redo();
    }

    public void cutContent() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, fileExplorer)) {
            fileExplorer.cut();
        } else if (editorTabbedPane != null) {
            editorTabbedPane.cut();
        }
    }

    public void copyContent() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, fileExplorer)) {
            fileExplorer.copy();
        } else if (editorTabbedPane != null) {
            editorTabbedPane.copy();
        }
    }

    public void pasteContent() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, fileExplorer)) {
            fileExplorer.paste();
        } else if (editorTabbedPane != null) {
            editorTabbedPane.paste();
        }
    }

    public void deleteItem() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, fileExplorer)) {
            fileExplorer.deleteSelectedFile();
        } else {
            // Optional: Handle deleteItem in editor (e.g. deleteItem text) if needed,
            // but usually DELETE key is handled by text component directly.
            // We could delegate to active tab if we want "Delete File" action globally,
            // but that might be dangerous/confusing if user just wants to deleteItem text.
            // For now, let's only delegate to FileExplorer if focused.
        }
    }

    public void refreshFileExplorer() {
        if (fileExplorer != null) {
            fileExplorer.refresh();
        }
    }

    public void closeProject() {
        AppLogger.info("Closing project: " + (currentProjectPath != null ? currentProjectPath.getName() : "Unknown"));
        this.currentProjectPath = null;

        this.setTitle(AppConstants.APP_TITLE);

        if (projectFileWatcher != null) {
            projectFileWatcher.stop();
            projectFileWatcher = null;
        }

        // 1. Clear the File Explorer
        // (Assuming FileExplorer has a clear() method)
        this.fileExplorer.clear();

        // 2. Close all open tabs
        this.editorTabbedPane.removeAll();

        // 3. Force Fallback Panel to update (It's now visible because we removed all
        // tabs)
        this.editorTabbedPane.refreshFallbackState();
    }

    public void changeTheme(ThemeManager.THEMES theme) {
        ThemeManager.getInstance().changeTheme(theme);
    }

    public File getCurrentProjectPath() {
        return currentProjectPath;
    }

    public MainFrame getAppInstance() {
        return INSTANCE;
    }

    public static MainFrame getInstance() {
        return INSTANCE;
    }

    public AppActionsManager getAppActionInstance() {
        return this.AppActions;
    }

    public void appendLog(String message, Color color) {
        if (logTextPane != null) {
            StyledDocument doc = logTextPane.getStyledDocument();
            Style style = logTextPane.addStyle("LogStyle", null);
            StyleConstants.setForeground(style, color != null ? color : UIManager.getColor("Panel.foreground"));

            try {
                doc.insertString(doc.getLength(), message + "\n", style);
                // Scroll to bottom
                SwingUtilities.invokeLater(() -> {
                    logTextPane.setCaretPosition(doc.getLength());
                });
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    public void toggleNavigationPanel() {
        // Toggle Logic for Main Split (Left vs Right)
        if (mainSplitPane.getDividerLocation() < 50) {
            // It's closed/minimized -> Restore
            mainSplitPane.setDividerLocation(0.25);
        } else {
            // It's open -> Close
            mainSplitPane.setDividerLocation(0.0);
        }
    }

    public void toggleOutputPanel() {
        // Toggle Logic for Editor Split (Top vs Bottom)
        // Note: Divider at 1.0 means bottom is hidden
        if (editorSplitPane.getDividerLocation() >= editorSplitPane.getMaximumDividerLocation() - 50) {
            // It's closed -> Restore
            editorSplitPane.setDividerLocation(0.75);
        } else {
            // It's open -> Close
            editorSplitPane.setDividerLocation(1.0);
        }
    }

    // ----- UI/UX Logic -----

    private void initUIComponents() {
        initComponents();

        Border lineBorder = BorderFactory.createLineBorder(UIManager.getColor("Panel.foreground").darker(), 1);

        // this.setExtendedState(JFrame.MAXIMIZED_BOTH); // Handled in launchAppInstance
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setMinimumSize(AppConstants.MIN_WINDOW_SIZE);

        this.setJMenuBar(menuBar);

        AppToolBar toolBar = new AppToolBar(this.AppActions);
        this.getContentPane().add(toolBar, BorderLayout.NORTH);

        this.getContentPane().add(toolBar, BorderLayout.NORTH);

        this.mainSplitPane.setResizeWeight(AppConstants.MAIN_SPLIT_RESIZE_WEIGHT);
        this.mainSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        this.getContentPane().add(mainSplitPane, BorderLayout.CENTER);

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
        this.topNavigationTabbedPane.add("Projects", projectExplorer);
        this.topNavigationTabbedPane.add("Files", fileExplorer);
        this.topNavigationTabbedPane.setMinimizeAction(e -> {
            // Toggle Logic for Top Nav
            if (this.navigationSplitPane.getDividerLocation() < 50) {
                // It's currently minimized (Divider ~0). Restore to 50%
                this.navigationSplitPane.setDividerLocation(0.5);
            } else {
                // It's open. We want to minimize it.
                // Check if Bottom is already minimized (Divider ~Max)
                if (this.navigationSplitPane
                        .getDividerLocation() >= this.navigationSplitPane.getMaximumDividerLocation() - 50) {
                    // Bottom is minimized, so Top is full height.
                    // Minimizing Top now means hiding the whole sidebar.
                    this.mainSplitPane.setDividerLocation(0.0);
                } else {
                    // Bottom is visible. Just hide Top (Divider -> 0).
                    this.navigationSplitPane.setDividerLocation(0.0);
                }
            }
        });

        this.bottomNavigationTabbedPane.setMinimumSize(new Dimension(200, 100));
        this.bottomNavigationTabbedPane.add("Navigation", new JPanel().add(new JLabel("File Navigation Panel")));
        this.bottomNavigationTabbedPane.setMinimizeAction(e -> {
            // Toggle Logic for Bottom Nav
            if (this.navigationSplitPane.getDividerLocation() >= this.navigationSplitPane.getMaximumDividerLocation()
                    - 50) {
                // It's currently minimized (Divider ~Max). Restore to 50%
                this.navigationSplitPane.setDividerLocation(0.5);
            } else {
                // It's open. We want to minimize it.
                // Check if Top is already minimized (Divider ~0)
                if (this.navigationSplitPane.getDividerLocation() < 50) {
                    // Top is minimized, so Bottom is full height.
                    // Minimizing Bottom now means hiding the whole sidebar.
                    this.mainSplitPane.setDividerLocation(0.0);
                } else {
                    // Top is visible. Just hide Bottom (Divider -> 1.0/Max).
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
        this.terminalTextPane.setEditable(false);

        JScrollPane logScrollPane = new JScrollPane(logTextPane);
        JScrollPane terminalScrollPane = new JScrollPane(terminalTextPane);

        this.bottomEditorTabbedPane.add("Output", terminalScrollPane);
        this.bottomEditorTabbedPane.add("Logs", logScrollPane);

        this.editorSplitPane.setBottomComponent(bottomEditorTabbedPane);

        this.mainSplitPane.setRightComponent(this.editorSplitPane);

        this.bottomEditorTabbedPane.setMinimizeAction(e -> {
            // Toggle Logic:
            if (this.editorSplitPane.getDividerLocation() >= this.editorSplitPane.getMaximumDividerLocation() - 50) {
                // Restore (If explicitly closed) -> Set to 70%
                this.editorSplitPane.setDividerLocation(0.7);
            } else {
                // Minimize -> Set to bottom (1.0 means 100%)
                this.editorSplitPane.setDividerLocation(1.0);
            }
        });
    }

    private void initComponents() {
        this.menuBar = new AppMenuBar(this.AppActions);
        this.mainSplitPane = new JSplitPane();
        this.navigationSplitPane = new JSplitPane();
        this.editorSplitPane = new JSplitPane();
        this.topNavigationTabbedPane = new TabbedPane();
        this.bottomNavigationTabbedPane = new TabbedPane();
        this.fileExplorer = new FileExplorer();
        this.projectExplorer = new FileExplorer();
        this.editorTabbedPane = new EditorTabbedPane(this);
        this.bottomEditorTabbedPane = new TabbedPane();
        this.terminalTextPane = new TextPane();
        this.logTextPane = new TextPane();
    }

    private JMenuBar menuBar;
    private JSplitPane mainSplitPane;
    private JSplitPane navigationSplitPane;
    private JSplitPane editorSplitPane;
    private TabbedPane topNavigationTabbedPane;
    private TabbedPane bottomNavigationTabbedPane;
    private FileExplorer fileExplorer;
    private FileExplorer projectExplorer;
    private EditorTabbedPane editorTabbedPane;
    private TabbedPane bottomEditorTabbedPane;

    private TextPane terminalTextPane;
    private TextPane logTextPane;
}
