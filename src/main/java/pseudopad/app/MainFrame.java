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
    private final ThemeManager themeManager = ThemeManager.getInstance();
    private final PreferenceManager AppPref = PreferenceManager.getInstance();
    private final AppActionsManager AppActions = new AppActionsManager(this);
    private ProjectFileWatcher projectFileWatcher;

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

    public void launchAppInstance(File projectPath) {
        this.currentProjectPath = projectPath;

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
        } else {
            this.setTitle(AppConstants.APP_TITLE);
            // this.editorSplitPane.setTopComponent(new FallbackPanel(INSTANCE));
        }

        if (this.editorTabbedPane != null) {
            this.editorTabbedPane.refreshFallbackState();
        }

        // 3. Set Divider Locations LAST, inside invokeLater
        // This places the request at the end of the Event Queue, ensuring
        // the window is fully drawn before the dividers try to move.
        SwingUtilities.invokeLater(() -> {
            // Set Main Split (Navigation vs Editor)
            this.mainSplitPane.setDividerLocation(0.25); // e.g. 25% for file tree

            // Set Editor Split (Editor vs Console)
            if (currentProjectPath != null) {
                this.editorSplitPane.setDividerLocation(0.75);
            } else {
                this.editorSplitPane.setDividerLocation(1.0);
            }

            // Set Navigation Split (Files vs Outline)
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

    public void undo() {
        if (editorTabbedPane != null)
            editorTabbedPane.undo();
    }

    public void redo() {
        if (editorTabbedPane != null)
            editorTabbedPane.redo();
    }

    public void cut() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, fileExplorer)) {
            fileExplorer.cut();
        } else if (editorTabbedPane != null) {
            editorTabbedPane.cut();
        }
    }

    public void copy() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, fileExplorer)) {
            fileExplorer.copy();
        } else if (editorTabbedPane != null) {
            editorTabbedPane.copy();
        }
    }

    public void paste() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, fileExplorer)) {
            fileExplorer.paste();
        } else if (editorTabbedPane != null) {
            editorTabbedPane.paste();
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
                logTextPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    // ----- UI/UX Logic -----

    private void initUIComponents() {
        initComponents();

        Border lineBorder = BorderFactory.createLineBorder(UIManager.getColor("Panel.foreground").darker(), 1);

        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
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
            // Toggle Logic:
            if (this.navigationSplitPane.getDividerLocation() >= this.editorSplitPane.getMaximumDividerLocation()
                    - 50) {
                // Restore (If explicitly closed) -> Set to 70%
                this.navigationSplitPane.setDividerLocation(0.5);
            } else {
                // Minimize -> Set to bottom (1.0 means 100%)
                this.navigationSplitPane.setDividerLocation(0);
            }
        });

        this.bottomNavigationTabbedPane.setMinimumSize(new Dimension(200, 100));
        this.bottomNavigationTabbedPane.add("Navigation", new JPanel().add(new JLabel("File Navigation Panel")));
        this.bottomNavigationTabbedPane.setMinimizeAction(e -> {
            // Toggle Logic:
            if (this.navigationSplitPane.getDividerLocation() >= this.editorSplitPane.getMaximumDividerLocation()
                    - 50) {
                // Restore (If explicitly closed) -> Set to 70%
                this.navigationSplitPane.setDividerLocation(0.5);
            } else {
                // Minimize -> Set to bottom (1.0 means 100%)
                this.navigationSplitPane.setDividerLocation(1.0);
            }
        });

        this.navigationSplitPane.setTopComponent(this.topNavigationTabbedPane);
        this.navigationSplitPane.setBottomComponent(this.bottomNavigationTabbedPane);

        this.mainSplitPane.setLeftComponent(this.navigationSplitPane);

        this.editorSplitPane.setBorder(lineBorder);
        this.editorSplitPane.setResizeWeight(AppConstants.EDITOR_SPLIT_RESIZE_WEIGHT);
        this.editorSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        this.editorSplitPane.setTopComponent(editorTabbedPane);
        
        this.bottomEditorTabbedPane.add("Output", terminalTextPane);
        this.bottomEditorTabbedPane.add("Logs", logTextPane);

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
