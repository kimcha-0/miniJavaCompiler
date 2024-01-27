package comp520.syntacticanalyzer;

import comp520.ErrorReporter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Lexer implements LexerInterface {
    private char _charBuf;
    private boolean eot = false;
    private StringBuilder _stringBuf;
    private InputStream _in;
    private ErrorReporter _reporter;
    public Lexer(FileInputStream in, ErrorReporter reporter) {
        this._stringBuf = new StringBuilder();
        this._in = in;
        this._reporter = reporter;
    }

    @Override
    public Token scan() {
        Token token =  null;
        _stringBuf.setLength(0);
        switch(_charBuf) {
            case '/':
                if (peek('/')) {
                    skipIt();
                    while (_charBuf != '\n' || !eot)  {
                        skipIt();
                    }
                } else if (peek('*')) {
                    skipIt();
                    while (_charBuf != '*' || !eot) {
                        skipIt();
                    }
                } else {
                    takeIt();
                    token = new Token(TokenType.OPERATOR, _stringBuf.toString());
                }
                break;
            case '&':
                if (peek('&')) {
                    takeIt();
                    token = new Token(TokenType.OPERATOR, _stringBuf.toString());
                } else {
                    _reporter.reportError("token not recognized, character after '&' incorrect");
                }
                break;
            case '|':
                if (peek('|')) {
                    takeIt();
                    token = new Token(TokenType.OPERATOR, _stringBuf.toString());
                } else {
                    _reporter.reportError("token not recognized, character after '|' incorrect");
                }
                break;
            case '+', '-', '!', '*':
                takeIt();
                token = new Token(TokenType.OPERATOR, _stringBuf.toString());
                break;
        }
        return token;
    }

    public boolean peek(char expected) {
        nextChar();
        if (_charBuf == expected) {
            return true;
        }
        return false;
    }
    @Override
    public void nextChar() {
        int c;
        try {
            if((c = _in.read()) == -1) {
                eot = true;
            } else {
                _charBuf = (char)c;
            }
        } catch (IOException e) {
            _reporter.reportError("parser read error");
        }

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