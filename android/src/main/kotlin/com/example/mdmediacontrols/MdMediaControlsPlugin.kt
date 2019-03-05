package com.example.mdmediacontrols

import android.content.Context
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import java.io.IOException
import android.app.NotificationChannel
import android.app.NotificationManager

class MdMediaControlsPlugin(Channel: MethodChannel, Registrar: Registrar) : MethodCallHandler {
    private var mediaPlayer = MediaPlayer()
    private val registrar: Registrar = Registrar
    private val channel: MethodChannel = Channel
    private val am: AudioManager
    private var isOnPlay = false

    init {
        this.channel.setMethodCallHandler(this)
        val context = this.registrar.context().applicationContext
        this.am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "md_media_controls")
            channel.setMethodCallHandler(MdMediaControlsPlugin(channel, registrar))
        }

    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "play" -> {
                val args = call.arguments as HashMap<*, *>
                val url = args.get("url") as String
                val rate = args.get("rate") as Double
                this.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                if (this.isOnPlay) {
                    this.mediaPlayer.release();
                    this.mediaPlayer = MediaPlayer()
                }
                try {
                    this.mediaPlayer.setDataSource(url)
                } catch (error: IOException) {
                    Log.w("Play", "Invalid data source", error)
                    this.channel.invokeMethod("error", "play error")
                    return result.error("Playing error", "Invalid data source", null)
                }
                this.mediaPlayer.prepareAsync()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    this.mediaPlayer.playbackParams = this.mediaPlayer.playbackParams.setSpeed(rate.toFloat())
                }

                this.mediaPlayer.setOnPreparedListener {
                    it.start()
                    this.channel.invokeMethod("audio.play", null)
                    this.channel.invokeMethod("audio.duration", this.mediaPlayer.duration)
                }

                this.mediaPlayer.setOnCompletionListener {
                    this.channel.invokeMethod("audio.stop", null)
                }

                this.mediaPlayer.setOnErrorListener { _, _, _ ->
                    channel.invokeMethod("error", "start play error")
                    true
                }
                this.isOnPlay = true
                return result.success(true)
            }
            "pause" -> {
                if (this.mediaPlayer.isPlaying) {
                    this.mediaPlayer.pause()
                    this.channel.invokeMethod("audio.pause", null)
                }
                return result.success(true)
            }
            "playPrev" -> {
                this.mediaPlayer.start()
                this.channel.invokeMethod("audio.play", null)
                return result.success(true)
            }
            "seek" -> {
                val args = call.arguments as HashMap<*, *>
                val position = args.get("position") as Double
                this.mediaPlayer.seekTo(position.toInt() * 1000)
                return result.success(true)
            }
            "stop" -> {
                this.mediaPlayer.release()
                this.channel.invokeMethod("audio.stop", null)
                this.isOnPlay = false
                return result.success(true)
            }
            "rate" -> {
                val args = call.arguments as HashMap<*, *>
                val rate = args.get("rate") as Double
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    this.mediaPlayer.playbackParams = this.mediaPlayer.playbackParams.setSpeed(rate.toFloat())
                }
            }
            "infoControls" -> {
                val args = call.arguments as HashMap<*, *>
            }
            "info" -> {
                val args = call.arguments as HashMap<*, *>

            }
            "clearInfo" -> {

            }
            else -> {
                result.notImplemented()
            }
        }
    }
}
