package pseudopad.settings;

/**
 * Categories for organizing application settings.
 * Each category groups related settings together for the UI.
 * 
 * @author Geger John Paul Gabayeron
 */
public enum SettingsCategory {
    APPEARANCE("Appearance", "Visual appearance settings like theme and icons"),
    EDITOR("Editor", "Text editor settings like font and indentation"),
    BEHAVIOR("Behavior", "Application behavior settings like auto-save"),
    TERMINAL("Terminal", "Terminal emulator settings"),
    ADVANCED("Advanced", "Advanced settings for power users");

    private final String displayName;
    private final String description;

    SettingsCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
