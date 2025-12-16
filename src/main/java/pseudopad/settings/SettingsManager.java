package pseudopad.settings;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import pseudopad.utils.I18nManager;
import pseudopad.utils.ThemeManager;

/**
 * Central settings manager providing type-safe access to all application
 * settings.
 * Uses JSON files for persistence with two-tier support:
 * - Global settings: ~/.pseudopad/settings.json
 * - Project settings: <project>/.pseudopad/settings.json (overrides global)
 * 
 * @author Geger John Paul Gabayeron
 */
public class SettingsManager {
    private static SettingsManager INSTANCE;

    // JSON storage
    private static final String SETTINGS_DIR = pseudopad.app.AppConstants.CONFIG_DIR_NAME;
    private static final String SETTINGS_FILE = "settings.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // In-memory caches
    private Map<String, Object> globalSettings = new LinkedHashMap<>();
    private Map<String, Object> projectSettings = new LinkedHashMap<>();
    private File currentProjectPath = null;

    // Listeners map: key -> list of listeners
    private final Map<SettingsKey<?>, List<SettingsChangeListener<?>>> listeners = new ConcurrentHashMap<>();

    // Registry of all known settings keys
    private final List<SettingsKey<?>> allKeys = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════════════
    // APPEARANCE SETTINGS (Global only)
    // ═══════════════════════════════════════════════════════════════════════

    public static final SettingsKey<String> THEME = new SettingsKey<>(
            "appearance.theme", SettingsCategory.APPEARANCE, "SYSTEM",
            String.class, "Theme", "Application color theme (LIGHT, DARK, SYSTEM)", false);

    public static final SettingsKey<String> APPEARANCE_LANGUAGE = new SettingsKey<>(
            "appearance.language", SettingsCategory.APPEARANCE, "English",
            String.class, "Language", "Application language (English, Spanish)", false);

    public static final SettingsKey<Integer> ICON_SIZE = new SettingsKey<>(
            "appearance.icon.size", SettingsCategory.APPEARANCE, 16,
            Integer.class, "Icon Size", "Default icon size in pixels", false);

    // ═══════════════════════════════════════════════════════════════════════
    // EDITOR SETTINGS (Project overridable)
    // ═══════════════════════════════════════════════════════════════════════

    public static final SettingsKey<String> EDITOR_FONT_FAMILY = new SettingsKey<>(
            "editor.font.family", SettingsCategory.EDITOR, "Consolas",
            String.class, "Font Family", "Font used in the code editor", true);

    public static final SettingsKey<Integer> EDITOR_FONT_SIZE = new SettingsKey<>(
            "editor.font.size", SettingsCategory.EDITOR, 14,
            Integer.class, "Font Size", "Font size in points", true);

    public static final SettingsKey<Integer> EDITOR_TAB_WIDTH = new SettingsKey<>(
            "editor.tab.width", SettingsCategory.EDITOR, 4,
            Integer.class, "Tab Width", "Number of spaces per tab", true);

    public static final SettingsKey<Boolean> EDITOR_SHOW_LINE_NUMBERS = new SettingsKey<>(
            "editor.show.line.numbers", SettingsCategory.EDITOR, true,
            Boolean.class, "Show Line Numbers", "Display line numbers in the gutter", true);

    public static final SettingsKey<Boolean> EDITOR_WORD_WRAP = new SettingsKey<>(
            "editor.word.wrap", SettingsCategory.EDITOR, false,
            Boolean.class, "Word Wrap", "Wrap long lines to fit the editor width", true);

    // ═══════════════════════════════════════════════════════════════════════
    // BEHAVIOR SETTINGS (Mixed)
    // ═══════════════════════════════════════════════════════════════════════

    public static final SettingsKey<Boolean> AUTO_SAVE = new SettingsKey<>(
            "behavior.auto.save", SettingsCategory.BEHAVIOR, false,
            Boolean.class, "Auto-Save", "Automatically save files", true);

    public static final SettingsKey<Integer> AUTO_SAVE_INTERVAL = new SettingsKey<>(
            "behavior.auto.save.interval", SettingsCategory.BEHAVIOR, 30,
            Integer.class, "Auto-Save Interval", "Seconds between auto-saves", true);

    public static final SettingsKey<Boolean> CONFIRM_EXIT = new SettingsKey<>(
            "behavior.confirm.exit", SettingsCategory.BEHAVIOR, true,
            Boolean.class, "Confirm Exit", "Show confirmation dialog before exiting", false);

    public static final SettingsKey<Boolean> REOPEN_LAST_PROJECT = new SettingsKey<>(
            "behavior.reopen.last.project", SettingsCategory.BEHAVIOR, true,
            Boolean.class, "Reopen Last Project", "Automatically open the last project on startup", false);

    // ═══════════════════════════════════════════════════════════════════════
    // TERMINAL SETTINGS (Global only)
    // ═══════════════════════════════════════════════════════════════════════

    public static final SettingsKey<String> TERMINAL_FONT_FAMILY = new SettingsKey<>(
            "terminal.font.family", SettingsCategory.TERMINAL, "Consolas",
            String.class, "Terminal Font", "Font used in the terminal", false);

