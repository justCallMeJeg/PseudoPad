package pseudopad.ui.components;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import pseudopad.app.ActionController;
import pseudopad.utils.ThemeManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class AppMenuBar extends JMenuBar {
    private final ActionController actions;

    public AppMenuBar(ActionController actions) {
        this.actions = actions;

        initFileMenu();
        initEditMenu();
        initRunMenu();
        initWindowMenu();
        initHelpMenu();
    }

    private void initFileMenu() {
        JMenu fileMenu = new JMenu("File");

        // You just add the Action! Swing handles the Text, Icon, AND Shortcut
        // automatically.
        fileMenu.add(new JMenuItem(actions.NEW_PROJECT));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(actions.OPEN_PROJECT));
        fileMenu.add(new JMenuItem(actions.CLOSE_PROJECT));
        // fileMenu.add(new JMenuItem(AppActionsManager.OPEN)); // Assuming you created
        // this
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(actions.SAVE));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(actions.OPEN_SETTINGS));

        add(fileMenu);
    }

    private void initEditMenu() {
        JMenu editMenu = new JMenu("Edit");

        editMenu.add(new JMenuItem(actions.UNDO));
        editMenu.add(new JMenuItem(actions.REDO));
        editMenu.addSeparator();
        editMenu.add(new JMenuItem(actions.CUT));
        editMenu.add(new JMenuItem(actions.COPY));
        editMenu.add(new JMenuItem(actions.PASTE));
        editMenu.add(new JMenuItem(actions.DELETE));

        add(editMenu);
    }

    private void initRunMenu() {
        JMenu runMenu = new JMenu("Run");
        runMenu.add(new JMenuItem(actions.RUN_PROJECT));
        add(runMenu);
    }

    private void initWindowMenu() {
        JMenu windowMenu = new JMenu("Window");

        // Primary Toggles
        windowMenu.add(new JMenuItem(actions.TOGGLE_NAV_PANEL));
        windowMenu.add(new JMenuItem(actions.TOGGLE_OUTPUT_PANEL));

        windowMenu.addSeparator();

        // Individual Panel Toggles
        addPanelToggle(windowMenu, actions.TOGGLE_PROJECTS, pseudopad.app.WindowManager.PANEL_PROJECTS);
        addPanelToggle(windowMenu, actions.TOGGLE_FILES, pseudopad.app.WindowManager.PANEL_FILES);
        addPanelToggle(windowMenu, actions.TOGGLE_FILE_OUTLINE, pseudopad.app.WindowManager.PANEL_FILE_OUTLINE);
        windowMenu.addSeparator();
        addPanelToggle(windowMenu, actions.TOGGLE_OUTPUT, pseudopad.app.WindowManager.PANEL_OUTPUT);
        addPanelToggle(windowMenu, actions.TOGGLE_PROBLEMS, pseudopad.app.WindowManager.PANEL_PROBLEMS);
        addPanelToggle(windowMenu, actions.TOGGLE_LOGS, pseudopad.app.WindowManager.PANEL_LOGS);

        windowMenu.addSeparator();
        windowMenu.add(new JMenuItem(actions.RESET_WINDOWS));
        windowMenu.addSeparator();

        JMenu themeMenu = new JMenu("Theme");

        javax.swing.ButtonGroup themeGroup = new javax.swing.ButtonGroup();

        JRadioButtonMenuItem lightItem = new JRadioButtonMenuItem(actions.THEME_LIGHT);
        JRadioButtonMenuItem darkItem = new JRadioButtonMenuItem(actions.THEME_DARK);
        JRadioButtonMenuItem systemItem = new JRadioButtonMenuItem(actions.THEME_SYSTEM);

        themeGroup.add(lightItem);
        themeGroup.add(darkItem);
        themeGroup.add(systemItem);

        themeMenu.add(lightItem);
        themeMenu.add(darkItem);
        themeMenu.add(systemItem);

        ThemeManager.THEMES currentTheme = ThemeManager.getInstance().getCurrentTheme();

        switch (currentTheme) {
            case LIGHT -> lightItem.setSelected(true);
            case DARK -> darkItem.setSelected(true);
            case SYSTEM -> systemItem.setSelected(true);
        }

        windowMenu.add(themeMenu);
        add(windowMenu);
    }

    private void addPanelToggle(JMenu menu, javax.swing.Action action, String panelId) {
        javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(action);

        // Initialize state
        item.setSelected(pseudopad.app.WindowManager.getInstance().isPanelVisible(panelId));

        // Listen for external changes (e.g. ActivityBar restore)
        pseudopad.app.WindowManager.getInstance().addPanelChangeListener((id, visible) -> {
            if (id.equals(panelId)) {
                item.setSelected(visible);
            }
        });

        menu.add(item);
    }

    private void initHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        add(helpMenu);
    }
}
