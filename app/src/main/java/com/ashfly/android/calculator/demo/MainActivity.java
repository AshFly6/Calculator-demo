package com.ashfly.android.calculator.demo;

import static com.ashfly.android.calculator.demo.ExpressionBuilder.*;
import static com.ashfly.android.calculator.demo.DigitAdapter.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import android.graphics.*;
import android.os.Bundle;
import android.text.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import java.math.*;
import java.text.*;
import java.util.*;

public class MainActivity extends AppCompatActivity implements OnItemClickListener {

    private final DecimalFormat resultFormat = new DecimalFormat("###,###.############");
    private final DecimalFormat expressionFormat = new DecimalFormat("###,###");

    private final ExpressionBuilder expressionBuilder = new ExpressionBuilder();
    private final List<Item> normalItems = Arrays.asList(
            new Item(R.drawable.ic_expand_more), new Item(R.drawable.ic_backspace), new Item("%"), new Item("÷"),
            new Item('7'), new Item('8'), new Item('9'), new Item("×"),
            new Item('4'), new Item('5'), new Item('6'), new Item("-"),
            new Item('3'), new Item('2'), new Item('1'), new Item("+"),
            new Item('0'), new Item('.'), new Item("( )"), new Item("=")
    );
    private LinearLayout layout;
    private HorizontalScrollView scroll_expressions, scroll_result;
    private TextView tv_expressions, tv_result;
    private RecyclerView rv_digits;
    private List<Item> advancedItems;
    private boolean isRad, isFinalResult;
    private DigitAdapter adapter;

    private int columns;
    private int perLength;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        resizeViews();
        initDigits();
    }

    private void resizeViews() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
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

        rv_digits.setLayoutManager(new GridLayoutManager(this, columns));

        adapter = new DigitAdapter(perLength, normalItems);
        adapter.setOnItemClickListener(this);
        rv_digits.setAdapter(adapter);

        resultFormat.setMaximumFractionDigits(12);
        resultFormat.setRoundingMode(RoundingMode.DOWN);
    }

    @Override public void onClick(Item item) {
        int viewType = item.viewType;
        switch (viewType) {
            case VIEW_TYPE_DIGIT:
                if (!isFinalResult) {
                    appendDigit(item);
                    performCalculate();
                }
                break;

            case VIEW_TYPE_OPERATOR:
                String operator = item.operator;
                if ("AC".equals(operator)) {
                    performAllClear();
                    break;
                }

                if (isFinalResult)
                    return;

                if ("=".equals(operator)) {
                    performEqualSign();
                    break;
                }

                if ("( )".equals(operator))
                    appendBracket();
                else
                    appendBasicOperator(operator);
                performCalculate();
                break;

            case VIEW_TYPE_SPECIAL:
                if (item.resourceId == R.drawable.ic_backspace) {
                    performBackspace();
                } else if (item.resourceId == R.drawable.ic_expand_more) {
                    performSwitchAdvancedPanel(true);
                } else if (item.resourceId == R.drawable.ic_expand_less) {
                    performSwitchAdvancedPanel(false);
                }
                break;

            case VIEW_TYPE_ADVANCED:
                appendFunction(item.advanced);
                break;

        }
    }

    private void appendFunction(CharSequence advanced) {
        if (expressionBuilder.appendFunction(advanced.toString())) {
            tv_expressions.append(advanced);
            if (advanced != "√")
                tv_expressions.append("(");
            performCalculate();
        }
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
        isFinalResult = false;
    }

    private void performSwitchAdvancedPanel(boolean open) {
        if (!open) {
            adapter.setItems(normalItems);
            return;
        }
        if (advancedItems == null) {
            advancedItems = Arrays.asList(new Item(R.drawable.ic_expand_less), new Item(R.drawable.ic_backspace), new Item("AC"), new Item(),
                    new Item("DEG"), new Item((CharSequence) "sin"), new Item((CharSequence) "cos"), new Item((CharSequence) "tan"),
                    new Item("INV"), new Item('e'), new Item((CharSequence) "ln"), new Item((CharSequence) "lg"),
                    new Item((CharSequence) "√"), new Item('π'), new Item('^'), new Item('!'));
        }
        adapter.setItems(advancedItems);
    }

    private void appendBasicOperator(String operator) {
        char c = operator.charAt(0);
        if (expressionBuilder.append(c)) {
            tv_expressions.append(operator);
            scroll_expressions.post(() -> scroll_expressions.fullScroll(View.FOCUS_RIGHT));
        }
    }

    private void appendBracket() {
        char bracket = expressionBuilder.appendBracket();
        if (bracket == EMPTY_CHAR)
            return;
        tv_expressions.append(String.valueOf(bracket));
        scroll_expressions.post(() -> scroll_expressions.fullScroll(View.FOCUS_RIGHT));
    }

    private void appendDigit(Item item) {
        if (expressionBuilder.append(item.digit)) {
            Editable text = tv_expressions.getEditableText();
            String numberString = expressionBuilder.getNumberString(expressionBuilder.getNumberCount() - 1);
            if (numberString.length() < 3 || numberString.contains("."))
                text.append(item.digit);
            else
                performExpressionThousandSeparator(text, numberString);

            tv_expressions.setText(text, TextView.BufferType.EDITABLE);
            scroll_expressions.post(() -> scroll_expressions.fullScroll(View.FOCUS_RIGHT));
        }
    }


    private void performBackspace() {
        if (isFinalResult) {
            performAllClear();
            return;
        }
        Editable text = tv_expressions.getEditableText();
        int length = text.length();
        if (length == 0)
            return;
        char c = expressionBuilder.backspace();

        if (c == 'c' || c == 't' || c == 's' || c == 'l' || c == '√') {
            text.delete(text.toString().lastIndexOf(c), length);
        } else if (c == '.' || c == '(' || c == ')' || c == '%' || BASIC_OPERATORS.contains(c)) {
            text.delete(length - 1, length); //直接删
        } else {
            String numberString = expressionBuilder.getNumberString(expressionBuilder.getNumberCount() - 1);
            if (numberString.length() < 3 || numberString.contains(".")) //当整数位数刚刚从4转到3时，原格式还保留着千位分隔符，所以仍要重新格式化
                text.delete(length - 1, length); //小数位数和小数点，以及不够长的整数部分，直接删
            else {
                performExpressionThousandSeparator(text, numberString);
            }
        }
        tv_expressions.setText(text, TextView.BufferType.EDITABLE);
        scroll_expressions.post(() -> scroll_expressions.fullScroll(View.FOCUS_RIGHT));
        performCalculate();
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
            if ((delete >= '0' && delete <= '9') || delete == ',') {
                text.delete(length - 1, length);
                length--;
            } else {
                break;
            }
        }

        //把刚才的正号补回来,不然正号会被忽略
        if (firstChar == '+')
            text.append(firstChar);

        double number = expressionBuilder.getNumber(expressionBuilder.getNumberCount() - 1);
        String format = expressionFormat.format(number);
        text.append(format);
    }

    private void performCalculate() {
        double result;
        try {
            result = expressionBuilder.calculate(isRad);
        } catch (ArithmeticException e) {
            String message = e.getMessage();
            if (message == null)
                tv_result.setText(R.string.err);
            else
                tv_result.setText(Integer.parseInt(message));
            return;
        }

        tv_result.setText(resultFormat.format(result));
        scroll_result.post(() -> scroll_result.fullScroll(View.FOCUS_LEFT));
    }
}