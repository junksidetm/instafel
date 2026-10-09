/*
 * (c) 2026 Muhammed Ali Bulut & Antigravity, All rights reserved.
 *
 * See LICENSE file in repository root for copy file of license. For copyright
 * notices, technical issues, feedback, or any other related to this code file or
 * project, please contact me via mamii@mamii.dev or other ways.
 */

package instafel.app.managers;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.TransformationMethod;
import android.text.style.MetricAffectingSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import instafel.app.managers.PreferenceManager;
import instafel.app.utils.types.PreferenceKeys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiManager {

    public static final String EMOJI_TYPE_DEFAULT = "default";
    public static final String EMOJI_TYPE_IOS = "ios_emoji";
    public static final String EMOJI_TYPE_GOOGLE_3D = "google_3d_emoji";
    public static final String EMOJI_TYPE_CUSTOM = "custom";

    private static Typeface cachedEmojiTypeface = null;
    private static String cachedEmojiType = "";
    private static Context appContext;
    private static PreferenceManager preferenceManager;

    // Unicode Emoji sequence matcher (base pictographs, symbols, variation selectors, modifiers, flags, and ZWJ sequences)
    private static final Pattern EMOJI_PATTERN = Pattern.compile(
            "([\\uD83C-\\uD83E][\\uDC00-\\uDFFF]|[\\u2600-\\u27BF]|[\\u2B50-\\u2B55]|[\\u23E9-\\u23FA]|[\\u2194-\\u21AA])" +
            "([\\uFE0E\\uFE0F]|[\\uD83C][\\uDFFB-\\uDFFF])?" +
            "([\\u200D]([\\uD83C-\\uD83E][\\uDC00-\\uDFFF]|[\\u2600-\\u27BF])([\\uFE0E\\uFE0F]|[\\uD83C][\\uDFFB-\\uDFFF])?)*"
    );

    public static void init(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();
        preferenceManager = new PreferenceManager(appContext);
        ensureAssetsExtracted(appContext);
    }

    public static void clearCache() {
        cachedEmojiTypeface = null;
        cachedEmojiType = "";
    }

    public static boolean isCustomEmojiEnabled() {
        if (preferenceManager == null && appContext != null) {
            preferenceManager = new PreferenceManager(appContext);
        }
        return preferenceManager != null && preferenceManager.getPreferenceBoolean(PreferenceKeys.ifl_enable_custom_emojis, false);
    }

    public static String getActiveEmojiType() {
        if (preferenceManager == null && appContext != null) {
            preferenceManager = new PreferenceManager(appContext);
        }
        if (preferenceManager == null) return EMOJI_TYPE_DEFAULT;
        return preferenceManager.getPreferenceString(PreferenceKeys.ifl_custom_emoji_pack, EMOJI_TYPE_IOS);
    }

    public static File getEmojisDirectory(Context context) {
        File dir = new File(context.getFilesDir(), "emojis");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private static void ensureAssetsExtracted(Context context) {
        if (context == null) return;
        new Thread(() -> {
            try {
                File dir = getEmojisDirectory(context);
                String[] assets = new String[]{"iOS_26.4.ttf", "GoogleEmoji3D.ttf"};
                for (String name : assets) {
                    File target = new File(dir, name);
                    if (!target.exists() || target.length() == 0) {
                        try (InputStream in = context.getAssets().open("emojis/" + name);
                             OutputStream out = new FileOutputStream(target)) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }).start();
    }

    public static File getActiveEmojiFile(Context context) {
        String type = getActiveEmojiType();
        File emojisDir = getEmojisDirectory(context);

        if (EMOJI_TYPE_IOS.equals(type)) {
            return new File(emojisDir, "iOS_26.4.ttf");
        } else if (EMOJI_TYPE_GOOGLE_3D.equals(type)) {
            return new File(emojisDir, "GoogleEmoji3D.ttf");
        } else if (EMOJI_TYPE_CUSTOM.equals(type)) {
            String path = preferenceManager != null ? preferenceManager.getPreferenceString(PreferenceKeys.ifl_custom_emoji_path, "") : "";
            if (!path.isEmpty()) {
                File customFile = new File(path);
                if (customFile.exists()) return customFile;
            }
        }
        return null;
    }

    public static Typeface getEmojiTypeface(Context context) {
        if (!isCustomEmojiEnabled()) return null;

        String currentType = getActiveEmojiType();
        if (cachedEmojiTypeface != null && currentType.equals(cachedEmojiType)) {
            return cachedEmojiTypeface;
        }

        if (EMOJI_TYPE_DEFAULT.equals(currentType)) {
            return null;
        }

        // 1. Try loading directly from APK assets
        String assetPath = null;
        if (EMOJI_TYPE_IOS.equals(currentType)) {
            assetPath = "emojis/iOS_26.4.ttf";
        } else if (EMOJI_TYPE_GOOGLE_3D.equals(currentType)) {
            assetPath = "emojis/GoogleEmoji3D.ttf";
        }

        if (assetPath != null && context != null) {
            try {
                Typeface tf = Typeface.createFromAsset(context.getAssets(), assetPath);
                if (tf != null) {
                    cachedEmojiTypeface = tf;
                    cachedEmojiType = currentType;
                    return cachedEmojiTypeface;
                }
            } catch (Throwable ignored) {}
        }

        // 2. Try loading from files directory
        File file = getActiveEmojiFile(context != null ? context : appContext);
        if (file != null && file.exists()) {
            try {
                Typeface tf = Typeface.createFromFile(file);
                if (tf != null) {
                    cachedEmojiTypeface = tf;
                    cachedEmojiType = currentType;
                    return cachedEmojiTypeface;
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }

    public static CharSequence applyEmojiSpans(CharSequence text, Typeface emojiTf) {
        if (text == null || text.length() == 0 || emojiTf == null) return text;

        Matcher matcher = EMOJI_PATTERN.matcher(text);
        if (!matcher.find()) {
            return text; // No emoji glyphs present, zero allocation
        }

        Spannable spannable = (text instanceof Spannable) ? (Spannable) text : new SpannableStringBuilder(text);
        do {
            spannable.setSpan(
                    new EmojiTypefaceSpan(emojiTf),
                    matcher.start(),
                    matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        } while (matcher.find());

        return spannable;
    }

    public static void applyToTextView(TextView tv) {
        if (tv == null || !isCustomEmojiEnabled()) return;
        Typeface emojiTf = getEmojiTypeface(tv.getContext());
        if (emojiTf == null) return;

        TransformationMethod currentMethod = tv.getTransformationMethod();
        if (!(currentMethod instanceof EmojiTransformationMethod)) {
            tv.setTransformationMethod(new EmojiTransformationMethod(currentMethod, emojiTf));
        }

        if (tv instanceof EditText) {
            EditText et = (EditText) tv;
            Object tag = et.getTag(0x7F0A0001);
            if (tag == null) {
                et.setTag(0x7F0A0001, Boolean.TRUE);
                et.addTextChangedListener(new TextWatcher() {
                    private boolean formatting = false;

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (formatting || s == null) return;
                        formatting = true;
                        applyEmojiSpans(s, emojiTf);
                        formatting = false;
                    }
                });
            }
        }
    }

    public static class EmojiTypefaceSpan extends MetricAffectingSpan {
        private final Typeface typeface;

        public EmojiTypefaceSpan(Typeface typeface) {
            this.typeface = typeface;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            if (typeface != null) {
                ds.setTypeface(typeface);
            }
        }

        @Override
        public void updateMeasureState(TextPaint paint) {
            if (typeface != null) {
                paint.setTypeface(typeface);
            }
        }
    }

    public static class EmojiTransformationMethod implements TransformationMethod {
        private final TransformationMethod original;
        private final Typeface emojiTypeface;

        public EmojiTransformationMethod(TransformationMethod original, Typeface emojiTypeface) {
            this.original = original;
            this.emojiTypeface = emojiTypeface;
        }

        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            CharSequence transformed = source;
            if (original != null) {
                transformed = original.getTransformation(source, view);
            }
            if (transformed == null || transformed.length() == 0 || emojiTypeface == null) {
                return transformed;
            }
            return applyEmojiSpans(transformed, emojiTypeface);
        }

        @Override
        public void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction, android.graphics.Rect previouslyFocusedRect) {
            if (original != null) {
                original.onFocusChanged(view, sourceText, focused, direction, previouslyFocusedRect);
            }
        }
    }
}
