package pseudopad.utils;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.UIManager;

import org.kordamp.ikonli.materialdesign2.*;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Icon manager using Ikonli MaterialDesign2 icons for consistent UI.
 * Icons are color-coded by function category.
 * Falls back to FlatSVGIcon for custom PseudoPad branding icons.
 * 
 * @author Geger John Paul Gabayeron
 */
public class IconManager {
    private static final Map<String, Icon> iconCache = new HashMap<>();

    private static final String BASE_PATH = "icons/svg/";
    private static final int DEFAULT_SIZE = 16;

    // ═══════════════════════════════════════════════════════════════════════
    // COLOR DEFINITIONS (based on function categories)
    // ═══════════════════════════════════════════════════════════════════════

    /** Green - Save, create, add operations */
    private static final Color COLOR_SUCCESS = new Color(62, 204, 67); // #3ECC43

    /** Orange - Undo, redo, history operations */
    private static final Color COLOR_HISTORY = new Color(249, 160, 63); // #F9A03F

    /** Red - Delete, remove, destructive operations */
    private static final Color COLOR_DANGER = new Color(231, 76, 60); // #E74C3C

    /** Blue - Edit, clipboard operations */
    private static final Color COLOR_EDIT = new Color(52, 152, 219); // #3498DB

    /** Cyan/Teal - Run, execute, play operations */
    private static final Color COLOR_RUN = new Color(26, 188, 156); // #1ABC9C

    /** Yellow/Amber - Warning, alert icons */
    private static final Color COLOR_WARNING = new Color(241, 196, 15); // #F1C40F

    /** Purple - Search, find operations */
    private static final Color COLOR_SEARCH = new Color(155, 89, 182); // #9B59B6

    /** Default - Uses theme foreground color (null signals to use theme color) */
    private static final Color COLOR_DEFAULT = null;

    // ═══════════════════════════════════════════════════════════════════════
    // ICON MAPPINGS
    // ═══════════════════════════════════════════════════════════════════════

    private static final Map<String, Object> ICON_MAP = new HashMap<>();
    private static final Map<String, Color> ICON_COLORS = new HashMap<>();

