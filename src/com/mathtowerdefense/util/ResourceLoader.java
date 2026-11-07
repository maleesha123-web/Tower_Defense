package com.mathtowerdefense.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

/**
 * Central resource-loading utility (spec: load from
 * {@code resources/images|fonts|sounds}, never absolute paths).
 *
 * This class lives in the neutral {@code util} package on purpose: both the
 * engine (SoundManager) and the GUI (Theme) need it, so placing it in either
 * layer would create a backwards package dependency. Every other package may
 * depend on {@code util} without breaking the layering.
 *
 * Lookup order:
 *   1. classpath ("/" + path) - works in Eclipse where {@code resources/}
 *      is a source folder (copied to {@code bin/}) and in packaged JARs;
 *   2. relative file {@code resources/ + path} - works when running from
 *      the project root via javac/java without copying anything.
 *
 * Every method is null-safe: missing/broken resources return null so the
 * game falls back to generated visuals/sounds instead of crashing.
 */
public final class ResourceLoader {

    private ResourceLoader() {}

    /** Normalises "sounds/x.wav" and "/sounds/x.wav" to "sounds/x.wav". */
    private static String normalise(String path) {
        if (path == null) {
            return "";
        }
        String p = path.replace('\\', '/');
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    /**
     * Opens a resource stream, or null when unavailable.
     * CALLERS MUST CLOSE the returned stream.
     */
    public static InputStream openStream(String path) {
        String p = normalise(path);
        InputStream onClasspath = ResourceLoader.class.getResourceAsStream("/" + p);
        if (onClasspath != null) {
            return onClasspath;
        }
        try {
            Path file = Paths.get("resources", p);
            if (Files.isRegularFile(file)) {
                return Files.newInputStream(file);
            }
        } catch (IOException | SecurityException ignored) {
            // fall through to null
        }
        return null;
    }

    /** Loads raw bytes, or null when unavailable. */
    public static byte[] loadBytes(String path) {
        try (InputStream in = openStream(path)) {
            return in == null ? null : in.readAllBytes();
        } catch (Throwable t) {
            return null;
        }
    }

    /** Loads an image, or null when unavailable/broken. */
    public static BufferedImage loadImage(String path) {
        try (InputStream in = openStream(path)) {
            return in == null ? null : ImageIO.read(in);
        } catch (Throwable t) {
            return null;
        }
    }

    /** True when the resource exists via classpath or file fallback. */
    public static boolean exists(String path) {
        try (InputStream in = openStream(path)) {
            return in != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
