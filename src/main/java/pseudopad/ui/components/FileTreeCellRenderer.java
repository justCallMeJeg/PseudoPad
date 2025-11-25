package pseudopad.ui.components;

import java.awt.Component;
import java.io.File;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import pseudopad.utils.IconManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class FileTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, 
        boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        
        // Let the default renderer handle colors and selection visuals
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        // Customize the text and icon
        if (value instanceof DefaultMutableTreeNode node) {
            Object userObject = node.getUserObject();

            if (userObject instanceof File file) {
                
                // 1. Set Label: Show only the filename, not the full path
                setText(file.getName());

                // 2. Set Icon: Folder or File?
                if (file.isDirectory()) {
                    // You might want to add a "FOLDER" (closed) icon to IconManager too
                    if (file.getName().equals(".pseudocode")) {
                        setIcon(IconManager.get(expanded ? "folder_settings_open" : "folder_settings"));
                    } else {
                        setIcon(IconManager.get(expanded ? "folder_open" : "folder"));
                    }
                } else {
                    // Check extension for specific icons
                    if (file.getName().endsWith(".pc")) {
                        setIcon(IconManager.get("file_pseudocode"));
                    } else {
                        setIcon(IconManager.get("file"));
                    }
                }
            }
        }
        return this;
    }
}
