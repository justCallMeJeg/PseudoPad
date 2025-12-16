package pseudopad.settings;

import java.util.Objects;

/**
 * Type-safe settings key definition with metadata.
 * Each key defines its category, default value, type, and display information.
 * 
 * @param <T> The type of the setting value
 * @author Geger John Paul Gabayeron
 */
public final class SettingsKey<T> {
    private final String key;
    private final SettingsCategory category;
    private final T defaultValue;
    private final Class<T> type;
    private final String displayName;
    private final String description;
    private final boolean projectOverridable;

    public SettingsKey(String key, SettingsCategory category, T defaultValue,
            Class<T> type, String displayName, String description, boolean projectOverridable) {
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.defaultValue = defaultValue;
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "Display name cannot be null");
        this.description = description != null ? description : "";
        this.projectOverridable = projectOverridable;
    }

    public SettingsKey(String key, SettingsCategory category, T defaultValue,
            Class<T> type, String displayName, String description) {
        this(key, category, defaultValue, type, displayName, description, false);
    }

    public SettingsKey(String key, SettingsCategory category, T defaultValue,
            Class<T> type, String displayName) {
        this(key, category, defaultValue, type, displayName, "", false);
    }

    public String getKey() {
        return key;
    }

    public SettingsCategory getCategory() {
        return category;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public Class<T> getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isProjectOverridable() {
        return projectOverridable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SettingsKey<?> that = (SettingsKey<?>) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "SettingsKey{" + key + ", type=" + type.getSimpleName() +
                ", projectOverridable=" + projectOverridable + "}";
    }
}
