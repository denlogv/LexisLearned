package dev.denlogv.lexislearned

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.denlogv.lexislearned.ui.AppNavHost
import dev.denlogv.lexislearned.ui.LexisLearnedTheme

/** The app's only activity; it hosts the Compose navigation graph. */
class MainActivity : ComponentActivity() {
    /**
     * Shows the app edge to edge.
     *
     * @param savedInstanceState the saved state, unused because navigation restores itself.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LexisLearnedTheme {
                Surface(Modifier.fillMaxSize()) { AppNavHost() }
            }
        }
    }
}
