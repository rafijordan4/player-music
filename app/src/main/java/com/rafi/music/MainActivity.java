package com.rafi.music;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.ui.PlayerView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends android.app.Activity {
    private static final int REQ_MEDIA = 41;
    private static final int GOLD = 0xFFE0C27A;
    private static final int WHITE = 0xFFF5F2EA;
    private static final int MUTED = 0xFFAAA5B5;
    private static final int PANEL = 0xFF1C1B22;

    private LinearLayout root, mediaList;
    private TextView nowTitle, nowSubtitle, status, countLabel;
    private PlayerView videoView;
    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private final List<MediaEntry> entries = new ArrayList<>();
    private boolean showVideosOnly = false;

    static class MediaEntry {
        final Uri uri;
        final String title;
        final String artist;
        final boolean video;
        MediaEntry(Uri uri, String title, String artist, boolean video) {
            this.uri = uri;
            this.title = title;
            this.artist = artist;
            this.video = video;
        }
    }

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF101014);
        getWindow().setNavigationBarColor(0xFF101014);
        buildUi();
        connectController();
        requestMediaPermission();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private Button actionButton(String value, boolean primary) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(primary ? 0xFF191711 : WHITE);
        b.setBackground(shape(primary ? GOLD : PANEL, 16));
        b.setPadding(dp(12), 0, dp(12), 0);
        return b;
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(12));
        root.setBackgroundColor(0xFF101014);
        setContentView(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("♫  RAFI MUSIC", 14, GOLD);
        brand.setTypeface(null, Typeface.BOLD);
        brand.setLetterSpacing(0.10f);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        Button addLink = actionButton("+  Add link", true);
        top.addView(addLink, new LinearLayout.LayoutParams(-2, dp(44)));
        root.addView(top);

        TextView heading = text("Your media.\nYour space.", 30, WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(0, dp(18), 0, dp(16));
        root.addView(heading);

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        Button scan = actionButton("↻  Scan device", false);
        Button audioTab = actionButton("♫  Audio", true);
        Button videoTab = actionButton("▣  Video", false);
        quick.addView(scan, new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(0, dp(46), 1);
        tabLp.leftMargin = dp(7);
        quick.addView(audioTab, tabLp);
        LinearLayout.LayoutParams videoLp = new LinearLayout.LayoutParams(0, dp(46), 1);
        videoLp.leftMargin = dp(7);
        quick.addView(videoTab, videoLp);
        root.addView(quick);

        countLabel = text("YOUR LIBRARY", 11, GOLD);
        countLabel.setTypeface(null, Typeface.BOLD);
        countLabel.setPadding(0, dp(20), 0, dp(8));
        root.addView(countLabel);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        mediaList = new LinearLayout(this);
        mediaList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(mediaList);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        videoView = new PlayerView(this);
        videoView.setUseController(true);
        videoView.setBackground(shape(0xFF08080A, 18));
        videoView.setVisibility(View.GONE);
        LinearLayout.LayoutParams playerLp = new LinearLayout.LayoutParams(-1, dp(205));
        playerLp.topMargin = dp(10);
        root.addView(videoView, playerLp);

        LinearLayout now = new LinearLayout(this);
        now.setOrientation(LinearLayout.VERTICAL);
        now.setPadding(dp(15), dp(12), dp(15), dp(12));
        now.setBackground(shape(PANEL, 18));
        TextView overline = text("NOW PLAYING", 10, GOLD);
        overline.setTypeface(null, Typeface.BOLD);
        now.addView(overline);
        nowTitle = text("Nothing playing yet", 16, WHITE);
        nowTitle.setTypeface(null, Typeface.BOLD);
        nowTitle.setPadding(0, dp(5), 0, dp(2));
        now.addView(nowTitle);
        nowSubtitle = text("Choose local media or add a supported link", 11, MUTED);
        now.addView(nowSubtitle);
        root.addView(now, new LinearLayout.LayoutParams(-1, -2));

        status = text("Offline library • Online links", 10, MUTED);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(8), 0, 0);
        root.addView(status);

        scan.setOnClickListener(v -> loadMedia());
        addLink.setOnClickListener(v -> showLinkDialog());
        audioTab.setOnClickListener(v -> {
            showVideosOnly = false;
            audioTab.setBackground(shape(GOLD, 16));
            audioTab.setTextColor(0xFF191711);
            videoTab.setBackground(shape(PANEL, 16));
            videoTab.setTextColor(WHITE);
            renderList();
        });
        videoTab.setOnClickListener(v -> {
            showVideosOnly = true;
            videoTab.setBackground(shape(GOLD, 16));
            videoTab.setTextColor(0xFF191711);
            audioTab.setBackground(shape(PANEL, 16));
            audioTab.setTextColor(WHITE);
            renderList();
        });
    }

    private void connectController() {
        SessionToken token = new SessionToken(this, new ComponentName(this, MusicService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                videoView.setPlayer(controller);
                status.setText("Player connected • Ready");
            } catch (ExecutionException | InterruptedException e) {
                status.setText("Player connection failed");
            }
        }, command -> new Handler(Looper.getMainLooper()).post(command));
    }

    private void requestMediaPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            List<String> permissions = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            if (!permissions.isEmpty()) {
                requestPermissions(permissions.toArray(new String[0]), REQ_MEDIA);
                return;
            }
        } else if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_MEDIA);
            return;
        }
        loadMedia();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_MEDIA) loadMedia();
    }

    private void loadMedia() {
        entries.clear();
        if (Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Uri audio = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] columns = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST};
            try (Cursor c = getContentResolver().query(audio, columns,
                    MediaStore.Audio.Media.IS_MUSIC + " != 0", null, MediaStore.Audio.Media.TITLE + " ASC")) {
                if (c != null) while (c.moveToNext()) {
                    long id = c.getLong(0);
                    entries.add(new MediaEntry(Uri.withAppendedPath(audio, String.valueOf(id)),
                            safe(c.getString(1), "Unknown audio"),
                            safe(c.getString(2), "Unknown artist"), false));
                }
            } catch (Exception ignored) { }
        }

        if (Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED) {
            Uri videos = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            String[] columns = {MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DURATION};
            try (Cursor c = getContentResolver().query(videos, columns, null, null, MediaStore.Video.Media.TITLE + " ASC")) {
                if (c != null) while (c.moveToNext()) {
                    long id = c.getLong(0);
                    long duration = c.getLong(2);
                    entries.add(new MediaEntry(Uri.withAppendedPath(videos, String.valueOf(id)),
                            safe(c.getString(1), "Video"), "Video • " + durationLabel(duration), true));
                }
            } catch (Exception ignored) { }
        }
        renderList();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String durationLabel(long millis) {
        long seconds = Math.max(0, millis / 1000);
        return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60);
    }

    private void renderList() {
        mediaList.removeAllViews();
        int shown = 0;
        for (int i = 0; i < entries.size(); i++) {
            MediaEntry entry = entries.get(i);
            if (entry.video != showVideosOnly) continue;
            shown++;
            final int index = i;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(13), dp(9), dp(12), dp(9));
            row.setBackground(shape(PANEL, 15));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(68));
            rowLp.bottomMargin = dp(7);
            mediaList.addView(row, rowLp);

            TextView glyph = text(entry.video ? "▣" : "♫", 22, GOLD);
            glyph.setGravity(Gravity.CENTER);
            row.addView(glyph, new LinearLayout.LayoutParams(dp(42), -1));

            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = text(entry.title, 14, WHITE);
            title.setTypeface(null, Typeface.BOLD);
            title.setMaxLines(1);
            TextView subtitle = text(entry.artist, 11, MUTED);
            subtitle.setMaxLines(1);
            labels.addView(title);
            labels.addView(subtitle);
            row.addView(labels, new LinearLayout.LayoutParams(0, -1, 1));

            TextView play = text("▶", 16, GOLD);
            play.setGravity(Gravity.CENTER);
            row.addView(play, new LinearLayout.LayoutParams(dp(34), -1));
            row.setOnClickListener(v -> playEntry(index));
        }
        countLabel.setText((showVideosOnly ? "VIDEO LIBRARY  •  " : "AUDIO LIBRARY  •  ") + shown + " ITEMS");
        if (shown == 0) {
            TextView empty = text(showVideosOnly
                    ? "No videos found. Allow video access or add a direct video link."
                    : "No audio found. Allow audio access or add a supported audio link.",
                    13, MUTED);
            empty.setPadding(dp(5), dp(20), dp(5), dp(20));
            mediaList.addView(empty);
        }
    }

    private void playEntry(int index) {
        if (controller == null) {
            Toast.makeText(this, "Player is starting. Try again in a moment.", Toast.LENGTH_SHORT).show();
            return;
        }
        MediaEntry entry = entries.get(index);
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(entry.title)
                .setArtist(entry.artist)
                .build();
        MediaItem item = new MediaItem.Builder().setUri(entry.uri).setMediaMetadata(metadata).build();
        controller.setMediaItem(item);
        controller.prepare();
        controller.play();
        nowTitle.setText(entry.title);
        nowSubtitle.setText(entry.artist);
        videoView.setVisibility(entry.video ? View.VISIBLE : View.GONE);
        status.setText(entry.video ? "Playing local video" : "Playing local audio");
    }

    private void showLinkDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Paste YouTube, Spotify, MP3, MP4, or stream URL");
        input.setTextColor(WHITE);
        input.setHintTextColor(MUTED);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        box.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Add media link")
                .setMessage("Direct audio/video links can play in this player. YouTube and Spotify page links open in their official app or browser.")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open / Play", (dialog, which) -> handleLink(input.getText().toString().trim()))
                .show();
    }

    private void handleLink(String url) {
        if (!(url.startsWith("https://") || url.startsWith("http://"))) {
            Toast.makeText(this, "Enter a valid http or https URL", Toast.LENGTH_LONG).show();
            return;
        }
        Uri uri = Uri.parse(url);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("youtube.com") || host.endsWith(".youtube.com") ||
                host.equals("youtu.be") || host.equals("spotify.com") ||
                host.endsWith(".spotify.com")) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                status.setText("Opened link in its official app or browser");
            } catch (Exception e) {
                Toast.makeText(this, "No app available to open this link", Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (controller == null) {
            Toast.makeText(this, "Player is starting. Try again in a moment.", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean video = isVideoUrl(uri);
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(video ? "Online video" : "Online audio")
                .setArtist(host)
                .build();
        MediaItem item = new MediaItem.Builder().setUri(uri).setMediaMetadata(metadata).build();
        controller.setMediaItem(item);
        controller.prepare();
        controller.play();
        nowTitle.setText(video ? "Online video" : "Online audio");
        nowSubtitle.setText(host);
        videoView.setVisibility(video ? View.VISIBLE : View.GONE);
        status.setText(video ? "Playing online video" : "Playing online audio");
    }

    private boolean isVideoUrl(Uri uri) {
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        return path.endsWith(".mp4") || path.endsWith(".m4v") || path.endsWith(".mkv") ||
                path.endsWith(".webm") || path.endsWith(".mov") || path.endsWith(".m3u8") ||
                path.endsWith(".mpd");
    }

    @Override protected void onDestroy() {
        videoView.setPlayer(null);
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
        controller = null;
        super.onDestroy();
    }
}