    static {
        // ─────────────────────────────────────────────────────────────────
        // FILE OPERATIONS (Green for create/save)
        // ─────────────────────────────────────────────────────────────────
        registerIcon("new_project", MaterialDesignF.FOLDER_PLUS, COLOR_SUCCESS);
        registerIcon("open_project", MaterialDesignF.FOLDER_OPEN, COLOR_DEFAULT);
        registerIcon("new_file", MaterialDesignF.FILE_PLUS, COLOR_SUCCESS);
        registerIcon("save", MaterialDesignC.CONTENT_SAVE, COLOR_SUCCESS);

        // ─────────────────────────────────────────────────────────────────
        // HISTORY OPERATIONS (Orange for undo/redo)
        // ─────────────────────────────────────────────────────────────────
        registerIcon("undo", MaterialDesignU.UNDO, COLOR_HISTORY);
        registerIcon("redo", MaterialDesignR.REDO, COLOR_HISTORY);

        // ─────────────────────────────────────────────────────────────────
        // CLIPBOARD OPERATIONS (Blue for edit)
        // ─────────────────────────────────────────────────────────────────
        registerIcon("content_cut", MaterialDesignC.CONTENT_CUT, COLOR_EDIT);
        registerIcon("content_copy", MaterialDesignC.CONTENT_COPY, COLOR_EDIT);
        registerIcon("content_paste", MaterialDesignC.CONTENT_PASTE, COLOR_EDIT);

        // ─────────────────────────────────────────────────────────────────
        // DESTRUCTIVE OPERATIONS (Red for delete)
        // ─────────────────────────────────────────────────────────────────
        registerIcon("delete", MaterialDesignD.DELETE, COLOR_DANGER);

        // ─────────────────────────────────────────────────────────────────
        // NAVIGATION (Default theme color)
        // ─────────────────────────────────────────────────────────────────
        registerIcon("folder", MaterialDesignF.FOLDER, COLOR_DEFAULT);
        registerIcon("folder_open", MaterialDesignF.FOLDER_OPEN, COLOR_DEFAULT);
        registerIcon("file", MaterialDesignF.FILE, COLOR_DEFAULT);
        registerIcon("file_tree", MaterialDesignF.FILE_TREE, COLOR_DEFAULT);
        registerIcon("files", MaterialDesignF.FILE_MULTIPLE, COLOR_DEFAULT); // Files panel icon

        // ─────────────────────────────────────────────────────────────────
        // EXECUTION (Cyan/Teal for run)
        // ─────────────────────────────────────────────────────────────────
        registerIcon("terminal", MaterialDesignC.CONSOLE, COLOR_DEFAULT);
        registerIcon("run", MaterialDesignP.PLAY, COLOR_RUN);

        // ─────────────────────────────────────────────────────────────────
        // SEARCH (Purple)
        // ─────────────────────────────────────────────────────────────────
        registerIcon("search", MaterialDesignM.MAGNIFY, COLOR_SEARCH);
        registerIcon("find_replace", MaterialDesignF.FIND_REPLACE, COLOR_SEARCH);

        // ─────────────────────────────────────────────────────────────────
        // WARNINGS & ALERTS (Yellow/Amber)
        // ─────────────────────────────────────────────────────────────────
        registerIcon("warning", MaterialDesignA.ALERT, COLOR_WARNING);

        // ─────────────────────────────────────────────────────────────────
        // MISC (Default theme color)
        // ─────────────────────────────────────────────────────────────────
        registerIcon("comment", MaterialDesignC.COMMENT_TEXT, COLOR_DEFAULT);
        registerIcon("bookmark", MaterialDesignB.BOOKMARK, COLOR_DEFAULT);
        registerIcon("bookmark_add", MaterialDesignB.BOOKMARK_PLUS, COLOR_SUCCESS);
        registerIcon("log", MaterialDesignT.TEXT_BOX_OUTLINE, COLOR_DEFAULT);
        registerIcon("outline", MaterialDesignF.FORMAT_LIST_BULLETED, COLOR_DEFAULT);
        registerIcon("sidebar", MaterialDesignP.PAGE_LAYOUT_SIDEBAR_LEFT, COLOR_DEFAULT);
        registerIcon("minimize", MaterialDesignC.CHEVRON_DOWN, COLOR_DEFAULT);
        registerIcon("settings", MaterialDesignC.COG, COLOR_DEFAULT);

        // Theme Listener to clear cache on theme change so icons get new colors
        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName())) {
                iconCache.clear();
            }
        });
    }

    private static void registerIcon(String name, Object iconCode, Color color) {
        ICON_MAP.put(name, iconCode);
        if (color != null) {
            ICON_COLORS.put(name, color);
        }
    }

    // Custom PseudoPad icons that use SVG files (includes "project" for branding)
    private static final Set<String> CUSTOM_ICONS = Set.of(
            "file_pseudocode", "file_pseudo_code",
            "folder_settings", "folder_settings_open",
            "project" // Custom project icon with PseudoPad logo
    );

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
        Icon icon = loadIcon(cleanName, size);
        if (icon != null) {
            iconCache.put(cacheKey, icon);
        }

        return icon;
    }

    private static String sanitizeName(String name) {
        return name.endsWith(".svg") ? name.substring(0, name.length() - 4) : name;
    }

    private static Icon loadIcon(String name, int size) {
        // Check if this is a custom PseudoPad icon
        if (CUSTOM_ICONS.contains(name)) {
            return loadSvgIcon(name, size);
        }

        // Use Ikonli MaterialDesign2 icon
        Object iconCode = ICON_MAP.get(name);
        if (iconCode != null) {
            Color color = ICON_COLORS.get(name);
            return createFontIcon(iconCode, size, color);
        }

        // Fallback: try loading as SVG
        return loadSvgIcon(name, size);
    }

    private static Icon createFontIcon(Object iconCode, int size, Color customColor) {
        try {
            FontIcon icon = FontIcon.of((org.kordamp.ikonli.Ikon) iconCode, size);

            // Apply color: custom if specified, otherwise theme-aware
            if (customColor != null) {
                icon.setIconColor(customColor);
            } else {
                // Use current theme's foreground color
                Color foreground = UIManager.getColor("Label.foreground");
                if (foreground != null) {
                    icon.setIconColor(foreground);
                }
            }

            return icon;
        } catch (Exception e) {
            System.err.println("Failed to create FontIcon: " + e.getMessage());
            return null;
        }
    }

    private static FlatSVGIcon loadSvgIcon(String name, int size) {
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
        get("save");
        get("run");
        // Add other critical icons here
    }
}
