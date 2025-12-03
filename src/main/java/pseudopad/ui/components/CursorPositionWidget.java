package pseudopad.ui.components;

import javax.swing.JLabel;

/**
 * Status bar widget to display the current cursor position (Line, Column).
 * 
 * @author Geger John Paul Gabayeron
 */
public class CursorPositionWidget extends JLabel {

    public CursorPositionWidget() {
        super("Ln 1, Col 1");
        // Optional: Add tooltip or specific styling
        setToolTipText("Current Cursor Position");
    }

    public void updatePosition(int line, int column) {
        setText("Ln " + line + ", Col " + column);
    }
}
