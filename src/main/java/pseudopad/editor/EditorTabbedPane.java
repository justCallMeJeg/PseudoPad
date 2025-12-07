package pseudopad.editor;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import pseudopad.app.MainFrame;
import pseudopad.ui.FallbackPanel;
import pseudopad.ui.components.TabbedPane;
import pseudopad.utils.IconManager;
import pseudopad.utils.AppConstants;
import pseudopad.utils.AppLogger;

/**
 * Specialized TabPane for Editors with an "Add" button.
 */
public class EditorTabbedPane extends TabbedPane {
    private int untitledCount = 0;

    // The panel to show when no files are open
    private final FallbackPanel fallback;
    private boolean isFallbackVisible = false;
    private boolean isRemovingAll = false;

    public EditorTabbedPane(MainFrame mainFrame) {
        super();
        this.fallback = new FallbackPanel(mainFrame);

        this.fallback.putClientProperty(TabbedPane.PROP_IS_CLOSABLE, false);

        this.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK,
                (BiConsumer<JTabbedPane, Integer>) (tabPane, tabIndex) -> {
                    if (tabIndex >= 0 && tabIndex < tabPane.getTabCount()) {
                        Component c = tabPane.getComponentAt(tabIndex);

                        // If it's a file tab, check for safety
                        if (c instanceof FileTabPane fileTab) {
                            if (fileTab.requestClose()) {
                                tabPane.removeTabAt(tabIndex);
                            }
                        } else {
                            // Not a file tab (e.g. Fallback), just close it
                            tabPane.removeTabAt(tabIndex);
                        }
                    }
                });

