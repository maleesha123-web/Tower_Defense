package com.mathtowerdefense.api;

/**
 * Immutable result of a Fish Game API request.
 *
 * Either a {@link Question} was parsed successfully ({@link #isSuccess()} is
 * true and {@link #getQuestion()} is non-null), or the call failed and
 * {@link #getErrorMessage()} contains a human-readable reason. The game layer
 * never sees raw HTTP/JSON details - the API package isolates them.
 */
public final class APIResponse {

    private final boolean success;
    private final Question question;
    private final String errorMessage;

    private APIResponse(boolean success, Question question, String errorMessage) {
        this.success = success;
        this.question = question;
        this.errorMessage = errorMessage;
    }

    public static APIResponse ok(Question question) {
        return new APIResponse(true, question, null);
    }

    public static APIResponse fail(String errorMessage) {
        return new APIResponse(false, null,
                errorMessage == null || errorMessage.isBlank() ? "Unknown API error." : errorMessage);
    }

    public boolean isSuccess() { return success; }

    /** @return the parsed question, or {@code null} when the call failed. */
    public Question getQuestion() { return question; }

    /** @return a friendly error message, or {@code null} on success. */
    public String getErrorMessage() { return errorMessage; }

    @Override
    public String toString() {
        return success ? "APIResponse{question=" + question + "}"
                       : "APIResponse{error='" + errorMessage + "'}";
    }
}
