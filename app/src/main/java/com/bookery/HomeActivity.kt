package com.bookery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import com.bookery.ui.home.HomeRoot
import com.bookery.ui.theme.BookeryTheme

class HomeActivity : ComponentActivity() {

    companion object {
        // stato statico semplice per notificare aggiornamenti
        var progressVersion = mutableStateOf(0)
    }

    override fun onResume() {
        super.onResume()
        // ogni volta che l'activity torna visibile, aumenta la versione
        progressVersion.value++
    }

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

