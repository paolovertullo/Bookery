package com.bookery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.bookery.ui.home.HomeRoot
import com.bookery.ui.theme.BookeryTheme

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookeryTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    HomeRoot()
                }
            }
        }
    }
}
