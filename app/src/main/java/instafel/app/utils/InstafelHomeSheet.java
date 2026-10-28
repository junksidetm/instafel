/*
 * (c) 2026 Muhammed Ali Bulut & Antigravity, All rights reserved.
 *
 * See LICENSE file in repository root for copy file of license. For copyright
 * notices, technical issues, feedback, or any other related to this code file or
 * project, please contact me via mamii@mamii.dev or other ways.
 */

package instafel.app.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import instafel.app.InstafelEnv;
import instafel.app.R;
import instafel.app.utils.localization.LocalizationUtils;
import instafel.app.utils.localization.LocalizedStringGetter;

import java.util.Locale;

public class InstafelHomeSheet {

    private Activity act;
    private Dialog sheetDialog = null;
    private int sheetThemeMode;
    private Locale iflLocale;

    public InstafelHomeSheet(Activity act) {
        this.act = act;
        this.sheetThemeMode = GeneralFn.getUiMode(act);
        this.iflLocale = LocalizationUtils.getIflLocale(act);
    }

    public void buildAndShowDialog() {
        try {
            buildSheet();
            sheetDialog.show();
        } catch (Exception e) {
            Toast.makeText(act, "An error occurred while building / showing dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void buildSheet() {
        Dialog dialog = new Dialog(act);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(8), dp(16), dp(24));

        GradientDrawable bg = new GradientDrawable();
        if (sheetThemeMode == 0 || sheetThemeMode == 1) {
            bg.setColor(act.getResources().getColor(R.color.ifl_sheet_background_dark));
        } else {
            bg.setColor(act.getResources().getColor(R.color.ifl_sheet_background_light));
        }
        bg.setCornerRadii(new float[]{
                dp(28), dp(28), // Top-left
                dp(28), dp(28), // Top-right
                0, 0,           // Bottom-right
                0, 0            // Bottom-left
        });
        root.setBackground(bg);

        // Material 3 Drag Handle Pill
        View dragHandle = new View(act);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(dp(32), dp(4));
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.topMargin = dp(4);
        handleLp.bottomMargin = dp(16);
        dragHandle.setLayoutParams(handleLp);

        GradientDrawable handleBg = new GradientDrawable();
        int handleColor = (sheetThemeMode == 0 || sheetThemeMode == 1)
                ? act.getResources().getColor(R.color.ifl_sheet_handle_dark)
                : act.getResources().getColor(R.color.ifl_sheet_handle_light);
        handleBg.setColor(handleColor);
        handleBg.setCornerRadius(dp(100));
        dragHandle.setBackground(handleBg);
        root.addView(dragHandle);

        buildSheetContent(root);

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );

            window.setWindowAnimations(android.R.style.Animation_Dialog);
        }
        this.sheetDialog = dialog;
    }

    private void buildSheetContent(LinearLayout root) {
        int mainText;
        int subText;
        int primaryAccent;
        if (sheetThemeMode == 0 || sheetThemeMode == 1) {
            mainText = act.getResources().getColor(R.color.ifl_sheet_text_main_dark);
            subText = act.getResources().getColor(R.color.ifl_sheet_text_sub_dark);
            primaryAccent = act.getResources().getColor(R.color.ifl_m3_primary_dark);
        } else {
            mainText = act.getResources().getColor(R.color.ifl_sheet_text_main_light);
            subText = act.getResources().getColor(R.color.ifl_sheet_text_sub_light);
            primaryAccent = act.getResources().getColor(R.color.ifl_m3_primary_light);
        }

        // Header
        LinearLayout header = new LinearLayout(act);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(4), dp(8), dp(12));
        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Expressive App Icon Squircle Container
        LinearLayout iconContainer = new LinearLayout(act);
        LinearLayout.LayoutParams icContainerLp = new LinearLayout.LayoutParams(dp(48), dp(48));
        iconContainer.setLayoutParams(icContainerLp);
        iconContainer.setGravity(Gravity.CENTER);
        GradientDrawable icBg = new GradientDrawable();
        icBg.setColor((sheetThemeMode == 0 || sheetThemeMode == 1)
                ? act.getResources().getColor(R.color.ifl_sheet_item_bg_dark)
                : act.getResources().getColor(R.color.ifl_sheet_item_bg_light));
        icBg.setCornerRadius(dp(14));
        iconContainer.setBackground(icBg);

        ImageView icon = new ImageView(act);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(28), dp(28));
        icon.setLayoutParams(iconLp);
        icon.setImageResource(R.drawable.ifl_instafel);
        icon.setColorFilter(primaryAccent);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconContainer.addView(icon);

        View space = new View(act);
        space.setLayoutParams(new LinearLayout.LayoutParams(dp(14), 1));

        LinearLayout textBox = new LinearLayout(act);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView title = new TextView(act);
        title.setText(LocalizedStringGetter.getDialogLocalizedString(act, iflLocale, "ifl_d5_01"));
        title.setTextColor(mainText);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        TextView sub = new TextView(act);
        sub.setText(
                LocalizedStringGetter.getDialogLocalizedString(
                        act,
                        iflLocale,
                        "ifl_d5_02",
                        InstafelEnv.IFL_VERSION,
                        InstafelEnv.IG_VERSION
                )
        );
        sub.setTextColor(subText);
        sub.setTextSize(13);

        textBox.addView(title);
        textBox.addView(sub);
        header.addView(iconContainer);
        header.addView(space);
        header.addView(textBox);

        root.addView(header);

        // Subtle M3 Divider
        View divider = new View(act);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        divLp.setMargins(dp(8), dp(6), dp(8), dp(10));
        divider.setLayoutParams(divLp);
        divider.setBackgroundColor((sheetThemeMode == 0 || sheetThemeMode == 1)
                ? 0x1AFFFFFF
                : 0x1A000000);
        root.addView(divider);

        root.addView(menuItem(
                R.drawable.ifl_menu,
                LocalizedStringGetter.getDialogLocalizedString(act, iflLocale, "ifl_d5_03"),
                mainText, v -> {
                    dismissSheetDialog();
                    InitializeInstafel.startInstafel();
                }));

        root.addView(menuItem(
                R.drawable.ifl_metaoverrides,
                LocalizedStringGetter.getDialogLocalizedString(act, iflLocale, "ifl_d5_04"),
                mainText, v -> openDeveloperOptions()));

        root.addView(menuItem(
                R.drawable.ifl_community_icon,
                LocalizedStringGetter.getDialogLocalizedString(act, iflLocale, "ifl_d5_05"),
                mainText, v -> GeneralFn.openInWebBrowser(act, "https://github.com/junksidetm/instafel")));

        root.addView(menuItem(
                R.drawable.ifl_sheet_reload,
                LocalizedStringGetter.getDialogLocalizedString(act, iflLocale, "ifl_d5_06"),
                mainText, v -> restartInstagram()));
    }

    public void restartInstagram() {
        Intent intent = act.getPackageManager().getLaunchIntentForPackage(act.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        act.startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    public void dismissSheetDialog() {
        if (sheetDialog.isShowing()) sheetDialog.dismiss();
    }

    public void openDeveloperOptions() {
        /*
            This method should need to be filled by patcher.
            If you want to inspect it, please see ChangeHomeLongClick patch.
         */
    }

    private View menuItem(int iconRes, String text, int color, View.OnClickListener onClickListener) {
        LinearLayout item = new LinearLayout(act);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        itemLp.setMargins(0, dp(2), 0, dp(2));
        item.setLayoutParams(itemLp);
        item.setPadding(dp(8), dp(8), dp(8), dp(8));
        item.setClickable(true);
        item.setFocusable(true);

        TypedValue outValue = new TypedValue();
        act.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        item.setBackgroundResource(outValue.resourceId);
        item.setOnClickListener(onClickListener);

        // Expressive Squircle Icon Container
        LinearLayout iconContainer = new LinearLayout(act);
        LinearLayout.LayoutParams icContainerLp = new LinearLayout.LayoutParams(dp(38), dp(38));
        iconContainer.setLayoutParams(icContainerLp);
        iconContainer.setGravity(Gravity.CENTER);
        GradientDrawable icBg = new GradientDrawable();
        icBg.setColor((sheetThemeMode == 0 || sheetThemeMode == 1)
                ? act.getResources().getColor(R.color.ifl_sheet_item_bg_dark)
                : act.getResources().getColor(R.color.ifl_sheet_item_bg_light));
        icBg.setCornerRadius(dp(12));
        iconContainer.setBackground(icBg);

        ImageView icon = new ImageView(act);
        icon.setImageResource(iconRes);
        int primaryAccent = (sheetThemeMode == 0 || sheetThemeMode == 1)
                ? act.getResources().getColor(R.color.ifl_m3_primary_dark)
                : act.getResources().getColor(R.color.ifl_m3_primary_light);
        icon.setColorFilter(primaryAccent);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(20), dp(20)));
        iconContainer.addView(icon);

        View spacer = new View(act);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(14), 1));

        TextView tv = new TextView(act);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTextColor(color);
        tv.setMaxLines(1);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        item.addView(iconContainer);
        item.addView(spacer);
        item.addView(tv);

        return item;
    }

    private static int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                Resources.getSystem().getDisplayMetrics()
        );
    }
}
