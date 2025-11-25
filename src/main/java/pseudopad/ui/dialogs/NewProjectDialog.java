package pseudopad.ui.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import pseudopad.app.MainFrame;
import pseudopad.utils.ProjectManager;

/**
 * 
 * 
 * @author Geger John Paul Gabayeron
 */
public class NewProjectDialog extends JDialog {
    
    private final MainFrame parentFrame;
    
    public NewProjectDialog(MainFrame parent) {
        super(parent, "New Project", true); // Modal
        this.parentFrame = parent;
        
        initUIComponents();
        pack();
        
        // Set a reasonable minimum size
        setMinimumSize(new Dimension(600, 0));
        
        // Default values
        nameField.setText("PseudoProject");
        locationField.setText(System.getProperty("user.home") + File.separator + "Documents");
        updateCreatedFolder(); // Calc initial path
    }
    
    private void showFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(new File(locationField.getText()));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            locationField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void updateCreatedFolder() {
        String parent = locationField.getText().trim();
        String name = nameField.getText().trim();
        if (!parent.isEmpty() && !name.isEmpty()) {
            createdFolderField.setText(parent + File.separator + name);
        } else {
            createdFolderField.setText("");
        }
    }
    
    private void validateForm() {
        String name = nameField.getText().trim();
        String location = locationField.getText().trim();
        
        if (name.isEmpty()) {
            setError("Project Name is required.");
            return;
        }
        
        if (!name.matches("[a-zA-Z0-9_\\-\\s]+")) {
            setError("Invalid characters in Project Name.");
            return;
        }
        
        File parentDir = new File(location);
        if (!parentDir.exists() || !parentDir.isDirectory()) {
            setError("Project Location does not exist.");
            return;
        }
        
        File projectDir = new File(parentDir, name);
        if (projectDir.exists()) {
            setError("Project folder already exists!");
            return;
        }
        
        // Valid
        setError(null);
    }
    
    private void setError(String msg) {
        if (msg == null) {
            errorLabel.setText(" ");
            finishButton.setEnabled(true);
        } else {
            errorLabel.setText(msg);
            finishButton.setEnabled(false);
        }
    }
    
    private void createProject() {
        String name = nameField.getText().trim();
        File location = new File(locationField.getText().trim());
        
        try {
            ProjectManager.createProject(name, location);
            
            dispose();
            
            File newProjectRoot = new File(location, name);
            MainFrame newFrame = new MainFrame();
            newFrame.launchAppInstance(newProjectRoot);
            
            // Close the current "Welcome" window if it was empty and show the chosen project instead
            if (parentFrame.getAppInstance() != null && parentFrame.getCurrentProjectPath() == null) {
                parentFrame.dispose();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, 
                "Failed to create project:\n" + ex.getMessage(), 
                "Creation Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void initUIComponents() {
        unitComponents();
        
        setLayout(new BorderLayout());
        
        // --- 1. Header (Title Area) ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        
        JLabel titleLabel = new JLabel("Project Setup");
        titleLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2");
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // --- 2. Content (Form) ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Row 1: Project Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(new JLabel("Project Name:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(nameField, gbc);
        
        // Row 2: Project Location
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Project Location:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(locationField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0;
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> showFileChooser());
        formPanel.add(browseButton, gbc);
        
        // Row 3: Created Folder (Read Only)
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Created Folder:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0;
        createdFolderField.setEditable(false);
        createdFolderField.putClientProperty(FlatClientProperties.STYLE_CLASS, "monospaced");
        formPanel.add(createdFolderField, gbc);
        
        add(formPanel, BorderLayout.CENTER);
        
        // --- 3. Footer (Buttons & Errors) ---
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        
        // Error Message
        errorLabel.setForeground(Color.RED);
        footerPanel.add(errorLabel, BorderLayout.WEST);
        
        // Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        
        parentFrame.getRootPane().setDefaultButton(finishButton);
        
        cancelButton.addActionListener(e -> dispose());
        finishButton.addActionListener(e -> createProject());
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(finishButton);
        footerPanel.add(buttonPanel, BorderLayout.EAST);
        
        add(footerPanel, BorderLayout.SOUTH);
        
        // --- Listeners for Real-time Validation ---
        DocumentListener validationListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { validateForm(); updateCreatedFolder(); }
            @Override
            public void removeUpdate(DocumentEvent e) { validateForm(); updateCreatedFolder(); }
            @Override
            public void changedUpdate(DocumentEvent e) { validateForm(); updateCreatedFolder(); }
        };
        
        nameField.getDocument().addDocumentListener(validationListener);
        locationField.getDocument().addDocumentListener(validationListener);
    }
    
    private void unitComponents() {
        nameField = new JTextField();
        locationField = new JTextField();
        createdFolderField = new JTextField();
        finishButton = new JButton("Finish");
        errorLabel = new JLabel();
    }
    
    private JTextField nameField;
    private JTextField locationField;
    private JTextField createdFolderField;
    private JButton finishButton;
    private JLabel errorLabel;
}
