package pseudopad.project;

import com.google.gson.Gson;

import pseudopad.app.AppConstants;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class ProjectManager {
    private static final String CONFIG_FOLDER = AppConstants.CONFIG_DIR_NAME;
    private static final String CONFIG_FILE = AppConstants.CONFIG_FILE_NAME;
    private static final Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

    public static boolean isValidProject(File directory) {
        if (!directory.isDirectory()) {
            return false;
        }
        File configFile = new File(directory, CONFIG_FOLDER + File.separator + CONFIG_FILE);
        return configFile.exists();
    }

    public static void createProject(String name, File parentDir) throws IOException {
        File projectRoot = new File(parentDir, name);

        if (projectRoot.exists()) {
            throw new IOException("Directory already exists: " + projectRoot.getAbsolutePath());
        }

        // 1. Create Root
        if (!projectRoot.mkdirs()) {
            throw new IOException("Failed to create project directory.");
        }

        // 2. Create Config Folder (.pseudocode)
        File configDir = new File(projectRoot, CONFIG_FOLDER);
        if (!configDir.mkdir()) {
            throw new IOException("Failed to create config directory.");
        }

        // 3. Write Config File (project.json)
        ProjectConfig config = new ProjectConfig(name);
        try (FileWriter writer = new FileWriter(new File(configDir, CONFIG_FILE))) {
            gson.toJson(config, writer);
        }

        // 4. Create a default Main file (Optional but good UX)
        File mainFile = new File(projectRoot, "main.pc");
        try (FileWriter writer = new FileWriter(mainFile)) {
            writer.write("print(\"Hello World!\");");
        }
    }

    public static ProjectConfig loadConfig(File projectRoot) {
        File configFile = new File(projectRoot, CONFIG_FOLDER + File.separator + CONFIG_FILE);
        try (FileReader reader = new FileReader(configFile)) {
            return gson.fromJson(reader, ProjectConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveConfig(File projectRoot, ProjectConfig config) {
        File configFile = new File(projectRoot, CONFIG_FOLDER + File.separator + CONFIG_FILE);
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
