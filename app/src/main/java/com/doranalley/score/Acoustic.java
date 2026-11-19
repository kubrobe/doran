package com.doranalley.score;

/**
 * Offline "sound match": how closely the learner's take follows the model recording.
 *
 * Both recordings are trimmed to the speech, turned into normalized MFCC frames (so a different
 * voice, microphone or loudness matters less), and aligned with dynamic time warping. The alignment
 * cost is compared with the cost of aligning against the model played backwards (same sounds, wrong
 * order), which makes the score self-calibrating: saying a different phrase scores near 0, a close
 * imitation scores high. Korean is syllable-timed, so the model's speech is split evenly into its
 * syllables to show roughly which syllable drifted most.
 *
 * This compares overall sound shape and timing. It is not a phonetic judgment of individual sounds.
 * Plain Java (no Android types) so it can be unit-tested on the JVM.
 */
public final class Acoustic {

    private Acoustic() {}

    static final int SR = 16000;
    private static final double[] WARPS = {0.80, 0.85, 0.90, 0.95, 1.0, 1.05, 1.10, 1.16, 1.22};
    private static final int WIN = 400;   // 25 ms
    private static final int HOP = 160;   // 10 ms
    private static final int NFFT = 512;
    private static final int NMEL = 26;
    private static final int NCEP = 13;

    public static final class Result {
        /** 0..100; -1 when the take had no usable speech. */
        public final int score;
        /** Take speech length / model speech length. */
        public final double pace;
        /** Loudness of the take's speech in dBFS. */
        public final double levelDb;
        /** Seconds of speech found in the take. */
        public final double speechSeconds;
        /** Per model syllable closeness 0..1 (approximate, equal syllable split). */
        public final double[] syllables;

        Result(int score, double pace, double levelDb, double speechSeconds, double[] syllables) {
            this.score = score;
            this.pace = pace;
            this.levelDb = levelDb;
            this.speechSeconds = speechSeconds;
            this.syllables = syllables;
        }

        public boolean noSpeech() { return score < 0; }
        public boolean tooQuiet() { return levelDb < -42; }
        public boolean tooSlow() { return pace > 1.7; }
        public boolean tooFast() { return pace < 0.6; }

        /** Index of the syllable that matched least, or -1. */
        public int weakest() {
            if (syllables == null || syllables.length < 2) return -1;
            int w = 0;
            for (int i = 1; i < syllables.length; i++) if (syllables[i] < syllables[w]) w = i;
            return syllables[w] < 0.6 ? w : -1;
        }
    }

    /**
     * @param syllableCount number of Hangul syllables in the model phrase (for the per-syllable view), 0 to skip
     */
    public static Result compare(Wav.Audio take, Wav.Audio model, int syllableCount) {
        float[] u = resample(take.samples, take.sampleRate);
        float[] r = resample(model.samples, model.sampleRate);
        Speech su = trim(u);
        Speech sr = trim(r);
        if (su == null || su.seconds() < 0.25) {
            return new Result(-1, 0, su == null ? -90 : su.levelDb, su == null ? 0 : su.seconds(), null);
        }
        if (sr == null) return new Result(-1, 0, su.levelDb, su.seconds(), null);

        float[][] fr = features(sr.samples, 1.0);
        if (fr.length < 5) return new Result(-1, 0, su.levelDb, su.seconds(), null);
        // Cap very long takes so alignment stays fast on a phone.
        float[] us = su.samples.length > SR * 12 ? java.util.Arrays.copyOf(su.samples, SR * 12) : su.samples;

        // Vocal tract length normalization: a man imitating a woman's model (or the reverse) has all
        // resonances shifted. Try a few frequency warps of the take and keep the closest.
        Path match = null;
        float[][] fu = null;
        for (double alpha : WARPS) {
            float[][] f = features(us, alpha);
            if (f.length < 5) return new Result(-1, 0, su.levelDb, su.seconds(), null);
            Path p = dtw(f, fr);
            if (match == null || p.cost < match.cost) { match = p; fu = f; }
        }
        Path base = dtw(fu, reversed(fr));
        double ratio = match.cost / Math.max(1e-6, base.cost);
        int score = (int) Math.round(100 * map(ratio));

        double[] syl = null;
        if (syllableCount > 0) {
            int n = Math.min(syllableCount, Math.max(1, fr.length / 4));
            double[] sum = new double[n];
            int[] cnt = new int[n];
            for (int k = 0; k < match.i.length; k++) {
                int j = match.j[k];
                int s = Math.min(n - 1, j * n / fr.length);
                sum[s] += match.local[k];
                cnt[s]++;
            }
            syl = new double[n];
            double baseFrame = base.cost; // mean per-step cost of a non-match
            for (int s = 0; s < n; s++) {
                double d = cnt[s] == 0 ? baseFrame : sum[s] / cnt[s];
                syl[s] = map(d / Math.max(1e-6, baseFrame));
            }
        }
        return new Result(Math.max(0, Math.min(100, score)), su.seconds() / sr.seconds(), su.levelDb, su.seconds(), syl);
    }

