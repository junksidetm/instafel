/*
 * (c) 2026 Muhammed Ali Bulut & Antigravity, All rights reserved.
 *
 * See LICENSE file in repository root for copy file of license. For copyright
 * notices, technical issues, feedback, or any other related to this code file or
 * project, please contact me via mamii@mamii.dev or other ways.
 */

package instafel.app.activity;

import static instafel.app.utils.localization.LocalizationUtils.updateIflLocale;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import instafel.app.R;
import instafel.app.managers.EmojiManager;
import instafel.app.managers.FontManager;
import instafel.app.managers.PreferenceManager;
import instafel.app.ui.TileLarge;
import instafel.app.ui.TileLargeSwitch;
import instafel.app.utils.GeneralFn;
import instafel.app.utils.types.PreferenceKeys;

public class ifl_a_typography extends AppCompatActivity {

    private PreferenceManager preferenceManager;
    private TileLargeSwitch switchFont;
    private TileLargeSwitch switchEmoji;
    private TileLarge tileSelectFont;
    private TileLarge tileSelectEmoji;

    private TextView previewTitle;
    private TextView previewSubhead;
    private TextView previewBody;
    private TextView previewItalic;
    private TextView previewEmojis;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        GeneralFn.updateIflUi(this);
        updateIflLocale(this, false);
        setContentView(R.layout.ifl_at_typography);

        preferenceManager = new PreferenceManager(this);

