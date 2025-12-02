// com.bookery.ui.settings/SettingsScreen.kt
package com.bookery.ui.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.bookery.ui.theme.BookeryTheme

@Composable
fun SettingsScreen() {
    Text("Impostazioni")
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    BookeryTheme { SettingsScreen() }
}
