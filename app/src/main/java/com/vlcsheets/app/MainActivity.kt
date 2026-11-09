package com.vlcsheets.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vlcsheets.app.ui.VlcSheetsRoot

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory(application as VlcSheetsApp)
    }

    /**
     * Set by the viewer while it is on screen. Receives (direction, keyCode).
     * Returning true consumes the key event.
     */
    var pageTurnHandler: ((Int, Int) -> Boolean)? = null

    /** Set by Settings while it waits for the user to press a pedal key; gets the next key code. */
    var keyCaptureHandler: ((Int) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { VlcSheetsRoot(viewModel) }
        // After Android recreates the activity, the same intent comes back: import it only once.
        if (savedInstanceState == null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data?.let { viewModel.importAndOpen(it) }
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                val stream = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
                stream?.let { viewModel.importAndOpen(it) }
            }
        }
    }

    // dispatchKeyEvent is a public Activity method; lint flags it only because androidx.core
    // re-declares it as restricted. It is needed so page-turn keys win over the focused view.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val capture = keyCaptureHandler
        if (capture != null && event.keyCode != KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                keyCaptureHandler = null
                capture(event.keyCode)
            }
            return true
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            val settings = viewModel.settings.value
            val direction = when (event.keyCode) {
                settings.nextPageKey.takeIf { it != 0 } -> 1
                settings.prevPageKey.takeIf { it != 0 } -> -1
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_PAGE_DOWN,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_SPACE,
                -> 1

                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_PAGE_UP,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_UP,
                -> -1

                else -> 0
            }
            if (direction != 0 && pageTurnHandler?.invoke(direction, event.keyCode) == true) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        pageTurnHandler = null
        super.onDestroy()
    }
}
