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
import org.w3c.dom.Node
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

                val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                val transformer = TransformerFactory.newInstance().newTransformer().apply {
                    setOutputProperty(OutputKeys.INDENT, "yes")
                    setOutputProperty(OutputKeys.ENCODING, "utf-8")
                }

                var healedFiles = 0
                var healedElements = 0

                val ignoredTags = setOf(
                    "resources", "merge", "requestFocus", "tag", "item",
                    "selector", "shape", "layer-list", "ripple", "vector",
                    "animated-vector", "transition", "mask", "clip"
                )

                for (layoutDir in layoutDirs) {
                    val xmlFiles = layoutDir.listFiles { file ->
                        file.isFile && file.name.endsWith(".xml")
                    } ?: continue

                    for (file in xmlFiles) {
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
                                    val hasWidth = node.hasAttribute("android:layout_width") || node.hasAttribute("layout_width")
                                    val hasHeight = node.hasAttribute("android:layout_height") || node.hasAttribute("layout_height")
                                    val hasId = node.hasAttribute("android:id") || node.hasAttribute("id")

                                    if (hasWidth && !hasHeight) {
                                        val widthVal = node.getAttribute("android:layout_width").ifEmpty { node.getAttribute("layout_width") }
                                        val fallbackHeight = if (widthVal == "match_parent") "match_parent" else "wrap_content"
                                        node.setAttribute("android:layout_height", fallbackHeight)
                                        fileModified = true
                                        healedElements++
                                        Log.info("Healed <include> missing layout_height in ${file.name}")
                                    } else if (hasHeight && !hasWidth) {
                                        node.setAttribute("android:layout_width", "match_parent")
                                        fileModified = true
                                        healedElements++
                                        Log.info("Healed <include> missing layout_width in ${file.name}")
                                    } else if (hasId && (!hasWidth || !hasHeight)) {
                                        if (!hasWidth) node.setAttribute("android:layout_width", "match_parent")
                                        if (!hasHeight) node.setAttribute("android:layout_height", "wrap_content")
                                        fileModified = true
                                        healedElements++
                                        Log.info("Healed identified <include> missing dimensions in ${file.name}")
                                    }
                                    continue
                                }

                                // Regular View / ViewGroup elements
                                val isRoot = (node == root)
                                val parentIsMerge = (node.parentNode is Element && (node.parentNode as Element).tagName == "merge")

                                // Only skip dimension requirements if parent is <merge>
                                if (parentIsMerge) continue

                                val hasHeight = node.hasAttribute("android:layout_height") || node.hasAttribute("layout_height")
                                val heightVal = node.getAttribute("android:layout_height").ifEmpty { node.getAttribute("layout_height") }.trim()

                                val hasWidth = node.hasAttribute("android:layout_width") || node.hasAttribute("layout_width")
                                val widthVal = node.getAttribute("android:layout_width").ifEmpty { node.getAttribute("layout_width") }.trim()

                                if (!hasHeight || heightVal.isEmpty()) {
                                    node.setAttribute("android:layout_height", "wrap_content")
                                    fileModified = true
                                    healedElements++
                                    Log.info("Healed tag <$tag> missing layout_height in ${file.name}")
                                }

                                if (!hasWidth || widthVal.isEmpty()) {
                                    node.setAttribute("android:layout_width", if (isRoot) "match_parent" else "wrap_content")
                                    fileModified = true
                                    healedElements++
                                    Log.info("Healed tag <$tag> missing layout_width in ${file.name}")
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

                Log.info("Layout healing complete: healed $healedElements element(s) across $healedFiles file(s).")
                success("Layout inflation audit complete (healed $healedElements elements across $healedFiles files).")
            }
        }
    )
}
