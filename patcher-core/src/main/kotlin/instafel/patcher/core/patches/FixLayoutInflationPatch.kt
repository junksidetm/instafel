/*
 * (c) 2026 Muhammed Ali Bulut & Antigravity, All rights reserved.
 *
 * See LICENSE file in repository root for copy file of license. For copyright
 * notices, technical issues, feedback, or any other related to this code file or
 * project, please contact me via mamii@mamii.dev or other ways.
 */

package instafel.patcher.core.patches

import instafel.patcher.core.utils.Env
import instafel.patcher.core.utils.Log
import instafel.patcher.core.utils.patch.InstafelPatch
import instafel.patcher.core.utils.patch.InstafelTask
import instafel.patcher.core.utils.patch.PInfos
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

@PInfos.PatchInfo(
    name = "Fix Layout Inflation Exceptions",
    shortname = "fix_layout_inflation",
    desc = "Prevents android.view.InflateException: You must supply a layout_height attribute by enforcing valid layout dimensions and generating Superpack include fallbacks",
    isSingle = true
)
class FixLayoutInflationPatch : InstafelPatch() {

    private data class HealedDetail(val file: String, val tag: String, val action: String)

    private fun isValidDimension(v: String?): Boolean {
        if (v == null) return false
        val trimmed = v.trim()
        if (trimmed.isEmpty() || trimmed == "@null" || trimmed == "null" || trimmed == "@empty" || trimmed == "0") return false
        // Theme attributes (e.g. ?attr/... or ?foo) can throw TypedArray.getLayoutDimension if undefined in cold-start themes
        if (trimmed.startsWith("?")) return false
        return true
    }

    private fun getEffectiveAttribute(node: Element, localName: String): String? {
        if (node.hasAttribute("android:$localName")) {
            val v = node.getAttribute("android:$localName").trim()
            if (v.isNotEmpty()) return v
        }
        val attrs = node.attributes
        for (i in 0 until attrs.length) {
            val attr = attrs.item(i)
            val name = attr.nodeName
            if (name == localName || name.endsWith(":$localName")) {
                val v = attr.nodeValue?.trim()
                if (!v.isNullOrEmpty()) return v
            }
        }
        return null
    }

