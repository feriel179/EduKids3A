package com.edukids.edukids3a.service;

import com.edukids.edukids3a.model.Quiz;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuizImageGeneratorService {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final Path OUTPUT_DIR = Path.of("data", "generated-images");
    private static final DateTimeFormatter FILE_SUFFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public String generateQuizCover(Quiz quiz) throws IOException {
        Files.createDirectories(OUTPUT_DIR);

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            Palette palette = choosePalette(quiz);
            paintBackground(graphics, palette);
            paintMainPanel(graphics, palette);
            paintBadges(graphics, quiz, palette);
            paintTitleAndDescription(graphics, quiz, palette);
            paintStatChips(graphics, quiz, palette);
            paintPlayArea(graphics, quiz, palette);
        } finally {
            graphics.dispose();
        }

        Path outputPath = OUTPUT_DIR.resolve(buildFileName(quiz));
        ImageIO.write(image, "png", outputPath.toFile());
        return outputPath.toAbsolutePath().toUri().toString();
    }

    private void paintBackground(Graphics2D graphics, Palette palette) {
        graphics.setPaint(new GradientPaint(0, 0, palette.backgroundStart(), WIDTH, HEIGHT, palette.backgroundEnd()));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        graphics.setColor(withAlpha(palette.accentStrong(), 65));
        graphics.fill(new Ellipse2D.Double(-120, -100, 420, 320));
        graphics.fill(new Ellipse2D.Double(910, 40, 320, 260));
        graphics.fill(new Ellipse2D.Double(980, 510, 250, 190));

        graphics.setColor(withAlpha(palette.accentSoft(), 48));
        graphics.fill(new Ellipse2D.Double(60, 520, 320, 210));
        graphics.fill(new Ellipse2D.Double(760, -70, 260, 180));

        for (int index = 0; index < 16; index++) {
            int x = 60 + (index * 74) % 1160;
            int y = 52 + (index * 91) % 620;
            int size = index % 3 == 0 ? 14 : 8;
            graphics.setColor(withAlpha(Color.WHITE, index % 3 == 0 ? 110 : 75));
            paintStar(graphics, x, y, size);
        }
    }

    private void paintMainPanel(Graphics2D graphics, Palette palette) {
        RoundRectangle2D shadow = new RoundRectangle2D.Double(74, 86, 760, 560, 44, 44);
        graphics.setColor(withAlpha(new Color(10, 16, 28), 60));
        graphics.fill(shadow);

        RoundRectangle2D mainPanel = new RoundRectangle2D.Double(58, 68, 760, 560, 44, 44);
        graphics.setColor(withAlpha(palette.panel(), 232));
        graphics.fill(mainPanel);

        graphics.setColor(withAlpha(Color.WHITE, 36));
        graphics.setStroke(new BasicStroke(2f));
        graphics.draw(mainPanel);
    }

    private void paintBadges(Graphics2D graphics, Quiz quiz, Palette palette) {
        drawChip(graphics, 94, 106, 158, 50, "EduKids Quiz", palette.accentStrong(), Color.WHITE, 24f);
        drawChip(graphics, 266, 106, 182, 50, valueOrDefault(quiz.getCategorieAge(), "Tous les ages"), withAlpha(Color.WHITE, 34), palette.textPrimary(), 22f);
        drawChip(graphics, 94, 174, 142, 46, "Niveau " + valueOrDefault(quiz.getNiveau(), "Fun"), withAlpha(Color.WHITE, 24), palette.textSecondary(), 20f);
        drawChip(graphics, 250, 174, 126, 46, safeCount(quiz.getNombreQuestions()) + " questions", withAlpha(Color.WHITE, 24), palette.textSecondary(), 20f);
    }

    private void paintTitleAndDescription(Graphics2D graphics, Quiz quiz, Palette palette) {
        String title = valueOrDefault(quiz.getTitre(), "Quiz magique");
        int titleSize = title.length() > 40 ? 54 : 64;
        Font titleFont = new Font("SansSerif", Font.BOLD, titleSize);
        graphics.setFont(titleFont);
        graphics.setColor(palette.textPrimary());
        List<String> titleLines = wrapText(graphics, title, titleFont, 620, 3);
        int titleY = 282;
        for (String line : titleLines) {
            graphics.drawString(line, 96, titleY);
            titleY += graphics.getFontMetrics().getHeight() + 4;
        }

        String description = buildDescription(quiz);
        Font descriptionFont = new Font("SansSerif", Font.PLAIN, 28);
        graphics.setFont(descriptionFont);
        graphics.setColor(palette.textSecondary());
        List<String> descriptionLines = wrapText(graphics, description, descriptionFont, 620, 4);
        int descriptionY = titleY + 28;
        int lineHeight = graphics.getFontMetrics().getHeight() + 4;
        for (String line : descriptionLines) {
            graphics.drawString(line, 96, descriptionY);
            descriptionY += lineHeight;
        }
    }

    private void paintStatChips(Graphics2D graphics, Quiz quiz, Palette palette) {
        int startY = 528;
        drawStatCard(
                graphics,
                96,
                startY,
                192,
                90,
                "Duree",
                safeCount(quiz.getDureeMinutes()) + " min",
                palette
        );
        drawStatCard(
                graphics,
                306,
                startY,
                192,
                90,
                "Objectif",
                safeCount(quiz.getScoreMinimum()) + "%",
                palette
        );
        drawStatCard(
                graphics,
                516,
                startY,
                240,
                90,
                "Statut",
                valueOrDefault(quiz.getStatut(), "Pret a jouer"),
                palette
        );
    }

    private void paintPlayArea(Graphics2D graphics, Quiz quiz, Palette palette) {
        RoundRectangle2D stage = new RoundRectangle2D.Double(868, 122, 320, 454, 38, 38);
        graphics.setColor(withAlpha(new Color(16, 22, 38), 162));
        graphics.fill(stage);

        graphics.setColor(withAlpha(Color.WHITE, 32));
        graphics.setStroke(new BasicStroke(2f));
        graphics.draw(stage);

        graphics.setColor(withAlpha(palette.accentStrong(), 92));
        graphics.fill(new Ellipse2D.Double(930, 152, 192, 192));
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 82));
        drawCenteredText(graphics, "?", 1026, 275);

        graphics.setFont(new Font("SansSerif", Font.BOLD, 24));
        graphics.setColor(Color.WHITE);
        drawCenteredText(graphics, buildThemeLabel(quiz), 1028, 338);

        String[] answers = {
                "1. J'apprends",
                "2. Je joue",
                "3. Je relie",
                "4. Je reussis"
        };
        int cardY = 374;
        for (int index = 0; index < answers.length; index++) {
            Color cardColor = index % 2 == 0 ? withAlpha(palette.accentStrong(), 230) : withAlpha(palette.accentSoft(), 214);
            drawAnswerCard(graphics, 914, cardY + index * 54, 228, 42, answers[index], cardColor);
        }

        graphics.setColor(withAlpha(Color.WHITE, 58));
        graphics.setStroke(new BasicStroke(4f));
        graphics.drawArc(882, 78, 210, 140, 12, 146);
        graphics.drawArc(1032, 88, 146, 126, 32, 134);
        graphics.drawArc(1048, 534, 126, 94, 180, 118);
    }

    private void drawStatCard(Graphics2D graphics, int x, int y, int width, int height, String label, String value, Palette palette) {
        RoundRectangle2D card = new RoundRectangle2D.Double(x, y, width, height, 28, 28);
        graphics.setColor(withAlpha(Color.WHITE, 22));
        graphics.fill(card);

        graphics.setColor(withAlpha(Color.WHITE, 30));
        graphics.setStroke(new BasicStroke(1.8f));
        graphics.draw(card);

        graphics.setFont(new Font("SansSerif", Font.PLAIN, 20));
        graphics.setColor(withAlpha(palette.textSecondary(), 220));
        graphics.drawString(label, x + 22, y + 30);

        graphics.setFont(new Font("SansSerif", Font.BOLD, 30));
        graphics.setColor(palette.textPrimary());
        graphics.drawString(value, x + 22, y + 66);
    }

    private void drawAnswerCard(Graphics2D graphics, int x, int y, int width, int height, String text, Color fillColor) {
        RoundRectangle2D card = new RoundRectangle2D.Double(x, y, width, height, 22, 22);
        graphics.setColor(fillColor);
        graphics.fill(card);

        graphics.setColor(withAlpha(Color.WHITE, 52));
        graphics.setStroke(new BasicStroke(1.2f));
        graphics.draw(card);

        graphics.setFont(new Font("SansSerif", Font.BOLD, 19));
        graphics.setColor(Color.WHITE);
        graphics.drawString(text, x + 18, y + 28);
    }

    private void drawChip(Graphics2D graphics, int x, int y, int width, int height, String text, Color background, Color foreground, float fontSize) {
        RoundRectangle2D chip = new RoundRectangle2D.Double(x, y, width, height, 26, 26);
        graphics.setColor(background);
        graphics.fill(chip);

        graphics.setFont(new Font("SansSerif", Font.BOLD, Math.round(fontSize)));
        graphics.setColor(foreground);
        drawCenteredText(graphics, text, x + width / 2, y + height / 2 + 7);
    }

    private void drawCenteredText(Graphics2D graphics, String text, int centerX, int baselineY) {
        FontMetrics metrics = graphics.getFontMetrics();
        int width = metrics.stringWidth(text);
        graphics.drawString(text, centerX - (width / 2), baselineY);
    }

    private void paintStar(Graphics2D graphics, double centerX, double centerY, double radius) {
        Path2D.Double star = new Path2D.Double();
        for (int index = 0; index < 10; index++) {
            double angle = Math.PI / 5d * index - Math.PI / 2d;
            double currentRadius = index % 2 == 0 ? radius : radius * 0.45d;
            double x = centerX + Math.cos(angle) * currentRadius;
            double y = centerY + Math.sin(angle) * currentRadius;
            if (index == 0) {
                star.moveTo(x, y);
            } else {
                star.lineTo(x, y);
            }
        }
        star.closePath();
        graphics.fill(star);
    }

    private List<String> wrapText(Graphics2D graphics, String text, Font font, int maxWidth, int maxLines) {
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (metrics.stringWidth(candidate) <= maxWidth) {
                currentLine.setLength(0);
                currentLine.append(candidate);
                continue;
            }
            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }
            currentLine.setLength(0);
            currentLine.append(word);
            if (lines.size() == maxLines - 1) {
                break;
            }
        }
        if (!currentLine.isEmpty() && lines.size() < maxLines) {
            lines.add(currentLine.toString());
        }
        if (lines.isEmpty()) {
            lines.add(text);
        }
        if (lines.size() == maxLines) {
            String original = String.join(" ", text.trim().split("\\s+"));
            String consumed = String.join(" ", lines);
            if (!original.equals(consumed)) {
                String lastLine = lines.get(lines.size() - 1);
                while (!lastLine.isEmpty() && metrics.stringWidth(lastLine + "...") > maxWidth) {
                    lastLine = lastLine.substring(0, lastLine.length() - 1).trim();
                }
                lines.set(lines.size() - 1, lastLine + "...");
            }
        }
        return lines;
    }

    private Palette choosePalette(Quiz quiz) {
        String category = quiz.getCategorieAge() == null ? "" : quiz.getCategorieAge().toLowerCase(Locale.ROOT);
        String level = quiz.getNiveau() == null ? "" : quiz.getNiveau().toLowerCase(Locale.ROOT);
        if (category.contains("8-10")) {
            return new Palette(
                    new Color(251, 116, 72),
                    new Color(255, 201, 71),
                    new Color(51, 166, 255),
                    new Color(255, 88, 146),
                    new Color(28, 31, 56),
                    new Color(58, 67, 104),
                    Color.WHITE,
                    new Color(225, 233, 247)
            );
        }
        if (level.contains("avance")) {
            return new Palette(
                    new Color(23, 92, 161),
                    new Color(20, 154, 143),
                    new Color(255, 173, 71),
                    new Color(75, 208, 170),
                    new Color(16, 26, 46),
                    new Color(42, 56, 88),
                    Color.WHITE,
                    new Color(216, 230, 248)
            );
        }
        return new Palette(
                new Color(63, 94, 251),
                new Color(70, 189, 198),
                new Color(255, 145, 77),
                new Color(108, 99, 255),
                new Color(22, 30, 52),
                new Color(47, 60, 92),
                Color.WHITE,
                new Color(221, 230, 246)
        );
    }

    private String buildDescription(Quiz quiz) {
        String description = quiz.getDescription() == null ? "" : quiz.getDescription().trim();
        if (!description.isBlank()) {
            return description;
        }
        if (Quiz.CATEGORIE_AGE_FACILE.equalsIgnoreCase(quiz.getCategorieAge())) {
            return "Une aventure de questions simples, ludiques et colorees pour apprendre en s'amusant.";
        }
        return "Un quiz dynamique pour progresser pas a pas, renforcer ses connaissances et gagner en confiance.";
    }

    private String buildThemeLabel(Quiz quiz) {
        String title = valueOrDefault(quiz.getTitre(), "theme surprise");
        String[] pieces = title.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < Math.min(pieces.length, 2); index++) {
            if (index > 0) {
                builder.append(' ');
            }
            builder.append(capitalize(pieces[index]));
        }
        String label = builder.toString();
        return label.length() > 16 ? label.substring(0, 16) + "..." : label;
    }

    private String buildFileName(Quiz quiz) {
        String slug = slugify(valueOrDefault(quiz.getTitre(), "quiz"));
        return slug + "-" + LocalDateTime.now().format(FILE_SUFFIX_FORMATTER) + ".png";
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            return "quiz";
        }
        if (slug.length() > 36) {
            return slug.substring(0, 36).replaceAll("-+$", "");
        }
        return slug;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int safeCount(int value) {
        return Math.max(0, value);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private record Palette(
            Color backgroundStart,
            Color backgroundEnd,
            Color accentStrong,
            Color accentSoft,
            Color panel,
            Color chip,
            Color textPrimary,
            Color textSecondary
    ) {
    }
}
