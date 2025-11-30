package pseudopad.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import pseudopad.app.MainFrame;
import pseudopad.utils.AppLogger;

/**
 * Monitors the project directory for changes and triggers a refresh in the File
 * Explorer.
 */
public class ProjectFileWatcher {

    public interface FileChangeListener {
        void onFileCreated(File file);

        void onFileDeleted(File file);

        void onFileRenamed(File oldFile, File newFile);
    }

    private final File projectRoot;
    private WatchService watchService;
    private Thread watcherThread;
    private volatile boolean running = false;
    private final Map<WatchKey, Path> keys = new HashMap<>();
    private FileChangeListener listener;

    // Debouncing
    private final ScheduledExecutorService debouncer = Executors.newSingleThreadScheduledExecutor();
    private volatile long lastChangeTime = 0;
    private static final long DEBOUNCE_DELAY_MS = 500;

    // Rename Heuristic State
    private final Set<File> pendingDeletes = new HashSet<>();

    public ProjectFileWatcher(File projectRoot) {
        this.projectRoot = projectRoot;
    }

    public void setFileChangeListener(FileChangeListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (running)
            return;
        running = true;

        try {
            watchService = FileSystems.getDefault().newWatchService();
            registerRecursive(projectRoot.toPath());

            watcherThread = new Thread(this::processEvents);
            watcherThread.setDaemon(true);
            watcherThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                // ignore
            }
        }
        debouncer.shutdownNow();
    }

    private void registerRecursive(final Path start) throws IOException {
        // Register all subdirectories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
        keys.put(key, dir);
    }

    private void processEvents() {
        while (running) {
            WatchKey key;
            try {
                // Wait for key to be signalled
                key = watchService.take();
            } catch (InterruptedException | java.nio.file.ClosedWatchServiceException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                continue;
            }

            // We process all events for this key
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);
                File childFile = child.toFile();

                if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    pendingDeletes.add(childFile);
                } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    // Check for rename (heuristic: delete + create in same directory usually,
                    // but standard rename might be delete old + create new)
                    // Simple heuristic: if we have a pending delete in the same directory, treat as
                    // rename
                    // NOTE: This is simplistic. A better approach tracks time or checks for exact
                    // swap.
                    // But for now, let's check if there is ANY pending delete in the same parent
                    // dir.

                    File renamedFrom = null;
                    for (File deleted : pendingDeletes) {
                        if (deleted.getParentFile().equals(childFile.getParentFile())) {
                            renamedFrom = deleted;
                            break;
                        }
                    }

                    if (renamedFrom != null) {
                        pendingDeletes.remove(renamedFrom);
                        if (listener != null) {
                            listener.onFileRenamed(renamedFrom, childFile);
                        }
                        AppLogger.info(
                                "External rename detected: " + renamedFrom.getName() + " -> " + childFile.getName());
                    } else {
                        if (listener != null) {
                            listener.onFileCreated(childFile);
                        }
                        AppLogger.info("External file created: " + childFile.getName());
                    }

                    // Register new directory
                    try {
                        if (Files.isDirectory(child)) {
                            registerRecursive(child);
                        }
                    } catch (IOException x) {
                        // ignore
                    }
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    // Modification
                }

                triggerRefresh();
            }

            // Process remaining deletes that weren't matched to a create (i.e. actual
            // deletes)
            // Note: This logic is tricky because events come in batches.
            // Ideally we should wait a tiny bit to see if a Create follows.
            // But for simplicity, we'll assume if it's in the same batch, we catch it.
            // If it's a split batch, we might miss the rename.
            // For V1, we just clear pending deletes and notify.
            for (File deleted : pendingDeletes) {
                if (listener != null) {
                    listener.onFileDeleted(deleted);
                }
                AppLogger.info("External file deleted: " + deleted.getName());
            }
            pendingDeletes.clear();

            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    private void triggerRefresh() {
        // Debounce logic
        lastChangeTime = System.currentTimeMillis();
        debouncer.schedule(() -> {
            if (System.currentTimeMillis() - lastChangeTime >= DEBOUNCE_DELAY_MS) {
                SwingUtilities.invokeLater(() -> {
                    if (MainFrame.getInstance() != null) {
                        MainFrame.getInstance().refreshFileExplorer();
                    }
                });
            }
        }, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }
}
