package com.ashfly.android.calculator.demo;

//历史版本：
//版本1：使用CalculatePhase，但只有支持一个运算符的算式
//版本2：更新CalculatePhase，使之支持只包括同一级运算符的算式，
//同时用List<CalculatorPhase>和List<Character> operators
//将所有片段连接起来并计算
//当前是版本3：直接创建算式整体来计算

import java.math.*;
import java.util.*;

public class ExpressionBuilder {

    public static final List<Character> BASIC_OPERATORS = Arrays.asList('+', '-', '×', '÷');
    public static final List<Character> DIGIT_CHARS = Arrays.asList('1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'e', 'π');
    public static final char EMPTY_CHAR = '\u0000';
    public static final int DIVISION_SCALE = 12;

    /**
     *  numberBuilders:  0      1       2       3       4      5       ...
     *  operators:           0      1       2       3       4       ...
     */
    private final List<StringBuilder> numberBuilders = new ArrayList<>();
    private final List<Character> operators = new ArrayList<>();

    private int index = 0, unmatchedLeftBracket = 0;

    private static BigDecimal parseNumber(String num) {
        int length = num.length();
        if (length == 0 ||
                (length == 1 && (num.charAt(0) < '0' || num.charAt(0) > '9'))) //空数字
            return BigDecimal.valueOf(0);


        //更多的处理，TODO()
        return new BigDecimal(num);
    }

    private static <E> int indexOfRange(List<E> list, E element, int start) {
        int end = list.size();
        if (element == null)
            for (int i = start; i < end; i++) {
                if (list.get(i) == null)
                    return i;
            }
        else
            for (int i = start; i < end; i++) {
                if (list.get(i).equals(element))
                    return i;
            }
        return -1;
    }

    public boolean append(char c) {
        StringBuilder builder = tryGetCurrentBuilder();
        int length = builder.length();

        if (BASIC_OPERATORS.contains(c))
            return appendBasicOperator(c, builder, length);

        if (DIGIT_CHARS.contains(c))
            return appendDigitChar(c, builder, length);

        if (c == '.')
            return appendDot(c, builder, length);

        if (c == '%')
            return appendPercent(c, builder, length);

        return false;
    }

    //添加左括号或右括号
    public char appendBracket() {
        StringBuilder builder = tryGetCurrentBuilder();
        int length = builder.length();
        char bracket = EMPTY_CHAR;

        if (length == 0)
            bracket = '(';
        else {
            if (length == 1) {
                char lastChar = builder.charAt(0);
                if (lastChar == '+' || lastChar == '-') {
                    bracket = '(';
                }
            }

            createNewBuilder(EMPTY_CHAR);
            builder = tryGetCurrentBuilder();

            if (bracket == EMPTY_CHAR)
                bracket = unmatchedLeftBracket > 0 ? ')' : '(';

        }

        builder.append(bracket);

        if (bracket == '(') {
            unmatchedLeftBracket++;
            createNewBuilder(EMPTY_CHAR);
        } else
            unmatchedLeftBracket--;

        return bracket;
    }

    public char backspace() {
        if (numberBuilders.isEmpty())
            return EMPTY_CHAR;

        StringBuilder builder = tryGetCurrentBuilder();
        int length = builder.length();
        if (length > 0) {
            char back = builder.charAt(length - 1);
            builder.deleteCharAt(length - 1);
            length--;

            if (length == 0 && index > 0 && operators.get(index - 1) == EMPTY_CHAR) {
                numberBuilders.remove(index);

                index--;
                operators.remove(index);
            }

            if (back == ')')
                unmatchedLeftBracket++;
            else if (back == '(')
                unmatchedLeftBracket--;

            return back;
        }

        //length = 0...
        numberBuilders.remove(index);
        if (index > 0) {
            index--;
            return operators.remove(index);
        }
        return EMPTY_CHAR;
    }

    public BigDecimal calculate() {
        int size = numberBuilders.size();
        if (size == 0)
            return BigDecimal.valueOf(0);

        tryGetCurrentBuilder();

        List<String> list = new ArrayList<>(size);
        for (StringBuilder builder : numberBuilders) {
            list.add(builder.toString());
        }

        List<Character> operators = new ArrayList<>(this.operators);
        for (int i = size - 1; i >= 0; i--) {
            String number = list.get(i);
            if (!number.equals("") && !number.equals("(") && !number.equals("+") && !number.equals("-")) {
                break;
            }
            list.remove(i);
            if (i > 1)
                operators.remove(i - 1);
            size--;
        }

        if (size == 0)
            return BigDecimal.valueOf(0);

        List<BigDecimal> decimals = new ArrayList<>();
        List<Character> decimalOperators = new ArrayList<>();

        BigDecimal decimalResult;

        int scopeStart, scopeEnd;
        boolean hasLeftBracket, hasRightBracket;
        while (true) {
            //进入了一对括号
            scopeStart = list.lastIndexOf("(") + 1;
            hasLeftBracket = scopeStart != 0;

            scopeEnd = indexOfRange(list, ")", scopeStart) - 1;
            hasRightBracket = scopeEnd > 0;
            if (!hasRightBracket)
                scopeEnd = list.size() - 1;

            //将括号里的数字和运算符提取出来
            decimals.clear();
            decimalOperators.clear();

            for (int indexEntirely = scopeStart; indexEntirely <= scopeEnd; indexEntirely++) {
                String num = list.get(indexEntirely);

                //去除百分号
                if (num.equals("%")) {
                    list.remove(indexEntirely);
                    scopeEnd--;

                    //把百分号后的运算符移动到百分号前的数字的后边，即百分号前的位置
                    int indexInsideScope = indexEntirely - scopeStart;
                    if (indexInsideScope == scopeEnd - scopeStart + 1) {
                        indexEntirely--;
                        indexInsideScope--;
                        decimalOperators.remove(indexInsideScope);
                        operators.remove(indexEntirely);
                    } else {
                        char operator = operators.get(indexEntirely);
                        operators.remove(indexEntirely);
                        indexEntirely--;
                        indexInsideScope--;
                        decimalOperators.set(indexInsideScope, operator);
                        operators.set(indexEntirely, operator);
                    }

                    BigDecimal divided = decimals.get(indexInsideScope).divide(BigDecimal.valueOf(100), DIVISION_SCALE, RoundingMode.DOWN);
                    decimals.set(indexInsideScope, divided);
                    list.set(indexEntirely, divided.toString());
                } else {
                    decimals.add(parseNumber(num));
                    if (indexEntirely != scopeEnd)
                        decimalOperators.add(operators.get(indexEntirely));
                }
            }

            //先乘除,用前后两个数字的乘积或比值替换原来的两个数字
            for (int i = 0; i < decimalOperators.size(); i++) {

                char operator = decimalOperators.get(i);
                if (operator == EMPTY_CHAR)
                    operator = '×';
                if (operator == '×' || operator == '÷') {
                    BigDecimal value = operator == '×' ?
                            decimals.get(i).multiply(decimals.get(i + 1)) :
                            decimals.get(i).divide(decimals.get(i + 1), DIVISION_SCALE, RoundingMode.DOWN);

                    decimals.set(i, value);
                    decimals.remove(i + 1);
                    decimalOperators.remove(i);
                    i--;
                }
            }

            //再加减
            BigDecimal result = decimals.get(0);
            for (int i = 0; i < decimalOperators.size(); i++) {
                char operator = decimalOperators.get(i);
                if (operator == '+')
                    result = result.add(decimals.get(i + 1));
                else
                    result = result.subtract(decimals.get(i + 1));
            }

            //将最后的结果放回,并删除多余位置
            if (scopeStart < list.size())
                list.set(scopeStart, result.toString());
            decimalResult = result;

            //删掉右括号
            if (hasRightBracket) {
                list.remove(scopeEnd + 1);
                operators.remove(scopeEnd);
            }

            //删掉括号里的内容
            for (int i = scopeStart + 1; i <= scopeEnd; i++) {
                list.remove(i);
                operators.remove(i - 1);
                i--;
                scopeEnd--;
            }

            //0=-1+1,表明所有括号内容都计算完毕
            if (!hasLeftBracket)
                break;

            //删掉左括号
            list.remove(scopeStart - 1);
            operators.remove(scopeStart - 1);
        }
        return decimalResult;
    }

    public BigDecimal getNumber(int index) {
        return parseNumber(numberBuilders.get(index).toString());
    }

    public String getNumberString(int index) {
        return numberBuilders.get(index).toString();
    }

    public int getNumberCount() {
        return numberBuilders.size();
    }

    private StringBuilder tryGetCurrentBuilder() {
        int numbersSize = numberBuilders.size();
        if (index > numbersSize)
            throw new IndexOutOfBoundsException();

        if (index == numbersSize) {
            numberBuilders.add(new StringBuilder());
        }

        if (numberBuilders.size() != operators.size() + 1)
            throw new IllegalStateException();

        return numberBuilders.get(index);
    }

    private void createNewBuilder(char operator) {
        operators.add(operator);
        numberBuilders.add(new StringBuilder());
        index++;
    }

    private boolean appendPercent(char c, StringBuilder builder, int length) {
        //百分号不得单独出现
        if (length == 0)
            return false;

        if (length == 1) {
            char lastChar = builder.charAt(0);
            if (lastChar != '%' && lastChar != ')' && !DIGIT_CHARS.contains(lastChar))
                return false;
        }
        createNewBuilder(EMPTY_CHAR);
        builder = tryGetCurrentBuilder();
        builder.append(c);
        return true;
    }

    private boolean appendDot(char c, StringBuilder builder, int length) {
        //无意义的0可以省略， 例如 .9=0.9
        if (length == 0) {
            builder.append(c);
            return true;
        }

        //一个数最多有一个小数点
        if (builder.toString().contains("."))
            return false;

        //数字后可以跟小数点
        char lastChar = builder.charAt(length - 1);
        if (lastChar >= '0' && lastChar <= '9') {
            builder.append(c);
            return true;
        }

        //无意义的0可以省略, 例如 -.9=-0.9
        if (lastChar == '+' || lastChar == '-') {
            builder.append(c);
            return true;
        }

        return false;
    }

    private boolean appendDigitChar(char c, StringBuilder builder, int length) {
        if (length == 0) {
            builder.append(c);
            return true;
        }

        char lastChar = builder.charAt(length - 1);

        if (lastChar == ')' || lastChar == '%') {
            createNewBuilder(EMPTY_CHAR);
            return append(c);
        }

        //不得在数字开头加多余的0
        if (length == 1 && lastChar == '0')
            return false;

        builder.append(c);
        return true;
    }

    private boolean appendBasicOperator(char c, StringBuilder builder, int length) {
        if (length == 0) {
            //正号和负号必须位于一个数的开头
            if (c != '+' && c != '-') {
                return false;
            }
            builder.append(c);
            return true;
        }
        if (length == 1) {
            char lastChar = builder.charAt(0);
            if (lastChar != '%' && lastChar != ')' && !DIGIT_CHARS.contains(lastChar))
                return false;
        }
        createNewBuilder(c);
        return true;
    }

}