        initViews();
        setupListeners();
        updateUiState();
        updatePreviews();
    }

    private void initViews() {
        switchFont = findViewById(R.id.ifl_switch_custom_font);
        switchEmoji = findViewById(R.id.ifl_switch_custom_emoji);
        tileSelectFont = findViewById(R.id.ifl_tile_select_font);
        tileSelectEmoji = findViewById(R.id.ifl_tile_select_emoji);

        previewTitle = findViewById(R.id.ifl_preview_title);
        previewSubhead = findViewById(R.id.ifl_preview_subhead);
        previewBody = findViewById(R.id.ifl_preview_body);
        previewItalic = findViewById(R.id.ifl_preview_italic);
        previewEmojis = findViewById(R.id.ifl_preview_emojis);
    }

    private void setupListeners() {
        Switch fontSwitchView = switchFont.getSwitchView();
        fontSwitchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferenceManager.setPreferenceBoolean(PreferenceKeys.ifl_enable_custom_fonts, isChecked);
            FontManager.clearCache();
            FontManager.applySystemFontOverride(this);
            updatePreviews();
            Toast.makeText(this, isChecked ? "Custom fonts enabled" : "Custom fonts disabled", Toast.LENGTH_SHORT).show();
        });

        Switch emojiSwitchView = switchEmoji.getSwitchView();
        emojiSwitchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferenceManager.setPreferenceBoolean(PreferenceKeys.ifl_enable_custom_emojis, isChecked);
            EmojiManager.clearCache();
            FontManager.applySystemFontOverride(this);
            updatePreviews();
            Toast.makeText(this, isChecked ? "Custom emojis enabled" : "Custom emojis disabled", Toast.LENGTH_SHORT).show();
        });

        tileSelectFont.setOnClickListener(v -> showFontSelectionDialog());
        tileSelectEmoji.setOnClickListener(v -> showEmojiSelectionDialog());
    }

    private void updateUiState() {
        boolean fontsEnabled = preferenceManager.getPreferenceBoolean(PreferenceKeys.ifl_enable_custom_fonts, false);
        switchFont.getSwitchView().setChecked(fontsEnabled);

        boolean emojisEnabled = preferenceManager.getPreferenceBoolean(PreferenceKeys.ifl_enable_custom_emojis, false);
        switchEmoji.getSwitchView().setChecked(emojisEnabled);

        String activeFont = preferenceManager.getPreferenceString(PreferenceKeys.ifl_custom_font_family, FontManager.FONT_TYPE_SF_PRO);
        tileSelectFont.setSubtitleText(getFontLabel(activeFont));

        String activeEmoji = preferenceManager.getPreferenceString(PreferenceKeys.ifl_custom_emoji_pack, EmojiManager.EMOJI_TYPE_IOS);
        tileSelectEmoji.setSubtitleText(getEmojiLabel(activeEmoji));
    }

    private String getFontLabel(String key) {
        if (FontManager.FONT_TYPE_SF_PRO.equals(key)) {
            return getString(R.string.ifl_font_sf_pro);
        } else if (FontManager.FONT_TYPE_GOOGLE_SANS_FLEX.equals(key)) {
            return getString(R.string.ifl_font_google_sans_flex);
        }
        return getString(R.string.ifl_font_default);
    }

    private String getEmojiLabel(String key) {
        if (EmojiManager.EMOJI_TYPE_IOS.equals(key)) {
            return getString(R.string.ifl_emoji_ios);
        } else if (EmojiManager.EMOJI_TYPE_GOOGLE_3D.equals(key)) {
            return getString(R.string.ifl_emoji_google_3d);
        }
        return getString(R.string.ifl_emoji_default);
    }

    private void showFontSelectionDialog() {
        String[] options = new String[]{
                getString(R.string.ifl_font_sf_pro),
                getString(R.string.ifl_font_google_sans_flex),
                getString(R.string.ifl_font_default)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.ifl_font_family_picker_title)
                .setItems(options, (dialog, which) -> {
                    String selectedKey;
                    if (which == 0) selectedKey = FontManager.FONT_TYPE_SF_PRO;
                    else if (which == 1) selectedKey = FontManager.FONT_TYPE_GOOGLE_SANS_FLEX;
                    else selectedKey = FontManager.FONT_TYPE_DEFAULT;

                    preferenceManager.setPreferenceString(PreferenceKeys.ifl_custom_font_family, selectedKey);
                    FontManager.clearCache();
                    FontManager.applySystemFontOverride(this);
                    tileSelectFont.setSubtitleText(options[which]);
                    updatePreviews();
                })
                .show();
    }

    private void showEmojiSelectionDialog() {
        String[] options = new String[]{
                getString(R.string.ifl_emoji_ios),
                getString(R.string.ifl_emoji_google_3d),
                getString(R.string.ifl_emoji_default)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.ifl_emoji_picker_title)
                .setItems(options, (dialog, which) -> {
                    String selectedKey;
                    if (which == 0) selectedKey = EmojiManager.EMOJI_TYPE_IOS;
                    else if (which == 1) selectedKey = EmojiManager.EMOJI_TYPE_GOOGLE_3D;
                    else selectedKey = EmojiManager.EMOJI_TYPE_DEFAULT;

                    preferenceManager.setPreferenceString(PreferenceKeys.ifl_custom_emoji_pack, selectedKey);
                    EmojiManager.clearCache();
                    FontManager.applySystemFontOverride(this);
                    tileSelectEmoji.setSubtitleText(options[which]);
                    updatePreviews();
                })
                .show();
    }

    private void updatePreviews() {
        // Typography Preview
        Typeface titleTf = FontManager.resolveTypeface(this, null, previewTitle.getTextSize(), true, false);
        if (titleTf != null) previewTitle.setTypeface(titleTf);
        else previewTitle.setTypeface(Typeface.DEFAULT_BOLD);

        Typeface subheadTf = FontManager.resolveTypeface(this, null, previewSubhead.getTextSize(), false, false);
        if (subheadTf != null) previewSubhead.setTypeface(subheadTf);
        else previewSubhead.setTypeface(Typeface.DEFAULT);

        Typeface bodyTf = FontManager.resolveTypeface(this, null, previewBody.getTextSize(), false, false);
        if (bodyTf != null) previewBody.setTypeface(bodyTf);
        else previewBody.setTypeface(Typeface.DEFAULT);

        Typeface italicTf = FontManager.resolveTypeface(this, null, previewItalic.getTextSize(), false, true);
        if (italicTf != null) previewItalic.setTypeface(italicTf);
        else previewItalic.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));

        // Emoji Preview
        Typeface emojiTf = EmojiManager.getEmojiTypeface(this);
        if (emojiTf != null) {
            previewEmojis.setTypeface(emojiTf);
            EmojiManager.applyToTextView(previewEmojis);
        } else {
            previewEmojis.setTypeface(Typeface.DEFAULT);
        }
    }
}
