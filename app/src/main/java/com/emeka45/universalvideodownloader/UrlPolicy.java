package com.emeka45.universalvideodownloader;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class UrlPolicy {
    private UrlPolicy() {}

    public static boolean isHttpUrl(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            return scheme != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !uri.getHost().trim().isEmpty();
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return false;
        }
    }

    public static boolean isLikelyMediaUrl(String url, String contentType) {
        if (contentType != null
                && (contentType.toLowerCase(Locale.US).startsWith("video/")
                || contentType.toLowerCase(Locale.US).startsWith("application/vnd.apple.mpegurl")
                || contentType.toLowerCase(Locale.US).startsWith("application/x-mpegurl"))) {
            return true;
        }
        if (!isHttpUrl(url)) return false;
        try {
            String path = new URI(url.trim()).getPath();
            if (path == null) return false;
            String lower = path.toLowerCase(Locale.US);
            return lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mkv")
                    || lower.endsWith(".3gp") || lower.endsWith(".mov") || lower.endsWith(".m4v")
                    || lower.endsWith(".avi") || lower.endsWith(".mpeg") || lower.endsWith(".mpg")
                    || lower.endsWith(".m3u8") || lower.endsWith(".mpd");
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return false;
        }
    }

    public static String safeFileName(String value) {
        String name = value == null ? "video" : value.trim();
        if (name.isEmpty()) name = "video";
        name = name.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_");
        name = name.replaceAll("\\s+", " ").trim();
        if (name.length() > 100) name = name.substring(0, 100).trim();
        return name.isEmpty() ? "video" : name;
    }
}
