package pseudopad.config;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class ProjectConfig {
    public String projectName;
    public String version = "1.0.0";
    public String mainFile = "main.pc"; // Default entry point
    // Add other settings like "author", "theme preference", etc.
    
    public ProjectConfig() {} // for JSON deserialization
    
    public ProjectConfig(String name) {
        this.projectName = name;
    }
}
