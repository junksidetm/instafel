/*
 * (c) 2026 Muhammed Ali Bulut, All rights reserved.
 *
 * See LICENSE file in repository root for copy file of license. For copyright
 * notices, technical issues, feedback, or any other related to this code file or
 * project, please contact me via mamii@mamii.dev or other ways.
 */

package instafel.patcher.core.patches.general

import instafel.patcher.core.utils.Env
import instafel.patcher.core.utils.LocaleUtils
import instafel.patcher.core.utils.Log
import instafel.patcher.core.utils.Utils
import instafel.patcher.core.utils.modals.LastResourceIDs
import instafel.patcher.core.utils.patch.InstafelPatch
import instafel.patcher.core.utils.patch.InstafelTask
import instafel.patcher.core.utils.patch.PInfos
import instafel.patcher.core.utils.resources.IFLResData
import instafel.patcher.core.utils.resources.RTypes
import instafel.patcher.core.utils.resources.ResourceHelper
import instafel.patcher.core.utils.resources.ResourceParser
import instafel.patcher.core.utils.resources.ResourceType
import instafel.patcher.core.utils.resources.Resources
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.PrefixFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.util.Locale

@PInfos.PatchInfo(
    name = "Copy Instafel Sources",
    shortname = "copy_instafel_src",
    desc = "This patch needs to executed for use Instafel stuffs",
    isSingle = false
)
class CopyInstafelSources: InstafelPatch() {

    lateinit var resDataParser: IFLResData.Parser
    var valuesFolderPath = Utils.mergePaths(Env.PROJECT_DIR, "sources", "res", "values");

    init {
        prepareResData()
    }

