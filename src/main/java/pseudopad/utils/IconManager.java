package pseudopad.utils;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.UIManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class IconManager {
    private static final Map<String, FlatSVGIcon> iconCache = new HashMap<>();
    private static final String BASE_PATH = "icons/svg/"; 
    
    // Theme Listener
    static {
        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName())) {
                // Clear cache so icons are re-loaded with new theme colors
                iconCache.clear();
            }
        });
    }

    public static Icon get(String name) {
        // Sanitation
        String cleanName = name;
        if (cleanName.endsWith(".svg")) {
            cleanName = cleanName.substring(0, cleanName.length() - 4);
        }

        // Cache Check
        if (iconCache.containsKey(cleanName)) {
            return iconCache.get(cleanName);
        }

        String fullPath = BASE_PATH + cleanName + ".svg";

        try {
            URL url = IconManager.class.getClassLoader().getResource(fullPath);

            if (url != null) {
                FlatSVGIcon icon = new FlatSVGIcon(fullPath, 16, 16);
                
                // Color Filter for dynamic icon change based on current UI theme
                icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> {
                    // If the SVG part is RED...
                    if (color.equals(Color.RED)) {
                        // ...Ask UIManager for the CURRENT color dynamically
                        return UIManager.getColor("Label.foreground");
                    }
                    return color;
                }));

                iconCache.put(cleanName, icon);
                return icon;
            } else {
                System.err.println("Icon not found: " + fullPath);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Preload function during SplashScreen to avoid lag on startup icons
    public static void preloadCoreIcons() {
        get("open_project");
    }
}