    private fun newDocBuilder() = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        isValidating = false
        try {
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        } catch (ignored: Throwable) {}
    }.newDocumentBuilder()

    private fun newTransformer() = TransformerFactory.newInstance().newTransformer().apply {
        setOutputProperty(OutputKeys.INDENT, "yes")
        setOutputProperty(OutputKeys.ENCODING, "utf-8")
    }

    override fun initializeTasks(): MutableList<InstafelTask> = mutableListOf(
        @PInfos.TaskInfo("Guarantee layout stubs for Superpack includes (action_bar.xml)")
        object : InstafelTask() {
            override fun execute() {
                val resDir = File(Env.PROJECT_DIR, "sources/res")
                if (!resDir.exists() || !resDir.isDirectory) {
                    success("Skipped (res folder not present).")
                    return
                }

                val layoutDir = File(resDir, "layout")
                if (!layoutDir.exists()) layoutDir.mkdirs()

                // 1. Ensure action_bar.xml exists with valid FrameLayout and child action_bar_container
                val actionBarFile = File(layoutDir, "action_bar.xml")
                if (!actionBarFile.exists() || actionBarFile.length() < 20L) {
                    val content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
                            android:id="@id/action_bar"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content">
                            <View
                                android:id="@id/action_bar_container"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content" />
                        </FrameLayout>
                    """.trimIndent()
                    actionBarFile.writeText(content)
                    Log.info("Generated fallback layout: ${actionBarFile.absolutePath}")
                }

                // 2. Scan all layouts for any unresolved @layout/ references inside <include>
                val allLayoutFiles = resDir.listFiles { f -> f.isDirectory && f.name.startsWith("layout") }
                    ?.flatMap { it.listFiles { f -> f.isFile && f.name.endsWith(".xml") }?.toList() ?: emptyList() }
                    ?: emptyList()

                val existingLayoutNames = allLayoutFiles.map { it.name.removeSuffix(".xml") }.toSet()
                val docBuilder = newDocBuilder()

                var stubCount = 0
                for (file in allLayoutFiles) {
                    try {
                        val doc = docBuilder.parse(file)
                        val includes = doc.getElementsByTagName("include")
                        for (i in 0 until includes.length) {
                            val inc = includes.item(i) as? Element ?: continue
                            val layoutRef = inc.getAttribute("layout").trim()
                            if (layoutRef.startsWith("@layout/")) {
                                val targetName = layoutRef.removePrefix("@layout/")
                                if (!existingLayoutNames.contains(targetName)) {
                                    val stubFile = File(layoutDir, "$targetName.xml")
                                    if (!stubFile.exists()) {
                                        stubFile.writeText("""
                                            <?xml version="1.0" encoding="utf-8"?>
                                            <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
                                                android:layout_width="match_parent"
                                                android:layout_height="wrap_content" />
                                        """.trimIndent())
                                        Log.info("Generated stub layout for missing include: ${stubFile.name}")
                                        stubCount++
                                    }
                                }
                            }
                        }
                    } catch (t: Throwable) {
                        Log.warning("Error scanning includes in ${file.name}: ${t.message}")
                    }
                }

                success("Layout fallbacks guaranteed (action_bar.xml verified, $stubCount stub layout(s) generated).")
            }
        },

        @PInfos.TaskInfo("Harden theme attributes and fallback dimensions in styles.xml & dimens.xml")
        object : InstafelTask() {
            override fun execute() {
                val resDir = File(Env.PROJECT_DIR, "sources/res")
                if (!resDir.exists() || !resDir.isDirectory) {
                    success("Skipped (res folder not present).")
                    return
                }

                val docBuilder = newDocBuilder()
                val transformer = newTransformer()

                // 1. Harden styles.xml & themes.xml
                val valuesDir = File(resDir, "values")
                val stylesFiles = listOf(File(valuesDir, "styles.xml"), File(valuesDir, "themes.xml"))
                    .filter { it.exists() && it.isFile }

                var stylesModified = 0
                val targetStyles = setOf(
                    "IgPanorama", "IgPanoramaBase", "Theme.Instagram", "Base.Theme",
                    "Base.Theme.Instagram", "Base.Theme.Launcher", "Theme.AppCompat.Light.NoActionBar",
                    "Theme.AppCompat.DayNight.NoActionBar"
                )

                val requiredItems = mapOf(
                    "actionBarSize" to "56.0dip",
                    "actionBarHeight" to "56.0dip",
                    "actionBarHeightWithShadow" to "56.0dip",
                    "actionBarNameAndTimestampHeight" to "56.0dip",
                    "actionBarShadowHeight" to "0.0dip",
                    "android:actionBarSize" to "56.0dip"
                )

                for (sf in stylesFiles) {
                    try {
                        val doc = docBuilder.parse(sf)
                        val styleNodes = doc.getElementsByTagName("style")
                        var fileChanged = false

                        for (i in 0 until styleNodes.length) {
                            val styleEl = styleNodes.item(i) as? Element ?: continue
                            val styleName = styleEl.getAttribute("name")
                            if (targetStyles.any { styleName.equals(it, ignoreCase = true) || styleName.endsWith(".$it") }) {
                                val existingItems = mutableSetOf<String>()
                                val children = styleEl.childNodes
                                for (j in 0 until children.length) {
                                    val item = children.item(j) as? Element ?: continue
                                    if (item.tagName == "item") {
                                        existingItems.add(item.getAttribute("name"))
                                    }
                                }

                                for ((itemName, itemVal) in requiredItems) {
                                    if (!existingItems.contains(itemName)) {
                                        val newEl = doc.createElement("item").apply {
                                            setAttribute("name", itemName)
                                            textContent = itemVal
                                        }
                                        styleEl.appendChild(newEl)
                                        fileChanged = true
                                        stylesModified++
                                    }
                                }
                            }
                        }

                        if (fileChanged) {
                            transformer.transform(DOMSource(doc), StreamResult(sf))
                            Log.info("Injected action bar fallback attributes into ${sf.name}")
                        }
                    } catch (t: Throwable) {
                        Log.warning("Error hardening styles in ${sf.name}: ${t.message}")
                    }
                }

                // 2. Harden dimens.xml
                val dimensFile = File(valuesDir, "dimens.xml")
                if (dimensFile.exists()) {
                    try {
                        val doc = docBuilder.parse(dimensFile)
                        val root = doc.documentElement
                        if (root != null) {
                            val requiredDimens = mapOf(
                                "direct_composer_redesign_background_height" to "56.0dip",
                                "action_bar_height" to "56.0dip",
                                "action_bar_item_spacing_right" to "0.0dip"
                            )

                            val existingDimens = mutableSetOf<String>()
                            val dimenNodes = doc.getElementsByTagName("dimen")
                            for (i in 0 until dimenNodes.length) {
                                val dEl = dimenNodes.item(i) as? Element ?: continue
                                existingDimens.add(dEl.getAttribute("name"))
                            }

                            var dimensChanged = false
                            for ((dName, dVal) in requiredDimens) {
                                if (!existingDimens.contains(dName)) {
                                    val newD = doc.createElement("dimen").apply {
                                        setAttribute("name", dName)
                                        textContent = dVal
                                    }
                                    root.appendChild(newD)
                                    dimensChanged = true
                                }
                            }

                            if (dimensChanged) {
                                transformer.transform(DOMSource(doc), StreamResult(dimensFile))
                                Log.info("Injected safe fallback dimensions into dimens.xml")
                            }
                        }
                    } catch (t: Throwable) {
                        Log.warning("Error hardening dimens.xml: ${t.message}")
                    }
                }

                success("Theme attributes and dimens hardened ($stylesModified style item(s) injected).")
            }
        },

        @PInfos.TaskInfo("Audit and heal layout XML files across sources/res/layout*")
        object : InstafelTask() {
            override fun execute() {
                val resDir = File(Env.PROJECT_DIR, "sources/res")
                if (!resDir.exists() || !resDir.isDirectory) {
                    Log.info("res directory not found in ${resDir.absolutePath}, skipping layout healing.")
                    success("Skipped (res folder not present).")
                    return
                }

                val layoutDirs = resDir.listFiles { file ->
                    file.isDirectory && file.name.startsWith("layout")
                } ?: emptyArray()

                if (layoutDirs.isEmpty()) {
                    Log.info("No layout directories found in res.")
                    success("No layout directories found.")
                    return
                }

                val docBuilder = newDocBuilder()
                val transformer = newTransformer()

                var totalAuditedFiles = 0
                var healedFiles = 0
                var healedElements = 0
                val healedDetails = mutableListOf<HealedDetail>()

                val ignoredTags = setOf(
                    "resources", "merge", "requestFocus", "tag", "item",
                    "data", "variable", "import",
                    "selector", "shape", "layer-list", "ripple", "vector",
                    "animated-vector", "transition", "mask", "clip",
                    "animated-selector", "bitmap", "nine-patch", "gradient",
                    "corners", "solid", "stroke", "padding", "size"
                )

                for (layoutDir in layoutDirs) {
                    val xmlFiles = layoutDir.listFiles { file ->
                        file.isFile && file.name.endsWith(".xml")
                    } ?: continue

                    for (file in xmlFiles) {
                        totalAuditedFiles++
                        try {
                            val doc = docBuilder.parse(file)
                            val root = doc.documentElement ?: continue
                            var fileModified = false

                            val isMainActivityLayout = file.name.startsWith("layout_activity_main")

                            // Ensure android namespace is declared on root
                            val androidNs = "http://schemas.android.com/apk/res/android"
                            if (!root.hasAttribute("xmlns:android")) {
                                root.setAttribute("xmlns:android", androidNs)
                                fileModified = true
                            }

                            val allNodes = doc.getElementsByTagName("*")
                            for (i in 0 until allNodes.length) {
                                val node = allNodes.item(i)
                                if (node !is Element) continue
                                val tag = node.tagName

                                if (tag in ignoredTags) continue

                                if (tag == "include") {
                                    val h = getEffectiveAttribute(node, "layout_height")
                                    val w = getEffectiveAttribute(node, "layout_width")
                                    val needHeight = (h == null || !isValidDimension(h) || (isMainActivityLayout && h.startsWith("@dimen/")))
                                    val needWidth = (w == null || !isValidDimension(w) || (isMainActivityLayout && w.startsWith("@dimen/")))

                                    if (needHeight || needWidth) {
                                        val fallbackW = if (!needWidth) w!! else "match_parent"
                                        val fallbackH = if (!needHeight) h!! else "wrap_content"
                                        node.setAttribute("android:layout_width", fallbackW)
                                        node.setAttribute("android:layout_height", fallbackH)
                                        if (node.hasAttribute("layout_width")) node.removeAttribute("layout_width")
                                        if (node.hasAttribute("layout_height")) node.removeAttribute("layout_height")
                                        fileModified = true
                                        healedElements++
                                        healedDetails.add(HealedDetail(file.name, "include", "Guaranteed layout dimensions (w=$fallbackW, h=$fallbackH)"))
                                        Log.info("Guaranteed <include> dimensions (w=$fallbackW, h=$fallbackH) in ${file.name}")
                                    } else {
                                        if (node.hasAttribute("layout_width")) {
                                            node.removeAttribute("layout_width")
                                            node.setAttribute("android:layout_width", w!!)
                                            fileModified = true
                                        }
                                        if (node.hasAttribute("layout_height")) {
                                            node.removeAttribute("layout_height")
                                            node.setAttribute("android:layout_height", h!!)
                                            fileModified = true
                                        }
                                    }
                                    continue
                                }

                                val isRoot = (node == root)
                                val isMergeChild = (node.parentNode == root && root.tagName == "merge")

                                // In main activity layouts, also sanitize any @dimen/ or ?attr/ reference that might fail on cold start
                                val h = getEffectiveAttribute(node, "layout_height")
                                val hInvalid = (h == null || !isValidDimension(h) || (isMainActivityLayout && h.startsWith("@dimen/")))
                                if (hInvalid) {
                                    val fallbackHeight = if (isRoot && !isMergeChild) "match_parent" else "wrap_content"
                                    node.setAttribute("android:layout_height", fallbackHeight)
                                    fileModified = true
                                    healedElements++
                                    healedDetails.add(HealedDetail(file.name, tag, "Injected fallback layout_height=$fallbackHeight (was '${h ?: "missing"}')"))
                                    Log.info("Healed tag <$tag> layout_height in ${file.name} to $fallbackHeight")
                                } else if (!node.hasAttribute("android:layout_height")) {
                                    node.setAttribute("android:layout_height", h)
                                    if (node.hasAttribute("layout_height")) node.removeAttribute("layout_height")
                                    fileModified = true
                                    healedElements++
                                    healedDetails.add(HealedDetail(file.name, tag, "Normalized layout_height namespace"))
                                    Log.info("Normalized tag <$tag> layout_height to android: namespace in ${file.name}")
                                }

                                val w = getEffectiveAttribute(node, "layout_width")
                                val wInvalid = (w == null || !isValidDimension(w) || (isMainActivityLayout && w.startsWith("@dimen/")))
                                if (wInvalid) {
                                    val fallbackWidth = if (isRoot && !isMergeChild) "match_parent" else "wrap_content"
                                    node.setAttribute("android:layout_width", fallbackWidth)
                                    fileModified = true
                                    healedElements++
                                    healedDetails.add(HealedDetail(file.name, tag, "Injected fallback layout_width=$fallbackWidth (was '${w ?: "missing"}')"))
                                    Log.info("Healed tag <$tag> layout_width in ${file.name} to $fallbackWidth")
                                } else if (!node.hasAttribute("android:layout_width")) {
                                    node.setAttribute("android:layout_width", w)
                                    if (node.hasAttribute("layout_width")) node.removeAttribute("layout_width")
                                    fileModified = true
                                    healedElements++
                                    healedDetails.add(HealedDetail(file.name, tag, "Normalized layout_width namespace"))
                                    Log.info("Normalized tag <$tag> layout_width to android: namespace in ${file.name}")
                                }
                            }

                            if (fileModified) {
                                transformer.transform(DOMSource(doc), StreamResult(file))
                                healedFiles++
                            }
                        } catch (t: Throwable) {
                            Log.warning("Could not parse layout ${file.name} for dimension audit: ${t.message}")
                        }
                    }
                }

                // Write markdown report for GitHub Actions Step Summary
                try {
                    val summaryContent = buildString {
                        appendLine("### 📐 Layout Inflation Audit & Healing Report")
                        appendLine("- **Audited Files:** $totalAuditedFiles")
                        appendLine("- **Modified/Healed Files:** $healedFiles")
                        appendLine("- **Normalized/Healed Elements:** $healedElements")
                        if (healedDetails.isNotEmpty()) {
                            appendLine("\n<details><summary>Click to view healed elements (${healedDetails.size})</summary>\n")
                            appendLine("| Layout File | Tag | Action |")
                            appendLine("| :--- | :--- | :--- |")
                            for (d in healedDetails.take(100)) {
                                appendLine("| `${d.file}` | `<${d.tag}>` | ${d.action} |")
                            }
                            if (healedDetails.size > 100) {
                                appendLine("| ... | ... | ... and ${healedDetails.size - 100} more |")
                            }
                            appendLine("\n</details>\n")
                        } else {
                            appendLine("\n✅ All layout XML schemas verified compliant.")
                        }
                    }
                    File(Env.PROJECT_DIR, "layout_audit_summary.md").writeText(summaryContent)
                    File("layout_audit_summary.md").writeText(summaryContent)
                } catch (ignored: Throwable) {}

                Log.info("Layout healing complete: audited $totalAuditedFiles files, healed $healedElements element(s) across $healedFiles file(s).")
                success("Layout inflation audit complete (audited $totalAuditedFiles files, healed $healedElements elements across $healedFiles files).")
            }
        }
    )
}
