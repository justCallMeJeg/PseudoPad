package pseudopad.app;

import pseudopad.ui.MainLayout;
import java.awt.Image;
import java.awt.Taskbar;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import pseudopad.ui.components.TabbedPane;
import pseudopad.ui.dialogs.NewProjectDialog;
import pseudopad.ui.dialogs.OpenProjectDialog;
import pseudopad.utils.PreferenceManager;
import pseudopad.utils.ThemeManager;
import pseudopad.editor.EditorTabbedPane;
import pseudopad.project.ProjectConfig;
import pseudopad.project.ProjectContext;

import java.awt.KeyboardFocusManager;
import java.awt.Component;
import java.awt.Color;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class MainFrame extends JFrame implements AppController {
    private static MainFrame INSTANCE;

    private final ProjectContext projectContext = new ProjectContext(this);
    private final ActionController AppActions = new ActionController(this);
    private final MainLayout mainLayout;
    private File pendingProjectToLoad;

    public MainFrame() {
        INSTANCE = this;

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setMinimumSize(AppConstants.MIN_WINDOW_SIZE);

        this.mainLayout = new MainLayout(this, AppActions);
        this.setContentPane(mainLayout);

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
        projectContext.closeProject();
        super.dispose();
        if (INSTANCE == this) {
            INSTANCE = null;
        }
    }

    private void saveWindowState() {
        int width = getWidth();
        int height = getHeight();
        int x = getX();
        int y = getY();
        int state = getExtendedState();

        int divMain = mainLayout.getMainSplitPane().getDividerLocation();
        int divEditor = mainLayout.getEditorSplitPane().getDividerLocation();
        int divNav = mainLayout.getNavigationSplitPane().getDividerLocation();

        if (projectContext.getProjectPath() != null) {
            ProjectConfig config = projectContext.getConfig();
            if (config == null)
                config = new ProjectConfig(projectContext.getProjectPath().getName());

            config.windowWidth = width;
            config.windowHeight = height;
            config.windowX = x;
            config.windowY = y;
            config.windowState = state;

            config.dividerMain = divMain;
            config.dividerEditor = divEditor;
            config.dividerNav = divNav;

            // Save Session State
            if (mainLayout.getEditorTabbedPane() != null) {
                config.openFiles = mainLayout.getEditorTabbedPane().getOpenFiles();
                config.activeFile = mainLayout.getEditorTabbedPane().getActiveFile();
            }

            projectContext.setConfig(config);
            projectContext.saveConfig();
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

        if (projectPath != null) {
            projectContext.loadProject(projectPath);
        }

        // Determine initial bounds and state
        int w = 1280, h = 720, x = -1, y = -1, state = JFrame.MAXIMIZED_BOTH; // Default to Maximized
        int divMain = -1, divEditor = -1, divNav = -1;

        if (projectContext.getProjectPath() != null) {
            ProjectConfig config = projectContext.getConfig();
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
        if (projectContext.getProjectPath() != null) {
            AppLogger.info("Launching project: " + projectContext.getProjectPath().getAbsolutePath());
            this.setTitle(projectContext.getProjectPath().getName() + " - " + AppConstants.APP_TITLE);
            mainLayout.getFileExplorer().openProject(projectPath);
            mainLayout.setTerminalProjectName(projectContext.getProjectPath().getName());

            // File Watcher is now handled by ProjectContext

            // Save as Last Project
            PreferenceManager.getInstance().saveLastProject(projectContext.getProjectPath());

            // Add to Recent Projects
            PreferenceManager.getInstance().addRecentProject(projectContext.getProjectPath());
            if (mainLayout.getRecentProjectsPanel() != null) {
                mainLayout.getRecentProjectsPanel().refresh();
            }
        } else {
            this.setTitle(AppConstants.APP_TITLE);
            // this.editorSplitPane.setTopComponent(new FallbackPanel(INSTANCE));
        }

        if (mainLayout.getEditorTabbedPane() != null) {
            mainLayout.getEditorTabbedPane().refreshFallbackState();
        }

        // Restore Session State (Open Files)
        if (projectContext.getProjectPath() != null) {
            ProjectConfig config = projectContext.getConfig();
            if (config != null && config.openFiles != null) {
                for (String filePath : config.openFiles) {
                    File f = new File(filePath);
                    if (f.exists()) {
                        mainLayout.getEditorTabbedPane().openFileTab(f);
                    }
                }
                if (config.activeFile != null) {
                    File f = new File(config.activeFile);
                    if (f.exists()) {
                        mainLayout.getEditorTabbedPane().openFileTab(f); // Selects it
                    }
                }
            }
            // Select "Files" tab in Top Navigation
            if (mainLayout.getTopNavigationTabbedPane().getTabCount() > 1) {
                mainLayout.getTopNavigationTabbedPane().setSelectedIndex(1);
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
            if (projectContext.getProjectPath() == null) {
                mainLayout.getMainSplitPane().setDividerLocation(0.0); // Enforce hidden if no project
            } else if (fDivMain > 0) { // Check > 0 to avoid collapsing if uninitialized
                mainLayout.getMainSplitPane().setDividerLocation(fDivMain);
            } else {
                mainLayout.getMainSplitPane().setDividerLocation(0.25); // Default show
            }

            // Set Editor Split (Editor vs Console)
            if (fDivEditor > 0) // Check > 0
                mainLayout.getEditorSplitPane().setDividerLocation(fDivEditor);
            else {
                if (projectContext.getProjectPath() != null) {
                    mainLayout.getEditorSplitPane().setDividerLocation(0.75); // Show output panel
                } else {
                    mainLayout.getEditorSplitPane().setDividerLocation(1.0); // Hide output panel
                }
            }

            // Set Navigation Split (Files vs Outline)
            if (fDivNav > 0)
                mainLayout.getNavigationSplitPane().setDividerLocation(fDivNav);
            else
                mainLayout.getNavigationSplitPane().setDividerLocation(0.5);

            // Ensure window is on top and focused after UI setup
            this.toFront();
            this.requestFocus();

            setStatusMessage("Ready");
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
        if (projectContext.getProjectPath() == null) {
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
        if (mainLayout.getEditorTabbedPane() != null) {
            AppLogger.info("Saving current file...");
            // Delegate deeper to the specific component
            mainLayout.getEditorTabbedPane().saveActiveTab();
        }
    }

    public void undoAction() {
        if (mainLayout.getEditorTabbedPane() != null)
            mainLayout.getEditorTabbedPane().undo();
    }

    public void redoAction() {
        if (mainLayout.getEditorTabbedPane() != null)
            mainLayout.getEditorTabbedPane().redo();
    }

    public void cutContent() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, mainLayout.getFileExplorer())) {
            mainLayout.getFileExplorer().cut();
        } else if (mainLayout.getEditorTabbedPane() != null) {
            mainLayout.getEditorTabbedPane().cut();
        }
    }

    public void copyContent() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, mainLayout.getFileExplorer())) {
            mainLayout.getFileExplorer().copy();
        } else if (mainLayout.getEditorTabbedPane() != null) {
            mainLayout.getEditorTabbedPane().copy();
        }
    }

    public void pasteContent() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, mainLayout.getFileExplorer())) {
            mainLayout.getFileExplorer().paste();
        } else if (mainLayout.getEditorTabbedPane() != null) {
            mainLayout.getEditorTabbedPane().paste();
        }
    }

    public void deleteItem() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, mainLayout.getFileExplorer())) {
            mainLayout.getFileExplorer().deleteSelectedFile();
        } else {
            // Optional: Handle deleteItem in editor (e.g. deleteItem text) if needed,
            // but usually DELETE key is handled by text component directly.
            // We could delegate to active tab if we want "Delete File" action globally,
            // but that might be dangerous/confusing if user just wants to deleteItem text.
            // For now, let's only delegate to FileExplorer if focused.
        }
    }

    public void refreshFileExplorer() {
        if (mainLayout.getFileExplorer() != null) {
            mainLayout.getFileExplorer().refresh();
        }
    }

    public void closeProject() {
        AppLogger.info("Closing project: "
                + (projectContext.getProjectPath() != null ? projectContext.getProjectPath().getName() : "Unknown"));

        projectContext.closeProject();
        PreferenceManager.getInstance().saveLastProject(null);

        this.setTitle(AppConstants.APP_TITLE);

        // 1. Clear the File Explorer
        // (Assuming FileExplorer has a clear() method)
        mainLayout.getFileExplorer().clear();

        // 2. Close all open tabs
        mainLayout.getEditorTabbedPane().removeAll();

        // 3. Force Fallback Panel to update (It's now visible because we removed all
        // tabs)
        mainLayout.getEditorTabbedPane().refreshFallbackState();

        // 4. Reset Layout
        mainLayout.resetLayout();
    }

    public void changeTheme(ThemeManager.THEMES theme) {
        ThemeManager.getInstance().changeTheme(theme);
    }

    public File getCurrentProjectPath() {
        return projectContext.getProjectPath();
    }

    public MainFrame getAppInstance() {
        return INSTANCE;
    }

    public static MainFrame getInstance() {
        return INSTANCE;
    }

    public ActionController getAppActionInstance() {
        return this.AppActions;
    }

    public EditorTabbedPane getEditorTabbedPane() {
        return mainLayout.getEditorTabbedPane();
    }

    public pseudopad.ui.components.TabbedPane getBottomEditorTabbedPane() {
        return mainLayout.getBottomEditorTabbedPane();
    }

    public void appendLog(String message, Color color) {
        mainLayout.appendLog(message, color);
    }

    public void setStatusMessage(String message) {
        if (mainLayout.getStatusBar() != null) {
            mainLayout.getStatusBar().setMessage(message);
        }
    }

    public void toggleNavigationPanel() {
        // Toggle Logic for Main Split (Left vs Right)
        if (mainLayout.getMainSplitPane().getDividerLocation() < 50) {
            // It's closed/minimized -> Restore
            mainLayout.getMainSplitPane().setDividerLocation(0.25);
        } else {
            // It's open -> Close
            mainLayout.getMainSplitPane().setDividerLocation(0.0);
        }
    }

    public void toggleOutputPanel() {
        // Toggle Logic for Editor Split (Top vs Bottom)
        // Note: Divider at 1.0 means bottom is hidden
        if (mainLayout.getEditorSplitPane()
                .getDividerLocation() >= mainLayout.getEditorSplitPane().getMaximumDividerLocation() - 50) {
            // It's closed -> Restore
            mainLayout.getEditorSplitPane().setDividerLocation(0.75);
        } else {
            // It's open -> Close
            mainLayout.getEditorSplitPane().setDividerLocation(1.0);
        }
    }

    @Override
    public void runProject() {
        // 1. Save File
        saveCurrentFile();

        // 2. Ensure output panel area is visible (divider)
        if (mainLayout.getEditorSplitPane()
                .getDividerLocation() >= mainLayout.getEditorSplitPane().getMaximumDividerLocation() - 50) {
            mainLayout.getEditorSplitPane().setDividerLocation(0.75);
        }

        // 3. Switch to "Output" tab
        if (mainLayout.getBottomEditorTabbedPane() != null) {
            TabbedPane bottomTabs = mainLayout.getBottomEditorTabbedPane();
            for (int i = 0; i < bottomTabs.getTabCount(); i++) {
                if ("Output".equals(bottomTabs.getTitleAt(i))) {
                    bottomTabs.setSelectedIndex(i);
                    break;
                }
            }
        }

        // 4. Execute "run" command
        mainLayout.runTerminalCommand("run");
    }

    // ----- UI/UX Logic -----

    // initUIComponents and initComponents moved to MainLayout
}
