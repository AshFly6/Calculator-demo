package com.ashfly.android.calculator.demo;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Supplier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 构造和计算算式
 */
public class EquationBuilder implements Parcelable {

    public static final List<Character> DIGIT_CHARS = Arrays.asList('1', '2', '3', '4', '5', '6', '7', '8', '9', '0');

    public static final List<Character> BASIC_OPERATORS = Arrays.asList('+', '-', '×', '÷', '^');
    public static final List<Character> ADVANCED_OPERATORS = Arrays.asList('!', '%');

    public static final List<Character> SEPARATE_CHARS = Arrays.asList('(', ')', 'e', 'π'); //这些字符总会独占一个位置
    public static final List<String> MATH_FUNCTIONS = Arrays.asList("sin-1", "cos-1", "tan-1", "sin", "cos", "tan", "ln", "lg", "exp", "√");

    public static final char EMPTY_CHAR = '\u0000';
    public static final String TAG = "ExpressionBuilder";

    public static final Creator<EquationBuilder> CREATOR = new Creator<EquationBuilder>() {
        @Override
        public EquationBuilder createFromParcel(Parcel in) {
            return new EquationBuilder(in);
        }

        @Override
        public EquationBuilder[] newArray(int size) {
            return new EquationBuilder[size];
        }
    };
    /**
     * numberBuilders:  0      1       2       3       4      5       ...
     * operators:           0      1       2       3       4       ...
     */
    private final List<StringBuilder> numberBuilders = new ArrayList<>();
    private final List<Character> operators = new ArrayList<>();
    private int index = 0, unmatchedLeftBracket = 0;

    public EquationBuilder() {
    }

    protected EquationBuilder(Parcel in) {
        List<String> list = new ArrayList<>();
        in.readStringList(list);
        for (String s : list)
            numberBuilders.add(new StringBuilder(s));

        list.clear();
        in.readStringList(list);
        for (String s : list)
            operators.add(s.charAt(0));

        index = in.readInt();
        unmatchedLeftBracket = in.readInt();
    }

    private static double parseNumber(String num) {
        if (num.equals("π"))
            return Math.PI;
        if (num.equals("e"))
            return Math.E;

        try {
            return Double.parseDouble(num);
        } catch (NumberFormatException ignored) {
        }

        if (num.startsWith("+"))
            return Double.POSITIVE_INFINITY;
        if (num.startsWith("-"))
            return Double.NEGATIVE_INFINITY;
        return Double.NaN;
    }

    /**
     * @noinspection SameParameterValue
     */
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

    private static void checkInfiniteAndNaN(double thisNum) {
        if (Double.isInfinite(thisNum))
            throw new CalculateException("Too gigantic!", R.string.value_too_gigantic);
        if (Double.isNaN(thisNum)) {
            throw new CalculateException("Occur NaN!", R.string.NaN);
        }
    }

    public boolean appendChar(char c) {
        StringBuilder builder = tryGetCurrentBuilder();

        if (BASIC_OPERATORS.contains(c))
            return appendBasicOperator(c, builder);
        if (c == '%' || c == '!')
            return appendEndingFunction(c, builder);
        if (c == 'e' || c == 'π')
            return appendPIorE(c, builder);
        if (c == '√')
            return appendLeadingFunction(String.valueOf(c));


        boolean needNewBuilder = false;
        if (builder.length() == 1) {
            char firstChar = builder.charAt(0);
            if (firstChar == '√' || ADVANCED_OPERATORS.contains(firstChar) || SEPARATE_CHARS.contains(firstChar))
                needNewBuilder = true;
        }

        if (!needNewBuilder) {
            int length = builder.length();
            if (length > 1 && (builder.charAt(0) == '+' || builder.charAt(0) == '-'))
                length--;
            if (length >= 15)
                return false;
        }

        if (DIGIT_CHARS.contains(c))
            return appendDigitChar(c, needNewBuilder ? createNewBuilder(EMPTY_CHAR) : builder);
        if (c == '.')
            return appendDot(c, needNewBuilder ? createNewBuilder(EMPTY_CHAR) : builder);

        Log.d(TAG, toString());
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

        Log.d(TAG, toString());
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

    public boolean appendLeadingFunction(String function) {
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

    private boolean appendEndingFunction(char c, StringBuilder builder) {
        int length = builder.length();

        for (int i = length - 1; i >= 0; i--) {
            char lastChar = builder.charAt(i);
            if ((lastChar != '(' && lastChar != '√') &&
                    (ADVANCED_OPERATORS.contains(lastChar) || SEPARATE_CHARS.contains(lastChar) || DIGIT_CHARS.contains(lastChar))) {

                if (c == '!' && builder.charAt(0) == '-')
                    splitNegative(builder);

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
            if ((c == '+' || c == '-') && (lastChar == '√' || lastChar == '(')) {
                builder = createNewBuilder(EMPTY_CHAR);
                builder.append(c);
                return true;
            }
            if (DIGIT_CHARS.contains(lastChar)) {
                if (c == '^' && builder.charAt(0) == '-') {
                    splitNegative(builder);
                }
                createNewBuilder(c);
                return true;
            }
            if (lastChar == '!' || lastChar == 'π' || lastChar == 'e' || lastChar == ')' || lastChar == '%') {
                createNewBuilder(c);
                return true;
            }
        }

        return false;
    }

    private void splitNegative(StringBuilder builder) {
        String s = builder.toString().replace("-", "");
        builder.delete(1, builder.length());
        builder = createNewBuilder(EMPTY_CHAR);
        builder.append(s);
    }

    public char backspace(char displayedLastChar) {
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
                        stringBuilder.delete(0, stringBuilder.length());
                    }
                }
            }

            Log.d(TAG, toString());
            return back;
        }

