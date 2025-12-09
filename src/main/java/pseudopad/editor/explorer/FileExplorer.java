package pseudopad.editor.explorer;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import pseudopad.app.AppLogger;

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

    // Callback to tell the main app a file was deleted
    private Consumer<File> onFileDeleted;

    // Clipboard State
    private List<File> clipboardFiles = new ArrayList<>();
    private boolean isCutOperation = false;

    public FileExplorer() {
        super(new BorderLayout());

        // 1. Initialize Tree with a dummy root initially
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("No Project Opened");
        treeModel = new DefaultTreeModel(root);
        fileTree = new JTree(treeModel);

        // 2. Visual Polish
        fileTree.setCellRenderer(new FileTreeCellRenderer());
        fileTree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        fileTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        // Drag and Drop
        fileTree.setDragEnabled(true);
        fileTree.setDropMode(DropMode.ON);
        fileTree.setTransferHandler(new FileTransferHandler());

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

        // 5. Context Menu
        JPopupMenu contextMenu = new JPopupMenu();

        // New Submenu
        JMenu newMenu = new JMenu("New");
        JMenuItem newFolderItem = new JMenuItem("Folder");
        newFolderItem.addActionListener(e -> createNewFolder());
        newMenu.add(newFolderItem);

        JMenuItem newFileItem = new JMenuItem("File (.pc)");
        newFileItem.addActionListener(e -> createNewFile(".pc"));
        newMenu.add(newFileItem);
        contextMenu.add(newMenu);

        contextMenu.addSeparator();

        JMenuItem renameItem = new JMenuItem("Rename");
        renameItem.addActionListener(e -> renameSelectedFile());
        contextMenu.add(renameItem);

        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> deleteSelectedFile());
        contextMenu.add(deleteItem);

        contextMenu.addSeparator();

        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.addActionListener(e -> copy());
        contextMenu.add(copyItem);

        JMenuItem cutItem = new JMenuItem("Cut");
        cutItem.addActionListener(e -> cut());
        contextMenu.add(cutItem);

        JMenuItem pasteItem = new JMenuItem("Paste");
        pasteItem.addActionListener(e -> paste());
        contextMenu.add(pasteItem);

        // Keyboard Shortcuts for Tree
        fileTree.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "copy");
        fileTree.getActionMap().put("copy", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copy();
            }
        });

        fileTree.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "cut");
        fileTree.getActionMap().put("cut", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cut();
            }
        });

        fileTree.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "paste");
        fileTree.getActionMap().put("paste", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                paste();
            }
        });

        fileTree.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        fileTree.getActionMap().put("delete", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedFile();
            }
        });

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
                    if (row != -1) {
                        boolean isSelected = false;
                        int[] selectedRows = fileTree.getSelectionRows();
                        if (selectedRows != null) {
                            for (int r : selectedRows) {
                                if (r == row) {
                                    isSelected = true;
                                    break;
                                }
                            }
                        }
                        if (!isSelected) {
                            fileTree.setSelectionRow(row);
                        }
                    }
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

    public void setOnFileDeletedListener(Consumer<File> listener) {
        this.onFileDeleted = listener;
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
        // Rename only supports single selection
        if (fileTree.getSelectionCount() != 1) {
            return;
        }

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
            AppLogger.info("Renamed file: " + fileToRename.getName() + " -> " + newFile.getName());

            // Notify listeners
            if (onFileRenamed != null) {
                onFileRenamed.accept(fileToRename, newFile);
            }

            // Refresh Tree
            refresh();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error renaming file: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createNewFolder() {
        File parentDir = getTargetParentDir();
        if (parentDir == null)
            return;

        String folderName = JOptionPane.showInputDialog(this, "Enter folder name:");
        if (folderName == null || folderName.trim().isEmpty())
            return;

        File newFolder = new File(parentDir, folderName);
        if (newFolder.mkdir()) {
            AppLogger.info("Created folder: " + newFolder.getAbsolutePath());
            refresh();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to create folder.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createNewFile(String extension) {
        File parentDir = getTargetParentDir();
        if (parentDir == null)
            return;

        String fileName = JOptionPane.showInputDialog(this, "Enter file name (without extension):");
        if (fileName == null || fileName.trim().isEmpty())
            return;

        if (!fileName.endsWith(extension)) {
            fileName += extension;
        }

        File newFile = new File(parentDir, fileName);
        try {
            if (newFile.createNewFile()) {
                AppLogger.info("Created file: " + newFile.getAbsolutePath());
                refresh();
                if (onFileOpen != null) {
                    onFileOpen.accept(newFile);
                }
            } else {
                JOptionPane.showMessageDialog(this, "File already exists or failed to create.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            AppLogger.error("Error creating file", e);
            JOptionPane.showMessageDialog(this, "Error creating file: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private File getTargetParentDir() {
        File parentDir = currentProjectRoot;
        List<File> selected = getSelectedFiles();
        if (selected.size() == 1) {
            File f = selected.get(0);
            if (f.isDirectory()) {
                parentDir = f;
            } else {
                parentDir = f.getParentFile();
            }
        }
        return parentDir;
    }

    public void deleteSelectedFile() {
        List<File> filesToDelete = getSelectedFiles();
        if (filesToDelete.isEmpty())
            return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete " + filesToDelete.size() + " item(s)?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION)
            return;

        for (File fileToDelete : filesToDelete) {
            try {
                if (fileToDelete.isDirectory()) {
                    pseudopad.utils.FileManager.deleteDirectoryForcefully(fileToDelete);
                } else {
                    pseudopad.utils.FileManager.delete(fileToDelete);
                }
                AppLogger.info("Deleted file: " + fileToDelete.getAbsolutePath());

                // Notify listeners
                if (onFileDeleted != null) {
                    onFileDeleted.accept(fileToDelete);
                }

            } catch (Exception ex) {
                AppLogger.error("Error deleting file: " + fileToDelete.getName(), ex);
                JOptionPane.showMessageDialog(this, "Error deleting file: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        // Refresh Tree
        refresh();
    }

    public void copy() {
        List<File> selectedFiles = getSelectedFiles();
        if (!selectedFiles.isEmpty()) {
            AppLogger.info("Copied " + selectedFiles.size() + " file(s) to clipboard");
            setClipboard(selectedFiles, false);
        }
    }

    public void cut() {
        List<File> selectedFiles = getSelectedFiles();
        if (!selectedFiles.isEmpty()) {
            AppLogger.info("Cut " + selectedFiles.size() + " file(s) to clipboard");
            setClipboard(selectedFiles, true);
        }
    }

    public void paste() {
        File targetDir = null;

        // Try to get the single selected folder as target
        List<File> selected = getSelectedFiles();
        if (selected.size() == 1) {
            File f = selected.get(0);
            if (f.isDirectory()) {
                targetDir = f;
            } else {
                targetDir = f.getParentFile();
            }
        } else {
            // If multiple selected or none, use project root
            targetDir = currentProjectRoot;
        }

        if (targetDir == null || !targetDir.exists()) {
            return;
        }

        List<File> filesToPaste = getClipboardFiles();
        if (filesToPaste == null || filesToPaste.isEmpty()) {
            return;
        }

        for (File srcFile : filesToPaste) {
            if (!srcFile.exists())
                continue;

            File destFile = new File(targetDir, srcFile.getName());

            // Auto-rename if exists and not moving (or even if moving, to avoid overwrite)
            if (destFile.exists()) {
                String name = srcFile.getName();
                String baseName = name;
                String ext = "";
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex > 0) {
                    baseName = name.substring(0, dotIndex);
                    ext = name.substring(dotIndex);
                }
                int counter = 1;
                while (destFile.exists()) {
                    destFile = new File(targetDir, baseName + " (" + counter++ + ")" + ext);
                }
            }

            try {
                if (isCutOperation) {
                    Files.move(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    AppLogger.info("Moved file: " + srcFile.getName() + " -> " + destFile.getName());
                } else {
                    copyRecursive(srcFile, destFile);
                    AppLogger.info("Pasted file: " + srcFile.getName() + " -> " + destFile.getName());
                }
            } catch (IOException e) {
                e.printStackTrace();
                AppLogger.error("Error pasting file: " + srcFile.getName(), e);
                JOptionPane.showMessageDialog(this, "Error pasting file: " + e.getMessage());
            }
        }

        // If it was a cut operation, clear the clipboard state after paste
        if (isCutOperation) {
            isCutOperation = false;
            clipboardFiles.clear();
        }

        refresh();
    }

    private void copyRecursive(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdir();
            }
            String[] children = src.list();
            if (children != null) {
                for (String child : children) {
                    copyRecursive(new File(src, child), new File(dest, child));
                }
            }
        } else {
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private List<File> getSelectedFiles() {
        List<File> files = new ArrayList<>();
        TreePath[] paths = fileTree.getSelectionPaths();
        if (paths != null) {
            for (TreePath path : paths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = node.getUserObject();
                if (userObject instanceof File) {
                    files.add((File) userObject);
                }
            }
        }
        return files;
    }

    // --- Clipboard Helpers ---

    private void setClipboard(List<File> files, boolean isCut) {
        this.clipboardFiles = new ArrayList<>(files);
        this.isCutOperation = isCut;

        // Also set to System Clipboard for external paste
        FileTransferable transferable = new FileTransferable(files);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
    }

    private List<File> getClipboardFiles() {
        // First check system clipboard to see if we have external files
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            try {
                @SuppressWarnings("unchecked")
                List<File> sysFiles = (List<File>) contents.getTransferData(DataFlavor.javaFileListFlavor);

                // If system clipboard matches our internal state, respect isCutOperation
                // Otherwise, treat as new copy from external source
                if (sysFiles.equals(clipboardFiles)) {
                    return clipboardFiles;
                } else {
                    isCutOperation = false; // External paste is always a copy
                    return sysFiles;
                }
            } catch (UnsupportedFlavorException | IOException e) {
                e.printStackTrace();
            }
        }
        return clipboardFiles;
    }

    private static class FileTransferable implements Transferable {
        private final List<File> files;

        public FileTransferable(List<File> files) {
            this.files = files;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.javaFileListFlavor };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.javaFileListFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return files;
        }
    }

    private class FileTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(javax.swing.JComponent c) {
            return COPY_OR_MOVE;
        }

        @Override
        protected Transferable createTransferable(javax.swing.JComponent c) {
            List<File> files = getSelectedFiles();
            if (files.isEmpty())
                return null;
            return new FileTransferable(files);
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false;
            }
            // Don't allow dropping on itself (handled by JTree default logic usually, but
            // good to check)
            return true;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                // Get dropped files
                @SuppressWarnings("unchecked")
                List<File> droppedFiles = (List<File>) support.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);

                // Get target location
                JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
                TreePath path = dl.getPath();
                if (path == null)
                    return false;

                DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = targetNode.getUserObject();

                File targetDir;
                if (userObject instanceof File f && f.isDirectory()) {
                    targetDir = f;
                } else if (userObject instanceof File f) {
                    targetDir = f.getParentFile();
                } else {
                    targetDir = currentProjectRoot;
                }

                if (targetDir == null)
                    return false;

                // Perform Move (Default for DnD within app)
                boolean isMove = (support.getDropAction() & MOVE) == MOVE;

                for (File srcFile : droppedFiles) {
                    if (srcFile.getParentFile().equals(targetDir))
                        continue; // Same dir

                    File destFile = new File(targetDir, srcFile.getName());

                    // Auto-rename logic
                    if (destFile.exists()) {
                        String name = srcFile.getName();
                        String baseName = name;
                        String ext = "";
                        int dotIndex = name.lastIndexOf('.');
                        if (dotIndex > 0) {
                            baseName = name.substring(0, dotIndex);
                            ext = name.substring(dotIndex);
                        }
                        int counter = 1;
                        while (destFile.exists()) {
                            destFile = new File(targetDir, baseName + " (" + counter++ + ")" + ext);
                        }
                    }

                    if (isMove) {
                        Files.move(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        AppLogger.info("Moved (DnD): " + srcFile.getName() + " -> " + destFile.getName());
                    } else {
                        copyRecursive(srcFile, destFile);
                        AppLogger.info("Copied (DnD): " + srcFile.getName() + " -> " + destFile.getName());
                    }
                }

                refresh();
                return true;

            } catch (Exception e) {
                e.printStackTrace();
                AppLogger.error("Drag and Drop failed", e);
            }
            return false;
        }
    }
}
