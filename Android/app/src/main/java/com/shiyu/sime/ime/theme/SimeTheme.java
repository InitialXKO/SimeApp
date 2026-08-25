package com.shiyu.sime.ime.theme;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

/**
 * Theme palette + sizing for the IME. Two presets (light / dark) chosen
 * by the system uiMode. Includes both colors and a small set of layout
 * dimensions so the framework's {@code KeyView} can read everything
 * from one place.
 */
public final class SimeTheme {

    // ===== Bar / surface =====
    public final int barBackground;
    public final int barForeground;
    public final int keyboardBackground;

    // ===== Normal key =====
    public final int keyBackground;
    public final int keyBackgroundPressed;
    public final int keyText;

    // ===== Function key =====
    public final int functionKeyBackground;
    public final int functionKeyBackgroundPressed;
    public final int keyTextFunction;

    // ===== Misc =====
    public final int candidateText;
    public final int candidateHighlight;
    public final int preeditText;
    public final int dividerColor;
    public final int accentColor;
    public final int hintLabelColor;
    public final int keyShadowColor;

    // ===== Layout dimensions (dp) =====
    public final int keyCornerRadiusDp;
    public final int keyShadowDyDp;
    public final int keyShadowRadiusDp;

    private SimeTheme(int[] c) {
        barBackground                = c[0];
        barForeground                = c[1];
        keyboardBackground           = c[2];
        keyBackground                = c[3];
        keyBackgroundPressed         = c[4];
        keyText                      = c[5];
        functionKeyBackground        = c[6];
        functionKeyBackgroundPressed = c[7];
        keyTextFunction              = c[8];
        candidateText                = c[9];
        candidateHighlight           = c[10];
        preeditText                  = c[11];
        dividerColor                 = c[12];
        accentColor                  = c[13];
        hintLabelColor               = c[14];
        keyShadowColor               = c[15];

        keyCornerRadiusDp = 8;
        keyShadowDyDp     = 1;
        keyShadowRadiusDp = 2;
    }

    public static SimeTheme light() {
        return new SimeTheme(new int[]{
            Color.parseColor("#F7F9FC"),  // barBackground
            Color.parseColor("#1C1B1F"),  // barForeground
            Color.parseColor("#EFEFF4"),  // keyboardBackground
            Color.parseColor("#FFFFFF"),  // keyBackground
            Color.parseColor("#D8DEF0"),  // keyBackgroundPressed
            Color.parseColor("#1C1B1F"),  // keyText
            Color.parseColor("#E1E6ED"),  // functionKeyBackground
            Color.parseColor("#C5CBD6"),  // functionKeyBackgroundPressed
            Color.parseColor("#30343D"),  // keyTextFunction
            Color.parseColor("#1C1B1F"),  // candidateText
            Color.parseColor("#1B6EF3"),  // candidateHighlight
            Color.parseColor("#505A69"),  // preeditText
            Color.parseColor("#E0E4EA"),  // dividerColor
            Color.parseColor("#1B6EF3"),  // accentColor
            Color.parseColor("#7A8599"),  // hintLabelColor
            Color.parseColor("#26000000"), // keyShadowColor (~15% black)
        });
    }

    public static SimeTheme dark() {
        return new SimeTheme(new int[]{
            Color.parseColor("#1C1B1F"),  // barBackground
            Color.parseColor("#E6E1E5"),  // barForeground
            Color.parseColor("#121316"),  // keyboardBackground
            Color.parseColor("#2B2D33"),  // keyBackground
            Color.parseColor("#3E424B"),  // keyBackgroundPressed
            Color.parseColor("#F2F2F7"),  // keyText
            Color.parseColor("#202227"),  // functionKeyBackground
            Color.parseColor("#353842"),  // functionKeyBackgroundPressed
            Color.parseColor("#C4C7C6"),  // keyTextFunction
            Color.parseColor("#F2F2F7"),  // candidateText
            Color.parseColor("#5294FF"),  // candidateHighlight
            Color.parseColor("#A2A8B5"),  // preeditText
            Color.parseColor("#2D3038"),  // dividerColor
            Color.parseColor("#5294FF"),  // accentColor
            Color.parseColor("#8E94A0"),  // hintLabelColor
            Color.parseColor("#59000000"), // keyShadowColor (~35% black)
        });
    }

    public static SimeTheme fromContext(Context ctx) {
        int mode = ctx.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return (mode == Configuration.UI_MODE_NIGHT_YES) ? dark() : light();
    }
}
