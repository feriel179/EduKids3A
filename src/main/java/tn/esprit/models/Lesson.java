package tn.esprit.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Lesson {
    private final long id;
    private Course course;
    private int order;
    private String title;
    private String mediaType;
    private String mediaUrl;
    private String videoUrl;
    private String youtubeUrl;
    private String image;

    public Lesson(long id, Course course, int order, String title, String mediaType,
                  String mediaUrl, String videoUrl, String youtubeUrl, String image) {
        this.id = id;
        this.course = course;
        this.order = order;
        this.title = title;
        this.mediaType = normalizeMediaType(mediaType);
        this.mediaUrl = mediaUrl;
        this.videoUrl = videoUrl;
        this.youtubeUrl = youtubeUrl;
        this.image = image;
    }

    public long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = normalizeMediaType(mediaType);
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getPdfUrl() {
        return mediaUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.mediaUrl = pdfUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getActiveUrl() {
        return switch (normalizeMediaType(mediaType)) {
            case "VIDEO" -> firstNonBlank(videoUrl, mediaUrl);
            case "YOUTUBE" -> firstNonBlank(youtubeUrl, mediaUrl);
            default -> mediaUrl == null ? "" : mediaUrl;
        };
    }

    public String getDisplayMediaType() {
        List<String> labels = getAvailableMediaLabels();
        return labels.isEmpty() ? "MEDIA" : String.join(" + ", labels);
    }

    public String getUrlSummary() {
        List<String> urls = new ArrayList<>();
        if (hasText(getPdfUrl())) {
            urls.add("PDF: " + getPdfUrl());
        }
        if (hasText(videoUrl)) {
            urls.add("VIDEO: " + videoUrl);
        }
        if (hasText(youtubeUrl)) {
            urls.add("YOUTUBE: " + youtubeUrl);
        }
        return urls.isEmpty() ? "" : String.join(" | ", urls);
    }

    public String getMediaIcon() {
        List<String> labels = getAvailableMediaLabels();
        if (labels.size() > 1) {
            return "[MULTI]";
        }
        if (labels.contains("VIDEO")) {
            return "[VIDEO]";
        }
        if (labels.contains("YOUTUBE")) {
            return "[YT]";
        }
        if (labels.contains("PDF")) {
            return "[PDF]";
        }
        return "[MEDIA]";
    }

    private String normalizeMediaType(String value) {
        return value == null ? "PDF" : value.trim().toUpperCase(Locale.ROOT);
    }

    private List<String> getAvailableMediaLabels() {
        List<String> labels = new ArrayList<>();
        if (hasText(getPdfUrl())) {
            labels.add("PDF");
        }
        if (hasText(videoUrl)) {
            labels.add("VIDEO");
        }
        if (hasText(youtubeUrl)) {
            labels.add("YOUTUBE");
        }

        if (!labels.isEmpty()) {
            return labels;
        }

        String normalized = normalizeMediaType(mediaType);
        if (normalized.contains("PDF")) {
            labels.add("PDF");
        }
        if (normalized.contains("VIDEO")) {
            labels.add("VIDEO");
        }
        if (normalized.contains("YOUTUBE")) {
            labels.add("YOUTUBE");
        }
        return labels;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback == null ? "" : fallback;
    }
}
