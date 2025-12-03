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
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.DisposableEffect
import com.bookery.data.local.BookProgressStore
import com.bookery.ui.reader.ReaderActivity
import androidx.compose.runtime.LaunchedEffect
import com.bookery.HomeActivity.Companion.progressVersion

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
fun LibraryScreen(progressVersion: Int) {
    val context = LocalContext.current
    val parser = remember { EpubParser(context) }
    val storage = remember { LibraryStorage(context) }

    var books by remember { mutableStateOf(storage.loadBooks()) }

    // ricalcola i progressi ogni volta che cambia progressVersion o books
    val progressMap: Map<String, Float> =
        remember(progressVersion, books) {
            books.associate { it.id to BookProgressStore.getProgress(context, it.id) }
        }

    Scaffold(
        topBar = { /* come prima */ }
    ) { padding ->
        if (books.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                Text(
                    text = "Nessun libro presente, carica un EPUB.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LibraryList(
                books = books,
                progressMap = progressMap,
                modifier = Modifier.padding(padding),
                onBookClick = { book ->
                    context.startActivity(ReaderActivity.newIntent(context, book.id))
                }
            )
        }
    }
}

@Composable
private fun LibraryList(
    books: List<Book>,
    progressMap: Map<String, Float>,
    modifier: Modifier = Modifier,
    onBookClick: (Book) -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(books) { book ->
            val progress = progressMap[book.id] ?: 0f
            BookRow(book = book, progress = progress, onClick = { onBookClick(book) })
        }
    }
}

@Composable
private fun BookRow(book: Book, progress: Float, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
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
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth()
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
    BookeryTheme { LibraryScreen(progressVersion = 0) }
}