    /** Ratio of match cost to "wrong order" cost → 0..1. Calibrated on varied voices/tempos (see AcousticTest). */
    static double map(double ratio) {
        final double good = 0.55, bad = 0.92;
        double x = (bad - ratio) / (bad - good);
        if (x < 0) x = 0;
        if (x > 1) x = 1;
        // Gentle curve so near-misses still read as progress.
        return Math.sqrt(x);
    }

    // ------------------------------------------------------------------ speech trimming

    static final class Speech {
        final float[] samples;
        final double levelDb;

        Speech(float[] s, double levelDb) { this.samples = s; this.levelDb = levelDb; }

        double seconds() { return samples.length / (double) SR; }
    }

    /** Keeps the span between the first and last loud-enough frame; null if there is nothing above the noise floor. */
    static Speech trim(float[] x) {
        int frames = x.length < WIN ? 0 : 1 + (x.length - WIN) / HOP;
        if (frames < 3) return null;
        double[] db = new double[frames];
        double peak = -200;
        for (int f = 0; f < frames; f++) {
            double e = 0;
            for (int i = 0; i < WIN; i++) { double v = x[f * HOP + i]; e += v * v; }
            db[f] = 10 * Math.log10(e / WIN + 1e-12);
            peak = Math.max(peak, db[f]);
        }
        double[] sorted = db.clone();
        java.util.Arrays.sort(sorted);
        double floor = sorted[(int) (frames * 0.1)];
        if (peak < -60 || peak - floor < 10) return null; // silence, or noise without a voice on top
        double thr = Math.max(peak - 30, floor + 0.35 * (peak - floor));
        int a = -1, b = -1;
        double speechE = 0;
        int speechN = 0;
        for (int f = 0; f < frames; f++) {
            if (db[f] >= thr) {
                if (a < 0) a = f;
                b = f;
                speechE += Math.pow(10, db[f] / 10);
                speechN++;
            }
        }
        if (a < 0) return null;
        a = Math.max(0, a - 3);
        b = Math.min(frames - 1, b + 3);
        int start = a * HOP, end = Math.min(x.length, b * HOP + WIN);
        float[] out = new float[end - start];
        System.arraycopy(x, start, out, 0, out.length);
        return new Speech(out, 10 * Math.log10(speechE / Math.max(1, speechN) + 1e-12));
    }

    // ------------------------------------------------------------------ features

    /** MFCC c1..c12 plus deltas, each dimension mean/variance normalized over the utterance. */
    static float[][] features(float[] x, double warp) {
        int frames = x.length < WIN ? 0 : 1 + (x.length - WIN) / HOP;
        float[][] mel = melBank(warp);
        double[] window = new double[WIN];
        for (int i = 0; i < WIN; i++) window[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (WIN - 1));
        float[][] cep = new float[frames][NCEP - 1];
        double[] re = new double[NFFT], im = new double[NFFT];
        double[] logMel = new double[NMEL];
        for (int f = 0; f < frames; f++) {
            java.util.Arrays.fill(re, 0);
            java.util.Arrays.fill(im, 0);
            int o = f * HOP;
            for (int i = 0; i < WIN; i++) {
                double prev = (o + i - 1) >= 0 ? x[o + i - 1] : 0;
                re[i] = (x[o + i] - 0.97 * prev) * window[i];
            }
            fft(re, im);
            for (int m = 0; m < NMEL; m++) {
                double s = 0;
                for (int k = 0; k <= NFFT / 2; k++) {
                    float w = mel[m][k];
                    if (w != 0f) s += w * (re[k] * re[k] + im[k] * im[k]);
                }
                logMel[m] = Math.log(s + 1e-10);
            }
            for (int c = 1; c < NCEP; c++) {
                double s = 0;
                for (int m = 0; m < NMEL; m++) s += logMel[m] * Math.cos(Math.PI * c * (m + 0.5) / NMEL);
                cep[f][c - 1] = (float) s;
            }
        }
        int d = NCEP - 1;
        float[][] out = new float[frames][2 * d];
        for (int f = 0; f < frames; f++) {
            for (int c = 0; c < d; c++) {
                out[f][c] = cep[f][c];
                // Delta over ±2 frames.
                double num = 0;
                for (int k = 1; k <= 2; k++) {
                    int a = Math.min(frames - 1, f + k), b = Math.max(0, f - k);
                    num += k * (cep[a][c] - cep[b][c]);
                }
                out[f][d + c] = (float) (num / 10.0);
            }
        }
        normalize(out);
        return out;
    }

