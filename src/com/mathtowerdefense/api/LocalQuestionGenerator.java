package com.mathtowerdefense.api;

import java.util.Random;

/**
 * Local question generator used when the Fish Game API is unreachable.
 *
 * IMPORTANT: every question produced by this class is flagged with
 * {@link Question.Source#LOCAL_FALLBACK}. The UI must visibly show that the
 * game is in OFFLINE FALLBACK MODE - a local question must NEVER be presented
 * as if it came from the API.
 */
public final class LocalQuestionGenerator {

    private final Random random;

    public LocalQuestionGenerator() {
        this(new Random());
    }

    /** Seeded constructor for deterministic unit tests. */
    public LocalQuestionGenerator(Random random) {
        this.random = random;
    }

    /**
     * Generates an arithmetic question scaled to the given difficulty level.
     *
     * @param level 1 = small +/− ; 2 = ×/÷ up to 12 ; 3+ = harder operands
     */
    public Question generate(int level) {
        int safeLevel = Math.max(1, Math.min(level, 5));
        int op = random.nextInt(4); // 0=+, 1=−, 2=×, 3=÷
        int a, b, answer;
        String symbol;

        int max = switch (safeLevel) {
            case 1 -> 12;
            case 2 -> 24;
            case 3 -> 50;
            case 4 -> 80;
            default -> 100;
        };

        switch (op) {
            case 0 -> {
                a = random.nextInt(max) + 1;
                b = random.nextInt(max) + 1;
                answer = a + b;
                symbol = "+";
            }
            case 1 -> {
                a = random.nextInt(max) + 1;
                b = random.nextInt(a); // never negative
                answer = a - b;
                symbol = "−";
            }
            case 2 -> {
                a = random.nextInt(Math.min(max, 15)) + 2;
                b = random.nextInt(11) + 2;
                answer = a * b;
                symbol = "×";
            }
            default -> {
                b = random.nextInt(11) + 2;
                answer = random.nextInt(Math.min(max, 12)) + 1;
                a = b * answer; // exact division
                symbol = "÷";
            }
        }

        return new Question(
                a + " " + symbol + " " + b + " = ?",
                String.valueOf(answer),
                Question.Source.LOCAL_FALLBACK,
                null,
                null);
    }
}
