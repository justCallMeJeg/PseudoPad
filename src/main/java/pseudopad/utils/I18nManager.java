package pseudopad.utils;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

/**
 * Manages application internationalization (i18n).
 * Loads and provides access to localized strings from ResourceBundles.
 * 
 * @author Geger John Paul Gabayeron
 */
public class I18nManager {
    private static I18nManager INSTANCE;
    private ResourceBundle bundle;
    private Locale currentLocale;

    private I18nManager() {
        // Default to English
        setLocale(Locale.ENGLISH);
    }

    public static synchronized I18nManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new I18nManager();
        }
        return INSTANCE;
    }

    /**
     * Sets the current locale and loads the corresponding resource bundle.
     * 
     * @param locale The locale to switch to.
     */
    public void setLocale(Locale locale) {
        this.currentLocale = locale;
        try {
            this.bundle = ResourceBundle.getBundle("messages", locale);
        } catch (MissingResourceException e) {
            System.err.println("Could not find resource bundle for locale: " + locale);
            // Fallback to minimal empty bundle or default
            this.bundle = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        }
    }

    public Locale getLocale() {
        return currentLocale;
    }

    /**
     * Retrieves a localized string for the given key.
     * 
     * @param key The property key.
     * @return The localized string, or the key itself if not found.
     */
    public static String get(String key) {
        return getInstance().getString(key);
    }

    public String getString(String key) {
        if (bundle == null)
            return key;
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}