    override fun initializeTasks() = mutableListOf(
        @PInfos.TaskInfo("Copy smali / resources")
        object: InstafelTask() {
            override fun execute() {
                val smallDexFolder: File? = smaliUtils.getSmallSizeSmaliFolder(smaliUtils.smaliFolders)

                if (smallDexFolder == null) {
                    failure("Smallest smali folder not found")
                    return;
                }

                val destFolder = File(
                    Utils.mergePaths(
                        Env.PROJECT_DIR,
                        "sources",
                        smallDexFolder.name,
                        "instafel"
                    )
                )

                Env.Project.iflSourcesFolder = smallDexFolder.name

                Log.info("Copying instafel resources")
                Utils.unzipFromResources(false, "/ifl_sources/ifl_sources.zip", destFolder.absolutePath)

                val igResourcesFolder = File(Utils.mergePaths(Env.PROJECT_DIR, "sources", "res"))
                Utils.unzipFromResources(false, "/ifl_sources/ifl_resources.zip", igResourcesFolder.absolutePath)
                success("Instafel resources copied")
            }
        },
        @PInfos.TaskInfo("Add activities and providers to manifest")
        object: InstafelTask() {
            override fun execute() {
                val manifestFile = File(Utils.mergePaths(Env.PROJECT_DIR, "sources", "AndroidManifest.xml"))
                val manifestDoc = ResourceParser.parseResourceDocument(manifestFile)

                val applicationElement = ResourceParser.getNodesFromResFile(manifestDoc, "application").item(0)
                val manifestElement = manifestDoc.documentElement

                val iflActivities = resDataParser.activities
                iflActivities.forEach { activity ->
                    applicationElement?.appendChild(manifestDoc.importNode(activity, true))
                }
                Log.info("Totally ${iflActivities.size} activity added")

                val iflProviders = resDataParser.providers
                iflProviders.forEach { provider ->
                    applicationElement?.appendChild(manifestDoc.importNode(provider, true))
                }
                Log.info("Totally ${iflProviders.size} provider added")

                val requestPermEl = manifestDoc.createElement("uses-permission").apply {
                    setAttribute("android:name", "android.permission.REQUEST_INSTALL_PACKAGES")
                }
                manifestElement.appendChild(requestPermEl)

                // Strip split APK enforcement attributes for standalone monolithic APK installation
                val splitAttrs = listOf(
                    "android:isSplitRequired",
                    "android:requiredSplitTypes",
                    "android:splitTypes",
                    "isSplitRequired",
                    "requiredSplitTypes",
                    "splitTypes"
                )
                splitAttrs.forEach { attr ->
                    if (manifestElement.hasAttribute(attr)) {
                        manifestElement.removeAttribute(attr)
                    }
                }

                val attrsToRemove = mutableListOf<String>()
                for (i in 0 until manifestElement.attributes.length) {
                    val attr = manifestElement.attributes.item(i)
                    val attrName = attr.localName ?: attr.nodeName
                    if (attrName.contains("isSplitRequired", ignoreCase = true) || attrName.contains("SplitTypes", ignoreCase = true)) {
                        attrsToRemove.add(attr.nodeName)
                    }
                }
                attrsToRemove.forEach { manifestElement.removeAttribute(it) }

                // Strip Google Play split & distribution stamp metadata
                val metaDataNodes = manifestDoc.getElementsByTagName("meta-data")
                val nodesToRemove = mutableListOf<Node>()
                for (i in 0 until metaDataNodes.length) {
                    val node = metaDataNodes.item(i)
                    if (node is Element) {
                        val name = node.getAttribute("android:name").ifEmpty { node.getAttribute("name") }
                        if (name.startsWith("com.android.vending.splits") ||
                            name.startsWith("com.android.vending.derived.apk.id") ||
                            name.startsWith("com.android.stamp.")) {
                            nodesToRemove.add(node)
                        }
                    }
                }
                nodesToRemove.forEach { it.parentNode?.removeChild(it) }

                ResourceParser.buildXmlFile(manifestDoc, manifestFile) // build manifest xml file
                success("Activities & providers added successfully from Instafel base")
            }
        },
        @PInfos.TaskInfo("Merge Instafel strings with IG")
        object: InstafelTask() {
            override fun execute() {
                val igResources = ResourceParser.parseResString(getValueResourceFile("strings.xml"))

                resDataParser.resourcesStrings["strings"]?.let { sourceResources ->
                    mergeResources(igResources, sourceResources)
                }

                igResources.document?.let { doc ->
                    igResources.sourceFile?.let { file ->
                        ResourceParser.buildXmlFile(doc, file)
                    }
                }

                val strings: Map<String, Resources<RTypes.TString>> = resDataParser.resourcesStrings

                Env.INSTAFEL_LOCALES.forEach { localeInfo ->
                    val targetFile = File(Utils.mergePaths("$valuesFolderPath-${localeInfo.androidLangCode}", "strings.xml"))
                    val targetResources = ResourceParser.parseResString(targetFile)

                    strings["strings-${localeInfo.androidLangCode}"]?.let { sourceResources ->
                        mergeResources(targetResources, sourceResources)
                    }
                }

                success("Instafel strings merged successfully.")
            }
        },
        @PInfos.TaskInfo("Copy IFL resources into Instagram")
        object: InstafelTask() {
            override fun execute() {
                mergeResources(ResourceParser.parseResColor(
                    getValueResourceFile("colors.xml")
                ), resDataParser.resourcesColor);
                mergeResources(ResourceParser.parseResAttr(
                    getValueResourceFile("attrs.xml")
                ), resDataParser.resourcesAttr);
                mergeResources(ResourceParser.parseResId(
                    getValueResourceFile("ids.xml")
                ), resDataParser.resourcesId);
                mergeResources(ResourceParser.parseResStyle(
                    getValueResourceFile("styles.xml")
                ), resDataParser.resourcesStyle);

                success("All resources merged succesfully");
            }
        },
        @PInfos.TaskInfo("Ensure fallback layout dimensions in cold-start themes")
        object: InstafelTask() {
            override fun execute() {
                val themeFiles = listOf(
                    getValueResourceFile("styles.xml"),
                    getValueResourceFile("themes.xml")
                ).filter { it.exists() && it.isFile }

                if (themeFiles.isEmpty()) {
                    success("Skipped (theme files not found)")
                    return
                }

                try {
                    val targetThemes = setOf("IgPanorama", "IgPanoramaBase", "Theme.Instagram", "Base.Theme", "Base.Theme.Instagram")
                    val fallbackDimensions = mapOf(
                        "actionBarSize" to "56.0dip",
                        "actionBarHeight" to "56.0dip",
                        "android:actionBarSize" to "56.0dip"
                    )

                    var totalInjected = 0
                    for (file in themeFiles) {
                        val doc = ResourceParser.parseResourceDocument(file)
                        val styleNodes = doc.getElementsByTagName("style")
                        var fileModified = false

                        for (i in 0 until styleNodes.length) {
                            val styleEl = styleNodes.item(i) as? Element ?: continue
                            val name = styleEl.getAttribute("name")
                            if (targetThemes.any { name.equals(it, ignoreCase = true) || name.endsWith(".$it") }) {
                                val existingItems = mutableSetOf<String>()
                                val children = styleEl.childNodes
                                for (j in 0 until children.length) {
                                    val item = children.item(j) as? Element ?: continue
                                    if (item.tagName == "item") {
                                        existingItems.add(item.getAttribute("name"))
                                    }
                                }

                                for ((dimenName, dimenVal) in fallbackDimensions) {
                                    if (!existingItems.contains(dimenName)) {
                                        val itemEl = doc.createElement("item").apply {
                                            setAttribute("name", dimenName)
                                            textContent = dimenVal
                                        }
                                        styleEl.appendChild(itemEl)
                                        fileModified = true
                                        totalInjected++
                                    }
                                }
                            }
                        }

                        if (fileModified) {
                            ResourceParser.buildXmlFile(doc, file)
                            Log.info("Injected fallback dimensions into cold-start themes in " + file.name)
                        }
                    }

                    success("Cold-start theme fallback dimensions verified ($totalInjected injected)")
                } catch (t: Throwable) {
                    Log.warning("Theme dimension hardening exception: " + t.message)
                    success("Theme dimension hardening completed with warning")
                }
            }
        },
        @PInfos.TaskInfo("Copy public resources with new IDs")
        object: InstafelTask() {
            override fun execute() {
                val igPublic: Resources<RTypes.TPublic> = ResourceParser.parseResPublic(getValueResourceFile("public.xml"))
                val categorizedIGPublics: Map<String, List<Int>> = ResourceHelper.getIDsWithCategory(igPublic)
                val lastResourceIds = ResourceHelper.getBiggestResourceID(categorizedIGPublics)

                val iflPublic: Resources<RTypes.TPublic> = resDataParser.resourcesPublic
                iflPublic.forEach { tPublic ->
                    val newId = lastResourceIds[tPublic.type] + 1
                    lastResourceIds[tPublic.type] = newId
                    tPublic.id = ResourceHelper.convertToHex(newId)
                }

                Log.info("Totally ${iflPublic.count()} public's id updated.")

                mergeResources(igPublic, iflPublic)
                success("ID's successfully defined.")
            }
        },
        @PInfos.TaskInfo("Update Instafel R classes")
        object: InstafelTask() {
            override fun execute() {
                val smaliFolder = File(
                    Utils.mergePaths(
                        smaliUtils.getSmaliFolderByPaths("instafel", "app")!!.absolutePath,
                        "instafel", "app"
                    )
                )

                val files: Collection<File> = FileUtils.listFiles(smaliFolder, PrefixFileFilter("R$"), TrueFileFilter.INSTANCE)
                val igResources: Resources<RTypes.TPublic> = ResourceParser.parseResPublic(getValueResourceFile("public.xml"))
                igResources.resources.removeIf { !it.name.startsWith("ifl_") }
                files.forEach { file -> ResourceHelper.updateRClass(igResources, file) }

                success("All R classes updated successfully.")
            }
        }
    )

    private fun <T : ResourceType> mergeResources(target: Resources<T>, source: Resources<T>) {
        source.forEach { resource ->
            target.addExternalResource(resource)
        }

        target.document?.let { doc ->
            target.sourceFile?.let { file ->
                ResourceParser.buildXmlFile(doc, file)
            }
        }

        Log.info("Totally ${source.count()} resource(s) added to ${target.resTypeName}")
    }

    private fun getValueResourceFile(fileName: String): File {
        return File(Utils.mergePaths(valuesFolderPath, fileName))
    }

    private fun prepareResData() {
        val resDataPath = File(Utils.mergePaths(Env.PROJECT_DIR, "ifl_data_temp.xml"))
        Utils.copyResourceToFile("/ifl_sources/ifl_data.xml", resDataPath)
        resDataParser = IFLResData.Parser(resDataPath)
    }
}