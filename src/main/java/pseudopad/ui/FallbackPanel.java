package pseudopad.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import pseudopad.ui.components.ImagePanel;
import pseudopad.utils.ThemeManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class FallbackPanel extends JPanel {    
    public FallbackPanel() {
        this.setLayout(new GridBagLayout());
        initUIComponents();
        
        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName())) {
               if (ThemeManager.getInstance().isDarkMode()) {
                    brandImage.setImageResourcePath("/img/PseudoPad_Logomark_Dark.png");
                } else {
                    brandImage.setImageResourcePath("/img/PseudoPad_Logomark.png");
                }
            }
        });
    }
    
    private JPanel createButton(String title, String shortcut) {
        JPanel button = new JPanel();
        button.setLayout(new FlowLayout(FlowLayout.LEFT, 24, 8));
        
        JPanel textContainer = new JPanel();
        textContainer.setLayout(new BoxLayout(textContainer, BoxLayout.PAGE_AXIS));
        
        JLabel titleLabel = new JLabel();
        titleLabel.setText(title);
        
        JLabel shortcutLabel = new JLabel();
        shortcutLabel.setForeground(UIManager.getColor("Component.accentColor"));
        shortcutLabel.setText(shortcut);
        
        textContainer.add(titleLabel);
        textContainer.add(shortcutLabel);
        
        button.add(textContainer);
        
        return button;
    }
    
    private void initUIComponents() {
        initComponents();
        
        if (ThemeManager.getInstance().isDarkMode()) {
            brandImage.setImageResourcePath("/img/PseudoPad_Logomark_Dark.png");
        } else {
            brandImage.setImageResourcePath("/img/PseudoPad_Logomark.png");
        }
        
        brandImage.setPreferredSize(new Dimension(150, 150));
        
        body.add(brandImage, BorderLayout.WEST);
        
        gridPanel.setLayout(new GridLayout(3, 0));
        
        gridPanel.add(createButton("New Project...", "Ctrl + Shift + N"));
        gridPanel.add(createButton("Open Project...", "Ctrl + Shift + O"));
        gridPanel.add(createButton("Open File...", "-"));
        
        body.add(gridPanel, BorderLayout.EAST);
        body.setMinimumSize(new Dimension(350, 250));
        
        this.add(body);
    }
    
    private void initComponents() {
        this.body = new JPanel();
        this.brandImage = new ImagePanel();
        this.gridPanel = new JPanel();
    }
    
    private JPanel body;
    private ImagePanel brandImage;
    private JPanel gridPanel;
}
