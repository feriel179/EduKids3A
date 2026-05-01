package com.edukids.edukids3a.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Extrait le texte d'un PDF avec PDFBox puis demande un résumé court à OpenRouter.
 */
public class PdfSummaryService {

    private final OpenRouterTranslationService openRouterService;

    public PdfSummaryService(OpenRouterTranslationService openRouterService) {
        this.openRouterService = openRouterService;
    }

    public String summarize(Path pdfPath) {
        String text = extractText(pdfPath);
        if (text.isBlank()) {
            throw new IllegalStateException("PDF sans texte exploitable.");
        }
        return openRouterService.summarizePdfText(text);
    }

    private String extractText(Path pdfPath) {
        if (pdfPath == null || !Files.isRegularFile(pdfPath)) {
            throw new IllegalStateException("PDF introuvable.");
        }

        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            if (document.isEncrypted()) {
                throw new IllegalStateException("PDF illisible: document protégé ou chiffré.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        } catch (IOException e) {
            throw new IllegalStateException("PDF illisible: " + e.getMessage(), e);
        }
    }
}
