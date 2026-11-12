package com.mathtowerdefense.api;

import java.awt.image.BufferedImage;

/**
 * A single mathematics challenge shown to the player.
 *
 * A question originates either from the Fish Game API (a picture that asks the
 * player to count the fish in it - see https://marcconrad.com/uob/fish/) or
 * from the local offline fallback generator. The {@link Source} makes the
 * origin explicit so the UI can clearly distinguish API MODE from OFFLINE
 * FALLBACK MODE - a local question never pretends to come from the API.
 */
public final class Question {

    public enum Source { API, LOCAL_FALLBACK }

    private final String prompt;        // e.g. "How many fish are in the picture?" or "15 + 27 = ?"
    private final String answer;        // canonical answer as text (the API returns numbers)
    private final Source source;
    private final BufferedImage image;  // question image, may be null (text-only fallback questions)
    private final String imageUrl;      // original image URL when provided by the API, may be null

    public Question(String prompt, String answer, Source source, BufferedImage image, String imageUrl) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Question prompt must not be blank.");
        }
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("Question answer must not be blank.");
        }
        this.prompt = prompt;
        this.answer = answer.trim();
        this.source = source;
        this.image = image;
        this.imageUrl = imageUrl;
    }

    public String getPrompt() { return prompt; }

    public String getAnswer() { return answer; }

    public Source getSource() { return source; }

    /** True when this question was produced by the local offline generator. */
    public boolean isFallback() { return source == Source.LOCAL_FALLBACK; }

    /** @return the question image (API questions) or {@code null} for text-only questions. */
    public BufferedImage getImage() { return image; }

    public String getImageUrl() { return imageUrl; }

    /**
     * Validates a player's answer. Tolerates surrounding whitespace and
     * accepts integer-equivalent input ("7" vs "07").
     *
     * @param userInput raw text entered by the player; {@code null}/empty is incorrect
     * @return true when the input matches the canonical answer
     */
    public boolean isCorrect(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return false;
        }
        String normalized = userInput.trim();
        if (normalized.equals(answer)) {
            return true;
        }
        // Numeric tolerance: compare parsed integers when both sides are numbers.
        try {
            return Integer.parseInt(normalized) == Integer.parseInt(answer);
        } catch (NumberFormatException notANumber) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Question{" + prompt + " = " + answer + ", source=" + source + "}";
    }
}