        initAddButton();
        showFallback();
    }

    public void refreshFallbackState() {
        if (isFallbackVisible) {
            fallback.updateState();
        }
    }

    private void initAddButton() {
        JButton addBtn = new JButton("+");

        // Optional: Use Icon if available
        if (IconManager.get("add") != null) {
            addBtn.setIcon(IconManager.get("add"));
            addBtn.setText("");
        }

        addBtn.setToolTipText("New File");
        addBtn.addActionListener((ActionEvent e) -> addNewTab());

        // CRITICAL: Add to the LEFT side
        addLeftHeaderButton(addBtn);
    }

    public void addNewTab() {
        untitledCount++;
        FileTabPane newEditor = new FileTabPane();
        this.addTab(AppConstants.DEFAULT_TAB_TITLE + untitledCount + AppConstants.FILE_EXTENSION, newEditor);
        this.setSelectedIndex(this.getTabCount() - 1);
    }

    public void openFileTab(File file) {
        // 1. Check if a tab with this title already exists
        // 1. Check if a tab with this file already exists
        int tabCount = this.getTabCount();
        for (int i = 0; i < tabCount; i++) {
            Component c = getComponentAt(i);
            if (c instanceof FileTabPane fileTab) {
                File tabFile = fileTab.getFile();
                // Compare files (null-safely, though 'file' arg is assumed non-null here)
                if (tabFile != null && tabFile.getAbsolutePath().equals(file.getAbsolutePath())) {
                    // 2. If it exists, select that existing tab and stop
                    this.setSelectedIndex(i);
                    System.out.println("Tab '" + file.getName() + "' already open. Switched to existing tab.");
                    return; // Exit the method
                }
            }
        }

        // 3. If the loop finishes without finding a match, create a new tab
        FileTabPane newEditor = new FileTabPane(file);
        this.addTab(file.getName(), newEditor);
        this.setSelectedComponent(newEditor);
        AppLogger.info("Opened file: " + file.getName());
        System.out.println("Created new tab: '" + file.getName() + "'");
    }

    public void saveActiveTab() {
        Component c = getSelectedComponent();
        if (c instanceof FileTabPane fileTabPane) {
            fileTabPane.saveFile();
            if (fileTabPane.getFile() != null) {
                AppLogger.info("Saved file: " + fileTabPane.getFile().getName());
            }
        }
    }

    public void undo() {
        Component c = getSelectedComponent();
        if (c instanceof FileTabPane fileTabPane) {
            fileTabPane.undo();
        }
    }

    public void redo() {
        Component c = getSelectedComponent();
        if (c instanceof FileTabPane fileTabPane) {
            fileTabPane.redo();
        }
    }

    public void cut() {
        Component c = getSelectedComponent();
        if (c instanceof FileTabPane fileTabPane) {
            fileTabPane.cut();
        }
    }

    public void copy() {
        Component c = getSelectedComponent();
        if (c instanceof FileTabPane fileTabPane) {
            fileTabPane.copy();
        }
    }

    public void paste() {
        Component c = getSelectedComponent();
        if (c instanceof FileTabPane fileTabPane) {
            fileTabPane.paste();
        }
    }

    public void handleFileRename(File oldFile, File newFile) {
        for (int i = 0; i < getTabCount(); i++) {
            Component c = getComponentAt(i);
            if (c instanceof FileTabPane fileTabPane) {
                File tabFile = fileTabPane.getFile();
                if (tabFile != null && tabFile.equals(oldFile)) {
                    fileTabPane.updateFileSource(newFile);
                    // No need to manually set title here, updateFileSource -> updateTabTitle will
                    // handle it
                    // and preserve the dirty state logic if needed (though usually rename happens
                    // on saved files)
                    break; // Assuming only one tab per file
                }
            }
        }
    }

    public void closeFileTab(File file) {
        for (int i = 0; i < getTabCount(); i++) {
            Component c = getComponentAt(i);
            if (c instanceof FileTabPane fileTabPane) {
                File tabFile = fileTabPane.getFile();
                // Check if the tab's file matches the deleted file
                // OR if the deleted file is a directory and the tab's file is inside it
                if (tabFile != null) {
                    if (tabFile.equals(file)
                            || tabFile.getAbsolutePath().startsWith(file.getAbsolutePath() + File.separator)) {
                        AppLogger.info("Closed tab due to deletion: " + tabFile.getName());
                        removeTabAt(i);
                        i--; // Adjust index since we removed a tab
                    }
                }
            }
        }
    }

    // ==========================================================================
    // FALLBACK LOGIC
    // ==========================================================================

    /**
     * Shows the fallback panel and hides the tab header to make it look seamless.
     */
    private void showFallback() {
        if (isFallbackVisible)
            return;

        untitledCount = 0;

        // Ensure fallback state matches current project state
        fallback.updateState();

        // Add the fallback as a tab (index 0)
        // We pass 'null' for icon/tip as the header will be hidden anyway
        super.insertTab("Welcome", null, fallback, null, 0);

        // VISUAL TRICK: Hide the tab strip so it looks like a regular panel
        putClientProperty(FlatClientProperties.TABBED_PANE_HIDE_TAB_AREA_WITH_ONE_TAB, false);

        isFallbackVisible = true;
    }

    /**
     * Intercepts ALL insertions to check for the fallback.
     */
    @Override
    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        // 1. Safety check: If we are adding the fallback itself, let it pass
        if (component == fallback) {
            super.insertTab(title, icon, component, tip, index);
            return;
        }

        // 2. If the fallback is currently showing, remove it to make room for the real
        // tab
        if (isFallbackVisible) {
            super.removeTabAt(0); // Remove the fallback
            isFallbackVisible = false;

            // Restore the tab header visibility
            putClientProperty(FlatClientProperties.TABBED_PANE_HIDE_TAB_AREA_WITH_ONE_TAB, false);

            // Reset index to 0 (since the pane is now empty)
            index = 0;
        }

        // 3. Proceed with normal insertion
        super.insertTab(title, icon, component, tip, index);
    }

    /**
     * Intercepts removals to re-show fallback if empty.
     */
    @Override
    public void removeTabAt(int index) {
        if (index < 0 || index >= getTabCount()) {
            return;
        }
        super.removeTabAt(index);

        // If the last tab was closed, show the fallback
        // But NOT if we are in the middle of a bulk remove (removeAll)
        if (getTabCount() == 0 && !isRemovingAll) {
            showFallback();
        }
    }

    /**
     * Handle removeAll() just in case it's called externally.
     */
    @Override
    public void removeAll() {
        isRemovingAll = true;
        try {
            super.removeAll();
        } finally {
            isRemovingAll = false;
        }
        isFallbackVisible = false; // Reset flag

        // Defer adding the fallback to ensure the pane is fully cleared and events
        // processed
        SwingUtilities.invokeLater(this::showFallback);
    }

    public List<String> getOpenFiles() {
        List<String> files = new ArrayList<>();
        for (int i = 0; i < getTabCount(); i++) {
            Component c = getComponentAt(i);
            if (c instanceof FileTabPane fileTabPane) {
                File f = fileTabPane.getFile();
                if (f != null) {
                    files.add(f.getAbsolutePath());
                }
            }
        }
        return files;
    }

    public String getActiveFile() {
        Component c = getSelectedComponent();
        if (c instanceof FileTabPane fileTabPane) {
            File f = fileTabPane.getFile();
            if (f != null) {
                return f.getAbsolutePath();
            }
        }
        return null;
    }

    public String getActiveFileContent() {
        Component c = getSelectedComponent();
        if (c instanceof FileTabPane fileTabPane) {
            return fileTabPane.getText();
        }
        return null;
    }
}
