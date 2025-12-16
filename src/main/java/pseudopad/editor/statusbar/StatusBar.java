package pseudopad.editor.statusbar;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * A dynamic status bar that sits at the bottom of the application.
 * It supports a main status message, left-side restore buttons, and right-side
 * widgets.
 * 
 * @author Geger John Paul Gabayeron
 */
public class StatusBar extends JPanel {
    private final JPanel leftWidgetPanel; // For restore buttons
    private final JLabel statusLabel;
    private final JPanel rightPanel;

    // Track restore buttons by panel ID
    private final Map<String, JButton> restoreButtons = new HashMap<>();

    public StatusBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)));

        // Left widget panel for restore buttons
        leftWidgetPanel = new JPanel();
        leftWidgetPanel.setLayout(new BoxLayout(leftWidgetPanel, BoxLayout.X_AXIS));
        leftWidgetPanel.setOpaque(false);

        // Initialize components
        statusLabel = new JLabel("Ready");
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setOpaque(false);

        // Center panel to hold left widgets + status label
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(leftWidgetPanel, BorderLayout.WEST);
        centerPanel.add(statusLabel, BorderLayout.CENTER);

        // Add components to layout
        add(centerPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Set preferred height (optional, but good for consistency)
        setPreferredSize(new Dimension(getWidth(), 24));
    }

    /**
     * Sets the main status message.
     * 
     * @param message The message to display.
     */
    public void setMessage(String message) {
        statusLabel.setText(message);
    }

    /**
     * Adds a component to the right side of the status bar.
     * 
     * @param component The component to add.
     */
    public void addRightComponent(JComponent component) {
        rightPanel.add(Box.createHorizontalStrut(10)); // Add spacing
        rightPanel.add(component);
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    /**
     * Adds a vertical separator to the right side.
     */
    public void addSeparator() {
        rightPanel.add(Box.createHorizontalStrut(5));
        rightPanel.add(new JSeparator(SwingConstants.VERTICAL));
        rightPanel.add(Box.createHorizontalStrut(5));
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    /**
     * Clears all components from the right side.
     */
    public void clearRightComponents() {
        rightPanel.removeAll();
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    /**
     * Shows a restore button for a hidden output panel.
     * 
     * @param panelId Unique identifier for the panel
     * @param text    Button text (panel name)
     * @param tooltip Tooltip text (panel name + shortcut)
     * @param onClick Action to perform when clicked
     */
    public void showRestoreButton(String panelId, String text, String tooltip,
            java.awt.event.ActionListener onClick) {
        if (restoreButtons.containsKey(panelId)) {
            return; // Already showing
        }

        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.addActionListener(onClick);
        styleRestoreButton(button);

        restoreButtons.put(panelId, button);
        leftWidgetPanel.add(button);
        leftWidgetPanel.add(Box.createHorizontalStrut(4));

        leftWidgetPanel.revalidate();
        leftWidgetPanel.repaint();
    }

    /**
     * Hides the restore button for a panel (when it becomes visible again).
     * 
     * @param panelId Unique identifier for the panel
     */
    public void hideRestoreButton(String panelId) {
        JButton button = restoreButtons.remove(panelId);
        if (button != null) {
            // Find and remove button + its strut
            int idx = -1;
            for (int i = 0; i < leftWidgetPanel.getComponentCount(); i++) {
                if (leftWidgetPanel.getComponent(i) == button) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                leftWidgetPanel.remove(idx);
                // Remove the strut after it (if exists)
                if (idx < leftWidgetPanel.getComponentCount()) {
                    leftWidgetPanel.remove(idx);
                }
            }

            leftWidgetPanel.revalidate();
            leftWidgetPanel.repaint();
        }
    }

    /**
     * Returns true if any restore buttons are currently visible.
     */
    public boolean hasRestoreButtons() {
        return !restoreButtons.isEmpty();
    }

    private void styleRestoreButton(JButton button) {
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.setFocusable(false);
        button.setMargin(new Insets(1, 4, 1, 4));
    }
}
