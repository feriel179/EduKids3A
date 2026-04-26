package com.edukids.edukids3a.services;

import com.edukids.edukids3a.util.DotEnvLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Génération assistée des activités de programme.
 * <p>
 * Version locale (sans clé API) inspirée du fallback web : planification
 * d'activités cohérentes avec le thème de l'événement, formatée ligne par ligne.
 */
public final class ProgrammeActivitesAiService {

    private static final Logger LOG = LoggerFactory.getLogger(ProgrammeActivitesAiService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(20))
            .build();

    public String genererActivites(String titre, String description, LocalTime eventDebut, LocalTime eventFin,
                                   LocalTime pauseDebut, LocalTime pauseFin) {
        if (eventDebut == null || eventFin == null || !eventFin.isAfter(eventDebut)) {
            throw new IllegalArgumentException("L'événement doit avoir un intervalle horaire valide.");
        }

        String groqKey = getGroqApiKey();
        if (groqKey != null && !groqKey.isBlank()) {
            try {
                String ai = genererAvecGroq(titre, description, eventDebut, eventFin, pauseDebut, pauseFin, groqKey);
                if (ai != null && !ai.isBlank()) {
                    return ai;
                }
            } catch (Exception ex) {
                LOG.warn("Génération Groq indisponible, fallback local: {}", ex.getMessage());
            }
        } else {
            LOG.info("GROQ_API_KEY absente -> génération locale.");
        }

        String t = titre == null ? "" : titre.trim();
        String d = description == null ? "" : description.trim();
        String contexte = normaliser(t + " " + d);

        int start = toMinutes(eventDebut);
        int end = toMinutes(eventFin);
        Integer breakStart = (pauseDebut != null) ? toMinutes(pauseDebut) : null;
        Integer breakEnd = (pauseFin != null) ? toMinutes(pauseFin) : null;
        boolean hasPause = breakStart != null && breakEnd != null && breakEnd > breakStart;

        List<String> lignes = new ArrayList<>();
        int cursor = start;

        int accueilEnd = Math.min(end, cursor + 15);
        lignes.add(formatLigne(cursor, accueilEnd, "Accueil des participants et présentation de l'événement"));
        cursor = accueilEnd;

        int idx = 1;
        while (cursor < end - 15) {
            if (hasPause && cursor >= breakStart && cursor < breakEnd) {
                cursor = breakEnd;
                continue;
            }

            int slot = 45;
            if (hasPause && cursor < breakStart && cursor + slot > breakStart) {
                slot = breakStart - cursor;
            }
            if (cursor + slot > end - 15) {
                slot = end - 15 - cursor;
            }
            if (slot < 15) {
                if (hasPause && cursor < breakStart) {
                    cursor = breakEnd;
                    continue;
                }
                break;
            }

            int next = cursor + slot;
            lignes.add(formatLigne(cursor, next, "Activité " + idx + " - " + nomActiviteTheme(contexte, idx)));
            idx++;
            cursor = next;
        }

        if (cursor < end) {
            int closeStart = Math.max(cursor, end - 15);
            lignes.add(formatLigne(closeStart, end, "Clôture, bilan et remise des certificats"));
        }

        return String.join("\n", lignes);
    }

    private String genererAvecGroq(String titre, String description, LocalTime eventDebut, LocalTime eventFin,
                                   LocalTime pauseDebut, LocalTime pauseFin, String apiKey)
            throws IOException, InterruptedException {
        String prompt = buildPrompt(titre, description, eventDebut, eventFin, pauseDebut, pauseFin);
        String body = "{"
                + "\"model\":\"llama-3.1-70b-versatile\","
                + "\"temperature\":0.7,"
                + "\"max_tokens\":1300,"
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"Tu es un expert en organisation d'evenements educatifs.\"},"
                + "{\"role\":\"user\",\"content\":\"" + jsonEscape(prompt) + "\"}"
                + "]"
                + "}";

        HttpRequest req = HttpRequest.newBuilder(URI.create(GROQ_URL))
                .timeout(java.time.Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            LOG.warn("Groq HTTP status {}: {}", resp.statusCode(), truncate(resp.body(), 220));
            return null;
        }
        String content = extractAssistantContent(resp.body());
        if (content == null || content.isBlank()) {
            return null;
        }
        return normaliserSortieActivites(content);
    }

