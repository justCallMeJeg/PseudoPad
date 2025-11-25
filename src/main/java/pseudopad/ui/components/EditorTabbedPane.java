package pseudopad.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.JButton;
import pseudopad.app.MainFrame;
import pseudopad.ui.FallbackPanel;
import pseudopad.utils.IconManager; // Optional if you have it

/**
 * Specialized TabPane for Editors with an "Add" button.
 */
public class EditorTabbedPane extends TabbedPane {
    private int untitledCount = 0;
    
    // The panel to show when no files are open
    private final FallbackPanel fallback;
    private boolean isFallbackVisible = false;
    
    public EditorTabbedPane(MainFrame mainFrame) {
        super();
        this.fallback = new FallbackPanel(mainFrame);
        
        this.fallback.putClientProperty(TabbedPane.PROP_IS_CLOSABLE, false);
        
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
        FileTabPane newEditor = new FileTabPane(""); 
        this.addTab("Untitled" + untitledCount + ".pc", newEditor);
        this.setSelectedIndex(this.getTabCount() - 1);
    }
    
    public void openFileTab(String title, String content) {
        // 1. Check if a tab with this title already exists
        int tabCount = this.getTabCount();
        for (int i = 0; i < tabCount; i++) {
            if (this.getTitleAt(i).equals(title)) {
                // 2. If it exists, select that existing tab and stop
                this.setSelectedIndex(i);
                System.out.println("Tab '" + title + "' already open. Switched to existing tab.");
                return; // Exit the method
            }
        }

        // 3. If the loop finishes without finding a match, create a new tab
        FileTabPane newEditor = new FileTabPane(content);
        this.addTab(title, newEditor);
        this.setSelectedComponent(newEditor);
        System.out.println("Created new tab: '" + title + "'");
    }

    // ==========================================================================
    //  FALLBACK LOGIC
    // ==========================================================================

    /**
     * Shows the fallback panel and hides the tab header to make it look seamless.
     */
    private void showFallback() {
        if (isFallbackVisible) return;
        
        untitledCount = 0;
        
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

        // 2. If the fallback is currently showing, remove it to make room for the real tab
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
        super.removeTabAt(index);
        
        // If the last tab was closed, show the fallback
        if (getTabCount() == 0) {
            showFallback();
        }
    }

    /**
     * Handle removeAll() just in case it's called externally.
     */
    @Override
    public void removeAll() {
        super.removeAll();
        showFallback();
    }
}
