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
        } else {
            prefs.remove(KEY_LAST_PROJECT);
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

    // --- WINDOW SETTINGS (Global Fallback) ---
    private static final String KEY_WIN_WIDTH = "win_width";
    private static final String KEY_WIN_HEIGHT = "win_height";
    private static final String KEY_WIN_X = "win_x";
    private static final String KEY_WIN_Y = "win_y";
    private static final String KEY_WIN_STATE = "win_state";
    private static final String KEY_DIV_MAIN = "div_main";
    private static final String KEY_DIV_EDITOR = "div_editor";
    private static final String KEY_DIV_NAV = "div_nav";

    public void saveWindowState(int width, int height, int x, int y, int state) {
        prefs.putInt(KEY_WIN_WIDTH, width);
        prefs.putInt(KEY_WIN_HEIGHT, height);
        prefs.putInt(KEY_WIN_X, x);
        prefs.putInt(KEY_WIN_Y, y);
        prefs.putInt(KEY_WIN_STATE, state);
    }

    public int[] loadWindowState() {
        // Returns [width, height, x, y, state]
        // Defaults: 1280x720, -1, -1, Normal
        return new int[] {
                prefs.getInt(KEY_WIN_WIDTH, 1280),
                prefs.getInt(KEY_WIN_HEIGHT, 720),
                prefs.getInt(KEY_WIN_X, -1),
                prefs.getInt(KEY_WIN_Y, -1),
                prefs.getInt(KEY_WIN_STATE, 0)
        };
    }

    public void saveDividerLocations(int main, int editor, int nav) {
        prefs.putInt(KEY_DIV_MAIN, main);
        prefs.putInt(KEY_DIV_EDITOR, editor);
        prefs.putInt(KEY_DIV_NAV, nav);
    }

    public int[] loadDividerLocations() {
        // Returns [main, editor, nav]
        return new int[] {
                prefs.getInt(KEY_DIV_MAIN, -1),
                prefs.getInt(KEY_DIV_EDITOR, -1),
                prefs.getInt(KEY_DIV_NAV, -1)
        };
    }

    // --- DIALOG SETTINGS ---
    private static final String KEY_LAST_DIALOG_DIR = "last_dialog_dir";

    public void saveLastDialogDir(File dir) {
        if (dir != null) {
            prefs.put(KEY_LAST_DIALOG_DIR, dir.getAbsolutePath());
        }
    }

    public File loadLastDialogDir() {
        String path = prefs.get(KEY_LAST_DIALOG_DIR, null);
        if (path != null) {
            File file = new File(path);
            if (file.exists() && file.isDirectory()) {
                return file;
            }
        }
        return new File(System.getProperty("user.home")); // Default
    }
}
