package pseudopad.app;

import java.io.File;
import javax.swing.SwingUtilities;
import pseudopad.config.ProjectConfig;
import pseudopad.utils.ProjectFileWatcher;
import pseudopad.utils.ProjectManager;

/**
 * Encapsulates the state and logic for a loaded project.
 */
public class ProjectContext {
    private File projectPath;
    private ProjectConfig config;
    private ProjectFileWatcher fileWatcher;
    private final MainFrame mainFrame;

    public ProjectContext(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public void loadProject(File path) {
        this.projectPath = path;
        this.config = ProjectManager.loadConfig(path);

        // Initialize Config if null
        if (this.config == null) {
            this.config = new ProjectConfig(path.getName());
        }

        startFileWatcher();
    }

    public void closeProject() {
        stopFileWatcher();
        this.projectPath = null;
        this.config = null;
    }

    private void startFileWatcher() {
        stopFileWatcher(); // Ensure existing watcher is stopped

        if (projectPath == null)
            return;

        fileWatcher = new ProjectFileWatcher(projectPath);
        fileWatcher.setFileChangeListener(new ProjectFileWatcher.FileChangeListener() {
            @Override
            public void onFileCreated(File file) {
                // Do nothing specific, refresh handles it
            }

            @Override
            public void onFileDeleted(File file) {
                SwingUtilities.invokeLater(() -> {
                    if (mainFrame.getEditorTabbedPane() != null) {
                        mainFrame.getEditorTabbedPane().closeFileTab(file);
                    }
                });
            }

            @Override
            public void onFileRenamed(File oldFile, File newFile) {
                SwingUtilities.invokeLater(() -> {
                    if (mainFrame.getEditorTabbedPane() != null) {
                        mainFrame.getEditorTabbedPane().handleFileRename(oldFile, newFile);
                    }
                });
            }
        });
        fileWatcher.start();
    }

    private void stopFileWatcher() {
        if (fileWatcher != null) {
            fileWatcher.stop();
            fileWatcher = null;
        }
    }

    public File getProjectPath() {
        return projectPath;
    }

    public ProjectConfig getConfig() {
        return config;
    }

    public void setConfig(ProjectConfig config) {
        this.config = config;
    }

    public void saveConfig() {
        if (projectPath != null && config != null) {
            ProjectManager.saveConfig(projectPath, config);
        }
    }
}
