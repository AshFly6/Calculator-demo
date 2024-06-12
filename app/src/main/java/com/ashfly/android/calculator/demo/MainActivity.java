package com.ashfly.android.calculator.demo;

import static com.ashfly.android.calculator.demo.ExpressionBuilder.*;
import static com.ashfly.android.calculator.demo.DigitAdapter.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import android.graphics.*;
import android.os.Bundle;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import java.math.*;
import java.text.*;
import java.util.*;

/** @noinspection UnusedReturnValue*/
public class MainActivity extends AppCompatActivity implements OnItemClickListener {

    private final NumberFormat resultFormat = NumberFormat.getNumberInstance();
    private final NumberFormat originalNumFormat = new DecimalFormat("###0");
    private final NumberFormat expressionFormat = NumberFormat.getNumberInstance();

    private final ExpressionBuilder expressionBuilder = new ExpressionBuilder();

    private final List<Item> normalItems = Arrays.asList(
            new Item(R.drawable.ic_expand_more), new Item(R.drawable.ic_backspace), new Item("%"), new Item("÷"),
            new Item('7'), new Item('8'), new Item('9'), new Item("×"),
            new Item('4'), new Item('5'), new Item('6'), new Item("-"),
            new Item('3'), new Item('2'), new Item('1'), new Item("+"),
            new Item('0'), new Item('.'), new Item("( )"), new Item("="));
    private final Item RADItem = new Item("RAD"), DEGItem = new Item("DEG");

    private final List<Item> advancedItems = Arrays.asList(
            new Item(R.drawable.ic_expand_less), new Item(R.drawable.ic_backspace), new Item("AC"), new Item(),
            DEGItem, new Item((CharSequence) "sin"), new Item((CharSequence) "cos"), new Item((CharSequence) "tan"),
            new Item("INV"), new Item('e'), new Item((CharSequence) "ln"), new Item((CharSequence) "lg"),
            new Item((CharSequence) "√"), new Item('π'), new Item((CharSequence) "^"), new Item((CharSequence) "!"));
    private final List<Item> INVItems = Arrays.asList(
            newPowItem("sin", "-1"), newPowItem("cos", "-1"), newPowItem("tan", "-1"),
            newPowItem("e", "x"), newPowItem("10", "x"), newPowItem("x", "2"));
    private final int[] INVIndexes = new int[]{5, 6, 7, 10, 11, 12};

    private LinearLayout layout;
    private HorizontalScrollView scroll_expressions, scroll_result;
    private TextView tv_expressions, tv_result;
    private RecyclerView rv_digits;
    private boolean isRad, isFinalResult, isINV;
    private DigitAdapter adapter;

    private int columns;
    private int perLength;

    private static int getStringNumLength(String text) {
        return (text.startsWith("+") || text.startsWith("-")) ? text.length() - 1 : text.length();
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        resizeViews();
        initDigits();
    }

    private void resizeViews() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        int rows = 5;
        columns = 4;

        perLength = width / columns;
        int digitsHeight = perLength * rows;
        LinearLayout.LayoutParams rvParams = (LinearLayout.LayoutParams) rv_digits.getLayoutParams();
        rvParams.height = digitsHeight;
        rv_digits.setLayoutParams(rvParams);

        int tvsHeight = height - digitsHeight;
        int tvHeight = tvsHeight / 2;
        tv_expressions.setHeight(tvHeight);
        scroll_expressions.setMinimumHeight(tvHeight);
        tv_expressions.setTextSize(TypedValue.COMPLEX_UNIT_PX, tvHeight / 2.5f);

