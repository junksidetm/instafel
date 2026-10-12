/*
 * (c) 2026 Muhammed Ali Bulut & Antigravity, All rights reserved.
 *
 * See LICENSE file in repository root for copy file of license. For copyright
 * notices, technical issues, feedback, or any other related to this code file or
 * project, please contact me via mamii@mamii.dev or other ways.
 */

package instafel.app.managers;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import instafel.app.managers.PreferenceManager;
import instafel.app.utils.types.PreferenceKeys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FontManager {

    public static final String FONT_TYPE_DEFAULT = "default";
    public static final String FONT_TYPE_SF_PRO = "sf_pro";
    public static final String FONT_TYPE_GOOGLE_SANS_FLEX = "google_sans_flex";
    public static final String FONT_TYPE_CUSTOM = "custom";

    private static final Map<String, Typeface> typefaceCache = new HashMap<>();
    private static Map<String, Typeface> originalSystemFontMap = null;
    private static Context appContext;
    private static PreferenceManager preferenceManager;

    public static void init(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();
        preferenceManager = new PreferenceManager(appContext);
        ensureAssetsExtracted(appContext);
        applySystemFontOverride(appContext);
    }

    public static void clearCache() {
        typefaceCache.clear();
    }

    public static boolean isCustomFontEnabled() {
        if (preferenceManager == null && appContext != null) {
            preferenceManager = new PreferenceManager(appContext);
        }
        return preferenceManager != null && preferenceManager.getPreferenceBoolean(PreferenceKeys.ifl_enable_custom_fonts, false);
    }

    public static String getActiveFontFamily() {
        if (preferenceManager == null && appContext != null) {
            preferenceManager = new PreferenceManager(appContext);
        }
        if (preferenceManager == null) return FONT_TYPE_DEFAULT;
        return preferenceManager.getPreferenceString(PreferenceKeys.ifl_custom_font_family, FONT_TYPE_SF_PRO);
    }

    public static File getFontsDirectory(Context context) {
        File dir = new File(context.getFilesDir(), "fonts");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Unpacks bundled font assets from APK assets into internal storage in the background.
     */
    public static void ensureAssetsExtracted(Context context) {
        if (context == null) return;
        new Thread(() -> {
            try {
                File fontsDir = getFontsDirectory(context);
                File sfDir = new File(fontsDir, "sf_pro");
                if (!sfDir.exists()) {
                    sfDir.mkdirs();
                }

                // Copy GoogleSansFlex.ttf if missing
                File gsfTarget = new File(fontsDir, "GoogleSansFlex.ttf");
                if (!gsfTarget.exists() || gsfTarget.length() == 0) {
                    copyAssetToFile(context, "fonts/GoogleSansFlex.ttf", gsfTarget);
                }

                // Copy SF Pro fonts if missing
                AssetManager am = context.getAssets();
                try {
                    String[] sfAssets = am.list("fonts/sf_pro");
                    if (sfAssets != null) {
                        for (String sfName : sfAssets) {
                            File target = new File(sfDir, sfName);
                            if (!target.exists() || target.length() == 0) {
                                copyAssetToFile(context, "fonts/sf_pro/" + sfName, target);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}
        }).start();
    }

    private static void copyAssetToFile(Context context, String assetPath, File dest) {
        try (InputStream in = context.getAssets().open(assetPath);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Resolves a Typeface according to the selected font family, element weight, optical size, and style.
     */
    public static Typeface resolveTypeface(Context context, Typeface original, float textSizePx, boolean isBold, boolean isItalic) {
        if (!isCustomFontEnabled()) {
            return original;
        }

        String family = getActiveFontFamily();
        if (FONT_TYPE_DEFAULT.equals(family)) {
            return original;
        }

        try {
            if (FONT_TYPE_GOOGLE_SANS_FLEX.equals(family)) {
                Typeface tf = getGoogleSansFlexTypeface(context, textSizePx, isBold, isItalic);
                return tf != null ? tf : original;
            } else if (FONT_TYPE_SF_PRO.equals(family)) {
                Typeface tf = getSfProTypeface(context, textSizePx, isBold, isItalic);
                return tf != null ? tf : original;
            } else if (FONT_TYPE_CUSTOM.equals(family)) {
                Typeface tf = getCustomTypeface(context, isBold, isItalic);
                return tf != null ? tf : original;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return original;
    }

    /**
     * Resolves Google Sans Flex with configured variable axes:
     * - Optical Size ('opsz'): ON (dynamically matched to text size)
     * - Width ('wdth'): 97.5
     * - Roundness ('ROND'): 73
     * - Weight ('wght'): 300 to 800 based on context
     * - Slant ('slnt'): -10 for italic, 0 for regular
     * - Grade ('GRAD'): 0
     */
    private static Typeface getGoogleSansFlexTypeface(Context context, float textSizePx, boolean isBold, boolean isItalic) {
        int weight = 400;
        if (isBold && textSizePx >= 48) {
            weight = 800;
        } else if (isBold) {
            weight = 700;
        } else if (textSizePx >= 40) {
            weight = 600;
        } else if (textSizePx <= 28) {
            weight = 350;
        }

        int slant = isItalic ? -10 : 0;
        int opsz = Math.max(6, Math.min(144, Math.round(textSizePx > 0 ? textSizePx / 2.5f : 14f)));

        String cacheKey = String.format(Locale.US, "gsf_w%d_s%d_o%d", weight, slant, opsz);
        if (typefaceCache.containsKey(cacheKey)) {
            return typefaceCache.get(cacheKey);
        }

        // Try Android O+ Typeface.Builder with AssetManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context != null) {
            String variationSettings = String.format(Locale.US,
                    "'wght' %d, 'wdth' 97.5, 'opsz' %d, 'ROND' 73, 'GRAD' 0, 'slnt' %d",
                    weight, opsz, slant);
            try {
                Typeface typeface = new Typeface.Builder(context.getAssets(), "fonts/GoogleSansFlex.ttf")
                        .setFontVariationSettings(variationSettings)
                        .build();
                if (typeface != null) {
                    typefaceCache.put(cacheKey, typeface);
                    return typeface;
                }
            } catch (Throwable ignored) {}

            // Try Typeface.Builder with File from filesDir
            File fontFile = new File(getFontsDirectory(context), "GoogleSansFlex.ttf");
            if (fontFile.exists()) {
                try {
                    Typeface typeface = new Typeface.Builder(fontFile)
                            .setFontVariationSettings(variationSettings)
                            .build();
                    if (typeface != null) {
                        typefaceCache.put(cacheKey, typeface);
                        return typeface;
                    }
                } catch (Throwable ignored) {}
            }
        }

        // Fallback from asset directly
        if (context != null) {
            try {
                Typeface tf = Typeface.createFromAsset(context.getAssets(), "fonts/GoogleSansFlex.ttf");
                if (tf != null) {
                    typefaceCache.put(cacheKey, tf);
                    return tf;
                }
            } catch (Throwable ignored) {}
        }

        // Fallback from file
        File fontFile = new File(getFontsDirectory(context), "GoogleSansFlex.ttf");
        if (fontFile.exists()) {
            try {
                Typeface fallback = Typeface.createFromFile(fontFile);
                typefaceCache.put(cacheKey, fallback);
                return fallback;
            } catch (Throwable ignored) {}
        }

        return null;
    }

    /**
     * Resolves Apple San Francisco (SF Pro) across optical sizes (Display for >=20sp, Text for <20sp)
     * and weights (Regular, Medium, Semibold, Bold, Heavy, Black).
     */
    private static Typeface getSfProTypeface(Context context, float textSizePx, boolean isBold, boolean isItalic) {
        boolean isDisplay = textSizePx >= 48;
        String category = isDisplay ? "SF-Pro-Display" : "SF-Pro-Text";

        String weightName;
        if (isBold && textSizePx >= 60) {
            weightName = "Black";
        } else if (isBold && textSizePx >= 48) {
            weightName = "Heavy";
        } else if (isBold) {
            weightName = "Bold";
        } else if (textSizePx >= 38) {
            weightName = "Semibold";
        } else if (textSizePx <= 26) {
            weightName = "Light";
        } else {
            weightName = "Regular";
        }

        String variant = isItalic ? weightName + "Italic" : weightName;
        String fileName = String.format("%s-%s.otf", category, variant);

        if (typefaceCache.containsKey(fileName)) {
            return typefaceCache.get(fileName);
        }

        // 1. Try loading directly from APK assets
        if (context != null) {
            try {
                Typeface tf = Typeface.createFromAsset(context.getAssets(), "fonts/sf_pro/" + fileName);
                if (tf != null) {
                    typefaceCache.put(fileName, tf);
                    return tf;
                }
            } catch (Throwable ignored) {}
        }

        // 2. Try loading from filesDir
        File fontFile = new File(new File(getFontsDirectory(context), "sf_pro"), fileName);
        if (fontFile.exists()) {
            try {
                Typeface tf = Typeface.createFromFile(fontFile);
                if (tf != null) {
                    typefaceCache.put(fileName, tf);
                    return tf;
                }
            } catch (Throwable ignored) {}
        }

        // 3. Fallback to regular Text variant from assets
        if (context != null) {
            try {
                Typeface regularFallback = Typeface.createFromAsset(context.getAssets(), "fonts/sf_pro/SF-Pro-Text-Regular.otf");
                if (regularFallback != null) {
                    typefaceCache.put(fileName, regularFallback);
                    return regularFallback;
                }
            } catch (Throwable ignored) {}
        }

        // 4. Fallback to regular Text variant from file
        File regularFallback = new File(new File(getFontsDirectory(context), "sf_pro"), "SF-Pro-Text-Regular.otf");
        if (regularFallback.exists()) {
            try {
                Typeface tf = Typeface.createFromFile(regularFallback);
                typefaceCache.put(fileName, tf);
                return tf;
            } catch (Throwable ignored) {}
        }

        return null;
    }

    private static Typeface getCustomTypeface(Context context, boolean isBold, boolean isItalic) {
        if (preferenceManager == null) return null;
        String path = preferenceManager.getPreferenceString(PreferenceKeys.ifl_custom_font_path, "");
        if (path.isEmpty()) return null;

        File f = new File(path);
        if (!f.exists()) return null;

        String key = "custom_" + (isBold ? "b" : "n") + "_" + (isItalic ? "i" : "n");
        if (typefaceCache.containsKey(key)) {
            return typefaceCache.get(key);
        }

        try {
            Typeface base = Typeface.createFromFile(f);
            int style = Typeface.NORMAL;
            if (isBold && isItalic) style = Typeface.BOLD_ITALIC;
            else if (isBold) style = Typeface.BOLD;
            else if (isItalic) style = Typeface.ITALIC;

            Typeface styled = Typeface.create(base, style);
            typefaceCache.put(key, styled);
            return styled;
        } catch (Throwable ignored) {}

        return null;
    }

    private static volatile boolean hiddenApiBypassed = false;

    private static void bypassHiddenApiRestrictions() {
        if (hiddenApiBypassed) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                java.lang.reflect.Method forName = Class.class.getDeclaredMethod("forName", String.class);
                java.lang.reflect.Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);
                Class<?> vmRuntimeClass = (Class<?>) forName.invoke(null, "dalvik.system.VMRuntime");
                java.lang.reflect.Method getRuntime = (java.lang.reflect.Method) getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", (Class<?>[]) null);
                java.lang.reflect.Method setHiddenApiExemptions = (java.lang.reflect.Method) getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class<?>[]{String[].class});
                Object vmRuntime = getRuntime.invoke(null);
                setHiddenApiExemptions.invoke(vmRuntime, (Object) new String[]{"L"});
                hiddenApiBypassed = true;
            } catch (Throwable ignored) {}
        } else {
            hiddenApiBypassed = true;
        }
    }

    private static void setStaticField(Class<?> clazz, String fieldName, Object value) {
        bypassHiddenApiRestrictions();
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(null, value);
            return;
        } catch (Throwable ignored) {}

        // Fallback using sun.misc.Unsafe to bypass static final constraints on ART
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            Object base = unsafe.staticFieldBase(f);
            long offset = unsafe.staticFieldOffset(f);
            unsafe.putObject(base, offset, value);
        } catch (Throwable ignored) {}
    }

    /**
     * Reflectively hooks into Typeface.sSystemFontMap and standard static Typeface fields
     * to globally replace sans-serif, roboto, and Instagram fonts with custom typography.
     */
    @SuppressWarnings("unchecked")
    public static void applySystemFontOverride(Context context) {
        if (context == null) return;
        bypassHiddenApiRestrictions();

        try {
            Field field = Typeface.class.getDeclaredField("sSystemFontMap");
            field.setAccessible(true);
            Map<String, Typeface> systemFontMap = (Map<String, Typeface>) field.get(null);

            if (originalSystemFontMap == null && systemFontMap != null) {
                originalSystemFontMap = new HashMap<>(systemFontMap);
            }

            if (!isCustomFontEnabled()) {
                if (originalSystemFontMap != null) {
                    setStaticField(Typeface.class, "sSystemFontMap", Collections.unmodifiableMap(originalSystemFontMap));
                }
                return;
            }

            Typeface regular = resolveTypeface(context, null, 32f, false, false);
            Typeface bold = resolveTypeface(context, null, 32f, true, false);
            Typeface medium = resolveTypeface(context, null, 40f, false, false);
            Typeface light = resolveTypeface(context, null, 24f, false, false);
            Typeface italic = resolveTypeface(context, null, 32f, false, true);
            Typeface boldItalic = resolveTypeface(context, null, 32f, true, true);

            if (regular == null) return;
            if (bold == null) bold = regular;
            if (medium == null) medium = regular;
            if (light == null) light = regular;
            if (italic == null) italic = regular;
            if (boldItalic == null) boldItalic = bold;

            Map<String, Typeface> newMap = new HashMap<>();
            if (originalSystemFontMap != null) {
                newMap.putAll(originalSystemFontMap);
            } else if (systemFontMap != null) {
                newMap.putAll(systemFontMap);
            }

            // Standard Android / Roboto typography mappings
            newMap.put("sans-serif", regular);
            newMap.put("sans-serif-regular", regular);
            newMap.put("sans-serif-medium", medium);
            newMap.put("sans-serif-bold", bold);
            newMap.put("sans-serif-light", light);
            newMap.put("sans-serif-thin", light);
            newMap.put("sans-serif-black", bold);
            newMap.put("sans-serif-condensed", regular);
            newMap.put("sans-serif-condensed-light", light);
            newMap.put("sans-serif-condensed-medium", medium);
            newMap.put("sans-serif-condensed-bold", bold);
            newMap.put("roboto", regular);
            newMap.put("roboto-regular", regular);
            newMap.put("roboto-medium", medium);
            newMap.put("roboto-bold", bold);
            newMap.put("roboto-light", light);
            newMap.put("serif", regular);
            newMap.put("default", regular);
            newMap.put("default-bold", bold);
            newMap.put("normal", regular);
            newMap.put("bold", bold);

            // Instagram-specific typography mappings
            newMap.put("Instagram Sans", regular);
            newMap.put("Instagram Sans Regular", regular);
            newMap.put("Instagram Sans Medium", medium);
            newMap.put("Instagram Sans Bold", bold);
            newMap.put("Instagram Sans Headline", bold);
            newMap.put("Instagram Sans Condensed", regular);
            newMap.put("InstagramSans", regular);
            newMap.put("InstagramSans-Regular", regular);
            newMap.put("InstagramSans-Medium", medium);
            newMap.put("InstagramSans-Bold", bold);
            newMap.put("InstagramSans-Headline", bold);
            newMap.put("InstagramSans-Condensed", regular);
            newMap.put("InstagramSansCondensed-Bold", bold);
            newMap.put("InstagramSansScript-Regular", regular);
            newMap.put("InstagramSansScript-Bold", bold);
            newMap.put("instagram-sans", regular);
            newMap.put("instagram_sans", regular);
            newMap.put("ig_sans", regular);
            newMap.put("ig-sans", regular);
            newMap.put("IGSans", regular);
            newMap.put("IGSans-Regular", regular);
            newMap.put("IGSans-Medium", medium);
            newMap.put("IGSans-Bold", bold);
            newMap.put("Instagram Sans UI", regular);
            newMap.put("InstagramSansUI", regular);
            newMap.put("InstagramSansUI-Regular", regular);
            newMap.put("InstagramSansUI-Medium", medium);
            newMap.put("InstagramSansUI-Bold", bold);

            // Map custom emoji typeface into fallback font map if enabled
            if (EmojiManager.isCustomEmojiEnabled()) {
                Typeface emojiTf = EmojiManager.getEmojiTypeface(context);
                if (emojiTf != null) {
                    newMap.put("emoji", emojiTf);
                    newMap.put("NotoColorEmoji", emojiTf);
                    newMap.put("noto-color-emoji", emojiTf);
                    newMap.put("AndroidEmoji", emojiTf);
                    newMap.put("AndroidEmoji.ttf", emojiTf);
                    newMap.put("AppleColorEmoji", emojiTf);
                }
            }

            // Write updated system font map
            setStaticField(Typeface.class, "sSystemFontMap", Collections.unmodifiableMap(newMap));

            // Reflectively set standard Typeface static fields
            setStaticField(Typeface.class, "DEFAULT", regular);
            setStaticField(Typeface.class, "DEFAULT_BOLD", bold);
            setStaticField(Typeface.class, "SANS_SERIF", regular);
            setStaticField(Typeface.class, "SERIF", regular);

            // Reflectively set Typeface.sDefaults array [NORMAL, BOLD, ITALIC, BOLD_ITALIC]
            Typeface[] newDefaults = new Typeface[]{ regular, bold, italic, boldItalic };
            setStaticField(Typeface.class, "sDefaults", newDefaults);

            // Hook EmojiCompat so Instagram's emoji framework utilizes the selected pack
            EmojiManager.hookEmojiCompat(context);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Recursively walks view tree and updates all text elements with hierarchical font weights and emojis.
     */
    public static void applyToViewHierarchy(View view) {
        if (view == null) return;

        if (view instanceof TextView) {
            TextView tv = (TextView) view;

            // Apply Custom Font
            if (isCustomFontEnabled()) {
                Typeface currentTf = tv.getTypeface();
                boolean isBold = false;
                boolean isItalic = false;

                if (currentTf != null) {
                    isBold = currentTf.isBold();
                    isItalic = currentTf.isItalic();
                }

                Paint paint = tv.getPaint();
                if (paint != null && paint.isFakeBoldText()) {
                    isBold = true;
                }

                float textSize = tv.getTextSize();
                Typeface target = resolveTypeface(tv.getContext(), currentTf, textSize, isBold, isItalic);
                if (target != null && target != currentTf) {
                    tv.setTypeface(target);
                }
            }

            // Apply Custom Emoji Spans & Transformation
            if (EmojiManager.isCustomEmojiEnabled()) {
                EmojiManager.applyToTextView(tv);
            }

        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            // Attach listener for views added dynamically in RecyclerView / DMs
            Object listenerTag = group.getTag(0x7F0A0002);
            if (listenerTag == null) {
                group.setTag(0x7F0A0002, Boolean.TRUE);
                group.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                    @Override
                    public void onChildViewAdded(View parent, View child) {
                        applyToViewHierarchy(child);
                    }

                    @Override
                    public void onChildViewRemoved(View parent, View child) {}
                });
            }

            for (int i = 0; i < group.getChildCount(); i++) {
                applyToViewHierarchy(group.getChildAt(i));
            }
        }
    }

    /**
     * Attaches global layout listener and applies custom typography and emojis to the activity decor view.
     */
    public static void applyToActivity(Activity activity) {
        if (activity == null) return;
        applySystemFontOverride(activity);

        if (!isCustomFontEnabled() && !EmojiManager.isCustomEmojiEnabled()) return;

        View decor = activity.getWindow().getDecorView();
        decor.post(() -> applyToViewHierarchy(decor));

        // Attach listener for dynamic Instagram Litho / RecyclerView components
        ViewTreeObserver observer = decor.getViewTreeObserver();
        if (observer != null && observer.isAlive()) {
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                private long lastRun = 0;

                @Override
                public void onGlobalLayout() {
                    long now = System.currentTimeMillis();
                    if (now - lastRun > 150) { // Throttled to avoid unnecessary cycles
                        lastRun = now;
                        applyToViewHierarchy(decor);
                    }
                }
            });
        }
    }
}
