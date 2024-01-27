package syntacticanalyzer;

import static org.junit.jupiter.api.Assertions.*;
import static comp520.syntacticanalyzer.TokenType.*;

class LexerTest {

    @org.junit.jupiter.api.Test
    void scan() {
        assertAll(
                () -> assertEquals(OPERATOR, '*'),
                () -> assertEquals(EQUALS, '='),
                () -> assertEquals(OPERATOR, "==")
        );
    }
}