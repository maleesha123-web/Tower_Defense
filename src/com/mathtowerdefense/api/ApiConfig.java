package com.mathtowerdefense.api;

/**
 * Central configuration for the Fish Game API.
 *
 * The OFFICIAL documentation (https://marcconrad.com/uob/fish/doc.php) defines
 * exactly one endpoint with two OPTIONAL parameters:
 *
 *   base URL : http://marcconrad.com/uob/fish/api.php
 *   out      : json (default) | csv
 *   base64   : no (default) | yes
 *
 * The documentation does NOT define any API key or authentication mechanism,
 * therefore no key is invented and none is attached to requests. The optional
 * FISH_API_KEY environment variable is still read here so that a key can be
 * configured centrally (single source of configuration) if the service ever
 * requires one - it is explicitly never hard-coded in source files.
 */
public final class ApiConfig {

    /** Official base URL exactly as published in the documentation. */
    public static final String DEFAULT_BASE_URL = "http://marcconrad.com/uob/fish/api.php";

    /** Environment variable names (see .env.example). */
    public static final String ENV_API_KEY = "FISH_API_KEY";
    public static final String ENV_BASE_URL = "MTD_FISH_API_URL";

    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 4000;

    private static ApiConfig instance;

    private final String baseUrl;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final String apiKey; // optional; NOT required by the official API

    private ApiConfig() {
        String url = System.getenv(ENV_BASE_URL);
        this.baseUrl = (url == null || url.isBlank()) ? DEFAULT_BASE_URL : url.trim();
        this.connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
        this.readTimeoutMs = DEFAULT_READ_TIMEOUT_MS;

        String key = System.getenv(ENV_API_KEY);
        if (key != null && !key.isBlank() && !key.equals("YOUR_API_KEY_HERE")) {
            this.apiKey = key.trim();
            // Informational only: the official API defines no authentication,
            // so the key is never appended to request URLs.
            System.out.println("[ApiConfig] " + ENV_API_KEY + " is set but the official Fish Game "
                    + "API requires no key; it will not be sent.");
        } else {
            this.apiKey = null;
        }
    }
    public static synchronized ApiConfig getInstance() {
        if (instance == null) {
            instance = new ApiConfig();
        }
        return instance;
    }

    /** For tests: allows a custom config instance. */
    public ApiConfig(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        this.baseUrl = baseUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.apiKey = null;
    }

    public String getBaseUrl() { return baseUrl; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }

    /**
     * @return the configured API key or {@code null}. The official Fish Game API
     *         does not require a key; this exists only as a central config hook.
     */
    public String getApiKey() { return apiKey; }

    /** True when the official API documentation requires a key (currently always false). */
    public boolean isApiKeyRequired() { return false; }

    /**
     * Builds the documented JSON question URL:
     *   http://marcconrad.com/uob/fish/api.php?out=json
     */
    public String buildQuestionUrl() {
        return baseUrl + (baseUrl.contains("?") ? "&" : "?") + "out=json";
    }

    /** Builds the documented base64 variant: {@code ...?out=json&base64=yes}. */
    public String buildBase64QuestionUrl() {
        return buildQuestionUrl() + "&base64=yes";
    }

    @Override
    public String toString() {
        return "ApiConfig{baseUrl='" + baseUrl + "', apiKeyRequired=" + isApiKeyRequired() + "}";
    }
}
