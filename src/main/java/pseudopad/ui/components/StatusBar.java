package pseudopad.ui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * A dynamic status bar that sits at the bottom of the application.
 * It supports a main status message and additional widgets on the right.
 * 
 * @author Geger John Paul Gabayeron
 */
public class StatusBar extends JPanel {
    private final JLabel statusLabel;
    private final JPanel rightPanel;

    public StatusBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)));

        // Initialize components
        statusLabel = new JLabel("Ready");
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setOpaque(false);

        // Add components to layout
        add(statusLabel, BorderLayout.WEST);
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
}
