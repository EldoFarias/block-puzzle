package com.farias.prismablocks.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farias.prismablocks.R
import com.farias.prismablocks.ads.AdsManager
import com.farias.prismablocks.audio.Sfx
import com.farias.prismablocks.audio.SoundManager
import com.farias.prismablocks.data.Progress
import com.farias.prismablocks.game.GameViewModel
import com.farias.prismablocks.notify.DailyReminder
import kotlin.math.sin

const val VIDEO_COINS = 40

@Composable
fun HomeScreen(
    ads: AdsManager,
    sound: SoundManager,
    hasSave: Boolean,
    onPlay: (continueSaved: Boolean) -> Unit,
    onDaily: () -> Unit,
    onThemes: () -> Unit,
) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var showHowTo by remember { mutableStateOf(false) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Progress.setNotifications(granted)
        DailyReminder.sync(context)
    }

    fun enableNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !DailyReminder.canNotify(context)) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Progress.setNotifications(true)
            DailyReminder.sync(context)
        }
    }

    // Ask for notification permission once, after the player has finished a game (not on first launch).
    LaunchedEffect(Unit) {
        if (Progress.gamesPlayed >= 1 && !Progress.notifAsked && Progress.notificationsOn) {
            Progress.markNotifAsked()
            enableNotifications()
        }
    }

    fun watchForCoins() {
        if (ads.adsReady && ads.rewardedReady) {
            ads.showRewarded {
                Progress.addCoins(VIDEO_COINS)
                sound.play(Sfx.COIN)
                Toast.makeText(context, context.getString(R.string.coins_reward, VIDEO_COINS), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, R.string.no_video, Toast.LENGTH_SHORT).show()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        FloatingGems()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoinPill(Progress.coins, onPlus = { watchForCoins() })
                Spacer(Modifier.weight(1f))
                GlassCircleButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings), tint = Palette.TextPrimary)
                }
            }

            Spacer(Modifier.weight(0.7f))
            Logo()
            Spacer(Modifier.height(10.dp))
            if (Progress.best > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CrownIcon(Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.best_x, Progress.best), style = Styles.glow(18, Palette.Gold))
                }
            }
            Spacer(Modifier.weight(0.5f))

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PlayButton(if (hasSave) stringResource(R.string.home_continue) else stringResource(R.string.home_play)) {
                    sound.play(Sfx.WHOOSH, 0.7f)
                    onPlay(hasSave)
                }
                if (hasSave) {
                    Text(
                        stringResource(R.string.home_new_game),
                        style = Styles.plain(15, Palette.TextSecondary, FontWeight.Bold),
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPlay(false) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    Spacer(Modifier.height(16.dp))
                }
                Spacer(Modifier.height(8.dp))
                DailyCard(onDaily)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeTile(stringResource(R.string.themes), Modifier.weight(1f), onThemes) {
                        PaletteIcon(Modifier.size(22.dp))
                    }
                    HomeTile(stringResource(R.string.how_to), Modifier.weight(1f), { showHowTo = true }) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Palette.Cyan, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            BannerAd(ads)
        }

        if (showSettings) {
            SettingsDialog(
                ads = ads,
                onToggleReminder = {
                    if (Progress.notificationsOn) {
                        Progress.setNotifications(false)
                        DailyReminder.sync(context)
                    } else {
                        enableNotifications()
                    }
                },
                onClose = { showSettings = false },
            )
        }
        if (showHowTo) HowToDialog { showHowTo = false }
    }
}

@Composable
private fun Logo() {
    val transition = rememberInfiniteTransition(label = "logo")
    val t by transition.animateFloat(
        0f, 6.2832f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "bob",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .size(96.dp)
                .graphicsLayer { translationY = sin(t) * 6.dp.toPx() },
        ) {
            val unit = size.width * 0.46f
            val gap = size.width * 0.04f
            val colors = listOf(1, 2, 6, 7)
            for (i in 0 until 4) {
                val col = i % 2
                val row = i / 2
                val bob = sin(t + i * 0.8f) * size.width * 0.015f
                drawGem(Offset(col * (unit + gap) + gap / 2f, row * (unit + gap) + gap / 2f + bob), unit, Palette.block(colors[i]))
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("PRISMA", style = Styles.gradient(48))
        Text("BLOCKS", style = Styles.glow(22, Color.White, FontWeight.Black).copy(letterSpacing = 9.sp))
    }
}

@Composable
private fun PlayButton(text: String, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "play")
    val pulse by transition.animateFloat(
        1f, 1.04f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse",
    )
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6), Color(0xFFDB2777))))
            .border(1.5.dp, Color.White.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(22.dp)) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.15f, 0f)
                    lineTo(size.width, size.height / 2f)
                    lineTo(size.width * 0.15f, size.height)
                    close()
                }
                drawPath(path, Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Text(text, style = Styles.plain(24, Color.White, FontWeight.Black).copy(letterSpacing = 2.sp))
        }
    }
}

