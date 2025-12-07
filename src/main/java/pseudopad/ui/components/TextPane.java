package pseudopad.ui.components;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JTextPane;
import javax.swing.text.Element;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class TextPane extends JTextPane {
    public TextPane() {
        super();
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
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
