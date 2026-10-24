package com.emeka45.universalvideodownloader;

import org.junit.Test;

import static org.junit.Assert.*;

public class UrlPolicyTest {
    @Test public void acceptsHttpAndHttps() {
        assertTrue(UrlPolicy.isHttpUrl("https://example.com/video.mp4"));
        assertTrue(UrlPolicy.isHttpUrl("http://example.com/a"));
    }

    @Test public void rejectsUnsafeSchemes() {
        assertFalse(UrlPolicy.isHttpUrl("file:///sdcard/video.mp4"));
        assertFalse(UrlPolicy.isHttpUrl("javascript:alert(1)"));
        assertFalse(UrlPolicy.isHttpUrl("not a url"));
    }

    @Test public void detectsVideoByMimeOrExtension() {
        assertTrue(UrlPolicy.isLikelyMediaUrl("https://example.com/download", "video/mp4"));
        assertTrue(UrlPolicy.isLikelyMediaUrl("https://example.com/clip.MP4?token=1", null));
        assertFalse(UrlPolicy.isLikelyMediaUrl("https://example.com/page", "text/html"));
    }

    @Test public void sanitizesFileNames() {
        assertEquals("my__video.mp4", UrlPolicy.safeFileName("my:/video.mp4"));
        assertEquals("video", UrlPolicy.safeFileName("   "));
    }
}
