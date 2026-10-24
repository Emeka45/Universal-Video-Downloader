package com.emeka45.universalvideodownloader;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class DownloadStore {
    private static final String PREFS = "downloads";
    private static final String PREFIX = "d_";
    private final SharedPreferences prefs;

    public DownloadStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void add(long id, String name, String url) {
        String value = encode(name) + "\n" + encode(url);
        prefs.edit().putString(PREFIX + id, value).apply();
    }

    public List<Record> all() {
        List<Record> result = new ArrayList<>();
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith(PREFIX)) continue;
            try {
                long id = Long.parseLong(key.substring(PREFIX.length()));
                String value = prefs.getString(key, "");
                if (value == null) continue;
                String[] parts = value.split("\\n", 2);
                if (parts.length == 2) result.add(new Record(id, decode(parts[0]), decode(parts[1])));
            } catch (RuntimeException ignored) { }
        }
        Collections.sort(result, new Comparator<Record>() {
            @Override public int compare(Record a, Record b) { return Long.compare(b.id, a.id); }
        });
        return result;
    }

    public void remove(long id) { prefs.edit().remove(PREFIX + id).apply(); }

    private static String encode(String value) {
        return Base64.encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    private static String decode(String value) {
        return new String(Base64.decode(value, Base64.NO_WRAP), StandardCharsets.UTF_8);
    }

    public static final class Record {
        public final long id;
        public final String name;
        public final String url;
        public Record(long id, String name, String url) { this.id = id; this.name = name; this.url = url; }
    }
}
