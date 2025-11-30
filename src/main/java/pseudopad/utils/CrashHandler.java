package pseudopad.utils;

import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import pseudopad.app.MainFrame;

/**
 * Global exception handler to catch crashes and report them to the user.
 * 
 * @author Geger John Paul Gabayeron
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        // 1. Log the error
        AppLogger.error("Uncaught Exception in thread '" + t.getName() + "'", e);

        // 2. Show Error Dialog
        SwingUtilities.invokeLater(() -> {
            Component parent = MainFrame.getInstance();

            String message = "An unexpected error occurred:\n" + e.getMessage() +
                    "\n\nCheck the Logs tab for details.";

            JOptionPane.showMessageDialog(parent, message, "Application Error", JOptionPane.ERROR_MESSAGE);
        });
    }
}
