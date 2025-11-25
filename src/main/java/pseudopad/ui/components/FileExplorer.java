package pseudopad.ui.components;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
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
                loadChildren(node);
            }
            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {}
        });
        
        // 4. Double Click to Open
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2 && onFileOpen != null) {
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
            }
        });

        // 5. Add to layout
        add(new JScrollPane(fileTree), BorderLayout.CENTER);
    }
    
    public void setOnFileOpenListener(Consumer<File> listener) {
        this.onFileOpen = listener;
    }

    public void openProject(File projectDir) {
        this.currentProjectRoot = projectDir;
        
        // Create Root Node
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(projectDir);
        
        // Add a dummy child so the "+" handle appears (allowing lazy load)
        rootNode.add(new DefaultMutableTreeNode("Loading..."));
        
        treeModel.setRoot(rootNode);
        
        // Automatically expand the root to show top-level files
        loadChildren(rootNode); 
        fileTree.expandRow(0);
    }

    private void loadChildren(DefaultMutableTreeNode node) {
        // If it's already loaded (doesn't have the dummy node), skip
        if (node.getChildCount() > 0 && !(node.getFirstChild().toString().equals("Loading..."))) {
            return;
        }

        File folder = (File) node.getUserObject();
        if (!folder.isDirectory()) return;

        File[] files = folder.listFiles();
        if (files == null) return;

        // Sort: Folders first, then files
        Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        // Clear "Loading..." dummy
        node.removeAllChildren();

        for (File file : files) {
            // Filter out hidden files if you want
            if (file.isHidden()) continue;

            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file);
            
            // If it's a directory, add a dummy child so it can be expanded later
            if (file.isDirectory()) {
                childNode.add(new DefaultMutableTreeNode("Loading..."));
            }
            
            node.add(childNode);
        }

        treeModel.nodeStructureChanged(node);
    }
}
