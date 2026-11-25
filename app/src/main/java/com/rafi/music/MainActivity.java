package com.rafi.music;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;

import android.content.ComponentName;
import android.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends android.app.Activity {
    private static final int REQ_AUDIO = 41;
    private LinearLayout root, songList;
    private TextView nowTitle, nowSubtitle, status;
    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private final List<Song> songs = new ArrayList<>();

    static class Song {
        final Uri uri; final String title; final String artist;
        Song(Uri uri, String title, String artist) { this.uri=uri; this.title=title; this.artist=artist; }
    }

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        buildUi();
        connectController();
        requestAudioPermission();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density); }
    private TextView label(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private Button button(String text) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false);
        b.setTextColor(0xFFFFFFFF); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF292631));
        return b;
    }
    private void buildUi() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(12)); root.setBackgroundColor(0xFF101014);
        setContentView(root);
        TextView brand = label("RAFI MUSIC", 13, 0xFFB69CFF); brand.setLetterSpacing(0.18f);
        root.addView(brand);
        TextView heading = label("Your sound.\nYour space.", 30, 0xFFFFFFFF);
        heading.setTypeface(null, 1); heading.setPadding(0, dp(12), 0, dp(18)); root.addView(heading);
        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        Button scan = button("Scan musik"); Button online = button("Tambah URL");
        actions.addView(scan, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(0, dp(48), 1); op.leftMargin=dp(8); actions.addView(online, op);
        root.addView(actions);
        TextView section = label("LIBRARY", 12, 0xFF9B96A8); section.setPadding(0, dp(22), 0, dp(8)); root.addView(section);
        ScrollView scroll = new ScrollView(this); songList = new LinearLayout(this); songList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(songList); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView playerHead = label("NOW PLAYING", 11, 0xFFB69CFF); playerHead.setPadding(0, dp(12), 0, dp(5)); root.addView(playerHead);
        nowTitle = label("Belum ada lagu", 17, 0xFFFFFFFF); nowTitle.setTypeface(null, 1); root.addView(nowTitle);
        nowSubtitle = label("Pilih lagu dari library atau URL", 12, 0xFF9B96A8); root.addView(nowSubtitle);
        LinearLayout controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER); controls.setPadding(0, dp(8), 0, 0);
        Button prev=button("|◀"); Button play=button("▶ / ❚❚"); Button next=button("▶|");
        controls.addView(prev); LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-2,dp(48)); pp.setMargins(dp(12),0,dp(12),0);
        controls.addView(play, pp); controls.addView(next); root.addView(controls);
        status = label("Offline + Online • Siap", 11, 0xFF777383); status.setGravity(Gravity.CENTER); status.setPadding(0,dp(8),0,0); root.addView(status);
        scan.setOnClickListener(v -> loadSongs());
        online.setOnClickListener(v -> showOnlineDialog());
        play.setOnClickListener(v -> { if (controller != null) { if (controller.isPlaying()) controller.pause(); else controller.play(); } });
        prev.setOnClickListener(v -> { if (controller != null) controller.seekToPreviousMediaItem(); });
        next.setOnClickListener(v -> { if (controller != null) controller.seekToNextMediaItem(); });
    }

    private void connectController() {
        SessionToken token = new SessionToken(this, new ComponentName(this, MusicService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try { controller = controllerFuture.get(); status.setText("Player terhubung • Siap memutar"); }
            catch (ExecutionException | InterruptedException e) { status.setText("Player gagal terhubung"); }
        }, command -> new Handler(Looper.getMainLooper()).post(command));
    }

    private void requestAudioPermission() {
        String permission = Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{permission}, REQ_AUDIO);
        else loadSongs();
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_AUDIO && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) loadSongs();
        else if (requestCode == REQ_AUDIO) status.setText("Izin audio belum diberikan");
    }

    private void loadSongs() {
        if (Build.VERSION.SDK_INT >= 23) {
            String permission = Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { requestAudioPermission(); return; }
        }
        songs.clear(); songList.removeAllViews();
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST};
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        try (Cursor cursor = getContentResolver().query(collection, projection, selection, null, MediaStore.Audio.Media.TITLE + " ASC")) {
            if (cursor != null) while (cursor.moveToNext()) {
                long id=cursor.getLong(0); String title=cursor.getString(1); String artist=cursor.getString(2);
                songs.add(new Song(ContentUris.withAppendedId(collection,id), title == null ? "Unknown title" : title, artist == null ? "Unknown artist" : artist));
            }
        } catch (Exception e) { status.setText("Gagal membaca library audio"); }
        for (int i=0;i<songs.size();i++) {
            final int index=i; Song s=songs.get(i);
            Button item=button("♫   " + s.title + "\n      " + s.artist);
            item.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT); item.setPadding(dp(12),dp(8),dp(8),dp(8));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(66)); lp.bottomMargin=dp(6); songList.addView(item,lp);
            item.setOnClickListener(v -> playSong(index));
        }
        status.setText(songs.size() + " lagu ditemukan");
        if (songs.isEmpty()) {
            TextView empty=label("Belum ada musik lokal. Simpan file audio di perangkat lalu tekan Scan musik.",13,0xFF9B96A8);
            empty.setPadding(dp(4),dp(16),dp(4),dp(16)); songList.addView(empty);
        }
    }
    private void playSong(int index) {
        if (controller == null) { Toast.makeText(this,"Player sedang disiapkan",Toast.LENGTH_SHORT).show(); return; }
        ArrayList<MediaItem> items=new ArrayList<>();
        for (Song s:songs) {
            MediaMetadata metadata=new MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist).build();
            items.add(new MediaItem.Builder().setUri(s.uri).setMediaMetadata(metadata).build());
        }
        controller.setMediaItems(items,index,0); controller.prepare(); controller.play();
        nowTitle.setText(songs.get(index).title); nowSubtitle.setText(songs.get(index).artist);
    }
    private void showOnlineDialog() {
        EditText input=new EditText(this); input.setSingleLine(true); input.setHint("https://contoh.com/audio.mp3");
        input.setTextColor(0xFFFFFFFF); input.setHintTextColor(0xFF888494);
        LinearLayout box=new LinearLayout(this); box.setPadding(dp(20),dp(8),dp(20),0); box.addView(input);
        new AlertDialog.Builder(this).setTitle("Putar dari URL").setMessage("Masukkan URL audio langsung yang legal dan dapat diakses. Mendukung format yang dikenali Media3, termasuk MP3 dan HLS.")
            .setView(box).setNegativeButton("Batal",null).setPositiveButton("Putar",(d,w)->{
                String url=input.getText().toString().trim();
                if (!(url.startsWith("https://") || url.startsWith("http://"))) { Toast.makeText(this,"Masukkan URL http atau https yang valid",Toast.LENGTH_LONG).show(); return; }
                if (controller==null) { Toast.makeText(this,"Player sedang disiapkan",Toast.LENGTH_SHORT).show(); return; }
                MediaItem item=new MediaItem.Builder().setUri(Uri.parse(url))
                    .setMediaMetadata(new MediaMetadata.Builder().setTitle("Online stream").setArtist(url).build()).build();
                controller.setMediaItem(item); controller.prepare(); controller.play();
                nowTitle.setText("Online stream"); nowSubtitle.setText(url); status.setText("Memutar audio online");
            }).show();
    }
    @Override protected void onDestroy() {
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
        controller = null;
        super.onDestroy();
    }
}
