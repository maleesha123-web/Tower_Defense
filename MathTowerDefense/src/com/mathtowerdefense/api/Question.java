package com.mathtowerdefense.api;

import java.awt.image.BufferedImage;

/**
 * A single mathematics challenge shown to the player.
 *
 * All questions originate exclusively from the Fish Game API (a picture that asks the
 * player to count the fish in it - see https://marcconrad.com/uob/fish/).
 */
public final class Question {

    public enum Source { API }

    private final String prompt;        // e.g. "How many fish are in the picture?"
    private final String answer;        // canonical answer as text (the API returns numbers)
    private final Source source;
    private final BufferedImage image;  // question image
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
        this.source = source != null ? source : Source.API;
        this.image = image;
        this.imageUrl = imageUrl;
    }

    public Question(String prompt, String answer, BufferedImage image, String imageUrl) {
        this(prompt, answer, Source.API, image, imageUrl);
    }

    public String getPrompt() { return prompt; }

    public String getAnswer() { return answer; }

    public Source getSource() { return source; }

    /** Always false: only real API questions are used. */
    public boolean isFallback() { return false; }

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
