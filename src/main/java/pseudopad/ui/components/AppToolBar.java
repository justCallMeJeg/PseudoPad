package pseudopad.ui.components;

import com.formdev.flatlaf.FlatClientProperties;

import pseudopad.app.ActionController;

import javax.swing.JButton;
import javax.swing.JToolBar;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class AppToolBar extends JToolBar {
    private final ActionController actions;

    public AppToolBar(ActionController actions) {
        this.actions = actions;
        initToolbar();
    }

    private void initToolbar() {
        // 1. Setup Basic Properties
        setFloatable(false); // Modern apps don't let you drag the toolbar out
        setRollover(true); // Highlight buttons on hover

        // Optional: FlatLaf styling for a cleaner look
        // putClientProperty(FlatClientProperties.STYLE, "padding: 4,4,4,4;");
        // 2. Add Actions
        // Just adding the Action object automatically creates a JButton
        // with the correct Icon and Tooltip!
        // --- File Group ---
        add(createButton(actions.NEW_PROJECT));
        add(createButton(actions.OPEN_PROJECT));
        add(createButton(actions.SAVE));

        addSeparator();

        // --- Edit Group ---
        add(createButton(actions.UNDO));
        add(createButton(actions.REDO));
        // add(createButton(actions.CUT));
        // add(createButton(actions.COPY));
        // add(createButton(actions.PASTE));
        // add(createButton(actions.DELETE));

        addSeparator();

        // --- Run Group ---
        add(createButton(actions.RUN_PROJECT));
    }

    /**
     * Helper to create a button from an Action. We wrap this to apply specific
     * toolbar styling if needed.
     */
    private JButton createButton(javax.swing.Action action) {
        if (action == null) {
            // Fallback for actions you haven't implemented yet
            JButton placeHolder = new JButton("?");
            placeHolder.setEnabled(false);
            return placeHolder;
        }

        JButton btn = new JButton(action);

        // Hide the text, show only the icon (standard for toolbars)
        btn.setText("");

        // Force FlatLaf Toolbar style (removes borders until hovered)
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);

        return btn;
    }
}
