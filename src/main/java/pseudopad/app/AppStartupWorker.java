package pseudopad.app;

import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

import pseudopad.ui.splash.SplashScreenWindow;
import pseudopad.ui.splash.StartupProgress;
import pseudopad.utils.IconManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class AppStartupWorker extends SwingWorker<Boolean, StartupProgress> {
    private final SplashScreenWindow splashScreen;
    private final MainFrame mainFrame;

    public AppStartupWorker(SplashScreenWindow splashScreen, MainFrame mainFrame) {
        this.splashScreen = splashScreen;
        this.mainFrame = mainFrame;
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        int currentStep = 0;
        int totalSteps = 5;

        // --- 1. Load Configuration & Theme ---
        currentStep++;
        publish(new StartupProgress(
                (currentStep * 100) / totalSteps, // 20%
                "Loading user preferences and theme..."));

        // Placeholder for real Preference/Theme loading
        // PreferencesManager prefsManager = new PreferencesManager();

        // Load theme before initializing UI components
        // themeManager.loadSavedTheme();

        // --- 2. Initialize FlatLaf & Fonts ---
        currentStep++;
        publish(new StartupProgress(
                (currentStep * 100) / totalSteps, // 20%
                "Initializing Look and Feel..."));

        // --- 3. Load/Cache Resources (e.g., icons, keywords) ---
        // This is where you would normally initialize your custom syntax highlighter
        // resources (keywords, operators, etc.) to cache them.
        currentStep++;
        publish(new StartupProgress(
                (currentStep * 100) / totalSteps, // 20%
                "Caching UI icons..."));
        IconManager.preloadCoreIcons();

        currentStep++;
        publish(new StartupProgress(
                (currentStep * 100) / totalSteps, // 20%
                "Loading example templates..."));
        // Example: check resource directory for example files
        // Thread.sleep(2500); // Simulate task time

        // --- 4. Final Application State Check ---
        currentStep++;
        publish(new StartupProgress(
                (currentStep * 100) / totalSteps, // 20%
                "Preparing main workspace..."));

        // Simulate a final brief delay for visual effect
        Thread.sleep(3000);

        return true; // Return success
    }

    @Override
    protected void process(List<StartupProgress> chunks) {
        // Update the status label on the splash screen
        StartupProgress latestProgress = chunks.get(chunks.size() - 1);
        splashScreen.updateProgress(latestProgress);
    }

    @Override
    protected void done() {
        try {
            // Check for successful completion
            if (get()) {
                // 1. Launch the Main Frame
                mainFrame.launchAppInstance(null);
            } else {
                // Handle case where startup failed (e.g., show an error dialog)
                System.err.println("Startup failed.");
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            // 2. Close the splash screen
            splashScreen.close();
        }
    }
}
