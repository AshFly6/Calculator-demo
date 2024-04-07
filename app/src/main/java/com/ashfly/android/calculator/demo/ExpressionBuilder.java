package com.ashfly.android.calculator.demo;

//历史版本：
//版本1：使用CalculatePhase，但只有支持一个运算符的算式
//版本2：更新CalculatePhase，使之支持只包括同一级运算符的算式，
//同时用List<CalculatorPhase>和List<Character> operators
//将所有片段连接起来并计算
//当前是版本3：直接创建算式整体来计算

import android.util.*;

import java.math.*;
import java.util.*;

public class ExpressionBuilder {

    public static final List<Character> BASIC_OPERATORS = Arrays.asList('+', '-', '×', '÷');
    public static final List<Character> DIGIT_CHARS = Arrays.asList('1', '2', '3', '4', '5', '6', '7', '8', '9', '0');
    public static final List<Character> SEPARATE_CHARS = Arrays.asList('%', '(', ')', 'e', 'π'); //这些字符总会独占一个位置
    public static final List<String> MATH_FUNCTIONS = Arrays.asList("sin", "cos", "tan", "ln", "lg");

    public static final char EMPTY_CHAR = '\u0000';
    public static final String TAG = "ExpressionBuilder";
    private static final double RATIO_TO_RAD = Math.PI / 180;

    /**
     * numberBuilders:  0      1       2       3       4      5       ...
     * operators:           0      1       2       3       4       ...
     */
    private final List<StringBuilder> numberBuilders = new ArrayList<>();
    private final List<Character> operators = new ArrayList<>();

    private int index = 0, unmatchedLeftBracket = 0;

    private static double parseNumber(String num) {
        if (num.equals("π"))
            return Math.PI;
        if (num.equals("e"))
            return Math.E;

        for (char c : num.toCharArray()) {
            if (DIGIT_CHARS.contains(c)) {
                try {
                    return Double.parseDouble(num);
                } catch (NumberFormatException e) {
                    break;
                }
            }
        }

        if (num.startsWith("+"))
            return Double.POSITIVE_INFINITY;
        if (num.startsWith("-"))
            return Double.NEGATIVE_INFINITY;
        return Double.NaN;
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

        if (BASIC_OPERATORS.contains(c))
            return appendBasicOperator(c, builder);
        if (c == '%')
            return appendPercent(c, builder);
        if (c == 'e' || c == 'π')
            return appendPIorE(c, builder);

        if (builder.length() == 1) {
            char c1 = builder.charAt(0);
            if (c1 == '√' || SEPARATE_CHARS.contains(c1))
                builder = createNewBuilder(EMPTY_CHAR);
        }

        if (DIGIT_CHARS.contains(c))
            return appendDigitChar(c, builder);
        if (c == '.')
            return appendDot(c, builder);

        Log.d(TAG, builder.toString());
        return false;
    }

    //适时添加左括号或右括号，并将添加的括号返回
    public char appendBracket() {
        StringBuilder builder = tryGetCurrentBuilder();
        int length = builder.length();
        char bracket;

        if (length == 0)
            bracket = '(';
        else {
            String s = builder.toString();
            if (s.equals(".") || s.equals("+.") || s.equals("-."))
                return EMPTY_CHAR;

            if (s.equals("+") || s.equals("-") || s.equals("("))
                bracket = '(';
            else
                bracket = unmatchedLeftBracket > 0 ? ')' : '(';

            builder = createNewBuilder(EMPTY_CHAR);
        }

        appendBracket(builder, bracket);

        Log.d(TAG, builder.toString());
        return bracket;
    }

    private void appendBracket(StringBuilder builder, char bracket) {
        if (bracket == '(') {
            unmatchedLeftBracket++;
        } else if (bracket == ')')
            unmatchedLeftBracket--;
        else
            return;

        builder.append(bracket);
    }

    public boolean appendFunction(String function) {
        if (!MATH_FUNCTIONS.contains(function) && !function.equals("√"))
            return false;

        StringBuilder builder = tryGetCurrentBuilder();
        if (builder.length() > 0)
            builder = createNewBuilder(EMPTY_CHAR);

        builder.append(function);

        if (!function.equals("√"))
            appendBracket(createNewBuilder(EMPTY_CHAR), '(');
        return true;
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
            else if (back == '(') {
                unmatchedLeftBracket--;

                if (index < numberBuilders.size()) {
                    StringBuilder stringBuilder = numberBuilders.get(index);
                    if (MATH_FUNCTIONS.contains(stringBuilder.toString())) {
                        back = stringBuilder.charAt(0);
                        stringBuilder.delete(0, stringBuilder.length());  // sin  (   3
                    }
                }
            }

            Log.d(TAG, builder.toString());
            return back;
        }

