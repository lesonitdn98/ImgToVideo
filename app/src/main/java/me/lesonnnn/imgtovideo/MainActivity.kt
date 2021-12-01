package me.lesonnnn.imgtovideo

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private var selectedCodec: String = "mpeg4"
    private var videoView: VideoView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        videoView = findViewById(R.id.videoPlayerFrame)
        encodeVideo()
    }

    private fun encodeVideo() {
        val image1File = File(this.cacheDir, "colosseum.jpg")
        val image2File = File(this.cacheDir, "pyramid.jpg")
        val image3File = File(this.cacheDir, "tajmahal.jpg")
        val videoFile: File = getVideoFile()
        try {

            if (videoFile.exists()) {
                videoFile.delete()
            }
            val videoCodec: String = selectedCodec
            Log.d("TAG", String.format("Testing VIDEO encoding with '%s' codec", videoCodec))
            ResourcesUtil.resourceToFile(resources, R.drawable.colosseum, image1File)
            ResourcesUtil.resourceToFile(resources, R.drawable.pyramid, image2File)
            ResourcesUtil.resourceToFile(resources, R.drawable.tajmahal, image3File)
            val ffmpegCommand: String = Video.generateEncodeVideoScript(
                image1File.absolutePath,
                image2File.absolutePath,
                image3File.absolutePath,
                videoFile.absolutePath,
                getSelectedVideoCodec(),
                getCustomOptions()
            )
            Log.d(
                "TAG", String.format(
                    "FFmpeg process started with arguments\n'%s'.",
                    ffmpegCommand
                )
            )
            val executionId = FFmpeg.executeAsync(
                ffmpegCommand
            ) { _, returnCode ->
                if (returnCode == Config.RETURN_CODE_SUCCESS) {
                    Log.d("TAG", "Encode completed successfully; playing video.")
                    playVideo()
                } else {
                    Toast.makeText(
                        this,
                        "Encode failed. Please check log for the details.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("TAG", String.format("Encode failed with rc=%d.", returnCode))
                }
            }
            Log.d(
                "TAG",
                String.format("Async FFmpeg process started with executionId %d.", executionId)
            )
        } catch (e: IOException) {
            Log.e(
                "TAG",
                java.lang.String.format(
                    "Encode video failed.",
                )
            )
        }
    }

    private fun getVideoFile(): File {
        val videoCodec: String = selectedCodec
        val extension: String
        extension = when (videoCodec) {
            "vp8", "vp9" -> "webm"
            "aom" -> "mkv"
            "theora" -> "ogv"
            "hap" -> "mov"
            else ->
                // mpeg4, x264, x265, xvid, kvazaar
                "mp4"
        }
        val video = "video.$extension"
        return File(this.cacheDir, video)
    }

    private fun getCustomOptions(): String {
        return when (selectedCodec) {
            "x265" -> "-crf 28 -preset fast "
            "vp8" -> "-b:v 1M -crf 10 "
            "vp9" -> "-b:v 2M "
            "aom" -> "-crf 30 -strict experimental "
            "theora" -> "-qscale:v 7 "
            "hap" -> "-format hap_q "
            else ->
                // kvazaar, mpeg4, x264, xvid
                ""
        }
    }

    private fun getSelectedVideoCodec(): String {

        // NOTE THAT MPEG4 CODEC IS ASSIGNED HERE
        var videoCodec = selectedCodec
        when (videoCodec) {
            "x264" -> videoCodec = "libx264"
            "openh264" -> videoCodec = "libopenh264"
            "x265" -> videoCodec = "libx265"
            "xvid" -> videoCodec = "libxvid"
            "vp8" -> videoCodec = "libvpx"
            "vp9" -> videoCodec = "libvpx-vp9"
            "aom" -> videoCodec = "libaom-av1"
            "kvazaar" -> videoCodec = "libkvazaar"
            "theora" -> videoCodec = "libtheora"
        }
        return videoCodec
    }

    private fun playVideo() {
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView?.let {
            it.setVideoURI(Uri.parse("file://" + getVideoFile().absolutePath))
            it.setMediaController(mediaController)
            it.requestFocus()
            it.setOnErrorListener { _, _, _ ->
                it.stopPlayback()
                false
            }
            it.start()
        }
    }
}