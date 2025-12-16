package pseudopad.settings;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;

import pseudopad.app.MainFrame;
import pseudopad.utils.ThemeManager;

/**
 * Central settings manager providing type-safe access to all application
 * settings.
 * Uses Java Preferences API for persistence and supports change listeners.
 * 
 * @author Geger John Paul Gabayeron
 */
public class SettingsManager {
    private static SettingsManager INSTANCE;
    private final Preferences prefs;

    // Listeners map: key -> list of listeners
    private final Map<SettingsKey<?>, List<SettingsChangeListener<?>>> listeners = new ConcurrentHashMap<>();

    // Registry of all known settings keys
    private final List<SettingsKey<?>> allKeys = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════════════
    // APPEARANCE SETTINGS
    // ═══════════════════════════════════════════════════════════════════════

    public static final SettingsKey<String> THEME = new SettingsKey<>(
            "appearance.theme", SettingsCategory.APPEARANCE, "SYSTEM",
            String.class, "Theme", "Application color theme (LIGHT, DARK, SYSTEM)");

    public static final SettingsKey<Integer> ICON_SIZE = new SettingsKey<>(
            "appearance.icon.size", SettingsCategory.APPEARANCE, 16,
            Integer.class, "Icon Size", "Default icon size in pixels");

    // ═══════════════════════════════════════════════════════════════════════
    // EDITOR SETTINGS
    // ═══════════════════════════════════════════════════════════════════════

    public static final SettingsKey<String> EDITOR_FONT_FAMILY = new SettingsKey<>(
            "editor.font.family", SettingsCategory.EDITOR, "Consolas",
            String.class, "Font Family", "Font used in the code editor");

    public static final SettingsKey<Integer> EDITOR_FONT_SIZE = new SettingsKey<>(
            "editor.font.size", SettingsCategory.EDITOR, 14,
            Integer.class, "Font Size", "Font size in points");

    public static final SettingsKey<Integer> EDITOR_TAB_WIDTH = new SettingsKey<>(
            "editor.tab.width", SettingsCategory.EDITOR, 4,
            Integer.class, "Tab Width", "Number of spaces per tab");

    public static final SettingsKey<Boolean> EDITOR_SHOW_LINE_NUMBERS = new SettingsKey<>(
            "editor.show.line.numbers", SettingsCategory.EDITOR, true,
            Boolean.class, "Show Line Numbers", "Display line numbers in the gutter");

    public static final SettingsKey<Boolean> EDITOR_WORD_WRAP = new SettingsKey<>(
            "editor.word.wrap", SettingsCategory.EDITOR, false,
            Boolean.class, "Word Wrap", "Wrap long lines to fit the editor width");

    // ═══════════════════════════════════════════════════════════════════════
    // BEHAVIOR SETTINGS
    // ═══════════════════════════════════════════════════════════════════════

    public static final SettingsKey<Boolean> AUTO_SAVE = new SettingsKey<>(
            "behavior.auto.save", SettingsCategory.BEHAVIOR, false,
            Boolean.class, "Auto-Save", "Automatically save files");

    public static final SettingsKey<Integer> AUTO_SAVE_INTERVAL = new SettingsKey<>(
            "behavior.auto.save.interval", SettingsCategory.BEHAVIOR, 30,
            Integer.class, "Auto-Save Interval", "Seconds between auto-saves");

    public static final SettingsKey<Boolean> CONFIRM_EXIT = new SettingsKey<>(
            "behavior.confirm.exit", SettingsCategory.BEHAVIOR, true,
            Boolean.class, "Confirm Exit", "Show confirmation dialog before exiting");

    public static final SettingsKey<Boolean> REOPEN_LAST_PROJECT = new SettingsKey<>(
            "behavior.reopen.last.project", SettingsCategory.BEHAVIOR, true,
            Boolean.class, "Reopen Last Project", "Automatically open the last project on startup");

    // ═══════════════════════════════════════════════════════════════════════
    // TERMINAL SETTINGS
    // ═══════════════════════════════════════════════════════════════════════

    public static final SettingsKey<String> TERMINAL_FONT_FAMILY = new SettingsKey<>(
            "terminal.font.family", SettingsCategory.TERMINAL, "Consolas",
            String.class, "Terminal Font", "Font used in the terminal");

    public static final SettingsKey<Integer> TERMINAL_FONT_SIZE = new SettingsKey<>(
            "terminal.font.size", SettingsCategory.TERMINAL, 13,
            Integer.class, "Terminal Font Size", "Terminal font size in points");

