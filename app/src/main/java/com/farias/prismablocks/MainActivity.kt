package com.farias.prismablocks

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.farias.prismablocks.ads.AdsManager
import com.farias.prismablocks.audio.MusicPlayer
import com.farias.prismablocks.audio.SoundManager
import com.farias.prismablocks.data.Progress
import com.farias.prismablocks.game.GameViewModel
import com.farias.prismablocks.game.Mode
import com.farias.prismablocks.notify.DailyReminder
import com.farias.prismablocks.ui.AdventureScreen
import com.farias.prismablocks.ui.GameScreen
import com.farias.prismablocks.ui.HomeScreen
import com.farias.prismablocks.ui.SplashScreen
import com.farias.prismablocks.ui.StatsScreen
import com.farias.prismablocks.ui.ThemesScreen
import com.farias.prismablocks.ui.skipChestToday

enum class Screen { SPLASH, HOME, GAME, THEMES, STATS, ADVENTURE }

class MainActivity : ComponentActivity() {

    private val vm: GameViewModel by viewModels()
    private lateinit var ads: AdsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Progress.init(this)
        // Dark game UI: always light system bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        DailyReminder.createChannel(this)
        DailyReminder.sync(this)
        ads = AdsManager(this)
        // Debug store screenshots (`--ez shots true`) are taken without the test ad banner.
        if (!(BuildConfig.DEBUG && intent.getBooleanExtra("shots", false))) ads.start()
        val sound = SoundManager.get(this)
        MusicPlayer.init(this)

        val initial = debugStartScreen()

        setContent {
            var screen by remember { mutableStateOf(initial) }
            Crossfade(targetState = screen, animationSpec = tween(350), label = "screen") { current ->
                when (current) {
                    Screen.SPLASH -> SplashScreen(sound) { screen = Screen.HOME }
                    Screen.HOME -> HomeScreen(
                        ads = ads,
                        sound = sound,
                        hasSave = vm.hasClassicSave(),
                        onPlay = { continueSaved ->
                            if (continueSaved) {
                                vm.startClassic(continueSaved = true)
                                screen = Screen.GAME
                            } else {
                                ads.onNewGame {
                                    vm.startClassic(continueSaved = false)
                                    screen = Screen.GAME
                                }
                            }
                        },
                        onDaily = {
                            ads.onNewGame {
                                vm.startDaily()
                                screen = Screen.GAME
                            }
                        },
                        onThemes = { screen = Screen.THEMES },
                        onStats = { screen = Screen.STATS },
                        onAdventure = { screen = Screen.ADVENTURE },
                    )
                    Screen.GAME -> GameScreen(
                        vm = vm,
                        ads = ads,
                        onGreatMoment = { ReviewPrompt.maybeAsk(this@MainActivity) },
                        onHome = {
                            vm.save()
                            screen = if (vm.mode == Mode.ADVENTURE) Screen.ADVENTURE else Screen.HOME
                        },
                    )
                    Screen.THEMES -> ThemesScreen(sound) { screen = Screen.HOME }
                    Screen.STATS -> StatsScreen { screen = Screen.HOME }
                    Screen.ADVENTURE -> AdventureScreen(
                        sound = sound,
                        onBack = { screen = Screen.HOME },
                        onPlay = { number ->
                            ads.onNewGame {
                                vm.startAdventure(number)
                                screen = Screen.GAME
                            }
                        },
                    )
                }
            }
        }
    }

    /**
     * Debug builds only: lets the automated screenshot test open a screen directly, e.g.
     * `adb shell am start -n <pkg>/com.farias.prismablocks.MainActivity --es screen game`
     * (`--ei level 41` picks the adventure level of `adventure_game`; `continue` resumes the saved classic game;
     * `--ez shots true` skips ads for store screenshots).
     */
    private fun debugStartScreen(): Screen {
        if (!BuildConfig.DEBUG) return Screen.SPLASH
        val target = intent.getStringExtra("screen") ?: return Screen.SPLASH
        if (intent.getBooleanExtra("landscape", false)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        if (target != "chest") skipChestToday()
        return when (target) {
            "game" -> {
                vm.startClassic(continueSaved = false)
                Screen.GAME
            }
            "continue" -> {
                vm.startClassic(continueSaved = true)
                Screen.GAME
            }
            "adventure_game" -> {
                vm.startAdventure(intent.getIntExtra("level", 5))
                Screen.GAME
            }
            "map" -> Screen.ADVENTURE
            "themes" -> Screen.THEMES
            "stats" -> Screen.STATS
            else -> Screen.HOME
        }
    }

    override fun onResume() {
        super.onResume()
        Progress.ensureMissionDay()
        MusicPlayer.onResume()
    }

    override fun onPause() {
        super.onPause()
        MusicPlayer.onPause()
        vm.cancelDrag()
        vm.save()
    }
}
