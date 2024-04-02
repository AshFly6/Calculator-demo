package com.ashfly.android.calculator.demo;

import static com.ashfly.android.calculator.demo.ExpressionBuilder.*;
import static com.ashfly.android.calculator.demo.DigitAdapter.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
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

    private LinearLayout layout;
    private TextView tv_expressions, tv_result;
    private HorizontalScrollView scroll_expressions, scroll_result;
    private RecyclerView rv_digits;
    private int columns;
    private int perLength;
    private ExpressionBuilder expressionBuilder = new ExpressionBuilder();

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
        rv_digits.setLayoutManager(new GridLayoutManager(this, columns));

        final List<Item> items = Arrays.asList(
                new Item(R.drawable.ic_arrow_down), new Item(R.drawable.ic_backspace), new Item("%"), new Item("÷"),
                new Item('7'), new Item('8'), new Item('9'), new Item("×"),
                new Item('4'), new Item('5'), new Item('6'), new Item("-"),
                new Item('3'), new Item('2'), new Item('1'), new Item("+"),
                new Item('0'), new Item('.'), new Item("( )"), new Item("=")
        );
        DigitAdapter adapter = new DigitAdapter(perLength, items);
        adapter.setOnItemClickListener(this);
        rv_digits.setAdapter(adapter);

        resultFormat.setMaximumIntegerDigits(12);
        resultFormat.setRoundingMode(RoundingMode.DOWN);
    }

    @Override public void onClick(Item item) {
        int viewType = item.viewType;
        switch (viewType) {
            case VIEW_TYPE_DIGIT:
                appendDigit(item);
                performCalculate();
                break;

            case VIEW_TYPE_OPERATOR:
                String operator = item.operator;
                if ("=".equals(operator)) {
                    performCalculate();
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
                }

        }
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
        tv_expressions.append("" + bracket);
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
        Editable text = tv_expressions.getEditableText();
        int length = text.length();
        if (length == 0)
            return;
        char c = expressionBuilder.backspace();

        if (c == '.' || c == '(' || c == ')' || c == '%' || BASIC_OPERATORS.contains(c)) {
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

        BigDecimal number = expressionBuilder.getNumber(expressionBuilder.getNumberCount() - 1);
        String format = expressionFormat.format(number);
        text.append(format);
    }

    private void performCalculate() {
        BigDecimal result;
        try {
            result = expressionBuilder.calculate();
        } catch (ArithmeticException e) {
            tv_result.setText(R.string.cannot_divide_by_zero);
            return;
        }

        tv_result.setText(resultFormat.format(result));
        scroll_result.post(() -> scroll_result.fullScroll(View.FOCUS_LEFT));
    }
}