package com.farias.prismablocks.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.farias.prismablocks.game.Adventure
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
    var musicOn by mutableStateOf(true)
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

    // Statistics
    var totalLines by mutableIntStateOf(0)
        private set
    var bestCombo by mutableIntStateOf(0)
        private set
    var totalScore by mutableStateOf(0L)
        private set
    var dailiesDone by mutableIntStateOf(0)
        private set
    var bestStreak by mutableIntStateOf(0)
        private set
    var lastReviewAsk by mutableStateOf(0L)
        private set

    // Adventure
    /** Highest level the player can play. */
    var adventureLevel by mutableIntStateOf(1)
        private set
    /** Best stars per finished level. */
    var levelStars by mutableStateOf(mapOf<Int, Int>())
        private set
    /** Trail chests already opened, by the level they follow. */
    var trailChests by mutableStateOf(setOf<Int>())
        private set
    /** Star chests opened so far (one every [Adventure.STARS_PER_CHEST] stars). */
    var starChests by mutableIntStateOf(0)
        private set

    // Daily missions
    private var missionDay = 0
    var missionProgress by mutableStateOf(List(Missions.COUNT) { 0 })
        private set
    var missionClaimed by mutableStateOf(List(Missions.COUNT) { false })
        private set

    // Daily chest (7-day calendar)
    var chestLastDay by mutableIntStateOf(0)
        private set
    /** Consecutive days the chest was opened, as of [chestLastDay]. */
    var chestRun by mutableIntStateOf(0)
        private set

    val isReady: Boolean get() = ::prefs.isInitialized

    fun init(context: Context) {
        if (isReady) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        coins = prefs.getInt("coins", 100) // a small welcome gift
        best = prefs.getInt("best", 0)
        soundOn = prefs.getBoolean("sound", true)
        musicOn = prefs.getBoolean("music", true)
        vibrationOn = prefs.getBoolean("vibration", true)
        notificationsOn = prefs.getBoolean("notifications", true)
        themeId = prefs.getString("theme", "neon") ?: "neon"
        ownedThemes = prefs.getStringSet("owned_themes", setOf("neon"))?.toSet() ?: setOf("neon")
        tutorialDone = prefs.getBoolean("tutorial_done", false)
        gamesPlayed = prefs.getInt("games_played", 0)
        streak = prefs.getInt("streak", 0)
        lastDailyDay = prefs.getInt("last_daily_day", 0)
        notifAsked = prefs.getBoolean("notif_asked", false)
        totalLines = prefs.getInt("total_lines", 0)
        bestCombo = prefs.getInt("best_combo", 0)
        totalScore = prefs.getLong("total_score", 0L)
        dailiesDone = prefs.getInt("dailies_done", 0)
        bestStreak = prefs.getInt("best_streak", 0)
        lastReviewAsk = prefs.getLong("last_review_ask", 0L)
        missionDay = prefs.getInt("mission_day", 0)
        missionProgress = (prefs.getString("mission_progress", "") ?: "").split(",").mapNotNull { it.toIntOrNull() }
            .let { if (it.size == Missions.COUNT) it else List(Missions.COUNT) { 0 } }
        missionClaimed = (prefs.getString("mission_claimed", "") ?: "").split(",").map { it == "1" }
            .let { if (it.size == Missions.COUNT) it else List(Missions.COUNT) { false } }
        chestLastDay = prefs.getInt("chest_last_day", 0)
        chestRun = prefs.getInt("chest_run", 0)
        adventureLevel = prefs.getInt("adventure_level", 1)
        levelStars = (prefs.getString("level_stars", "") ?: "").split(",").mapNotNull { e ->
            val p = e.split(":")
            if (p.size == 2) p[0].toIntOrNull()?.let { k -> p[1].toIntOrNull()?.let { v -> k to v } } else null
        }.toMap()
        trailChests = (prefs.getString("trail_chests", "") ?: "").split(",").mapNotNull { it.toIntOrNull() }.toSet()
        starChests = prefs.getInt("star_chests", 0)
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

    fun toggleMusic() {
        musicOn = !musicOn
        prefs.edit().putBoolean("music", musicOn).apply()
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

    fun markTutorial(done: Boolean) {
        tutorialDone = done
        prefs.edit().putBoolean("tutorial_done", done).apply()
    }

    fun onGameFinished(score: Int) {
        gamesPlayed += 1
        totalScore += score
        prefs.edit().putInt("games_played", gamesPlayed).putLong("total_score", totalScore).apply()
    }

    fun recordMove(lines: Int, combo: Int) {
        if (lines <= 0) return
        totalLines += lines
        val e = prefs.edit().putInt("total_lines", totalLines)
        if (combo > bestCombo) {
            bestCombo = combo
            e.putInt("best_combo", bestCombo)
        }
        e.apply()
    }

    val averageScore: Int get() = if (gamesPlayed == 0) 0 else (totalScore / gamesPlayed).toInt()

    fun markReviewAsked(now: Long) {
        lastReviewAsk = now
        prefs.edit().putLong("last_review_ask", now).apply()
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

    // ------------------------------------------------------------------ daily missions

    /** Read-only, safe to call while drawing the UI. */
    fun todaysMissions(): List<Mission> = Missions.forDay(today())

    fun missionProgressOf(index: Int): Int = if (missionDay == today()) missionProgress.getOrElse(index) { 0 } else 0

    fun missionClaimedOf(index: Int): Boolean = missionDay == today() && missionClaimed.getOrElse(index) { false }

    /** Starts a fresh set of missions when the day changes. Call from events, not from drawing. */
    fun ensureMissionDay() {
        val t = today()
        if (missionDay == t) return
        missionDay = t
        missionProgress = List(Missions.COUNT) { 0 }
        missionClaimed = List(Missions.COUNT) { false }
        saveMissions()
    }

    private fun saveMissions() {
        prefs.edit()
            .putInt("mission_day", missionDay)
            .putString("mission_progress", missionProgress.joinToString(","))
            .putString("mission_claimed", missionClaimed.joinToString(",") { if (it) "1" else "0" })
            .apply()
    }

    /**
     * Reports progress for missions of [type]. With [asMax] the value is a record (best combo,
     * best score) instead of an amount to add. Returns the missions completed by this event.
     */
    fun missionEvent(type: MissionType, value: Int, asMax: Boolean = false): List<Mission> {
        if (value <= 0) return emptyList()
        ensureMissionDay()
        val missions = todaysMissions()
        val progress = missionProgress.toMutableList()
        val done = ArrayList<Mission>()
        var changed = false
        missions.forEachIndexed { i, m ->
            if (m.type != type || progress[i] >= m.target) return@forEachIndexed
            val next = (if (asMax) maxOf(progress[i], value) else progress[i] + value).coerceAtMost(m.target)
            if (next != progress[i]) {
                progress[i] = next
                changed = true
                if (next >= m.target) done.add(m)
            }
        }
        if (changed) {
            missionProgress = progress
            saveMissions()
        }
        return done
    }

    /** Missions finished but not yet claimed. */
    val claimableMissions: Int
        get() {
            val missions = todaysMissions()
            return missions.indices.count { missionProgressOf(it) >= missions[it].target && !missionClaimedOf(it) }
        }

    /** Claims mission [index]; returns the coins earned (0 if not claimable). */
    fun claimMission(index: Int): Int {
        ensureMissionDay()
        val missions = todaysMissions()
        val m = missions.getOrNull(index) ?: return 0
        if (missionProgress[index] < m.target || missionClaimed[index]) return 0
        missionClaimed = missionClaimed.toMutableList().also { it[index] = true }
        saveMissions()
        addCoins(m.reward)
        return m.reward
    }

    // ------------------------------------------------------------------ daily chest

    val CHEST_REWARDS = listOf(20, 30, 40, 50, 70, 90, 150)

    fun chestAvailable(): Boolean = chestLastDay != today()

    /** Calendar position (0..6) of today's chest: the run continues only if yesterday was opened. */
    fun chestIndex(): Int = if (chestLastDay == yesterday()) chestRun % CHEST_REWARDS.size else 0

    /** Opens today's chest; [multiplier] is 2 when doubled with a video. Returns the coins earned. */
    fun openChest(multiplier: Int): Int {
        if (!chestAvailable()) return 0
        val idx = chestIndex()
        val reward = CHEST_REWARDS[idx] * multiplier
        chestRun = if (chestLastDay == yesterday()) chestRun + 1 else 1
        chestLastDay = today()
        prefs.edit().putInt("chest_last_day", chestLastDay).putInt("chest_run", chestRun).apply()
        addCoins(reward)
        return reward
    }

    // ------------------------------------------------------------------ adventure

    val totalStars: Int get() = levelStars.values.sum()

    fun starsFor(level: Int): Int = levelStars[level] ?: 0

    /** Saves the result of a level and unlocks the next one. Returns true the first time it's beaten. */
    fun completeLevel(level: Int, stars: Int): Boolean {
        val firstTime = level !in levelStars
        if (stars > starsFor(level)) levelStars = levelStars + (level to stars)
        if (level >= adventureLevel) adventureLevel = level + 1
        prefs.edit()
            .putInt("adventure_level", adventureLevel)
            .putString("level_stars", levelStars.entries.joinToString(",") { "${it.key}:${it.value}" })
            .apply()
        return firstTime
    }

    /** The chest after [level] can be opened: that level is beaten and the chest is still closed. */
    fun trailChestReady(level: Int): Boolean =
        Adventure.hasChestAfter(level) && adventureLevel > level && level !in trailChests

    /** Opens the chest after [level]; returns the coins earned (0 if it wasn't ready). */
    fun openTrailChest(level: Int): Int {
        if (!trailChestReady(level)) return 0
        trailChests = trailChests + level
        prefs.edit().putString("trail_chests", trailChests.joinToString(",")).apply()
        val reward = Adventure.trailChestReward(level)
        addCoins(reward)
        return reward
    }

    /** Stars needed for the next star chest. */
    val nextStarChest: Int get() = (starChests + 1) * Adventure.STARS_PER_CHEST

    fun starChestReady(): Boolean = totalStars >= nextStarChest

    fun openStarChest(): Int {
        if (!starChestReady()) return 0
        starChests += 1
        prefs.edit().putInt("star_chests", starChests).apply()
        addCoins(Adventure.STAR_CHEST_REWARD)
        return Adventure.STAR_CHEST_REWARD
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
        dailiesDone += 1
        bestStreak = maxOf(bestStreak, streak)
        prefs.edit().putInt("streak", streak).putInt("last_daily_day", t)
            .putInt("dailies_done", dailiesDone).putInt("best_streak", bestStreak).apply()
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
