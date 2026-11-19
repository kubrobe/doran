package com.doranalley.score;

import java.util.ArrayList;
import java.util.List;

/**
 * Compares what the speech recognizer heard with the target phrase, syllable by syllable.
 * Syllables are compared through their jamo (initial consonant, vowel, final consonant), so
 * 바뀌었어요 heard as 바꼈어요 counts as close rather than wrong.
 * Plain Java (no Android types) so it can be unit-tested on the JVM.
 */
public final class Hangul {

    private Hangul() {}

    /** Per target syllable: 2 = matched, 1 = close, 0 = missed. */
    public static final class Result {
        public final int score;          // 0..100
        public final String target;      // target syllables only (no spaces/punctuation)
        public final int[] marks;        // one per char of target
        public final String heard;       // the alternative that matched best

        Result(int score, String target, int[] marks, String heard) {
            this.score = score;
            this.target = target;
            this.marks = marks;
            this.heard = heard;
        }
    }

    /**
     * Best result over the recognizer's alternatives.
     * @param allowExtra true when the phrase may sit inside a longer reply (extra words around it are free)
     */
    public static Result best(String target, List<String> heardAlternatives, boolean allowExtra) {
        Result best = null;
        for (String h : heardAlternatives) {
            Result r = compare(target, h, allowExtra);
            if (best == null || r.score > best.score) best = r;
        }
        return best != null ? best : compare(target, "", allowExtra);
    }

    public static Result compare(String target, String heard) { return compare(target, heard, false); }

