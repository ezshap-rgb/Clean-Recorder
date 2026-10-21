package com.cleanrecorder.app.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * Lossless, sub-second audio trimmer using native Android MediaExtractor + MediaMuxer.
 * Directly copies encoded audio frames between inPointUs and outPointUs without re-encoding!
 */
object AudioTrimmer {

    data class TrimResult(
        val outputFile: File,
        val durationMs: Long,
        val sizeBytes: Long
    )

    fun trimLossless(
        sourceFile: File,
        destinationFile: File,
        inPointMs: Long,
        outPointMs: Long
    ): TrimResult {
        val inPointUs = inPointMs * 1000L
        val outPointUs = outPointMs * 1000L

        val extractor = MediaExtractor()
        extractor.setDataSource(sourceFile.absolutePath)

        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                audioFormat = format
                break
            }
        }

        require(audioTrackIndex >= 0 && audioFormat != null) { "No audio track found in file" }

        extractor.selectTrack(audioTrackIndex)
        extractor.seekTo(inPointUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        if (destinationFile.exists()) destinationFile.delete()

        val muxer = MediaMuxer(destinationFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val muxerTrackIndex = muxer.addTrack(audioFormat)
        muxer.start()

        val maxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            256 * 1024
        }
        val buffer = ByteBuffer.allocateDirect(maxBufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        var firstSampleUs = -1L

        try {
            while (true) {
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > outPointUs) break

                if (sampleTime >= inPointUs) {
                    if (firstSampleUs < 0) firstSampleUs = sampleTime

                    bufferInfo.presentationTimeUs = sampleTime - firstSampleUs
                    bufferInfo.flags = extractor.sampleFlags
                    bufferInfo.offset = 0

                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                }

                extractor.advance()
            }
        } finally {
            try { muxer.stop() } catch (_: Exception) {}
            muxer.release()
            extractor.release()
        }

        return TrimResult(
            outputFile = destinationFile,
            durationMs = outPointMs - inPointMs,
            sizeBytes = destinationFile.length()
        )
    }
}
