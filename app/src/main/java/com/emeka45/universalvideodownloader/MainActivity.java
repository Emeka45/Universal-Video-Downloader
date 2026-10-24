package com.emeka45.universalvideodownloader;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int STORAGE_REQUEST = 42;
    private static final int NOTIFICATION_REQUEST = 43;
    private EditText urlInput;
    private LinearLayout list;
    private DownloadManager downloadManager;
    private DownloadStore store;
    private final Handler handler = new Handler();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            refreshDownloads();
            handler.postDelayed(this, 1500);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        store = new DownloadStore(this);
        buildUi();
        requestNotificationPermissionIfNeeded();
        handleIncomingIntent(getIntent());
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
        }
    }

    private void buildUi() {
        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Universal Video Downloader");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText("Download direct video files and watch them here.");
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        urlInput = new EditText(this);
        urlInput.setHint(getString(R.string.url_hint));
        urlInput.setSingleLine(true);
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, -2);
        inputParams.topMargin = dp(12);
        root.addView(urlInput, inputParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        Button download = new Button(this);
        download.setText(getString(R.string.download));
        Button refresh = new Button(this);
        refresh.setText(getString(R.string.refresh));
        buttons.addView(download, new LinearLayout.LayoutParams(0, -2, 1));
        buttons.addView(refresh, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(buttons);

        TextView heading = new TextView(this);
        heading.setText(getString(R.string.downloads));
        heading.setTextSize(18);
        heading.setPadding(0, dp(14), 0, dp(6));
        root.addView(heading);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ViewGroup.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        download.setOnClickListener(v -> startDownloadFromInput());
        refresh.setOnClickListener(v -> refreshDownloads());
        setContentView(root);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (shared != null && UrlPolicy.isHttpUrl(shared.trim())) urlInput.setText(shared.trim());
    }

    private void startDownloadFromInput() {
        String url = urlInput.getText().toString().trim();
        if (!UrlPolicy.isHttpUrl(url)) {
            toast("Enter a valid HTTP or HTTPS URL.");
            return;
        }
        if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST);
            return;
        }
        inspectAndDownload(url);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startDownloadFromInput();
        } else if (requestCode == STORAGE_REQUEST) {
            toast("Storage permission is needed on older Android versions.");
        }
    }

    private void inspectAndDownload(final String url) {
        toast("Checking video link…");
        executor.execute(() -> {
            String type = null;
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "UniversalVideoDownloader/0.1");
                connection.connect();
                type = connection.getContentType();
                connection.disconnect();
            } catch (Exception ignored) { }
            final String contentType = type;
            runOnUiThread(() -> {
                if (!UrlPolicy.isLikelyMediaUrl(url, contentType)) {
                    toast("This link does not look like a direct video file. Use a direct downloadable video URL.");
                    return;
                }
                queueDownload(url, contentType);
            });
        });
    }

    private void queueDownload(String url, String contentType) {
        String name = fileName(url);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(name);
        request.setDescription("Universal Video Downloader");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(false);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "Universal Video Downloader/" + name);
        if (contentType != null) request.setMimeType(contentType.split(";", 2)[0]);
        try {
            long id = downloadManager.enqueue(request);
            store.add(id, name, url);
            urlInput.setText("");
            refreshDownloads();
            toast("Download started.");
        } catch (RuntimeException e) {
            toast("Could not start the download.");
        }
    }

    private String fileName(String url) {
        String path = Uri.parse(url).getPath();
        String candidate = path == null ? "" : Uri.decode(path.substring(path.lastIndexOf('/') + 1));
        candidate = UrlPolicy.safeFileName(candidate);
        if (!candidate.contains(".")) candidate += ".mp4";
        return candidate;
    }

    private void refreshDownloads() {
        if (list == null) return;
        list.removeAllViews();
        List<DownloadStore.Record> records = store.all();
        if (records.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(getString(R.string.empty_downloads));
            empty.setPadding(0, dp(12), 0, dp(12));
            list.addView(empty);
            return;
        }
        for (DownloadStore.Record record : records) addDownloadRow(record);
    }

    private void addDownloadRow(DownloadStore.Record record) {
        DownloadState state = query(record.id);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView name = new TextView(this);
        name.setText(record.name);
        name.setTextSize(16);
        row.addView(name);

        TextView status = new TextView(this);
        status.setText(state.label);
        row.addView(status);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        if (state.completed && state.localUri != null) {
            Button play = new Button(this);
            play.setText("Watch");
            play.setOnClickListener(v -> openPlayer(state.localUri));
            actions.addView(play, new LinearLayout.LayoutParams(0, -2, 1));

            Button share = new Button(this);
            share.setText("Share");
            share.setOnClickListener(v -> shareFile(state.localUri));
            actions.addView(share, new LinearLayout.LayoutParams(0, -2, 1));
        }
        Button remove = new Button(this);
        remove.setText("Remove");
        remove.setOnClickListener(v -> {
            downloadManager.remove(record.id);
            store.remove(record.id);
            refreshDownloads();
        });
        actions.addView(remove, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(actions);
        list.addView(row);
    }

    private DownloadState query(long id) {
        DownloadManager.Query q = new DownloadManager.Query().setFilterById(id);
        Cursor c = null;
        try {
            c = downloadManager.query(q);
            if (c == null || !c.moveToFirst()) return new DownloadState("No longer available", false, null);
            int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            String local = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
            long soFar = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            long total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            String label;
            if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING) {
                label = total > 0 ? "Downloading " + ((soFar * 100) / total) + "%" : "Downloading…";
            } else if (status == DownloadManager.STATUS_PAUSED) {
                label = "Paused";
            } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                label = "Completed";
            } else {
                label = "Download failed";
            }
            return new DownloadState(label, status == DownloadManager.STATUS_SUCCESSFUL, local == null ? null : Uri.parse(local));
        } finally {
            if (c != null) c.close();
        }
    }

    private void openPlayer(Uri uri) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_URI, uri.toString());
        startActivity(intent);
    }

    private void shareFile(Uri uri) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("video/*");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Share video"));
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refreshRunnable);
        handler.post(refreshRunnable);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(refreshRunnable);
        executor.shutdownNow();
        super.onDestroy();
    }

    private static final class DownloadState {
        final String label; final boolean completed; final Uri localUri;
        DownloadState(String label, boolean completed, Uri localUri) { this.label = label; this.completed = completed; this.localUri = localUri; }
    }
}
