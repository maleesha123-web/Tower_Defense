package com.mathtowerdefense.managers;

import com.mathtowerdefense.api.APIResponse;
import com.mathtowerdefense.api.FishGameAPI;
import com.mathtowerdefense.api.LocalQuestionGenerator;
import com.mathtowerdefense.api.Question;

/**
 * Bridge between the game and the question providers.
 *
 * Request flow per question:
 *   1. If the API has not failed yet, ask the Fish Game API.
 *   2. On any failure, remember the reason, switch visibly to
 *      OFFLINE FALLBACK MODE and generate a local question instead.
 *   3. The game NEVER crashes and NEVER claims a local question came from
 *      the API - the mode is always exposed to the UI via
 *      {@link #isOffline()} / {@link #getModeLabel()}.
 *
 * Network access happens on the calling thread, so the GUI layer invokes it
 * from a background worker. Methods are synchronized because the worker and
 * the EDT both touch this object.
 */
public class QuestionManager {

    private final FishGameAPI api;
    private final LocalQuestionGenerator fallback;

    private boolean offline = false;
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

        FetchResult(Question question, boolean fromApi, String warning) {
            this.question = question;
            this.fromApi = fromApi;
            this.warning = warning;
        }

        public Question getQuestion() { return question; }

        public boolean isFromApi() { return fromApi; }

        public String getWarning() { return warning; }
    }

    /**
     * Fetches the next question for the given difficulty level.
     * Never throws; on API failure it returns a clearly-marked fallback question.
     *
     * @param level difficulty hint for the local generator (1..5+)
     */
    public synchronized FetchResult fetchQuestion(int level) {
        questionsServed++;
        if (!offline) {
            APIResponse response = api.fetchQuestion();
            if (response.isSuccess() && response.getQuestion() != null) {
                apiServed++;
                return new FetchResult(response.getQuestion(), true, null);
            }
            // API failed -> record why and switch to offline mode (visible in UI).
            apiFailures++;
            lastError = response.getErrorMessage() != null
                    ? response.getErrorMessage()
                    : "Unable to connect to Math Question Service.";
            offline = true;
            Question local = fallback.generate(level);
            return new FetchResult(local, false, lastError);
        }
        // Already offline: keep serving local questions immediately.
        return new FetchResult(fallback.generate(level), false, null);
    }

    /**
     * Retries the API once the game is offline (called when a new game starts)
     * so the game can recover automatically when the service comes back.
     *
     * @return true when the API answered again and we are back in API mode
     */
    public synchronized boolean retryApi() {
        if (!offline) {
            return true;
        }
        APIResponse response = api.fetchQuestion();
        if (response.isSuccess() && response.getQuestion() != null) {
            offline = false;
            lastError = null;
            return true;
        }
        return false;
    }

    /** True when the game is currently serving local fallback questions. */
    public synchronized boolean isOffline() { return offline; }

    /** Human-readable mode for the HUD: "API MODE" or "OFFLINE FALLBACK MODE". */
    public synchronized String getModeLabel() {
        return offline ? "OFFLINE FALLBACK MODE" : "API MODE";
    }

    /** Why the API failed (may be null while in API mode). */
    public synchronized String getLastError() { return lastError; }

    public synchronized int getApiFailures() { return apiFailures; }

    public synchronized int getQuestionsServed() { return questionsServed; }

    public synchronized int getApiServed() { return apiServed; }

    /** Resets counters/mode (used when starting a fresh game). */
    public synchronized void reset() {
        offline = false;
        lastError = null;
        apiFailures = 0;
        questionsServed = 0;
        apiServed = 0;
    }
}
