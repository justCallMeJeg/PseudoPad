package pseudopad.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import pseudopad.core.Errors.CompilationError;
import pseudopad.ui.components.TabbedPane;
import pseudopad.app.MainFrame;

public class ProblemsPanel extends JPanel {
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode root;

    public ProblemsPanel() {
        super(new BorderLayout());

        root = new DefaultMutableTreeNode("Problems");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Handle double-click to navigate
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 || e.getClickCount() == 2) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node.getUserObject() instanceof ProblemNodeData data) {
                            navigateToProblem(data);
                        }
                    }
                }
            }
        });

        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    public void updateErrors(File sourceFile, List<CompilationError> errors) {
        SwingUtilities.invokeLater(() -> {
            String validationKey = (sourceFile != null) ? sourceFile.getAbsolutePath() : "Untitled";
            String displayName = "Untitled";

            if (sourceFile != null) {
                File projectPath = MainFrame.getInstance().getCurrentProjectPath();
                if (projectPath != null) {
                    // Make relative
                    String projectAbs = projectPath.getAbsolutePath();
                    String fileAbs = sourceFile.getAbsolutePath();
                    if (fileAbs.startsWith(projectAbs)) {
                        displayName = fileAbs.substring(projectAbs.length());
                        if (displayName.startsWith(File.separator)) {
                            displayName = displayName.substring(1);
                        }
                    } else {
                        displayName = sourceFile.getAbsolutePath();
                    }
                } else {
                    displayName = sourceFile.getAbsolutePath();
                }
            }

            // 1. Remove existing node for this file
            DefaultMutableTreeNode fileNode = null;
            for (int i = 0; i < root.getChildCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
                // Check if this node belongs to the file
                if (node.getUserObject() instanceof FileNodeData data) {
                    if (data.key.equals(validationKey)) {
                        fileNode = node;
                        break;
                    }
                } else if (node.getUserObject().toString().equals(displayName)
                        || node.getUserObject().toString().equals("No problems found.")) {
                    if (node.getUserObject().toString().equals("No problems found.")) {
                        root.remove(node);
                        i--;
                    } else if (sourceFile == null && node.getUserObject().toString().equals("Untitled")) {
                        fileNode = node;
                        break;
                    }
                }
            }

            if (fileNode != null) {
                root.remove(fileNode);
            }

            // 2. Add new node if there are errors
            if (errors != null && !errors.isEmpty()) {
                DefaultMutableTreeNode newFileNode = new DefaultMutableTreeNode(
                        new FileNodeData(displayName, validationKey, sourceFile));
                for (CompilationError error : errors) {
                    DefaultMutableTreeNode errorNode = new DefaultMutableTreeNode(
                            new ProblemNodeData(sourceFile, error));
                    newFileNode.add(errorNode);
                }
                root.add(newFileNode);
            }

            // 3. Show "No problems" if empty
            if (root.getChildCount() == 0) {
                root.add(new DefaultMutableTreeNode("No problems found."));
            }

            treeModel.reload();
            for (int i = 0; i < tree.getRowCount(); i++) {
                tree.expandRow(i);
            }

            // 4. Update Tab Title Count
            updateTabTitleCount();
        });
    }

    private void updateTabTitleCount() {
        int totalErrors = 0;
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
            if (node.getUserObject() instanceof FileNodeData) {
                totalErrors += node.getChildCount();
            }
        }

        // Find parent TabbedPane
        Container parent = getParent();
        while (parent != null && !(parent instanceof TabbedPane)) {
            parent = parent.getParent();
        }

        if (parent instanceof TabbedPane tabs) {
            int index = tabs.indexOfComponent(this);
            if (index != -1) {
                tabs.setTitleAt(index, "Problems [ " + totalErrors + " ]");
            }
        } else {
            // Fallback via MainFrame if not directly parented correctly in hierarchy search
            if (MainFrame.getInstance().getBottomEditorTabbedPane() != null) {
                TabbedPane tabs = MainFrame.getInstance().getBottomEditorTabbedPane();
                int index = tabs.indexOfComponent(this);
                if (index != -1) {
                    tabs.setTitleAt(index, "Problems [ " + totalErrors + " ]");
                }
            }
        }
    }

    private static class FileNodeData {
        final String displayName;
        final String key;
        final File file;

        public FileNodeData(String displayName, String key, File file) {
            this.displayName = displayName;
            this.key = key;
            this.file = file;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private void navigateToProblem(ProblemNodeData data) {
        if (data.file != null) {
            MainFrame.getInstance().getEditorTabbedPane().openFileTab(data.file);
        }

        // Move cursor via MainFrame/EditorTabbedPane
        // We need to assume the active tab is now the correct one
        EditorTabbedPane tabs = MainFrame.getInstance().getEditorTabbedPane();
        Component c = tabs.getSelectedComponent();
        if (c instanceof FileTabPane fileTab) {
            fileTab.getTextPane().setCaretPositionForLine(data.error.line, data.error.column);
            fileTab.getTextPane().requestFocus();
        }
    }

    private static class ProblemNodeData {
        final File file;
        final CompilationError error;

        public ProblemNodeData(File file, CompilationError error) {
            this.file = file;
            this.error = error;
        }

        @Override
        public String toString() {
            return error.message + " [Line " + error.line + ":" + error.column + "]";
        }
    }
}
