package pseudopad.utils;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the FileManager utility class.
 * Tests file operations including read, write, create, and delete.
 */
class FileManagerTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("pseudopad-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up temp directory
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a)) // Reverse order for deletion
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    // ==================================================================================
    // HELPER METHODS
    // ==================================================================================

    private File tempFile(String name) {
        return tempDir.resolve(name).toFile();
    }

    // ==================================================================================
    // READ OPERATIONS
    // ==================================================================================

    @Nested
    @DisplayName("Read Operations")
    class ReadOperations {
        @Test
        @DisplayName("Should read existing file")
        void readExistingFile() throws IOException {
            File file = tempFile("test.txt");
            Files.writeString(file.toPath(), "Hello World");

            String content = FileManager.readFile(file);
            assertEquals("Hello World", content);
        }

        @Test
        @DisplayName("Should read UTF-8 content")
        void readUtf8Content() throws IOException {
            File file = tempFile("utf8.txt");
            String utf8Content = "Hello 世界 🌍";
            Files.writeString(file.toPath(), utf8Content);

            String content = FileManager.readFile(file);
            assertEquals(utf8Content, content);
        }

        @Test
        @DisplayName("Should throw on non-existent file")
        void throwOnNonExistentFile() {
            File nonExistent = tempFile("does-not-exist.txt");
            assertThrows(IOException.class, () -> FileManager.readFile(nonExistent));
        }
    }

    // ==================================================================================
    // WRITE OPERATIONS
    // ==================================================================================

    @Nested
    @DisplayName("Write Operations")
    class WriteOperations {
        @Test
        @DisplayName("Should save file")
        void saveFile() throws IOException {
            File file = tempFile("output.txt");
            FileManager.saveFile(file, "Test content");

            assertTrue(file.exists());
            assertEquals("Test content", Files.readString(file.toPath()));
        }

        @Test
        @DisplayName("Should overwrite existing file")
        void overwriteExistingFile() throws IOException {
            File file = tempFile("overwrite.txt");
            FileManager.saveFile(file, "Original");
            FileManager.saveFile(file, "Updated");

            assertEquals("Updated", Files.readString(file.toPath()));
        }

        @Test
        @DisplayName("Should append to file")
        void appendToFile() throws IOException {
            File file = tempFile("append.txt");
            FileManager.saveFile(file, "First");
            FileManager.appendToFile(file, "Second");

            assertEquals("FirstSecond", Files.readString(file.toPath()));
        }

        @Test
        @DisplayName("Should create parent directories")
        void createParentDirectories() throws IOException {
            File file = tempDir.resolve("subdir/nested/file.txt").toFile();
            FileManager.saveFile(file, "Nested content");

            assertTrue(file.exists());
        }
    }

    // ==================================================================================
    // CREATION OPERATIONS
    // ==================================================================================

    @Nested
    @DisplayName("Creation Operations")
    class CreationOperations {
        @Test
        @DisplayName("Should create file")
        void createFile() throws IOException {
            File file = tempFile("newfile.txt");
            assertFalse(file.exists());

            FileManager.createFile(file);
            assertTrue(file.exists());
        }

        @Test
        @DisplayName("Should not fail on existing file")
        void notFailOnExistingFile() throws IOException {
            File file = tempFile("existing.txt");
            Files.createFile(file.toPath());

            assertDoesNotThrow(() -> FileManager.createFile(file));
        }

        @Test
        @DisplayName("Should create directory")
        void createDirectory() throws IOException {
            File dir = tempDir.resolve("newdir").toFile();
            assertFalse(dir.exists());

            FileManager.createDirectory(dir);
            assertTrue(dir.exists());
            assertTrue(dir.isDirectory());
        }

        @Test
        @DisplayName("Should create nested directories")
        void createNestedDirectories() throws IOException {
            File dir = tempDir.resolve("a/b/c").toFile();

            FileManager.createDirectory(dir);
            assertTrue(dir.exists());
        }
    }

    // ==================================================================================
    // MANAGEMENT OPERATIONS
    // ==================================================================================

    @Nested
    @DisplayName("Management Operations")
    class ManagementOperations {
        @Test
        @DisplayName("Should rename file")
        void renameFile() throws IOException {
            File source = tempFile("source.txt");
            File dest = tempFile("dest.txt");
            Files.writeString(source.toPath(), "Content");

            FileManager.rename(source, dest);

            assertFalse(source.exists());
            assertTrue(dest.exists());
            assertEquals("Content", Files.readString(dest.toPath()));
        }

        @Test
        @DisplayName("Should delete file")
        void deleteFile() throws IOException {
            File file = tempFile("todelete.txt");
            Files.createFile(file.toPath());
            assertTrue(file.exists());

            FileManager.delete(file);
            assertFalse(file.exists());
        }

        @Test
        @DisplayName("Should not fail when deleting non-existent file")
        void deleteNonExistent() {
            File file = tempFile("nonexistent.txt");
            assertDoesNotThrow(() -> FileManager.delete(file));
        }

        @Test
        @DisplayName("Should copy file")
        void copyFile() throws IOException {
            File source = tempFile("source.txt");
            File dest = tempFile("copy.txt");
            Files.writeString(source.toPath(), "Original content");

            FileManager.copy(source, dest);

            assertTrue(source.exists());
            assertTrue(dest.exists());
            assertEquals("Original content", Files.readString(dest.toPath()));
        }

        @Test
        @DisplayName("Should delete directory forcefully")
        void deleteDirectoryForcefully() throws IOException {
            File dir = tempDir.resolve("toremove").toFile();
            File subFile = tempDir.resolve("toremove/file.txt").toFile();
            Files.createDirectories(dir.toPath());
            Files.createFile(subFile.toPath());

            FileManager.deleteDirectoryForcefully(dir);

            assertFalse(dir.exists());
        }
    }
}
