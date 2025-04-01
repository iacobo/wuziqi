package com.iacobo.wuziqi.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.iacobo.wuziqi.R
import com.iacobo.wuziqi.data.GameType

/**
 * Manager for game sound effects. Encapsulates sound loading and playback for different game
 * events.
 */
class SoundManager(context: Context) {
    // SoundPool for softer, more natural sounds
    private val soundPool: SoundPool

    // Game sound effects
    private val soundPlaceTile: Int
    private val soundPlaceTicTacToe: Int
    private val soundPlaceConnect4: Int
    private val soundWin: Int
    private val soundUndo: Int
    private val soundReset: Int

    // Volume constants
    private val defaultVolume = 0.7f

    init {
        // Initialize SoundPool
        val audioAttributes =
                AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()

        soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(audioAttributes).build()

        // Load sound effects
        soundPlaceTile = soundPool.load(context, R.raw.soft_tap, 1)
        soundPlaceTicTacToe = soundPool.load(context, R.raw.soft_scratch, 1)
        soundPlaceConnect4 = soundPool.load(context, R.raw.soft_drop, 1)
        soundWin = soundPool.load(context, R.raw.soft_success, 1)
        soundUndo = soundPool.load(context, R.raw.soft_pop, 1)
        soundReset = soundPool.load(context, R.raw.soft_click, 1)
    }

    /** Plays the appropriate sound for placing a game piece based on game type. */
    fun playTileSound(gameType: GameType) {
        val soundId =
                when (gameType) {
                    GameType.TicTacToe -> soundPlaceTicTacToe
                    GameType.Connect4 -> soundPlaceConnect4
                    else -> soundPlaceTile
                }

        soundPool.play(soundId, defaultVolume, defaultVolume, 1, 0, 1.0f)
    }

    /** Plays the win sound effect. */
    fun playWinSound() {
        soundPool.play(soundWin, defaultVolume, defaultVolume, 1, 0, 1.0f)
    }

    /** Plays the undo sound effect. */
    fun playUndoSound() {
        soundPool.play(soundUndo, defaultVolume, defaultVolume, 1, 0, 1.0f)
    }

    /** Plays the reset sound effect. */
    fun playResetSound() {
        soundPool.play(soundReset, defaultVolume, defaultVolume, 1, 0, 1.0f)
    }

    /**
     * Releases all sound resources. Should be called when the sound manager is no longer needed.
     */
    fun release() {
        soundPool.release()
    }
}
