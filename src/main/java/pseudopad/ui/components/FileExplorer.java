package pseudopad.ui.components;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class FileExplorer extends JPanel {
    private final JTree fileTree;
    private final DefaultTreeModel treeModel;
    private File currentProjectRoot;

    // Callback to tell the main app to open a file
    private Consumer<File> onFileOpen;

    // Callback to tell the main app a file was renamed
    private BiConsumer<File, File> onFileRenamed;

    public FileExplorer() {
        super(new BorderLayout());

        // 1. Initialize Tree with a dummy root initially
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("No Project Opened");
        treeModel = new DefaultTreeModel(root);
        fileTree = new JTree(treeModel);

        // 2. Visual Polish
        fileTree.setCellRenderer(new FileTreeCellRenderer());
        fileTree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 3. Lazy Loading Logic (Performance)
        fileTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                // When a folder is about to expand, load its children if not already loaded
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                loadChildren(node, false);
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            }
        });

        // 5. Context Menu for Rename
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("Rename");
        renameItem.addActionListener(e -> renameSelectedFile());
        contextMenu.add(renameItem);

        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Double Click to Open
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && onFileOpen != null) {
                    TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node.getUserObject() instanceof File file) {
                            if (file.isFile()) {
                                onFileOpen.accept(file); // Trigger the callback
                            }
                        }
                    }
                }

                // Right Click for Context Menu
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = fileTree.getClosestRowForLocation(e.getX(), e.getY());
                    fileTree.setSelectionRow(row);
                    contextMenu.show(fileTree, e.getX(), e.getY());
                }
            }
        });

        // 5. Add to layout
        add(new JScrollPane(fileTree), BorderLayout.CENTER);
    }

    public void clear() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("No Project Opened");
        treeModel.setRoot(root);
    }

    public void setOnFileOpenListener(Consumer<File> listener) {
        this.onFileOpen = listener;
    }

    public void setOnFileRenamedListener(BiConsumer<File, File> listener) {
        this.onFileRenamed = listener;
    }

    public void openProject(File projectDir) {
        this.currentProjectRoot = projectDir;

        // Create Root Node
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(projectDir);
        treeModel.setRoot(rootNode);

        // Load initial children
        loadChildren(rootNode, true);
    }

    private void loadChildren(DefaultMutableTreeNode node, boolean isRefresh) {
        Object userObject = node.getUserObject();
        if (!(userObject instanceof File)) {
            return;
        }

        File folder = (File) userObject;
        if (!folder.isDirectory()) {
            return;
        }

        File[] files = folder.listFiles();
        if (files == null)
            return;

        // Sort: Folders first, then files
        Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory())
                return -1;
            if (!f1.isDirectory() && f2.isDirectory())
                return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        // Clear children (including "Loading..." dummy)
        node.removeAllChildren();

        for (File file : files) {
            // Filter out hidden files if you want
            if (file.isHidden())
                continue;

            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file);

            // If it's a directory, add a dummy child so it can be expanded later
            if (file.isDirectory()) {
                childNode.add(new DefaultMutableTreeNode("Loading..."));
            }

            node.add(childNode);
        }

        treeModel.nodeStructureChanged(node);
    }

    public void refresh() {
        if (currentProjectRoot == null) {
            return;
        }

        // Helper to recursively refresh only what is currently visible
        refreshNode((DefaultMutableTreeNode) treeModel.getRoot());
    }

    private void refreshNode(DefaultMutableTreeNode node) {
        // Only refresh if it has children (meaning it was loaded)
        if (node.getChildCount() > 0) {
            // Check if it's the "Loading..." dummy
            if (node.getFirstChild().toString().equals("Loading...")) {
                return; // Not expanded yet, ignore
            }

            // 1. Reload THIS node's children from disk
            loadChildren(node, true);

            // 2. Recursively check children (to find sub-folders that were expanded)
            // Note: This simple recursion might lose selection of deep sub-files
            // if the object references change, but it keeps the structure updated.
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                if (child.getUserObject() instanceof File && ((File) child.getUserObject()).isDirectory()) {
                    // Ideally, check if 'child' corresponds to an expanded path in the JTree
                    // and recurse only then.
                }
            }
        }
    }

    private void renameSelectedFile() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();
        if (node == null)
            return;

        Object userObject = node.getUserObject();
        if (!(userObject instanceof File))
            return;

        File fileToRename = (File) userObject;

        String newName = JOptionPane.showInputDialog(this, "Enter new name:", fileToRename.getName());
        if (newName == null || newName.trim().isEmpty())
            return;

        File newFile = new File(fileToRename.getParent(), newName);

        try {
            pseudopad.utils.FileManager.rename(fileToRename, newFile);

            // Notify listeners
            if (onFileRenamed != null) {
                onFileRenamed.accept(fileToRename, newFile);
            }

            // Refresh Tree
            // Ideally we should just update the node, but a full refresh is safer for now
            refresh();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error renaming file: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
