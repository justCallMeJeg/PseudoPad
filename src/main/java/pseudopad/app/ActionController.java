package pseudopad.app;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.Toolkit;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import pseudopad.settings.SettingsManager;
import pseudopad.ui.settings.SettingsDialog;
import pseudopad.utils.IconManager;
import pseudopad.utils.I18nManager;
import pseudopad.utils.ThemeManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class ActionController {
    private final AppController appController;

    public ActionController(AppController appController) {
        this.appController = appController;

        // Listen for theme changes and refresh all action icons
        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName())) {
                SwingUtilities.invokeLater(this::refreshIcons);
            }
        });
    }

    public final Action NEW_PROJECT = new AbstractAction(I18nManager.get("action.new_project"),
            IconManager.get("new_project")) {
        {
            setup(this, "new_project", KeyStroke.getKeyStroke(KeyEvent.VK_N,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Action: New Project");
            appController.newProject();
        }
    };

    public final Action OPEN_PROJECT = new AbstractAction(I18nManager.get("action.open_project")) {
        {
            setup(this, "open_project",
                    KeyStroke.getKeyStroke(KeyEvent.VK_O,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Action: Open Project");
            appController.openProject();
        }
    };

    public final Action CLOSE_PROJECT = new AbstractAction(I18nManager.get("action.close_project")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Action: Close Project");
            appController.closeProject();
        }
    };

    public final Action SAVE = new AbstractAction(I18nManager.get("action.save")) {
        {
            setup(this, "save",
                    KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Action: Save");
            appController.saveCurrentFile();
        }
    };

    public final Action OPEN_SETTINGS = new AbstractAction(I18nManager.get("action.settings")) {
        {
            setup(this, "settings",
                    KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Action: Open Settings");
            Frame frame = (Frame) SwingUtilities.getWindowAncestor((java.awt.Component) e.getSource());
            new SettingsDialog(frame).setVisible(true);
        }
    };

    public final Action OPEN_GLOBAL_SETTINGS = new AbstractAction(I18nManager.get("action.settings.global")) {
        {
            putValue(Action.SMALL_ICON, IconManager.get("settings"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Action: Open Global Settings JSON");
            File settingsFile = SettingsManager.getInstance().getGlobalSettingsFile();
            openFileInEditor(settingsFile);
        }
    };

    public final Action OPEN_PROJECT_SETTINGS = new AbstractAction(I18nManager.get("action.settings.project")) {
        {
            putValue(Action.SMALL_ICON, IconManager.get("settings"));
            setEnabled(false); // Disabled until a project is opened
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Action: Open Project Settings JSON");
            File settingsFile = SettingsManager.getInstance().getProjectSettingsFile();
            if (settingsFile != null) {
                // Create file if it doesn't exist
                if (!settingsFile.exists()) {
                    SettingsManager.getInstance().saveProjectSettings();
                }
                openFileInEditor(settingsFile);
            } else {
                JOptionPane.showMessageDialog(null,
                        "No project is currently open.",
                        "Project Settings",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    };

    private void openFileInEditor(File file) {
        if (file == null)
            return;
        try {
            // Ensure parent directory exists
            file.getParentFile().mkdirs();

            // Create file with default JSON object if it doesn't exist
            if (!file.exists()) {
                java.nio.file.Files.writeString(file.toPath(), "{\n}");
            }

            // Open in the app's editor
            MainFrame mainFrame = MainFrame.getInstance();
            if (mainFrame != null && mainFrame.getEditorTabbedPane() != null) {
                mainFrame.getEditorTabbedPane().openFileTab(file);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    I18nManager.get("msg.error.settings_open") + " " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Edit Actions">
    public final Action UNDO = new AbstractAction(I18nManager.get("action.undo")) {
        {
            setup(this, "undo",
                    KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.undoAction();
        }
    };

    public final Action REDO = new AbstractAction(I18nManager.get("action.redo")) {
        {
            setup(this, "redo",
                    KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.redoAction();
        }
    };

    public final Action CUT = new AbstractAction(I18nManager.get("action.cut")) {
        {
            setup(this, "content_cut",
                    KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.cutContent();
        }
    };

    public final Action COPY = new AbstractAction(I18nManager.get("action.copy")) {
        {
            setup(this, "content_copy",
                    KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.copyContent();
        }
    };

    public final Action PASTE = new AbstractAction(I18nManager.get("action.paste")) {
        {
            setup(this, "content_paste",
                    KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.pasteContent();
        }
    };

    public final Action DELETE = new AbstractAction(I18nManager.get("action.delete")) {
        {
            setup(this, "delete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.deleteItem();
        }
    };
    public final Action FIND = new AbstractAction("Find") {
        {
            setup(this, "search",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.findAction();
        }
    };

    public final Action REPLACE = new AbstractAction("Replace") {
        {
            setup(this, "replace",
                    KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.replaceAction();
        }
    };

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Theme Related Actions">
    public final Action THEME_LIGHT = new AbstractAction(I18nManager.get("theme.light")) {
        {
            putValue(Action.SHORT_DESCRIPTION, "Change UI theme to Light Mode");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Action: Change Theme to Light");
            // Now valid: accessing instance variable from instance context
            appController.changeTheme(ThemeManager.THEMES.LIGHT);
        }
    };

    public final Action THEME_DARK = new AbstractAction(I18nManager.get("theme.dark")) {
        {
            putValue(Action.SHORT_DESCRIPTION, "Change UI theme to Dark Mode");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Action: Change Theme to Dark");
            // Now valid: accessing instance variable from instance context
            appController.changeTheme(ThemeManager.THEMES.DARK);
        }
    };

    public final Action THEME_SYSTEM = new AbstractAction(I18nManager.get("theme.system")) {
        {
            putValue(Action.SHORT_DESCRIPTION, "Change UI theme to OS's Preference");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Action: Change Theme to System");
            // Now valid: accessing instance variable from instance context
            appController.changeTheme(ThemeManager.THEMES.SYSTEM);
        }
    };
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="View Actions">
    public final Action TOGGLE_NAV_PANEL = new AbstractAction(I18nManager.get("action.toggle_nav")) {
        {
            setup(this, "sidebar",
                    KeyStroke.getKeyStroke(KeyEvent.VK_1, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.toggleNavigationPanel();
        }
    };

    public final Action TOGGLE_OUTPUT_PANEL = new AbstractAction(I18nManager.get("action.toggle_output")) {
        {
            setup(this, "terminal",
                    KeyStroke.getKeyStroke(KeyEvent.VK_2, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.toggleOutputPanel();
        }
    };
    // </editor-fold>

    public final Action RUN_PROJECT = new AbstractAction(I18nManager.get("action.run_project")) {
        {
            setup(this, "run",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.runProject();
        }
    };
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Panel Toggle Actions">
    public final Action TOGGLE_PROJECTS = new AbstractAction("Projects") {
        {
            setup(this, "folder",
                    KeyStroke.getKeyStroke(KeyEvent.VK_1,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.togglePanel(WindowManager.PANEL_PROJECTS);
        }
    };

    public final Action TOGGLE_FILES = new AbstractAction("Files") {
        {
            setup(this, "file_tree",
                    KeyStroke.getKeyStroke(KeyEvent.VK_2,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.togglePanel(WindowManager.PANEL_FILES);
        }
    };

    public final Action TOGGLE_FILE_OUTLINE = new AbstractAction("File Outline") {
        {
            setup(this, "outline",
                    KeyStroke.getKeyStroke(KeyEvent.VK_3,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.togglePanel(WindowManager.PANEL_FILE_OUTLINE);
        }
    };

    public final Action TOGGLE_OUTPUT = new AbstractAction("Output") {
        {
            setup(this, "terminal",
                    KeyStroke.getKeyStroke(KeyEvent.VK_4,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.togglePanel(WindowManager.PANEL_OUTPUT);
        }
    };

    public final Action TOGGLE_PROBLEMS = new AbstractAction("Problems") {
        {
            setup(this, "warning",
                    KeyStroke.getKeyStroke(KeyEvent.VK_5,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.togglePanel(WindowManager.PANEL_PROBLEMS);
        }
    };

    public final Action TOGGLE_LOGS = new AbstractAction("Logs") {
        {
            setup(this, "log",
                    KeyStroke.getKeyStroke(KeyEvent.VK_6,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.togglePanel(WindowManager.PANEL_LOGS);
        }
    };

    public final Action RESET_WINDOWS = new AbstractAction(I18nManager.get("action.reset_windows")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            appController.resetWindows();
        }
    };
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Helper Functions">
    private void setup(Action a, String iconName, KeyStroke key) {
        // 1. Menu Icon (16x16)
        a.putValue(Action.SMALL_ICON, IconManager.get(iconName, 16));

        // 2. Toolbar Icon (24x24) - Saved under a special key
        a.putValue(Action.LARGE_ICON_KEY, IconManager.get(iconName, 32));

        // 3. Metadata
        a.putValue(Action.SHORT_DESCRIPTION,
                a.getValue(Action.NAME) + (key != null ? " (" + getKeyString(key) + ")" : ""));
        a.putValue(Action.ACTION_COMMAND_KEY, getKeyString(key));
        if (key != null) {
            a.putValue(Action.ACCELERATOR_KEY, key);
        }
    }

    private String getKeyString(KeyStroke key) {
        // Simple helper to format tooltip text (e.g., "Ctrl+O")
        return KeyEvent.getKeyModifiersText(key.getModifiers()) + "+" + KeyEvent.getKeyText(key.getKeyCode());
    }

    /**
     * Refreshes all action icons after a theme change.
     * Called automatically when the look and feel changes.
     */
    private void refreshIcons() {
        refreshActionIcon(NEW_PROJECT, "new_project");
        refreshActionIcon(OPEN_PROJECT, "open_project");
        refreshActionIcon(SAVE, "save");
        refreshActionIcon(UNDO, "undo");
        refreshActionIcon(REDO, "redo");
        refreshActionIcon(CUT, "content_cut");
        refreshActionIcon(COPY, "content_copy");
        refreshActionIcon(PASTE, "content_paste");
        refreshActionIcon(DELETE, "delete");
        refreshActionIcon(FIND, "search");
        refreshActionIcon(REPLACE, "replace");
        refreshActionIcon(TOGGLE_NAV_PANEL, "sidebar");
        refreshActionIcon(TOGGLE_OUTPUT_PANEL, "terminal");
        refreshActionIcon(RUN_PROJECT, "run");
        refreshActionIcon(TOGGLE_PROJECTS, "folder");
        refreshActionIcon(TOGGLE_FILES, "file_tree");
        refreshActionIcon(TOGGLE_FILE_OUTLINE, "outline");
        refreshActionIcon(TOGGLE_OUTPUT, "terminal");
        refreshActionIcon(TOGGLE_PROBLEMS, "warning");
        refreshActionIcon(TOGGLE_LOGS, "log");
        refreshActionIcon(OPEN_SETTINGS, "settings");
        refreshActionIcon(OPEN_GLOBAL_SETTINGS, "settings");
        refreshActionIcon(OPEN_PROJECT_SETTINGS, "settings");
    }

    private void refreshActionIcon(Action action, String iconName) {
        action.putValue(Action.SMALL_ICON, IconManager.get(iconName, 16));
        action.putValue(Action.LARGE_ICON_KEY, IconManager.get(iconName, 32));
    }

    /**
     * Updates project-related action enabled states.
     * Call this when a project is opened or closed.
     */
    public void updateProjectState(boolean projectOpen) {
        OPEN_PROJECT_SETTINGS.setEnabled(projectOpen);
    }
    // </editor-fold>
}
