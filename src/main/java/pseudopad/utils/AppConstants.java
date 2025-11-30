package pseudopad.utils;

import java.awt.Dimension;

/**
 * Application-wide constants.
 * 
 * @author Geger John Paul Gabayeron
 */
public class AppConstants {

    // App Info
    public static final String APP_TITLE = "PseudoPad v0";
    public static final String APP_VERSION = "0.0-DEV";

    // Window Settings
    public static final Dimension MIN_WINDOW_SIZE = new Dimension(900, 506);
    public static final double MAIN_SPLIT_RESIZE_WEIGHT = 0.0;
    public static final double NAV_SPLIT_RESIZE_WEIGHT = 0.5;
    public static final double EDITOR_SPLIT_RESIZE_WEIGHT = 1.0;

    // Resources
    public static final String ICON_PATH_LIGHT = "/img/icon.png";
    public static final String ICON_PATH_DARK = "/img/icon_dark.png";

    // Editor
    public static final String DEFAULT_TAB_TITLE = "Untitled";
    public static final String FILE_EXTENSION = ".pc";

    // Dialogs
    public static final String DIALOG_TITLE_NEW_PROJECT = "New Project";
    public static final String DIALOG_TITLE_OPEN_PROJECT = "Open Project Folder";

    private AppConstants() {
        // Prevent instantiation
    }
}
