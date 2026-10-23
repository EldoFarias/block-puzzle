package com.farias.prismablocks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.farias.prismablocks.R
import com.farias.prismablocks.ads.AdsManager
import com.farias.prismablocks.game.GameViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
private fun Dim(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC05040F))
            .blockTouches(),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .padding(24.dp)
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xF2251B52), Color(0xF2110B2A))))
            .border(1.dp, Palette.GlassBorder, shape)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun PauseOverlay(vm: GameViewModel, ads: AdsManager) {
    Dim {
        GlassCard {
            Text(stringResource(R.string.paused), style = Styles.gradient(34))
            Spacer(Modifier.height(4.dp))
            ToggleRow(stringResource(R.string.sound), vm.soundOn) { vm.toggleSound() }
            ToggleRow(stringResource(R.string.vibration), vm.vibrationOn) { vm.toggleVibration() }
            Spacer(Modifier.height(8.dp))
            PrimaryButton(stringResource(R.string.resume)) { vm.paused = false }
            SecondaryButton(stringResource(R.string.restart)) {
                vm.paused = false
                ads.onNewGame { vm.restart() }
            }
            if (ads.privacyOptionsRequired) {
                Text(
                    text = stringResource(R.string.privacy_options),
                    style = Styles.plain(14, Palette.TextSecondary),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { ads.showPrivacyOptions() }
                        .padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
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
fun GameOverOverlay(vm: GameViewModel, ads: AdsManager) {
    Dim {
        GlassCard {
            Text(
                text = stringResource(if (vm.newRecord) R.string.new_record else R.string.no_space),
                style = if (vm.newRecord) Styles.glow(28, Palette.Gold, FontWeight.Black) else Styles.plain(24, Palette.TextPrimary, FontWeight.Black),
                textAlign = TextAlign.Center,
            )
            Text(stringResource(R.string.score), style = Styles.plain(14, Palette.TextSecondary))
            Text(vm.score.toString(), style = Styles.gradient(60))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CrownIcon(Modifier.height(18.dp).widthIn(min = 22.dp, max = 22.dp))
                Spacer(Modifier.padding(4.dp))
                Text(stringResource(R.string.best_x, vm.best), style = Styles.glow(17, Palette.Gold))
            }
            Spacer(Modifier.height(8.dp))
            if (vm.canRevive && ads.adsReady) {
                PrimaryButton(
                    text = stringResource(if (ads.rewardedReady) R.string.continue_video else R.string.loading_video),
                    enabled = ads.rewardedReady,
                ) {
                    ads.showRewarded { vm.revive() }
                }
            }
            SecondaryButton(stringResource(R.string.play_again)) {
                ads.onNewGame { vm.restart() }
            }
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
