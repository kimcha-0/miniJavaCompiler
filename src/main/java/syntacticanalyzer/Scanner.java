package syntacticanalyzer;

import java.io.InputStream;
import java.io.FileInputStream;

public class Scanner implements ScannerInterface {
    private char _charBuf;
    private StringBuilder _stringBuf;
    private InputStream _in;
    public Scanner(FileInputStream in) {
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

    }

    @Override
    public void skipIt() {

    }
}
