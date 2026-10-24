package com.emeka45.universalvideodownloader;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class PlayerActivity extends Activity {
    public static final String EXTRA_URI = "video_uri";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String raw = getIntent().getStringExtra(EXTRA_URI);
        if (raw == null) { finish(); return; }
        Uri uri;
        try { uri = Uri.parse(raw); } catch (RuntimeException e) { finish(); return; }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("Universal Video Downloader");
        title.setPadding(16, 12, 16, 12);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        VideoView video = new VideoView(this);
        video.setVideoURI(uri);
        MediaController controller = new MediaController(this);
        controller.setAnchorView(video);
        video.setMediaController(controller);
        root.addView(video, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
        video.setOnPreparedListener(mp -> video.start());
    }
}
