/*
 * (c) 2026 Muhammed Ali Bulut & Antigravity, All rights reserved.
 *
 * See LICENSE file in repository root for copy file of license. For copyright
 * notices, technical issues, feedback, or any other related to this code file or
 * project, please contact me via mamii@mamii.dev or other ways.
 */

package instafel.app.managers;

import android.content.Context;
import android.graphics.Typeface;
import instafel.app.managers.PreferenceManager;
import instafel.app.utils.types.PreferenceKeys;

import java.io.File;

public class EmojiManager {

    public static final String EMOJI_TYPE_DEFAULT = "default";
    public static final String EMOJI_TYPE_IOS = "ios_emoji";
    public static final String EMOJI_TYPE_GOOGLE_3D = "google_3d_emoji";
    public static final String EMOJI_TYPE_CUSTOM = "custom";

    private static Typeface cachedEmojiTypeface = null;
    private static String cachedEmojiType = "";
    private static Context appContext;
    private static PreferenceManager preferenceManager;

    public static void init(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();
        preferenceManager = new PreferenceManager(appContext);
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

        File file = getActiveEmojiFile(context);
        if (file != null && file.exists()) {
            try {
                cachedEmojiTypeface = Typeface.createFromFile(file);
                cachedEmojiType = currentType;
                return cachedEmojiTypeface;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
