package pseudopad.app;

import pseudopad.utils.ThemeManager;

/**
 * Interface defining the actions that can be performed in the application.
 * Decouples the UI/Actions from the concrete MainFrame implementation.
 */
public interface AppController {
    void newProject();

    void openProject();

    void closeProject();

    void saveCurrentFile();

    void undoAction();

    void redoAction();

    void cutContent();

    void copyContent();

    void pasteContent();

    void deleteItem();

    void changeTheme(ThemeManager.THEMES theme);

    void toggleNavigationPanel();

    void toggleOutputPanel();
}
