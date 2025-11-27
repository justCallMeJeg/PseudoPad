package pseudopad.app;

import javax.swing.border.Border;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Taskbar;
import java.io.File;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import pseudopad.ui.components.AppMenuBar;
import pseudopad.ui.components.AppToolBar;
import pseudopad.ui.components.EditorTabbedPane;
import pseudopad.ui.components.FileExplorer;
import pseudopad.ui.components.TabbedPane;
import pseudopad.ui.components.TextPane;
import pseudopad.utils.AppActionsManager;
import pseudopad.utils.FileManager;
import pseudopad.utils.PreferenceManager;
import pseudopad.utils.ProjectManager;
import pseudopad.utils.ThemeManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class MainFrame extends JFrame {
    private static MainFrame INSTANCE;
    
    private File currentProjectPath;
    private final ThemeManager themeManager = ThemeManager.getInstance();
    private final PreferenceManager AppPref = PreferenceManager.getInstance();
    private final AppActionsManager AppActions = new AppActionsManager(this);
    
    public MainFrame() {
        INSTANCE = this;
        initUIComponents();
        
        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName())) {
               Boolean isDark = ThemeManager.getInstance().isDarkMode();
               setupAppIcon(isDark);
            }
        });
    }
    
    public void setupAppIcon(Boolean isDark) {
        Image icon = new ImageIcon(MainFrame.class.getResource(isDark ? "/img/icon_dark.png" : "/img/icon.png")).getImage();
        
        // macOS app icon setup
        // Check if the Taskbar API is supported on the current OS
        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            try {
                // Set the icon for the application's global taskbar/dock icon
                taskbar.setIconImage(icon);
            } catch (UnsupportedOperationException e) {
                System.err.println("The taskbar setIconImage feature is not supported on this platform.");
            }
        }
        
        // Windows/Linux app icon setup
        setIconImage(icon);
    }
    
    public void launchAppInstance(File projectPath) {
        this.currentProjectPath = projectPath;
        
        // 1. Show the window FIRST so components get their sizes
        this.setVisible(true);
        this.toFront();
        this.requestFocus();
        
        // 2. Configure components
        if (currentProjectPath != null) {
            this.setTitle(this.currentProjectPath.getName() + " - PseudoPad v0");
            this.fileExplorer.openProject(projectPath);
//            this.editorSplitPane.setTopComponent(editorTabbedPane);
        } else {
            this.setTitle("PseudoPad v0");
//            this.editorSplitPane.setTopComponent(new FallbackPanel(INSTANCE));
        }
        
        if (this.editorTabbedPane != null) {
            this.editorTabbedPane.refreshFallbackState();
        }

        // 3. Set Divider Locations LAST, inside invokeLater
        // This places the request at the end of the Event Queue, ensuring
        // the window is fully drawn before the dividers try to move.
        SwingUtilities.invokeLater(() -> {
            // Set Main Split (Navigation vs Editor)
            this.mainSplitPane.setDividerLocation(0.25); // e.g. 25% for file tree
            
            // Set Editor Split (Editor vs Console)
            if (currentProjectPath != null) {
                this.editorSplitPane.setDividerLocation(0.75);
            } else {
                this.editorSplitPane.setDividerLocation(1.0);
            }
            
            // Set Navigation Split (Files vs Outline)
            this.navigationSplitPane.setDividerLocation(0.5);
        });
    }
    
    public void showOpenProjectDialog() {
        JFileChooser chooser = new JFileChooser();

        // 1. CRITICAL: Allow selecting DIRECTORIES only
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Open Project Folder");

        // Optional: Start in a sensible location (like Documents)
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = chooser.getSelectedFile();

            // THE NEW CHECK
            if (ProjectManager.isValidProject(selectedFolder)) {
                // It has the .pseudocode folder -> Open it!
                MainFrame newWindow = new MainFrame();
                newWindow.setupAppIcon(themeManager.isDarkMode());
                newWindow.launchAppInstance(selectedFolder);
            } else {
                // It's just a random folder -> Error or Prompt to Initialize
                int choice = JOptionPane.showConfirmDialog(this,
                        "This folder is not a PseudoPad project.\nDo you want to initialize it?",
                        "Project Not Found",
                        JOptionPane.YES_NO_OPTION);

                if (choice == JOptionPane.YES_OPTION) {
                    // Call init logic (maybe assume folder name is project name)
                    try {
                        // Note: This helper needs to support 'initializing existing dir'
                        // ProjectManager.initializeExisting(selectedFolder); 
                        MainFrame newWindow = new MainFrame();
                        newWindow.setupAppIcon(themeManager.isDarkMode());
                        newWindow.launchAppInstance(selectedFolder);
                    } catch (Exception e) {
                        // handle error
                    }
                }
            }
        }
    }
    
    public void closeProject() {
        this.currentProjectPath = null;
        this.setTitle("PseudoPad v0");
        
        // 1. Clear the File Explorer
        // (Assuming FileExplorer has a clear() method)
        this.fileExplorer.clear(); 
        
        // 2. Close all open tabs
        this.editorTabbedPane.removeAll(); 
        
        // 3. Force Fallback Panel to update (It's now visible because we removed all tabs)
        this.editorTabbedPane.refreshFallbackState();
    }
    
    public void changeTheme(ThemeManager.THEMES theme) {
        ThemeManager.getInstance().changeTheme(theme);
    }
    
    public File getCurrentProjectPath() {
        return currentProjectPath;
    }
    
    public MainFrame getAppInstance() {
        return INSTANCE;
    }
    
    public AppActionsManager getAppActionInstance() {
        return this.AppActions;
    }
    
    private void initUIComponents() {
        initComponents();
        
        Border lineBorder = BorderFactory.createLineBorder(UIManager.getColor("Panel.foreground").darker(), 1);
        
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setMinimumSize(new Dimension(900, 506));
        
        this.setJMenuBar(menuBar);
        
        AppToolBar toolBar = new AppToolBar(this.AppActions);
        this.getContentPane().add(toolBar, BorderLayout.NORTH);
        
        this.mainSplitPane.setResizeWeight(0.0);
        this.mainSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        this.getContentPane().add(mainSplitPane, BorderLayout.CENTER);
        
        this.navigationSplitPane.setBorder(lineBorder);
        this.navigationSplitPane.setResizeWeight(0.5);
        this.navigationSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        
        this.fileExplorer.setOnFileOpenListener((File file) -> {
            try {
                // This calls your tabbed pane to open the file
                // You might need to read the file content using FileOperation logic here
                String content = FileManager.readFile(file); // Implement this helper
                editorTabbedPane.openFileTab(file.getName(), content);
            } catch (IOException ex) {
                System.err.println("Failed to open file: " + ex);
            }
        });
        
        this.topNavigationTabbedPane.setMinimumSize(new Dimension(200, 100));
        this.topNavigationTabbedPane.add("Projects", projectExplorer);
        this.topNavigationTabbedPane.add("Files", fileExplorer);
        this.topNavigationTabbedPane.setMinimizeAction(e -> {
            // Toggle Logic:
            if (this.navigationSplitPane.getDividerLocation() >= this.editorSplitPane.getMaximumDividerLocation() - 50) {
                // Restore (If explicitly closed) -> Set to 70%
                this.navigationSplitPane.setDividerLocation(0.5);
            } else {
                // Minimize -> Set to bottom (1.0 means 100%)
                this.navigationSplitPane.setDividerLocation(0);
            }
        });
        
        this.bottomNavigationTabbedPane.setMinimumSize(new Dimension(200, 100));
        this.bottomNavigationTabbedPane.add("Navigation", new JPanel().add(new JLabel("File Navigation Panel")));
        this.bottomNavigationTabbedPane.setMinimizeAction(e -> {
            // Toggle Logic:
            if (this.navigationSplitPane.getDividerLocation() >= this.editorSplitPane.getMaximumDividerLocation() - 50) {
                // Restore (If explicitly closed) -> Set to 70%
                this.navigationSplitPane.setDividerLocation(0.5);
            } else {
                // Minimize -> Set to bottom (1.0 means 100%)
                this.navigationSplitPane.setDividerLocation(1.0);
            }
        });
        
        this.navigationSplitPane.setTopComponent(this.topNavigationTabbedPane);
        this.navigationSplitPane.setBottomComponent(this.bottomNavigationTabbedPane);
        
        this.mainSplitPane.setLeftComponent(this.navigationSplitPane);
        
        this.editorSplitPane.setBorder(lineBorder);
        this.editorSplitPane.setResizeWeight(1.0);
        this.editorSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        this.editorSplitPane.setTopComponent(editorTabbedPane);
        
        this.bottomEditorTabbedPane.add("Output", terminalTextPane);
        this.bottomEditorTabbedPane.add("Logs", logTextPane);
        
        this.editorSplitPane.setBottomComponent(bottomEditorTabbedPane);
        
        this.mainSplitPane.setRightComponent(this.editorSplitPane);
        
        this.bottomEditorTabbedPane.setMinimizeAction(e -> {
            // Toggle Logic:
            if (this.editorSplitPane.getDividerLocation() >= this.editorSplitPane.getMaximumDividerLocation() - 50) {
                // Restore (If explicitly closed) -> Set to 70%
                this.editorSplitPane.setDividerLocation(0.7);
            } else {
                // Minimize -> Set to bottom (1.0 means 100%)
                this.editorSplitPane.setDividerLocation(1.0);
            }
        });
    }
    
    private void initComponents() {
        this.menuBar = new AppMenuBar(this.AppActions);
        this.mainSplitPane = new JSplitPane();
        this.navigationSplitPane = new JSplitPane();
        this.editorSplitPane = new JSplitPane();
        this.topNavigationTabbedPane = new TabbedPane();
        this.bottomNavigationTabbedPane = new TabbedPane();
        this.fileExplorer = new FileExplorer();
        this.projectExplorer = new FileExplorer();
        this.editorTabbedPane = new EditorTabbedPane(this);
        this.bottomEditorTabbedPane = new TabbedPane();
        this.terminalTextPane = new TextPane();
        this.logTextPane = new TextPane();
    }
    
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
    
    private TextPane terminalTextPane;
    private TextPane logTextPane;
}
