package pseudopad.services;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Font;
import javax.swing.UIManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class ThemeManager {
    public static void init() {
        try {
            FlatDarkLaf.setup();
            
            // Design System Variables
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
