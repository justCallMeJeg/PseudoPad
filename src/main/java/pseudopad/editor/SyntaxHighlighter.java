package pseudopad.editor;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import pseudopad.core.Lexer;
import pseudopad.core.Token;
import pseudopad.core.TokenType;
import pseudopad.utils.ThemeManager;

/**
 * Applies syntax highlighting to a StyledDocument using the PseudoPad Lexer.
 * 
 * @author Geger John Paul Gabayeron
 */
public class SyntaxHighlighter {

    private final StyledDocument doc;
    private final Map<TokenType, AttributeSet> tokenStyles;

    // Default Text Style
    private final SimpleAttributeSet defaultStyle;

    public SyntaxHighlighter(StyledDocument doc) {
        this.doc = doc;
        this.tokenStyles = new HashMap<>();
        this.defaultStyle = new SimpleAttributeSet();

        initStyles();
    }

    private void initStyles() {
        boolean isDark = ThemeManager.getInstance().isDarkMode();

        // Reset default style
        StyleConstants.setForeground(defaultStyle, isDark ? new Color(220, 220, 220) : Color.BLACK);

        // Define Colors
        Color keywordColor = isDark ? new Color(86, 156, 214) : new Color(0, 0, 255); // Blue
        Color stringColor = isDark ? new Color(206, 145, 120) : new Color(163, 21, 21); // Red/Orange
        Color numberColor = isDark ? new Color(181, 206, 168) : new Color(9, 134, 88); // Green
        Color typeColor = isDark ? new Color(78, 201, 176) : new Color(43, 145, 175); // Teal
        Color funcColor = isDark ? new Color(220, 220, 170) : new Color(121, 94, 38); // Yellow/Brown
        Color identifierColor = isDark ? new Color(220, 220, 220) : new Color(220, 220, 220); //
        Color commentColor = isDark ? new Color(106, 153, 85, 180) : new Color(0, 128, 0, 180); // Green with alpha

        // Map TokenTypes to Styles
        registerStyle(TokenType.SET, keywordColor);
        registerStyle(TokenType.CONST, keywordColor);
        registerStyle(TokenType.PRINT, keywordColor);
        registerStyle(TokenType.IF, keywordColor);
        registerStyle(TokenType.THEN, keywordColor);
        registerStyle(TokenType.ELIF, keywordColor);
        registerStyle(TokenType.ELSE, keywordColor);
        registerStyle(TokenType.ENDIF, keywordColor);
        registerStyle(TokenType.WHILE, keywordColor);
        registerStyle(TokenType.DO, keywordColor);
        registerStyle(TokenType.ENDWHILE, keywordColor);
        registerStyle(TokenType.FOR, keywordColor);
        registerStyle(TokenType.ENDFOR, keywordColor);
        registerStyle(TokenType.BREAK, keywordColor);
        registerStyle(TokenType.SKIP, keywordColor);
        registerStyle(TokenType.FUNC, funcColor);
        registerStyle(TokenType.ENDFUNC, funcColor);
        registerStyle(TokenType.RETURN, keywordColor);
        registerStyle(TokenType.CLASS, keywordColor);
        registerStyle(TokenType.ENDCLASS, keywordColor);
        registerStyle(TokenType.THIS, keywordColor);
        registerStyle(TokenType.AND, keywordColor);
        registerStyle(TokenType.OR, keywordColor);
        registerStyle(TokenType.NOT, keywordColor);
        registerStyle(TokenType.BOOLEAN, keywordColor);

        registerStyle(TokenType.IDENTIFIER, identifierColor);
        registerStyle(TokenType.TYPE, typeColor);

        registerStyle(TokenType.STRING, stringColor);
        registerStyle(TokenType.NUMBER, numberColor);
        registerStyle(TokenType.COMMENT, commentColor);
    }

    private void registerStyle(TokenType type, Color color) {
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, color);
        tokenStyles.put(type, style);
    }

    public void highlight() {
        SwingUtilities.invokeLater(() -> {
            try {
                String text = doc.getText(0, doc.getLength());

                // Reset all styles to default first
                doc.setCharacterAttributes(0, text.length(), defaultStyle, true);

                Lexer lexer = new Lexer(text);
                List<Token> tokens;
                try {
                    tokens = lexer.tokenize();
                } catch (Exception e) {
                    return;
                }

                for (Token token : tokens) {
                    if (token.type == TokenType.EOF)
                        continue;

                    AttributeSet style = tokenStyles.get(token.type);
                    if (style != null) {
                        doc.setCharacterAttributes(token.startIndex, token.length, style, false);
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
