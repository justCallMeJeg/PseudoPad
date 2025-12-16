package pseudopad.ui.settings;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import pseudopad.settings.SettingsCategory;
import pseudopad.settings.SettingsKey;
import pseudopad.settings.SettingsManager;
import pseudopad.utils.I18nManager;
import pseudopad.utils.ThemeManager;

/**
 * Modal settings dialog with category-based navigation.
 * 
 * @author Geger John Paul Gabayeron
 */
public class SettingsDialog extends JDialog {
    private final SettingsManager settings = SettingsManager.getInstance();
    private final Map<SettingsKey<?>, Object> pendingChanges = new LinkedHashMap<>();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private JList<SettingsCategory> categoryList;

    public SettingsDialog(Frame parent) {
        super(parent, I18nManager.get("dialog.settings.title"), true);
        initComponents();
        setMinimumSize(new Dimension(700, 500));
        setSize(800, 550);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Left sidebar with categories
        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);

        // Main content area
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        add(contentPanel, BorderLayout.CENTER);

        // Bottom buttons
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);

        // Populate category panels
        for (SettingsCategory category : SettingsCategory.values()) {
            JPanel panel = createCategoryPanel(category);
            contentPanel.add(panel, category.name());
        }

        // Select first category
        if (categoryList.getModel().getSize() > 0) {
            categoryList.setSelectedIndex(0);
        }
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1,
                UIManager.getColor("Separator.foreground")));
        sidebar.setPreferredSize(new Dimension(180, 0));

        categoryList = new JList<>(SettingsCategory.values());
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.setCellRenderer(new CategoryListRenderer());
        categoryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                SettingsCategory selected = categoryList.getSelectedValue();
                if (selected != null) {
                    cardLayout.show(contentPanel, selected.name());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(categoryList);
        scrollPane.setBorder(null);
        sidebar.add(scrollPane, BorderLayout.CENTER);

        return sidebar;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                UIManager.getColor("Separator.foreground")));

        JButton resetButton = new JButton(I18nManager.get("button.reset"));
        resetButton.addActionListener(e -> resetToDefaults());

        JButton cancelButton = new JButton(I18nManager.get("button.cancel"));
        cancelButton.addActionListener(e -> dispose());

        JButton applyButton = new JButton(I18nManager.get("button.apply"));
        applyButton.addActionListener(e -> applyChangesImmediately());

        JButton okButton = new JButton(I18nManager.get("button.ok"));
        okButton.addActionListener(e -> {
            if (!pendingChanges.isEmpty()) {
                applyChangesImmediately();
                JOptionPane.showMessageDialog(this,
                        I18nManager.get("msg.settings.saved") + "\n" + I18nManager.get("msg.settings.restart"),
                        I18nManager.get("dialog.settings.title"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
            dispose();
        });

        panel.add(resetButton);
        panel.add(Box.createHorizontalStrut(50));
        panel.add(cancelButton);
        panel.add(applyButton);
        panel.add(okButton);

        getRootPane().setDefaultButton(okButton);

        return panel;
    }

    private JPanel createCategoryPanel(SettingsCategory category) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Category header
        JLabel header = new JLabel(category.getDisplayName());
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setBorder(new EmptyBorder(0, 0, 5, 0));
        panel.add(header);

        JLabel description = new JLabel(category.getDescription());
        description.setForeground(UIManager.getColor("Label.disabledForeground"));
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(description);

        // Settings for this category
        List<SettingsKey<?>> keys = settings.getKeysForCategory(category);
        for (SettingsKey<?> key : keys) {
            JPanel settingRow = createSettingRow(key);
            settingRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(settingRow);
            panel.add(Box.createVerticalStrut(8));
        }

        // Container with scroll and top alignment
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.NORTH); // NORTH keeps content at top

        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel container = new JPanel(new BorderLayout());
        container.add(scrollPane, BorderLayout.CENTER);

        return container;
    }

    @SuppressWarnings("unchecked")
    private JPanel createSettingRow(SettingsKey<?> key) {
        JPanel row = new JPanel(new BorderLayout(15, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // Label side
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));

        // Use key.getKey() to lookup localized name, e.g. "appearance.theme" maps to
        // "Theme"
        JLabel nameLabel = new JLabel(I18nManager.get(key.getKey()));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelPanel.add(nameLabel);

        if (!key.getDescription().isEmpty()) {
            JLabel descLabel = new JLabel(key.getDescription());
            descLabel.setFont(descLabel.getFont().deriveFont(11f));
            descLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            labelPanel.add(descLabel);
        }

        row.add(labelPanel, BorderLayout.CENTER);

        // Control side
        JComponent control = createControl(key);
        JPanel controlWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controlWrapper.add(control);
        row.add(controlWrapper, BorderLayout.EAST);

        return row;
    }

    @SuppressWarnings("unchecked")
    private JComponent createControl(SettingsKey<?> key) {
        Class<?> type = key.getType();
        Object currentValue = settings.get(key);

        // Handle theme specially
        if (key == SettingsManager.THEME) {
            JComboBox<String> combo = new JComboBox<>(new String[] { "LIGHT", "DARK", "SYSTEM" });
            combo.setSelectedItem(currentValue);
            combo.setPreferredSize(new Dimension(120, 25));
            combo.addActionListener(e -> pendingChanges.put(key, combo.getSelectedItem()));
            return combo;
        }

        // Handle language specially
        if (key == SettingsManager.APPEARANCE_LANGUAGE) {
            JComboBox<String> combo = new JComboBox<>(new String[] { "English", "Spanish" });
            combo.setSelectedItem(currentValue);
            combo.setPreferredSize(new Dimension(120, 25));
            combo.addActionListener(e -> pendingChanges.put(key, combo.getSelectedItem()));
            return combo;
        }

        if (type == Boolean.class) {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setSelected(Boolean.TRUE.equals(currentValue));
            checkBox.addActionListener(e -> pendingChanges.put(key, checkBox.isSelected()));
            return checkBox;
        } else if (type == Integer.class) {
            SpinnerNumberModel model = new SpinnerNumberModel(
                    ((Integer) currentValue).intValue(), 1, 999, 1);
            JSpinner spinner = new JSpinner(model);
            spinner.setPreferredSize(new Dimension(80, 25));
            spinner.addChangeListener(e -> pendingChanges.put(key, spinner.getValue()));
            return spinner;
        } else if (type == String.class) {
            // Font family selector for known font keys
            if (key.getKey().contains("font.family")) {
                String[] fonts = { "Consolas", "Courier New", "Lucida Console",
                        "Monaco", "Monospaced", "JetBrains Mono", "Fira Code" };
                JComboBox<String> combo = new JComboBox<>(fonts);
                combo.setSelectedItem(currentValue);
                combo.setEditable(true);
                combo.setPreferredSize(new Dimension(150, 25));
                combo.addActionListener(e -> pendingChanges.put(key, combo.getSelectedItem()));
                return combo;
            } else {
                JTextField textField = new JTextField((String) currentValue, 15);
                textField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                    public void insertUpdate(javax.swing.event.DocumentEvent e) {
                        update();
                    }

                    public void removeUpdate(javax.swing.event.DocumentEvent e) {
                        update();
                    }

                    public void changedUpdate(javax.swing.event.DocumentEvent e) {
                        update();
                    }

                    private void update() {
                        pendingChanges.put(key, textField.getText());
                    }
                });
                return textField;
            }
        }

        return new JLabel("Unsupported type");
    }

    @SuppressWarnings("unchecked")
    private void applyChangesImmediately() {
        for (Map.Entry<SettingsKey<?>, Object> entry : pendingChanges.entrySet()) {
            SettingsKey<?> key = entry.getKey();
            Object newValue = entry.getValue();
            applyChange((SettingsKey<Object>) key, newValue);
        }

        // Special handling for theme changes
        if (pendingChanges.containsKey(SettingsManager.THEME)) {
            String themeName = (String) pendingChanges.get(SettingsManager.THEME);
            ThemeManager.THEMES theme = ThemeManager.THEMES.valueOf(themeName);
            ThemeManager.getInstance().changeTheme(theme);
        }

        pendingChanges.clear();
    }

    private <T> void applyChange(SettingsKey<T> key, Object value) {
        @SuppressWarnings("unchecked")
        T typedValue = (T) value;
        settings.set(key, typedValue);
    }

    private void resetToDefaults() {
        int result = JOptionPane.showConfirmDialog(this,
                "Reset all settings to their default values?", // Should be localized too but I defined defaults in
                                                               // English properties
                I18nManager.get("button.reset"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            settings.resetAll();
            dispose();
            // Reopen dialog with fresh values
            new SettingsDialog((Frame) getOwner()).setVisible(true);
        }
    }

    /**
     * Custom renderer for category list items.
     */
    private static class CategoryListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof SettingsCategory category) {
                // e.g. "category.appearance" maps to "Appearance"
                setText(I18nManager.get("category." + category.name().toLowerCase()));
                setBorder(new EmptyBorder(10, 15, 10, 15));
            }

            return this;
        }
    }
}
