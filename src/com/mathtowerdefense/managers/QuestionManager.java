package com.mathtowerdefense.managers;

import com.mathtowerdefense.api.APIResponse;
import com.mathtowerdefense.api.FishGameAPI;
import com.mathtowerdefense.api.LocalQuestionGenerator;
import com.mathtowerdefense.api.Question;

/**
 * Bridge between the game and the Fish Game API.
 *
 * All questions come exclusively from the Fish Game API (no local questions).
 */
public class QuestionManager {

    private final FishGameAPI api;
    private final LocalQuestionGenerator fallback;

    private String lastError;
    private int apiFailures = 0;
    private int questionsServed = 0;
    private int apiServed = 0;

    public QuestionManager() {
        this(new FishGameAPI(), new LocalQuestionGenerator());
    }

    public QuestionManager(FishGameAPI api, LocalQuestionGenerator fallback) {
        this.api = api;
        this.fallback = fallback;
    }

    /** Result of a fetch: the question plus where it came from + warning. */
    public static final class FetchResult {
        private final Question question;
        private final boolean fromApi;
        private final String warning; // non-null when the API failed for THIS question

        public FetchResult(Question question, boolean fromApi, String warning) {
            this.question = question;
            this.fromApi = fromApi;
            this.warning = warning;
        }

        public Question getQuestion() { return question; }

        public boolean isFromApi() { return fromApi; }

        public String getWarning() { return warning; }
    }

    /**
     * Fetches the next question exclusively from the Fish Game API.
     * Retries automatically on transient network issues; never serves local fake questions.
     *
     * @param level difficulty level hint
     */
    public synchronized FetchResult fetchQuestion(int level) {
        questionsServed++;
        APIResponse response = api.fetchQuestion();
        if (response.isSuccess() && response.getQuestion() != null) {
            apiServed++;
            lastError = null;
            return new FetchResult(response.getQuestion(), true, null);
        }

        // Retry on transient failure
        for (int i = 0; i < 2; i++) {
            response = api.fetchQuestion();
            if (response.isSuccess() && response.getQuestion() != null) {
                apiServed++;
                lastError = null;
                return new FetchResult(response.getQuestion(), true, null);
            }
        }

        apiFailures++;
        lastError = response.getErrorMessage() != null
                ? response.getErrorMessage()
                : "Unable to connect to Math Question Service.";
        // No local fallback questions: only questions from the API
        return new FetchResult(null, false, lastError);
    }

    /**
     * Checks/retries the API.
     */
    public synchronized boolean retryApi() {
        APIResponse response = api.fetchQuestion();
        if (response.isSuccess() && response.getQuestion() != null) {
            lastError = null;
            return true;
        }
        return false;
    }

    /** The game operates in API mode only. */
    public synchronized boolean isOffline() { return false; }

    /** Human-readable mode for the HUD: always "API MODE". */
    public synchronized String getModeLabel() {
        return "API MODE";
    }

    /** Why the API failed (may be null while in API mode). */
    public synchronized String getLastError() { return lastError; }

    public synchronized int getApiFailures() { return apiFailures; }

    public synchronized int getQuestionsServed() { return questionsServed; }

    public synchronized int getApiServed() { return apiServed; }

    /** Resets counters (used when starting a fresh game). */
    public synchronized void reset() {
        lastError = null;
        apiFailures = 0;
        questionsServed = 0;
        apiServed = 0;
    }
}
