package com.farias.prismablocks.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar

/**
 * Everything the player keeps between sessions: coins, themes, records, daily streak and settings.
 * Backed by SharedPreferences and exposed as Compose state so screens update automatically.
 */
object Progress {

    private const val PREFS = "prisma_blocks"
    private lateinit var prefs: SharedPreferences

    var coins by mutableIntStateOf(0)
        private set
    var best by mutableIntStateOf(0)
        private set
    var soundOn by mutableStateOf(true)
        private set
    var vibrationOn by mutableStateOf(true)
        private set
    var notificationsOn by mutableStateOf(true)
        private set
    var themeId by mutableStateOf("neon")
        private set
    var ownedThemes by mutableStateOf(setOf("neon"))
        private set
    var tutorialDone by mutableStateOf(false)
        private set
    var gamesPlayed by mutableIntStateOf(0)
        private set
    var streak by mutableIntStateOf(0)
        private set
    /** Last day (yyyyMMdd) the daily challenge was completed. */
    var lastDailyDay by mutableIntStateOf(0)
        private set
    var notifAsked by mutableStateOf(false)
        private set

    val isReady: Boolean get() = ::prefs.isInitialized

    fun init(context: Context) {
        if (isReady) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        coins = prefs.getInt("coins", 100) // a small welcome gift
        best = prefs.getInt("best", 0)
        soundOn = prefs.getBoolean("sound", true)
        vibrationOn = prefs.getBoolean("vibration", true)
        notificationsOn = prefs.getBoolean("notifications", true)
        themeId = prefs.getString("theme", "neon") ?: "neon"
        ownedThemes = prefs.getStringSet("owned_themes", setOf("neon"))?.toSet() ?: setOf("neon")
        tutorialDone = prefs.getBoolean("tutorial_done", false)
        gamesPlayed = prefs.getInt("games_played", 0)
        streak = prefs.getInt("streak", 0)
        lastDailyDay = prefs.getInt("last_daily_day", 0)
        notifAsked = prefs.getBoolean("notif_asked", false)
    }

    // ------------------------------------------------------------------ coins

    fun addCoins(amount: Int) {
        if (amount <= 0) return
        coins += amount
        prefs.edit().putInt("coins", coins).apply()
    }

    /** Spends coins if the player has enough. */
    fun spend(amount: Int): Boolean {
        if (coins < amount) return false
        coins -= amount
        prefs.edit().putInt("coins", coins).apply()
        return true
    }

    // ------------------------------------------------------------------ records and settings

    fun submitScore(score: Int): Boolean {
        if (score <= best) return false
        best = score
        prefs.edit().putInt("best", best).apply()
        return true
    }

    fun toggleSound() {
        soundOn = !soundOn
        prefs.edit().putBoolean("sound", soundOn).apply()
    }

    fun toggleVibration() {
        vibrationOn = !vibrationOn
        prefs.edit().putBoolean("vibration", vibrationOn).apply()
    }

    fun setNotifications(on: Boolean) {
        notificationsOn = on
        prefs.edit().putBoolean("notifications", on).apply()
    }

    fun markNotifAsked() {
        notifAsked = true
        prefs.edit().putBoolean("notif_asked", true).apply()
    }

    fun setTutorialDone(done: Boolean) {
        tutorialDone = done
        prefs.edit().putBoolean("tutorial_done", done).apply()
    }

    fun onGameFinished() {
        gamesPlayed += 1
        prefs.edit().putInt("games_played", gamesPlayed).apply()
    }

    // ------------------------------------------------------------------ themes

    fun owns(id: String) = id in ownedThemes

    fun buyTheme(id: String, price: Int): Boolean {
        if (owns(id)) return true
        if (!spend(price)) return false
        ownedThemes = ownedThemes + id
        prefs.edit().putStringSet("owned_themes", ownedThemes).apply()
        return true
    }

    fun selectTheme(id: String) {
        if (!owns(id)) return
        themeId = id
        prefs.edit().putString("theme", id).apply()
    }

    // ------------------------------------------------------------------ daily challenge

    fun dailyDoneToday(): Boolean = lastDailyDay == today()

    /** Streak to show: it survives until the end of the day after the last completion. */
    fun currentStreak(): Int =
        if (lastDailyDay == today() || lastDailyDay == yesterday()) streak else 0

    /** Records today's daily as completed and returns the new streak. */
    fun completeDaily(): Int {
        val t = today()
        if (lastDailyDay == t) return streak
        streak = if (lastDailyDay == yesterday()) streak + 1 else 1
        lastDailyDay = t
        prefs.edit().putInt("streak", streak).putInt("last_daily_day", t).apply()
        return streak
    }

    // ------------------------------------------------------------------ saved games

    fun loadString(key: String): String? = prefs.getString(key, null)
    fun saveString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun remove(key: String) = prefs.edit().remove(key).apply()
    fun has(key: String): Boolean = prefs.contains(key)

    // ------------------------------------------------------------------ dates (yyyyMMdd, local time)

    fun today(): Int = dayOf(Calendar.getInstance())

    fun yesterday(): Int = dayOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) })

    private fun dayOf(c: Calendar): Int =
        c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
}
