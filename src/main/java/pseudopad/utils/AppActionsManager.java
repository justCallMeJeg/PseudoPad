package pseudopad.utils;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.Toolkit;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import pseudopad.app.AppController;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class AppActionsManager {
    private final AppController appController;

    public AppActionsManager(AppController appController) {
        this.appController = appController;
    }

    public final Action NEW_PROJECT = new AbstractAction("New Project...", IconManager.get("new_project")) {
        {
            setup(this, "new_project",
                    KeyStroke.getKeyStroke(KeyEvent.VK_N,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Action: New Project");
            appController.newProject();
        }
    };

    public final Action OPEN_PROJECT = new AbstractAction("Open Project...") {
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

    public final Action CLOSE_PROJECT = new AbstractAction("Close Project") {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Action: Close Project");
            appController.closeProject();
        }
    };

    public final Action SAVE = new AbstractAction("Save") {
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

    // <editor-fold defaultstate="collapsed" desc="Edit Actions">
    public final Action UNDO = new AbstractAction("Undo") {
        {
            setup(this, "undo",
                    KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.undoAction();
        }
    };

    public final Action REDO = new AbstractAction("Redo") {
        {
            setup(this, "redo",
                    KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.redoAction();
        }
    };

    public final Action CUT = new AbstractAction("Cut") {
        {
            setup(this, "content_cut",
                    KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.cutContent();
        }
    };

    public final Action COPY = new AbstractAction("Copy") {
        {
            setup(this, "content_copy",
                    KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.copyContent();
        }
    };

    public final Action PASTE = new AbstractAction("Paste") {
        {
            setup(this, "content_paste",
                    KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.pasteContent();
        }
    };

    public final Action DELETE = new AbstractAction("Delete") {
        {
            setup(this, "delete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.deleteItem();
        }
    };
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Theme Related Actions">
    public final Action THEME_LIGHT = new AbstractAction("Light") {
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

    public final Action THEME_DARK = new AbstractAction("Dark") {
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

    public final Action THEME_SYSTEM = new AbstractAction("System") {
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
    public final Action TOGGLE_NAV_PANEL = new AbstractAction("Toggle Navigation") {
        {
            setup(this, "sidebar",
                    KeyStroke.getKeyStroke(KeyEvent.VK_1, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            appController.toggleNavigationPanel();
        }
    };

    public final Action TOGGLE_OUTPUT_PANEL = new AbstractAction("Toggle Output") {
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

    // <editor-fold defaultstate="collapsed" desc="Run Actions">
    public final Action RUN_PROJECT = new AbstractAction("Run Project") {
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
    // </editor-fold>
}
