package com.farias.prismablocks

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.farias.prismablocks.ads.AdsManager
import com.farias.prismablocks.game.GameViewModel
import com.farias.prismablocks.ui.GameScreen

class MainActivity : ComponentActivity() {

    private val vm: GameViewModel by viewModels()
    private lateinit var ads: AdsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark game UI: always light system bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        ads = AdsManager(this)
        ads.start()
        setContent { GameScreen(vm, ads) }
    }

    override fun onPause() {
        super.onPause()
        vm.cancelDrag()
        vm.save()
    }
}
