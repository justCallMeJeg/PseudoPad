package pseudopad.app;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Singleton that manages the visibility state of all panels in the application.
 * Coordinates between MainLayout, ActivityBar, StatusBar, and Menu items.
 * 
 * @author Geger John Paul Gabayeron
 */
public class WindowManager {
    private static WindowManager instance;

    // Panel identifiers - Navigation
    public static final String PANEL_PROJECTS = "projects";
    public static final String PANEL_FILES = "files";
    public static final String PANEL_FILE_OUTLINE = "file_outline";

    // Panel identifiers - Output
    public static final String PANEL_OUTPUT = "output";
    public static final String PANEL_PROBLEMS = "problems";
    public static final String PANEL_LOGS = "logs";

    // Display names for panels
    private static final Map<String, String> PANEL_NAMES = Map.of(
            PANEL_PROJECTS, "Projects",
            PANEL_FILES, "Files",
            PANEL_FILE_OUTLINE, "File Outline",
            PANEL_OUTPUT, "Output",
            PANEL_PROBLEMS, "Problems",
            PANEL_LOGS, "Logs");

    // Panel visibility states (true = visible)
    private final Map<String, Boolean> panelVisibility = new HashMap<>();

    // Listeners for visibility changes
    private final List<BiConsumer<String, Boolean>> listeners = new ArrayList<>();

    private WindowManager() {
        // Initialize all panels as visible by default
        panelVisibility.put(PANEL_PROJECTS, true);
        panelVisibility.put(PANEL_FILES, true);
        panelVisibility.put(PANEL_FILE_OUTLINE, true);
        panelVisibility.put(PANEL_OUTPUT, true);
        panelVisibility.put(PANEL_PROBLEMS, true);
        panelVisibility.put(PANEL_LOGS, true);
    }

    public static synchronized WindowManager getInstance() {
        if (instance == null) {
            instance = new WindowManager();
        }
        return instance;
    }

    /**
     * Shows a panel and notifies listeners.
     */
    public void showPanel(String panelId) {
        if (!panelVisibility.getOrDefault(panelId, true)) {
            panelVisibility.put(panelId, true);
            notifyListeners(panelId, true);
        }
    }

    /**
     * Hides a panel and notifies listeners.
     */
    public void hidePanel(String panelId) {
        if (panelVisibility.getOrDefault(panelId, true)) {
            panelVisibility.put(panelId, false);
            notifyListeners(panelId, false);
        }
    }

    /**
     * Toggles panel visibility.
     */
    public void togglePanel(String panelId) {
        if (isPanelVisible(panelId)) {
            hidePanel(panelId);
        } else {
            showPanel(panelId);
        }
    }

    /**
     * Returns true if the panel is currently visible.
     */
    public boolean isPanelVisible(String panelId) {
        return panelVisibility.getOrDefault(panelId, true);
    }

    /**
     * Gets the display name for a panel.
     */
    public static String getPanelName(String panelId) {
        return PANEL_NAMES.getOrDefault(panelId, panelId);
    }

    /**
     * Resets all windows to their default (visible) state.
     */
    public void resetWindows() {
        for (String panelId : panelVisibility.keySet()) {
            if (!panelVisibility.get(panelId)) {
                panelVisibility.put(panelId, true);
                notifyListeners(panelId, true);
            }
        }
    }

    /**
     * Checks if the panel is a navigation panel.
     */
    public static boolean isNavigationPanel(String panelId) {
        return PANEL_PROJECTS.equals(panelId) ||
                PANEL_FILES.equals(panelId) ||
                PANEL_FILE_OUTLINE.equals(panelId);
    }

    /**
     * Checks if the panel is an output panel.
     */
    public static boolean isOutputPanel(String panelId) {
        return PANEL_OUTPUT.equals(panelId) ||
                PANEL_PROBLEMS.equals(panelId) ||
                PANEL_LOGS.equals(panelId);
    }

    /**
     * Adds a listener that will be notified when panel visibility changes.
     * 
     * @param listener BiConsumer receiving (panelId, isVisible)
     */
    public void addPanelChangeListener(BiConsumer<String, Boolean> listener) {
        listeners.add(listener);
    }

    /**
     * Removes a previously added listener.
     */
    public void removePanelChangeListener(BiConsumer<String, Boolean> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(String panelId, boolean isVisible) {
        for (BiConsumer<String, Boolean> listener : listeners) {
            listener.accept(panelId, isVisible);
        }
    }

    /**
     * Returns all navigation panel IDs.
     */
    public static String[] getNavigationPanelIds() {
        return new String[] { PANEL_PROJECTS, PANEL_FILES, PANEL_FILE_OUTLINE };
    }

    /**
     * Returns all output panel IDs.
     */
    public static String[] getOutputPanelIds() {
        return new String[] { PANEL_OUTPUT, PANEL_PROBLEMS, PANEL_LOGS };
    }

    /**
     * Returns the default order index for a panel within its group.
     * Lower index = appears first (leftmost).
     */
    public static int getDefaultPanelOrder(String panelId) {
        return switch (panelId) {
            case PANEL_PROJECTS -> 0;
            case PANEL_FILES -> 1;
            case PANEL_FILE_OUTLINE -> 0; // Only one in bottom nav
            case PANEL_OUTPUT -> 0;
            case PANEL_PROBLEMS -> 1;
            case PANEL_LOGS -> 2;
            default -> 99;
        };
    }
}