        //length = 0...
        numberBuilders.remove(index);
        if (index > 0) {
            index--;
            char back = operators.remove(index);
            Log.d(TAG, toString());
            return back == displayedLastChar ? back : backspace(displayedLastChar);
        }

        Log.d(TAG, toString());
        return EMPTY_CHAR;
    }

    public void clear() {
        numberBuilders.clear();
        operators.clear();
        index = 0;
    }

    public double calculate(boolean isRad) {
        int size = this.numberBuilders.size();
        if (size == 0)
            return 0;

        tryGetCurrentBuilder();

        //将numbers和操作符复制一份，不影响外部
        List<String> numbers = new ArrayList<>(size);
        for (StringBuilder builder : this.numberBuilders) {
            numbers.add(builder.toString());
        }

        //把末尾未闭合的符号删掉
        List<Character> operators = new ArrayList<>(this.operators);
        for (int i = size - 1; i >= 0; i--) {
            String number = numbers.get(i);
            if (!number.isEmpty() && !number.equals("(") && !number.equals("+") && !number.equals("-") &&
                    !number.equals("^") && !number.equals("√") && !MATH_FUNCTIONS.contains(number)) {
                break;
            }
            size--;
            numbers.remove(i);
            if (i > 0) {
                operators.remove(i - 1);
            }
        }

        if (size == 0)
            return 0;

        double finalResult;

        boolean hasLeftBracket, hasRightBracket;
        List<Double> scopeNumbers = new ArrayList<>();
        List<Character> scopeOperators = new ArrayList<>();
        int scopeStart, scopeEnd;

        while (true) {
            {
                //进入了一对括号
                scopeStart = numbers.lastIndexOf("(") + 1;
                hasLeftBracket = scopeStart > 0;

                scopeEnd = indexOfRange(numbers, ")", scopeStart) - 1;
                hasRightBracket = scopeEnd > 0;
                if (!hasRightBracket)
                    scopeEnd = numbers.size() - 1;

                scopeNumbers.clear();
                scopeOperators.clear();
            }

            //处理函数
            while (true) {

                int functionStart = -1;
                int functionEnd = -1;
                for (int globalIndex = scopeStart; globalIndex <= scopeEnd; globalIndex++) {
                    String number = numbers.get(globalIndex);

                    if (functionStart < 0) {
                        if (MATH_FUNCTIONS.contains(number)) {
                            functionStart = globalIndex;
                        }
                    } else {
                        double thisNum = parseNumber(number);
                        if (!Double.isNaN(thisNum)) {
                            functionEnd = globalIndex;
                            break;
                        }
                    }

                }

                if (functionStart >= 0 && functionEnd < 0)
                    throw new CalculateException("Format error", R.string.formate_wrong);

                if (functionStart < 0)
                    break;

                for (int globalIndex = functionEnd; globalIndex > functionStart; globalIndex--) {
                    double thisNum = parseNumber(numbers.get(globalIndex));
                    String previousNum = numbers.get(globalIndex - 1);

                    if (!MATH_FUNCTIONS.contains(previousNum))
                        throw new CalculateException(String.format("Cannot calculate %s%s", previousNum, thisNum), R.string.formate_wrong);

                    if (previousNum.contains("-1")) {
                        if (thisNum > 1 || thisNum < -1)
                            throw new CalculateException(String.format("Cannot calculate \"%s%f!\"", previousNum, thisNum), R.string.beyond_define_domain);

                        switch (previousNum) {
                            case "sin-1":
                                thisNum = Math.asin(thisNum);
                                break;

                            case "cos-1":
                                thisNum = Math.acos(thisNum);
                                break;

                            case "tan-1":
                                thisNum = Math.atan(thisNum);
                                break;
                        }

                        if (!isRad)
                            thisNum = Math.toDegrees(thisNum);

                    } else if (previousNum.equals("√")) {
                        if (thisNum < 0)
                            throw new CalculateException(String.format("Cannot calculate \"%s%f!\"", previousNum, thisNum), R.string.beyond_define_domain);

                        thisNum = Math.sqrt(thisNum);

                    } else if (previousNum.startsWith("l")) {
                        if (thisNum <= 0)
                            throw new CalculateException(String.format("Cannot calculate \"%s%f!\"", previousNum, thisNum), R.string.beyond_define_domain);

                        switch (previousNum) {
                            case "ln":
                                thisNum = Math.log(thisNum);
                                break;

                            case "lg":
                                thisNum = Math.log10(thisNum);
                                break;
                        }

                    } else {
                        switch (previousNum) {
                            case "exp":
                                thisNum = Math.exp(thisNum);
                                break;

                            case "sin":
                                thisNum = Math.sin(isRad ? thisNum : Math.toRadians(thisNum));
                                break;

                            case "cos":
                                thisNum = Math.cos(isRad ? thisNum : Math.toRadians(thisNum));
                                break;

                            case "tan":
                                if (!isRad)
                                    thisNum = Math.toRadians(thisNum);
                                if (Math.abs(Math.cos(thisNum)) < 1e-10)
                                    throw new CalculateException(String.format("Cannot calculate \"%s%f!\"", previousNum, thisNum), R.string.beyond_define_domain);

                                thisNum = Math.tan(thisNum);
                                break;

                        }
                    }

                    checkInfiniteAndNaN(thisNum);

                    //globalIndex-1 globalIndex
                    //cos            180         ->  -1
                    //        空
                    numbers.set(globalIndex - 1, String.valueOf(thisNum));
                    numbers.remove(globalIndex);
                    operators.remove(globalIndex - 1);

                    scopeEnd--;
                }
            }


            //将数字和操作符添加进scope，同时处理%和!
            for (int globalIndex = scopeStart; globalIndex <= scopeEnd; globalIndex++) {
                String number = numbers.get(globalIndex);
                int scopeIndex = globalIndex - scopeStart;

                if (globalIndex != scopeEnd)
                    scopeOperators.add(operators.get(globalIndex));

                if ((!number.equals("%") && !number.equals("!"))) {
                    double parseNumber = parseNumber(number);
                    if (Double.isNaN(parseNumber))
                        throw new CalculateException("Format error", R.string.formate_wrong);
                    scopeNumbers.add(parseNumber);
                } else {
                    // 1    %   2   ->    0.01   2
                    //   空   +                +

                    numbers.remove(globalIndex);

                    globalIndex--;
                    scopeIndex--;

                    operators.remove(globalIndex);
                    scopeOperators.remove(scopeIndex);

                    scopeEnd--;

                    double thisNum = scopeNumbers.get(scopeIndex);
                    if (number.equals("%")) {
                        thisNum = thisNum / 100;
                    } else {
                        if (thisNum < 0)
                            throw new CalculateException(String.format("Cannot calculate \"%f!\"", thisNum), R.string.beyond_define_domain);
                        double result = thisNum;
                        if (thisNum == 0)
                            result = 1;
                        else {
                            if (Math.floor(thisNum) != thisNum)
                                throw new CalculateException(String.format("Cannot calculate \"%f!\"", thisNum), R.string.beyond_define_domain);

                            for (long i = (long) (thisNum - 1); i > 0; i--) {
                                result = result * i;
                                checkInfiniteAndNaN(result);
                            }
                        }
                        thisNum = result;
                    }

                    checkInfiniteAndNaN(thisNum);

                    scopeNumbers.set(scopeIndex, thisNum);
                    numbers.set(globalIndex, String.valueOf(thisNum));
                }
            }


            //先乘方
            //2    3    2     ->      2     9    ->    512
            //  ^    ^                   ^
            while (scopeOperators.contains('^')) {
                for (int scopeIndex = scopeOperators.size() - 1; scopeIndex >= 0; scopeIndex--) {
                    char operator = scopeOperators.get(scopeIndex);
                    if (operator == '^') {
                        double thisNumber = scopeNumbers.get(scopeIndex);
                        double nextNumber = scopeNumbers.get(scopeIndex + 1);

                        if (Double.isInfinite(thisNumber))
                            thisNumber = thisNumber > 0 ? 1 : -1;
                        if (Double.isInfinite(nextNumber))
                            nextNumber = nextNumber > 0 ? 1 : -1;

                        double value = thisNumber == 0 && nextNumber == 0 ? Double.NaN :
                                Math.pow(thisNumber, nextNumber);
                        checkInfiniteAndNaN(value);

                        scopeNumbers.set(scopeIndex, value);
                        scopeNumbers.remove(scopeIndex + 1);
                        scopeOperators.remove(scopeIndex);
                    }
                }

            }

            //再乘除
            for (int i = 0; i < scopeOperators.size(); i++) {

                char operator = scopeOperators.get(i);
                if (operator == EMPTY_CHAR)
                    operator = '×';
                if (operator == '×' || operator == '÷') {
                    double thisNumber = scopeNumbers.get(i);
                    double nextNumber = scopeNumbers.get(i + 1);

                    if (Double.isInfinite(thisNumber))
                        thisNumber = thisNumber > 0 ? 1 : -1;
                    if (Double.isInfinite(nextNumber))
                        nextNumber = nextNumber > 0 ? 1 : -1;

                    double value;
                    if (operator == '×') {
                        value = thisNumber * nextNumber;
                    } else {
                        if (nextNumber == 0)
                            throw new CalculateException(String.format("Cannot calculate \"%f%s%f!\"", thisNumber, operator, nextNumber), R.string.cannot_divide_by_zero);
                        value = thisNumber / nextNumber;
                    }

                    checkInfiniteAndNaN(value);

                    scopeNumbers.set(i, value);
                    scopeNumbers.remove(i + 1);
                    scopeOperators.remove(i);
                    i--;
                }

            }

            //最后加减
            double result = 0;

            if (!scopeNumbers.isEmpty()) {
                result = scopeNumbers.get(0);
                for (int i = 0; i < scopeOperators.size(); i++) {
                    char operator = scopeOperators.get(i);
                    result = operator == '+' ? result + scopeNumbers.get(i + 1) : result - scopeNumbers.get(i + 1);
                    checkInfiniteAndNaN(result);
                }
            }

            //将最后的结果放回，并删除多余位置
            checkInfiniteAndNaN(result);
            if (scopeStart < numbers.size())
                numbers.set(scopeStart, String.valueOf(result));

            finalResult = result;

            //删掉右括号
            if (hasRightBracket) {
                numbers.remove(scopeEnd + 1);
                operators.remove(scopeEnd);
            }

            //删掉括号里的内容
            for (int i = scopeStart + 1; i <= scopeEnd; i++) {
                numbers.remove(i);
                operators.remove(i - 1);
                i--;
                scopeEnd--;
            }

            //0=-1+1,表明所有括号内容都计算完毕
            if (!hasLeftBracket)
                break;

            //删掉左括号
            numbers.remove(scopeStart - 1);
            operators.remove(scopeStart - 1);
        }

        return finalResult;
    }

    public double getCurrentNumber() {
        return parseNumber(numberBuilders.get(index).toString());
    }

    public String getCurrentNumberString() {
        return numberBuilders.get(index).toString();
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

    @NonNull
    @Override
    public String toString() {
        return build();
    }

    @NonNull
    public String build() {
        StringBuilder builder = new StringBuilder();
        int size = numberBuilders.size();
        for (int i = 0; i < size; i++) {
            builder.append(numberBuilders.get(i).toString());
            if (i < operators.size()) {
                char character = operators.get(i);
                if (character != EMPTY_CHAR)
                    builder.append(character);
            }
        }
        return builder.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        List<String> list = new ArrayList<>();
        for (StringBuilder builder : numberBuilders) {
            if (builder != null)
                list.add(builder.toString());
        }
        dest.writeStringList(list);

        list.clear();
        for (Character character : operators) {
            if (character != null)
                list.add(character.toString());
        }

        dest.writeStringList(list);
        dest.writeInt(index);
        dest.writeInt(unmatchedLeftBracket);
    }

    public static final class Parser {

        public static EquationBuilder parseEquation(String equation) {
            equation = equation.replace("/", "÷").replace('−', '-')
                    .replace('＋', '+').replace(" ", "");

            char[] chars = equation.toCharArray();
            EquationBuilder equationBuilder = new EquationBuilder();

            for (int index = 0, charsLength = chars.length; index < charsLength; ) {
                int _index = index;
                char c = chars[index];

                index += tryAppendSomeOperators(equationBuilder, new char[]{'x', '*', '×'}, () -> equationBuilder.appendChar('×'), c);
                if (index != _index)
                    continue;

                index += tryAppendSomeOperators(equationBuilder, new char[]{'(', ')'}, () -> equationBuilder.appendBracket() == c, c);
                if (index != _index)
                    continue;

                if (c != 'e' && equationBuilder.appendChar(c)) {
                    index += 1;
                    continue;
                }

                StringBuilder builder = new StringBuilder();

                //arccos(
                int lastIndex = Math.min(chars.length - 1, index + 7);
                for (int i = index; i <= lastIndex; i++) {
                    builder.append(chars[i]);
                }
                String followup = builder.toString();

                int length = followup.length();

                if (followup.startsWith("E") || (
                        followup.startsWith("e") && length >= 2 && DIGIT_CHARS.contains(followup.charAt(1)))) {

                    if (equationBuilder.appendChar('×')) {
                        equationBuilder.appendChar('1');
                        equationBuilder.appendChar('0');
                        equationBuilder.appendChar('^');
                    } else if (!equationBuilder.appendChar('e')) {
                        return null;
                    }
                    index += 1;
                    continue;
                }

                index += tryAppendLeadingFunction(equationBuilder, new String[]{"sin-1(", "arcsin(", "asin("}, "sin-1", followup);
                if (index != _index)
                    continue;

                index += tryAppendLeadingFunction(equationBuilder, new String[]{"cos-1(", "arccos(", "acos("}, "cos-1", followup);
                if (index != _index)
                    continue;

                index += tryAppendLeadingFunction(equationBuilder, new String[]{"tan-1(", "arctan(", "atan("}, "tan-1", followup);
                if (index != _index)
                    continue;

                index += tryAppendLeadingFunction(equationBuilder, new String[]{"sin(", "cos(", "tan(", "exp("}, 3, followup);
                if (index != _index)
                    continue;

                index += tryAppendLeadingFunction(equationBuilder, new String[]{"lg(", "ln("}, 2, followup);
                if (index != _index)
                    continue;

                index += tryAppendLeadingFunction(equationBuilder, new String[]{"log("}, "lg", followup);
                if (index != _index)
                    continue;

                index += tryAppendLeadingFunction(equationBuilder, new String[]{"sqrt(", "√"}, "√", followup);
                if (index != _index)
                    continue;

                if (c == 'e' && equationBuilder.appendChar(c)) {
                    index += 1;
                    continue;
                }

                return null;
            }
            return equationBuilder;

        }

        private static int tryAppendSomeOperators(EquationBuilder builder, char[] chars, Supplier<Boolean> appender, char tryChar) {
            for (char c : chars) {
                if (c == tryChar && appender.get())
                    return 1;
            }
            return 0;
        }

        private static int tryAppendLeadingFunction(EquationBuilder builder, String[] functionGroups,
                                                    String function, String tryFunctionString) {
            for (String f : functionGroups) {
                if (tryFunctionString.startsWith(f)) {
                    builder.appendLeadingFunction(function);
                    return f.length();
                }
            }
            return 0;
        }

        private static int tryAppendLeadingFunction(EquationBuilder builder, String[] functionGroups,
                                                    int length, String tryFunctionString) {
            for (String f : functionGroups) {
                if (tryFunctionString.startsWith(f)) {
                    builder.appendLeadingFunction(tryFunctionString.substring(0, length));
                    return f.length();
                }
            }
            return 0;
        }
    }

    public static final class CalculateException extends ArithmeticException {
        int textResourceId;

        public CalculateException(String message, int textResourceId) {
            super(message);
            this.textResourceId = textResourceId;
        }
    }
}
