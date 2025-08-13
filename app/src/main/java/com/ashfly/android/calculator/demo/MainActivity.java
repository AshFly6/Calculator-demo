package com.ashfly.android.calculator.demo;

import static com.ashfly.android.calculator.demo.DigitAdapter.Item;
import static com.ashfly.android.calculator.demo.DigitAdapter.Item.EMPTY_ITEM;
import static com.ashfly.android.calculator.demo.DigitAdapter.OnItemClickListener;
import static com.ashfly.android.calculator.demo.DigitAdapter.SUPERSCRIPT_SPAN;
import static com.ashfly.android.calculator.demo.DigitAdapter.VIEW_TYPE_ADVANCED;
import static com.ashfly.android.calculator.demo.DigitAdapter.VIEW_TYPE_DIGIT;
import static com.ashfly.android.calculator.demo.DigitAdapter.VIEW_TYPE_OPERATOR;
import static com.ashfly.android.calculator.demo.DigitAdapter.VIEW_TYPE_SPECIAL;
import static com.ashfly.android.calculator.demo.EquationBuilder.ADVANCED_OPERATORS;
import static com.ashfly.android.calculator.demo.EquationBuilder.BASIC_OPERATORS;
import static com.ashfly.android.calculator.demo.EquationBuilder.DIGIT_CHARS;
import static com.ashfly.android.calculator.demo.EquationBuilder.EMPTY_CHAR;
import static com.ashfly.android.calculator.demo.EquationBuilder.SEPARATE_CHARS;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.TypedValue;
import android.view.DisplayCutout;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @noinspection UnusedReturnValue
 */
public class MainActivity extends AppCompatActivity implements OnItemClickListener {

    private final NumberFormat resultFormat = NumberFormat.getNumberInstance();
    private final NumberFormat originNumFormat = new DecimalFormat("###0");
    private final NumberFormat expressionFormat = NumberFormat.getNumberInstance();
    private final List<Item> normalItems = Arrays.asList(
            new Item(R.drawable.ic_expand_more), new Item(R.drawable.ic_backspace), new Item("%"), new Item("÷"),
            new Item('7'), new Item('8'), new Item('9'), new Item("×"),
            new Item('4'), new Item('5'), new Item('6'), new Item("-"),
            new Item('3'), new Item('2'), new Item('1'), new Item("+"),
            new Item('0'), new Item('.'), new Item("( )"), new Item("="));
    private final Item RADItem = new Item("RAD"), DEGItem = new Item("DEG");
    private final List<Item> advancedItems = Arrays.asList(
            new Item(R.drawable.ic_expand_less), new Item(R.drawable.ic_backspace), new Item("AC"), EMPTY_ITEM,
            DEGItem, new Item((CharSequence) "sin"), new Item((CharSequence) "cos"), new Item((CharSequence) "tan"),
            new Item("INV"), new Item('e'), new Item((CharSequence) "ln"), new Item((CharSequence) "lg"),
            new Item((CharSequence) "√"), new Item('π'), new Item((CharSequence) "^"), new Item((CharSequence) "!"));
    private final List<Item> INVItems = Arrays.asList(
            Item.newPowItem("sin", "-1"), Item.newPowItem("cos", "-1"), Item.newPowItem("tan", "-1"),
            Item.newPowItem("e", "x"), Item.newPowItem("10", "x"), Item.newPowItem("x", "2"));
    private final List<Item> combinedItems, functionItems;
    private final int[] functionIndexes = new int[]{5, 6, 7, 10, 11, 12}; //因为布局是固定的，所以提前定义好索引，避免动态查询
    private final int[] functionIndexesCombined = new int[]{9, 10, 11, 18, 19, 24};
    private EquationBuilder expressionBuilder = new EquationBuilder();
    private Drawable digitalBackground, operatorBackground, specialBackground;
    private LinearLayout layout;
    private HorizontalScrollView scroll_expressions, scroll_result;
    private TextView tv_expressions, tv_result;
    private RecyclerView rv_digits;
    private boolean isRad, isFinalResult, isINV;
    private DigitAdapter adapter;
    private int itemWidth, itemHeight;
    private boolean combinedLayoutStyle;
    private boolean initialized;
    private boolean isAdvancedOpen;
    private View spacer_top;