    private static void normalize(float[][] m) {
        if (m.length == 0) return;
        int dims = m[0].length;
        for (int c = 0; c < dims; c++) {
            double mean = 0;
            for (float[] r : m) mean += r[c];
            mean /= m.length;
            double var = 0;
            for (float[] r : m) var += (r[c] - mean) * (r[c] - mean);
            double sd = Math.sqrt(var / m.length) + 1e-6;
            for (float[] r : m) r[c] = (float) ((r[c] - mean) / sd);
        }
    }

    private static final java.util.Map<Double, float[][]> melBanks = new java.util.HashMap<>();

    /** Triangular mel filters; warp > 1 moves the filters up (for a voice with higher resonances). */
    private static synchronized float[][] melBank(double warp) {
        float[][] cached = melBanks.get(warp);
        if (cached != null) return cached;
        double lo = hzToMel(100), hi = hzToMel(7000);
        double[] pts = new double[NMEL + 2];
        for (int i = 0; i < pts.length; i++) pts[i] = Math.min(SR / 2.0 - 1, melToHz(lo + (hi - lo) * i / (NMEL + 1)) * warp);
        float[][] bank = new float[NMEL][NFFT / 2 + 1];
        for (int m = 0; m < NMEL; m++) {
            for (int k = 0; k <= NFFT / 2; k++) {
                double hz = k * (double) SR / NFFT;
                double w = 0;
                double up = pts[m + 1] - pts[m], down = pts[m + 2] - pts[m + 1];
                if (up > 0 && hz >= pts[m] && hz <= pts[m + 1]) w = (hz - pts[m]) / up;
                else if (down > 0 && hz > pts[m + 1] && hz <= pts[m + 2]) w = (pts[m + 2] - hz) / down;
                bank[m][k] = (float) w;
            }
        }
        melBanks.put(warp, bank);
        return bank;
    }

    private static double hzToMel(double hz) { return 2595 * Math.log10(1 + hz / 700); }

    private static double melToHz(double mel) { return 700 * (Math.pow(10, mel / 2595) - 1); }

