package pseudopad;

import java.io.File;

import javax.swing.SwingUtilities;

import pseudopad.app.AppStartupWorker;
import pseudopad.app.MainFrame;
import pseudopad.app.SplashScreenWindow;
import pseudopad.utils.CrashHandler;
import pseudopad.utils.PreferenceManager;
import pseudopad.utils.ThemeManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class Main {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());

        SwingUtilities.invokeLater(() -> {
            ThemeManager.init();

            // 1. Create and Show the Splash Screen (A JWindow)
            SplashScreenWindow splashScreen = new SplashScreenWindow();
            // We set the splash visible immediately for instant feedback
            splashScreen.setVisible(true);

            // 2. Create the Main Frame (Invisible initially, but initialized)
            MainFrame mainFrame = new MainFrame();

            mainFrame.setupAppIcon(ThemeManager.getInstance().isDarkMode());

            // 3. Execute the Worker
            // The worker takes charge of the timeline from this point on.
            AppStartupWorker worker = new AppStartupWorker(splashScreen, mainFrame);

            // Check for last project
            File lastProject = PreferenceManager.getInstance().loadLastProject();
            if (lastProject != null) {
                mainFrame.setPendingProject(lastProject);
            }

            worker.execute();
        });
    }
}
