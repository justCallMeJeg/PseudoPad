package pseudopad.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import pseudopad.app.MainFrame;
import pseudopad.editor.CursorPositionWidget;
import pseudopad.editor.EditorTabbedPane;
import pseudopad.editor.FileExplorer;
import pseudopad.editor.FileTabPane;
import pseudopad.editor.MemoryUsageWidget;
import pseudopad.editor.ProblemsPanel;
import pseudopad.editor.FileOutlinePanel;
import pseudopad.editor.ReadOnlyWidget;
import pseudopad.editor.StatusBar;
import pseudopad.editor.TerminalPane;
import pseudopad.editor.terminal.SimpleTerminalBackend;
import pseudopad.ui.components.AppMenuBar;
import pseudopad.ui.components.AppToolBar;
import pseudopad.ui.components.RecentProjectsPanel;
import pseudopad.ui.components.TabbedPane;
import pseudopad.ui.components.TextPane;
import pseudopad.utils.AppActionsManager;
import pseudopad.utils.AppConstants;

/**
 * Manages the UI layout and components for the MainFrame.
 * 
 * @author Geger John Paul Gabayeron
 */
public class MainLayout extends JPanel {
    private final MainFrame mainFrame;
    private final AppActionsManager appActions;

    private JMenuBar menuBar;
    private JSplitPane mainSplitPane;
    private JSplitPane navigationSplitPane;
    private JSplitPane editorSplitPane;
    private TabbedPane topNavigationTabbedPane;
    private TabbedPane bottomNavigationTabbedPane;
    private FileExplorer fileExplorer;
    private RecentProjectsPanel projectExplorer;
    private EditorTabbedPane editorTabbedPane;
    private TabbedPane bottomEditorTabbedPane;

    private TerminalPane terminalTextPane;
    private TextPane logTextPane;
    private StatusBar statusBar;

    private CursorPositionWidget cursorWidget;
    private ReadOnlyWidget readOnlyWidget;
    private MemoryUsageWidget memoryWidget;

    private ProblemsPanel problemsPanel;

    private FileOutlinePanel fileOutlinePanel;

    public MainLayout(MainFrame mainFrame, AppActionsManager appActions) {
        this.mainFrame = mainFrame;
        this.appActions = appActions;
        setLayout(new BorderLayout());
        initUIComponents();
    }

