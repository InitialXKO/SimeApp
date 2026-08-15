package com.shiyu.sime.ime.keyboard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.shiyu.sime.R;
import com.shiyu.sime.ime.InputKernel;
import com.shiyu.sime.ime.feedback.InputFeedbacks;
import com.shiyu.sime.ime.handwriting.HCCRRecognizer;
import com.shiyu.sime.ime.theme.Typography;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Offline single-character handwriting keyboard.  Recognition is delayed
 * until the pen lifts, so multiple strokes naturally form one character.
 */
public final class HandwritingKeyboardView extends KeyboardView {
    private static final int TOP_K = 8;
    private static final long RECOGNIZE_DELAY_MS = 180;
    private static HCCRRecognizer recognizer;
    private static final Object RECOGNIZER_LOCK = new Object();

    private final InputKernel kernel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private HandwritingCanvas canvas;
    private Runnable pendingRecognize;
    private int generation;

    public HandwritingKeyboardView(Context context, InputKernel inputKernel) {
        super(context);
        kernel = inputKernel;
        build();
    }

    private void build() {
        LinearLayout writingRow = new LinearLayout(getContext());
        writingRow.setOrientation(HORIZONTAL);
        writingRow.setPadding(dp(4), 0, dp(4), dp(3));
        addView(writingRow, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
        canvas = new HandwritingCanvas(getContext());
        canvas.setContentDescription("手写区");
        canvas.setStrokeListener(new HandwritingCanvas.StrokeListener() {
            @Override public void onStrokeStart() { cancelRecognition(); }
            @Override public void onStrokeEnd() { scheduleRecognition(); }
        });
        writingRow.addView(canvas, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1));

        LinearLayout rail = new LinearLayout(getContext());
        rail.setOrientation(VERTICAL);
        rail.setPadding(dp(5), 0, 0, 0);
        writingRow.addView(rail, new LinearLayout.LayoutParams(dp(52), LayoutParams.MATCH_PARENT));
        ImageButton erase = railIcon(R.drawable.ic_handwriting_backspace, "删除文字");
        erase.setContentDescription("删除文字");
        erase.setOnClickListener(v -> emit(SimeKey.backspace()));
        rail.addView(erase, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(48)));
        ScrollView punctuationScroll = new ScrollView(getContext());
        punctuationScroll.setVerticalScrollBarEnabled(false);
        punctuationScroll.setFillViewport(true);
        rail.addView(punctuationScroll, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout punctuationStrip = new LinearLayout(getContext());
        punctuationStrip.setOrientation(LinearLayout.VERTICAL);
        punctuationScroll.addView(punctuationStrip, new ScrollView.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        for (int i = 0; i < CommonPunctuation.SIDE_STRIP.length; i++) {
            String punctuation = CommonPunctuation.SIDE_STRIP[i];
            final String value = punctuation;
            punctuationStrip.addView(flatRailPunctuation(value));
            if (i + 1 < CommonPunctuation.SIDE_STRIP.length) {
                View divider = new View(getContext());
                divider.setBackgroundColor(theme.dividerColor);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT, dp(1));
                params.setMargins(dp(6), 0, dp(6), 0);
                punctuationStrip.addView(divider, params);
            }
        }

        LinearLayout bottom = new LinearLayout(getContext());
        bottom.setPadding(dp(4), 0, dp(4), 0);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        // Match QWERTY's bottom-row key height (57dp row, 5dp insets),
        // but give handwriting's space bar a little more room.
        addView(bottom, new LayoutParams(LayoutParams.MATCH_PARENT, dp(57)));
        addBottom(bottom, "符号", 1.5f, SimeKey.toSymbol(), true);
        addBottom(bottom, "123", 1f, SimeKey.toNumber(), true);
        addBottom(bottom, "", 3.2f, SimeKey.space(), false);
        addBottom(bottom, "中", 1f, SimeKey.toggleLang(), true);
        addBottom(bottom, "换行", 1.5f, SimeKey.enter(), true);
    }

