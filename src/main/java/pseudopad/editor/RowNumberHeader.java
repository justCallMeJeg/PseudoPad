package pseudopad.editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class RowNumberHeader extends JPanel {
    private final JEditorPane textPane;

    public RowNumberHeader(JEditorPane textPane) {
        this.textPane = textPane;

        // Visual Polish
        this.setBackground(UIManager.getColor("Panel.background")); // Matches theme
        this.setBorder(new EmptyBorder(0, 5, 0, 10)); // Padding: Top, Left, Bottom, Right

        // Listen for changes so we can repaint numbers when lines are added/removed
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });
    }

    /**
     * Force the component to recalculate its width based on the number of digits.
     */
    private void update() {
        revalidate();
        repaint();
    }

    /**
     * Calculate the preferred width: "9" needs less space than "1000".
     */
    @Override
    public Dimension getPreferredSize() {
        int lines = getLineCount();
        int digits = String.valueOf(lines).length();

        // Get width of '0' character in the current font
        FontMetrics fm = getFontMetrics(getFont());
        int charWidth = fm.charWidth('0');
        int padding = 20; // Extra space for comfort

        int width = (digits * charWidth) + padding;
        return new Dimension(width, textPane.getHeight()); // Height matches editor
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Sync font with editor
        g.setFont(textPane.getFont());
        FontMetrics fm = g.getFontMetrics();

        // Drawing Logic
        Rectangle clip = g.getClipBounds();
        int startOffset = textPane.viewToModel2D(new Point(0, clip.y));
        int endOffset = textPane.viewToModel2D(new Point(0, clip.y + clip.height));

        // Use the Document's root element to find line indices
        Element root = textPane.getDocument().getDefaultRootElement();
        int startLine = root.getElementIndex(startOffset);
        int endLine = root.getElementIndex(endOffset);

        // Set Color for numbers (Gray is standard)
        g.setColor(Color.GRAY);

        for (int i = startLine; i <= endLine; i++) {
            // Get the exact Y location of this line from the editor
            Element line = root.getElement(i);
            int lineStart = line.getStartOffset();

            try {
                // Get the rectangle coordinates of the text line
                Rectangle r = textPane.modelToView2D(lineStart).getBounds();

                // Draw the number right-aligned
                String number = String.valueOf(i + 1);
                int stringWidth = fm.stringWidth(number);
                int x = getWidth() - stringWidth - 10; // 10px padding from right
                int y = r.y + fm.getAscent(); // Align baseline

                g.drawString(number, x, y);

                // Draw Error Indicator
                if (errorLines.contains(i + 1)) {
                    g.setColor(new Color(255, 100, 100));
                    int dotSize = 4;
                    int dotX = 2; // Left margin
                    int dotY = r.y + (r.height - dotSize) / 2;
                    g.fillOval(dotX, dotY, dotSize, dotSize);
                    g.setColor(Color.GRAY); // Reset for next number
                }

            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private Set<Integer> errorLines = new HashSet<>();

    public void setErrorLines(Set<Integer> lines) {
        this.errorLines = lines;
        update();
    }

    private int getLineCount() {
        return textPane.getDocument().getDefaultRootElement().getElementCount();
    }
}
