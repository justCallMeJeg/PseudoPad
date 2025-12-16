package pseudopad.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class FileManager {
    // Prevent instantiation (Static Utility Class)
    private FileManager() {}

    // ==================================================================================
    //  READ OPERATIONS
    // ==================================================================================

    public static String readFile(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    // ==================================================================================
    //  WRITE / EDIT OPERATIONS
    // ==================================================================================

    public static void saveFile(File file, String content) throws IOException {
        validateParentDirectory(file);
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.TRUNCATE_EXISTING, 
                StandardOpenOption.WRITE);
    }

    public static void appendToFile(File file, String content) throws IOException {
        validateParentDirectory(file);
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.APPEND);
    }

    // ==================================================================================
    //  CREATION OPERATIONS
    // ==================================================================================

    public static void createFile(File file) throws IOException {
        validateParentDirectory(file);
        if (!file.exists()) {
            Files.createFile(file.toPath());
        }
    }

    public static void createDirectory(File dir) throws IOException {
        if (!dir.exists()) {
            Files.createDirectories(dir.toPath());
        }
    }

    // ==================================================================================
    //  MANAGEMENT OPERATIONS (Rename, Move, Delete)
    // ==================================================================================

    public static void rename(File source, File destination) throws IOException {
        Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void delete(File file) throws IOException {
        Files.deleteIfExists(file.toPath());
    }

    public static void deleteDirectoryForcefully(File directory) throws IOException {
        if (!directory.exists()) return;
        
        Path rootPath = directory.toPath();
        
        // Walk the tree in reverse order (deepest files first) to delete them before the parent
        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    public static void copy(File source, File dest) throws IOException {
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // ==================================================================================
    //  HELPERS
    // ==================================================================================

    private static void validateParentDirectory(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            Files.createDirectories(parent.toPath());
        }
    }
}
