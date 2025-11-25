package pseudopad.ui.components;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class FileTabPane extends JPanel {
    private final TextPane textPane;
    private final JScrollPane scrollPane;
    private final RowNumberHeader lineNumbers;

    public FileTabPane() {
        this(""); // Default to empty content
    }

    public FileTabPane(String content) {
        super(new BorderLayout());
        
        // 1. Initialize the Editor
        textPane = new TextPane();
        textPane.setText(content);
        
        // Set a Monospaced font by default for code editing
        // (You can also move this to your ThemeManager later)
        textPane.setFont(new Font("Consolas", Font.PLAIN, 14));

        // 2. Initialize the ScrollPane with the editor
        scrollPane = new JScrollPane(textPane);
        
        // 3. Initialize and attach the Line Numbers
        lineNumbers = new RowNumberHeader(textPane);
        scrollPane.setRowHeaderView(lineNumbers);
        
        // 4. Add to layout
        add(scrollPane, BorderLayout.CENTER);
    }

    public String getText() {
        return textPane.getText();
    }

    public void setText(String text) {
        textPane.setText(text);
        // Move caret to top after loading text
        textPane.setCaretPosition(0); 
    }

    public TextPane getTextPane() {
        return textPane;
    }
}
