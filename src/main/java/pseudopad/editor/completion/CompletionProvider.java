package pseudopad.editor.completion;

import java.util.List;
import javax.swing.text.JTextComponent;

/**
 * Interface for providing completion suggestions.
 */
public interface CompletionProvider {
    /**
     * Gets the list of completions based on the current context of the text
     * component.
     * 
     * @param comp The text component.
     * @return List of completions, or empty list if none.
     */
    List<CompletionItem> getCompletions(JTextComponent comp);
}
