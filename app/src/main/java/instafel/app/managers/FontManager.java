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
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import instafel.app.managers.PreferenceManager;
import instafel.app.utils.types.PreferenceKeys;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FontManager {

    public static final String FONT_TYPE_DEFAULT = "default";
    public static final String FONT_TYPE_SF_PRO = "sf_pro";
    public static final String FONT_TYPE_GOOGLE_SANS_FLEX = "google_sans_flex";
    public static final String FONT_TYPE_CUSTOM = "custom";

    private static final Map<String, Typeface> typefaceCache = new HashMap<>();
    private static Context appContext;
    private static PreferenceManager preferenceManager;

    public static void init(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();
        preferenceManager = new PreferenceManager(appContext);
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
                return getGoogleSansFlexTypeface(context, textSizePx, isBold, isItalic);
            } else if (FONT_TYPE_SF_PRO.equals(family)) {
                return getSfProTypeface(context, textSizePx, isBold, isItalic);
            } else if (FONT_TYPE_CUSTOM.equals(family)) {
                return getCustomTypeface(context, isBold, isItalic);
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
     * - Weight ('wght'): 300 to 800 based on context (heading vs body)
     * - Slant ('slnt'): -10 for italic, 0 for regular
     * - Grade ('GRAD'): 0
     */
    private static Typeface getGoogleSansFlexTypeface(Context context, float textSizePx, boolean isBold, boolean isItalic) {
        File fontFile = new File(getFontsDirectory(context), "GoogleSansFlex.ttf");
        if (!fontFile.exists()) {
            return null;
        }

        // Determine weight based on bold status and size
        int weight = 400; // Regular
        if (isBold && textSizePx >= 48) {
            weight = 800; // Headline / Title
        } else if (isBold) {
            weight = 700; // Bold Heading
        } else if (textSizePx >= 40) {
            weight = 600; // Subheading / Username
        } else if (textSizePx <= 28) {
            weight = 350; // Caption / Fine print
        }

        int slant = isItalic ? -10 : 0;
        int opsz = Math.max(6, Math.min(144, Math.round(textSizePx > 0 ? textSizePx / 2.5f : 14f)));

        String cacheKey = String.format(Locale.US, "gsf_w%d_s%d_o%d", weight, slant, opsz);
        if (typefaceCache.containsKey(cacheKey)) {
            return typefaceCache.get(cacheKey);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String variationSettings = String.format(Locale.US,
                    "'wght' %d, 'wdth' 97.5, 'opsz' %d, 'ROND' 73, 'GRAD' 0, 'slnt' %d",
                    weight, opsz, slant);
            try {
                Typeface typeface = new Typeface.Builder(fontFile)
                        .setFontVariationSettings(variationSettings)
                        .build();
                if (typeface != null) {
                    typefaceCache.put(cacheKey, typeface);
                    return typeface;
                }
            } catch (Exception ignored) {}
        }

        Typeface fallback = Typeface.createFromFile(fontFile);
        typefaceCache.put(cacheKey, fallback);
        return fallback;
    }

    /**
     * Resolves Apple San Francisco (SF Pro) across optical sizes (Display for >=20sp, Text for <20sp)
     * and weights (Regular, Medium, Semibold, Bold, Heavy, Black).
     */
    private static Typeface getSfProTypeface(Context context, float textSizePx, boolean isBold, boolean isItalic) {
        boolean isDisplay = textSizePx >= 48; // ~20sp on xx-hdpi displays
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

        File fontFile = new File(new File(getFontsDirectory(context), "sf_pro"), fileName);
        if (!fontFile.exists()) {
            // Fallback to regular file if specific weight isn't found
            File regularFallback = new File(new File(getFontsDirectory(context), "sf_pro"), "SF-Pro-Text-Regular.otf");
            if (regularFallback.exists()) {
                Typeface tf = Typeface.createFromFile(regularFallback);
                typefaceCache.put(fileName, tf);
                return tf;
            }
            return null;
        }

        Typeface tf = Typeface.createFromFile(fontFile);
        typefaceCache.put(fileName, tf);
        return tf;
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

        Typeface base = Typeface.createFromFile(f);
        int style = Typeface.NORMAL;
        if (isBold && isItalic) style = Typeface.BOLD_ITALIC;
        else if (isBold) style = Typeface.BOLD;
        else if (isItalic) style = Typeface.ITALIC;

        Typeface styled = Typeface.create(base, style);
        typefaceCache.put(key, styled);
        return styled;
    }

    /**
     * Recursively walks view tree and updates all text elements with hierarchical font weights.
     */
    public static void applyToViewHierarchy(View view) {
        if (view == null || !isCustomFontEnabled()) return;

        if (view instanceof TextView) {
            TextView tv = (TextView) view;
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
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyToViewHierarchy(group.getChildAt(i));
            }
        }
    }

    public static void applyToActivity(Activity activity) {
        if (activity == null || !isCustomFontEnabled()) return;
        View decor = activity.getWindow().getDecorView();
        decor.post(() -> applyToViewHierarchy(decor));
    }
}