        //length = 0...
        Log.d(TAG, builder.toString());
        numberBuilders.remove(index);
        if (index > 0) {
            index--;
            return operators.remove(index);
        }
        return EMPTY_CHAR;
    }

    public void clear() {
        numberBuilders.clear();
        operators.clear();
        index = 0;
    }

    public double calculate(boolean isRad) {
        int size = numberBuilders.size();
        if (size == 0)
            return 0;

        tryGetCurrentBuilder();

        List<String> list = new ArrayList<>(size);
        for (StringBuilder builder : numberBuilders) {
            list.add(builder.toString());
        }

        List<Character> operators = new ArrayList<>(this.operators);
        for (int i = size - 1; i >= 0; i--) {
            String number = list.get(i);
            if (!number.equals("") && !number.equals("(") && !number.equals("+") &&
                    !number.equals("-") && !MATH_FUNCTIONS.contains(number) && !number.equals("√")) {
                break;
            }
            list.remove(i);
            if (i > 0)
                operators.remove(i - 1);
            size--;
        }

        if (size == 0)
            return 0;

        List<Double> numbers = new ArrayList<>();
        List<Character> numberOperators = new ArrayList<>();

        double numberResult;

        int scopeStart, scopeEnd;
        boolean hasLeftBracket, hasRightBracket;
        while (true) {
            //进入了一对括号
            scopeStart = list.lastIndexOf("(") + 1;
            hasLeftBracket = scopeStart > 0;

            scopeEnd = indexOfRange(list, ")", scopeStart) - 1;
            hasRightBracket = scopeEnd > 0;
            if (!hasRightBracket)
                scopeEnd = list.size() - 1;

            //将括号里的数字和运算符提取出来
            numbers.clear();
            numberOperators.clear();

            for (int indexEntirely = scopeStart; indexEntirely <= scopeEnd; indexEntirely++) {
                String num = list.get(indexEntirely);

                if (indexEntirely != scopeEnd)
                    numberOperators.add(operators.get(indexEntirely));

                int indexInsideScope = indexEntirely - scopeStart;

                if (num.equals("%")) {
                    // 1    %   2   ->    0.01   2
                    //   空   +                +

                    list.remove(indexEntirely);

                    indexEntirely--;
                    operators.remove(indexEntirely);
                    scopeEnd--;

                    indexInsideScope--;
                    numberOperators.remove(indexInsideScope);

                    double divided = numbers.get(indexInsideScope) / 100;
                    numbers.set(indexInsideScope, divided);
                    list.set(indexEntirely, String.valueOf(divided));

                } else if (num.equals("√") || MATH_FUNCTIONS.contains(num)) {
                    //cos   1  ->
                    //    空
                    list.remove(indexEntirely);
                    operators.remove(indexEntirely);
                    numberOperators.remove(indexInsideScope);

                    scopeEnd--;

                    double thisNum = parseNumber(list.get(indexEntirely));

                    switch (num) {
                        case "√":
                            thisNum = Math.sqrt(thisNum);
                            break;
                        case "sin":
                            thisNum = Math.sin(isRad ? thisNum : thisNum * RATIO_TO_RAD);
                            break;
                        case "cos":
                            thisNum = Math.cos(isRad ? thisNum : thisNum * RATIO_TO_RAD);
                            break;
                        case "tan":
                            if (!isRad)
                                thisNum = thisNum * RATIO_TO_RAD;
                            if ((thisNum - Math.PI / 2) % Math.PI == 0)
                                throw new ArithmeticException(String.valueOf(R.string.beyond_define_domain));

                            if (thisNum % (Math.PI / 2) == 0)
                                thisNum = 0;
                            else
                                thisNum = Math.tan(thisNum);
                            break;
                        case "ln":
                            thisNum = Math.log(thisNum);
                            break;
                        case "lg":
                            thisNum = Math.log10(thisNum);
                            break;

                    }

                    list.set(indexEntirely, String.valueOf(thisNum));
                    numbers.add(thisNum);
                    if (indexEntirely != scopeEnd)
                        numberOperators.add(operators.get(indexEntirely));
                } else {
                    numbers.add(parseNumber(num));
                }
            }

            //先乘除,用前后两个数字的乘积或比值替换原来的两个数字
            for (int i = 0; i < numberOperators.size(); i++) {

                char operator = numberOperators.get(i);
                if (operator == EMPTY_CHAR)
                    operator = '×';
                if (operator == '×' || operator == '÷') {
                    double thisNumber = numbers.get(i);
                    double nextNumber = numbers.get(i + 1);

                    if (Double.isNaN(thisNumber) || Double.isInfinite(thisNumber))
                        thisNumber = convertInfinityToNormal(thisNumber, 1);
                    if (Double.isNaN(nextNumber) || Double.isInfinite(nextNumber))
                        nextNumber = convertInfinityToNormal(nextNumber, 1);

                    double value;
                    if (operator == '×')
                        value = thisNumber * nextNumber;
                    else {
                        if (nextNumber == 0)
                            throw new ArithmeticException(String.valueOf(R.string.cannot_divide_by_zero));
                        value = thisNumber / nextNumber;
                    }

                    numbers.set(i, value);
                    numbers.remove(i + 1);
                    numberOperators.remove(i);
                    i--;
                }
            }

            //再加减
            double result = 0;

            if (numbers.size() > 0) {
                result = numbers.get(0);
                for (int i = 0; i < numberOperators.size(); i++) {
                    char operator = numberOperators.get(i);
                    result = operator == '+' ? result + numbers.get(i + 1) : result - numbers.get(i + 1);
                }
            }

            //将最后的结果放回，并删除多余位置
            if (scopeStart < list.size())
                list.set(scopeStart, String.valueOf(result));
            numberResult = result;

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
        return numberResult;
    }

    private double convertInfinityToNormal(double orign, double base) {
        if (orign < 0)
            return -base;
        return base;
    }

    public double getNumber(int index) {
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

        if (index == numbersSize)
            numberBuilders.add(new StringBuilder());

        if (numberBuilders.size() != operators.size() + 1)
            throw new IllegalStateException();

        return numberBuilders.get(index);
    }

    private StringBuilder createNewBuilder(char operator) {
        operators.add(operator);
        numberBuilders.add(new StringBuilder());
        index++;
        return tryGetCurrentBuilder();
    }

    private boolean appendPercent(char c, StringBuilder builder) {
        //百分号实际上不得单独出现
        int length = builder.length();

        for (int i = length - 1; i >= 0; i--) {
            char lastChar = builder.charAt(i);
            if (lastChar != '(' && (SEPARATE_CHARS.contains(lastChar) || DIGIT_CHARS.contains(lastChar))) {
                builder = createNewBuilder(EMPTY_CHAR);
                builder.append(c);
                return true;
            }

        }

        return false;
    }

    private boolean appendDot(char c, StringBuilder builder) {
        //无意义的0可以省略， 例如 .9=0.9
        int length = builder.length();
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

    private boolean appendPIorE(char c, StringBuilder builder) {
        if (builder.length() > 0)
            builder = createNewBuilder(EMPTY_CHAR);
        builder.append(c);
        return true;
    }

    private boolean appendDigitChar(char c, StringBuilder builder) {
        int length = builder.length();
        if (length == 0) {
            builder.append(c);
            return true;
        }

        char lastChar = builder.charAt(length - 1);

        //不得在数字开头加多余的0
        if (length == 1 && lastChar == '0')
            return false;


        if (length > 1 && builder.charAt(0) == '+' || builder.charAt(0) == '-')
            length--;

        int place = length - builder.indexOf(".") - 1;
        if (place < 0)
            place = length;

        if (place >= 12)
            return false;

        builder.append(c);
        return true;
    }

    private boolean appendBasicOperator(char c, StringBuilder builder) {
        int length = builder.length();
        if (length == 0) {
            if (c != '+' && c != '-') {
                return false;
            }
            builder.append(c);
            return true;
        }

        for (int i = length - 1; i >= 0; i--) {
            char lastChar = builder.charAt(i);
            if (DIGIT_CHARS.contains(lastChar) || lastChar == 'π' || lastChar == ')' || lastChar == '%') {
                createNewBuilder(c);
                return true;
            }
            if ((lastChar == '√' || lastChar == '(') && (c == '+' || c == '-')) {
                builder = createNewBuilder(EMPTY_CHAR);
                builder.append(c);
                return true;
            }
        }

        return false;
    }

}
