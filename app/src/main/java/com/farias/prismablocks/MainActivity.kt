package com.farias.prismablocks

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
import com.farias.prismablocks.notify.DailyReminder
import com.farias.prismablocks.ui.GameScreen
import com.farias.prismablocks.ui.HomeScreen
import com.farias.prismablocks.ui.SplashScreen
import com.farias.prismablocks.ui.StatsScreen
import com.farias.prismablocks.ui.ThemesScreen

enum class Screen { SPLASH, HOME, GAME, THEMES, STATS }

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
        ads.start()
        val sound = SoundManager.get(this)
        MusicPlayer.init(this)

        setContent {
            var screen by remember { mutableStateOf(Screen.SPLASH) }
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
                    )
                    Screen.GAME -> GameScreen(
                        vm = vm,
                        ads = ads,
                        onGreatMoment = { ReviewPrompt.maybeAsk(this@MainActivity) },
                        onHome = {
                            vm.save()
                            screen = Screen.HOME
                        },
                    )
                    Screen.THEMES -> ThemesScreen(sound) { screen = Screen.HOME }
                    Screen.STATS -> StatsScreen { screen = Screen.HOME }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MusicPlayer.onResume()
    }

    override fun onPause() {
        super.onPause()
        MusicPlayer.onPause()
        vm.cancelDrag()
        vm.save()
    }
}
