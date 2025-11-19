package pseudopad;

import pseudopad.app.MainFrame;
import pseudopad.services.ThemeManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Hello World!");
        
        ThemeManager.init();
        
        java.awt.EventQueue.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
