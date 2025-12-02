// com.bookery.ui.home/HomeScreen.kt
package com.bookery.ui.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.bookery.ui.theme.BookeryTheme

@Composable
fun HomeScreen() {
    Text("Home Screen")
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    BookeryTheme { HomeScreen() }
}
