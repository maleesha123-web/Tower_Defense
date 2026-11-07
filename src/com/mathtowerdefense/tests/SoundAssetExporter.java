package com.mathtowerdefense.tests;

import com.mathtowerdefense.engine.SoundManager;
import com.mathtowerdefense.engine.SoundSynth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Bakes the procedural SoundSynth recipes into real .wav files so the game
 * ships with proper sound effects instead of relying on live synthesis.
 *
 * Run:  java -cp bin com.mathtowerdefense.tests.SoundAssetExporter
 *       (regenerates resources/sounds/*.wav deterministically)
 *
 * The files are small (8 kHz 8-bit mono) and committed to the repository.
 * If they are ever deleted, SoundManager transparently renders the IDENTICAL
 * compositions live from SoundSynth - the game always has sound.
 */
public final class SoundAssetExporter {

    private SoundAssetExporter() {}

    public static void main(String[] args) throws Exception {
        Path dir = args.length > 0 ? Paths.get(args[0]) : Paths.get("resources", "sounds");
        Files.createDirectories(dir);

        int files = 0;
        long bytes = 0;
        for (SoundManager.Sound sound : SoundManager.Sound.values()) {
            byte[] pcm = SoundSynth.renderNamed(sound.getFileName());
            if (pcm == null) {
                System.out.println("  [SKIP] " + sound.getFileName() + " (no recipe)");
                continue;
            }
            byte[] wav = SoundSynth.toWav(pcm);
            Path out = dir.resolve(sound.getFileName() + ".wav");
            Files.write(out, wav);
            files++;
            bytes += wav.length;
            System.out.println("  [WROTE] " + out + " (" + wav.length + " bytes)");
        }

        byte[] music = SoundSynth.toWav(SoundSynth.renderMusicLoop());
        Path musicOut = dir.resolve("music.wav");
        Files.write(musicOut, music);
        files++;
        bytes += music.length;
        System.out.println("  [WROTE] " + musicOut + " (" + music.length + " bytes)");

        System.out.println("Exported " + files + " files, " + bytes + " bytes total.");
    }
}