    private ImageButton railIcon(int resource, String description) {
        ImageButton view = new ImageButton(getContext());
        view.setImageResource(resource);
        view.setImageTintList(ColorStateList.valueOf(theme.keyText));
        view.setContentDescription(description);
        // The punctuation column is visually centered a little left of the
        // rail's geometric center because its divider has a right edge.
        view.setPadding(dp(6), dp(8), dp(16), dp(8));
        view.setBackground(makeKeySelector(theme.keyboardBackground, theme.keyboardBackground));
        return view;
    }

    private TextView flatRailPunctuation(String value) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(Typography.BODY);
        view.setTextColor(theme.keyText);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        view.setContentDescription(value);
        InputFeedbacks.wireClick(view, () -> emit(SimeKey.punctuation(value)));
        view.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dp(36)));
        return view;
    }

    private void addBottom(LinearLayout row, String label, float weight, SimeKey key,
                           boolean emphasized) {
        addBottom(row, label, weight, () -> emit(key), emphasized);
    }

    private void addBottom(LinearLayout row, String label, float weight, Runnable action,
                           boolean emphasized) {
        TextView view = key(label, (int) Typography.SMALL,
                emphasized ? theme.functionKeyBackground : theme.keyBackground,
                emphasized ? theme.functionKeyBackgroundPressed : theme.keyBackgroundPressed);
        view.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, weight);
        params.setMargins(dp(2), dp(5), dp(2), dp(5));
        row.addView(view, params);
    }

    private TextView key(String label, int textSize, int normalColor, int pressedColor) {
        TextView view = new TextView(getContext());
        view.setText(label);
        view.setTextSize(textSize);
        view.setTextColor(theme.keyText);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(!label.contains("\n"));
        view.setBackground(makeKeySelector(normalColor, pressedColor));
        return view;
    }

    private void scheduleRecognition() {
        cancelRecognition();
        final int request = ++generation;
        pendingRecognize = () -> recognize(request);
        handler.postDelayed(pendingRecognize, RECOGNIZE_DELAY_MS);
    }

    private void cancelRecognition() {
        ++generation;
        if (pendingRecognize != null) handler.removeCallbacks(pendingRecognize);
        pendingRecognize = null;
    }

    private void recognize(int request) {
        pendingRecognize = null;
        if (canvas.isEmpty()) { clearCandidates(); return; }
        Bitmap bitmap = canvas.renderForModel();
        worker.execute(() -> {
            HCCRRecognizer.Result[] result = recognizeBitmap(bitmap);
            bitmap.recycle();
            handler.post(() -> {
                if (request == generation) showCandidates(result);
            });
        });
    }

    private HCCRRecognizer.Result[] recognizeBitmap(Bitmap bitmap) {
        try {
            HCCRRecognizer loaded = getRecognizer();
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            byte[] gray = new byte[pixels.length];
            for (int i = 0; i < pixels.length; ++i) {
                int color = pixels[i];
                gray[i] = (byte) (((299 * ((color >>> 16) & 255))
                        + (587 * ((color >>> 8) & 255)) + (114 * (color & 255))) / 1000);
            }
            float[] input = new float[64 * 64];
            return loaded.preprocess(gray, width, height, input) ? loaded.predict(input, TOP_K)
                    : new HCCRRecognizer.Result[0];
        } catch (IOException | RuntimeException e) {
            return new HCCRRecognizer.Result[0];
        }
    }

    private HCCRRecognizer getRecognizer() throws IOException {
        synchronized (RECOGNIZER_LOCK) {
            if (recognizer == null) recognizer = new HCCRRecognizer(getContext().getAssets());
            return recognizer;
        }
    }

    private void showCandidates(HCCRRecognizer.Result[] result) {
        List<String> texts = new ArrayList<>(result.length);
        for (HCCRRecognizer.Result candidate : result) texts.add(candidate.text);
        kernel.setHandwritingCandidates(texts);
    }

    private void clearCandidates() {
        kernel.clearHandwritingCandidates();
    }

    /** Called by InputView after a tap in the shared candidates bar. */
    public void clearAfterCandidatePick() {
        // InputKernel owns the shared candidate strip and must consume the
        // picked item first.  Clearing that strip here races its posted pick
        // task and can turn a valid tap into a no-op.
        cancelRecognition();
        canvas.clear();
    }

    /** Called by InputView before this detached view is discarded. */
    public void dispose() {
        cancelRecognition();
        worker.shutdownNow();
    }
}
