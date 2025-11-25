package pseudopad.ui.components;

import java.awt.Dimension;
import javax.swing.JTextPane;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class TextPane extends JTextPane {
    public TextPane() {
        super();
    }
    
    @Override
    public boolean getScrollableTracksViewportWidth() {
        // Return true if the text content is smaller than the view (to center/align properly)
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
}