    private void initUIComponents() {
        initComponents();

        Border lineBorder = BorderFactory.createLineBorder(UIManager.getColor("Panel.foreground").darker(), 1);

        this.menuBar = new AppMenuBar(this.appActions);
        mainFrame.setJMenuBar(menuBar);

        AppToolBar toolBar = new AppToolBar(this.appActions);
        add(toolBar, BorderLayout.NORTH);

        this.mainSplitPane.setResizeWeight(AppConstants.MAIN_SPLIT_RESIZE_WEIGHT);
        this.mainSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        add(mainSplitPane, BorderLayout.CENTER);

        // Add Status Bar
        add(statusBar, BorderLayout.SOUTH);

        this.navigationSplitPane.setBorder(lineBorder);
        this.navigationSplitPane.setResizeWeight(AppConstants.NAV_SPLIT_RESIZE_WEIGHT);
        this.navigationSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

        this.fileExplorer.setOnFileOpenListener((File file) -> {
            editorTabbedPane.openFileTab(file);
        });

        this.fileExplorer.setOnFileRenamedListener((File oldFile, File newFile) -> {
            if (editorTabbedPane != null) {
                editorTabbedPane.handleFileRename(oldFile, newFile);
            }
        });

        this.fileExplorer.setOnFileDeletedListener((File file) -> {
            if (editorTabbedPane != null) {
                editorTabbedPane.closeFileTab(file);
            }
        });

        this.topNavigationTabbedPane.setMinimumSize(new Dimension(200, 100));
        this.topNavigationTabbedPane.add("Projects", projectExplorer);
        this.topNavigationTabbedPane.add("Files", fileExplorer);
        this.topNavigationTabbedPane.setMinimizeAction(e -> {
            if (this.navigationSplitPane.getDividerLocation() < 50) {
                this.navigationSplitPane.setDividerLocation(0.5);
            } else {
                if (this.navigationSplitPane
                        .getDividerLocation() >= this.navigationSplitPane.getMaximumDividerLocation() - 50) {
                    this.mainSplitPane.setDividerLocation(0.0);
                } else {
                    this.navigationSplitPane.setDividerLocation(0.0);
                }
            }
        });

        this.bottomNavigationTabbedPane.setMinimumSize(new Dimension(200, 100));
        this.bottomNavigationTabbedPane.add("File Outline", fileOutlinePanel);
        this.bottomNavigationTabbedPane.setMinimizeAction(e -> {
            if (this.navigationSplitPane.getDividerLocation() >= this.navigationSplitPane.getMaximumDividerLocation()
                    - 50) {
                this.navigationSplitPane.setDividerLocation(0.5);
            } else {
                if (this.navigationSplitPane.getDividerLocation() < 50) {
                    this.mainSplitPane.setDividerLocation(0.0);
                } else {
                    this.navigationSplitPane.setDividerLocation(1.0);
                }
            }
        });

        this.navigationSplitPane.setTopComponent(this.topNavigationTabbedPane);
        this.navigationSplitPane.setBottomComponent(this.bottomNavigationTabbedPane);

        this.mainSplitPane.setLeftComponent(this.navigationSplitPane);

        this.editorSplitPane.setBorder(lineBorder);
        this.editorSplitPane.setResizeWeight(AppConstants.EDITOR_SPLIT_RESIZE_WEIGHT);
        this.editorSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        this.editorSplitPane.setTopComponent(editorTabbedPane);

        this.logTextPane.setEditable(false);
        // this.terminalTextPane.setEditable(false);

        JScrollPane logScrollPane = new JScrollPane(logTextPane);
        JScrollPane terminalScrollPane = new JScrollPane(terminalTextPane);

        this.bottomEditorTabbedPane.add("Output", terminalScrollPane);
        this.bottomEditorTabbedPane.add("Problems", problemsPanel);
        this.bottomEditorTabbedPane.add("Logs", logScrollPane);

        this.editorSplitPane.setBottomComponent(bottomEditorTabbedPane);

        this.mainSplitPane.setRightComponent(this.editorSplitPane);

        this.editorSplitPane.setBottomComponent(bottomEditorTabbedPane);

        this.mainSplitPane.setRightComponent(this.editorSplitPane);

        this.bottomEditorTabbedPane.setMinimizeAction(e -> {
            if (this.editorSplitPane.getDividerLocation() >= this.editorSplitPane.getMaximumDividerLocation() - 50) {
                this.editorSplitPane.setDividerLocation(0.7);
            } else {
                this.editorSplitPane.setDividerLocation(1.0);
            }
        });

        // Enable double-click to reset dividers
        enableDividerDoubleClickListener(mainSplitPane, 0.25);
        enableDividerDoubleClickListener(navigationSplitPane, 0.5);
        enableDividerDoubleClickListener(editorSplitPane, 0.75);

        statusBar.addRightComponent(cursorWidget);
        statusBar.addSeparator();
        statusBar.addRightComponent(readOnlyWidget);
        statusBar.addSeparator();
        statusBar.addRightComponent(memoryWidget);

        editorTabbedPane.addChangeListener(e -> {
            updateStatusBarWidgets(cursorWidget, readOnlyWidget);
            updateFileOutline();
        });

        updateStatusBarWidgets(cursorWidget, readOnlyWidget);
        updateFileOutline();
    }

    private void updateFileOutline() {
        java.awt.Component selected = editorTabbedPane.getSelectedComponent();
        if (selected instanceof FileTabPane fileTab) {
            pseudopad.core.AST.ProgramNode ast = fileTab.getCachedAST();
            fileOutlinePanel.updateOutline(ast);
        } else {
            fileOutlinePanel.updateOutline(null);
        }
    }

