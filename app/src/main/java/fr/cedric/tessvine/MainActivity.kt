package fr.cedric.tessvine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.cedric.tessvine.data.Session
import fr.cedric.tessvine.ui.App
import fr.cedric.tessvine.ui.theme.TessvineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Session.init(this)
        coil.Coil.setImageLoader(
            coil.ImageLoader.Builder(applicationContext)
                .okHttpClient {
                    okhttp3.OkHttpClient.Builder()
                        .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                }
                .crossfade(true)
                .build()
        )
        enableEdgeToEdge()
        setContent {
            TessvineTheme {
                App()
            }
        }
    }
}
