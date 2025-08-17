package com.ashfly.android.calculator.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private static final double DELTA = 1e-6;
    private final boolean isRad = false; // 使用角度制

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.ashfly.android.calculator.demo", appContext.getPackageName());
    }

    @Test
    public void parseEquation() {
        long start = System.currentTimeMillis();
        System.out.println(start);
        testInvalidExpressions();
        testSimpleExpressions();
        testComplexExpressions();
        testSpecialCases();
        testScientificNotation();
        testMixedExpressions();
        System.out.println(System.currentTimeMillis() - start);
    }

    @Test
    public void testInvalidExpressions() {
        String[] invalid = {
                // 1. 语法错误
                "3+++5",
                "2..3",

                // 2. 函数错误
                "sin6",    // 缺少括号
                "inv(30)",// 无效函数名
        };

        for (String expr : invalid) {
            EquationBuilder builder = EquationBuilder.Parser.parseEquation(expr);
            assertNull("应拒绝无效表达式: " + expr, builder);
        }
    }

    @Test
    public void testSimpleExpressions() {
        Object[][] cases = {
                // 1. 基本运算
                {"3+5", 8.0},
                {"7-4", 3.0},
                {"6×3", 18.0},
                {"15÷3", 5.0},

                // 2. 常数处理
                {"π×2", 2 * Math.PI},
                {"e+1", Math.E + 1},
                {"E+1", Math.E + 1},

                // 3. 科学计数法
                {"2E3", 2000.0},
                {"1.23e4", 12300.0},
                {"5e-2", 11.591409142295225},
                {"5E-2", 5e-2}
        };

        for (Object[] testCase : cases) {
            String expr = (String) testCase[0];
            double expected = (Double) testCase[1];

            EquationBuilder builder = EquationBuilder.Parser.parseEquation(expr);
            assertNotNull("应成功解析: " + expr, builder);

            double result = builder.calculate(isRad);
            assertEquals("结果不匹配: " + expr, expected, result, DELTA);
        }
    }

    @Test
    public void testComplexExpressions() {
        Object[][] cases = {
                // 1. 基础函数组合
                {"sin(30)+cos(60)×tan(45)", 1.0},
                {"ln(e^2)+lg(100)×π", 2 + 2 * Math.PI},

                // 2. 嵌套函数
                {"sin(cos-1(0.5))×√16", Math.sin(Math.toRadians(60)) * 4},
                {"√(sin(90)^2+cos(0)^2)", 1.4142135623730951},

                // 3. 混合运算
                {"((3+2)!÷(10-5)!+exp(0)", 2.0}, // 5!÷5! + 1 = 1 + 1 = 2? 修正为1+1=2
                {"2^(3!−4)×5÷2", Math.pow(2, 6 - 4) * 5 / 2}, // 2^2×2.5=4×2.5=10

                // 4. 边界值测试
                {"sin(0.001)", Math.sin(Math.toRadians(0.001))},
                {"9999999×0.0000001", 0.9999999},
                {"(0.1+0.2)×10", 3.0} // 验证浮点精度处理
        };

        for (Object[] testCase : cases) {
            String expr = (String) testCase[0];
            double expected = (Double) testCase[1];

            EquationBuilder builder = EquationBuilder.Parser.parseEquation(expr);
            assertNotNull("应成功解析: " + expr, builder);

            double result = builder.calculate(isRad);
            assertEquals("结果不匹配: " + expr, expected, result, DELTA);
        }
    }

    @Test
    public void testSpecialCases() {
        // 括号智能添加
        testExpression("(3+2", 5.0); // 自动补全括号
        // 函数别名
        testExpression("arcsin(0.5)", Math.toDegrees(Math.asin(0.5)));
        testExpression("asin(0.5)", Math.toDegrees(Math.asin(0.5)));
        testExpression("log(100)", 2.0);

        // 运算符兼容性
        testExpression("3x4", 12.0);    // x → ×
        testExpression("5/2", 2.5);     // / → ÷
        testExpression("3 * 2", 6.0);     // * → ×
    }

    @Test
    public void testScientificNotation() {
        // 大写E科学计数法
        testExpression("1.23E-3", 0.00123);
        testExpression("5E2", 500.0);

        // 小写e自然常数
        testExpression("1.23e-3", 1.23 * Math.E - 3); // 1.23×e - 3
        testExpression("2.5e+2", 2.5 * Math.E + 2);   // 2.5×e + 2

        // 纯自然常数
        testExpression("e", Math.E);
        testExpression("2e", 2 * Math.E);
        testExpression("1.5xe5", 1.5 * Math.E * 5);
        testExpression("1.5x+E5", 1.5 * Math.E * 5);
    }

    @Test
    public void testMixedExpressions() {
        // 自然常数与科学计数法共存
        testExpression("e + 2E3", Math.E + 2000);
        testExpression("sin(e) × 10E-2", Math.sin(Math.toRadians(Math.E)) * (10e-2));

        // 复杂表达式
        testExpression("(1.5e-2) ÷ (3E-3)", (1.5 * Math.E - 2) / 0.003);
    }

    private void testExpression(String expr, double expected) {
        EquationBuilder builder = EquationBuilder.Parser.parseEquation(expr);
        assertNotNull("应成功解析: " + expr, builder);
        double result = builder.calculate(isRad);
        assertEquals("结果不匹配: " + expr, expected, result, DELTA);
    }
}