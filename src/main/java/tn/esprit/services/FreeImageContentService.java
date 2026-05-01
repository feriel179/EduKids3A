package tn.esprit.services;

import tn.esprit.util.AppSettings;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class FreeImageContentService {
    private static final String DEFAULT_BASE_URL = "https://image.pollinations.ai";
    private static final String DEFAULT_GATEWAY_BASE_URL = "https://gen.pollinations.ai";
    private static final String DEFAULT_LEGACY_BASE_URL = "https://pollinations.ai";
    private static final String DEFAULT_IMAGE_MODEL = "automatic";
    private static final Path OUTPUT_DIRECTORY = Path.of(System.getProperty("user.dir"), "generated", "course-images");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public String generateCourseImage(String title, String subject, int level, String description) {
        String prompt = buildPrompt(title, subject, level, description);
        String encodedPrompt = encodePathSegment(prompt);
        long seed = ThreadLocalRandom.current().nextLong(1, Integer.MAX_VALUE);

        List<String> attempts = new ArrayList<>();
        for (RequestTarget target : buildTargets(encodedPrompt, seed)) {
            try {
                return downloadImage(target, slugify(title));
            } catch (IllegalStateException exception) {
                attempts.add(exception.getMessage());
            }
        }

        throw new IllegalStateException("Free image provider failed. Tried: " + String.join(" | ", attempts));
    }

    public String getBaseUrl() {
        return readSettingOrDefault("POLLINATIONS_BASE_URL", DEFAULT_BASE_URL);
    }

    public String getImageModel() {
        return readSettingOrDefault("POLLINATIONS_IMAGE_MODEL", DEFAULT_IMAGE_MODEL);
    }

    private List<RequestTarget> buildTargets(String encodedPrompt, long seed) {
        String model = getImageModel();
        Set<String> urls = new LinkedHashSet<>();

        urls.add(buildConfiguredUrl(encodedPrompt, seed, model, true));
        urls.add(buildConfiguredUrl(encodedPrompt, seed, model, false));
        urls.add(buildPromptEndpoint(DEFAULT_BASE_URL, encodedPrompt, seed, model, false));
        urls.add(buildGatewayEndpoint(DEFAULT_GATEWAY_BASE_URL, encodedPrompt, seed, model, false));
        urls.add(buildLegacyEndpoint(DEFAULT_LEGACY_BASE_URL, encodedPrompt, seed, model, false));

        List<RequestTarget> targets = new ArrayList<>();
        for (String url : urls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            targets.add(new RequestTarget(extractHostLabel(url), url));
        }
        return targets;
    }

    private String buildConfiguredUrl(String encodedPrompt, long seed, String model, boolean includeModel) {
        String baseUrl = trimTrailingSlash(getBaseUrl());
        if (baseUrl.contains("/prompt")) {
            return baseUrl + "/" + encodedPrompt + buildQuery(seed, model, includeModel);
        }
        if (baseUrl.contains("/image")) {
            return baseUrl + "/" + encodedPrompt + buildQuery(seed, model, includeModel);
        }
        if (baseUrl.contains("gen.pollinations.ai")) {
            return buildGatewayEndpoint(baseUrl, encodedPrompt, seed, model, includeModel);
        }
        if (baseUrl.contains("pollinations.ai") && !baseUrl.contains("image.pollinations.ai")) {
            return buildLegacyEndpoint(baseUrl, encodedPrompt, seed, model, includeModel);
        }
        return buildPromptEndpoint(baseUrl, encodedPrompt, seed, model, includeModel);
    }

    private String buildPromptEndpoint(String baseUrl, String encodedPrompt, long seed, String model, boolean includeModel) {
        return trimTrailingSlash(baseUrl) + "/prompt/" + encodedPrompt + buildQuery(seed, model, includeModel);
    }

    private String buildGatewayEndpoint(String baseUrl, String encodedPrompt, long seed, String model, boolean includeModel) {
        return trimTrailingSlash(baseUrl) + "/image/" + encodedPrompt + buildQuery(seed, model, includeModel);
    }

    private String buildLegacyEndpoint(String baseUrl, String encodedPrompt, long seed, String model, boolean includeModel) {
        return trimTrailingSlash(baseUrl) + "/p/" + encodedPrompt + buildQuery(seed, model, includeModel);
    }

    private String buildQuery(long seed, String model, boolean includeModel) {
        StringBuilder query = new StringBuilder("?width=1024&height=1024");
        query.append("&seed=").append(seed);
        query.append("&safe=true");
        query.append("&nologo=true");

        if (includeModel && model != null && !model.isBlank() && !"automatic".equalsIgnoreCase(model.trim())) {
            query.append("&model=").append(encodeQueryValue(model.trim()));
        }

        return query.toString();
    }

    private String downloadImage(RequestTarget target, String titleSlug) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(target.url()))
                .timeout(Duration.ofSeconds(90))
                .header("Accept", "image/*")
                .header("User-Agent", "EduKids/1.0")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(formatStatusFailure(target.name(), response.statusCode(), response.body()));
            }

            byte[] body = response.body();
            String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
            ImageKind imageKind = detectImageKind(contentType, body);
            if (imageKind == ImageKind.UNKNOWN) {
                throw new IllegalStateException(formatNonImageFailure(target.name(), contentType, body));
            }
            if (imageKind == ImageKind.WEBP) {
                throw new IllegalStateException("unsupported WEBP image via " + target.name());
            }

            Files.createDirectories(OUTPUT_DIRECTORY);
            String safeSlug = shortenSlug(titleSlug);
            String fileName = safeSlug + "-" + System.currentTimeMillis() + imageKind.extension();
            Path outputPath = OUTPUT_DIRECTORY.resolve(fileName);
            Files.write(outputPath, body);
            return Path.of("generated", "course-images", fileName).toString();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("image request interrupted via " + target.name());
        } catch (IOException exception) {
            throw new IllegalStateException("provider unreachable via " + target.name(), exception);
        }
    }

    private String buildPrompt(String title, String subject, int level, String description) {
        return """
                Educational cover illustration for an EduKids course.
                Friendly classroom poster style, colorful, clean, modern, high quality.
                Subject: %s.
                Course title concept: %s.
                Level: Niveau %d.
                Description cues: %s.
                No text, no letters, no numbers, no watermark, no logo.
                """.formatted(
                safeText(subject, "general education"),
                safeText(title, "learning adventure"),
                Math.max(1, level),
                compactDescription(description)
        ).replaceAll("\\s+", " ").trim();
    }

    private String compactDescription(String description) {
        String clean = safeText(description, "playful learning, visual storytelling, child-friendly atmosphere");
        clean = clean.replace('\n', ' ').replace('\r', ' ').trim();
        return clean.length() <= 220 ? clean : clean.substring(0, 220).trim();
    }

    private String formatStatusFailure(String providerName, int statusCode, byte[] body) {
        String suffix = extractTextSnippet(body);
        if (!suffix.isBlank()) {
            return statusCode + " via " + providerName + " (" + suffix + ")";
        }
        return statusCode + " via " + providerName;
    }

    private String formatNonImageFailure(String providerName, String contentType, byte[] body) {
        String normalizedType = contentType == null || contentType.isBlank() ? "unknown" : contentType;
        String suffix = extractTextSnippet(body);
        if (!suffix.isBlank()) {
            return "non-image response via " + providerName + " [" + normalizedType + "] (" + suffix + ")";
        }
        return "non-image response via " + providerName + " [" + normalizedType + "]";
    }

    private String extractTextSnippet(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }
        String text = new String(body, 0, Math.min(body.length, 220), StandardCharsets.UTF_8)
                .replaceAll("\\s+", " ")
                .replaceAll("[<>]", "")
                .trim();
        return text.length() <= 90 ? text : text.substring(0, 90).trim() + "...";
    }

    private ImageKind detectImageKind(String contentType, byte[] body) {
        if (body == null || body.length < 8) {
            return ImageKind.UNKNOWN;
        }
        if (contentType.startsWith("image/jpeg") || isJpeg(body)) {
            return ImageKind.JPEG;
        }
        if (contentType.startsWith("image/png") || isPng(body)) {
            return ImageKind.PNG;
        }
        if (contentType.startsWith("image/gif") || isGif(body)) {
            return ImageKind.GIF;
        }
        if (contentType.startsWith("image/webp") || isWebp(body)) {
            return ImageKind.WEBP;
        }
        return ImageKind.UNKNOWN;
    }

    private boolean isJpeg(byte[] body) {
        return body.length >= 3
                && (body[0] & 0xFF) == 0xFF
                && (body[1] & 0xFF) == 0xD8
                && (body[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] body) {
        return body.length >= 8
                && (body[0] & 0xFF) == 0x89
                && body[1] == 0x50
                && body[2] == 0x4E
                && body[3] == 0x47
                && body[4] == 0x0D
                && body[5] == 0x0A
                && body[6] == 0x1A
                && body[7] == 0x0A;
    }

    private boolean isGif(byte[] body) {
        if (body.length < 6) {
            return false;
        }
        String header = new String(body, 0, 6, StandardCharsets.US_ASCII);
        return "GIF87a".equals(header) || "GIF89a".equals(header);
    }

    private boolean isWebp(byte[] body) {
        return body.length >= 12
                && body[0] == 'R'
                && body[1] == 'I'
                && body[2] == 'F'
                && body[3] == 'F'
                && body[8] == 'W'
                && body[9] == 'E'
                && body[10] == 'B'
                && body[11] == 'P';
    }

    private String extractHostLabel(String url) {
        try {
            URI uri = URI.create(url);
            return Optional.ofNullable(uri.getHost()).orElse("provider");
        } catch (IllegalArgumentException exception) {
            return "provider";
        }
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String encodeQueryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String slugify(String value) {
        String normalized = safeText(value, "course")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "course" : normalized;
    }

    private String shortenSlug(String value) {
        String slug = slugify(value);
        if (slug.length() <= 48) {
            return slug;
        }
        return slug.substring(0, 48).replaceAll("(^-+|-+$)", "");
    }

    private String readSettingOrDefault(String key, String fallback) {
        String value = AppSettings.get(key, "");
        return value.isBlank() ? fallback : value.trim();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trimTrailingSlash(String value) {
        String safeValue = safeText(value, DEFAULT_BASE_URL);
        return safeValue.endsWith("/") ? safeValue.substring(0, safeValue.length() - 1) : safeValue;
    }

    private enum ImageKind {
        JPEG(".jpg"),
        PNG(".png"),
        GIF(".gif"),
        WEBP(".webp"),
        UNKNOWN("");

        private final String extension;

        ImageKind(String extension) {
            this.extension = extension;
        }

        public String extension() {
            return extension;
        }
    }

    private record RequestTarget(String name, String url) {
    }
}
