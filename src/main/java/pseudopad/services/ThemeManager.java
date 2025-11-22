package pseudopad.services;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Font;
import javax.swing.UIManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class ThemeManager {
    public static void init() {
        try {
            FlatLightLaf.setup();
            
            // Design System Variables
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