    public MainActivity() {
        combinedItems = new ArrayList<>();

        //row 0
        for (int i = 0; i <= 3; i++) {
            combinedItems.add(EMPTY_ITEM);
        }
        combinedItems.add(advancedItems.get(2));
        combinedItems.add(normalItems.get(1));
        combinedItems.add(normalItems.get(2));
        combinedItems.add(normalItems.get(3));

        //row 1~3
        for (int rows = 1; rows < 4; rows++) {
            for (int columns = 0; columns < 8; columns++) {
                if (columns <= 3) {
                    combinedItems.add(advancedItems.get(rows * 4 + columns));
                } else {
                    combinedItems.add(normalItems.get(rows * 4 + columns - 4));
                }
            }
        }

        //row 4
        for (int i = 0; i <= 3; i++) {
            combinedItems.add(EMPTY_ITEM);
        }
        combinedItems.add(normalItems.get(16));
        combinedItems.add(normalItems.get(17));
        combinedItems.add(normalItems.get(18));
        combinedItems.add(normalItems.get(19));

        functionItems = new ArrayList<>();
        for (int index : functionIndexes) {
            functionItems.add(advancedItems.get(index));
        }
    }

    private static int getStringNumLength(String text) {
        return (text.startsWith("+") || text.startsWith("-")) ? text.length() - 1 : text.length();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableEdgeToEdge();
        setContentView(R.layout.activity_main);

        initViews();
        initEdgeToEdge();
        layout.post(() -> {
            initViewSizes();
            initDrawables(itemWidth, itemHeight);
            initDigits();
            postRestoreInstanceState(savedInstanceState);
        });
    }

