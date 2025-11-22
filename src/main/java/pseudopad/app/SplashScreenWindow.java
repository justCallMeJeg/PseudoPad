package pseudopad.app;

import javax.swing.JWindow;
import pseudopad.ui.SplashScreenPanel;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class SplashScreenWindow extends JWindow{
    private final SplashScreenPanel splashScreenPanel;
    
    public SplashScreenWindow() {        
        this.splashScreenPanel = new SplashScreenPanel();
       
        this.add(this.splashScreenPanel);
        this.pack();
        this.setAlwaysOnTop(true);
        this.setLocationRelativeTo(null);
    }
    
    public void updateProgress(StartupProgress progress) {
        splashScreenPanel.setDebugText(progress.getMessage());
        splashScreenPanel.setProgressValue(progress.getProgress());
    }
    
    public void close() {
        this.dispose();
    }
}
