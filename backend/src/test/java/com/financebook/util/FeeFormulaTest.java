package com.financebook.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.OptionalDouble;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for the fee formula evaluator. Expected values follow the Python
 * admin panel's safe_eval_formula(), which validates the formulas.
 */
class FeeFormulaTest {

    private static double eval(String formula, double x, double y) {
        OptionalDouble result = FeeFormula.evaluate(formula, x, y);
        assertThat(result).as("formula '%s' should be valid", formula).isPresent();
        return result.getAsDouble();
    }

    @Test
    void evaluatesArithmeticWithVariables() {
        assertThat(eval("x * 0.01 + y", 200, 0.5)).isCloseTo(2.5, within(1e-12));
        assertThat(eval("(x + 10) / 2", 30, 0)).isCloseTo(20, within(1e-12));
        assertThat(eval("x - y - 1", 10, 2)).isCloseTo(7, within(1e-12));
    }

    @Test
    void followsPythonOperatorPrecedence() {
        assertThat(eval("2 + 3 * 4", 0, 0)).isEqualTo(14);
        assertThat(eval("-2**2", 0, 0)).isEqualTo(-4);       // ** binds tighter than unary minus
        assertThat(eval("2**3**2", 0, 0)).isEqualTo(512);    // right-associative
        assertThat(eval("2**-1", 0, 0)).isEqualTo(0.5);
        assertThat(eval("--x", 3, 0)).isEqualTo(3);
    }

    @Test
    void supportsNumberFormats() {
        assertThat(eval(".5 + 5. + 1e2 + 2E-1", 0, 0)).isCloseTo(105.7, within(1e-12));
    }

    @Test
    void supportsTheWhitelistedFunctions() {
        assertThat(eval("abs(-x)", 4, 0)).isEqualTo(4);
        assertThat(eval("sqrt(x)", 16, 0)).isEqualTo(4);
        assertThat(eval("min(x, 5, y)", 10, 2)).isEqualTo(2);
        assertThat(eval("max(x, 5)", 1, 0)).isEqualTo(5);
        assertThat(eval("log(x)", Math.E, 0)).isCloseTo(1, within(1e-12));
        assertThat(eval("log(x, 10)", 1000, 0)).isCloseTo(3, within(1e-12));
        assertThat(eval("sin(0) + cos(0)", 0, 0)).isEqualTo(1);
    }

    @Test
    void divisionByZeroYieldsZero() {
        assertThat(eval("x / (y - y)", 5, 1)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "   ", "x +", "(x", "x)", "z * 2", "x y", "x // 2", "x % 2", "x ^ 2",
            "abs", "abs(1, 2)", "min(1)", "foo(1)", "x(2)", "1..2", "1e", "__import__('os')",
            "sqrt(-1)", "log(0)", "10 ** 400", "x == 1"
    })
    void rejectsInvalidOrNonFiniteFormulas(String formula) {
        assertThat(FeeFormula.evaluate(formula, 1.0, 0.5)).isEmpty();
    }

    @Test
    void rejectsExcessiveNesting() {
        String deep = "(".repeat(5000) + "x" + ")".repeat(5000);
        assertThat(FeeFormula.evaluate(deep, 1.0, 0.5)).isEmpty();
    }
}
