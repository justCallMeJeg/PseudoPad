package pseudopad.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import pseudopad.app.MainFrame;
import pseudopad.ui.components.ImagePanel;
import pseudopad.utils.AppActionsManager;
import pseudopad.utils.ThemeManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class FallbackPanel extends JPanel {
    private final MainFrame appFrame;
    
    public FallbackPanel(MainFrame appFrame) {
        this.appFrame = appFrame;
        updateState();
        
        this.setLayout(new GridBagLayout());
        
        if (appFrame.getCurrentProjectPath() == null) {
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
        } else {
            this.add(new JLabel("Select a file to edit..."));
        }
    }
    
    public final void updateState() {
        this.removeAll(); // Clear current view
        
        if (appFrame.getCurrentProjectPath() == null) {
            // No Project -> Show Buttons
            initUIComponents(); 
        } else {
            // Active Project -> Show "Select File" instruction
            this.setLayout(new GridBagLayout()); // Center it
            this.add(new JLabel("Select a file to edit..."));
        }
        
        this.revalidate();
        this.repaint();
    }
    
    private JButton createButton(String title, Action action) {
        JButton button = new JButton();
        button.setLayout(new FlowLayout(FlowLayout.LEFT, 24, 8));
        
        button.addActionListener(action);
        
        JPanel textContainer = new JPanel();
        textContainer.setOpaque(false);
        textContainer.setLayout(new BoxLayout(textContainer, BoxLayout.PAGE_AXIS));
        
        JLabel titleLabel = new JLabel();
        titleLabel.setText(title);
        
        JLabel shortcutLabel = new JLabel();
        shortcutLabel.setForeground(UIManager.getColor("Component.accentColor"));
        
        if (action != null) {
            String actionShortcut = (String) action.getValue(Action.ACTION_COMMAND_KEY);
            shortcutLabel.setText(actionShortcut != null ? actionShortcut : "-");
        } else {
            shortcutLabel.setText("-");
        }
        
        textContainer.add(titleLabel);
        textContainer.add(shortcutLabel);
        
        button.add(textContainer);
        
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        
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
        
        AppActionsManager actionManager = appFrame.getAppActionInstance();
        
        gridPanel.add(createButton("New Project...", actionManager.NEW_PROJECT));
        gridPanel.add(createButton("Open Project...", actionManager.OPEN_PROJECT));
        gridPanel.add(createButton("Open File...", null));
        
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
