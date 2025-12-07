package pseudopad.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.undo.UndoManager;
import pseudopad.app.MainFrame;
import pseudopad.core.Errors.CompilationError;
import pseudopad.editor.completion.AutoCompletion;
import pseudopad.editor.completion.PseudoCompletionProvider;
import pseudopad.ui.components.TextPane;
import pseudopad.utils.FileManager;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class FileTabPane extends JPanel {
    private final TextPane textPane;
    private final JScrollPane scrollPane;
    private final RowNumberHeader lineNumbers;
    private final SyntaxHighlighter highlighter;

    private File fileSource; // Null if it's a new "Untitled" file
    private String originalContent;
    private boolean isDirty = false;
    private String tabTitle; // Store the clean title (without *)

    private final UndoManager undoManager = new UndoManager();

    public FileTabPane() {
        this(null);
    }

    public FileTabPane(File source) {
        super(new BorderLayout());

        this.fileSource = source;

        if (source != null) {
            try {
                this.originalContent = FileManager.readFile(source);
            } catch (IOException ex) {
                System.err.println("Failed to open file: " + ex);
                this.originalContent = "";
            }
        } else {
            this.originalContent = "";
        }

        // 1. Initialize Editor
        textPane = new TextPane();
        textPane.setText(this.originalContent);
        textPane.setFont(new Font("Consolas", Font.PLAIN, 14));
        textPane.setCaretPosition(0);

        // Colors
        boolean isDark = pseudopad.utils.ThemeManager.getInstance().isDarkMode();
        if (isDark) {
            textPane.setCaretColor(Color.WHITE);
            textPane.setSelectionColor(new Color(75, 110, 175));
        } else {
            textPane.setCaretColor(Color.BLACK);
            textPane.setSelectionColor(new Color(173, 214, 255));
        }

        // Syntax Highlighting
        highlighter = new SyntaxHighlighter(textPane.getStyledDocument());
        highlighter.highlight();

        // 2. Scroll & Gutter
        scrollPane = new JScrollPane(textPane);
        lineNumbers = new RowNumberHeader(textPane);
        scrollPane.setRowHeaderView(lineNumbers);

        add(scrollPane, BorderLayout.CENTER);

        // 3. Track Changes
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                checkDirty();
                triggerAnalysis();
                highlighter.highlight();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkDirty();
                triggerAnalysis();
                highlighter.highlight();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkDirty();
                triggerAnalysis();
            }
        });

        // 4. Undo Manager
        undoManager.setLimit(100); // Limit history to prevent memory issues
        textPane.getDocument().addUndoableEditListener((UndoableEditEvent e) -> {
            if (e.getEdit() instanceof javax.swing.text.AbstractDocument.DefaultDocumentEvent de) {
                if (de.getType() == javax.swing.event.DocumentEvent.EventType.CHANGE) {
                    return;
                }
            }
            undoManager.addEdit(e.getEdit());
        });

        // 5. Auto Completion
        new AutoCompletion(textPane, new PseudoCompletionProvider());

        // 6. Proactive Analysis
        analysisTimer = new javax.swing.Timer(1000, e -> performAnalysis());
        analysisTimer.setRepeats(false);

        // Initial check
        SwingUtilities.invokeLater(this::performAnalysis);
    }

    private final javax.swing.Timer analysisTimer;
    private final Object highlightTag = new Object(); // Marker for our highlights (not easily doable with
                                                      // DefaultHighlighter, we track references if needed, or clear
                                                      // all)
    // Actually, we should probably clear only error highlights.
    // For simplicity, we can remove all highlights that match our painter, or just
    // clear all if syntax highlighting is done via Styles.

    private pseudopad.core.AST.ProgramNode cachedAST;

    private void triggerAnalysis() {
        analysisTimer.restart();
    }

    private void performAnalysis() {
        String code = textPane.getText();

        // Run in background to avoid freezing UI if large
        new Thread(() -> {
            pseudopad.core.Errors.CompilationResult result = pseudopad.core.PseudoRunner.compile(code);
            this.cachedAST = result.ast;

            SwingUtilities.invokeLater(() -> {
                // 1. Update Problems View
                if (MainFrame.getInstance() != null) {
                    pseudopad.ui.MainLayout layout = (pseudopad.ui.MainLayout) MainFrame.getInstance().getContentPane();

                    pseudopad.editor.ProblemsPanel problems = layout.getProblemsPanel();
                    if (problems != null) {
                        problems.updateErrors(fileSource, result.errors);
                    }

                    // Update Outline if this tab is active
                    if (isShowing()) {
                        pseudopad.editor.FileOutlinePanel outline = layout.getFileOutlinePanel();
                        if (outline != null) {
                            outline.updateOutline(result.ast);
                        }
                    }
                }

                // 2. Update Editor Highlights
                updateHighlighter(result.errors);
            });
        }).start();
    }

    public pseudopad.core.AST.ProgramNode getCachedAST() {
        return cachedAST;
    }

    private void updateHighlighter(List<CompilationError> errors) {
        Highlighter h = textPane.getHighlighter();
        h.removeAllHighlights(); // Assumption: Syntax highlighting uses Styles, not Highlighter. If
                                 // Search/Selection uses Highlighter, this might clear them.
        // If we want to preserve other highlights, we need to track ours.
        // But for now, let's assume simple clearing is OK or we accept the tradeoff for
        // errors.

        DefaultHighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(
                new Color(255, 100, 100, 50)); // Light Red

        for (CompilationError error : errors) {
            try {
                // Map line/col to offset
                Element root = textPane.getDocument().getDefaultRootElement();
                int line = Math.max(0, error.line - 1);
                if (line >= root.getElementCount())
                    continue;

                Element lineElem = root.getElement(line);
                int start = lineElem.getStartOffset() + Math.max(0, error.column - 1);
                int end = start + Math.max(1, error.length); // Ensure at least 1 char width

                // Clamp to line end
                end = Math.min(end, lineElem.getEndOffset() - 1);
                if (end <= start)
                    end = start + 1; // Fallback

                h.addHighlight(start, end, painter);
            } catch (Exception e) {
                // Ignore invalid positions
            }
        }

        Set<Integer> errorLines = new HashSet<>();
        for (CompilationError error : errors) {
            errorLines.add(error.line); // 1-based
        }
        lineNumbers.setErrorLines(errorLines);
    }

    private void checkDirty() {
        // Optimization: First check length. If lengths differ, it's definitely changed.
        // This avoids expensive String comparisons for every single keystroke.
        int currentLength = textPane.getDocument().getLength();
        int originalLength = originalContent.length();

        boolean changed;
        if (currentLength != originalLength) {
            changed = true;
        } else {
            // Only do the expensive check if lengths match (e.g. replaced a char)
            changed = !textPane.getText().equals(originalContent);
        }

        if (changed != isDirty) {
            isDirty = changed;
            updateTabTitle();
        }
    }

    private void updateTabTitle() {
        // We need to find our parent TabbedPane to update the title
        EditorTabbedPane parentTab = (EditorTabbedPane) SwingUtilities.getAncestorOfClass(EditorTabbedPane.class, this);
        if (parentTab != null) {
            int index = parentTab.indexOfComponent(this);
            if (index != -1) {
                // If we have a file source, ALWAYS use its name as the base title
                if (fileSource != null) {
                    tabTitle = fileSource.getName();
                } else if (tabTitle == null) {
                    // If no file source and no title set, grab it from the tab (initial state)
                    tabTitle = parentTab.getTitleAt(index).replace("*", "").trim();
                }

                String newTitle = tabTitle + (isDirty ? " *" : "");
                parentTab.setTitleAt(index, newTitle);

                parentTab.setForegroundAt(index, isDirty ? Color.CYAN : null); // Simple visual cue
            }
        }
    }

    public boolean requestClose() {
        if (!isDirty)
            return true; // Safe to close

        String name = (fileSource != null) ? fileSource.getName() : (tabTitle != null ? tabTitle : "Untitled");

        int choice = JOptionPane.showConfirmDialog(null,
                "Do you want to save changes to '" + name + "'?",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION);

        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
            return false; // Abort closing
        }

        if (choice == JOptionPane.YES_OPTION) {
            return saveFile(); // Close only if save succeeds
        }

        return true; // NO_OPTION -> Discard changes and close
    }

    public boolean saveFile() {
        if (fileSource == null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Specify a file to save");

            // Set default directory to current project if available
            if (MainFrame.getInstance() != null && MainFrame.getInstance().getCurrentProjectPath() != null) {
                fileChooser.setCurrentDirectory(MainFrame.getInstance().getCurrentProjectPath());
            }

            // Optional: Set a file filter (e.g., only .txt files)
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Documents (*.pc)", "pc");
            fileChooser.setFileFilter(filter);

            // Show the Save dialog
            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();

                // Ensure the file has the correct extension if needed (optional logic)
                if (!filePath.toLowerCase().endsWith(".pc")) {
                    fileToSave = new File(filePath + ".pc");
                }

                this.fileSource = fileToSave;
                saveFile();
            }
        }

        try {
            FileManager.saveFile(fileSource, textPane.getText());
            originalContent = textPane.getText(); // Update baseline
            isDirty = false;
            updateTabTitle();

            // Refresh File Explorer if we just saved a new file
            if (MainFrame.getInstance() != null) {
                MainFrame.getInstance().refreshFileExplorer();
            }

            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage());
            return false;
        }
    }

    // ----- EDIT ACTIONS -----

    public void undo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
        }
    }

    public void redo() {
        if (undoManager.canRedo()) {
            undoManager.redo();
        }
    }

    public void cut() {
        textPane.cut();
    }

    public void copy() {
        textPane.copy();
    }

    public void paste() {
        textPane.paste();
    }

    // ----- SETTERS and GETTERS -----

    public File getFile() {
        return fileSource;
    }

    public void setFile(File file) {
        this.fileSource = file;
    }

    public void updateFileSource(File newFile) {
        this.fileSource = newFile;
        // Reset title so it gets refreshed from the new file name
        this.tabTitle = null;
        updateTabTitle();
    }

    public String getText() {
        return textPane.getText();
    }

    public TextPane getTextPane() {
        return textPane;
    }
}
