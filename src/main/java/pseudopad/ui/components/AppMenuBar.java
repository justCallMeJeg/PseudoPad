package pseudopad.ui.components;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import pseudopad.utils.AppActionsManager;
import pseudopad.utils.ThemeManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class AppMenuBar extends JMenuBar {
    private final AppActionsManager actions;
    
    public AppMenuBar(AppActionsManager actions) {
        this.actions = actions;
        
        initFileMenu();
        initEditMenu();
//        initRunMenu();
        initWindowMenu();
        initHelpMenu();
    }
    
    private void initFileMenu() {
        JMenu fileMenu = new JMenu("File");
        
        // You just add the Action! Swing handles the Text, Icon, AND Shortcut automatically.
        fileMenu.add(new JMenuItem(actions.NEW_PROJECT));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(actions.OPEN_PROJECT));
        fileMenu.add(new JMenuItem(actions.CLOSE_PROJECT));
//        fileMenu.add(new JMenuItem(AppActionsManager.OPEN)); // Assuming you created this
//        fileMenu.addSeparator();
//        fileMenu.add(new JMenuItem(AppActionsManager.SAVE));
        
        add(fileMenu);
    }
    
    private void initEditMenu() {
        JMenu editMenu = new JMenu("Edit");
        add(editMenu);
    }
    
    private void initWindowMenu() {
        JMenu windowMenu = new JMenu("Window");
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
//        themeMenu.addSeparator();
//        themeMenu.add(new JMenuItem("Customize..."));
        
        ThemeManager.THEMES currentTheme = ThemeManager.getInstance().getCurrentTheme();
        
        switch (currentTheme) {
            case LIGHT -> lightItem.setSelected(true);
            case DARK -> darkItem.setSelected(true);
            case SYSTEM -> systemItem.setSelected(true);
        }
        
        windowMenu.add(themeMenu);
        add(windowMenu);
    }
    
    private void initHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        add(helpMenu);
    }
}
