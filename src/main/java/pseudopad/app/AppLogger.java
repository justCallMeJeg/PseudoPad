package pseudopad.app;

import java.awt.Color;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.SwingUtilities;

/**
 * Utility for logging messages to the application's log pane.
 * 
 * @author Geger John Paul Gabayeron
 */
public class AppLogger {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void info(String message) {
        log("INFO", message, null, null);
    }

    public static void warn(String message) {
        log("WARN", message, null, new Color(255, 140, 0)); // Orange
    }

    public static void error(String message) {
        log("ERROR", message, null, Color.RED);
    }

    public static void error(String message, Throwable t) {
        log("ERROR", message, t, Color.RED);
    }

    private static void log(String level, String message, Throwable t, Color color) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = MainFrame.getInstance();
            if (mainFrame != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("[").append(LocalDateTime.now().format(TIME_FORMATTER)).append("] ");
                sb.append("[").append(level).append("] ");
                sb.append(message);

                if (t != null) {
                    sb.append("\n");
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    t.printStackTrace(pw);
                    sb.append(sw.toString());
                }

                mainFrame.appendLog(sb.toString(), color);
            } else {
                // Fallback to stderr if UI not ready
                System.err.println("[" + level + "] " + message);
                if (t != null)
                    t.printStackTrace();
            }
        });
    }
}
