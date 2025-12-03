package pseudopad.ui.components;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.Timer;

/**
 * Status bar widget to display current memory usage.
 * Updates every 2 seconds. Clicking it triggers garbage collection.
 * 
 * @author Geger John Paul Gabayeron
 */
public class MemoryUsageWidget extends JLabel {
    private final Timer timer;

    public MemoryUsageWidget() {
        super("Memory: ...");
        setToolTipText("Click to run Garbage Collector");

        // Update every 2 seconds
        timer = new Timer(2000, e -> updateMemoryUsage());
        timer.start();

        // Initial update
        updateMemoryUsage();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.gc();
                updateMemoryUsage();
            }
        });
    }

    private void updateMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        long usedMB = usedMemory / (1024 * 1024);
        long totalMB = totalMemory / (1024 * 1024);

        setText(String.format("Memory: %d of %d MB", usedMB, totalMB));
    }
}
