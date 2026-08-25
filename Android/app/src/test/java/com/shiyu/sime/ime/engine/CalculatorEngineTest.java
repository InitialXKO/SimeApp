package com.shiyu.sime.ime.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CalculatorEngineTest {

    @Test
    public void testBasicOperations() {
        assertEquals("5", CalculatorEngine.evaluate("2+3"));
        assertEquals("12", CalculatorEngine.evaluate("3×4"));
        assertEquals("4", CalculatorEngine.evaluate("12÷3"));
        assertEquals("7", CalculatorEngine.evaluate("10-3"));
    }

    @Test
    public void testComplexExpressions() {
        assertEquals("14", CalculatorEngine.evaluate("2+3×4"));
        assertEquals("20", CalculatorEngine.evaluate("(2+3)×4"));
        assertEquals("2.5", CalculatorEngine.evaluate("5÷2"));
    }

    @Test
    public void testInvalidAndEdgeCases() {
        assertNull(CalculatorEngine.evaluate("abc"));
        assertNull(CalculatorEngine.evaluate("123"));
        assertNull(CalculatorEngine.evaluate("5÷0"));
    }
}
