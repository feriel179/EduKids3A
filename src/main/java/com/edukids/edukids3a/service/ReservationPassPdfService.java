package com.edukids.edukids3a.service;

import com.edukids.edukids3a.model.Evenement;
import com.edukids.edukids3a.model.Reservation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Pass réservation en PDF avec QR code, aligné sur le web Symfony
 * ({@code pass.html.twig} / {@code pass_pdf.html.twig}) : même charge utile JSON dans le QR.
 */
public final class ReservationPassPdfService {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final DateTimeFormatter DATE_ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DT_FR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH);
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FR = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

    /** Taille du QR (pixels), proche du canvas web (~200). */
    private static final int QR_SIZE = 200;
    /** Couleur modules QR (ARGB), comme le web {@code #28a745}. */
    private static final int QR_DARK = 0xFF28a745;
    private static final int QR_LIGHT = 0xFFFFFFFF;

    /**
     * Charge utile identique au JS du site : {@code id}, {@code event}, {@code user}, {@code date}, {@code places},
     * et {@code reservation} (chaîne) comme dans {@code pass.html.twig}.
     */
    public static String buildQrPayloadJson(Reservation r) {
        Evenement ev = r.getEvenement();
        String titre = ev != null && ev.getTitre() != null ? ev.getTitre() : "";
        String user = (nvl(r.getPrenom()) + " " + nvl(r.getNom())).trim();
        String dateEv = "";
        if (ev != null && ev.getDateEvenement() != null) {
            dateEv = ev.getDateEvenement().format(DATE_ISO);
        }
        JsonObject o = new JsonObject();
        if (r.getId() != null) {
            o.addProperty("id", r.getId());
        }
        o.addProperty("event", titre);
        o.addProperty("user", user);
        o.addProperty("date", dateEv);
        o.addProperty("places", r.getNbPlacesTotal());
        if (r.getId() != null) {
            o.addProperty("reservation", String.valueOf(r.getId()));
        }
        return GSON.toJson(o);
    }

    public static BufferedImage renderQrImage(String jsonPayload) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(jsonPayload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
        MatrixToImageConfig cfg = new MatrixToImageConfig(QR_DARK, QR_LIGHT);
        return MatrixToImageWriter.toBufferedImage(matrix, cfg);
    }

    /**
     * Génère un PDF A4 (texte + QR), même logique métier que l’export web (html2pdf / impression).
     */
    public void exportPdf(Reservation r, Path outputPath) throws IOException {
        if (r.getId() == null) {
            throw new IllegalArgumentException("Réservation sans identifiant.");
        }
        Evenement ev = r.getEvenement();
        String json = buildQrPayloadJson(r);
        BufferedImage qr;
        try {
            qr = renderQrImage(json);
        } catch (WriterException e) {
            throw new IOException("QR code : " + e.getMessage(), e);
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float pageW = page.getMediaBox().getWidth();
            float pageH = page.getMediaBox().getHeight();
            float margin = 48;
            float bodyTop = pageH - margin - 72;
            float leftX = margin;
            float rightX = pageW - margin - 210;
            float leftW = Math.max(220, rightX - leftX - 14);
            float leftBoxBottom = Math.min(bodyTop - 250, margin + 60);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                drawHeader(cs, margin, pageW, pageH);
                drawLightCard(cs, leftX - 8, leftBoxBottom, leftW + 16, bodyTop - leftBoxBottom + 12);

                float eventY = bodyTop;
                eventY = drawSectionTitle(cs, leftX, eventY, "Evenement");
                if (ev != null) {
                    eventY = drawLabelValue(cs, leftX, eventY, "Titre", nvl(ev.getTitre()));
                    if (ev.getDateEvenement() != null) {
                        eventY = drawLabelValue(cs, leftX, eventY, "Date", ev.getDateEvenement().format(DATE_FR));
                    }
                    if (ev.getHeureDebut() != null && ev.getHeureFin() != null) {
                        eventY = drawLabelValue(cs, leftX, eventY, "Horaires",
                                ev.getHeureDebut().format(TIME_FR) + " - " + ev.getHeureFin().format(TIME_FR));
                    }
                    String lieu = ev.getLocalisation();
                    if (lieu != null && !lieu.isBlank()) {
                        eventY = drawLabelValue(cs, leftX, eventY, "Lieu", lieu.trim());
                    }
                    if (ev.getNbPlacesDisponibles() != null) {
                        eventY = drawLabelValue(cs, leftX, eventY, "Capacite",
                                ev.getNbPlacesDisponibles() + " place(s)");
                    }
                } else {
                    eventY = drawLabelValue(cs, leftX, eventY, "Info", "(Evenement non charge)");
                }
                eventY -= 8;

                float resaY = drawSectionTitle(cs, leftX, eventY, "Reservation");
                resaY = drawLabelValue(cs, leftX, resaY, "ID",
                        "#" + String.format("%06d", r.getId()) + " (id " + r.getId() + ")");
                resaY = drawLabelValue(cs, leftX, resaY, "Participant",
                        nvl(r.getPrenom()) + " " + nvl(r.getNom()));
                resaY = drawLabelValue(cs, leftX, resaY, "E-mail", nvl(r.getEmail()));
                String tel = r.getTelephone();
                if (tel != null && !tel.isBlank()) {
                    resaY = drawLabelValue(cs, leftX, resaY, "Tel.", tel.trim());
                }
                resaY = drawLabelValue(cs, leftX, resaY, "Places",
                        r.getNbPlacesTotal() + " (adultes " + r.getNbAdultes() + ", enfants " + r.getNbEnfants() + ")");
                if (r.getDateReservation() != null) {
                    resaY = drawLabelValue(cs, leftX, resaY, "Date reservation",
                            r.getDateReservation().format(DT_FR));
                }

                PDImageXObject qrObj = LosslessFactory.createFromImage(doc, qr);
                float qrDraw = 168f;
                float qrX = rightX + 21;
                float qrY = bodyTop - 205;
                drawLightCard(cs, rightX, qrY - 36, 210, 255);
                cs.drawImage(qrObj, qrX, qrY, qrDraw, qrDraw);
                drawLine(cs, PDType1Font.HELVETICA_BOLD, 11, rightX + 12, qrY + qrDraw + 20, "Validation QR");
                drawLine(cs, PDType1Font.HELVETICA_OBLIQUE, 9, rightX + 12, qrY + qrDraw + 6,
                        "Scannez ce code a l'entree.");
                drawLine(cs, PDType1Font.HELVETICA_BOLD, 10, rightX + 12, qrY - 14,
                        "#" + String.format("%06d", r.getId()));

                drawLine(cs, PDType1Font.HELVETICA_OBLIQUE, 8, margin, margin,
                        "Genere le " + LocalDateTime.now().format(DT_FR) + " - EduKids");
            }
            doc.save(outputPath.toFile());
        }
    }

    private static void drawHeader(PDPageContentStream cs, float margin, float pageW, float pageH) throws IOException {
        float h = 58;
        float y = pageH - margin - h + 8;
        cs.setNonStrokingColor(new Color(79, 70, 229));
        cs.addRect(margin, y, pageW - (2 * margin), h);
        cs.fill();
        cs.setNonStrokingColor(Color.WHITE);
        drawLine(cs, PDType1Font.HELVETICA_BOLD, 18, margin + 14, y + 38, "EduKids - Pass reservation");
        drawLine(cs, PDType1Font.HELVETICA_OBLIQUE, 10, margin + 14, y + 20,
                "Document valide pour l'entree de l'evenement");
        cs.setNonStrokingColor(Color.BLACK);
    }

    private static float drawSectionTitle(PDPageContentStream cs, float x, float y, String title) throws IOException {
        return drawLine(cs, PDType1Font.HELVETICA_BOLD, 12, x, y, title);
    }

    private static float drawLabelValue(PDPageContentStream cs, float x, float y, String label, String value)
            throws IOException {
        float next = drawLine(cs, PDType1Font.HELVETICA_BOLD, 10, x, y, label + " :");
        return drawLine(cs, PDType1Font.HELVETICA, 10.5f, x + 72, y, value);
    }

    private static void drawLightCard(PDPageContentStream cs, float x, float y, float w, float h) throws IOException {
        cs.setNonStrokingColor(new Color(245, 247, 251));
        cs.addRect(x, y, w, h);
        cs.fill();
        cs.setStrokingColor(new Color(218, 223, 236));
        cs.addRect(x, y, w, h);
        cs.stroke();
        cs.setStrokingColor(Color.BLACK);
        cs.setNonStrokingColor(Color.BLACK);
    }

    private static float drawLine(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String text)
            throws IOException {
        cs.setFont(font, size);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(safePdfText(text));
        cs.endText();
        return y - size - 4;
    }

    private static String safePdfText(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (c == '\n' || c == '\r') {
                sb.append(' ');
            } else if (c >= 32 && c <= 126) {
                sb.append(c);
            } else if (c == 0x0152) {
                sb.append("OE");
            } else if (c == 0x0153) {
                sb.append("oe");
            } else {
                sb.append('?');
            }
        }
        String t = sb.toString();
        if (t.length() > 500) {
            return t.substring(0, 497) + "...";
        }
        return t;
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
