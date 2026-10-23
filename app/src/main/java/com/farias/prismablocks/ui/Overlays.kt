package com.farias.prismablocks.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.farias.prismablocks.R
import com.farias.prismablocks.ads.AdsManager
import com.farias.prismablocks.data.Progress
import com.farias.prismablocks.game.GameViewModel
import com.farias.prismablocks.game.Loss
import com.farias.prismablocks.game.Mode
import com.farias.prismablocks.game.Power
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun PauseOverlay(vm: GameViewModel, ads: AdsManager, onHome: () -> Unit) {
    DimOverlay {
        GlassPanel {
            Text(stringResource(R.string.paused), style = Styles.gradient(34))
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
            Spacer(Modifier.height(8.dp))
            PrimaryButton(stringResource(R.string.resume)) { vm.paused = false }
            SecondaryButton(stringResource(R.string.restart)) {
                vm.paused = false
                ads.onNewGame { vm.restart() }
            }
            SecondaryButton(stringResource(R.string.home)) {
                vm.paused = false
                vm.save()
                onHome()
            }
            if (ads.privacyOptionsRequired) PrivacyLink(ads)
        }
    }
}

@Composable
fun PrivacyLink(ads: AdsManager) {
    Text(
        text = stringResource(R.string.privacy_options),
        style = Styles.plain(14, Palette.TextSecondary),
        modifier = Modifier
            .padding(top = 4.dp)
            .clickable { ads.showPrivacyOptions() }
            .padding(8.dp),
    )
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Styles.plain(17), modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF8B5CF6),
                uncheckedThumbColor = Palette.TextSecondary,
                uncheckedTrackColor = Color(0x33FFFFFF),
                uncheckedBorderColor = Color(0x55FFFFFF),
            ),
        )
    }
}

@Composable
private fun CoinsEarned(amount: Int) {
    if (amount <= 0) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        CoinIcon(Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.coins_earned, amount), style = Styles.glow(19, Palette.Gold, FontWeight.Black))
    }
}

@Composable
fun GameOverOverlay(vm: GameViewModel, ads: AdsManager, onHome: () -> Unit) {
    val daily = vm.mode == Mode.DAILY
    val lv = vm.level
    val adventure = vm.mode == Mode.ADVENTURE && lv != null
    DimOverlay {
        GlassPanel {
            when {
                adventure && lv != null -> {
                    Text(stringResource(R.string.level_x, lv.number), style = Styles.plain(16, Palette.TextSecondary, FontWeight.Bold))
                    when (vm.loss) {
                        Loss.NO_MOVES -> Text(stringResource(R.string.out_of_moves), style = Styles.glow(20, Color(0xFFFB923C), FontWeight.Black))
                        Loss.EXPLODED -> Text(stringResource(R.string.bomb_exploded), style = Styles.glow(20, Palette.Danger, FontWeight.Black), textAlign = TextAlign.Center)
                        Loss.NO_SPACE -> Unit
                    }
                    val missing = lv.objectives.firstOrNull { vm.progress(it.goal) < it.target } ?: lv.objectives[0]
                    Text(
                        missingText(missing, vm.progress(missing.goal)),
                        style = Styles.plain(24, Palette.TextPrimary, FontWeight.Black),
                        textAlign = TextAlign.Center,
                    )
                }
                daily -> Text(
                    stringResource(R.string.daily_fail, (vm.dailyTarget - vm.lines).coerceAtLeast(1)),
                    style = Styles.plain(24, Palette.TextPrimary, FontWeight.Black),
                    textAlign = TextAlign.Center,
                )
                vm.newRecord -> Text(stringResource(R.string.new_record), style = Styles.glow(28, Palette.Gold, FontWeight.Black))
                else -> Text(stringResource(R.string.no_space), style = Styles.plain(24, Palette.TextPrimary, FontWeight.Black))
            }
            Text(stringResource(R.string.score), style = Styles.plain(14, Palette.TextSecondary))
            Text(vm.score.toString(), style = Styles.gradient(60))
            if (adventure) {
                Spacer(Modifier.height(0.dp))
            } else if (daily) {
                Text(
                    stringResource(R.string.lines_progress, vm.lines, vm.dailyTarget),
                    style = Styles.plain(17, Palette.Cyan, FontWeight.Bold),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CrownIcon(Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.best_x, Progress.best), style = Styles.glow(17, Palette.Gold))
                }
                CoinsEarned(vm.coinsEarned)
                val missing = Progress.best - vm.score
                if (!vm.newRecord && missing > 0 && vm.score >= Progress.best * 0.6f) {
                    Text(
                        stringResource(R.string.near_record, missing),
                        style = Styles.glow(17, Color(0xFFFB923C), FontWeight.Black),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (vm.canRevive && ads.adsReady) {
                val label = if (vm.loss == Loss.NO_MOVES) R.string.continue_moves else R.string.continue_video
                PrimaryButton(
                    text = stringResource(if (ads.rewardedReady) label else R.string.loading_video),
                    enabled = ads.rewardedReady,
                ) {
                    ads.showRewarded { vm.revive() }
                }
            }
            SecondaryButton(stringResource(if (daily || adventure) R.string.try_again else R.string.play_again)) {
                ads.onNewGame { vm.restart() }
            }
            SecondaryButton(stringResource(if (adventure) R.string.map else R.string.home)) { onHome() }
        }
    }
}

@Composable
fun DailyWinOverlay(vm: GameViewModel, onHome: () -> Unit) {
    DimOverlay {
        GlassPanel {
            Text(
                stringResource(R.string.daily_win),
                style = Styles.glow(28, Palette.Gold, FontWeight.Black),
                textAlign = TextAlign.Center,
            )
            Text(stringResource(R.string.streak_days, vm.dailyStreak), style = Styles.glow(22, Color(0xFFFB923C), FontWeight.Black))
            Text(vm.score.toString(), style = Styles.gradient(52))
            CoinsEarned(vm.coinsEarned)
            Spacer(Modifier.height(8.dp))
            PrimaryButton(stringResource(R.string.home)) { onHome() }
        }
    }
}

@Composable
fun PayWithVideoDialog(power: Power, ads: AdsManager, onWatch: () -> Unit, onCancel: () -> Unit) {
    DimOverlay {
        GlassPanel {
            Text(stringResource(R.string.not_enough_title), style = Styles.plain(22, Palette.TextPrimary, FontWeight.Black))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.need_coins, (power.cost - Progress.coins).coerceAtLeast(1)),
                    style = Styles.plain(16, Palette.Gold, FontWeight.Bold),
                )
            }
            Spacer(Modifier.height(4.dp))
            if (ads.adsReady && ads.rewardedReady) {
                PrimaryButton(stringResource(R.string.watch_free)) { onWatch() }
            } else {
                Text(
                    stringResource(R.string.no_video),
                    style = Styles.plain(14, Palette.TextSecondary),
                    textAlign = TextAlign.Center,
                )
            }
            SecondaryButton(stringResource(R.string.cancel)) { onCancel() }
        }
    }
}