    private void initEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= 21) {
            ViewCompat.setOnApplyWindowInsetsListener(layout, ((v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) spacer_top.getLayoutParams();
                layoutParams.height = systemBars.top;
                spacer_top.setLayoutParams(layoutParams);

                if (Build.VERSION.SDK_INT >= 28) {
                    DisplayCutout displayCutout = Objects.requireNonNull(insets.toWindowInsets()).getDisplayCutout();
                    if (displayCutout == null && Build.VERSION.SDK_INT >= 29)
                        displayCutout = getWindowManager().getDefaultDisplay().getCutout();

                    if (displayCutout != null) {
                        layout.setPadding(Math.max(displayCutout.getSafeInsetLeft(), systemBars.left), 0,
                                Math.max(displayCutout.getSafeInsetRight(), systemBars.right), 0);

                        rv_digits.setPadding(0, 0, 0, Math.max(displayCutout.getSafeInsetBottom(), systemBars.bottom));
                        return WindowInsetsCompat.CONSUMED;
                    }
                }

                layout.setPadding(systemBars.left, 0, systemBars.right, 0);
                rv_digits.setPadding(0, 0, 0, systemBars.bottom);
                return WindowInsetsCompat.CONSUMED;
            }));
        }
    }

    private void enableEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= 35)
            getWindow().setDecorFitsSystemWindows(false);
        if (Build.VERSION.SDK_INT >= 21)
            EdgeToEdge.enable(this);
    }

    private void initDrawables(int width, int height) {
        GradientDrawable baseShape = new GradientDrawable();
        baseShape.setSize(width, height);
        baseShape.setCornerRadius(Math.min(width, height) / 2f);

        GradientDrawable special = (GradientDrawable) Objects.requireNonNull(baseShape.getConstantState()).newDrawable().mutate();
        special.setColor(ContextCompat.getColor(this, R.color.background_special));
        GradientDrawable digits = (GradientDrawable) baseShape.getConstantState().newDrawable().mutate();
        digits.setColor(ContextCompat.getColor(this, R.color.background_digits));
        GradientDrawable operator = (GradientDrawable) baseShape.getConstantState().newDrawable().mutate();
        operator.setColor(ContextCompat.getColor(this, R.color.background_operators));


        if (Build.VERSION.SDK_INT >= 21) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.colorControlHighlight, typedValue, true);
            int rippleColor = typedValue.data;

            digitalBackground = new RippleDrawable(ColorStateList.valueOf(rippleColor), digits, null);
            specialBackground = new RippleDrawable(ColorStateList.valueOf(rippleColor), special, null);
            operatorBackground = new RippleDrawable(ColorStateList.valueOf(rippleColor), operator, null);
        } else {
            GradientDrawable pressedDrawable = (GradientDrawable) baseShape.getConstantState().newDrawable().mutate();
            pressedDrawable.setColor(0x10000000);

            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
            digitalBackground = Objects.requireNonNull(stateListDrawable.getConstantState()).newDrawable().mutate();
            ((StateListDrawable) digitalBackground).addState(new int[0], digits);
            operatorBackground = stateListDrawable.getConstantState().newDrawable().mutate();
            ((StateListDrawable) operatorBackground).addState(new int[0], operator);
            specialBackground = stateListDrawable.getConstantState().newDrawable().mutate();
            ((StateListDrawable) specialBackground).addState(new int[0], special);
        }
    }

    private void initViewSizes() {
        int width = layout.getWidth() - layout.getPaddingLeft() - layout.getPaddingRight();
        int rvHeight = rv_digits.getHeight() - rv_digits.getPaddingBottom();

        int rows = 5;

        itemHeight = rvHeight / rows;

        int columns;
        if (width >= itemHeight * 8) {
            columns = 8;
            combinedLayoutStyle = true;
        } else {
            columns = 4;
            combinedLayoutStyle = false;
        }

        itemWidth = width / columns;

        int tvHeight = tv_expressions.getHeight();
        tv_expressions.setTextSize(TypedValue.COMPLEX_UNIT_PX, tvHeight / 2.5f);
        tv_result.setTextSize(TypedValue.COMPLEX_UNIT_PX,  tvHeight / 2.5f);

        if (initialized) {
            adapter.setItemSize(itemWidth, itemHeight);
        }
    }


    private void initViews() {
        layout = findViewById(R.id.main);
        tv_expressions = findViewById(R.id.tv_expressions);
        scroll_expressions = findViewById(R.id.scroll_expressions);
        tv_expressions.setText("", TextView.BufferType.EDITABLE);
        tv_result = findViewById(R.id.tv_result);
        scroll_result = findViewById(R.id.scroll_result);
        rv_digits = findViewById(R.id.rv_digits);
        spacer_top = findViewById(R.id.spacer_top);
    }

    private void initDigits() {

        tv_result.setText("0");
        tv_result.setTextColor(Color.GRAY);

        scroll_expressions.setHorizontalScrollBarEnabled(false);
        scroll_result.setHorizontalScrollBarEnabled(false);

        rv_digits.setLayoutManager(new GridLayoutManager(this, combinedLayoutStyle ? 8 : 4));

        adapter = new DigitAdapter(itemWidth, itemHeight, combinedLayoutStyle ? combinedItems : normalItems);

        adapter.setOnItemClickListener(this);
        adapter.setBackgrounds(digitalBackground, operatorBackground, specialBackground);
        rv_digits.setAdapter(adapter);

        originNumFormat.setMaximumFractionDigits(17);
        resultFormat.setMaximumFractionDigits(17);
        resultFormat.setRoundingMode(RoundingMode.HALF_UP);

        initialized = true;
    }

    @Override
    public void onClick(Item item) {
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

    //不会在combined布局使用
    private void performSwitchAdvancedPanel(boolean open) {
        if (combinedLayoutStyle)
            return;
        this.isAdvancedOpen = open;
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
        int[] indexes = combinedLayoutStyle ? functionIndexesCombined : functionIndexes;
        if (isINV) {
            adapter.setItems(indexes, INVItems);
        } else {
            adapter.setItems(indexes, functionItems);
        }
    }

    private void performSwitchRad(boolean rad) {
        this.isRad = rad;

        Item item = rad ? RADItem : DEGItem;
        if (combinedLayoutStyle) {
            adapter.setItem(8, item);
        } else if (isAdvancedOpen) {
            adapter.setItem(4, item);
        }
    }

    private boolean appendFunction(CharSequence advanced) {
        if (!expressionBuilder.appendLeadingFunction(advanced.toString()))
            return false;

        Editable text = tv_expressions.getEditableText();
        text.append(advanced.toString());

        if (advanced instanceof SpannableString) {
            SpannableString spannable = (SpannableString) advanced;

            //拼接时需要在新的位置设置新的Span
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

    private void performExpressionThousandSeparator(Editable text, String currentNumberString) {
        if (currentNumberString.contains("."))
            return;

        //先将原有的整数部分全部删掉，再替换成千位分隔符格式

        char check;
        int length = text.length();
        int deleteStart = length;
        char firstChar = currentNumberString.charAt(0);

        while (deleteStart > 0) {

            //正负号说明我们来到了数字的开头
            check = text.charAt(deleteStart - 1);
            if (firstChar == '+' || firstChar == '-') {
                if (check == firstChar) {
                    deleteStart--;
                    break;
                }
            }

            //删掉数字
            if (DIGIT_CHARS.contains(check) || check == ',') {
                deleteStart--;
            } else {
                break;
            }
        }
        text.delete(deleteStart, length);

        //把刚才的正号补回来,不然正号会被忽略
        if (firstChar == '+')
            text.append(firstChar);

        double number = expressionBuilder.getCurrentNumber();
        String format = expressionFormat.format(number);
        text.append(format);
    }

    private void performCalculate() {
        String resultTextDisplay = null;

        Double result = null;
        try {
            result = expressionBuilder.calculate(isRad);
        } catch (Exception e) {
            if (e instanceof EquationBuilder.CalculateException) {
                try {
                    resultTextDisplay = getString(((EquationBuilder.CalculateException) e).textResourceId);
                } catch (Resources.NotFoundException ignored) {
                }
            }
            if (resultTextDisplay == null) {
                String message = e.getMessage();
                if (message == null)
                    resultTextDisplay = getString(R.string.err);
                else
                    resultTextDisplay = message;
            }
        }

        if (result != null) {
            String origin = originNumFormat.format(result); //去除double自动添加的科学记数法

            if (origin.equals("0") || origin.equals("-0")) {
                resultTextDisplay = "0";
            } else {
                String resultString = result.toString();
                int dotIndex = resultString.indexOf("."), EIndex = resultString.indexOf("E");
                if (dotIndex >= 0) {
                    if (EIndex > dotIndex) {
                        int fractionalLength = resultString.substring(dotIndex + 1, EIndex).length();
                        int ELength = Integer.parseInt(resultString.substring(EIndex + 1));
                        if (ELength <= -10 ||
                                (ELength > fractionalLength && ELength >= 10)) {
                            resultTextDisplay = resultString; //需要使用科学记数法
                        }
                    }
                }
            }
        }


        if (resultTextDisplay == null)
            resultTextDisplay = resultFormat.format(result);

        tv_result.setText(resultTextDisplay);
        if (result == null) {
            tv_result.setTextColor(Color.RED);
        } else {
            tv_result.setTextColor(Color.GRAY);
            scroll_result.post(() -> scroll_result.fullScroll(View.FOCUS_LEFT));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("isINV", isINV);
        outState.putBoolean("isRad", isRad);
        outState.putBoolean("isAdvancedOpen", isAdvancedOpen);

        if (isFinalResult)
            outState.putBoolean("isFinalResult", true);
        else {
            outState.putParcelable("expressionBuilder", expressionBuilder);
            outState.putBoolean("isFinalResult", false);
        }

        outState.putCharSequence("displayedExpression", tv_expressions.getText());
    }

    //不是重写onRestoreInstanceState(Bundle)
    protected void postRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;

        if (savedInstanceState.getBoolean("isINV")) {
            performSwitchINV(true);
        }
        if (savedInstanceState.getBoolean("isRad")) {
            performSwitchRad(true);
        }
        if (!combinedLayoutStyle && savedInstanceState.getBoolean("isAdvancedOpen")) {
            performSwitchAdvancedPanel(true);
        }
        if (savedInstanceState.getBoolean("isFinalResult")) {
            isFinalResult = true;
        } else {
            expressionBuilder = (EquationBuilder) savedInstanceState.get("expressionBuilder");
            performCalculate();
        }
        tv_expressions.setText(savedInstanceState.getCharSequence("displayedExpression"), TextView.BufferType.EDITABLE);

    }
}