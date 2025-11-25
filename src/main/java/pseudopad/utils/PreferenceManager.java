package pseudopad.utils;

import java.io.File;
import java.util.prefs.Preferences;
import pseudopad.app.MainFrame;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class PreferenceManager {
    private static PreferenceManager INSTANCE;
    private final Preferences prefs;

    // --- KEYS ---
    public static enum KEY {
        THEME("app_theme");
        
        private final String keyString;

        // The constructor for the enum constants
        KEY(String keyString) {
            this.keyString = keyString;
        }

        // A public method to retrieve the associated String value
        public String getKey() {
            return this.keyString;
        }

        @Override
        public String toString() {
            return this.keyString;
        }
    }
    
    public static final String KEY_THEME = "app_theme";
    public static final String KEY_LAST_PROJECT = "last_project_path";
    
    // Private constructor for Singleton
    private PreferenceManager() {
        // This creates a node specifically for this class's package
        this.prefs = Preferences.userNodeForPackage(MainFrame.class);
    }

    // Singleton Accessor
    public static PreferenceManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PreferenceManager();
        }
        return INSTANCE;
    }

    public void savePreference(KEY prefKey, String value) {
        prefs.put(prefKey.getKey(), value);
    }
    
    // --- THEME SETTINGS ---
    
    public void saveTheme(ThemeManager.THEMES theme) {
        prefs.put(KEY_THEME, theme.name());
    }

    public ThemeManager.THEMES loadTheme() {
        String themeName = prefs.get(KEY_THEME, ThemeManager.THEMES.SYSTEM.name());
        try {
            return ThemeManager.THEMES.valueOf(themeName);
        } catch (IllegalArgumentException e) {
            return ThemeManager.THEMES.SYSTEM; // Fallback
        }
    }

    // --- PROJECT SETTINGS ---

    public void saveLastProject(File projectPath) {
        if (projectPath != null) {
            prefs.put(KEY_LAST_PROJECT, projectPath.getAbsolutePath());
        }
    }

    public File loadLastProject() {
        String path = prefs.get(KEY_LAST_PROJECT, null);
        if (path != null) {
            File file = new File(path);
            if (file.exists() && file.isDirectory()) {
                return file;
            }
        }
        return null; // No valid last project found
    }
}
