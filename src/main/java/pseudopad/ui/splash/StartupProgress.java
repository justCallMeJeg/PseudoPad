package pseudopad.ui.splash;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class StartupProgress {
    private final int progress;
    private final String message;

    public StartupProgress(int progress, String message) {
        this.progress = progress;
        this.message = message;
    }

    public int getProgress() {
        return progress;
    }

    public String getMessage() {
        return message;
    }
}
