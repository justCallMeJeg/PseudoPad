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
import pseudopad.ui.components.AppMenuBar;
import pseudopad.ui.components.AppToolBar;
import pseudopad.ui.components.EditorTabbedPane;
import pseudopad.ui.components.FileExplorer;
import pseudopad.ui.components.TabbedPane;
import pseudopad.ui.components.TextPane;
import pseudopad.ui.components.terminal.SimpleTerminalBackend;
import pseudopad.ui.components.TerminalPane;
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
    private FileExplorer projectExplorer;
    private EditorTabbedPane editorTabbedPane;
    private TabbedPane bottomEditorTabbedPane;

    private TerminalPane terminalTextPane;
    private TextPane logTextPane;

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
        this.bottomNavigationTabbedPane.add("Navigation", new JPanel().add(new JLabel("File Navigation Panel")));
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
        this.projectExplorer = new FileExplorer();
        this.editorTabbedPane = new EditorTabbedPane(mainFrame);
        this.bottomEditorTabbedPane = new TabbedPane();
        this.terminalTextPane = new TerminalPane(new SimpleTerminalBackend());
        this.logTextPane = new TextPane();
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
}
