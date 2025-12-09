package pseudopad.project;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class ProjectConfig {
    public String projectName;
    public String version = "1.0.0";
    public String mainFile = "main.pc"; // Default entry point

    // Window Management
    public int windowWidth = 1280;
    public int windowHeight = 720;
    public int windowX = -1; // -1 means let OS decide or center
    public int windowY = -1;
    public int windowState = 0; // Frame.NORMAL

    // Layout Management (Split Panes)
    public int dividerMain = -1;
    public int dividerEditor = -1;
    public int dividerNav = -1;

    // Session State
    public java.util.List<String> openFiles = new java.util.ArrayList<>();
    public String activeFile;

    public ProjectConfig() {
    } // for JSON deserialization

    public ProjectConfig(String name) {
        this.projectName = name;
    }
}
