package pseudopad.ui.dialogs;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import pseudopad.app.AppConstants;
import pseudopad.app.MainFrame;
import pseudopad.project.ProjectManager;
import pseudopad.utils.PreferenceManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class OpenProjectDialog extends JFileChooser {

    public OpenProjectDialog() {
        this.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        this.setDialogTitle(AppConstants.DIALOG_TITLE_OPEN_PROJECT);

        this.setCurrentDirectory(PreferenceManager.getInstance().loadLastDialogDir());
    }

    @Override
    public void approveSelection() {
        File selectedFolder = getSelectedFile();

        // Save the parent directory for next time
        if (selectedFolder != null) {
            PreferenceManager.getInstance().saveLastDialogDir(selectedFolder.getParentFile());
        }

        // Checking if chosen project path is valid
        if (ProjectManager.isValidProject(selectedFolder)) {

            MainFrame.getInstance().openProject(selectedFolder);
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
                    // ProjectManager.initializeExisting(selectedFolder);
                    MainFrame.getInstance().openProject(selectedFolder);
                } catch (Exception e) {
                    // handle error
                }
            }
        }

        super.approveSelection();
    }
}
