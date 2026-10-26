package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import java.util.Locale;

import org.junit.Test;

public class GreetingTest {
    @Test
    public void titleCaseCapitalisesEachWordOnly() {
        assertEquals("Good Afternoon", Greeting.titleCase("Good afternoon", Locale.ENGLISH));
        assertEquals("Good Evening", Greeting.titleCase("good evening", Locale.ENGLISH));
        assertEquals("Buenas Noches", Greeting.titleCase("Buenas noches", Locale.ENGLISH));
        assertEquals("", Greeting.titleCase("", Locale.ENGLISH));
    }

}
