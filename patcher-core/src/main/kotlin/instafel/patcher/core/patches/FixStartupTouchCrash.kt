/*
 * (c) 2026 Muhammed Ali Bulut & Antigravity, All rights reserved.
 *
 * See LICENSE file in repository root for copy file of license. For copyright
 * notices, technical issues, feedback, or any other related to this code file or
 * project, please contact me via mamii@mamii.dev or other ways.
 */

package instafel.patcher.core.patches

import instafel.patcher.core.utils.Log
import instafel.patcher.core.utils.SearchUtils
import instafel.patcher.core.utils.patch.InstafelPatch
import instafel.patcher.core.utils.patch.InstafelTask
import instafel.patcher.core.utils.patch.PInfos
import instafel.patcher.core.utils.modals.FileSearchResult
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FileUtils
import java.io.File

@PInfos.PatchInfo(
    name = "Fix Startup Touch Crash",
    shortname = "fix_startup_touch_crash",
    desc = "Prevents INVOKE_RETURN must not be null / NullPointerException during touch events before tab_bar is inflated",
    isSingle = true
)
class FixStartupTouchCrash : InstafelPatch() {

    private var delegateClassFile: File? = null

    override fun initializeTasks(): MutableList<InstafelTask> = mutableListOf(

        @PInfos.TaskInfo("Harden TabBarTouchDelegate against uninflated tab bar")
        object : InstafelTask() {
            override fun execute() {
                val mainActivityFiles = smaliUtils.getSmaliFilesByName("InstagramMainActivity.smali")

                var targetClassName: String? = null

                if (mainActivityFiles.isNotEmpty()) {
                    val mainActivityContent = smaliUtils.getSmaliFileContent(mainActivityFiles.first().absolutePath)
                    val a2bStart = mainActivityContent.indexOfFirst {
                        val t = it.trim()
                        t.startsWith(".method") && (t.contains("A2B(") || (t.contains("(") && t.contains("Landroid/view/MotionEvent;") && t.endsWith(")Z")))
                    }
                    if (a2bStart != -1) {
                        val a2bEnd = (a2bStart until mainActivityContent.size).firstOrNull { mainActivityContent[it].trim() == ".end method" } ?: mainActivityContent.size
                        val checkCastRegex = Regex("""check-cast\s+v\d+,\s+L([^;]+);""")
                        for (i in a2bStart until a2bEnd) {
                            val line = mainActivityContent[i].trim()
                            if (line.contains("invoke-interface") && line.contains("getValue()Ljava/lang/Object;")) {
                                for (k in 1..5) {
                                    if (i + k < a2bEnd) {
                                        val match = checkCastRegex.find(mainActivityContent[i + k].trim())
                                        if (match != null) {
                                            targetClassName = match.groupValues[1]
                                            Log.info("Resolved TabBarTouchDelegate from InstagramMainActivity: $targetClassName")
                                            break
                                        }
                                    }
                                }
                                if (targetClassName != null) break
                            }
                        }
                    }
                }

                if (targetClassName != null) {
                    val simpleName = targetClassName.substringAfterLast("/") + ".smali"
                    val candidate = smaliUtils.getSmaliFilesByName(simpleName).firstOrNull()
                    if (candidate != null) {
                        delegateClassFile = candidate
                    }
                }

                // Fallback: search for smali file with <init>(View) querying tab_bar and tab_bar_shadow
                if (delegateClassFile == null) {
                    Log.info("Searching for TabBarTouchDelegate by constructor signature and resource IDs...")
                    val allSmali = smaliUtils.getAllSmaliFiles()
                    for (file in allSmali) {
                        val lines = smaliUtils.getSmaliFileContent(file.absolutePath)
                        val hasTabBar = lines.any { it.contains("0x7f0b4077") || it.contains("tab_bar") }
                        val hasTabBarShadow = lines.any { it.contains("0x7f0b4078") || it.contains("tab_bar_shadow") }
                        if (hasTabBar && hasTabBarShadow) {
                            val hasInitView = lines.any { it.contains("<init>(") && it.contains("Landroid/view/View;") }
                            if (hasInitView) {
                                delegateClassFile = file
                                Log.info("Located TabBarTouchDelegate file: ${file.name}")
                                break
                            }
                        }
                    }
                }

                val targetFile = delegateClassFile
                if (targetFile == null || !targetFile.exists()) {
                    Log.warning("TabBarTouchDelegate class not found, skipping delegate constructor hardening.")
                    success("Delegate hardening skipped (not present in this base).")
                    return
                }

                val content = smaliUtils.getSmaliFileContent(targetFile.absolutePath).toMutableList()

                if (content.any { it.contains("[IFL] hardened") }) {
                    Log.info("TabBarTouchDelegate already hardened, skipping.")
                    success("TabBarTouchDelegate already hardened.")
                    return
                }

                val initStart = content.indexOfFirst {
                    val t = it.trim()
                    t.startsWith(".method") && t.contains("<init>(") && t.contains("Landroid/view/View;")
                }

                if (initStart == -1) {
                    Log.warning("<init>(View) constructor not found in ${targetFile.name}")
                    success("Constructor not found, skipping.")
                    return
                }

                val initEnd = (initStart until content.size).firstOrNull { content[it].trim() == ".end method" }
                if (initEnd == null) {
                    failure("Malformed smali: end of constructor not found.")
                    return
                }

                // Neutralize fatal checkNotNull calls following findViewById
                var modified = false
                for (i in initStart until initEnd) {
                    val line = content[i].trim()
                    if (line.startsWith("invoke-static") &&
                        (line.contains("checkNotNull") || line.contains("->A0B(") || line.contains("Intrinsics") || line.endsWith("(Ljava/lang/Object;)V"))
                    ) {
                        val prev = if (i > 0) content[i - 1].trim() else ""
                        val prev2 = if (i > 1) content[i - 2].trim() else ""
                        if (prev.startsWith("move-result-object") || prev.startsWith("move-result") ||
                            prev2.startsWith("move-result-object") || prev2.startsWith("move-result")
                        ) {
                            content[i] = "    # [IFL] hardened: removed checkNotNull for safe touch startup"
                            modified = true
                            Log.info("Removed checkNotNull at line $i in ${targetFile.name}")
                        }
                    }
                }

                if (modified) {
                    FileUtils.writeLines(targetFile, content)
                    Log.info("Successfully hardened ${targetFile.name} against startup touch crashes.")
                }

                success("TabBarTouchDelegate successfully hardened.")
            }
        },

        @PInfos.TaskInfo("Safeguard InstagramMainActivity touch dispatcher")
        object : InstafelTask() {
            override fun execute() {
                val mainActivityFiles = smaliUtils.getSmaliFilesByName("InstagramMainActivity.smali")

                if (mainActivityFiles.isEmpty()) {
                    Log.warning("InstagramMainActivity.smali not found, skipping touch dispatcher safeguard.")
                    success("Skipped (InstagramMainActivity not found).")
                    return
                }

                val targetFile = mainActivityFiles.first()
                val content = smaliUtils.getSmaliFileContent(targetFile.absolutePath).toMutableList()

                if (content.any { it.contains(":try_start_ifl_a2b") }) {
                    Log.info("InstagramMainActivity touch dispatcher already safeguarded.")
                    success("InstagramMainActivity touch dispatcher already safeguarded.")
                    return
                }

                val a2bStart = content.indexOfFirst {
                    val t = it.trim()
                    t.startsWith(".method") && (t.contains("A2B(") || (t.contains("(") && t.contains("Landroid/view/MotionEvent;") && t.endsWith(")Z")))
                }

                if (a2bStart == -1) {
                    Log.warning("Touch dispatcher method not found in InstagramMainActivity.smali")
                    success("Touch dispatcher method not found, skipping.")
                    return
                }

                val a2bEnd = (a2bStart until content.size).firstOrNull { content[it].trim() == ".end method" }
                if (a2bEnd == null) {
                    failure("End of touch dispatcher method not found in InstagramMainActivity.smali")
                    return
                }

                val regIndex = (a2bStart until a2bEnd).firstOrNull {
                    content[it].trim().startsWith(".registers") || content[it].trim().startsWith(".locals")
                }

                if (regIndex == null) {
                    failure("Registers declaration not found in touch dispatcher method.")
                    return
                }

                content.add(regIndex + 1, "    :try_start_ifl_a2b")

                val updatedA2bEnd = (a2bStart until content.size).firstOrNull { content[it].trim() == ".end method" } ?: content.size
                val handlerBlock = listOf(
                    "    :try_end_ifl_a2b",
                    "    .catch Ljava/lang/Throwable; {:try_start_ifl_a2b .. :try_end_ifl_a2b} :catch_ifl_a2b_error",
                    "",
                    "    :catch_ifl_a2b_error",
                    "    move-exception v0",
                    "    const-string v1, \"IFL_TOUCH\"",
                    "    const-string v2, \"Safely handled touch event anomaly in InstagramMainActivity\"",
                    "    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I",
                    "    const/4 v0, 0x0",
                    "    return v0"
                )

                content.addAll(updatedA2bEnd, handlerBlock)
                FileUtils.writeLines(targetFile, content)
                Log.info("InstagramMainActivity touch dispatcher successfully safeguarded with try/catch.")
                success("InstagramMainActivity touch dispatcher safeguarded successfully.")
            }
        },

        @PInfos.TaskInfo("Safeguard IgFragmentActivity.dispatchTouchEvent")
        object : InstafelTask() {
            override fun execute() {
                val igFragFiles = smaliUtils.getSmaliFilesByName("IgFragmentActivity.smali")

                if (igFragFiles.isEmpty()) {
                    Log.warning("IgFragmentActivity.smali not found, skipping dispatch safeguard.")
                    success("Skipped (IgFragmentActivity not found).")
                    return
                }

                val targetFile = igFragFiles.first()
                val content = smaliUtils.getSmaliFileContent(targetFile.absolutePath).toMutableList()

                if (content.any { it.contains(":try_start_ifl_dispatch") }) {
                    Log.info("IgFragmentActivity.dispatchTouchEvent already safeguarded.")
                    success("IgFragmentActivity.dispatchTouchEvent already safeguarded.")
                    return
                }

                var superClass = "androidx/appcompat/app/AppCompatActivity"
                for (line in content) {
                    val t = line.trim()
                    if (t.startsWith(".super L") && t.endsWith(";")) {
                        superClass = t.removePrefix(".super L").removeSuffix(";")
                        break
                    }
                }

                val dispatchStart = content.indexOfFirst {
                    val t = it.trim()
                    t.startsWith(".method") && t.contains("dispatchTouchEvent(") && t.endsWith(")Z")
                }

                if (dispatchStart == -1) {
                    Log.warning("dispatchTouchEvent(...)Z method not found in IgFragmentActivity.smali")
                    success("dispatchTouchEvent not found, skipping.")
                    return
                }

                val dispatchEnd = (dispatchStart until content.size).firstOrNull { content[it].trim() == ".end method" }
                if (dispatchEnd == null) {
                    failure("End of dispatchTouchEvent method not found in IgFragmentActivity.smali")
                    return
                }

                val regIndex = (dispatchStart until dispatchEnd).firstOrNull {
                    content[it].trim().startsWith(".registers") || content[it].trim().startsWith(".locals")
                }

                if (regIndex == null) {
                    failure("Registers declaration not found in dispatchTouchEvent method.")
                    return
                }

                content.add(regIndex + 1, "    :try_start_ifl_dispatch")

                val updatedDispatchEnd = (dispatchStart until content.size).firstOrNull { content[it].trim() == ".end method" } ?: content.size
                val handlerBlock = listOf(
                    "    :try_end_ifl_dispatch",
                    "    .catch Ljava/lang/Throwable; {:try_start_ifl_dispatch .. :try_end_ifl_dispatch} :catch_ifl_dispatch_error",
                    "",
                    "    :catch_ifl_dispatch_error",
                    "    move-exception v0",
                    "    const-string v1, \"IFL_TOUCH\"",
                    "    const-string v2, \"Safely recovered from touch dispatch anomaly in IgFragmentActivity\"",
                    "    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I",
                    "    invoke-super {p0, p1}, L$superClass;->dispatchTouchEvent(Landroid/view/MotionEvent;)Z",
                    "    move-result v0",
                    "    return v0"
                )

                content.addAll(updatedDispatchEnd, handlerBlock)
                FileUtils.writeLines(targetFile, content)
                Log.info("IgFragmentActivity.dispatchTouchEvent successfully safeguarded with try/catch fallback.")
                success("IgFragmentActivity.dispatchTouchEvent safeguarded successfully.")
            }
        }
    )
}
