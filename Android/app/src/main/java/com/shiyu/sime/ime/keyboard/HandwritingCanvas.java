package com.shiyu.sime.ime.keyboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import com.shiyu.sime.ime.theme.SimeTheme;

/** Writing surface. The model rendering intentionally mirrors Handwritten. */
final class HandwritingCanvas extends View {
    interface StrokeListener { void onStrokeStart(); void onStrokeEnd(); }

    private static final float DISPLAY_RATIO = 0.018f;
    private static final float MODEL_RATIO = 0.044f;
    private static final int MODEL_RENDER_SIZE = 360;
    private final List<Path> strokes = new ArrayList<>();
    private final Paint displayPaint = paint(true, 0xff252a33);
    // The HCCR training and standalone Handwritten app render model strokes
    // in pure black.  Do not reuse the softer display ink here: its grayscale
    // value is part of the model input and measurably lowers confidence.
    private final Paint modelPaint = paint(false, Color.BLACK);
    private Path current;
    private StrokeListener listener;

    HandwritingCanvas(Context context) {
        super(context);
        // A continuous writing surface leaves the eye on the stroke instead
        // of framing it inside a second white card.
        setBackgroundColor(SimeTheme.fromContext(context).keyboardBackground);
    }

    private Paint paint(boolean antialias, int color) {
        Paint p = new Paint();
        p.setColor(color);
        p.setAntiAlias(antialias);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(antialias ? Paint.Cap.ROUND : Paint.Cap.BUTT);
        p.setStrokeJoin(Paint.Join.ROUND);
        return p;
    }

    void setStrokeListener(StrokeListener value) { listener = value; }
    void clear() { strokes.clear(); current = null; invalidate(); }
    void undo() { if (!strokes.isEmpty()) { strokes.remove(strokes.size() - 1); invalidate(); } }
    boolean isEmpty() { return strokes.isEmpty(); }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        displayPaint.setStrokeWidth(width * DISPLAY_RATIO);
        modelPaint.setStrokeWidth(width * MODEL_RATIO);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Path path : strokes) canvas.drawPath(path, displayPaint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                current = new Path();
                current.moveTo(x, y);
                strokes.add(current);
                if (listener != null) listener.onStrokeStart();
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (current != null) {
                    for (int i = 0; i < event.getHistorySize(); ++i) {
                        current.lineTo(event.getHistoricalX(i), event.getHistoricalY(i));
                    }
                    current.lineTo(x, y);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (current != null) current.lineTo(x, y);
                invalidate();
                if (listener != null) listener.onStrokeEnd();
                return true;
            default:
                return true;
        }
    }

    Bitmap renderForModel() {
        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        float scale = (float) MODEL_RENDER_SIZE / Math.max(width, height);
        Bitmap bitmap = Bitmap.createBitmap(Math.max(1, Math.round(width * scale)),
                Math.max(1, Math.round(height * scale)), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.scale(scale, scale);
        for (Path path : strokes) canvas.drawPath(path, modelPaint);
        return bitmap;
    }

}
