// SUic Player Ad Blocker
// Drop-in ad host pattern matching
package com.soiadmahedi.suicTh;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SoiadMahediAdBlocker {
    private static final Set<String> AD_HOSTS = new HashSet<>(Arrays.asList(
        // Google and common video ad networks
        "googlesyndication.com", "doubleclick.net", "googleadservices.com", "google-analytics.com", "googletagmanager.com","googletagservices.com",
        "adservice.google.com", "pagead2.googlesyndication.com", "video-ad-stats.googlesyndication.com",
        "facebook.com/tr","connect.facebook.net","ads.facebook.com","amazon-adsystem.com","taboola.com","outbrain.com","criteo.com","pubmatic.com","rubiconproject.com",
        "crashlytics.com","hotjar.com","mouseflow.com"
    ));
    public static boolean shouldBlock(String url) {
        if (url == null || url.isEmpty()) return false;
        String lowerUrl = url.toLowerCase();
        for (String adHost : AD_HOSTS) if (lowerUrl.contains(adHost)) return true;
        return lowerUrl.contains("/ads/") || lowerUrl.contains("/ad?") || lowerUrl.contains("/advert") || lowerUrl.contains("ad-") || lowerUrl.contains("-ad.") || lowerUrl.contains("tracking") || lowerUrl.contains("analytics");
    }
    public static WebResourceResponse createEmptyResponse() {
        return new WebResourceResponse(
            "text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    }
    public static WebResourceResponse blockAds(WebResourceRequest request) {
        String url = request.getUrl().toString();
        if (shouldBlock(url)) return createEmptyResponse();
        return null;
    }
}