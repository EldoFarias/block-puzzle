package com.farias.prismablocks.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalUriHandler
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
import kotlinx.coroutines.delay
import kotlin.math.sin

const val VIDEO_COINS = 40

const val PRIVACY_POLICY_URL = "https://eldofarias.github.io/prisma-blocks/privacy.html"

/** Time to press back a second time to leave the game from the menu. */
private const val EXIT_WINDOW_MS = 2000L

@Composable
fun HomeScreen(
    ads: AdsManager,
    sound: SoundManager,
    hasSave: Boolean,
    onPlay: (continueSaved: Boolean) -> Unit,
    onDaily: () -> Unit,
    onThemes: () -> Unit,
    onStats: () -> Unit,
    onAdventure: () -> Unit,
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

    var showMissions by remember { mutableStateOf(false) }
    var showChest by remember { mutableStateOf(false) }
    // The daily chest opens by itself the first time the menu appears each day.
    LaunchedEffect(Unit) {
        if (Progress.chestAvailable() && chestAutoShownDay != Progress.today()) {
            chestAutoShownDay = Progress.today()
            showChest = true
        }
    }

    // Back closes an open dialog; on the menu itself it must be pressed twice to leave the game.
    var backPressedAt by remember { mutableLongStateOf(0L) }
    var showExitHint by remember { mutableStateOf(false) }
    BackHandler {
        when {
            showSettings || showHowTo || showMissions || showChest -> {
                showSettings = false
                showHowTo = false
                showMissions = false
                showChest = false
            }
            SystemClock.uptimeMillis() - backPressedAt < EXIT_WINDOW_MS -> (context as? Activity)?.finish()
            else -> {
                backPressedAt = SystemClock.uptimeMillis()
                showExitHint = true
            }
        }
    }
    LaunchedEffect(backPressedAt) {
        if (backPressedAt == 0L) return@LaunchedEffect
        delay(EXIT_WINDOW_MS)
        showExitHint = false
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
                Box {
                    GlassCircleButton(onClick = { showChest = true }) {
                        ChestIcon(Modifier.fillMaxSize(), shaking = Progress.chestAvailable())
                    }
                    if (Progress.chestAvailable()) NotificationDot(Modifier.align(Alignment.TopEnd))
                }
                Spacer(Modifier.width(10.dp))
                GlassCircleButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings), tint = Palette.TextPrimary)
                }
            }

            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val sz = homeSizes(maxWidth, maxHeight)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Logo(sz)
                        if (Progress.best > 0) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CrownIcon(Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.best_x, Progress.best), style = Styles.glow(18, Palette.Gold))
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .widthIn(max = sz.contentWidth),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PlayButton(if (hasSave) stringResource(R.string.home_continue) else stringResource(R.string.home_play), sz.playHeight) {
                            sound.play(Sfx.WHOOSH, 0.7f)
                            onPlay(hasSave)
                        }
                        if (hasSave) {
                            Text(
                                stringResource(R.string.home_new_game),
                                style = Styles.plain(15, Palette.TextSecondary, FontWeight.Bold),
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPlay(false) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        } else {
                            Spacer(Modifier.height(sz.gap))
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdventureCard(Modifier.weight(1f), sz.cardHeight, onAdventure)
                            DailyCard(Modifier.weight(1f), sz.cardHeight, onDaily)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HomeTile(stringResource(R.string.missions), Modifier.weight(1f), sz.tileHeight, Progress.claimableMissions, { showMissions = true }) {
                                MissionIcon(Modifier.size(24.dp))
                            }
                            HomeTile(stringResource(R.string.themes), Modifier.weight(1f), sz.tileHeight, 0, onThemes) {
                                PaletteIcon(Modifier.size(24.dp))
                            }
                            HomeTile(stringResource(R.string.tile_stats), Modifier.weight(1f), sz.tileHeight, 0, onStats) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Palette.Gold, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
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
                onHowTo = {
                    showSettings = false
                    showHowTo = true
                },
                onClose = { showSettings = false },
            )
        }
        if (showHowTo) HowToDialog { showHowTo = false }
        if (showMissions) MissionsDialog(sound) { showMissions = false }
        if (showChest) ChestDialog(ads, sound) { showChest = false }

        AnimatedVisibility(
            visible = showExitHint,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = 96.dp, start = 24.dp, end = 24.dp),
        ) {
            Text(
                stringResource(R.string.press_back_exit),
                style = Styles.plain(15, Color.White, FontWeight.Bold),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xE6120B2E))
                    .border(1.dp, Palette.GlassBorder, RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

/** Day the chest was last opened automatically, so it pops up only once per day. */
private var chestAutoShownDay = 0

/** Prevents the automatic chest popup today (used by debug screenshots). */
fun skipChestToday() {
    chestAutoShownDay = Progress.today()
}

/** Sizes for the menu: compact on short phones, larger on tablets. */
private class HomeSizes(
    val logo: Dp,
    val title: Int,
    val subtitle: Int,
    val playHeight: Dp,
    val cardHeight: Dp,
    val tileHeight: Dp,
    val contentWidth: Dp,
    val gap: Dp,
)

private fun homeSizes(width: Dp, height: Dp): HomeSizes = when {
    width >= 600.dp && height >= 700.dp -> HomeSizes(128.dp, 64, 28, 80.dp, 140.dp, 88.dp, 560.dp, 16.dp)
    height < 560.dp -> HomeSizes(56.dp, 34, 16, 56.dp, 100.dp, 62.dp, 420.dp, 4.dp)
    height < 640.dp -> HomeSizes(72.dp, 40, 19, 60.dp, 108.dp, 66.dp, 420.dp, 8.dp)
    else -> HomeSizes(96.dp, 48, 22, 68.dp, 118.dp, 72.dp, 420.dp, 16.dp)
}

@Composable
private fun NotificationDot(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(Palette.Danger)
            .border(1.5.dp, Color.White, CircleShape),
    )
}

