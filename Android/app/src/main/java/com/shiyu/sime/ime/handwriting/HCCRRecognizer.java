package com.shiyu.sime.ime.handwriting;

import android.content.res.AssetManager;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Small Java facade for the bundled offline handwritten-character model. */
public final class HCCRRecognizer {
    static {
        System.loadLibrary("sime_jni");
    }
    private long nativeHandle;
    private final String[] idxToChar;

    public HCCRRecognizer(AssetManager assets) throws IOException {
        nativeHandle = nativeInit(assets, "mbv2_aug.int8.ncnn.param",
                "mbv2_aug.int8.ncnn.bin");
        if (nativeHandle == 0) throw new IOException("手写模型加载失败");
        idxToChar = loadCharset(assets);
    }

    public Result[] predict(float[] input, int k) {
        int[] indices = new int[k];
        float[] probabilities = new float[k];
        int n = nativePredict(nativeHandle, input, indices, probabilities, k);
        Result[] result = new Result[Math.max(0, n)];
        for (int i = 0; i < result.length; i++) {
            int index = indices[i];
            String text = index >= 0 && index < idxToChar.length ? idxToChar[index] : "?";
            result[i] = new Result(text, probabilities[i]);
        }
        return result;
    }

    public boolean preprocess(byte[] gray, int width, int height, float[] output) {
        return nativePreprocess(gray, width, height, output);
    }

    private static String[] loadCharset(AssetManager assets) throws IOException {
        try (InputStream stream = assets.open("charset.json")) {
            byte[] bytes = stream.readAllBytes();
            JSONObject root = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            String[] chars = new String[root.getInt("num_classes")];
            JSONObject map = root.getJSONObject("char_to_idx");
            java.util.Iterator<String> it = map.keys();
            while (it.hasNext()) {
                String text = it.next();
                int index = map.getInt(text);
                if (index >= 0 && index < chars.length) chars[index] = text;
            }
            return chars;
        } catch (Exception e) {
            throw new IOException("手写字表加载失败", e);
        }
    }

    public static final class Result {
        public final String text;
        public final float probability;
        Result(String text, float probability) {
            this.text = text;
            this.probability = probability;
        }
    }

    private static native long nativeInit(AssetManager assets, String param, String bin);
    private static native int nativePredict(long handle, float[] input, int[] indices,
                                            float[] probabilities, int k);
    private static native boolean nativePreprocess(byte[] gray, int width, int height,
                                                   float[] output);
}
