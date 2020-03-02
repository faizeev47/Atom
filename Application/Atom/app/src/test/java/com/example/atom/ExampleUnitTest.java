package com.example.atom;

import org.junit.Test;

import static com.example.atom.Helpers.formatFileName;
import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void filenameFormatter_removesPdf() {
        String formattedName = formatFileName("Turing.pdf");
        assertEquals("Turing", formattedName);
    }

    @Test
    public void filenameFormatter_capitalizes() {
        String formattedName = formatFileName("alexi leonov");
        assertEquals("Alexi Leonov", formattedName);
    }

    @Test
    public void filenameFormatter_removesUnderscore() {
        String formattedName = formatFileName("deckard_shaw");
        assertEquals("Deckard Shaw", formattedName);
    }

}