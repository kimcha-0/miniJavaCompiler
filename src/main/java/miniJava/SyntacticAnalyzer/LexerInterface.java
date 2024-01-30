package comp520.syntacticanalyzer;

public interface LexerInterface {
    /**
     * @return Token object
     */
    Token scan();

    TokenType  scanToken();
}
