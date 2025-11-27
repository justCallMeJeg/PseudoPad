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
    // Cache keys are stored as "name@size" to allow caching different sizes of the same icon
    private static final Map<String, FlatSVGIcon> iconCache = new HashMap<>();
    
    private static final String BASE_PATH = "icons/svg/"; 
    private static final int DEFAULT_SIZE = 16;

    // Theme Listener to clear cache on theme change
    static {
        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName())) {
                iconCache.clear();
            }
        });
    }

    public static Icon get(String name) {
        return get(name, DEFAULT_SIZE);
    }
    
    public static Icon get(String name, int size) {
        String cleanName = sanitizeName(name);
        String cacheKey = cleanName + "@" + size;

        // 1. Check Cache
        if (iconCache.containsKey(cacheKey)) {
            return iconCache.get(cacheKey);
        }

        // 2. Load and Cache
        FlatSVGIcon icon = loadIcon(cleanName, size);
        if (icon != null) {
            iconCache.put(cacheKey, icon);
        }
        
        return icon;
    }
    
    private static String sanitizeName(String name) {
        return name.endsWith(".svg") ? name.substring(0, name.length() - 4) : name;
    }
    
    private static FlatSVGIcon loadIcon(String name, int size) {
        String fullPath = BASE_PATH + name + ".svg";
        
        try {
            // Check existence first
            URL url = IconManager.class.getClassLoader().getResource(fullPath);
            if (url == null) {
                System.err.println("Icon not found: " + fullPath);
                return null;
            }

            // Create Icon
            FlatSVGIcon icon = new FlatSVGIcon(fullPath, size, size);

            // Apply Theme Color Filter (Red -> Theme Label Color)
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> {
                if (color.equals(Color.RED)) {
                    return UIManager.getColor("Label.foreground");
                }
                return color;
            }));
            
            return icon;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void preloadCoreIcons() {
        get("open_project");
        // Add other critical icons here
    }
}
