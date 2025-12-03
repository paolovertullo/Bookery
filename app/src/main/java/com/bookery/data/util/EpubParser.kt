package com.bookery.data.util

import android.content.Context
import com.bookery.data.model.Book
import nl.siegmann.epublib.domain.Book as EpubBook
import nl.siegmann.epublib.epub.EpubReader
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class EpubParser(private val context: Context) {

    // ---------- METADATI + COVER PER LIBRARY ----------

    fun parse(file: File): Book? {
        return try {
            ZipFile(file).use { zip ->
                val containerEntry = zip.getEntry("META-INF/container.xml") ?: return null
                val opfPath = readOpfPath(zip.getInputStream(containerEntry)) ?: return null
                val opfEntry = zip.getEntry(opfPath) ?: return null
                val opfDir = opfPath.substringBeforeLast('/', "")

                val opfResult = readOpf(zip.getInputStream(opfEntry))

                val coverPathInZip = opfResult.coverHref?.let { href ->
                    if (opfDir.isNotEmpty()) "$opfDir/$href" else href
                }

                val coverLocalPath = coverPathInZip?.let { saveCover(zip, it, file) }

                Book(
                    id = file.absolutePath,
                    title = opfResult.title ?: file.nameWithoutExtension,
                    author = opfResult.author ?: "Sconosciuto",
                    language = opfResult.language ?: "English",
                    coverUrl = coverLocalPath
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private data class OpfResult(
        val title: String?,
        val author: String?,
        val language: String?,
        val coverHref: String?
    )

    private fun readOpfPath(input: InputStream): String? {
        val parser = newParser(input)
        var path: String? = null
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT && path == null) {
            if (event == XmlPullParser.START_TAG && parser.name == "rootfile") {
                for (i in 0 until parser.attributeCount) {
                    if (parser.getAttributeName(i) == "full-path") {
                        path = parser.getAttributeValue(i)
                        break
                    }
                }
            }
            event = parser.next()
        }

        input.close()
        return path
    }

    private fun readOpf(input: InputStream): OpfResult {
        val parser = newParser(input)

        var title: String? = null
        var author: String? = null
        var language: String? = null
        var coverId: String? = null
        var coverHref: String? = null

        var inMetadata = false
        var inManifest = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "metadata" -> inMetadata = true
                        "manifest" -> inManifest = true
                        "title", "dc:title" -> if (inMetadata) {
                            title = parser.nextText().trim()
                        }
                        "creator", "dc:creator" -> if (inMetadata) {
                            author = parser.nextText().trim()
                        }
                        "language", "dc:language" -> if (inMetadata) {
                            language = parser.nextText().trim()
                        }
                        "meta" -> if (inMetadata) {
                            val nameAttr = parser.getAttributeValue(null, "name")
                            val contentAttr = parser.getAttributeValue(null, "content")
                            if (nameAttr == "cover") {
                                coverId = contentAttr
                            }
                        }
                        "item" -> if (inManifest && coverId != null) {
                            val idAttr = parser.getAttributeValue(null, "id")
                            val hrefAttr = parser.getAttributeValue(null, "href")
                            if (idAttr == coverId) {
                                coverHref = hrefAttr
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "metadata" -> inMetadata = false
                        "manifest" -> inManifest = false
                    }
                }
            }
            event = parser.next()
        }

        input.close()
        return OpfResult(title, author, language, coverHref)
    }

    private fun saveCover(zip: ZipFile, coverPathInZip: String, epubFile: File): String? {
        val entry: ZipEntry = zip.getEntry(coverPathInZip) ?: return null

        val dir = File(context.filesDir, "covers")
        if (!dir.exists()) dir.mkdirs()

        val outFile = File(dir, epubFile.nameWithoutExtension + "_cover.png")

        zip.getInputStream(entry).use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }

        return outFile.absolutePath
    }

    private fun newParser(input: InputStream): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        return factory.newPullParser().apply {
            setInput(input, "UTF-8")
        }
    }

    // ---------- CAPITOLI CON EPUBLIB PER IL READER ----------

    data class Chapter(
        val index: Int,
        val title: String,
        val text: String
    )

    /**
     * Usa epublib per leggere i capitoli nell'ordine della spine
     * e restituisce testo semplice per ciascun capitolo.
     */
    fun readChapters(file: File): List<Chapter> {
        return try {
            val epubReader = EpubReader()
            val epubBook: EpubBook = FileInputStream(file).use { fis ->
                epubReader.readEpub(fis)
            }

            val spine = epubBook.spine
            val spineSize = spine.size()

            val allChapters = (0 until spineSize).mapNotNull { index ->
                val res = spine.getResource(index) ?: return@mapNotNull null
                val bytes = res.inputStream.readBytes()
                val html = bytes.toString(Charsets.UTF_8)
                val plain = htmlToPlainText(html)
                val title = res.title ?: "Capitolo ${index + 1}"
                Chapter(
                    index = index,
                    title = title,
                    text = plain
                )
            }

            // Heuristica: scarta i capitoli troppo corti (front matter, sinossi, ecc.)
            val minLength = 2000 // puoi alzare/abbassare questo valore
            allChapters
                .filter { it.text.length >= minLength }
                .ifEmpty { allChapters } // fallback: se filtra tutto, tieni comunque tutto
        } catch (_: Exception) {
            emptyList()
        }
    }


    /**
     * Estrae testo semplice da HTML con una pulizia veloce.
     */
    private fun htmlToPlainText(html: String): String {
        var text = html

        // rimuovi script e style
        text = text.replace(Regex("(?s)<script.*?</script>"), " ")
        text = text.replace(Regex("(?s)<style.*?</style>"), " ")

        // trasforma tag di blocco in ritorni a capo
        text = text.replace(Regex("(?i)</p>"), "\n\n")
        text = text.replace(Regex("(?i)<br\\s*/?>"), "\n")
        text = text.replace(Regex("(?i)</h[1-6]>"), "\n\n")

        // rimuovi il resto dei tag
        text = text.replace(Regex("<[^>]+>"), " ")

        // normalizza gli spazi riga per riga
        val lines = text.lines()
            .map { line -> line.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotEmpty() }

        // aggiungi rientro a ogni paragrafo
        return lines.joinToString(separator = "\n\n") { line -> "    $line" }
    }

}
