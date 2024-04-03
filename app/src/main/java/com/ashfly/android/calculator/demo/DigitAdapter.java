package com.ashfly.android.calculator.demo;

import android.content.*;
import android.graphics.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.core.content.*;
import androidx.recyclerview.widget.*;

import java.util.*;

public class DigitAdapter extends RecyclerView.Adapter<DigitAdapter.VH> {

    public static final int VIEW_TYPE_DIGIT = 0;

    public static final int VIEW_TYPE_OPERATOR = 1;

    public static final int VIEW_TYPE_SPECIAL = 2;

    private final int perLength;
    private final List<Item> list = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    public DigitAdapter(int perLength, List<Item> list) {
        this.perLength = perLength;
        this.list.addAll(list);
    }

    @Override public int getItemViewType(int position) {
        return list.get(position).viewType;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return VH.newVH(parent.getContext(), perLength, viewType);
    }

    @Override public void onBindViewHolder(@NonNull VH holder, int position) {
        Item item = list.get(position);
        View view = holder.view;

        switch (item.viewType) {
            case VIEW_TYPE_DIGIT:
                ((TextView) view).setText("" + item.digit);
                break;

            case VIEW_TYPE_OPERATOR:
                ((TextView) view).setText(item.operator);
                break;

            default:
                ((ImageView) view).setImageResource(item.resourceId);
                break;
        }

        view.setOnClickListener(v -> {
            if (onItemClickListener != null)
                onItemClickListener.onClick(item);

        });
    }

    @Override public int getItemCount() {
        return list.size();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {

        void onClick(Item item);
    }

    public static class Item {

        public final char digit;
        public final String operator;
        public final int resourceId;
        public final int viewType;

        public Item(char digit) {
            this.digit = digit;
            this.operator = null;
            this.resourceId = 0;
            viewType = VIEW_TYPE_DIGIT;
        }

        public Item(String operator) {
            this.digit = '\u0000';
            this.operator = operator;
            this.resourceId = 0;
            viewType = VIEW_TYPE_OPERATOR;
        }

        public Item(int resourceId) {
            this.digit = '\u0000';
            this.operator = null;
            this.resourceId = resourceId;
            viewType = VIEW_TYPE_SPECIAL;
        }
    }

    public static class VH extends RecyclerView.ViewHolder {

        final View view;
        final int perLength;

        private VH(View view, int perLength) {
            super(view);
            this.view = view;
            this.perLength = perLength;
        }

        public static VH newVH(Context context, int perLength, int viewType) {
            int margin = perLength / 10;
            int margins = margin * 2;
            int size = perLength - margins;

            int background;
            View view;

            if (viewType == VIEW_TYPE_DIGIT || viewType == VIEW_TYPE_OPERATOR) {
                view = new TextView(context);
                TextView textView = (TextView) view;
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, margin * 3.5f); //基于测试，3.5是合适的乘数，而3过小，4过大。
                textView.setTypeface(textView.getTypeface(), Typeface.BOLD);

                if (viewType == VIEW_TYPE_DIGIT)
                    background = R.drawable.drawable_background_digit;
                else
                    background = R.drawable.drawable_background_operator;
            } else {
                view = new ImageView(context);
                ((ImageView) view).setScaleType(ImageView.ScaleType.FIT_XY);
                view.setPadding(margins, margins, margins, margins);
                background = R.drawable.drawable_background_special;
            }

            view.setBackground(ContextCompat.getDrawable(context, background));

            GridLayoutManager.LayoutParams params = new GridLayoutManager.LayoutParams(size, size);
            params.setMargins(margin, margin, margin, margin);
            view.setLayoutParams(params);

            return new VH(view, perLength);
        }
    }
}