        tv_result.setHeight(tvHeight);
        scroll_expressions.setMinimumHeight(tvHeight);
        tv_result.setTextSize(TypedValue.COMPLEX_UNIT_PX, tvHeight / 2.5f);
        tv_result.setTextColor(Color.GRAY);

    }

    private void initViews() {
        layout = findViewById(R.id.layout_main);
        tv_expressions = findViewById(R.id.tv_expressions);
        scroll_expressions = findViewById(R.id.scroll_expressions);
        tv_expressions.setText("", TextView.BufferType.EDITABLE);
        tv_result = findViewById(R.id.tv_result);
        scroll_result = findViewById(R.id.scroll_result);
        rv_digits = findViewById(R.id.rv_digits);
    }

    private void initDigits() {
        tv_result.setText("0");
        scroll_expressions.setHorizontalScrollBarEnabled(false);
        scroll_result.setHorizontalScrollBarEnabled(false);

        rv_digits.setLayoutManager(new GridLayoutManager(this, columns));

        adapter = new DigitAdapter(perLength, normalItems);
        adapter.setOnItemClickListener(this);
        rv_digits.setAdapter(adapter);

        originalNumFormat.setMaximumFractionDigits(ACCURACY_LIMIT);
        resultFormat.setGroupingUsed(true);
        resultFormat.setMaximumFractionDigits(ACCURACY_LIMIT);
        resultFormat.setRoundingMode(RoundingMode.HALF_UP);
    }

    @Override public void onClick(Item item) {
        int viewType = item.viewType;
        switch (viewType) {
            case VIEW_TYPE_DIGIT:
                if (!isFinalResult) {
                    appendDigit(item.digit);
                    performCalculate();
                }
                break;

            case VIEW_TYPE_SPECIAL:
                if (item.resourceId == R.drawable.ic_backspace) {
                    performBackspace();
                    performCalculate();
                } else if (item.resourceId == R.drawable.ic_expand_more) {
                    performSwitchAdvancedPanel(true);
                } else if (item.resourceId == R.drawable.ic_expand_less) {
                    performSwitchAdvancedPanel(false);
                }
                break;

            case VIEW_TYPE_ADVANCED:
                if (isFinalResult)
                    return;

                CharSequence advanced = item.advanced;
                int length = advanced.length();

                if (length == 1) {
                    appendOperator(advanced.charAt(0));
                    performCalculate();
                    break;
                }

                if (length > 1) {
                    String s = advanced.toString();

                    switch (s) {
                        case "x2":  //x^2
                            if (appendOperator('^'))
                                appendDigit('2');
                            break;

                        case "10x":  // 10^x
                            if (appendDigit('1') && appendDigit('0'))
                                appendOperator('^');
                            break;

                        case "ex"://e^x
                            appendFunction("exp");
                            break;

                        default:
                            appendFunction(advanced);
                            break;

                    }

                    performCalculate();
                }

                break;

            case VIEW_TYPE_OPERATOR:
                String operator = item.operator;
                if (operator == null)
                    break;
                if (operator.equals("AC")) {
                    performAllClear();
                    break;
                }
                if (operator.equals("RAD") || operator.equals("DEG")) {
                    performSwitchRad(!isRad);
                    performCalculate();
                    break;
                }
                if (operator.equals("INV")) {
                    performSwitchINV(!isINV);
                }

                if (isFinalResult)
                    break;

                if (operator.equals("=")) {
                    performEqualSign();
                    break;
                }
                if (operator.equals("( )")) {
                    appendBracket();
                    performCalculate();
                    break;
                }
                char firstChar = operator.charAt(0);
                if (firstChar == '%' || BASIC_OPERATORS.contains(firstChar)) {
                    appendOperator(firstChar);
                    performCalculate();
                    break;
                }
        }
    }

    private void performSwitchAdvancedPanel(boolean open) {
        if (open) {
            adapter.setItems(advancedItems);
            if (isRad)
                adapter.setItem(4, RADItem);
            if (isINV)
                performSwitchINV(true);
        } else {
            adapter.setItems(normalItems);
        }
    }

    private void performSwitchINV(boolean isINV) {
        this.isINV = isINV;
        if (isINV) {
            adapter.setItems(INVIndexes, INVItems);
        } else {
            adapter.setItems(INVIndexes, advancedItems);
        }
    }

    private void performSwitchRad(boolean rad) {
        adapter.setItem(4, rad ? RADItem : DEGItem);
        this.isRad = rad;
    }

    private boolean appendFunction(CharSequence advanced) {
        if (!expressionBuilder.appendLeadingFunction(advanced.toString()))
            return false;

        Editable text = tv_expressions.getEditableText();
        text.append(advanced.toString());

        if (advanced instanceof SpannableString) {
            //拼接时需要在新的位置设置新的Span
            SpannableString spannable = (SpannableString) advanced;

            int appendedLength = text.length();
            int start = appendedLength - spannable.length() + spannable.getSpanStart(SUPERSCRIPT_SPAN);
            text.setSpan(new SuperscriptSpan(), start, appendedLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new RelativeSizeSpan(0.5f), start, appendedLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tv_expressions.append("(");
        scroll_expressions.post(() -> scroll_expressions.fullScroll(View.FOCUS_RIGHT));
        return true;
    }

    private void performEqualSign() {
        isFinalResult = true;
        expressionBuilder.clear();
        tv_expressions.setText(tv_result.getText(), TextView.BufferType.EDITABLE);
        tv_result.setText("");
    }

    private void performAllClear() {
        expressionBuilder.clear();
        tv_expressions.setText("", TextView.BufferType.EDITABLE);
        tv_result.setText("0");
        tv_result.setTextColor(Color.GRAY);
        isFinalResult = false;
    }

    private boolean appendOperator(char operator) {
        if (!expressionBuilder.appendChar(operator))
            return false;
        tv_expressions.append(String.valueOf(operator));
        scroll_expressions.post(() -> scroll_expressions.fullScroll(View.FOCUS_RIGHT));
        return true;
    }

    private boolean appendBracket() {
        char bracket = expressionBuilder.appendBracket();
        if (bracket == EMPTY_CHAR)
            return false;
        tv_expressions.append(String.valueOf(bracket));
        scroll_expressions.post(() -> scroll_expressions.fullScroll(View.FOCUS_RIGHT));
        return true;
    }

    private boolean appendDigit(char digit) {
        if (!expressionBuilder.appendChar(digit))
            return false;
        Editable text = tv_expressions.getEditableText();
        String numberString = expressionBuilder.getCurrentNumberString();
        if (numberString.length() < 3 || numberString.contains("."))
            text.append(digit);
        else
            performExpressionThousandSeparator(text, numberString);

        scroll_expressions.post(() -> scroll_expressions.fullScroll(View.FOCUS_RIGHT));
        return true;
    }

    private void performBackspace() {
        if (isFinalResult) {
            performAllClear();
            return;
        }
        Editable text = tv_expressions.getEditableText();
        int displayedTotalLength = text.length();
        if (displayedTotalLength == 0)
            return;

        char displayedLastChar = text.charAt(displayedTotalLength - 1);
        char deletedChar = expressionBuilder.backspace(displayedLastChar);

        if (deletedChar != displayedLastChar) {
            text.delete(text.toString().lastIndexOf(deletedChar), displayedTotalLength);
        } else if (ADVANCED_OPERATORS.contains(deletedChar) || SEPARATE_CHARS.contains(deletedChar) || BASIC_OPERATORS.contains(deletedChar)) {
            text.delete(displayedTotalLength - 1, displayedTotalLength);
        } else {
            String numberString = expressionBuilder.getCurrentNumberString();
            if (numberString.contains(".") || deletedChar == '.')
                text.delete(displayedTotalLength - 1, displayedTotalLength);
            else {
                int length = getStringNumLength(numberString);
                if (length < 3)
                    text.delete(displayedTotalLength - 1, displayedTotalLength);
                else
                    performExpressionThousandSeparator(text, numberString);

            }
        }

        scroll_expressions.post(() -> scroll_expressions.fullScroll(View.FOCUS_RIGHT));
    }

    private void performExpressionThousandSeparator(Editable text, String numberString) {
        //先将原有的整数部分全部删掉，再替换成千位分隔符格式

        char delete;
        char firstChar = numberString.charAt(0);
        int length = text.length();
        while (true) {
            if (length == 0)
                break;

            //正负号说明我们来到了数字的开头
            delete = text.charAt(length - 1);
            if (firstChar == '+' || firstChar == '-') {
                if (delete == firstChar) {
                    text.delete(length - 1, length);
                    break;
                }
            }

            //删掉数字
            if (DIGIT_CHARS.contains(delete) || delete == ',') {
                text.delete(length - 1, length);
                length--;
            } else {
                break;
            }
        }

        //把刚才的正号补回来,不然正号会被忽略
        if (firstChar == '+')
            text.append(firstChar);

        double number = expressionBuilder.getCurrentNumber();
        String format = expressionFormat.format(number);
        text.append(format);
    }

    private void performCalculate() {
        try {
            double result = expressionBuilder.calculate(isRad);

            //超出精度限制则使用科学计数法。使用originalFormat关闭原有的科学计数法以免影响长度判断。
            String original = originalNumFormat.format(result);
            if (original.equals("0")) {
                if (result == 0)
                    tv_result.setText(String.valueOf(0)); //int...
                else
                    tv_result.setText(String.valueOf(result)); //double
            } else {
                int length = getStringNumLength(original);
                if (original.contains("."))
                    length--;

                if (length > ACCURACY_LIMIT)
                    tv_result.setText(String.valueOf(result));
                else
                    tv_result.setText(resultFormat.format(result));
            }

            tv_result.setTextColor(Color.GRAY);
            scroll_result.post(() -> scroll_result.fullScroll(View.FOCUS_LEFT));
        } catch (ArithmeticException e) {
            String message = e.getMessage();
            if (message == null)
                tv_result.setText(R.string.err);
            else
                tv_result.setText(Integer.parseInt(message));
            tv_result.setTextColor(Color.RED);
        }
    }
}