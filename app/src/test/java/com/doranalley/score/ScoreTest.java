package com.doranalley.score;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;
import org.junit.Test;

/**
 * Pronunciation scoring on synthetic "speech": vowel-like harmonic sounds with formants.
 * A different voice (pitch, vocal tract length, tempo) saying the same sequence must score well above
 * a different sequence, and silence or noise must be reported as "no speech".
 */
public class ScoreTest {

    private static final double[][] VOWELS = {
        {800, 1200, 2500}, {300, 2300, 3000}, {320, 800, 2300}, {500, 1900, 2500},
        {450, 800, 2400}, {600, 1000, 2500}, {350, 1400, 2400},
    };

    private static Wav.Audio utter(int[] seq, double f0, double formantScale, double tempo, int sr, double noise, long seed) {
        Random rng = new Random(seed);
        int pause = (int) (0.3 * sr);
        int cons = (int) (0.05 * sr / tempo);
        int vow = (int) (0.16 * sr / tempo);
        float[] x = new float[2 * pause + seq.length * (cons + vow)];
        int p = pause;
        for (int k = 0; k < seq.length; k++) {
            for (int i = 0; i < cons; i++) {
                double w = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (cons - 1));
                x[p++] = (float) (rng.nextGaussian() * 0.15 * w);
            }
            double[] f = VOWELS[seq[k]];
            double pitch = f0 * (1 + 0.1 * Math.sin(k));
            float[] v = new float[vow];
            double max = 1e-9;
            for (int i = 0; i < vow; i++) {
                double t = i / (double) sr, s = 0;
                for (int h = 1; h < (int) (4000 / pitch); h++) {
                    double fh = h * pitch, amp = 0;
                    for (double fm : f) {
                        double c = fm * formantScale, bw = 80 + c * 0.06;
                        amp += 1 / (1 + Math.pow((fh - c) / bw, 2));
                    }
                    s += amp * Math.sin(2 * Math.PI * fh * t) / Math.sqrt(h);
                }
                v[i] = (float) s;
                max = Math.max(max, Math.abs(s));
            }
            for (int i = 0; i < vow; i++) {
                double env = Math.pow(Math.max(0, Math.sin(Math.PI * i / (vow - 1))), 0.3);
                x[p++] = (float) (0.3 * v[i] / max * env);
            }
        }
        for (int i = 0; i < x.length; i++) x[i] += (float) (rng.nextGaussian() * (noise + 1e-4));
        return new Wav.Audio(x, sr);
    }

    private static final int[] A = {0, 6, 3, 1, 5, 1, 4, 3, 0, 2};
    private static final int[] B = {4, 1, 2, 0, 3, 6, 0, 4, 1, 3};

    private final Wav.Audio model = utter(A, 200, 1.12, 1.0, 22050, 0, 1);

    @Test
    public void identicalTakeScoresFull() {
        Acoustic.Result r = Acoustic.compare(utter(A, 200, 1.12, 1.0, 22050, 0, 1), model, 10);
        assertTrue("score " + r.score, r.score >= 95);
        assertEquals(10, r.syllables.length);
    }

    @Test
    public void sameWordsOtherVoiceBeatsOtherWords() {
        Acoustic.Result same = Acoustic.compare(utter(A, 110, 0.92, 0.8, 16000, 0.01, 2), model, 10);
        Acoustic.Result fast = Acoustic.compare(utter(A, 130, 1.0, 1.4, 16000, 0.02, 3), model, 10);
        Acoustic.Result other = Acoustic.compare(utter(B, 110, 0.92, 0.8, 16000, 0.01, 4), model, 10);
        assertTrue("same " + same.score, same.score >= 60);
        assertTrue("fast " + fast.score, fast.score >= 55);
        assertTrue("other " + other.score, other.score <= 30);
        assertTrue(fast.tooFast() || fast.pace < 0.8);
    }

    @Test
    public void halfRightFlagsTheWrongHalf() {
        int[] half = {0, 6, 3, 1, 5, 6, 0, 4, 1, 3};
        Acoustic.Result r = Acoustic.compare(utter(half, 110, 0.92, 0.9, 16000, 0.01, 5), model, 10);
        double first = 0, second = 0;
        for (int i = 0; i < 5; i++) { first += r.syllables[i]; second += r.syllables[i + 5]; }
        assertTrue(Arrays.toString(r.syllables), first > second);
        assertTrue("weakest " + r.weakest(), r.weakest() >= 3); // approximate: never the clearly-right opening
    }

    @Test
    public void silenceAndNoiseAreNoSpeech() {
        Random rng = new Random(9);
        float[] quiet = new float[32000], hiss = new float[32000];
        for (int i = 0; i < quiet.length; i++) { quiet[i] = (float) (rng.nextGaussian() * 0.002); hiss[i] = (float) (rng.nextGaussian() * 0.1); }
        assertTrue(Acoustic.compare(new Wav.Audio(quiet, 16000), model, 10).noSpeech());
        assertTrue(Acoustic.compare(new Wav.Audio(hiss, 16000), model, 10).noSpeech());
    }

    @Test
    public void hangulExactAndClose() {
        assertEquals(100, Hangul.compare("강의실이 바뀌었어요", "강의실이 바뀌었어요").score);
        assertEquals(100, Hangul.compare("안녕하세요", "안녕하세요.").score);
        Hangul.Result close = Hangul.compare("어디로 가면 돼요?", "어디로 가면 되요");
        assertTrue(close.score >= 90);
        assertEquals(1, close.marks[5]); // 돼 heard as 되: close, not missed
        assertTrue(Hangul.compare("강의실이 어디예요?", "감사합니다").score < 40);
        assertEquals(0, Hangul.compare("안녕하세요", "").score);
    }

    @Test
    public void hangulNumbersAndExtraWords() {
        assertEquals(100, Hangul.compare("학생회관 302호예요.", "학생회관 삼백이 호예요").score);
        assertEquals(100, Hangul.compare("302호", "302호").score);
        Hangul.Result inside = Hangul.compare("강의실", "강의실이 어디예요", true);
        assertEquals(100, inside.score);
        assertArrayEquals(new int[] {2, 2, 2}, inside.marks);
        assertTrue(Hangul.compare("강의실", "강의실이 어디예요", false).score < 100);
    }

    @Test
    public void wavRoundTrip() throws Exception {
        java.io.File f = java.io.File.createTempFile("take", ".wav");
        try (java.io.RandomAccessFile out = new java.io.RandomAccessFile(f, "rw")) {
            Wav.writeHeader(out, 16000, 0);
            short[] s = {0, 16384, -16384, 32767};
            byte[] b = new byte[s.length * 2];
            for (int i = 0; i < s.length; i++) { b[2 * i] = (byte) s[i]; b[2 * i + 1] = (byte) (s[i] >> 8); }
            out.write(b);
            Wav.writeHeader(out, 16000, b.length);
        }
        Wav.Audio a = Wav.read(f);
        assertEquals(16000, a.sampleRate);
        assertEquals(4, a.samples.length);
        assertEquals(0.5f, a.samples[1], 1e-4);
        assertEquals(-0.5f, a.samples[2], 1e-4);
        f.delete();
    }
}
