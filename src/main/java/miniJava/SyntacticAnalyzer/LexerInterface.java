package miniJava.SyntacticAnalyzer;

public interface LexerInterface {
    /**
     * @return Token object
     */
    Token scan();

    TokenType  scanToken();
}
