package io.oreto.brew;

import io.oreto.brew.str.Word;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WordTest {

    @Test
    public void numberToWord() {
        assertEquals("nine hundred ninety nine"
                , Word.groupToWord(Word.ZeroTo9.nine, Word.ZeroTo9.nine, Word.ZeroTo9.nine));
        assertEquals("ten"
                , Word.groupToWord(Word.ZeroTo9.zero, Word.ZeroTo9.one, Word.ZeroTo9.zero));
        assertEquals("one hundred thirteen"
                , Word.groupToWord(Word.ZeroTo9.one, Word.ZeroTo9.one, Word.ZeroTo9.three));

        assertEquals(Optional.of("zero"), Word.fromNumber("00000"));
        assertEquals(Optional.of("zero"), Word.fromNumber("-00000"));
        assertEquals(Optional.of("zero"), Word.fromNumber("+00000"));
        assertEquals(Optional.empty(), Word.fromNumber("+-00000"));
        assertEquals(Optional.of("nine thousand nine hundred ninety nine"), Word.fromNumber("9999"));
        assertEquals(Optional.of("one hundred twenty three"), Word.fromNumber("123"));
        assertEquals(Optional.of("twelve"), Word.fromNumber("12"));
        assertEquals(Optional.of("negative one hundred thousand"), Word.fromNumber("-100000"));
        assertEquals(Optional.of("one hundred thousand"), Word.fromNumber("+100000"));
        assertEquals(Optional.of("one million one"), Word.fromNumber("1000001"));
        assertEquals(Optional.of("nine million nine hundred ninety nine thousand nine hundred nineteen")
                , Word.fromNumber("9999919"));
    }
}
