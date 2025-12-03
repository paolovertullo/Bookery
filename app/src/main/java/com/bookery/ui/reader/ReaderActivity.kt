package com.bookery.ui.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookery.data.local.BookProgressStore
import com.bookery.data.util.EpubParser
import com.bookery.ui.theme.BookeryTheme
import java.io.File

class ReaderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bookId = intent.getStringExtra(EXTRA_BOOK_ID) ?: return
        val initialProgress = BookProgressStore.getProgress(this, bookId)

        val epubFile = File(bookId)
        val parser = EpubParser(this)
        val chapters = parser.readChapters(epubFile)

        setContent {
            BookeryTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ReaderScreen(
                        bookId = bookId,
                        chapters = chapters,
                        initialProgress = initialProgress,
                        onProgressChanged = { p ->
                            BookProgressStore.setProgress(this, bookId, p)
                        },
                        onExit = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        private const val EXTRA_BOOK_ID = "book_id"

        fun newIntent(context: Context, bookId: String): Intent =
            Intent(context, ReaderActivity::class.java).putExtra(EXTRA_BOOK_ID, bookId)
    }
}

@Composable
private fun ReaderScreen(
    bookId: String,
    chapters: List<EpubParser.Chapter>,
    initialProgress: Float,
    onProgressChanged: (Float) -> Unit,
    onExit: () -> Unit
) {
    if (chapters.isEmpty()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Impossibile leggere questo EPUB.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onExit) { Text("Torna alla biblioteca") }
        }
        return
    }

    // mappiamo il progress iniziale su capitolo/pagina in modo semplice
    var chapterIndex by remember {
        mutableStateOf(
            (initialProgress * chapters.size)
                .toInt()
                .coerceIn(0, chapters.lastIndex)
        )
    }

    val currentChapter = chapters[chapterIndex]
    val charsPerPage = 1500.coerceAtLeast(500)
    val pagesInChapter = (currentChapter.text.length / charsPerPage) + 1

    var pageInChapter by remember(chapterIndex) {
        mutableStateOf(0)
    }

    val pageText by remember(currentChapter, pageInChapter, charsPerPage) {
        mutableStateOf(
            currentChapter.text.chunked(charsPerPage)
                .getOrNull(pageInChapter)
                ?: ""
        )
    }

    // aggiorna il progresso globale: capitolo + pagina
    LaunchedEffect(chapterIndex, pageInChapter, pagesInChapter) {
        val chapterPart = chapterIndex.toFloat() / chapters.size.coerceAtLeast(1)
        val pagePart =
            if (pagesInChapter <= 1) 0f else pageInChapter.toFloat() / pagesInChapter.toFloat()
        val progress = (chapterPart + pagePart / chapters.size.coerceAtLeast(1))
            .coerceIn(0f, 1f)
        onProgressChanged(progress)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        //Text("Lettura libro: $bookId")
        //Spacer(Modifier.height(4.dp))
        //Text(currentChapter.title)
        //Spacer(Modifier.height(8.dp))
        Text("Parte ${chapterIndex + 1} di ${chapters.size}")
        Text("Pagina ${pageInChapter + 1} di $pagesInChapter")
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(pageText)
        }

        Spacer(Modifier.height(16.dp))

        Row {
            Button(
                onClick = {
                    if (pageInChapter > 0) {
                        pageInChapter -= 1
                    } else if (chapterIndex > 0) {
                        chapterIndex -= 1
                        pageInChapter = 0
                    }
                },
                enabled = chapterIndex > 0 || pageInChapter > 0
            ) {
                Text("Indietro")
            }

            Spacer(Modifier.width(12.dp))

            Button(
                onClick = {
                    if (pageInChapter < pagesInChapter - 1) {
                        pageInChapter += 1
                    } else if (chapterIndex < chapters.lastIndex) {
                        chapterIndex += 1
                        pageInChapter = 0
                    }
                },
                enabled = chapterIndex < chapters.lastIndex || pageInChapter < pagesInChapter - 1
            ) {
                Text("Avanti")
            }

            Spacer(Modifier.width(12.dp))

            Button(onClick = { onExit() }) {
                Text("Esci")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReaderPreview() {
    val demoChapters = listOf(
        EpubParser.Chapter(
            index = 0,
            title = "Capitolo 1",
            text = ("Lorem ipsum ".repeat(200))
        )
    )
    BookeryTheme {
        ReaderScreen(
            bookId = "sample",
            chapters = demoChapters,
            initialProgress = 0.3f,
            onProgressChanged = {},
            onExit = {}
        )
    }
}
