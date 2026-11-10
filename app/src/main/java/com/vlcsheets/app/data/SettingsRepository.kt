package com.vlcsheets.app.data

import androidx.core.content.edit
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** What holding a finger on the left or right side of a page does. */
enum class SideAction { NONE, FIRST_PAGE, LAST_PAGE, PREV_SONG, NEXT_SONG, AUDIO, METRONOME }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val keepScreenOn: Boolean = true,
    val volumeKeys: Boolean = true,
    val invertPagesInDark: Boolean = false,
    /** Tapping the left or right side of a page turns it. */
    val tapToTurn: Boolean = true,
    /** Extra key codes (for example a Bluetooth pedal) for next and previous page; 0 = none. */
    val nextPageKey: Int = 0,
    val prevPageKey: Int = 0,
    val cropMargins: Boolean = false,
    /** Two pages side by side when the screen is wider than tall. */
    val twoPagesLandscape: Boolean = false,
    /** Turn half a page at a time: the top half changes first, then the bottom. */
    val halfPageTurn: Boolean = false,
    /** Cream paper tone on the pages instead of white. */
    val warmPaper: Boolean = false,
    val hideAnnotations: Boolean = false,
    val leftHold: SideAction = SideAction.NONE,
    val rightHold: SideAction = SideAction.NONE,
    /** Tuner reference pitch for A4, in Hz. */
    val tunerA4: Int = 440,
    /** While annotating, handwriting becomes text by itself after a short pause. */
    val autoHandwriting: Boolean = false,
    /** App language as a tag ("ca", "es", "en"); empty = the phone's language. */
    val language: String = "",
    /** Size of handwriting turned into text: index in Handwriting.TEXT_SIZES (0 = average of what was written). */
    val handwritingSize: Int = 0,
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = prefsOf(context)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun read(): AppSettings = AppSettings(
        themeMode = runCatching {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM),
        dynamicColor = prefs.getBoolean("dynamic_color", false),
        keepScreenOn = prefs.getBoolean("keep_screen_on", true),
        volumeKeys = prefs.getBoolean("volume_keys", true),
        invertPagesInDark = prefs.getBoolean("invert_pages_dark", false),
        tapToTurn = prefs.getBoolean("tap_to_turn", true),
        nextPageKey = prefs.getInt("next_page_key", 0),
        prevPageKey = prefs.getInt("prev_page_key", 0),
        cropMargins = prefs.getBoolean("crop_margins", false),
        twoPagesLandscape = prefs.getBoolean("two_pages_landscape", false),
        halfPageTurn = prefs.getBoolean("half_page_turn", false),
        warmPaper = prefs.getBoolean("warm_paper", false),
        hideAnnotations = prefs.getBoolean("hide_annotations", false),
        leftHold = sideAction(prefs.getString("left_hold", null)),
        rightHold = sideAction(prefs.getString("right_hold", null)),
        tunerA4 = prefs.getInt("tuner_a4", 440),
        autoHandwriting = prefs.getBoolean("auto_handwriting", false),
        language = prefs.getString(LANGUAGE_KEY, "").orEmpty(),
        handwritingSize = prefs.getInt("handwriting_size", 0),
    )

    private fun sideAction(name: String?) =
        SideAction.entries.firstOrNull { it.name == name } ?: SideAction.NONE

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        prefs.edit {
            putString("theme_mode", next.themeMode.name)
            putBoolean("dynamic_color", next.dynamicColor)
            putBoolean("keep_screen_on", next.keepScreenOn)
            putBoolean("volume_keys", next.volumeKeys)
            putBoolean("invert_pages_dark", next.invertPagesInDark)
            putBoolean("tap_to_turn", next.tapToTurn)
            putInt("next_page_key", next.nextPageKey)
            putInt("prev_page_key", next.prevPageKey)
            putBoolean("crop_margins", next.cropMargins)
            putBoolean("two_pages_landscape", next.twoPagesLandscape)
            putBoolean("half_page_turn", next.halfPageTurn)
            putBoolean("warm_paper", next.warmPaper)
            putBoolean("hide_annotations", next.hideAnnotations)
            putString("left_hold", next.leftHold.name)
            putString("right_hold", next.rightHold.name)
            putInt("tuner_a4", next.tunerA4)
            putBoolean("auto_handwriting", next.autoHandwriting)
            putString(LANGUAGE_KEY, next.language)
            putInt("handwriting_size", next.handwritingSize)
        }
        _settings.value = next
    }
}

private const val LANGUAGE_KEY = "language"

private fun prefsOf(context: Context): SharedPreferences =
    context.getSharedPreferences("vlc_sheets_settings", Context.MODE_PRIVATE)

/** The language picked in Settings, read before the app's screens exist; empty = the phone's language. */
fun savedLanguage(context: Context): String = prefsOf(context).getString(LANGUAGE_KEY, "").orEmpty()
