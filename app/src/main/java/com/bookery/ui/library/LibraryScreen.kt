@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bookery.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.bookery.data.model.Book
import com.bookery.data.util.EpubParser
import com.bookery.ui.theme.BookeryTheme
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream

// ---------- STORAGE SEMPLICE SU FILE ----------

private const val LIBRARY_FILE = "library.json"

private class LibraryStorage(private val context: android.content.Context) {

    fun loadBooks(): List<Book> {
        val file = File(context.filesDir, LIBRARY_FILE)
        if (!file.exists()) return emptyList()
        return try {
            val json = file.readText()
            val array = JSONArray(json)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                Book(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    author = o.getString("author"),
                    language = o.getString("language"),
                    coverUrl = o.optString("coverUrl", null)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveBooks(books: List<Book>) {
        val array = JSONArray()
        books.forEach { b ->
            val o = JSONObject()
            o.put("id", b.id)
            o.put("title", b.title)
            o.put("author", b.author)
            o.put("language", b.language)
            o.put("coverUrl", b.coverUrl)
            array.put(o)
        }
        val file = File(context.filesDir, LIBRARY_FILE)
        file.writeText(array.toString())
    }
}

// ---------- UI ----------

@Composable
fun LibraryScreen() {
    val context = LocalContext.current
    val parser = remember { EpubParser(context) }
    val storage = remember { LibraryStorage(context) }

    var books by remember {
        mutableStateOf(storage.loadBooks())
    }


//    var books by remember {
//        mutableStateOf(
//            storage.loadBooks().ifEmpty {
//                listOf(
//                    Book(
//                        id = "sample_1",
//                        title = "La Bella e la Bestia",
//                        author = "J.M. Leprince de Beaumont",
//                        language = "IT"
//                    ),
//                    Book(
//                        id = "sample_2",
//                        title = "To Build a Fire",
//                        author = "Jack London",
//                        language = "EN"
//                    )
//                )
//            }
//        )
//    }

    val openDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                val localFile = copyEpubToAppFolder(context.filesDir, uri, context.contentResolver)
                if (localFile != null) {
                    val parsedBook: Book? = parser.parse(localFile)
                    val newBook = parsedBook ?: Book(
                        id = localFile.absolutePath,
                        title = localFile.nameWithoutExtension,
                        author = "Sconosciuto",
                        language = "EN",
                        coverUrl = null
                    )
                    val updated = books + newBook
                    books = updated
                    storage.saveBooks(updated)
                }
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biblioteca") },
                actions = {
                    IconButton(
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("application/epub+zip"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aggiungi epub"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (books.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                Text(
                    text = "Nessun libro presente, carica un EPUB.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LibraryList(
                books = books,
                modifier = Modifier.padding(padding)
            )
        }
    }

}

/**
 * Copia l'epub scelto in /files/epub e restituisce il File locale.
 */
private fun copyEpubToAppFolder(
    filesDir: File,
    uri: Uri,
    resolver: android.content.ContentResolver
): File? {
    return try {
        val dir = File(filesDir, "epub")
        if (!dir.exists()) dir.mkdirs()

        val fileName = "book_${System.currentTimeMillis()}.epub"
        val dstFile = File(dir, fileName)

        resolver.openInputStream(uri).use { input: InputStream? ->
            dstFile.outputStream().use { output: OutputStream ->
                if (input != null) {
                    input.copyTo(output)
                }
            }
        }
        dstFile
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun LibraryList(books: List<Book>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(books) { book ->
            BookRow(book = book)
        }
    }
}

@Composable
private fun BookRow(book: Book) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            if (book.coverUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(book.coverUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .padding(end = 12.dp)
                )
            }
            Column {
                Text(book.title)
                Text(book.author)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryScreenPreview() {
    BookeryTheme { LibraryScreen() }
}
