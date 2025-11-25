package pseudopad.utils;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.Toolkit;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import pseudopad.app.MainFrame;
import pseudopad.ui.dialogs.NewProjectDialog;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class AppActionsManager {
    private final MainFrame appFrame;
    
    public AppActionsManager(MainFrame appFrame) {
        this.appFrame = appFrame;
    }
    
    public final Action NEW_PROJECT = new AbstractAction("New Project...", IconManager.get("new_project")) {
        {
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            // Show the dialog
            new NewProjectDialog(appFrame).setVisible(true);
        }
    };
    
    // <editor-fold defaultstate="collapsed" desc="Theme Related Actions">
    public final Action OPEN_PROJECT = new AbstractAction("Open Project...", IconManager.get("open_project")) {
        {
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
            putValue(Action.SHORT_DESCRIPTION, "Open an existing project");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Action: Open Project");
            // Now valid: accessing instance variable from instance context
            appFrame.showOpenProjectDialog();
        }
    };
    
    public final Action THEME_LIGHT = new AbstractAction("Light") {
        {
            putValue(Action.SHORT_DESCRIPTION, "Change UI theme to Light Mode");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Action: Change Theme to Light");
            // Now valid: accessing instance variable from instance context
            appFrame.changeTheme(ThemeManager.THEMES.LIGHT);
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
            appFrame.changeTheme(ThemeManager.THEMES.DARK);
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
            appFrame.changeTheme(ThemeManager.THEMES.SYSTEM);
        }
    };
    // </editor-fold>
}
