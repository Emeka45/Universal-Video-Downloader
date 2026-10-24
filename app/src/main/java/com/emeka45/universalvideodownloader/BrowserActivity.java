package com.emeka45.universalvideodownloader;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BrowserActivity extends Activity {
    private EditText address;
    private WebView webView;
    private LinearLayout candidatesPanel;
    private TextView detectionStatus;
    private DownloadManager downloadManager;
    private DownloadStore store;
    private final Handler handler = new Handler();
    private final Map<String, Candidate> candidates = new LinkedHashMap<>();
    private String currentPage = "";
    private boolean pageHasVideo = false;
    private final Runnable scanner = new Runnable() {
        @Override public void run() {
            if (webView != null && !isFinishing()) {
                webView.evaluateJavascript(scanScript(), null);
                handler.postDelayed(this, 1200);
            }
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        store = new DownloadStore(this);
        buildUi();
        String shared = getIntent() == null ? null : getIntent().getStringExtra(Intent.EXTRA_TEXT);
        if (UrlPolicy.isHttpUrl(shared)) loadAddress(shared.trim());
        else loadAddress("https://www.google.com");
    }

    private void buildUi() {
        int pad = dp(8);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        Button back = new Button(this);
        back.setText("‹");
        back.setContentDescription("Back");
        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextSize(14);
        address.setHint("Enter website or video URL");
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        Button go = new Button(this);
        go.setText("Go");
        nav.addView(back, new LinearLayout.LayoutParams(dp(42), -2));
        nav.addView(address, new LinearLayout.LayoutParams(0, -2, 1));
        nav.addView(go, new LinearLayout.LayoutParams(dp(56), -2));
        root.addView(nav);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setDatabaseEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(android.webkit.ConsoleMessage message) {
                String text = message.message();
                if (text != null && text.startsWith("UVDCANDIDATES:")) {
                    acceptScan(text.substring("UVDCANDIDATES:".length()));
                    return true;
                }
                return super.onConsoleMessage(message);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) return false;
                return true;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !(url.startsWith("http://") || url.startsWith("https://"));
            }
            @Override public void onPageStarted(WebView view, String url, Bitmap icon) {
                currentPage = url;
                address.setText(url);
                candidates.clear();
                pageHasVideo = false;
                renderCandidates();
            }
            @Override public void onPageFinished(WebView view, String url) {
                currentPage = url;
                address.setText(url);
                webView.evaluateJavascript(scanScript(), null);
            }
        });
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView panelTitle = new TextView(this);
        panelTitle.setText("VIDEO DETECTOR");
        panelTitle.setTextSize(12);
        panelTitle.setPadding(dp(4), dp(8), dp(4), dp(2));
        root.addView(panelTitle);

        detectionStatus = new TextView(this);
        detectionStatus.setText("Play a video on the page. Available downloadable sources will appear here.");
        detectionStatus.setTextSize(12);
        root.addView(detectionStatus);

        ScrollView candidatesScroll = new ScrollView(this);
        candidatesPanel = new LinearLayout(this);
        candidatesPanel.setOrientation(LinearLayout.VERTICAL);
        candidatesScroll.addView(candidatesPanel, new ViewGroup.LayoutParams(-1, -2));
        root.addView(candidatesScroll, new LinearLayout.LayoutParams(-1, dp(150)));

        back.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        go.setOnClickListener(v -> navigate());
        address.setOnEditorActionListener((v, actionId, event) -> { navigate(); return true; });
        setContentView(root);
    }

    private void navigate() {
        String value = address.getText().toString().trim();
        if (value.isEmpty()) return;
        if (!value.matches("(?i)^https?://.*")) {
            if (value.contains(" ") || !value.contains(".")) value = "https://www.google.com/search?q=" + Uri.encode(value);
            else value = "https://" + value;
        }
        if (!UrlPolicy.isHttpUrl(value)) {
            toast("Enter a valid website address.");
            return;
        }
        loadAddress(value);
    }

    private void loadAddress(String url) {
        if (!UrlPolicy.isHttpUrl(url)) return;
        address.setText(url);
        webView.loadUrl(url);
    }

    private String scanScript() {
        return "(function(){try{" +
            "if(!window.__uvdScanInstalled){" +
            "window.__uvdScanInstalled=true;" +
            "document.addEventListener('play',function(){setTimeout(window.__uvdScan,200)},true);" +
            "document.addEventListener('playing',function(){setTimeout(window.__uvdScan,200)},true);" +
            "document.addEventListener('loadedmetadata',function(){setTimeout(window.__uvdScan,200)},true);" +
            "}" +
            "window.__uvdScan=function(){" +
            "var out=[],seen={};" +
            "function add(u,label,w,h,playing,kind){if(!u||!/^https?:/i.test(u)||seen[u])return;seen[u]=1;" +
            "out.push({url:u,label:label||'',width:w||0,height:h||0,playing:!!playing,kind:kind||'video'});}" +
            "var vs=document.querySelectorAll('video');" +
            "for(var i=0;i<vs.length;i++){var v=vs[i];var w=v.videoWidth||0,h=v.videoHeight||0;" +
            "var current=v.currentSrc||v.src||'';var label=(w&&h)?(w+'×'+h+' (current playback)'):'Current video source';" +
            "if(current)add(current,label,w,h,!v.paused,'current');" +
            "var ss=v.querySelectorAll('source');for(var j=0;j<ss.length;j++){var s=ss[j];" +
            "var sw=parseInt(s.getAttribute('width')||0),sh=parseInt(s.getAttribute('height')||0);" +
            "var desc=s.getAttribute('label')||s.getAttribute('title')||s.getAttribute('data-quality')||'';" +
            "add(s.src,desc,sw,sh,false,'source');}" +
            "}" +
            "var all=document.querySelectorAll('video source,[data-src],a[href]');" +
            "for(var k=0;k<all.length;k++){var el=all[k];var u=el.src||el.getAttribute('data-src')||el.href||'';" +
            "if(/\\.(mp4|webm|m4v|mov|3gp|mkv|avi)(?:[?#]|$)/i.test(u)){" +
            "var t=(el.getAttribute('label')||el.getAttribute('title')||el.getAttribute('data-quality')||'');" +
            "var m=(u+' '+t).match(/(2160|1440|1080|720|480|360|240)p/i);" +
            "add(u,t||(m?m[1]+'p':'Video file'),0,m?parseInt(m[1]):0,false,'file');}}" +
            "try{console.log('UVDCANDIDATES:'+JSON.stringify({page:location.href,playing:Array.prototype.some.call(vs,function(v){return !v.paused&&!v.ended}),items:out}));}catch(e){}" +
            "};window.__uvdScan();setTimeout(window.__uvdScan,600);" +
            "}catch(e){}})();";
    }

    private void acceptScan(String json) {
        runOnUiThread(() -> {
            try {
                JSONObject payload = new JSONObject(json);
                pageHasVideo = payload.optBoolean("playing", false);
                JSONArray items = payload.optJSONArray("items");
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.optJSONObject(i);
                        if (item == null) continue;
                        String url = item.optString("url", "");
                        if (!UrlPolicy.isHttpUrl(url)) continue;
                        int width = item.optInt("width", 0);
                        int height = item.optInt("height", 0);
                        String label = item.optString("label", "");
                        if (label.isEmpty()) label = height > 0 ? height + "p" : "Video source";
                        Candidate candidate = new Candidate(url, label, width, height,
                                item.optBoolean("playing", false), item.optString("kind", "video"));
                        Candidate old = candidates.get(url);
                        if (old == null || (candidate.height > old.height)) candidates.put(url, candidate);
                    }
                }
                renderCandidates();
            } catch (Exception ignored) { }
        });
    }

    private void renderCandidates() {
        if (candidatesPanel == null) return;
        candidatesPanel.removeAllViews();
        if (candidates.isEmpty()) {
            detectionStatus.setText(pageHasVideo
                    ? "Video is playing, but this page has not exposed a direct downloadable source."
                    : "Play a video on the page. Available downloadable sources will appear here.");
            return;
        }
        detectionStatus.setText(pageHasVideo
                ? "Video detected. Choose a source below to download."
                : "Video sources found. Play the video to help identify its current resolution.");
        List<Candidate> sorted = new ArrayList<>(candidates.values());
        for (Candidate candidate : sorted) {
            String label = candidate.label;
            if (candidate.height > 0 && !label.toLowerCase(Locale.US).contains("p")
                    && !label.contains("×")) label += " · " + candidate.height + "p";
            if (candidate.width > 0 && candidate.height > 0
                    && !label.contains("×")) label += " · " + candidate.width + "×" + candidate.height;
            Button button = new Button(this);
            button.setText("Download " + label);
            button.setTextSize(12);
            button.setOnClickListener(v -> confirmDownload(candidate));
            candidatesPanel.addView(button, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private void confirmDownload(Candidate candidate) {
        String path = Uri.parse(candidate.url).getPath();
        String lower = path == null ? "" : path.toLowerCase(Locale.US);
        if (lower.endsWith(".m3u8") || lower.endsWith(".mpd") || lower.endsWith(".m3u")) {
            toast("This is an adaptive streaming playlist, not a video file. This lightweight build only downloads direct video files.");
            return;
        }
        String type = lower.endsWith(".webm") ? "video/webm" : "video/mp4";
        String name = path == null ? "video.mp4" : Uri.decode(path.substring(path.lastIndexOf('/') + 1));
        name = UrlPolicy.safeFileName(name);
        if (!name.contains(".")) name += ".mp4";
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(candidate.url));
            request.setTitle(name);
            request.setDescription("Universal Video Downloader · " + candidate.label);
            request.setMimeType(type);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "Universal Video Downloader/" + name);
            String cookie = CookieManager.getInstance().getCookie(candidate.url);
            if (cookie != null && !cookie.isEmpty()) request.addRequestHeader("Cookie", cookie);
            if (UrlPolicy.isHttpUrl(currentPage)) request.addRequestHeader("Referer", currentPage);
            request.addRequestHeader("User-Agent", webView.getSettings().getUserAgentString());
            long id = downloadManager.enqueue(request);
            store.add(id, name, candidate.url);
            toast("Download started: " + candidate.label);
        } catch (RuntimeException e) {
            toast("Could not start this download. The source may require a site-specific session.");
        }
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(scanner);
        handler.postDelayed(scanner, 800);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(scanner);
        super.onPause();
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(scanner);
        if (webView != null) {
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) parent.removeView(webView);
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }

    private static final class Candidate {
        final String url, label, kind;
        final int width, height;
        final boolean playing;
        Candidate(String url, String label, int width, int height, boolean playing, String kind) {
            this.url = url; this.label = label; this.width = width; this.height = height;
            this.playing = playing; this.kind = kind;
        }
    }
}
