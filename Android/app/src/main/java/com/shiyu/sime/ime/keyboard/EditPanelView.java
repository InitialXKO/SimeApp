package com.shiyu.sime.ime.keyboard;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.shiyu.sime.ime.feedback.InputFeedbacks;
import com.shiyu.sime.ime.theme.Typography;

/**
 * Text editing & cursor control panel.
 * Provides controls for directional cursor movement (←, →, ↑, ↓, Home, End),
 * selection/clipboard operations (全选, 复制, 剪切, 粘贴), and deletion (删除, 清空).
 */
public class EditPanelView extends KeyboardView {

    public interface EditActionListener {
        void onMoveLeft();
        void onMoveRight();
        void onMoveUp();
        void onMoveDown();
        void onMoveHome();
        void onMoveEnd();
        void onSelectAll();
        void onCopy();
        void onCut();
        void onPaste();
        void onDelete();
        void onClear();
    }

    private EditActionListener actionListener;

    public EditPanelView(Context context) {
        super(context);
        build();
    }

    public void setEditActionListener(EditActionListener l) {
        this.actionListener = l;
    }

    private void build() {
        setOrientation(LinearLayout.VERTICAL);
        setPadding(dp(12), dp(8), dp(12), dp(8));

        // Top Row: Directional & Cursor Movement
        LinearLayout topRow = new LinearLayout(getContext());
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        topRow.addView(createButton("首", () -> { if (actionListener != null) actionListener.onMoveHome(); }));
        topRow.addView(createButton("↑", () -> { if (actionListener != null) actionListener.onMoveUp(); }));
        topRow.addView(createButton("尾", () -> { if (actionListener != null) actionListener.onMoveEnd(); }));
        topRow.addView(createButton("⌫", () -> { if (actionListener != null) actionListener.onDelete(); }));

        addView(topRow);

        // Middle Row: Left/Right & Selection/Cut
        LinearLayout midRow = new LinearLayout(getContext());
        midRow.setOrientation(LinearLayout.HORIZONTAL);
        midRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        midRow.addView(createButton("←", () -> { if (actionListener != null) actionListener.onMoveLeft(); }));
        midRow.addView(createButton("↓", () -> { if (actionListener != null) actionListener.onMoveDown(); }));
        midRow.addView(createButton("→", () -> { if (actionListener != null) actionListener.onMoveRight(); }));
        midRow.addView(createButton("全选", () -> { if (actionListener != null) actionListener.onSelectAll(); }));

        addView(midRow);

        // Bottom Row: Copy, Cut, Paste, Clear
        LinearLayout botRow = new LinearLayout(getContext());
        botRow.setOrientation(LinearLayout.HORIZONTAL);
        botRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        botRow.addView(createButton("复制", () -> { if (actionListener != null) actionListener.onCopy(); }));
        botRow.addView(createButton("剪切", () -> { if (actionListener != null) actionListener.onCut(); }));
        botRow.addView(createButton("粘贴", () -> { if (actionListener != null) actionListener.onPaste(); }));
        botRow.addView(createButton("清空", () -> { if (actionListener != null) actionListener.onClear(); }));

        addView(botRow);
    }

    private View createButton(String text, Runnable action) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, Typography.BODY);
        tv.setTextColor(theme.keyText);
        tv.setGravity(Gravity.CENTER);

        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, roundedRect(theme.keyBackgroundPressed, dp(8)));
        bg.addState(new int[]{}, roundedRect(theme.keyBackground, dp(8)));
        tv.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        tv.setLayoutParams(lp);

        InputFeedbacks.wireClick(tv, action);
        return tv;
    }

    private GradientDrawable roundedRect(int color, int radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(radiusPx);
        d.setColor(color);
        return d;
    }
}
