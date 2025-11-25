package pseudopad.ui.components;

import java.awt.event.ActionEvent;
import javax.swing.JButton;
import pseudopad.utils.IconManager; // Optional if you have it

/**
 * Specialized TabPane for Editors with an "Add" button.
 */
public class EditorTabbedPane extends TabbedPane {
    private int untitledCount = 0;

    public EditorTabbedPane() {
        super();
        initAddButton();
    }

    private void initAddButton() {
        JButton addBtn = new JButton("+");
        
        // Optional: Use Icon if available
        if (IconManager.get("add") != null) {
            addBtn.setIcon(IconManager.get("add"));
            addBtn.setText("");
        }
        
        addBtn.setToolTipText("New File");
        addBtn.addActionListener((ActionEvent e) -> addNewTab());

        // CRITICAL: Add to the LEFT side
        addLeftHeaderButton(addBtn); 
    }

    public void addNewTab() {
        untitledCount++;
        // Ensure FileTabPane (or EditorTabPane) is defined in your project
        FileTabPane newEditor = new FileTabPane(); 
        this.addTab("Untitled " + untitledCount, newEditor);
        this.setSelectedIndex(this.getTabCount() - 1);
    }
    
    public void openFileTab(String title, String content) {
        FileTabPane newEditor = new FileTabPane();
        newEditor.setText(content);
        this.addTab(title, newEditor);
        this.setSelectedComponent(newEditor);
    }
}
