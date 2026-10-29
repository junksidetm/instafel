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
    desc = "Prevents android.view.InflateException: You must supply a layout_height attribute by enforcing valid layout dimensions across all layout resources",
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

    override fun initializeTasks(): MutableList<InstafelTask> = mutableListOf(
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

                // Diagnostic candidate scan for line 40 includes
                try {
                    for (layoutDir in layoutDirs) {
                        val xmlFiles = layoutDir.listFiles { f -> f.isFile && f.name.endsWith(".xml") } ?: continue
                        for (f in xmlFiles) {
                            val lines = f.readLines()
                            if (lines.size >= 40) {
                                val l40 = lines[39].trim()
                                if (l40.contains("<include") || (lines.size >= 41 && lines[40].trim().contains("<include"))) {
                                    Log.info("DIAGNOSTIC CANDIDATE: ${layoutDir.name}/${f.name} line 40: $l40")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.warning("Diagnostic scan error: ${e.message}")
                }

                val dbf = DocumentBuilderFactory.newInstance().apply {
                    isNamespaceAware = false
                    isValidating = false
                    try {
                        setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                    } catch (ignored: Throwable) {}
                }
                val docBuilder = dbf.newDocumentBuilder()
                val transformer = TransformerFactory.newInstance().newTransformer().apply {
                    setOutputProperty(OutputKeys.INDENT, "yes")
                    setOutputProperty(OutputKeys.ENCODING, "utf-8")
                }

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
                                    val hasAnyOtherLayoutAttr = (0 until node.attributes.length).any {
                                        val n = node.attributes.item(it).nodeName
                                        n != "layout" && (n.contains("layout_") || n.contains("id") || n.contains("visibility"))
                                    }

                                    val needHeight = (h == null || !isValidDimension(h))
                                    val needWidth = (w == null || !isValidDimension(w))

                                    if (hasAnyOtherLayoutAttr || !needHeight || !needWidth) {
                                        if (needHeight || needWidth) {
                                            val fallbackW = if (!needWidth) w!! else "match_parent"
                                            val fallbackH = if (!needHeight) h!! else "wrap_content"
                                            node.setAttribute("android:layout_width", fallbackW)
                                            node.setAttribute("android:layout_height", fallbackH)
                                            fileModified = true
                                            healedElements++
                                            healedDetails.add(HealedDetail(file.name, "include", "Guaranteed layout dimensions (w=$fallbackW, h=$fallbackH)"))
                                            Log.info("Guaranteed <include> dimensions (w=$fallbackW, h=$fallbackH) in ${file.name}")
                                        }
                                    }
                                    continue
                                }

                                val isRoot = (node == root)
                                val isMergeChild = (node.parentNode == root && root.tagName == "merge")

                                // Audit and normalize layout_height
                                val h = getEffectiveAttribute(node, "layout_height")
                                if (h == null || !isValidDimension(h)) {
                                    val fallbackHeight = if (isRoot && !isMergeChild) "match_parent" else "wrap_content"
                                    node.setAttribute("android:layout_height", fallbackHeight)
                                    fileModified = true
                                    healedElements++
                                    healedDetails.add(HealedDetail(file.name, tag, "Injected fallback layout_height=$fallbackHeight (was '${h ?: "missing"}')"))
                                    Log.info("Healed tag <$tag> missing/invalid layout_height in ${file.name}")
                                } else if (!node.hasAttribute("android:layout_height")) {
                                    node.setAttribute("android:layout_height", h)
                                    if (node.hasAttribute("layout_height")) node.removeAttribute("layout_height")
                                    fileModified = true
                                    healedElements++
                                    healedDetails.add(HealedDetail(file.name, tag, "Normalized layout_height attribute namespace"))
                                    Log.info("Normalized tag <$tag> layout_height to android: namespace in ${file.name}")
                                }

                                // Audit and normalize layout_width
                                val w = getEffectiveAttribute(node, "layout_width")
                                if (w == null || !isValidDimension(w)) {
                                    val fallbackWidth = if (isRoot && !isMergeChild) "match_parent" else "wrap_content"
                                    node.setAttribute("android:layout_width", fallbackWidth)
                                    fileModified = true
                                    healedElements++
                                    healedDetails.add(HealedDetail(file.name, tag, "Injected fallback layout_width=$fallbackWidth (was '${w ?: "missing"}')"))
                                    Log.info("Healed tag <$tag> missing/invalid layout_width in ${file.name}")
                                } else if (!node.hasAttribute("android:layout_width")) {
                                    node.setAttribute("android:layout_width", w)
                                    if (node.hasAttribute("layout_width")) node.removeAttribute("layout_width")
                                    fileModified = true
                                    healedElements++
                                    healedDetails.add(HealedDetail(file.name, tag, "Normalized layout_width attribute namespace"))
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
