package com.financebook.util;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Safe evaluator for fee formulas f(x, y) entered in the admin panel.
 *
 * Mirrors safe_eval_formula() of the Python admin panel (adminpanel/fee_engine.py),
 * which validates the formulas before they are stored:
 * <ul>
 *   <li>numbers, the variables {@code x} (absolute payment amount) and {@code y} (payment frequency)</li>
 *   <li>operators {@code + - * / **}, unary {@code + -}, parentheses</li>
 *   <li>functions {@code abs, min, max, sqrt, log, sin, cos}</li>
 *   <li>division by zero yields 0</li>
 * </ul>
 * Operator precedence follows Python: {@code **} binds tighter than unary minus
 * and is right-associative ({@code -2**2 == -4}, {@code 2**3**2 == 512}).
 *
 * No code is ever executed - the formula is parsed by a small recursive-descent parser.
 */
public final class FeeFormula {

    private static final int MAX_NESTING = 100;

    private FeeFormula() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Evaluate a formula.
     *
     * @return the result, or empty if the formula is invalid or the result is not a finite number
     */
    public static OptionalDouble evaluate(String formula, double x, double y) {
        if (formula == null || formula.isBlank()) {
            return OptionalDouble.empty();
        }
        try {
            double value = new Parser(formula, x, y).parse();
            return Double.isFinite(value) ? OptionalDouble.of(value) : OptionalDouble.empty();
        } catch (IllegalArgumentException e) {
            return OptionalDouble.empty();
        }
    }

    private static final class Parser {
        private final String src;
        private final double x;
        private final double y;
        private int pos;
        private int depth;

        Parser(String src, double x, double y) {
            this.src = src;
            this.x = x;
            this.y = y;
        }

        double parse() {
            double value = expression();
            skipWhitespace();
            if (pos < src.length()) {
                throw error("Unexpected '" + src.charAt(pos) + "'");
            }
            return value;
        }

        // expression := term (('+' | '-') term)*
        private double expression() {
            double value = term();
            while (true) {
                if (accept("+")) {
                    value += term();
                } else if (accept("-")) {
                    value -= term();
                } else {
                    return value;
                }
            }
        }

        // term := unary (('*' | '/') unary)*      ('**' is handled by power())
        private double term() {
            double value = unary();
            while (true) {
                skipWhitespace();
                if (src.startsWith("**", pos) || src.startsWith("//", pos)) {
                    // '**' is consumed in power(); a '**' here or Python's '//' is invalid
                    throw error("Unexpected operator");
                } else if (accept("*")) {
                    value *= unary();
                } else if (accept("/")) {
                    double divisor = unary();
                    value = divisor == 0 ? 0.0 : value / divisor;
                } else {
                    return value;
                }
            }
        }

        // unary := ('+' | '-') unary | power
        private double unary() {
            enter();
            try {
                if (accept("+")) {
                    return unary();
                }
                if (accept("-")) {
                    return -unary();
                }
                return power();
            } finally {
                depth--;
            }
        }

        // power := primary ('**' unary)?
        private double power() {
            double base = primary();
            if (accept("**")) {
                return Math.pow(base, unary());
            }
            return base;
        }

        // primary := number | variable | function '(' args ')' | '(' expression ')'
        private double primary() {
            skipWhitespace();
            if (pos >= src.length()) {
                throw error("Unexpected end of formula");
            }
            char c = src.charAt(pos);
            if (accept("(")) {
                double value = expression();
                expect(")");
                return value;
            }
            if (Character.isDigit(c) || c == '.') {
                return number();
            }
            if (Character.isLetter(c) || c == '_') {
                String name = identifier();
                if (accept("(")) {
                    return call(name, arguments());
                }
                return switch (name) {
                    case "x" -> x;
                    case "y" -> y;
                    default -> throw error("Unknown variable: " + name);
                };
            }
            throw error("Unexpected '" + c + "'");
        }

        private List<Double> arguments() {
            List<Double> args = new ArrayList<>();
            if (accept(")")) {
                return args;
            }
            do {
                args.add(expression());
            } while (accept(","));
            expect(")");
            return args;
        }

        private double call(String name, List<Double> args) {
            return switch (name) {
                case "abs" -> Math.abs(single(name, args));
                case "sqrt" -> Math.sqrt(single(name, args));
                case "sin" -> Math.sin(single(name, args));
                case "cos" -> Math.cos(single(name, args));
                case "log" -> {
                    if (args.size() == 1) {
                        yield Math.log(args.get(0));
                    }
                    if (args.size() == 2) {
                        yield Math.log(args.get(0)) / Math.log(args.get(1));
                    }
                    throw error("log() takes 1 or 2 arguments");
                }
                case "min" -> atLeastTwo(name, args).stream().mapToDouble(Double::doubleValue).min().orElseThrow();
                case "max" -> atLeastTwo(name, args).stream().mapToDouble(Double::doubleValue).max().orElseThrow();
                default -> throw error("Unknown function: " + name);
            };
        }

        private double single(String name, List<Double> args) {
            if (args.size() != 1) {
                throw error(name + "() takes exactly 1 argument");
            }
            return args.get(0);
        }

        private List<Double> atLeastTwo(String name, List<Double> args) {
            if (args.size() < 2) {
                throw error(name + "() needs at least 2 arguments");
            }
            return args;
        }

        private double number() {
            int start = pos;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
                pos++;
            }
            if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
                int expStart = pos++;
                if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                    pos++;
                }
                if (pos >= src.length() || !Character.isDigit(src.charAt(pos))) {
                    pos = expStart;
                    throw error("Malformed number");
                }
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                    pos++;
                }
            }
            String text = src.substring(start, pos);
            if (text.equals(".") || text.indexOf('.') != text.lastIndexOf('.')) {
                throw error("Malformed number: " + text);
            }
            return Double.parseDouble(text);
        }

        private String identifier() {
            int start = pos;
            while (pos < src.length()
                    && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
                pos++;
            }
            return src.substring(start, pos);
        }

        private boolean accept(String token) {
            skipWhitespace();
            if (src.startsWith(token, pos)) {
                pos += token.length();
                return true;
            }
            return false;
        }

        private void expect(String token) {
            if (!accept(token)) {
                throw error("Expected '" + token + "'");
            }
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        private void enter() {
            if (++depth > MAX_NESTING) {
                throw error("Formula is nested too deeply");
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at position " + pos);
        }
    }
}