    // ═══════════════════════════════════════════════════════════════════════
    // ADVANCED SETTINGS
    // ═══════════════════════════════════════════════════════════════════════

    public static final SettingsKey<Boolean> DEBUG_MODE = new SettingsKey<>(
            "advanced.debug.mode", SettingsCategory.ADVANCED, false,
            Boolean.class, "Debug Mode", "Enable debug logging and features");

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & SINGLETON
    // ═══════════════════════════════════════════════════════════════════════

    private SettingsManager() {
        this.prefs = Preferences.userNodeForPackage(MainFrame.class);
        registerAllKeys();
    }

    public static SettingsManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SettingsManager();
        }
        return INSTANCE;
    }

    private void registerAllKeys() {
        // Register all keys for enumeration
        allKeys.add(THEME);
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
    // GETTERS & SETTERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get a setting value with type safety.
     * 
     * @param <T> The setting value type
     * @param key The settings key
     * @return The current value, or the default if not set
     */
    @SuppressWarnings("unchecked")
    public <T> T get(SettingsKey<T> key) {
        Class<T> type = key.getType();

        if (type == String.class) {
            return (T) prefs.get(key.getKey(), (String) key.getDefaultValue());
        } else if (type == Integer.class) {
            Integer defaultVal = (Integer) key.getDefaultValue();
            return (T) Integer.valueOf(prefs.getInt(key.getKey(), defaultVal != null ? defaultVal : 0));
        } else if (type == Boolean.class) {
            Boolean defaultVal = (Boolean) key.getDefaultValue();
            return (T) Boolean.valueOf(prefs.getBoolean(key.getKey(), defaultVal != null ? defaultVal : false));
        } else if (type == Double.class) {
            Double defaultVal = (Double) key.getDefaultValue();
            return (T) Double.valueOf(prefs.getDouble(key.getKey(), defaultVal != null ? defaultVal : 0.0));
        } else if (type == Long.class) {
            Long defaultVal = (Long) key.getDefaultValue();
            return (T) Long.valueOf(prefs.getLong(key.getKey(), defaultVal != null ? defaultVal : 0L));
        }

        // Fallback for unknown types: try to get as string
        String stored = prefs.get(key.getKey(), null);
        if (stored == null) {
            return key.getDefaultValue();
        }
        return (T) stored;
    }

    /**
     * Set a setting value and notify listeners.
     * 
     * @param <T>   The setting value type
     * @param key   The settings key
     * @param value The new value
     */
    public <T> void set(SettingsKey<T> key, T value) {
        T oldValue = get(key);

        // Store the value
        Class<T> type = key.getType();
        if (value == null) {
            prefs.remove(key.getKey());
        } else if (type == String.class) {
            prefs.put(key.getKey(), (String) value);
        } else if (type == Integer.class) {
            prefs.putInt(key.getKey(), (Integer) value);
        } else if (type == Boolean.class) {
            prefs.putBoolean(key.getKey(), (Boolean) value);
        } else if (type == Double.class) {
            prefs.putDouble(key.getKey(), (Double) value);
        } else if (type == Long.class) {
            prefs.putLong(key.getKey(), (Long) value);
        } else {
            prefs.put(key.getKey(), value.toString());
        }

        // Notify listeners
        notifyListeners(key, oldValue, value);
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
    // LISTENERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Add a listener for changes to a specific setting.
     */
    public <T> void addListener(SettingsKey<T> key, SettingsChangeListener<T> listener) {
        listeners.computeIfAbsent(key, k -> new ArrayList<>()).add(listener);
    }

    /**
     * Remove a listener for a specific setting.
     */
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

    /**
     * Get all registered settings keys.
     */
    public List<SettingsKey<?>> getAllKeys() {
        return Collections.unmodifiableList(allKeys);
    }

    /**
     * Get all settings keys for a specific category.
     */
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

    /**
     * Get the current theme setting as a ThemeManager.THEMES enum.
     */
    public ThemeManager.THEMES getTheme() {
        String themeName = get(THEME);
        try {
            return ThemeManager.THEMES.valueOf(themeName);
        } catch (IllegalArgumentException e) {
            return ThemeManager.THEMES.SYSTEM;
        }
    }

    /**
     * Set the theme setting from a ThemeManager.THEMES enum.
     */
    public void setTheme(ThemeManager.THEMES theme) {
        set(THEME, theme.name());
    }
}
