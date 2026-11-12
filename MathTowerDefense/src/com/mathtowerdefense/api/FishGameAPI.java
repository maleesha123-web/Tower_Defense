package com.mathtowerdefense.api;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP client for the official Fish Game API.
 *
 * Documented behaviour (https://marcconrad.com/uob/fish/doc.php):
 * <pre>
 *   base URL : http://marcconrad.com/uob/fish/api.php
 *   ?out=json          -&gt; { "question": "&lt;image url&gt;", "fish": 9, "chest": 1 }
 *   ?out=json&amp;base64=yes -&gt; "question" contains the image as base64 instead of a URL
 *   ?out=csv           -&gt; &lt;image url&gt;,9,1
 * </pre>
 * The "question" is a picture of fish and chests; the player must enter the
 * number of fish (the official Fish Game wording), which is exactly the
 * documented "fish" value of the response.
 *
 * This class is the ONLY place that performs network I/O for questions. Every
 * failure mode (timeout, connection refused, HTTP error, malformed body,
 * unreadable image) is converted into a failed {@link APIResponse} with a
 * friendly message - it never throws at the game layer, so the game cannot
 * crash because the service is unreachable.
 */
public final class FishGameAPI {

    private static final int MAX_BODY_BYTES = 2 * 1024 * 1024; // guard against absurd payloads

    // Minimal JSON field extraction - the documented response is a flat object
    // with string/number values, so a full JSON library is not required.
    private static final Pattern QUESTION_FIELD =
            Pattern.compile("\"question\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern FISH_FIELD =
            Pattern.compile("\"fish\"\\s*:\\s*(-?\\d+)");
    private static final Pattern CHEST_FIELD =
            Pattern.compile("\"chest\"\\s*:\\s*(-?\\d+)");

    private final ApiConfig config;

    public FishGameAPI() {
        this(ApiConfig.getInstance());
    }

    public FishGameAPI(ApiConfig config) {
        this.config = config;
    }

    public ApiConfig getConfig() { return config; }
    /**
     * Fetches one question from the Fish Game API.
     *
     * Strategy (all officially documented variants):
     * 1. GET {@code ?out=json} - image referenced by URL.
     * 2. If the image URL cannot be downloaded, retry with
     *    {@code ?out=json&base64=yes} where the image is embedded directly.
     *
     * @return a successful response with a {@link Question}, or a failed
     *         response carrying a friendly error message - never throws.
     */
    public APIResponse fetchQuestion() {
        try {
            String body = httpGet(config.buildQuestionUrl());
            ParsedJson parsed = parseJson(body);
            if (parsed == null) {
                return APIResponse.fail("Invalid response from Math Question Service.");
            }

            BufferedImage image = null;
            if (parsed.isImageUrl()) {
                try {
                    image = downloadImage(parsed.questionValue);
                } catch (IOException imageFailed) {
                    // Documented fallback: ask again with the image embedded as base64.
                    image = fetchImageViaBase64();
                }
            } else {
                image = decodeBase64Image(parsed.questionValue);
            }

            if (image == null) {
                return APIResponse.fail("Unable to load the question image from Math Question Service.");
            }

            Question question = new Question(
                    "How many fish are in the picture?",
                    String.valueOf(parsed.fish),
                    Question.Source.API,
                    image,
                    parsed.isImageUrl() ? parsed.questionValue : null);
            return APIResponse.ok(question);

        } catch (IOException networkProblem) {
            // Timeout / connection refused / HTTP error status ...
            return APIResponse.fail("Unable to connect to Math Question Service: "
                    + friendlyNetworkMessage(networkProblem));
        } catch (RuntimeException unexpected) {
            return APIResponse.fail("Unexpected problem while contacting Math Question Service.");
        }
    }

    /** Retries the call using the documented {@code base64=yes} switch. */
    private BufferedImage fetchImageViaBase64() throws IOException {
        String body = httpGet(config.buildBase64QuestionUrl());
        ParsedJson parsed = parseJson(body);
        if (parsed == null || parsed.isImageUrl()) {
            return null;
        }
        return decodeBase64Image(parsed.questionValue);
    }

    // ------------------------------------------------------------------
    // Parsing (package-visible for unit tests)
    // ------------------------------------------------------------------

    /** Parsed fields of the documented JSON response. */
    public static final class ParsedJson {
        public final String questionValue;
        public final int fish;
        public final int chest;

        ParsedJson(String questionValue, int fish, int chest) {
            this.questionValue = questionValue;
            this.fish = fish;
            this.chest = chest;
        }

        public boolean isImageUrl() {
            return questionValue.startsWith("http://") || questionValue.startsWith("https://");
        }
    }

    /**
     * Extracts the documented {@code question}, {@code fish} and {@code chest}
     * fields from a JSON body.
     *
     * @return the parsed values, or {@code null} when the body is not a valid
     *         API response (missing question or fish count)
     */
    public static ParsedJson parseJson(String body) {
        if (body == null) {
            return null;
        }
        Matcher q = QUESTION_FIELD.matcher(body);
        Matcher f = FISH_FIELD.matcher(body);
        if (!q.find() || !f.find()) {
            return null;
        }
        String questionValue = unescapeJson(q.group(1));
        if (questionValue.isEmpty()) {
            return null;
        }
        try {
            int fish = Integer.parseInt(f.group(1));
            Matcher c = CHEST_FIELD.matcher(body);
            int chest = c.find() ? Integer.parseInt(c.group(1)) : 0;
            return new ParsedJson(questionValue, fish, chest);
        } catch (NumberFormatException badNumber) {
            return null;
        }
    }

    /** Decodes the {@code question} field when it holds raw base64 image data. */
    static BufferedImage decodeBase64Image(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        try {
            byte[] data = Base64.getMimeDecoder().decode(base64);
            return ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException | IllegalArgumentException badData) {
            return null;
        }
    }

    private static String unescapeJson(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\/", "/");
    }
    // ------------------------------------------------------------------
    // HTTP transport
    // ------------------------------------------------------------------

    /** Performs a documented GET request and returns the body as UTF-8 text. */
    private String httpGet(String url) throws IOException {
        byte[] raw = httpGetBytes(url);
        return new String(raw, StandardCharsets.UTF_8);
    }

    private byte[] httpGetBytes(String url) throws IOException {
        // Java's HttpURLConnection DOES follow same-protocol redirects, but NOT
        // cross-scheme (http -> https) redirects - and the official Fish Game
        // API now answers http:// with "301 Moved Permanently" to the https://
        // variant. Follow up to 5 redirects manually (any scheme/host).
        String current = url;
        for (int hop = 0; hop <= 5; hop++) {
            HttpURLConnection connection = (HttpURLConnection) new URL(current).openConnection();
            connection.setConnectTimeout(config.getConnectTimeoutMs());
            connection.setReadTimeout(config.getReadTimeoutMs());
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json, image/*, */*");
            connection.setInstanceFollowRedirects(true);

            try {
                int status = connection.getResponseCode();
                if (isRedirect(status)) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || location.isBlank()) {
                        throw new IOException("HTTP " + status);
                    }
                    current = new URL(new URL(current), location).toExternalForm();
                    continue;
                }
                if (status != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP " + status);
                }
                try (InputStream in = connection.getInputStream()) {
                    return readCapped(in);
                }
            } finally {
                connection.disconnect();
                // Note: FISH_API_KEY is intentionally NOT appended to the URL because
                // the official documentation defines no authentication mechanism.
            }
        }
        throw new IOException("Too many redirects");
    }

    /** 301/302/303/307/308 redirect statuses (note: 301 via cross-scheme is NOT
        auto-followed by HttpURLConnection, which is why we follow manually). */
    private static boolean isRedirect(int status) {
        return status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_SEE_OTHER
                || status == 307
                || status == 308;
    }

    private BufferedImage downloadImage(String imageUrl) throws IOException {
        byte[] raw = httpGetBytes(imageUrl);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(raw));
        if (img == null) {
            throw new IOException("Unsupported image data");
        }
        return img;
    }

    /** Reads at most {@link #MAX_BODY_BYTES} so a bad response cannot exhaust memory. */
    private static byte[] readCapped(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buffer)) != -1) {
            total += n;
            if (total > MAX_BODY_BYTES) {
                throw new IOException("Response too large");
            }
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    /** Converts low-level network exceptions into a friendly single sentence. */
    private static String friendlyNetworkMessage(IOException e) {
        String name = e.getClass().getSimpleName();
        if (e.getMessage() != null && e.getMessage().startsWith("HTTP")) {
            return "the service returned " + e.getMessage() + ".";
        }
        if (name.contains("Timeout") || (e.getMessage() != null && e.getMessage().toLowerCase().contains("timed out"))) {
            return "the request timed out.";
        }
        return "check your internet connection and try again.";
    }
}
