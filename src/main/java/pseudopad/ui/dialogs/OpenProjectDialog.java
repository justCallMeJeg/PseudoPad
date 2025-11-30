package pseudopad.ui.dialogs;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import pseudopad.app.MainFrame;
import pseudopad.utils.ProjectManager;
import pseudopad.utils.ThemeManager;
import pseudopad.utils.AppConstants;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class OpenProjectDialog extends JFileChooser {
    private final ThemeManager themeManager = ThemeManager.getInstance();

    public OpenProjectDialog() {
        this.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        this.setDialogTitle(AppConstants.DIALOG_TITLE_OPEN_PROJECT);

        this.setCurrentDirectory(new File(System.getProperty("user.home")));
    }

    @Override
    public void approveSelection() {
        File selectedFolder = getSelectedFile();

        // Checking if chosen project path is valid
        if (ProjectManager.isValidProject(selectedFolder)) {
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

        super.approveSelection();
    }
}
