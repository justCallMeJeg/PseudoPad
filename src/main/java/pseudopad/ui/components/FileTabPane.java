package pseudopad.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import pseudopad.utils.FileManager;
import pseudopad.utils.ThemeManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class FileTabPane extends JPanel {
    private final TextPane textPane;
    private final JScrollPane scrollPane;
    private final RowNumberHeader lineNumbers;
    
    private File fileSource; // Null if it's a new "Untitled" file
    private String originalContent;
    private boolean isDirty = false;
    private String tabTitle; // Store the clean title (without *)

    public FileTabPane() {
        this(null); 
    }

    public FileTabPane(File source) {
        super(new BorderLayout());
        
        this.fileSource = source;
        
        if (source != null) {
            try {
                this.originalContent = FileManager.readFile(source);
            } catch (IOException ex) {
                System.err.println("Failed to open file: " + ex);
                this.originalContent = "";
            }
        } else {
            this.originalContent = "";
        }
        
        // 1. Initialize Editor
        textPane = new TextPane();
        textPane.setText(this.originalContent);
        textPane.setFont(new Font("Consolas", Font.PLAIN, 14));
        textPane.setCaretPosition(0);

        // 2. Scroll & Gutter
        scrollPane = new JScrollPane(textPane);
        lineNumbers = new RowNumberHeader(textPane);
        scrollPane.setRowHeaderView(lineNumbers);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // 3. Track Changes
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override 
            public void insertUpdate(DocumentEvent e) { checkDirty(); }
            @Override 
            public void removeUpdate(DocumentEvent e) { checkDirty(); }
            @Override 
            public void changedUpdate(DocumentEvent e) { checkDirty(); }
        });
    }
    
    private void checkDirty() {
        boolean changed = !textPane.getText().equals(originalContent);
        
        if (changed != isDirty) {
            isDirty = changed;
            updateTabTitle();
        }
    }
    
    private void updateTabTitle() {
        // We need to find our parent TabbedPane to update the title
        EditorTabbedPane parentTab = (EditorTabbedPane) SwingUtilities.getAncestorOfClass(EditorTabbedPane.class, this);
        if (parentTab != null) {
            int index = parentTab.indexOfComponent(this);
            if (index != -1) {
                // If title is not set yet, grab it from the tab
                if (tabTitle == null) tabTitle = parentTab.getTitleAt(index).replace("*", "").trim();
                
                String newTitle = tabTitle + (isDirty ? " *" : "");
                parentTab.setTitleAt(index, newTitle);
                
                parentTab.setForegroundAt(index, isDirty ? Color.CYAN : null); // Simple visual cue
            }
        }
    }
    
    public boolean requestClose() {
        if (!isDirty) return true; // Safe to close
        
        String name = (fileSource != null) ? fileSource.getName() : (tabTitle != null ? tabTitle : "Untitled");
        
        int choice = JOptionPane.showConfirmDialog(null, 
                "Do you want to save changes to '" + name + "'?", 
                "Unsaved Changes", 
                JOptionPane.YES_NO_CANCEL_OPTION);
        
        
        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
            return false; // Abort closing
        }
        
        if (choice == JOptionPane.YES_OPTION) {
            return saveFile(); // Close only if save succeeds
        }
        
        return true; // NO_OPTION -> Discard changes and close
    }
    
    public boolean saveFile() {
        if (fileSource == null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Specify a file to save");

            // Optional: Set a file filter (e.g., only .txt files)
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Documents (*.pc)", "pc");
            fileChooser.setFileFilter(filter);

            // Show the Save dialog
            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();

                // Ensure the file has the correct extension if needed (optional logic)
                if (!filePath.toLowerCase().endsWith(".pc")) {
                    fileToSave = new File(filePath + ".pc");
                }
                
                this.fileSource = fileToSave;
                saveFile();
            }
        }
        
        try {
            FileManager.saveFile(fileSource, textPane.getText());
            originalContent = textPane.getText(); // Update baseline
            isDirty = false;
            updateTabTitle();
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage());
            return false;
        }
    }
    
    // ----- SETTERS and GETTERS -----
    
    public File getFile() {
        return fileSource;
    }
    
    public void setFile(File file) {
        this.fileSource = file;
    }
    
    public String getText() {
        return textPane.getText();
    }
}
