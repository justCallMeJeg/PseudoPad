package pseudopad.editor;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

import pseudopad.utils.ThemeManager;

/**
 * A toolbar for Find and Replace operations.
 *
 * @author Geger John Paul Gabayeron
 */
public class FindReplaceBar extends JPanel {

    private final JTextComponent targetEditor;
    private final Runnable closeCallback;
    private final JTextField findField;
    private final JTextField replaceField;
    private final JCheckBox matchCaseCheck;
    private final JPanel replaceContainer;

    private boolean inReplaceMode = false;

    // Callbacks for actual logic
    private ActionListener findNextAction;
    private ActionListener findPrevAction;
    private ActionListener replaceAction;
    private ActionListener replaceAllAction;

    public FindReplaceBar(JTextComponent targetEditor, Runnable closeCallback) {
        this.targetEditor = targetEditor;
        this.closeCallback = closeCallback;

        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
        applyTheme();
        setBorder(new EmptyBorder(2, 2, 2, 2));

        // --- Inputs ---

        // Find Field
        add(new JLabel("Find:"));
        findField = new JTextField(15);
        setupField(findField);
        add(findField);

        // Match Case
        matchCaseCheck = new JCheckBox("Aa");
        matchCaseCheck.setToolTipText("Match Case");
        matchCaseCheck.setOpaque(false);
        add(matchCaseCheck);

        // Navigation
        JButton prevBtn = createButton("<", "Find Previous");
        prevBtn.addActionListener(e -> {
            if (findPrevAction != null)
                findPrevAction.actionPerformed(e);
        });
        add(prevBtn);

        JButton nextBtn = createButton(">", "Find Next");
        nextBtn.addActionListener(e -> {
            if (findNextAction != null)
                findNextAction.actionPerformed(e);
        });
        add(nextBtn);

        // --- Replace Section (Toggleable) ---
        replaceContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        replaceContainer.setOpaque(false);

        replaceContainer.add(new JLabel("| Repl:"));
        replaceField = new JTextField(15);
        setupField(replaceField);
        replaceContainer.add(replaceField);

        JButton replaceBtn = createButton("Replace", "Replace current selection");
        replaceBtn.addActionListener(e -> {
            if (replaceAction != null)
                replaceAction.actionPerformed(e);
        });
        replaceContainer.add(replaceBtn);

        JButton replaceAllBtn = createButton("All", "Replace All");
        replaceAllBtn.addActionListener(e -> {
            if (replaceAllAction != null)
                replaceAllAction.actionPerformed(e);
        });
        replaceContainer.add(replaceAllBtn);

        add(replaceContainer);

        // --- Close ---
        JButton closeBtn = createButton("X", "Close");
        closeBtn.addActionListener(e -> close());
        add(closeBtn); // Always visible at end

        // Initial State
        setReplaceMode(false);
    }

    private void setupField(JTextField field) {
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (findNextAction != null)
                        findNextAction.actionPerformed(null);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    close();
                }
            }
        });
    }

    private JButton createButton(String text, String tip) {
        JButton btn = new JButton(text);
        btn.setToolTipText(tip);
        btn.setMargin(new Insets(2, 5, 2, 5));
        btn.setFocusable(false); // Keep focus on fields
        return btn;
    }

    public void setReplaceMode(boolean replace) {
        this.inReplaceMode = replace;
        replaceContainer.setVisible(replace);
        revalidate();
        repaint();
    }

    public boolean isReplaceMode() {
        return inReplaceMode;
    }

    public void open() {
        this.setVisible(true);
        this.findField.requestFocusInWindow();
        this.findField.selectAll();
    }

    public void close() {
        this.setVisible(false);
        targetEditor.requestFocusInWindow();
        if (closeCallback != null)
            closeCallback.run();
    }

    // --- Accessors ---
    public String getFindText() {
        return findField.getText();
    }

    public String getReplaceText() {
        return replaceField.getText();
    }

    public boolean isMatchCase() {
        return matchCaseCheck.isSelected();
    }

    public void setActions(ActionListener next, ActionListener prev, ActionListener replace,
            ActionListener replaceAll) {
        this.findNextAction = next;
        this.findPrevAction = prev;
        this.replaceAction = replace;
        this.replaceAllAction = replaceAll;
    }

    public void addFindDocumentListener(javax.swing.event.DocumentListener listener) {
        findField.getDocument().addDocumentListener(listener);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        applyTheme();
    }

    private void applyTheme() {
        if (ThemeManager.getInstance().isDarkMode()) {
            setBackground(new Color(45, 45, 45));
            if (findField != null) {
                findField.setCaretColor(Color.WHITE);
                replaceField.setCaretColor(Color.WHITE);
                matchCaseCheck.setForeground(Color.WHITE);
            }
        } else {
            setBackground(new Color(240, 240, 240));
            if (findField != null) {
                findField.setCaretColor(Color.BLACK);
                replaceField.setCaretColor(Color.BLACK);
                matchCaseCheck.setForeground(Color.BLACK);
            }
        }
    }
}
