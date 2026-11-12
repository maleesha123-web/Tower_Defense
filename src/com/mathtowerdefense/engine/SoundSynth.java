package com.mathtowerdefense.engine;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Procedural sound synthesis - zero audio assets required.
 *
 * Renders 8-bit mono PCM at 11025 Hz for every game sound effect. The exact
 * same renderer feeds BOTH paths:
 *   1. the live fallback in SoundManager (used when a .wav file is missing);
 *   2. tests/SoundAssetExporter, which bakes real .wav files into
 *      resources/sounds so the game ships with proper sound effects.
 *
 * All rendering is deterministic, so baked files never change between runs.
 */
public final class SoundSynth {

    /** Sample rate for both live playback and baked files (compact, clear). */
    public static final int SAMPLE_RATE = 11025;

    public enum Waveform { SINE, SQUARE, TRIANGLE }

    /** One pitched note; freqHz <= 0 means a silent rest. */
    public record Segment(double freqHz, int durationMs) {}

    private SoundSynth() {}

    /**
     * Renders a sequence of notes with a short attack and a fade-out release
     * on every note (no clicks between segments).
     */
    public static byte[] render(List<Segment> segments, Waveform wave, double volume) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Segment s : segments) {
            appendTone(out, s.freqHz(), s.durationMs(), wave, volume);
        }
        return out.toByteArray();
    }

    /** Renders several simultaneous sine tones (a chord). */
    public static byte[] chord(double[] freqs, int durationMs, double volume) {
        int total = (int) (SAMPLE_RATE * (long) durationMs / 1000);
        byte[] pcm = new byte[total];
        for (int i = 0; i < total; i++) {
            double t = i / (double) SAMPLE_RATE;
            double sum = 0;
            for (double f : freqs) {
                sum += Math.sin(2 * Math.PI * f * t);
            }
            double env = envelope(i, total);
            pcm[i] = (byte) (128 + 110 * volume * env * (sum / Math.max(1, freqs.length)));
        }
        return pcm;
    }

    /** 8-second calm C-G-Am-F loop used as the default background music. */
    public static byte[] renderMusicLoop() {
        double[][] progression = {
                { 261.63, 329.63, 392.00 }, // C major
                { 196.00, 246.94, 293.66 }, // G major
                { 220.00, 261.63, 329.63 }, // A minor
                { 174.61, 220.00, 261.63 }, // F major
        };
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (double[] chordNotes : progression) {
            byte[] part = chord(chordNotes, 2000, 0.22);
            out.write(part, 0, part.length);
        }
        byte[] loop = out.toByteArray();
        fadeEdges(loop, 40); // hide the loop seam
        return loop;
    }

    /** Wraps raw 8-bit mono PCM in a standard 44-byte WAV header. */
    public static byte[] toWav(byte[] pcm) {
        byte[] wav = new byte[44 + pcm.length];
        wav[0] = 'R'; wav[1] = 'I'; wav[2] = 'F'; wav[3] = 'F';
        putInt(wav, 4, 36 + pcm.length);
        wav[8] = 'W'; wav[9] = 'A'; wav[10] = 'V'; wav[11] = 'E';
        wav[12] = 'f'; wav[13] = 'm'; wav[14] = 't'; wav[15] = ' ';
        putInt(wav, 16, 16);          // fmt chunk size
        putShort(wav, 20, 1);         // PCM
        putShort(wav, 22, 1);         // mono
        putInt(wav, 24, SAMPLE_RATE);
        putInt(wav, 28, SAMPLE_RATE); // byte rate = rate * 1 ch * 1 byte
        putShort(wav, 32, 1);         // block align
        putShort(wav, 34, 8);         // bits per sample
        wav[36] = 'd'; wav[37] = 'a'; wav[38] = 't'; wav[39] = 'a';
        putInt(wav, 40, pcm.length);
        System.arraycopy(pcm, 0, wav, 44, pcm.length);
        return wav;
    }
    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static void appendTone(ByteArrayOutputStream out, double freqHz, int durationMs,
                                   Waveform wave, double volume) {
        int total = Math.max(1, (int) (SAMPLE_RATE * (long) durationMs / 1000));
        for (int i = 0; i < total; i++) {
            double value;
            if (freqHz <= 0) {
                value = 0; // rest
            } else {
                double phase = (i * freqHz / SAMPLE_RATE) % 1.0;
                value = switch (wave) {
                    case SQUARE -> phase < 0.5 ? 1.0 : -1.0;
                    case TRIANGLE -> 4.0 * Math.abs(phase - 0.5) - 1.0;
                    default -> Math.sin(2 * Math.PI * phase);
                };
            }
            // 4 ms attack, exponential-ish release - removes note clicks.
            double attack = Math.min(1.0, i / (SAMPLE_RATE * 0.004));
            double release = envelope(i, total);
            out.write((byte) (128 + 100 * volume * attack * release * value));
        }
    }

    /** 1.0 at the start, fading smoothly to 0.0 at the end. */
    private static double envelope(int i, int total) {
        if (total <= 1) {
            return 0.0;
        }
        double x = i / (double) (total - 1);
        return (1.0 - x) * (1.0 - x);
    }

    /** Fades the first/last N samples of a buffer (loop-seam de-click). */
    private static void fadeEdges(byte[] pcm, int samples) {
        int n = Math.min(samples, pcm.length / 2);
        for (int i = 0; i < n && i < pcm.length; i++) {
            double f = i / (double) n;
            pcm[i] = (byte) (128 + (pcm[i] - 128) * f);
            int j = pcm.length - 1 - i;
            pcm[j] = (byte) (128 + (pcm[j] - 128) * f);
        }
    }

    private static void putInt(byte[] buf, int offset, int value) {
        buf[offset] = (byte) value;
        buf[offset + 1] = (byte) (value >>> 8);
        buf[offset + 2] = (byte) (value >>> 16);
        buf[offset + 3] = (byte) (value >>> 24);
    }

    private static void putShort(byte[] buf, int offset, int value) {
        buf[offset] = (byte) value;
        buf[offset + 1] = (byte) (value >>> 8);
    }

    // ------------------------------------------------------------------
    // Shared sound recipes (single source of truth)
    // ------------------------------------------------------------------

    /**
     * One complete recipe for a game sound effect. Used by BOTH the live
     * {@code SoundManager} fallback and by {@code tests.SoundAssetExporter},
     * which bakes the identical bytes into resources/sounds/*.wav - what you
     * hear from a file and what you hear as a fallback is the same composition.
     */
    public record SoundSpec(Waveform wave, double volume, List<Segment> notes) {}

    /**
     * Returns the recipe for a sound file name (e.g. "click" or "music").
     *
     * @return the spec, or null for unknown names
     */
    public static SoundSpec specFor(String fileName) {
        if (fileName == null) {
            return null;
        }
        return switch (fileName) {
            case "click" -> spec(Waveform.SINE, 0.40,
                    new Segment(1200, 30), new Segment(900, 30));
            case "attack" -> spec(Waveform.SQUARE, 0.22,
                    new Segment(520, 35));
            case "hit" -> spec(Waveform.TRIANGLE, 0.50,
                    new Segment(220, 70), new Segment(140, 60));
            case "correct" -> spec(Waveform.SINE, 0.50,
                    new Segment(660, 90), new Segment(880, 90), new Segment(1320, 140));
            case "wrong" -> spec(Waveform.SQUARE, 0.40,
                    new Segment(330, 140), new Segment(196, 180));
            case "wave" -> spec(Waveform.SINE, 0.50,
                    new Segment(523, 80), new Segment(659, 80), new Segment(784, 140));
            case "gameover" -> spec(Waveform.TRIANGLE, 0.50,
                    new Segment(392, 160), new Segment(311, 160),
                    new Segment(247, 180), new Segment(196, 320));
            case "victory" -> spec(Waveform.SINE, 0.50,
                    new Segment(523, 110), new Segment(659, 110),
                    new Segment(784, 110), new Segment(1046, 220));
            case "build" -> spec(Waveform.SINE, 0.50,
                    new Segment(440, 60), new Segment(660, 80));
            case "upgrade" -> spec(Waveform.SINE, 0.50,
                    new Segment(660, 70), new Segment(880, 70), new Segment(1108, 120));
            case "leak" -> spec(Waveform.SQUARE, 0.50,
                    new Segment(180, 120), new Segment(120, 160));
            case "challenge" -> spec(Waveform.SINE, 0.50,
                    new Segment(740, 90), new Segment(988, 130));
            default -> null;
        };
    }

    private static SoundSpec spec(Waveform wave, double volume, Segment... notes) {
        return new SoundSpec(wave, volume, List.of(notes));
    }

    /** Renders the full PCM for a named sound (null when the name is unknown). */
    public static byte[] renderNamed(String fileName) {
        SoundSpec spec = specFor(fileName);
        return spec == null ? null : render(spec.notes(), spec.wave(), spec.volume());
    }
}
