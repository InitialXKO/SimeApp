package com.shiyu.sime.ime.feedback;

import android.content.Context;
import android.media.AudioManager;
import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Process-global key-press feedback (click sound + haptic).
 *
 * <p>When sound is enabled we call {@link AudioManager#playSoundEffect}
 * with {@link AudioManager#FX_KEYPRESS_STANDARD}; that helper is itself
 * gated by {@code Settings.System.SOUND_EFFECTS_ENABLED}, so the IME
 * toggle effectively means "follow the system click-sound setting".
 *
 * <p>For haptic we call {@link View#performHapticFeedback} without any
 * override flags, so the system's {@code HAPTIC_FEEDBACK_ENABLED} setting
 * still applies — same "follow system" semantics.
 *
 * <p>State is held in static volatile fields so {@link com.shiyu.sime.ime.keyboard.framework.KeyView}
 * can fire feedback without plumbing prefs through every constructor.
 * {@link com.shiyu.sime.SimeService} pushes the current prefs at
 * startup and on settings changes.
 */
public final class InputFeedbacks {

    private static volatile boolean soundEnabled = true;
    private static volatile boolean vibrationEnabled = true;
    private static volatile int vibrationIntensity = 2; // 1=low, 2=medium, 3=high

    private InputFeedbacks() {}

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public static void setVibrationEnabled(boolean enabled) {
        vibrationEnabled = enabled;
    }

    public static void setVibrationIntensity(int level) {
        vibrationIntensity = Math.max(1, Math.min(3, level));
    }

    /** Fire on key DOWN. Cheap when both toggles are off. */
    public static void onKeyPress(View view) {
        if (soundEnabled) {
            AudioManager am = (AudioManager) view.getContext()
                    .getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
            }
        }
        if (vibrationEnabled) {
            int feedbackType = HapticFeedbackConstants.KEYBOARD_TAP;
            if (vibrationIntensity == 1 && android.os.Build.VERSION.SDK_INT >= 27) {
                feedbackType = HapticFeedbackConstants.TEXT_HANDLE_MOVE;
            } else if (vibrationIntensity == 3) {
                feedbackType = HapticFeedbackConstants.LONG_PRESS;
            }
            view.performHapticFeedback(feedbackType);
        }
    }

    /**
     * Wrap a click listener so the system's default click sound is
     * suppressed and our gated {@link #onKeyPress} fires instead. Use
     * this for any clickable that isn't a {@link com.shiyu.sime.ime.keyboard.framework.KeyView}
     * (candidates, toolbar buttons, etc.) — KeyView already fires
     * feedback in its own touch dispatch and bypasses {@code performClick}.
     */
    public static void wireClick(View view, Runnable action) {
        view.setSoundEffectsEnabled(false);
        view.setOnClickListener(v -> {
            onKeyPress(v);
            action.run();
        });
    }
}
