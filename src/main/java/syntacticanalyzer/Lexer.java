package syntacticanalyzer;

import java.io.InputStream;
import java.io.FileInputStream;

public class Lexer implements LexerInterface {
    private char _charBuf;
    private StringBuilder _stringBuf;
    private InputStream _in;
    public Lexer(FileInputStream in) {
        this._stringBuf = new StringBuilder();
        this._in = in;
    }

    @Override
    public Token scan() {
        return null;
    }

    @Override
    public void nextChar() {

    }

    @Override
    public void takeIt() {
        _stringBuf.append(_charBuf);
    }

    @Override
    public void skipIt() {
        nextChar();
    }
}
