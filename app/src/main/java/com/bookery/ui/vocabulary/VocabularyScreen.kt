package com.bookery.ui.vocabulary

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.bookery.ui.theme.BookeryTheme

@Composable
fun VocabularyScreen() {
    // Qui in futuro metteremo gli esercizi di spaced repetition
    Text("DIZIONARIO / VOCABOLARIO")
}

@Preview(showBackground = true)
@Composable
private fun VocabularyScreenPreview() {
    BookeryTheme { VocabularyScreen() }
}
