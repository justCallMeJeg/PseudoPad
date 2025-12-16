package pseudopad.settings;

/**
 * Functional interface for listening to settings changes.
 * 
 * @param <T> The type of the setting value
 * @author Geger John Paul Gabayeron
 */
@FunctionalInterface
public interface SettingsChangeListener<T> {
    /**
     * Called when a setting value changes.
     * 
     * @param key      The settings key that changed
     * @param oldValue The previous value (may be null)
     * @param newValue The new value
     */
    void onSettingChanged(SettingsKey<T> key, T oldValue, T newValue);
}
