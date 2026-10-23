package com.farias.prismablocks.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.farias.prismablocks.R

enum class Sfx { PICK, PLACE, CLEAR, COMBO, INVALID, GAME_OVER, RECORD, REVIVE }

/** Short sound effects through SoundPool (low latency, game audio attributes). */
class SoundManager(context: Context) {

    var enabled: Boolean = true

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids: Map<Sfx, Int> = mapOf(
        Sfx.PICK to pool.load(context, R.raw.sfx_pick, 1),
        Sfx.PLACE to pool.load(context, R.raw.sfx_place, 1),
        Sfx.CLEAR to pool.load(context, R.raw.sfx_clear, 1),
        Sfx.COMBO to pool.load(context, R.raw.sfx_combo, 1),
        Sfx.INVALID to pool.load(context, R.raw.sfx_invalid, 1),
        Sfx.GAME_OVER to pool.load(context, R.raw.sfx_gameover, 1),
        Sfx.RECORD to pool.load(context, R.raw.sfx_record, 1),
        Sfx.REVIVE to pool.load(context, R.raw.sfx_revive, 1),
    )

    fun play(sfx: Sfx, volume: Float = 1f, rate: Float = 1f) {
        if (!enabled) return
        val id = ids[sfx] ?: return
        pool.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun release() = pool.release()
}
