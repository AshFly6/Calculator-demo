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

    public static final int VIEW_TYPE_DIGIT = 1;
    public static final int VIEW_TYPE_OPERATOR = 2;
    public static final int VIEW_TYPE_SPECIAL = 3;
    public static final int VIEW_TYPE_ADVANCED = 4;
    public static final int VIEW_TYPE_EMPTY = 0;

    private final int perLength;
    private final List<Item> list = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    public DigitAdapter(int perLength, List<Item> list) {
        this.perLength = perLength;
        this.list.addAll(list);
    }

    public void setItems(List<Item> list) {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    public void setItem(int index, Item item) {
        this.list.set(index, item);
        notifyItemChanged(index);
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

            int background = -1;
            View view;

            if (viewType == VIEW_TYPE_SPECIAL) {
                view = new ImageView(context);
                ((ImageView) view).setScaleType(ImageView.ScaleType.FIT_XY);
                view.setPadding(margins, margins, margins, margins);
                background = R.drawable.drawable_background_special;
            } else if (viewType == VIEW_TYPE_DIGIT || viewType == VIEW_TYPE_OPERATOR || viewType == VIEW_TYPE_ADVANCED) {
                view = new TextView(context);
                TextView textView = (TextView) view;
                textView.setGravity(Gravity.CENTER);

                if (viewType == VIEW_TYPE_ADVANCED) {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, margin * 3.5f);
                } else {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, margin * 3.5f); //基于测试，3.5是合适的乘数，而3过小，4过大。
                }

                if (viewType == VIEW_TYPE_OPERATOR) {
                    background = R.drawable.drawable_background_operator;
                    textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
                } else {
                    background = R.drawable.drawable_background_digit;
                }

            } else {
                view = new View(context);
            }

            if (background > 0)
                view.setBackground(ContextCompat.getDrawable(context, background));

            GridLayoutManager.LayoutParams params = new GridLayoutManager.LayoutParams(size, size);
            params.setMargins(margin, margin, margin, margin);
            view.setLayoutParams(params);

            return new VH(view, perLength);
        }
    }
}
