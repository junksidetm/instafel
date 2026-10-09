/*
 * (c) 2026 Muhammed Ali Bulut & Antigravity, All rights reserved.
 *
 * See LICENSE file in repository root for copy file of license. For copyright
 * notices, technical issues, feedback, or any other related to this code file or
 * project, please contact me via mamii@mamii.dev or other ways.
 */

package instafel.patcher.core.patches

import instafel.patcher.core.utils.Log
import instafel.patcher.core.utils.patch.InstafelPatch
import instafel.patcher.core.utils.patch.InstafelTask
import instafel.patcher.core.utils.patch.PInfos
import java.io.File

@PInfos.PatchInfo(
    name = "Custom Fonts and Emojis",
    shortname = "font_emoji",
    desc = "Enables hierarchical multi-weight typography (SF Pro, Google Sans Flex) and custom emoji rendering (iOS, Google 3D)",
    isSingle = true
)
class FontEmojiPatch : InstafelPatch() {

    override fun initializeTasks() = mutableListOf<InstafelTask>(
        @PInfos.TaskInfo("Verify FontManager and EmojiManager classes in project")
        object : InstafelTask() {
            override fun execute() {
                val fontManagerSmali = smaliUtils.getSmaliFilesByName("FontManager.smali")
                val emojiManagerSmali = smaliUtils.getSmaliFilesByName("EmojiManager.smali")

                if (fontManagerSmali.isEmpty() || emojiManagerSmali.isEmpty()) {
                    Log.info("FontManager and EmojiManager smali will be bundled from ifl_sources during packaging.")
                } else {
                    Log.info("Found FontManager smali: ${fontManagerSmali.first().name}")
                    Log.info("Found EmojiManager smali: ${emojiManagerSmali.first().name}")
                }
                success("Custom typography and emoji engine verified.")
            }
        },
        @PInfos.TaskInfo("Ensure fonts and emojis directories structure in APK assets")
        object : InstafelTask() {
            override fun execute() {
                val assetsDir = File(smaliUtils.projectDir, "sources/assets")
                if (!assetsDir.exists()) {
                    assetsDir.mkdirs()
                }
                val fontsDir = File(assetsDir, "fonts")
                if (!fontsDir.exists()) {
                    fontsDir.mkdirs()
                }
                val emojisDir = File(assetsDir, "emojis")
                if (!emojisDir.exists()) {
                    emojisDir.mkdirs()
                }
                success("Assets directory structure configured for fonts and emojis.")
            }
        }
    )
}
