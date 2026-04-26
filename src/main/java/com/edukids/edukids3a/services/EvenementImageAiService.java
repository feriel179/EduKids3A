package com.edukids.edukids3a.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Recommandation d'image pour événement, alignée sur le web :
 * Gemini (mots-clés) + Pexels (image), avec fallback local.
 */
public final class EvenementImageAiService {

    private static final Logger LOG = LoggerFactory.getLogger(EvenementImageAiService.class);
    private static final int MAX_DESC_SNIPPET = 420;
    private static final String GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String[] GEMINI_MODELS = {"gemini-1.5-flash", "gemini-1.5-flash-latest", "gemini-pro"};

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(20))
            .build();

    public String buildImageUrlFromEvenementText(String titre, String description) {
        String[] td = normalizeInput(titre, description);
        String t = td[0];
        String d = td[1];
        if (t.isEmpty() && d.isEmpty()) {
            throw new IllegalArgumentException("Saisissez au moins un titre ou une description.");
        }
        List<String> keywords = generateKeywords(t, d);
        String image = fetchSingleImageFromPexels(keywords);
        if (image != null) {
            return image;
        }
        return picsumUrlFromSeed(keywordSeed(keywords), 800, 420);
    }

    public List<String> buildAlbumImageUrls(String titre, String description, int nombre) {
        String[] td = normalizeInput(titre, description);
        String t = td[0];
        String d = td[1];
        if (t.isEmpty() && d.isEmpty()) {
            throw new IllegalArgumentException("Saisissez au moins un titre ou une description.");
        }
        if (nombre < 2 || nombre > 6) {
            throw new IllegalArgumentException("L’album doit proposer entre 2 et 6 images.");
        }
        List<String> keywords = generateKeywords(t, d);
        List<String> urls = fetchAlbumFromPexels(keywords, nombre);
        if (urls.size() >= nombre) {
            return urls;
        }
        int baseSeed = keywordSeed(keywords);
        while (urls.size() < nombre) {
            int i = urls.size();
            int w = 760 + (i * 18);
            int h = 420 + (i * 10);
            String fallback = picsumUrlFromSeed(Math.abs(baseSeed + i * 97), w, h);
            if (!urls.contains(fallback)) {
                urls.add(fallback);
            }
        }
        return urls;
    }

    private List<String> generateKeywords(String titre, String description) {
        String geminiKey = readKeyFromDotEnv("GEMINI_API_KEY");
        if (geminiKey == null || geminiKey.isBlank()) {
            return fallbackKeywords(titre, description);
        }
        String prompt = "Generate 6-10 English keywords for event image search. "
                + "Return keywords only, comma-separated.\n"
                + "Title: " + titre + "\n"
                + "Description: " + description + "\nKeywords:";
        for (String model : GEMINI_MODELS) {
            try {
                String endpoint = GEMINI_BASE + model + ":generateContent?key=" + URLEncoder.encode(geminiKey, StandardCharsets.UTF_8);
                String payload = "{\"contents\":[{\"parts\":[{\"text\":\"" + jsonEscape(prompt) + "\"}]}]}";
                HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint))
                        .timeout(java.time.Duration.ofSeconds(20))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    continue;
                }
                String text = extractFirstJsonValue(resp.body(), "\"text\":\"");
                List<String> parsed = parseKeywords(text);
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            } catch (Exception ignored) {
                // try next model
            }
        }
        return fallbackKeywords(titre, description);
    }

    private String fetchSingleImageFromPexels(List<String> keywords) {
        List<String> urls = fetchAlbumFromPexels(keywords, 1);
        return urls.isEmpty() ? null : urls.get(0);
    }

    private List<String> fetchAlbumFromPexels(List<String> keywords, int nombre) {
        String pexelsKey = readKeyFromDotEnv("PEXELS_API_KEY");
        if (pexelsKey == null || pexelsKey.isBlank()) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>();
        try {
            String query = String.join(" ", keywords.stream().limit(4).toList());
            if (query.isBlank()) {
                query = "children educational event";
            }
            String url = "https://api.pexels.com/v1/search?query="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&per_page=" + Math.max(1, Math.min(12, nombre * 3))
                    + "&orientation=landscape&size=large";
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .header("Authorization", pexelsKey)
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                LOG.warn("Pexels HTTP status {}", resp.statusCode());
                return out;
            }
            String json = resp.body();
            int idx = 0;
            while (out.size() < nombre) {
                int p1 = json.indexOf("\"large2x\":\"", idx);
                int p2 = json.indexOf("\"large\":\"", idx);
                int p3 = json.indexOf("\"medium\":\"", idx);
                int p = firstPositiveMin(p1, p2, p3);
                if (p < 0) {
                    break;
                }
                String marker = (p == p1) ? "\"large2x\":\"" : (p == p2) ? "\"large\":\"" : "\"medium\":\"";
                String imageUrl = extractJsonValueAt(json, p + marker.length());
                idx = p + marker.length();
                if (imageUrl != null && !imageUrl.isBlank() && !out.contains(imageUrl)) {
                    out.add(imageUrl);
                }
            }
        } catch (Exception ex) {
            LOG.warn("Pexels indisponible: {}", ex.getMessage());
        }
        return out;
    }

    private static int keywordSeed(List<String> keywords) {
        String q = String.join(" ", keywords);
        if (q.isBlank()) {
            q = "edukids-event";
        }
        return Math.abs(q.hashCode());
    }

    private static String picsumUrlFromSeed(int seed, int width, int height) {
        return "https://picsum.photos/seed/" + Math.abs(seed) + "/" + width + "/" + height;
    }

    private static List<String> fallbackKeywords(String titre, String description) {
        String txt = (titre + " " + description).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if (containsAny(txt, "workshop", "programm", "web", "code", "develop")) {
            out.add("coding workshop");
            out.add("children learning programming");
            out.add("educational technology class");
        } else if (containsAny(txt, "art", "paint", "dessin", "creat")) {
            out.add("children art workshop");
            out.add("creative painting class");
            out.add("family art event");
        } else if (containsAny(txt, "sport", "football", "basket", "course", "gym")) {
            out.add("kids sports activity");
            out.add("children team games");
            out.add("family sports event");
        } else {
            out.add("children educational event");
            out.add("family activity");
            out.add("kids workshop");
        }
        out.add("friendly illustration");
        out.add("school event");
        return out;
    }

    private static boolean containsAny(String txt, String... terms) {
        for (String t : terms) {
            if (txt.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private static String[] normalizeInput(String titre, String description) {
        String t = titre == null ? "" : titre.trim();
        String d = description == null ? "" : description.trim().replaceAll("\\s+", " ");
        if (d.length() > MAX_DESC_SNIPPET) {
            d = d.substring(0, MAX_DESC_SNIPPET) + "…";
        }
        return new String[]{t, d};
    }

    private static List<String> parseKeywords(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        String[] parts = text.split(",");
        for (String p : parts) {
            String k = p.trim().toLowerCase(Locale.ROOT);
            k = k.replaceAll("^[\\-•\\d\\.\\)\\s]+", "");
            k = k.replaceAll("[\\.;:!\\?]+$", "");
            if (!k.isBlank() && k.length() > 2 && !out.contains(k)) {
                out.add(k);
            }
            if (out.size() >= 10) {
                break;
            }
        }
        return out;
    }

    private static String readKeyFromDotEnv(String key) {
        String v = readValueFromFile(Path.of(".env.local"), key);
        if (v != null && !v.isBlank()) {
            return v.trim();
        }
        v = readValueFromFile(Path.of(".env"), key);
        if (v != null && !v.isBlank()) {
            return v.trim();
        }
        return null;
    }

    private static String readValueFromFile(Path path, String key) {
        try {
            if (!Files.isRegularFile(path)) {
                return null;
            }
            String prefix = key + "=";
            for (String rawLine : Files.readAllLines(path)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.startsWith(prefix)) {
                    continue;
                }
                String value = line.substring(prefix.length()).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        } catch (IOException ignored) {
            return null;
        }
        return null;
    }

    private static String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static String extractFirstJsonValue(String json, String marker) {
        int i = json.indexOf(marker);
        if (i < 0) {
            return null;
        }
        return extractJsonValueAt(json, i + marker.length());
    }

    private static String extractJsonValueAt(String json, int start) {
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (esc) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    default -> sb.append(c);
                }
                esc = false;
                continue;
            }
            if (c == '\\') {
                esc = true;
                continue;
            }
            if (c == '"') {
                return sb.toString();
            }
            sb.append(c);
        }
        return null;
    }

    private static int firstPositiveMin(int... vals) {
        int min = Integer.MAX_VALUE;
        for (int v : vals) {
            if (v >= 0 && v < min) {
                min = v;
            }
        }
        return min == Integer.MAX_VALUE ? -1 : min;
    }
}