@Composable
private fun MissionIcon(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFF34D399), Offset(w * 0.15f, h * 0.08f), androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.86f), androidx.compose.ui.geometry.CornerRadius(w * 0.12f))
        val line = Color.White.copy(alpha = 0.9f)
        for (i in 0 until 3) {
            val y = h * (0.3f + i * 0.22f)
            drawCircle(line, w * 0.05f, Offset(w * 0.32f, y))
            drawLine(line, Offset(w * 0.42f, y), Offset(w * 0.72f, y), w * 0.08f)
        }
    }
}

@Composable
private fun Logo(sz: HomeSizes) {
    val transition = rememberInfiniteTransition(label = "logo")
    val t by transition.animateFloat(
        0f, 6.2832f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "bob",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .size(sz.logo)
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
        Spacer(Modifier.height(10.dp))
        Text("PRISMA", style = Styles.gradient(sz.title))
        Text("BLOCKS", style = Styles.glow(sz.subtitle, Color.White, FontWeight.Black).copy(letterSpacing = (sz.subtitle * 0.4f).sp))
    }
}

@Composable
private fun PlayButton(text: String, height: Dp, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "play")
    val pulse by transition.animateFloat(
        1f, 1.04f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse",
    )
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
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
private fun ModeCard(
    modifier: Modifier,
    height: Dp,
    title: String,
    subtitle: String,
    colors: List<Color>,
    enabled: Boolean = true,
    badge: String? = null,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(Brush.linearGradient(colors.map { it.copy(alpha = 0.28f) }))
            .border(1.5.dp, Brush.linearGradient(colors), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Column {
                FitText(title, Styles.plain(17, Palette.TextPrimary, FontWeight.Black), minSp = 11f)
                Text(subtitle, style = Styles.plain(13, Palette.TextSecondary, FontWeight.SemiBold), maxLines = 2)
            }
        }
        if (badge != null) {
            Text(
                badge,
                style = Styles.glow(16, Color(0xFFFB923C), FontWeight.Black),
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun AdventureCard(modifier: Modifier, height: Dp, onClick: () -> Unit) {
    ModeCard(
        modifier = modifier,
        height = height,
        title = stringResource(R.string.adventure),
        subtitle = stringResource(R.string.level_x, Progress.adventureLevel),
        colors = listOf(Color(0xFFE879F9), Color(0xFF8B5CF6)),
        badge = if (Progress.totalStars > 0) "★ ${Progress.totalStars}" else null,
        onClick = onClick,
    ) {
        Canvas(Modifier.size(28.dp)) { drawGemIcon(center, size.width) }
    }
}

@Composable
private fun DailyCard(modifier: Modifier, height: Dp, onDaily: () -> Unit) {
    val done = Progress.dailyDoneToday()
    val streak = Progress.currentStreak()
    val target = GameViewModel.dailyTargetFor(Progress.today())
    ModeCard(
        modifier = modifier,
        height = height,
        title = stringResource(R.string.daily_title),
        subtitle = if (done) stringResource(R.string.daily_done) else stringResource(R.string.daily_goal, target),
        colors = listOf(Palette.Cyan, Color(0xFF34D399)),
        enabled = !done,
        badge = if (streak > 0) "🔥 $streak" else null,
        onClick = onDaily,
    ) {
        Icon(
            if (done) Icons.Default.Check else Icons.Default.Star,
            contentDescription = null,
            tint = if (done) Color(0xFF34D399) else Palette.Gold,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun HomeTile(
    label: String,
    modifier: Modifier,
    height: Dp,
    badge: Int,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .background(Palette.Glass)
                .border(1.dp, if (badge > 0) Color(0xFF34D399) else Palette.GlassBorder, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            icon()
            Spacer(Modifier.height(6.dp))
            FitText(label, Styles.plain(13, Palette.TextPrimary, FontWeight.Bold), minSp = 9f)
        }
        if (badge > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Palette.Danger),
                contentAlignment = Alignment.Center,
            ) { Text(badge.toString(), style = Styles.plain(12, Color.White, FontWeight.Black)) }
        }
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
private fun SettingsDialog(ads: AdsManager, onToggleReminder: () -> Unit, onHowTo: () -> Unit, onClose: () -> Unit) {
    DimOverlay {
        GlassPanel {
            Text(stringResource(R.string.settings), style = Styles.gradient(30))
            Spacer(Modifier.height(4.dp))
            ToggleRow(stringResource(R.string.music), Progress.musicOn) {
                Progress.toggleMusic()
                com.farias.prismablocks.audio.MusicPlayer.update()
            }
            ToggleRow(stringResource(R.string.sound), Progress.soundOn) { Progress.toggleSound() }
            val ctx = LocalContext.current
            ToggleRow(stringResource(R.string.vibration), Progress.vibrationOn) {
                Progress.toggleVibration()
                // A short buzz confirms that vibration is now on.
                if (Progress.vibrationOn) com.farias.prismablocks.audio.Haptics.play(ctx, com.farias.prismablocks.game.Haptic.CLEAR)
            }
            ToggleRow(stringResource(R.string.notifications), Progress.notificationsOn) { onToggleReminder() }
            if (ads.privacyOptionsRequired) PrivacyLink(ads)
            val uri = LocalUriHandler.current
            Text(
                text = stringResource(R.string.privacy_policy),
                style = Styles.plain(14, Palette.TextSecondary),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { uri.openUri(PRIVACY_POLICY_URL) }
                    .padding(8.dp),
            )
            SecondaryButton(stringResource(R.string.how_to)) { onHowTo() }
            PrimaryButton(stringResource(R.string.got_it)) { onClose() }
        }
    }
}
