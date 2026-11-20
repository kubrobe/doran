package com.doranalley

import android.Manifest
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.doranalley.audio.AudioPlayer
import com.doranalley.audio.KoreanAsr
import com.doranalley.audio.MicRecorder
import com.doranalley.core.Course
import com.doranalley.core.Hangul
import com.doranalley.core.SpeechResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AudioAndSpeechTest {
    @get:Rule val mic: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private val ctx get() = TestUtil.ctx
    private val course by lazy { Course.parse(ctx.assets.open("course/course.json").bufferedReader().readText()) }

    @Test fun everyBundledClipIsAValidMp3() {
        val files = ctx.assets.list("audio")!!.filter { it.endsWith(".mp3") }
        assertTrue("only ${files.size} clips", files.size > 600)
        var shortest = Long.MAX_VALUE
        for (f in files) {
            val afd = ctx.assets.openFd("audio/$f")
            val ex = MediaExtractor()
            ex.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            assertEquals("$f tracks", 1, ex.trackCount)
            val fmt = ex.getTrackFormat(0)
            assertTrue(f, fmt.getString(MediaFormat.KEY_MIME)!!.startsWith("audio/"))
            val dur = fmt.getLong(MediaFormat.KEY_DURATION)
            assertTrue("$f is ${dur}us", dur > 250_000)
            shortest = minOf(shortest, dur)
            ex.release(); afd.close()
        }
        Log.i("AudioTest", "${files.size} clips ok, shortest ${shortest / 1000} ms")
    }

    @Test fun playerPlaysClipsToTheEnd() {
        val main = Handler(Looper.getMainLooper())
        lateinit var player: AudioPlayer
        val made = CountDownLatch(1)
        main.post { player = AudioPlayer(ctx); made.countDown() }
        made.await(5, TimeUnit.SECONDS)
        for (text in listOf("안녕하세요", "화장실이 어디예요?", "ㅏ".let { "아" })) {
            for (slow in listOf(false, true)) {
                val started = CountDownLatch(1)
                var ok = false
                main.post { ok = player.play(text, slow); started.countDown() }
                started.await(5, TimeUnit.SECONDS)
                assertTrue("play($text) returned false", ok)
                // Wait for playback to finish (playing goes back to null).
                val deadline = System.currentTimeMillis() + 15_000
                Thread.sleep(300)
                while (player.playing.value != null && System.currentTimeMillis() < deadline) Thread.sleep(100)
                assertEquals("still playing $text", null, player.playing.value)
            }
        }
        main.post { player.release() }
    }

    @Test fun recognizerUnderstandsTheBundledAudio() = runBlocking {
        val asr = KoreanAsr(ctx)
        val sentences = course.units.flatMap { it.sentences }.filter { it.speak }
        val sample = sentences.filterIndexed { i, _ -> i % 8 == 0 }
        var passed = 0
        var total = 0.0
        val t0 = System.currentTimeMillis()
        for (s in sample) {
            val pcm = TestUtil.decodeAsset("audio/${Hangul.audioKey(s.ko)}.mp3")
            val heard = withTimeout(60_000) { asr.recognize(pcm) }
            requireNotNull(heard) { "recognizer failed: ${asr.state.value}" }
            val r = SpeechResult.of(s.ko, heard)
            Log.i("AsrTest", "${r.score}% '${s.ko}' -> '$heard'")
            if (r.passed) passed++
            total += r.score
            // The same audio must not pass for an unrelated sentence.
            val other = sentences[(sentences.indexOf(s) + sentences.size / 2) % sentences.size]
            assertTrue("'$heard' passed for '${other.ko}'", !SpeechResult.of(other.ko, heard).passed || Hangul.similarity(other.ko, s.ko) > 0.6)
        }
        Log.i("AsrTest", "passed $passed/${sample.size}, mean ${total / sample.size}, ${System.currentTimeMillis() - t0} ms")
        assertTrue(asr.state.value is KoreanAsr.State.Ready)
        assertTrue("only $passed of ${sample.size} passed", passed >= sample.size * 0.85)
    }

    @Test fun recorderNeverCrashes() = runBlocking {
        val rec = MicRecorder(ctx)
        val r = withTimeout(20_000) { rec.record(maxSeconds = 1.5f) }
        Log.i("RecorderTest", "result: $r")
        when (r) {
            is MicRecorder.Result.Ok -> {
                assertTrue(r.file.exists() && r.file.length() >= 44)
                assertTrue(r.seconds in 1.0f..2.0f)
            }
            is MicRecorder.Result.Failed -> assertTrue(r.reason.isNotBlank())
        }
    }
}
