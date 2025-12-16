package pseudopad.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

import pseudopad.app.WindowManager;

/**
 * A vertical bar on the left edge that shows icon buttons for hidden navigation
 * panels.
 * Only visible when there are hidden panels.
 * Buttons are ordered consistently based on panel order (same as tabs).
 * 
 * @author Geger John Paul Gabayeron
 */
public class ActivityBar extends JPanel {
    private final Map<String, JButton> panelButtons = new HashMap<>();
    private final JPanel buttonContainer;

    public ActivityBar() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));

        buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
        buttonContainer.setOpaque(false);

        add(buttonContainer);
        add(Box.createVerticalGlue());

        // Start hidden - will show when panels are hidden
        setVisible(false);
    }

    /**
     * Adds a restore button for a hidden panel at the correct position.
     * Buttons are ordered based on WindowManager.getDefaultPanelOrder().
     * 
     * @param panelId Unique identifier for the panel
     * @param icon    Icon to display on the button
     * @param tooltip Tooltip text (panel name + shortcut)
     * @param onClick Action to perform when clicked
     */
    public void showRestoreButton(String panelId, Icon icon, String tooltip, ActionListener onClick) {
        if (panelButtons.containsKey(panelId)) {
            return; // Already showing
        }

        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.addActionListener(onClick);
        button.putClientProperty("panelId", panelId); // Store panelId for ordering
        styleButton(button);

        panelButtons.put(panelId, button);

        // Find correct insertion position based on panel order
        int insertIndex = findInsertionIndex(panelId);

        // Each button is followed by a spacing strut, so multiply by 2
        int componentIndex = insertIndex * 2;

        buttonContainer.add(button, componentIndex);
        buttonContainer.add(Box.createVerticalStrut(4), componentIndex + 1);

        updateVisibility();
        buttonContainer.revalidate();
        buttonContainer.repaint();
    }

    /**
     * Finds the correct insertion index for a new button based on panel order.
     * Uses global ordering: Projects > Files > Outline > Output > Problems > Logs
     */
    private int findInsertionIndex(String newPanelId) {
        int targetOrder = WindowManager.getDefaultPanelOrder(newPanelId);

        int insertIndex = 0;

        for (String existingPanelId : panelButtons.keySet()) {
            int existingOrder = WindowManager.getDefaultPanelOrder(existingPanelId);
            if (targetOrder > existingOrder) {
                insertIndex++;
            }
        }

        return insertIndex;
    }

    /**
     * Removes the restore button for a panel (when it becomes visible again).
     * 
     * @param panelId Unique identifier for the panel
     */
    public void hideRestoreButton(String panelId) {
        JButton button = panelButtons.remove(panelId);
        if (button != null) {
            // Remove button and its spacing strut
            int idx = -1;
            for (int i = 0; i < buttonContainer.getComponentCount(); i++) {
                if (buttonContainer.getComponent(i) == button) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                buttonContainer.remove(idx);
                // Remove the strut after it (if exists)
                if (idx < buttonContainer.getComponentCount()) {
                    Component next = buttonContainer.getComponent(idx);
                    if (next instanceof Box.Filler) {
                        buttonContainer.remove(idx);
                    }
                }
            }

            updateVisibility();
            buttonContainer.revalidate();
            buttonContainer.repaint();
        }
    }

    /**
     * Returns true if any restore buttons are currently visible.
     */
    public boolean hasVisibleButtons() {
        return !panelButtons.isEmpty();
    }

    /**
     * Updates visibility based on whether there are buttons to show.
     */
    private void updateVisibility() {
        setVisible(hasVisibleButtons());
    }

    private void styleButton(JButton button) {
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.setFocusable(false);
        button.setMargin(new Insets(4, 4, 4, 4));
        button.setPreferredSize(new Dimension(28, 28));
        button.setMaximumSize(new Dimension(28, 28));
        button.setMinimumSize(new Dimension(28, 28));
    }
}