@Composable
fun HowToDialog(onClose: () -> Unit) {
    DimOverlay {
        GlassPanel {
            Text(stringResource(R.string.how_to), style = Styles.gradient(30))
            val steps = listOf(R.string.how_to_1, R.string.how_to_2, R.string.how_to_3, R.string.how_to_4)
            steps.forEachIndexed { i, res ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
                            drawGem(androidx.compose.ui.geometry.Offset.Zero, size.width, Palette.block(i + 1), glow = false)
                        }
                        Text("${i + 1}", style = Styles.plain(13, Color.White, FontWeight.Black))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(res), style = Styles.plain(16, Palette.TextPrimary, FontWeight.Medium))
                }
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton(stringResource(R.string.got_it)) { onClose() }
        }
    }
}

/** Adaptive banner at the bottom. Space is reserved up front so the layout doesn't jump. */
@Composable
fun BannerAd(ads: AdsManager) {
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt()
        @Suppress("DEPRECATION")
        val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(adSize.height.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (ads.adsReady) {
                AndroidView(
                    factory = { ctx ->
                        AdView(ctx).apply {
                            setAdSize(adSize)
                            adUnitId = ads.bannerId
                            loadAd(AdRequest.Builder().build())
                        }
                    },
                    onRelease = { it.destroy() },
                )
            }
        }
    }
}

@Composable
fun AdventureWinOverlay(vm: GameViewModel, ads: AdsManager, onMap: () -> Unit) {
    val lv = vm.level ?: return
    DimOverlay {
        GlassPanel {
            Text(stringResource(R.string.level_x, lv.number), style = Styles.plain(16, Palette.TextSecondary, FontWeight.Bold))
            Text(
                stringResource(if (lv.boss) R.string.boss_defeated else R.string.level_complete),
                style = Styles.glow(28, Palette.Gold, FontWeight.Black),
                textAlign = TextAlign.Center,
            )
            StarsReveal(vm.advStars, vm)
            Text(vm.score.toString(), style = Styles.gradient(44))
            CoinsEarned(vm.coinsEarned)
            Spacer(Modifier.height(8.dp))
            PrimaryButton(stringResource(R.string.next_level)) {
                ads.onNewGame { vm.startAdventure(lv.number + 1) }
            }
            SecondaryButton(stringResource(R.string.map)) { onMap() }
        }
    }
}

/** Three stars that pop in one after another, with a chime for each earned star. */
@Composable
private fun StarsReveal(stars: Int, vm: GameViewModel) {
    val anims = remember { List(3) { androidx.compose.animation.core.Animatable(0f) } }
    androidx.compose.runtime.LaunchedEffect(stars) {
        for (i in 0 until 3) {
            kotlinx.coroutines.delay(260)
            if (i < stars) vm.sound.play(com.farias.prismablocks.audio.Sfx.COIN, 1f, 1f + i * 0.15f)
            anims[i].animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 400f))
        }
    }
    Row(verticalAlignment = Alignment.Bottom) {
        for (i in 0 until 3) {
            val big = i == 1
            StarIcon(
                filled = i < stars,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(if (big) 64.dp else 50.dp)
                    .graphicsLayer {
                        val v = anims[i].value
                        scaleX = v
                        scaleY = v
                        rotationZ = (1f - v) * -40f
                    },
            )
        }
    }
}
