package com.ashfly.android.calculator.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DigitAdapter extends RecyclerView.Adapter<DigitAdapter.VH> {

    public static final int VIEW_TYPE_DIGIT = 1;
    public static final int VIEW_TYPE_OPERATOR = 2;
    public static final int VIEW_TYPE_SPECIAL = 3;
    public static final int VIEW_TYPE_ADVANCED = 4;
    public static final int VIEW_TYPE_EMPTY = 0;

    public static final SuperscriptSpan SUPERSCRIPT_SPAN = new SuperscriptSpan();
    public static final RelativeSizeSpan RELATIVE_SIZE_SPAN = new RelativeSizeSpan(0.5f);

    private final List<Item> list = new ArrayList<>();
    private int itemWidth, itemHeight;
    private OnItemClickListener onItemClickListener;
    private Drawable[] backgrounds;

    public DigitAdapter(int itemWidth, int itemHeight, List<Item> list) {
        this.itemWidth = itemWidth;
        this.itemHeight = itemHeight;
        this.list.addAll(list);
    }

    public void setBackgrounds(Drawable digitalBackground, Drawable operatorBackground, Drawable specialBackground) {
        if (backgrounds == null)
            backgrounds = new Drawable[]{digitalBackground, operatorBackground, specialBackground};
        else {
            backgrounds[0] = digitalBackground;
            backgrounds[1] = operatorBackground;
            backgrounds[2] = specialBackground;
        }
    }

    public void setItemSize(int width, int height) {
        if (this.itemWidth == width && this.itemHeight == height)
            return;
        this.itemWidth = width;
        this.itemHeight = height;
        setItems(new ArrayList<>(list));
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<Item> list) {
        this.list.clear();
        notifyDataSetChanged();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    public void setItem(int index, Item item) {
        this.list.set(index, item);
        notifyItemChanged(index);
    }

    public void setItems(int[] indexes, List<Item> list) {
        int size = list.size();
        int thisSize = this.list.size();

        if (indexes.length == size) {
            int i = 0;
            for (int index : indexes) {
                if (index < 0 || index >= thisSize)
                    continue;

                this.list.set(index, list.get(i));
                notifyItemChanged(index);
                i++;
            }
        } else if (thisSize == size) {
            for (int index : indexes) {
                if (index < 0 || index >= thisSize)
                    continue;

                this.list.set(index, list.get(index));
                notifyItemChanged(index);
            }
        } else {
            throw new IllegalStateException();
        }

    }


    @Override
    public int getItemViewType(int position) {
        return list.get(position).viewType;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return VH.newVH(parent.getContext(), itemWidth, itemHeight, viewType, backgrounds);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Item item = list.get(position);
        View view = holder.view;

        switch (item.viewType) {
            case VIEW_TYPE_DIGIT:
                ((TextView) view).setText(String.valueOf(item.digit));
                break;

            case VIEW_TYPE_OPERATOR:
                ((TextView) view).setText(item.operator);
                break;

            case VIEW_TYPE_ADVANCED:
                ((TextView) view).setText(item.advanced);
                break;

            case VIEW_TYPE_SPECIAL:
                ((ImageView) view).setImageResource(item.resourceId);
                break;

            default:
                return;
        }

        view.setOnClickListener(v -> {
            if (onItemClickListener != null)
                onItemClickListener.onClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {

        void onClick(Item item);
    }

    public static class Item {

        public static final Item EMPTY_ITEM = new Item();
        public final int viewType;
        public char digit;
        public String operator;
        public int resourceId;
        public CharSequence advanced;

        public Item(char digit) {
            this.digit = digit;
            viewType = VIEW_TYPE_DIGIT;
        }

        public Item(String operator) {
            this.operator = operator;
            viewType = VIEW_TYPE_OPERATOR;
        }

        public Item(int resourceId) {
            this.resourceId = resourceId;
            viewType = VIEW_TYPE_SPECIAL;
        }

        public Item(CharSequence advanced) {
            this.advanced = advanced;
            viewType = VIEW_TYPE_ADVANCED;
        }

        public Item() {
            viewType = VIEW_TYPE_EMPTY;
        }

        public static Item newPowItem(String base, String exponent) {
            SpannableString pow = new SpannableString(base + exponent);
            int length = base.length();
            int totalLength = pow.length();
            pow.setSpan(SUPERSCRIPT_SPAN, length, totalLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            pow.setSpan(RELATIVE_SIZE_SPAN, length, totalLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return new Item(pow);
        }
    }

    public static class VH extends RecyclerView.ViewHolder {

        final View view;

        private VH(View view) {
            super(view);
            this.view = view;
        }

        //Drawable[] digitalBackground, operatorBackground, specialBackground
        public static VH newVH(Context context, int itemWidth, int itemHeight, int viewType, Drawable[] backgrounds) {
            Objects.requireNonNull(context);
            if (backgrounds == null || backgrounds.length != 3)
                throw new IllegalArgumentException("invalid backgrounds[]");

            int horizontalMargin = itemWidth / 10;
            int contentWidth = itemWidth - horizontalMargin * 2;

            int contentHeight, verticalMargin;
            if (itemHeight > contentWidth) {
                //noinspection SuspiciousNameCombination 正方形
                contentHeight = contentWidth;
                verticalMargin = (itemHeight - contentHeight) / 2;
            } else {
                verticalMargin = itemHeight / 10;
                contentHeight = itemHeight - verticalMargin * 2;
            }


            Drawable background = null;
            View view;

            if (viewType == VIEW_TYPE_SPECIAL) {
                view = new ImageView(context);
                ((ImageView) view).setScaleType(ImageView.ScaleType.FIT_CENTER);
                int horizontalPadding = contentWidth / 4;
                int verticalPadding = contentHeight / 4;
                view.setPadding(horizontalPadding, verticalPadding , horizontalPadding, verticalPadding);
                background = backgrounds[2];
            } else if (viewType == VIEW_TYPE_DIGIT || viewType == VIEW_TYPE_OPERATOR || viewType == VIEW_TYPE_ADVANCED) {
                view = new TextView(context);
                TextView textView = (TextView) view;
                textView.setGravity(Gravity.CENTER);

                int min = Math.min(contentWidth, contentHeight);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, min / 2.2f);


                if (viewType == VIEW_TYPE_OPERATOR) {
                    background = backgrounds[1];
                    textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
                } else {
                    background = backgrounds[0];
                }

            } else {
                view = new View(context);
            }

            if (background != null) {
                background = Objects.requireNonNull(background.getConstantState()).newDrawable().mutate();
                view.setBackground(background);
            }
            GridLayoutManager.LayoutParams params = new GridLayoutManager.LayoutParams(contentWidth, contentHeight);
            params.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
            view.setLayoutParams(params);

            return new VH(view);
        }


    }
}
