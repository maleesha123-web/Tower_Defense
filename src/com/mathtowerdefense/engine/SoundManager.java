package com.mathtowerdefense.engine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.DataLine;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import com.mathtowerdefense.util.ResourceLoader;

/**
 * Sound playback with FULL graceful degradation:
 *
 *  - 13 real .wav files ship in resources/sounds (baked by
 *    tests.SoundAssetExporter from SoundSynth - see SOUNDS.md).
 *  - When a file is missing, the IDENTICAL composition is synthesised live
 *    from SoundSynth recipes, so the fallback and the files sound the same.
 *  - Rapid-fire sounds (attack) are throttled so they never spam the mixer.
 *  - When the audio subsystem is unavailable entirely, every method becomes
 *    a silent no-op - sound can NEVER crash the game.
 *
 * All playback respects the Sound/Music toggles from the settings screen.
 */
public final class SoundManager {

    public enum Sound {
        CLICK("click"),
        ATTACK("attack"),
        HIT("hit"),
        CORRECT("correct"),
        WRONG("wrong"),
        WAVE("wave"),
        GAME_OVER("gameover"),
        VICTORY("victory"),
        BUILD("build"),
        UPGRADE("upgrade"),
        LEAK("leak"),
        CHALLENGE("challenge");

        private final String fileName;

        Sound(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }

    /** Minimum gap between repeated sounds, in milliseconds. */
    private static final long THROTTLE_ATTACK_MS = 120;
    private static final long THROTTLE_HIT_MS = 60;

    private static final SoundManager INSTANCE = new SoundManager();

    private final Map<Sound, byte[]> loadedClips = new HashMap<>();
    private final Map<Sound, Long> lastPlayed = new HashMap<>();
    private Clip musicClip;
    private boolean musicPlaying;

    private SoundManager() {
        // Pre-load every shipped sound file; missing ones stay out of the map
        // and fall back to the SoundSynth recipes at play time.
        for (Sound s : Sound.values()) {
            byte[] data = loadResource("/sounds/" + s.fileName + ".wav");
            if (data != null) {
                loadedClips.put(s, data);
            }
        }
    }

    public static SoundManager getInstance() { return INSTANCE; }

    /**
     * Plays a sound effect if sound is enabled in settings.
     * Audio runs on a short daemon thread so the EDT/game loop is never
     * blocked by playback (and overlapping sounds do not queue up).
     */
    public synchronized void play(Sound sound) {
        if (!GameConfig.getInstance().isSoundEnabled() || sound == null) {
            return;
        }
        // Throttle high-frequency combat sounds so a 60 FPS game loop can
        // never flood the mixer with attack/hit clips.
        if (sound == Sound.ATTACK && !throttleOk(sound, THROTTLE_ATTACK_MS)) {
            return;
        }
        if (sound == Sound.HIT && !throttleOk(sound, THROTTLE_HIT_MS)) {
            return;
        }
        Thread worker = new Thread(() -> {
            try {
                byte[] data = loadedClips.get(sound);
                if (data != null) {
                    playWaveData(data);
                } else {
                    // Identical composition, rendered live from SoundSynth.
                    byte[] pcm = SoundSynth.renderNamed(sound.fileName);
                    if (pcm != null) {
                        playPcm(pcm);
                    }
                }
            } catch (Throwable audioProblem) {
                // Never let audio issues reach the game loop.
            }
        }, "sfx-" + sound.name().toLowerCase());
        worker.setDaemon(true);
        worker.start();
    }

    /** Throttle guard: true when enough time passed since the last play. */
    private boolean throttleOk(Sound sound, long minGapMs) {
        long now = System.currentTimeMillis();
        Long last = lastPlayed.get(sound);
        if (last != null && now - last < minGapMs) {
            return false;
        }
        lastPlayed.put(sound, now);
        return true;
    }
    // ------------------------------------------------------------------
    // Background music (optional file: resources/sounds/music.wav)
    // ------------------------------------------------------------------

    /** Starts looping background music when enabled and available. */
    public void startMusic() {
        if (!GameConfig.getInstance().isMusicEnabled() || musicPlaying) {
            return;
        }
        try {
            byte[] data = loadResource("/sounds/music.wav");
            if (data == null) {
                return; // no music file -> silent, but no crash
            }
            musicClip = AudioSystem.getClip();
            musicClip.open(AudioSystem.getAudioInputStream(new ByteArrayInputStream(data)));
            if (musicClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                ((FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN)).setValue(-16.0f);
            }
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
            musicPlaying = true;
        } catch (Throwable audioProblem) {
            musicPlaying = false;
        }
    }

    /** Stops the background music (if running). */
    public void stopMusic() {
        try {
            if (musicClip != null) {
                musicClip.stop();
                musicClip.close();
            }
        } catch (Throwable ignored) {
            // nothing to clean up
        } finally {
            musicClip = null;
            musicPlaying = false;
        }
    }

    public boolean isMusicPlaying() { return musicPlaying; }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private void playWaveData(byte[] data) throws Exception {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
             Clip clip = AudioSystem.getClip()) {
            clip.open(ais);
            playClipOnce(clip);
        }
    }

    /**
     * Plays raw 8-bit mono PCM rendered by SoundSynth (live-fallback path).
     * Same sample rate/format the baked .wav files use.
     */
    private void playPcm(byte[] pcm) {
        try {
            AudioFormat format = new AudioFormat(
                    SoundSynth.SAMPLE_RATE, 8, 1, true, false);
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            try (Clip clip = (Clip) AudioSystem.getLine(info)) {
                clip.open(format, pcm, 0, pcm.length);
                playClipOnce(clip);
            }
        } catch (Throwable noAudioDevice) {
            // Speaker unavailable -> silent no-op.
        }
    }

    /** Blocks until the short clip finished (runs on the caller thread). */
    private void playClipOnce(Clip clip) throws Exception {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            ((FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN)).setValue(-10.0f);
        }
        clip.start();
        long deadline = System.currentTimeMillis() + 3000;
        while (clip.isRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        clip.close();
    }

    /** Loads an optional resource via {@link ResourceLoader}; null when missing. */
    private byte[] loadResource(String path) {
        return ResourceLoader.loadBytes(path);
    }
}
