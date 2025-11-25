package pseudopad.utils;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;
import java.awt.Font;
import java.awt.Window;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class ThemeManager {
    private static ThemeManager INSTANCE;
    private static THEMES currentTheme;
    
    public enum THEMES { LIGHT, DARK, SYSTEM }
    
    private final boolean isSystemDarkMode = OsThemeDetector.getDetector().isDark();
    
    private ThemeManager(){}
    
    public static ThemeManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ThemeManager();
        }
        return INSTANCE;
    }
    
    public static void init() {
        currentTheme = PreferenceManager.getInstance().loadTheme();
        
        try {
            applyTheme(currentTheme);
            
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 12));
            UIManager.put("SplitPane.showsTypeZeroStub", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void changeTheme(THEMES theme) {
        PreferenceManager.getInstance().saveTheme(theme);
        currentTheme = theme;
        
        switch (theme) {
            case LIGHT -> updateLookAndFeel(new FlatLightLaf());
            case DARK -> updateLookAndFeel(new FlatDarkLaf());
            default -> {
                if (isSystemDarkMode) 
                    updateLookAndFeel(new FlatDarkLaf());
                else 
                    updateLookAndFeel(new FlatLightLaf());
            }
        }
    }
    
    private static void applyTheme(THEMES theme) {        
        switch (theme) {
            case LIGHT -> FlatLightLaf.setup();
            case DARK -> FlatDarkLaf.setup();
            default -> {
                if (OsThemeDetector.getDetector().isDark()) {
                    FlatDarkLaf.setup();
                } else {
                    FlatLightLaf.setup();
                }
            }
        }
    }
    
    private void updateLookAndFeel(FlatLaf newLaf) {
        try {
            UIManager.setLookAndFeel(newLaf);
            
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
        } catch (Exception ex) {
            System.err.println("Failed to set LookAndFeel: " + ex);
        }
    }
    
    public THEMES getCurrentTheme() {
        return currentTheme;
    }
    
    public boolean isDarkMode() {
        if (currentTheme == THEMES.SYSTEM) {
            return isSystemDarkMode;
        }
        return currentTheme == THEMES.DARK;
    }
}
