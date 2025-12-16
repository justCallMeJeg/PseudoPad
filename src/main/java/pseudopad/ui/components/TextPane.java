package pseudopad.ui.components;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * A theme-aware text pane component.
 * Automatically updates text colors when theme changes.
 *
 * @author Geger John Paul Gabayeron
 */
public class TextPane extends JTextPane {

    // Key for marking text that should follow theme
    // private static final String THEME_AWARE_KEY = "themeAware";

    public TextPane() {
        super();
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Listen for theme changes and update colors
        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName())) {
                SwingUtilities.invokeLater(() -> {
                    // Update the component's colors
                    setForeground(UIManager.getColor("TextPane.foreground"));
                    setBackground(UIManager.getColor("TextPane.background"));
                    setCaretColor(UIManager.getColor("TextPane.caretForeground"));

                    // Update all existing text to use new theme color
                    updateTextColors();
                });
            }
        });
    }

    /**
     * Updates all text in the document to use the current theme's foreground color.
     * This is called when the theme changes.
     */
    private void updateTextColors() {
        StyledDocument doc = getStyledDocument();
        if (doc.getLength() == 0)
            return;

        Color themeForeground = UIManager.getColor("TextPane.foreground");
        if (themeForeground == null) {
            themeForeground = UIManager.getColor("Panel.foreground");
        }
        if (themeForeground == null) {
            themeForeground = getForeground();
        }

        // Create a new attribute set with the theme foreground
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, themeForeground);

        // Apply to all text (preserving other attributes like bold/italic)
        doc.setCharacterAttributes(0, doc.getLength(), attrs, false);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // Return true if the text content is smaller than the view (to center/align
        // properly)
        // Return false if text is wider than the view (to trigger the scrollbar)
        return getUI().getPreferredSize(this).width <= getParent().getSize().width;
    }

    // Ensure it doesn't try to wrap based on size
    @Override
    public void setSize(Dimension d) {
        if (d.width < getParent().getSize().width) {
            d.width = getParent().getSize().width;
        }
        super.setSize(d);
    }

    public void setCaretPositionForLine(int line, int column) {
        try {
            Element root = getDocument().getDefaultRootElement();
            // line is 1-based, root element index is 0-based
            int lineIndex = Math.max(0, Math.min(line - 1, root.getElementCount() - 1));
            Element lineElem = root.getElement(lineIndex);

            // column is 1-based usually
            int offset = lineElem.getStartOffset() + Math.max(0, column - 1);
            offset = Math.min(offset, lineElem.getEndOffset() - 1); // Ensure within line (before \n)

            setCaretPosition(offset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