    private void enableDividerDoubleClickListener(JSplitPane splitPane, double defaultLocation) {
        if (splitPane.getUI() instanceof javax.swing.plaf.basic.BasicSplitPaneUI ui) {
            java.awt.Container divider = ui.getDivider();
            divider.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        splitPane.setDividerLocation(defaultLocation);
                    }
                }
            });
        }
    }

    private void initComponents() {
        this.mainSplitPane = new JSplitPane();
        this.navigationSplitPane = new JSplitPane();
        this.editorSplitPane = new JSplitPane();
        this.topNavigationTabbedPane = new TabbedPane();
        this.bottomNavigationTabbedPane = new TabbedPane();
        this.fileExplorer = new FileExplorer();
        this.fileOutlinePanel = new pseudopad.editor.FileOutlinePanel();
        this.projectExplorer = new RecentProjectsPanel(mainFrame);
        this.editorTabbedPane = new EditorTabbedPane(mainFrame);
        this.bottomEditorTabbedPane = new TabbedPane();

        SimpleTerminalBackend backend = new SimpleTerminalBackend();
        backend.setCodeProvider(() -> {
            if (editorTabbedPane != null) {
                return editorTabbedPane.getActiveFileContent();
            }
            return null;
        });

        this.terminalTextPane = new TerminalPane(backend);
        this.logTextPane = new TextPane();

        this.statusBar = new StatusBar();

        this.cursorWidget = new CursorPositionWidget();
        this.readOnlyWidget = new ReadOnlyWidget();
        this.memoryWidget = new MemoryUsageWidget();

        this.problemsPanel = new pseudopad.editor.ProblemsPanel();
    }

    private void updateStatusBarWidgets(CursorPositionWidget cursorWidget, ReadOnlyWidget readOnlyWidget) {
        java.awt.Component selected = editorTabbedPane.getSelectedComponent();

        if (selected instanceof FileTabPane fileTab) {
            // Update Read-Only Status
            File file = fileTab.getFile();
            readOnlyWidget.setReadOnly(file != null && !file.canWrite());

            // Update Cursor Position
            TextPane textPane = fileTab.getTextPane();
            updateCursorPosition(textPane, cursorWidget);

            // Update Outline explicitly? Maybe via FileTabPane itself?
            // Actually FileTabPane will analyze and update when focused or changed.
            // But if we switch tabs, we might want to refresh the outline immediately with
            // cached AST.

            // Add Caret Listener
            for (javax.swing.event.CaretListener l : textPane.getCaretListeners()) {
                // Remove existing listeners to avoid duplicates (simplistic approach)
                // In real app, cleaner listener management is needed
                if (l instanceof javax.swing.event.CaretListener) {

                }
            }

            // Let's use a client property on the textPane to store the listener
            javax.swing.event.CaretListener existingListener = (javax.swing.event.CaretListener) textPane
                    .getClientProperty("StatusBarCaretListener");
            if (existingListener == null) {
                javax.swing.event.CaretListener listener = e -> updateCursorPosition(textPane, cursorWidget);
                textPane.addCaretListener(listener);
                textPane.putClientProperty("StatusBarCaretListener", listener);
            }

        } else

        {
            readOnlyWidget.setReadOnly(false);
            cursorWidget.updatePosition(1, 1);
        }
    }

    private void updateCursorPosition(TextPane textPane, CursorPositionWidget cursorWidget) {
        try {
            int caretPos = textPane.getCaretPosition();
            int line = getLineOfOffset(textPane, caretPos);
            int column = getColumnOfOffset(textPane, caretPos);
            cursorWidget.updatePosition(line, column);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private int getLineOfOffset(javax.swing.JTextPane textPane, int offset) throws BadLocationException {
        javax.swing.text.Element map = textPane.getDocument().getDefaultRootElement();
        return map.getElementIndex(offset) + 1;
    }

    private int getColumnOfOffset(javax.swing.JTextPane textPane, int offset) throws BadLocationException {
        javax.swing.text.Element map = textPane.getDocument().getDefaultRootElement();
        int line = map.getElementIndex(offset);
        javax.swing.text.Element lineElem = map.getElement(line);
        return offset - lineElem.getStartOffset() + 1;
    }

    public void appendLog(String message, Color color) {
        if (logTextPane != null) {
            StyledDocument doc = logTextPane.getStyledDocument();
            Style style = logTextPane.addStyle("LogStyle", null);
            StyleConstants.setForeground(style, color != null ? color : UIManager.getColor("Panel.foreground"));

            try {
                doc.insertString(doc.getLength(), message + "\n", style);
                SwingUtilities.invokeLater(() -> {
                    logTextPane.setCaretPosition(doc.getLength());
                });
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    public void resetLayout() {
        SwingUtilities.invokeLater(() -> {
            // Hide Navigation
            mainSplitPane.setDividerLocation(0.0);

            // Hide Output
            editorSplitPane.setDividerLocation(1.0);

            // Default Nav Split
            navigationSplitPane.setDividerLocation(0.5);
        });
    }

    // Getters
    public JSplitPane getMainSplitPane() {
        return mainSplitPane;
    }

    public JSplitPane getNavigationSplitPane() {
        return navigationSplitPane;
    }

    public JSplitPane getEditorSplitPane() {
        return editorSplitPane;
    }

    public TabbedPane getTopNavigationTabbedPane() {
        return topNavigationTabbedPane;
    }

    public FileExplorer getFileExplorer() {
        return fileExplorer;
    }

    public EditorTabbedPane getEditorTabbedPane() {
        return editorTabbedPane;
    }

    public TextPane getLogTextPane() {
        return logTextPane;
    }

    public StatusBar getStatusBar() {
        return statusBar;
    }

    public void runTerminalCommand(String command) {
        if (terminalTextPane != null) {
            terminalTextPane.runCommand(command);
        }
    }

    public void setTerminalProjectName(String projectName) {
        if (terminalTextPane != null) {
            terminalTextPane.setProjectName(projectName);
        }
    }

    public pseudopad.editor.ProblemsPanel getProblemsPanel() {
        return problemsPanel;
    }

    public pseudopad.editor.FileOutlinePanel getFileOutlinePanel() {
        return fileOutlinePanel;
    }

    public TabbedPane getBottomEditorTabbedPane() {
        return bottomEditorTabbedPane;
    }

    public RecentProjectsPanel getRecentProjectsPanel() {
        return projectExplorer;
    }
}
