package pseudopad.editor;

import javax.swing.JLabel;

/**
 * Status bar widget to display the read-only status of the current file.
 * 
 * @author Geger John Paul Gabayeron
 */
public class ReadOnlyWidget extends JLabel {

    public ReadOnlyWidget() {
        super("RW");
        setToolTipText("Read-Only Status");
    }

    public void setReadOnly(boolean isReadOnly) {
        if (isReadOnly) {
            setText("RO");
            setToolTipText("File is Read-Only");
        } else {
            setText("RW");
            setToolTipText("File is Writable");
        }
    }
}
