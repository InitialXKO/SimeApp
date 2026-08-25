package com.shiyu.sime.ime.engine;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Offline simple expression evaluator for Number mode.
 * Supports basic arithmetic operations (+, -, *, /) and parentheses.
 */
public final class CalculatorEngine {

    private CalculatorEngine() {}

    public static String evaluate(String expr) {
        if (expr == null || expr.trim().isEmpty()) return null;
        // Check if string contains arithmetic operators
        if (!expr.matches(".*[+\\-*/×÷].*")) return null;

        try {
            String sanitized = expr.replace("×", "*").replace("÷", "/").replaceAll("\\s+", "");
            double result = parseAndEval(sanitized);
            if (Double.isNaN(result) || Double.isInfinite(result)) return null;

            if (result == (long) result) {
                return String.valueOf((long) result);
            } else {
                return String.valueOf(result);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static double parseAndEval(String expr) {
        Deque<Double> values = new ArrayDeque<>();
        Deque<Character> ops = new ArrayDeque<>();

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                StringBuilder sbuf = new StringBuilder();
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    sbuf.append(expr.charAt(i++));
                }
                i--;
                values.push(Double.parseDouble(sbuf.toString()));
            } else if (c == '(') {
                ops.push(c);
            } else if (c == ')') {
                while (!ops.isEmpty() && ops.peek() != '(') {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                }
                if (!ops.isEmpty()) ops.pop();
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                if (c == '-' && (i == 0 || expr.charAt(i - 1) == '(' || isOp(expr.charAt(i - 1)))) {
                    // Unary minus
                    StringBuilder sbuf = new StringBuilder("-");
                    i++;
                    while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                        sbuf.append(expr.charAt(i++));
                    }
                    i--;
                    values.push(Double.parseDouble(sbuf.toString()));
                    continue;
                }
                while (!ops.isEmpty() && hasPrecedence(c, ops.peek())) {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                }
                ops.push(c);
            }
        }

        while (!ops.isEmpty()) {
            values.push(applyOp(ops.pop(), values.pop(), values.pop()));
        }

        return values.isEmpty() ? Double.NaN : values.pop();
    }

    private static boolean isOp(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private static boolean hasPrecedence(char op1, char op2) {
        if (op2 == '(' || op2 == ')') return false;
        if ((op1 == '*' || op1 == '/') && (op2 == '+' || op2 == '-')) return false;
        return true;
    }

    private static double applyOp(char op, double b, double a) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/':
                if (b == 0) throw new UnsupportedOperationException("Divide by zero");
                return a / b;
        }
        return 0;
    }
}