    private static String buildPrompt(String titre, String description, LocalTime eventDebut, LocalTime eventFin,
                                      LocalTime pauseDebut, LocalTime pauseFin) {
        String t = (titre == null || titre.isBlank()) ? "Événement" : titre.trim();
        String d = (description == null) ? "" : description.trim();
        String pause = "";
        if (pauseDebut != null && pauseFin != null && pauseFin.isAfter(pauseDebut)) {
            pause = "Pause: " + pauseDebut.format(TIME_FMT) + " à " + pauseFin.format(TIME_FMT) + ".";
        }
        return "Crée un programme d'activités pour cet événement.\n"
                + "Titre: " + t + "\n"
                + "Description: " + d + "\n"
                + "Horaires: " + eventDebut.format(TIME_FMT) + " à " + eventFin.format(TIME_FMT) + ".\n"
                + pause + "\n"
                + "Contraintes: format EXACT '- De HH:MM à HH:MM : Description', une activité par ligne, "
                + "activités cohérentes avec le thème, pas de chevauchement, fin au plus tard à "
                + eventFin.format(TIME_FMT) + ".";
    }

    private static String normaliserSortieActivites(String raw) {
        String[] lines = raw.replace("\r", "").split("\n");
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            String l = line.trim();
            if (l.isBlank()) {
                continue;
            }
            if (!l.startsWith("-")) {
                if (l.matches("(?i)^De\\s+\\d{2}:\\d{2}\\s+à\\s+\\d{2}:\\d{2}\\s*:.*")) {
                    l = "- " + l;
                }
            }
            out.add(l);
        }
        return String.join("\n", out);
    }

    private static String extractAssistantContent(String json) {
        if (json == null) {
            return null;
        }
        String marker = "\"content\":\"";
        int i = json.indexOf(marker);
        if (i < 0) {
            return null;
        }
        int start = i + marker.length();
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int p = start; p < json.length(); p++) {
            char c = json.charAt(p);
            if (esc) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
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

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String getGroqApiKey() {
        String v = DotEnvLoader.get("GROQ_API_KEY");
        return v != null && !v.isBlank() ? v.trim() : null;
    }

    private static String formatLigne(int startMinutes, int endMinutes, String label) {
        return "- De " + toTime(startMinutes) + " à " + toTime(endMinutes) + " : " + label;
    }

    private static int toMinutes(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    private static String toTime(int minutes) {
        int h = Math.floorDiv(minutes, 60);
        int m = Math.floorMod(minutes, 60);
        return LocalTime.of(Math.max(0, Math.min(23, h)), m).format(TIME_FMT);
    }

    private static String normaliser(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replace('à', 'a').replace('á', 'a').replace('â', 'a').replace('ä', 'a')
                .replace('è', 'e').replace('é', 'e').replace('ê', 'e').replace('ë', 'e')
                .replace('ì', 'i').replace('í', 'i').replace('î', 'i').replace('ï', 'i')
                .replace('ò', 'o').replace('ó', 'o').replace('ô', 'o').replace('ö', 'o')
                .replace('ù', 'u').replace('ú', 'u').replace('û', 'u').replace('ü', 'u')
                .replace('ç', 'c');
    }

    private static String nomActiviteTheme(String ctx, int n) {
        String[] tech = {
                "Workshop programmation et coding",
                "Atelier création d'interface web",
                "Pratique API, données et intégration",
                "Mini-projet full stack guidé"
        };
        String[] art = {
                "Atelier créatif et artistique",
                "Peinture et expression visuelle",
                "Création manuelle en équipe",
                "Exposition des productions"
        };
        String[] sport = {
                "Parcours moteur et défis sportifs",
                "Jeux d'équipe et coordination",
                "Atelier mobilité et équilibre",
                "Challenge collectif"
        };
        String[] science = {
                "Expériences scientifiques guidées",
                "Atelier découverte et observation",
                "Mini-laboratoire en petits groupes",
                "Partage des résultats"
        };
        String[] generic = {
                "Jeu éducatif interactif",
                "Atelier pratique en groupe",
                "Activité collaborative",
                "Session de restitution"
        };

        String[] set = generic;
        if (containsAny(ctx, "web", "code", "programm", "full stack", "frontend", "backend", "api", "develop")) {
            set = tech;
        } else if (containsAny(ctx, "art", "peinture", "dessin", "creati", "artisan")) {
            set = art;
        } else if (containsAny(ctx, "sport", "football", "basket", "danse", "gym", "course")) {
            set = sport;
        } else if (containsAny(ctx, "science", "experience", "laboratoire", "robot", "nature")) {
            set = science;
        }
        return set[(Math.max(1, n) - 1) % set.length];
    }

    private static boolean containsAny(String s, String... terms) {
        for (String term : terms) {
            if (s.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
