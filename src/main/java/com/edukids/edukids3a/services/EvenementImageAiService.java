package com.edukids.edukids3a.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Produit une URL d’image générée à partir du titre / description d’un événement.
 * <p>Utilise l’API <a href="https://pollinations.ai">Pollinations</a> (pas de clé requise, usage raisonnable).
 * L’URL renvoyée peut être stockée telle quelle dans {@code evenement.image} et affichée par JavaFX.
 */
public final class EvenementImageAiService {

    private static final Logger LOG = LoggerFactory.getLogger(EvenementImageAiService.class);

    /** Limiter la longueur du prompt (navigateur / serveur). */
    private static final int MAX_DESC_SNIPPET = 420;
    private static final int MAX_PROMPT = 900;

    private static final String BASE = "https://image.pollinations.ai/prompt/";

    /**
     * @param titre       titre (peut être vide si description fournie)
     * @param description texte (tronqué si trop long)
     * @return URL https pointant vers l’image générée
     * @throws IllegalArgumentException si titre et description sont vides
     */
    public String buildImageUrlFromEvenementText(String titre, String description) {
        String t = titre == null ? "" : titre.trim();
        String d = description == null ? "" : description.trim()
                .replaceAll("\\s+", " ");
        if (d.length() > MAX_DESC_SNIPPET) {
            d = d.substring(0, MAX_DESC_SNIPPET) + "…";
        }
        if (t.isEmpty() && d.isEmpty()) {
            throw new IllegalArgumentException("Saisissez au moins un titre ou une description.");
        }
        String prompt = buildEnglishPrompt(t, d);
        if (prompt.length() > MAX_PROMPT) {
            prompt = prompt.substring(0, MAX_PROMPT);
        }
        String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
        // Paramètres : taille utile pour cartes + pas de logo intégré
        String url = BASE + encoded + "?width=800&height=400&nologo=true";
        if (LOG.isDebugEnabled()) {
            LOG.debug("URL image IA (tronqué): {}", url.substring(0, Math.min(120, url.length())));
        }
        return url;
    }

    /**
     * Plusieurs URLs différentes (seed + légère variation de prompt) pour laisser l’utilisateur en choisir une seule.
     *
     * @param nombre nombre de propositions (2 à 6)
     */
    public List<String> buildAlbumImageUrls(String titre, String description, int nombre) {
        String t = titre == null ? "" : titre.trim();
        String d = description == null ? "" : description.trim().replaceAll("\\s+", " ");
        if (d.length() > MAX_DESC_SNIPPET) {
            d = d.substring(0, MAX_DESC_SNIPPET) + "…";
        }
        if (t.isEmpty() && d.isEmpty()) {
            throw new IllegalArgumentException("Saisissez au moins un titre ou une description.");
        }
        if (nombre < 2 || nombre > 6) {
            throw new IllegalArgumentException("L’album doit proposer entre 2 et 6 images.");
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<String> urls = new ArrayList<>(nombre);
        String[] albumStyles = albumStyleSuffixes();
        for (int i = 0; i < nombre; i++) {
            String prompt = buildEnglishPrompt(t, d);
            prompt = prompt + " " + albumStyles[i % albumStyles.length];
            if (prompt.length() > MAX_PROMPT) {
                prompt = prompt.substring(0, MAX_PROMPT);
            }
            String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
            int seed = rnd.nextInt(1, Integer.MAX_VALUE);
            // largeurs légèrement différentes pour éviter le cache serveur identique entre vignettes
            int w = 720 + i * 24;
            int h = 400 + i * 8;
            urls.add(BASE + encoded + "?width=" + w + "&height=" + h + "&nologo=true&seed=" + seed);
        }
        return urls;
    }

    /** Variantes visuelles nettement différentes pour que chaque URL génère une image distincte. */
    private static String[] albumStyleSuffixes() {
        return new String[]{
                "Composition 1: wide establishing shot, outdoor or schoolyard, open space, horizon visible.",
                "Composition 2: medium shot, small group of cheerful cartoon children, playful activity, props.",
                "Composition 3: symbolic flat illustration, books paint music colorful shapes, tiny stylized figures.",
                "Composition 4: festive evening scene, soft lights balloons bunting, warm cozy atmosphere."
        };
    }

    /**
     * Prompt en anglais : meilleurs résultats avec les modèles d’imagerie courants.
     */
    private static String buildEnglishPrompt(String titre, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bright, friendly, educational event illustration for children and families, ");
        sb.append("clean modern digital art, soft colors, no text, no letters, no watermark, ");
        sb.append("single scene, not photorealistic portrait of a real person. ");
        if (!titre.isEmpty()) {
            sb.append("Theme: ");
            sb.append(titre);
            sb.append(". ");
        }
        if (!description.isEmpty()) {
            sb.append("Mood and details: ");
            sb.append(description);
            sb.append(". ");
        }
        return sb.toString().strip();
    }
}
