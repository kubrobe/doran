package com.doranalley.score;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * Minimal PCM WAV reader/writer. Plain Java (no Android types) so it is unit-testable on the JVM.
 * Reads 16-bit PCM (mono or multi-channel, mixed down) and 32-bit float WAV, at any sample rate.
 */
public final class Wav {

    private Wav() {}

    /** Decoded audio: mono samples in -1..1 and their sample rate. */
    public static final class Audio {
        public final float[] samples;
        public final int sampleRate;

        public Audio(float[] samples, int sampleRate) {
            this.samples = samples;
            this.sampleRate = sampleRate;
        }

        public double seconds() { return sampleRate == 0 ? 0 : samples.length / (double) sampleRate; }
    }

    public static Audio read(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            return read(in);
        }
    }

    public static Audio read(InputStream in) throws IOException {
        byte[] b = readAll(in);
        if (b.length < 12 || !tag(b, 0, "RIFF") || !tag(b, 8, "WAVE")) throw new IOException("not a WAV file");
        int pos = 12;
        int format = -1, channels = 1, rate = 0, bits = 16;
        int dataStart = -1, dataLen = 0;
        while (pos + 8 <= b.length) {
            int size = le32(b, pos + 4);
            if (size < 0) break;
            if (tag(b, pos, "fmt ")) {
                format = le16(b, pos + 8);
                channels = Math.max(1, le16(b, pos + 10));
                rate = le32(b, pos + 12);
                bits = le16(b, pos + 22);
                if (format == 0xFFFE && size >= 26) format = le16(b, pos + 32); // WAVE_FORMAT_EXTENSIBLE sub-format
            } else if (tag(b, pos, "data")) {
                dataStart = pos + 8;
                // Some writers leave the size at 0 or 0xFFFFFFFF while streaming; use what is there.
                dataLen = (size == 0 || dataStart + size > b.length) ? b.length - dataStart : size;
                break;
            }
            pos += 8 + size + (size & 1);
        }
        if (dataStart < 0 || rate <= 0) throw new IOException("WAV without fmt/data");
        int frameBytes = channels * (bits / 8);
        if (frameBytes <= 0) throw new IOException("bad WAV format");
        int frames = dataLen / frameBytes;
        float[] out = new float[frames];
        for (int i = 0; i < frames; i++) {
            float sum = 0f;
            for (int c = 0; c < channels; c++) {
                int o = dataStart + i * frameBytes + c * (bits / 8);
                if (format == 3 && bits == 32) sum += Float.intBitsToFloat(le32(b, o));
                else if (bits == 16) sum += (short) le16(b, o) / 32768f;
                else if (bits == 8) sum += ((b[o] & 0xFF) - 128) / 128f;
                else if (bits == 24) sum += ((b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8) | (b[o + 2] << 16)) / 8388608f;
                else if (bits == 32) sum += le32(b, o) / 2147483648f;
                else throw new IOException("unsupported WAV bit depth " + bits);
            }
            out[i] = sum / channels;
        }
        return new Audio(out, rate);
    }

    /** Writes a 44-byte header for mono 16-bit PCM. Call again after the data is written to fix sizes. */
    public static void writeHeader(RandomAccessFile f, int sampleRate, int dataBytes) throws IOException {
        byte[] h = new byte[44];
        put(h, 0, "RIFF"); le32(h, 4, 36 + dataBytes); put(h, 8, "WAVE");
        put(h, 12, "fmt "); le32(h, 16, 16); le16(h, 20, 1); le16(h, 22, 1);
        le32(h, 24, sampleRate); le32(h, 28, sampleRate * 2); le16(h, 32, 2); le16(h, 34, 16);
        put(h, 36, "data"); le32(h, 40, dataBytes);
        f.seek(0);
        f.write(h);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        return bo.toByteArray();
    }

    private static boolean tag(byte[] b, int o, String t) {
        if (o + 4 > b.length) return false;
        for (int i = 0; i < 4; i++) if (b[o + i] != (byte) t.charAt(i)) return false;
        return true;
    }

    private static int le16(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8); }

    private static int le32(byte[] b, int o) {
        return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8) | ((b[o + 2] & 0xFF) << 16) | ((b[o + 3] & 0xFF) << 24);
    }

    private static void put(byte[] b, int o, String t) { for (int i = 0; i < 4; i++) b[o + i] = (byte) t.charAt(i); }

    private static void le16(byte[] b, int o, int v) { b[o] = (byte) v; b[o + 1] = (byte) (v >> 8); }

    private static void le32(byte[] b, int o, int v) {
        b[o] = (byte) v; b[o + 1] = (byte) (v >> 8); b[o + 2] = (byte) (v >> 16); b[o + 3] = (byte) (v >> 24);
    }
}
