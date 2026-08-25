package com.shiyu.sime.ime.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests for {@link SimeTheme}.
 */
public class SimeThemeTest {

    @Test
    public void testThemeProperties() {
        SimeTheme lightTheme = SimeTheme.light();
        SimeTheme darkTheme = SimeTheme.dark();

        assertNotNull(lightTheme);
        assertNotNull(darkTheme);

        assertEquals(8, lightTheme.keyCornerRadiusDp);
        assertEquals(8, darkTheme.keyCornerRadiusDp);

        assertEquals(lightTheme.accentColor, lightTheme.candidateHighlight);
        assertEquals(darkTheme.accentColor, darkTheme.candidateHighlight);
    }
}