    /** In-place radix-2 FFT. */
    static void fft(double[] re, double[] im) {
        int n = re.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                double t = re[i]; re[i] = re[j]; re[j] = t;
                t = im[i]; im[i] = im[j]; im[j] = t;
            }
        }
        for (int len = 2; len <= n; len <<= 1) {
            double ang = -2 * Math.PI / len;
            double wr = Math.cos(ang), wi = Math.sin(ang);
            for (int i = 0; i < n; i += len) {
                double cr = 1, ci = 0;
                for (int k = 0; k < len / 2; k++) {
                    int a = i + k, b = i + k + len / 2;
                    double xr = re[b] * cr - im[b] * ci, xi = re[b] * ci + im[b] * cr;
                    re[b] = re[a] - xr; im[b] = im[a] - xi;
                    re[a] += xr; im[a] += xi;
                    double t = cr * wr - ci * wi;
                    ci = cr * wi + ci * wr;
                    cr = t;
                }
            }
        }
    }

    // ------------------------------------------------------------------ alignment

    static final class Path {
        final double cost;   // mean local distance along the path
        final int[] i, j;
        final double[] local;

        Path(double cost, int[] i, int[] j, double[] local) { this.cost = cost; this.i = i; this.j = j; this.local = local; }
    }

    static Path dtw(float[][] a, float[][] b) {
        int n = a.length, m = b.length;
        double[][] acc = new double[n][m];
        int[][] steps = new int[n][m];
        double inf = Double.POSITIVE_INFINITY;
        // Band wide enough for half or double speed.
        double band = Math.max(0.35, Math.abs(n - m) / (double) Math.max(n, m) + 0.2);
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < m; y++) {
                double rel = Math.abs(x / (double) Math.max(1, n - 1) - y / (double) Math.max(1, m - 1));
                if (rel > band) { acc[x][y] = inf; continue; }
                double d = dist(a[x], b[y]);
                if (x == 0 && y == 0) { acc[x][y] = d; steps[x][y] = 1; continue; }
                double best = inf;
                int bs = 0, from = -1;
                if (x > 0 && y > 0 && acc[x - 1][y - 1] < inf) { best = acc[x - 1][y - 1]; bs = steps[x - 1][y - 1]; from = 0; }
                if (x > 0 && acc[x - 1][y] < inf && norm(acc[x - 1][y], steps[x - 1][y]) < norm(best, bs)) { best = acc[x - 1][y]; bs = steps[x - 1][y]; from = 1; }
                if (y > 0 && acc[x][y - 1] < inf && norm(acc[x][y - 1], steps[x][y - 1]) < norm(best, bs)) { best = acc[x][y - 1]; bs = steps[x][y - 1]; from = 2; }
                if (from < 0) { acc[x][y] = inf; continue; }
                acc[x][y] = best + d;
                steps[x][y] = bs + 1;
            }
        }
        // Backtrack by the same rule.
        int len = steps[n - 1][m - 1];
        if (len <= 0 || acc[n - 1][m - 1] == inf) {
            return new Path(1e6, new int[0], new int[0], new double[0]);
        }
        int[] pi = new int[len], pj = new int[len];
        double[] loc = new double[len];
        int x = n - 1, y = m - 1, k = len - 1;
        while (k >= 0) {
            pi[k] = x; pj[k] = y; loc[k] = dist(a[x], b[y]);
            k--;
            if (x == 0 && y == 0) break;
            double dg = (x > 0 && y > 0) ? norm(acc[x - 1][y - 1], steps[x - 1][y - 1]) : inf;
            double up = x > 0 ? norm(acc[x - 1][y], steps[x - 1][y]) : inf;
            double lf = y > 0 ? norm(acc[x][y - 1], steps[x][y - 1]) : inf;
            if (dg <= up && dg <= lf) { x--; y--; } else if (up <= lf) x--; else y--;
        }
        int used = len - 1 - k;
        if (used != len) {
            pi = java.util.Arrays.copyOfRange(pi, k + 1, len);
            pj = java.util.Arrays.copyOfRange(pj, k + 1, len);
            loc = java.util.Arrays.copyOfRange(loc, k + 1, len);
        }
        return new Path(acc[n - 1][m - 1] / steps[n - 1][m - 1], pi, pj, loc);
    }

    private static double norm(double acc, int steps) { return steps == 0 ? Double.POSITIVE_INFINITY : acc / steps; }

    private static double dist(float[] p, float[] q) {
        double s = 0;
        for (int i = 0; i < p.length; i++) { double d = p[i] - q[i]; s += d * d; }
        return Math.sqrt(s);
    }

    private static float[][] reversed(float[][] f) {
        float[][] r = new float[f.length][];
        for (int i = 0; i < f.length; i++) r[i] = f[f.length - 1 - i];
        return r;
    }

    // ------------------------------------------------------------------ resampling

    /** Linear resampling to 16 kHz with a light low-pass when downsampling. Non-finite samples become 0. */
    static float[] resample(float[] x, int rate) {
        for (float v : x) {
            if (Float.isNaN(v) || Float.isInfinite(v)) {
                x = x.clone();
                for (int i = 0; i < x.length; i++) if (Float.isNaN(x[i]) || Float.isInfinite(x[i])) x[i] = 0f;
                break;
            }
        }
        if (rate == SR || x.length == 0) return x;
        float[] src = x;
        if (rate > SR) {
            // Moving-average low-pass sized to the ratio to limit aliasing.
            int k = Math.max(1, (int) Math.round(rate / (double) SR));
            if (k > 1) {
                src = new float[x.length];
                double acc = 0;
                for (int i = 0; i < x.length; i++) {
                    acc += x[i];
                    if (i >= k) acc -= x[i - k];
                    src[i] = (float) (acc / Math.min(i + 1, k));
                }
            }
        }
        int n = (int) ((long) src.length * SR / rate);
        float[] out = new float[n];
        double step = rate / (double) SR;
        for (int i = 0; i < n; i++) {
            double p = i * step;
            int a = (int) p;
            double t = p - a;
            float va = src[Math.min(a, src.length - 1)], vb = src[Math.min(a + 1, src.length - 1)];
            out[i] = (float) (va + (vb - va) * t);
        }
        return out;
    }
}
