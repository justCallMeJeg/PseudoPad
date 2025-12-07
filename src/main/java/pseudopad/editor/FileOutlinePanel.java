package pseudopad.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import pseudopad.app.MainFrame;
import pseudopad.core.AST;
import pseudopad.core.Token;

public class FileOutlinePanel extends JPanel {

    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode root;

    public FileOutlinePanel() {
        super(new BorderLayout());

        root = new DefaultMutableTreeNode("Outline");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);

        tree.setRootVisible(false); // Hide the root "Outline"
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new OutlineTreeCellRenderer());

        // Handle click to navigate
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 || e.getClickCount() == 2) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node.getUserObject() instanceof OutlineNodeData data) {
                            navigate(data);
                        }
                    }
                }
            }
        });

        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    public void updateOutline(AST.ProgramNode program) {
        SwingUtilities.invokeLater(() -> {
            root.removeAllChildren();

            if (program != null) {
                for (AST.Node node : program.statements) {
                    if (node instanceof AST.FunctionNode funcNode) {
                        DefaultMutableTreeNode funcTree = new DefaultMutableTreeNode(new OutlineNodeData(funcNode));
                        root.add(funcTree);
                    } else if (node instanceof AST.ClassNode classNode) {
                        DefaultMutableTreeNode classTree = new DefaultMutableTreeNode(new OutlineNodeData(classNode));
                        root.add(classTree);
                        // Add members
                        for (AST.VariableDeclarationNode field : classNode.fields) {
                            classTree.add(new DefaultMutableTreeNode(new OutlineNodeData(field)));
                        }
                        for (AST.FunctionNode method : classNode.methods) {
                            classTree.add(new DefaultMutableTreeNode(new OutlineNodeData(method)));
                        }
                    } else if (node instanceof AST.VariableDeclarationNode varNode) {
                        root.add(new DefaultMutableTreeNode(new OutlineNodeData(varNode)));
                    }
                }
            }

            if (root.getChildCount() == 0) {
                root.add(new DefaultMutableTreeNode("No identifiers found."));
            }

            treeModel.reload();
            for (int i = 0; i < tree.getRowCount(); i++) {
                tree.expandRow(i);
            }
        });
    }

    private void navigate(OutlineNodeData data) {
        Token token = data.getToken();
        if (token != null) {
            EditorTabbedPane tabs = MainFrame.getInstance().getEditorTabbedPane();
            Component c = tabs.getSelectedComponent();
            if (c instanceof FileTabPane fileTab) {
                // Use token.line and token.column
                fileTab.getTextPane().setCaretPositionForLine(token.line, token.column);
                fileTab.getTextPane().requestFocus();
            }
        }
    }

    private static class OutlineNodeData {
        private final Object node;

        public OutlineNodeData(Object node) {
            this.node = node;
        }

        public Token getToken() {
            if (node instanceof AST.FunctionNode fn) {
                return fn.nameToken;
            } else if (node instanceof AST.ClassNode cn) {
                return cn.nameToken;
            } else if (node instanceof AST.VariableDeclarationNode vn) {
                return vn.nameToken;
            }
            return null;
        }

        @Override
        public String toString() {
            if (node instanceof AST.FunctionNode fn) {
                StringBuilder sb = new StringBuilder();
                sb.append(fn.name).append("(");
                if (fn.parameters != null) {
                    for (int i = 0; i < fn.parameters.size(); i++) {
                        AST.FunctionNode.Parameter p = fn.parameters.get(i);
                        sb.append(p.name()).append(": ").append(p.type());
                        if (i < fn.parameters.size() - 1)
                            sb.append(", ");
                    }
                }
                sb.append(")");
                if (fn.returnType != null && !fn.returnType.isEmpty()) {
                    sb.append(": ").append(fn.returnType);
                }
                return sb.toString();
            } else if (node instanceof AST.ClassNode cn) {
                return "class " + cn.name;
            } else if (node instanceof AST.VariableDeclarationNode vn) {
                return vn.identifier + ": " + vn.typeName;
            }
            return node.toString();
        }

        public String getIconKey() {
            if (node instanceof AST.FunctionNode)
                return "Function";
            if (node instanceof AST.ClassNode)
                return "Class";
            if (node instanceof AST.VariableDeclarationNode)
                return "Variable";
            return "Default";
        }
    }

    private static class OutlineTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode node) {
                if (node.getUserObject() instanceof OutlineNodeData data) {
                    // We could set icons here if we had them
                    // setIcon(IconManager.getIcon(data.getIconKey()));
                    setText(data.toString());
                }
            }
            return this;
        }
    }
}