@Composable
private fun DailyCard(onDaily: () -> Unit) {
    val done = Progress.dailyDoneToday()
    val streak = Progress.currentStreak()
    val target = GameViewModel.dailyTargetFor(Progress.today())
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0x3322D3EE), Color(0x33E879F9))))
            .border(1.5.dp, Brush.horizontalGradient(listOf(Palette.Cyan, Palette.Magenta)), shape)
            .clickable(enabled = !done, onClick = onDaily)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (done) Icons.Default.Check else Icons.Default.Star,
                contentDescription = null,
                tint = if (done) Color(0xFF34D399) else Palette.Gold,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.daily_title), style = Styles.plain(18, Palette.TextPrimary, FontWeight.Black))
            Text(
                if (done) stringResource(R.string.daily_done) else stringResource(R.string.daily_goal, target),
                style = Styles.plain(14, if (done) Color(0xFF34D399) else Palette.TextSecondary, FontWeight.SemiBold),
            )
        }
        if (streak > 0) {
            Text("🔥 $streak", style = Styles.glow(20, Color(0xFFFB923C), FontWeight.Black))
        }
    }
}

@Composable
private fun HomeTile(label: String, modifier: Modifier, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(Palette.Glass)
            .border(1.dp, Palette.GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.width(10.dp))
        Text(label, style = Styles.plain(16, Palette.TextPrimary, FontWeight.Bold))
    }
}

@Composable
private fun PaletteIcon(modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension * 0.24f
        val colors = Palette.theme.blocks
        drawCircle(colors[0], r, Offset(size.width * 0.28f, size.height * 0.30f))
        drawCircle(colors[1], r, Offset(size.width * 0.72f, size.height * 0.30f))
        drawCircle(colors[2], r, Offset(size.width * 0.28f, size.height * 0.72f))
        drawCircle(colors[6], r, Offset(size.width * 0.72f, size.height * 0.72f))
    }
}

/** A few small gems drifting up behind the menu. */
@Composable
private fun FloatingGems() {
    val transition = rememberInfiniteTransition(label = "float")
    val t by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Canvas(Modifier.fillMaxSize()) {
        val seeds = listOf(0.08f, 0.22f, 0.37f, 0.55f, 0.70f, 0.86f, 0.15f, 0.63f, 0.93f)
        seeds.forEachIndexed { i, x ->
            val speed = 0.6f + (i % 3) * 0.25f
            val y = 1.1f - ((t * speed + i * 0.13f) % 1.2f)
            val s = size.minDimension * (0.035f + (i % 4) * 0.01f)
            drawGem(
                Offset(size.width * x, size.height * y), s, Palette.block(i % 7 + 1),
                alpha = 0.28f, glow = false,
            )
        }
    }
}

@Composable
private fun SettingsDialog(ads: AdsManager, onToggleReminder: () -> Unit, onClose: () -> Unit) {
    DimOverlay {
        GlassPanel {
            Text(stringResource(R.string.settings), style = Styles.gradient(30))
            Spacer(Modifier.height(4.dp))
            ToggleRow(stringResource(R.string.sound), Progress.soundOn) { Progress.toggleSound() }
            ToggleRow(stringResource(R.string.vibration), Progress.vibrationOn) { Progress.toggleVibration() }
            ToggleRow(stringResource(R.string.notifications), Progress.notificationsOn) { onToggleReminder() }
            if (ads.privacyOptionsRequired) PrivacyLink(ads)
            Spacer(Modifier.height(4.dp))
            PrimaryButton(stringResource(R.string.got_it)) { onClose() }
        }
    }
}
