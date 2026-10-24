package com.emeka45.universalvideodownloader;

import android.net.Uri;

import java.util.Locale;

public final class UrlPolicy {
    private UrlPolicy() {}

    public static boolean isHttpUrl(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            Uri uri = Uri.parse(value.trim());
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static boolean isLikelyMediaUrl(String url, String contentType) {
        if (contentType != null && contentType.toLowerCase(Locale.US).startsWith("video/")) return true;
        if (!isHttpUrl(url)) return false;
        String path = Uri.parse(url).getPath();
        if (path == null) return false;
        String lower = path.toLowerCase(Locale.US);
        return lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mkv")
                || lower.endsWith(".3gp") || lower.endsWith(".mov") || lower.endsWith(".m4v")
                || lower.endsWith(".avi") || lower.endsWith(".mpeg") || lower.endsWith(".mpg");
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
