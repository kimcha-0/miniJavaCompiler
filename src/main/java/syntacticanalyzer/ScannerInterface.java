package syntacticanalyzer;

public interface ScannerInterface {
    /**
     * @return Token object
     */
    Token scan();

    /**
     * loads next character from input stream into character buffer
     */
    void nextChar();

    /**
     * we "take" the current char in the buffer and move to the next char
     */
    void takeIt();

    /** skip current char in buffer and move to next char */
    void skipIt();
}