    public static Result compare(String target, String heard, boolean allowExtra) {
        String t = keep(Numbers.toSinoKorean(target));
        String h = keep(Numbers.toSinoKorean(heard));
        int n = t.length(), m = h.length();
        if (n == 0) return new Result(0, t, new int[0], heard);
        // Syllable-level edit distance where substitution cost = jamo distance (0..1).
        double[][] d = new double[n + 1][m + 1];
        for (int i = 0; i <= n; i++) d[i][0] = i;
        for (int j = 0; j <= m; j++) d[0][j] = allowExtra ? 0 : j * 0.8; // extra heard syllables cost a little less
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                double sub = d[i - 1][j - 1] + syllableDistance(t.charAt(i - 1), h.charAt(j - 1));
                double del = d[i - 1][j] + 1;
                double ins = d[i][j - 1] + 0.8;
                d[i][j] = Math.min(sub, Math.min(del, ins));
            }
        }
        // With allowExtra the phrase may end anywhere in what was heard.
        int endJ = m;
        if (allowExtra) for (int j = 0; j <= m; j++) if (d[n][j] < d[n][endJ]) endJ = j;
        // Backtrack to mark each target syllable.
        int[] marks = new int[n];
        int i = n, j = endJ;
        while (i > 0) {
            if (j > 0 && Math.abs(d[i][j] - (d[i - 1][j - 1] + syllableDistance(t.charAt(i - 1), h.charAt(j - 1)))) < 1e-9) {
                double sd = syllableDistance(t.charAt(i - 1), h.charAt(j - 1));
                marks[i - 1] = sd == 0 ? 2 : (sd <= 0.45 ? 1 : 0);
                i--; j--;
            } else if (Math.abs(d[i][j] - (d[i - 1][j] + 1)) < 1e-9) {
                marks[i - 1] = 0;
                i--;
            } else {
                j--;
            }
        }
        double dist = d[n][endJ];
        int score = (int) Math.round(100 * Math.max(0, 1 - dist / Math.max(n, 1)));
        return new Result(score, t, marks, heard);
    }

    /** Hangul syllables, Latin letters and digits only. */
    static String keep(String s) {
        StringBuilder b = new StringBuilder();
        if (s == null) return "";
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            if (isSyllable(c) || Character.isLetterOrDigit(c)) b.append(Character.toLowerCase(c));
        }
        return b.toString();
    }

    static boolean isSyllable(char c) { return c >= 0xAC00 && c <= 0xD7A3; }

    /** 0 = same, 1 = completely different; weights: vowel 0.4, initial 0.35, final 0.25. */
    static double syllableDistance(char a, char b) {
        if (a == b) return 0;
        if (!isSyllable(a) || !isSyllable(b)) return 1;
        int ia = a - 0xAC00, ib = b - 0xAC00;
        int ca = ia / 588, va = (ia % 588) / 28, fa = ia % 28;
        int cb = ib / 588, vb = (ib % 588) / 28, fb = ib % 28;
        double d = 0;
        if (ca != cb) d += 0.35 * (similarInitial(ca, cb) ? 0.5 : 1);
        if (va != vb) d += 0.40 * (similarVowel(va, vb) ? 0.5 : 1);
        if (fa != fb) d += 0.25 * (sameFinalSound(fa, fb) ? 0.3 : 1);
        return d;
    }

    // Initial consonant groups that learners and recognizers commonly swap (plain / tense / aspirated).
    private static final int[][] INITIAL_GROUPS = {
        {0, 1, 15},   // ㄱ ㄲ ㅋ
        {3, 4, 16},   // ㄷ ㄸ ㅌ
        {7, 8, 17},   // ㅂ ㅃ ㅍ
        {9, 10},      // ㅅ ㅆ
        {12, 13, 14}, // ㅈ ㅉ ㅊ
        {2, 5},       // ㄴ ㄹ
        {11, 18},     // ㅇ ㅎ
    };

    private static boolean similarInitial(int a, int b) {
        for (int[] g : INITIAL_GROUPS) if (in(g, a) && in(g, b)) return true;
        return false;
    }

    // Vowel pairs that sound alike in modern Seoul speech or differ only by a glide.
    private static final int[][] VOWEL_GROUPS = {
        {1, 5, 3, 7},     // ㅐ ㅔ ㅒ ㅖ
        {10, 11, 15},     // ㅙ ㅚ ㅞ
        {0, 2}, {4, 6}, {8, 12}, {13, 17}, {18, 19, 20}, {14, 16},
    };

    private static boolean similarVowel(int a, int b) {
        for (int[] g : VOWEL_GROUPS) if (in(g, a) && in(g, b)) return true;
        return false;
    }

    /** Finals that are pronounced the same at the end of a syllable (받침 neutralization). */
    private static boolean sameFinalSound(int a, int b) { return finalSound(a) == finalSound(b); }

    private static int finalSound(int f) {
        switch (f) {
            case 0: return 0;
            case 1: case 2: case 3: case 24: case 9: return 1;   // ㄱ ㄲ ㄳ ㅋ ㄺ → [k]
            case 4: case 5: case 6: return 4;                    // ㄴ ㄵ ㄶ → [n]
            case 7: case 19: case 20: case 22: case 23: case 25: case 27: return 7; // ㄷ ㅅ ㅆ ㅈ ㅊ ㅌ ㅎ → [t]
            case 8: case 11: case 12: case 13: case 14: case 15: return 8; // ㄹ and most ㄹ-clusters → [l]
            case 16: case 10: return 16;                         // ㅁ ㄻ → [m]
            case 17: case 18: case 26: return 17;                // ㅂ ㅄ ㅍ → [p]
            case 21: return 21;                                  // ㅇ → [ng]
            default: return f;
        }
    }

    private static boolean in(int[] g, int v) {
        for (int x : g) if (x == v) return true;
        return false;
    }

    /** Recognizers often write numbers as digits ("302호"); spell them the way the phrase is written. */
    static final class Numbers {
        private static final String[] D = {"공", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구"};

        static String toSinoKorean(String s) {
            if (s == null) return "";
            StringBuilder out = new StringBuilder();
            int k = 0;
            while (k < s.length()) {
                char c = s.charAt(k);
                if (c >= '0' && c <= '9') {
                    int st = k;
                    while (k < s.length() && s.charAt(k) >= '0' && s.charAt(k) <= '9') k++;
                    out.append(spell(s.substring(st, k)));
                } else {
                    out.append(c);
                    k++;
                }
            }
            return out.toString();
        }

        private static String spell(String digits) {
            if (digits.length() > 4) {
                StringBuilder b = new StringBuilder();
                for (char c : digits.toCharArray()) b.append(D[c - '0']);
                return b.toString();
            }
            int v = Integer.parseInt(digits);
            if (v == 0) return "영";
            String[] units = {"", "십", "백", "천"};
            StringBuilder b = new StringBuilder();
            String str = Integer.toString(v);
            for (int p = 0; p < str.length(); p++) {
                int dgt = str.charAt(p) - '0';
                int unit = str.length() - 1 - p;
                if (dgt == 0) continue;
                if (!(dgt == 1 && unit > 0)) b.append(D[dgt]);
                b.append(units[unit]);
            }
            return b.toString();
        }
    }

    /** Split into display units (for tests and debugging). */
    static List<String> syllables(String s) {
        List<String> out = new ArrayList<>();
        for (char c : keep(s).toCharArray()) out.add(String.valueOf(c));
        return out;
    }
}
