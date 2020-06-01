package com.example.atom;

import org.junit.Test;

import static com.example.atom.Utilities.Utils.formatFileName;
import static com.example.atom.Utilities.Utils.getFormattedTime;
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

    @Test
    public void getFormattedTime_correctSeconds() {
        String formattedTime = getFormattedTime(10);
        assertEquals("00:00:10", formattedTime);

        formattedTime = getFormattedTime(84);
        assertEquals("00:01:24", formattedTime);

        formattedTime = getFormattedTime(3684);
        assertEquals("01:01:24", formattedTime);
    }

}