    public static final SettingsKey<Integer> TERMINAL_FONT_SIZE = new SettingsKey<>(
            "terminal.font.size", SettingsCategory.TERMINAL, 13,
            Integer.class, "Terminal Font Size", "Terminal font size in points", false);

    // ═══════════════════════════════════════════════════════════════════════
    // ADVANCED SETTINGS
    // ═══════════════════════════════════════════════════════════════════════

    public static final SettingsKey<Boolean> DEBUG_MODE = new SettingsKey<>(
            "advanced.debug.mode", SettingsCategory.ADVANCED, false,
            Boolean.class, "Debug Mode", "Enable debug logging and features", false);

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & SINGLETON
    // ═══════════════════════════════════════════════════════════════════════

    private SettingsManager() {
        registerAllKeys();
        loadGlobalSettings();

        // Initialize I18n
        String lang = get(APPEARANCE_LANGUAGE);
        if ("Spanish".equalsIgnoreCase(lang)) {
            I18nManager.getInstance().setLocale(new Locale("es"));
        } else {
            I18nManager.getInstance().setLocale(Locale.ENGLISH);
        }
    }

    public static SettingsManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SettingsManager();
        }
        return INSTANCE;
    }

    private void registerAllKeys() {
        allKeys.add(THEME);
        allKeys.add(APPEARANCE_LANGUAGE);
        allKeys.add(ICON_SIZE);
        allKeys.add(EDITOR_FONT_FAMILY);
        allKeys.add(EDITOR_FONT_SIZE);
        allKeys.add(EDITOR_TAB_WIDTH);
        allKeys.add(EDITOR_SHOW_LINE_NUMBERS);
        allKeys.add(EDITOR_WORD_WRAP);
        allKeys.add(AUTO_SAVE);
        allKeys.add(AUTO_SAVE_INTERVAL);
        allKeys.add(CONFIRM_EXIT);
        allKeys.add(REOPEN_LAST_PROJECT);
        allKeys.add(TERMINAL_FONT_FAMILY);
        allKeys.add(TERMINAL_FONT_SIZE);
        allKeys.add(DEBUG_MODE);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FILE PATHS
    // ═══════════════════════════════════════════════════════════════════════

    private Path getGlobalSettingsPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, SETTINGS_DIR, SETTINGS_FILE);
    }

    private Path getProjectSettingsPath() {
        if (currentProjectPath == null)
            return null;
        return currentProjectPath.toPath().resolve(SETTINGS_DIR).resolve(SETTINGS_FILE);
    }

    public File getGlobalSettingsFile() {
        return getGlobalSettingsPath().toFile();
    }

    public File getProjectSettingsFile() {
        Path path = getProjectSettingsPath();
        return path != null ? path.toFile() : null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOADING & SAVING
    // ═══════════════════════════════════════════════════════════════════════

    private void loadGlobalSettings() {
        Path path = getGlobalSettingsPath();
        globalSettings = loadFromJson(path);

        // Create default file if it doesn't exist
        if (!Files.exists(path)) {
            saveGlobalSettings();
        }
    }

    public void loadProjectSettings(File projectPath) {
        this.currentProjectPath = projectPath;
        if (projectPath != null) {
            Path path = getProjectSettingsPath();
            projectSettings = loadFromJson(path);
        } else {
            projectSettings = new LinkedHashMap<>();
        }
    }

    private Map<String, Object> loadFromJson(Path path) {
        if (path == null || !Files.exists(path)) {
            return new LinkedHashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            java.lang.reflect.Type type = new TypeToken<LinkedHashMap<String, Object>>() {
            }.getType();
            Map<String, Object> loaded = GSON.fromJson(reader, type);
            return loaded != null ? loaded : new LinkedHashMap<>();
        } catch (Exception e) {
            System.err.println("Failed to load settings from " + path + ": " + e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    public void saveGlobalSettings() {
        saveToJson(getGlobalSettingsPath(), globalSettings);
    }

    public void saveProjectSettings() {
        Path path = getProjectSettingsPath();
        if (path != null) {
            saveToJson(path, projectSettings);
        }
    }

    private void saveToJson(Path path, Map<String, Object> settings) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(settings, writer);
            }
        } catch (Exception e) {
            System.err.println("Failed to save settings to " + path + ": " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GETTERS & SETTERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get a setting value with type safety.
     * Resolution order: Project -> Global -> Default
     */
    @SuppressWarnings("unchecked")
    public <T> T get(SettingsKey<T> key) {
        String keyStr = key.getKey();

        // Check project settings first (if overridable and project is set)
        if (key.isProjectOverridable() && currentProjectPath != null && projectSettings.containsKey(keyStr)) {
            return convertValue(projectSettings.get(keyStr), key.getType(), key.getDefaultValue());
        }

        // Check global settings
        if (globalSettings.containsKey(keyStr)) {
            return convertValue(globalSettings.get(keyStr), key.getType(), key.getDefaultValue());
        }

        // Return default
        return key.getDefaultValue();
    }

    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> type, T defaultValue) {
        if (value == null)
            return defaultValue;

        try {
            if (type == String.class) {
                return (T) value.toString();
            } else if (type == Integer.class) {
                if (value instanceof Number) {
                    return (T) Integer.valueOf(((Number) value).intValue());
                }
                return (T) Integer.valueOf(value.toString());
            } else if (type == Boolean.class) {
                if (value instanceof Boolean) {
                    return (T) value;
                }
                return (T) Boolean.valueOf(value.toString());
            } else if (type == Double.class) {
                if (value instanceof Number) {
                    return (T) Double.valueOf(((Number) value).doubleValue());
                }
                return (T) Double.valueOf(value.toString());
            } else if (type == Long.class) {
                if (value instanceof Number) {
                    return (T) Long.valueOf(((Number) value).longValue());
                }
                return (T) Long.valueOf(value.toString());
            }
        } catch (Exception e) {
            return defaultValue;
        }

        return defaultValue;
    }

    /**
     * Set a setting value in global settings and notify listeners.
     */
    public <T> void set(SettingsKey<T> key, T value) {
        setGlobal(key, value);
    }

    /**
     * Set a setting value in global settings.
     */
    public <T> void setGlobal(SettingsKey<T> key, T value) {
        T oldValue = get(key);
        globalSettings.put(key.getKey(), value);
        saveGlobalSettings();
        notifyListeners(key, oldValue, value);
    }

    /**
     * Set a setting value in project settings (if overridable).
     */
    public <T> void setProject(SettingsKey<T> key, T value) {
        if (!key.isProjectOverridable()) {
            throw new IllegalArgumentException("Setting " + key.getKey() + " cannot be overridden at project level");
        }
        if (currentProjectPath == null) {
            throw new IllegalStateException("No project is currently open");
        }

        T oldValue = get(key);
        projectSettings.put(key.getKey(), value);
        saveProjectSettings();
        notifyListeners(key, oldValue, value);
    }

    /**
     * Remove a project-level override, falling back to global setting.
     */
    public <T> void removeProjectOverride(SettingsKey<T> key) {
        if (projectSettings.containsKey(key.getKey())) {
            T oldValue = get(key);
            projectSettings.remove(key.getKey());
            saveProjectSettings();
            T newValue = get(key);
            notifyListeners(key, oldValue, newValue);
        }
    }

    /**
     * Check if a setting has a project-level override.
     */
    public boolean hasProjectOverride(SettingsKey<?> key) {
        return projectSettings.containsKey(key.getKey());
    }

    /**
     * Reset a setting to its default value.
     */
    public <T> void reset(SettingsKey<T> key) {
        set(key, key.getDefaultValue());
    }

    /**
     * Reset all settings to their default values.
     */
    public void resetAll() {
        for (SettingsKey<?> key : allKeys) {
            resetTyped(key);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void resetTyped(SettingsKey<T> key) {
        set(key, key.getDefaultValue());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PROJECT CONTEXT
    // ═══════════════════════════════════════════════════════════════════════

    public void setCurrentProject(File projectPath) {
        loadProjectSettings(projectPath);
    }

    public File getCurrentProject() {
        return currentProjectPath;
    }

    public boolean hasProject() {
        return currentProjectPath != null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LISTENERS
    // ═══════════════════════════════════════════════════════════════════════

    public <T> void addListener(SettingsKey<T> key, SettingsChangeListener<T> listener) {
        listeners.computeIfAbsent(key, k -> new ArrayList<>()).add(listener);
    }

    public <T> void removeListener(SettingsKey<T> key, SettingsChangeListener<T> listener) {
        List<SettingsChangeListener<?>> list = listeners.get(key);
        if (list != null) {
            list.remove(listener);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void notifyListeners(SettingsKey<T> key, T oldValue, T newValue) {
        List<SettingsChangeListener<?>> list = listeners.get(key);
        if (list != null) {
            for (SettingsChangeListener<?> listener : list) {
                ((SettingsChangeListener<T>) listener).onSettingChanged(key, oldValue, newValue);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // QUERY METHODS
    // ═══════════════════════════════════════════════════════════════════════

    public List<SettingsKey<?>> getAllKeys() {
        return Collections.unmodifiableList(allKeys);
    }

    public List<SettingsKey<?>> getKeysForCategory(SettingsCategory category) {
        List<SettingsKey<?>> result = new ArrayList<>();
        for (SettingsKey<?> key : allKeys) {
            if (key.getCategory() == category) {
                result.add(key);
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // THEME INTEGRATION (Legacy compatibility)
    // ═══════════════════════════════════════════════════════════════════════

    public ThemeManager.THEMES getTheme() {
        String themeName = get(THEME);
        try {
            return ThemeManager.THEMES.valueOf(themeName);
        } catch (IllegalArgumentException e) {
            return ThemeManager.THEMES.SYSTEM;
        }
    }

    public void setTheme(ThemeManager.THEMES theme) {
        set(THEME, theme.name());
    }
}
