# SUic Player - Quick Reference Guide

## ⚡ Essential Functions at a Glance

### File Operations

```java
// Check if file exists
boolean exists = SoiadMahediUtils.fileExists(context, uri);

// Get display name (without extension)
String name = SoiadMahediUtils.getFileName(context, uri);

// Check if network stream
boolean isNetwork = SoiadMahediUtils.isSupportedNetworkUri(uri);
```

### UI Utilities

```java
// Convert dp to pixels
int pixels = SoiadMahediUtils.dp2px(context, 48);

// Get screen dimensions
int width = SoiadMahediUtils.getScreenWidth(context, false);
int height = SoiadMahediUtils.getScreenHeight(context, false);

// Toggle fullscreen
SoiadMahediUtils.toggleSystemUi(activity, view, false);  // Hide
SoiadMahediUtils.toggleSystemUi(activity, view, true);   // Show

// Check edge touch
if (SoiadMahediUtils.isEdge(context, motionEvent)) {
    // Near screen edge
}
```

### Video Format

```java
// Check orientation
boolean isPortrait = SoiadMahediUtils.isPortrait(format);
boolean isRotated = SoiadMahediUtils.isRotated(format);

// Get aspect ratio
Rational aspectRatio = SoiadMahediUtils.getRational(format);
```

### Audio

```java
// Check minimum volume
boolean isMin = SoiadMahediUtils.isVolumeMin(audioManager);
```

### Formatting

```java
// Format duration (microseconds to HH:MM:SS)
String time = SoiadMahediUtils.convertMicrosecondsToDuration(90000000);
// Result: "01:30"

// Format bitrate
String bitrate = SoiadMahediUtils.formatBitrate(5000000);
// Result: "4.8 Mbps"

// Check subtitle MIME type
boolean isSub = SoiadMahediUtils.isSubtitleType(mimeType);

// Get language name
String lang = SoiadMahediUtils.getLanguageFullName("en");
// Result: "English"
```

---

## 🎬 ExoPlayer Quick Setup

### Basic Player

```java
// Create player
ExoPlayer player = new ExoPlayer.Builder(context)
    .setAudioAttributes(AudioAttributes.DEFAULT, true)
    .build();

// Attach to view
PlayerView playerView = findViewById(R.id.player_view);
playerView.setPlayer(player);

// Load and play
MediaItem mediaItem = MediaItem.fromUri(videoUri);
player.setMediaItem(mediaItem);
player.prepare();
player.play();

// Release when done
player.release();
```

### With Caching

```java
// Setup cache
Cache cache = new SimpleCache(
    new File(context.getCacheDir(), "media"),
    new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024)  // 100MB
);

// Create cached data source
CacheDataSource.Factory factory = new CacheDataSource.Factory()
    .setCache(cache)
    .setUpstreamDataSourceFactory(
        new DefaultHttpDataSource.Factory()
    );

// Build player with cache
ExoPlayer player = new ExoPlayer.Builder(context)
    .setMediaSourceFactory(new DefaultMediaSourceFactory(factory))
    .build();
```

---

## 👆 Gesture Patterns

### Horizontal Swipe (Seek)

```java
@Override
public boolean onTouchEvent(MotionEvent event) {
    switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            startX = event.getX();
            startPosition = player.getCurrentPosition();
            break;
            
        case MotionEvent.ACTION_MOVE:
            float deltaX = event.getX() - startX;
            long seekDelta = (long) (deltaX / getWidth() * player.getDuration());
            long newPosition = startPosition + seekDelta;
            player.seekTo(Math.max(0, Math.min(newPosition, player.getDuration())));
            break;
    }
    return true;
}
```

### Vertical Swipe (Volume/Brightness)

```java
@Override
public boolean onTouchEvent(MotionEvent event) {
    float x = event.getX();
    float y = event.getY();
    
    if (event.getAction() == MotionEvent.ACTION_MOVE) {
        float deltaY = y - startY;
        
        if (x > getWidth() / 2) {
            // Right side - Volume
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int volumeChange = (int) (-deltaY / getHeight() * maxVolume);
            int newVolume = Math.max(0, Math.min(currentVolume + volumeChange, maxVolume));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
        } else {
            // Left side - Brightness
            float brightnessChange = -deltaY / getHeight();
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = Math.max(0, Math.min(1, currentBrightness + brightnessChange));
            getWindow().setAttributes(lp);
        }
    }
    return true;
}
```

---

## 📦 Common Dependencies

### build.gradle (app)

```gradle
dependencies {
    // Media3 ExoPlayer
    def media3Version = "1.5.0"
    implementation "androidx.media3:media3-exoplayer:$media3Version"
    implementation "androidx.media3:media3-ui:$media3Version"
    implementation "androidx.media3:media3-session:$media3Version"
    
    // Networking
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    
    // Image Loading
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    
    // JSON
    implementation 'com.google.code.gson:gson:2.11.0'
}
```

---

## 🛠️ Common Patterns

### Error Handling

```java
try {
    // Risky operation
    Uri uri = Uri.parse(input);
    if (SoiadMahediUtils.fileExists(context, uri)) {
        player.setMediaItem(MediaItem.fromUri(uri));
    }
} catch (Exception e) {
    Log.e(TAG, "Error loading media", e);
    Toast.makeText(context, "Failed to load video", Toast.LENGTH_SHORT).show();
}
```

### Background Thread

```java
new Thread(() -> {
    try {
        // Heavy operation
        List<Video> videos = scanVideos();
        
        runOnUiThread(() -> {
            // Update UI
            adapter.setVideos(videos);
        });
    } catch (Exception e) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Scan failed", Toast.LENGTH_SHORT).show();
        });
    }
}).start();
```

### Lifecycle Management

```java
public class PlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    
    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= 24) {
            initializePlayer();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < 24 || player == null) {
            initializePlayer();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT < 24) {
            releasePlayer();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT >= 24) {
            releasePlayer();
        }
    }
}
```

---

## 📝 Supported Formats

### Video Codecs
- H.263, H.264 (AVC), H.265 (HEVC)
- MPEG-4 SP
- VP8, VP9
- AV1

### Audio Codecs
- AAC (LC/HE/ELD/xHE)
- MP3, MP2, MP1
- Vorbis, Opus
- FLAC, ALAC
- AC-3, E-AC3
- DTS, DTS-HD
- TrueHD
- PCM/WAVE

### Containers
- MP4, MOV
- MKV (Matroska)
- WebM
- AVI (limited)
- FLV
- MPEG-TS, MPEG-PS
- Ogg

### Streaming
- DASH
- HLS
- SmoothStreaming
- RTSP
- HTTP/HTTPS

### Subtitles
- SRT (SubRip)
- SSA/ASS (SubStation Alpha)
- WebVTT
- TTML
- DVB

---

## ⚡ Performance Tips

### 1. Enable Hardware Acceleration
```xml
<application android:hardwareAccelerated="true">
```

### 2. Use ProGuard
```gradle
buildTypes {
    release {
        minifyEnabled true
        shrinkResources true
    }
}
```

### 3. Optimize Images with Glide
```java
Glide.with(context)
    .load(uri)
    .override(200, 200)  // Resize
    .centerCrop()
    .into(imageView);
```

### 4. Implement Caching
```java
// Cache media for offline playback
Cache cache = new SimpleCache(cacheDir, new LeastRecentlyUsedCacheEvictor(500_000_000));
```

### 5. Lazy Load
```java
// Don't load all videos at once
recyclerView.addOnScrollListener(new EndlessScrollListener() {
    @Override
    public void onLoadMore() {
        loadNextPage();
    }
});
```

---

## 🔒 Permissions

### Required
```xml
<!-- Internet for streaming -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Storage access -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" 
    android:minSdkVersion="30" />

<!-- Notifications for background play -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

---

## 🐛 Common Issues & Solutions

### Issue: "File not found"
**Solution**: Check URI validity
```java
if (!SoiadMahediUtils.fileExists(context, uri)) {
    Log.e(TAG, "File doesn't exist: " + uri);
    return;
}
```

### Issue: "Player not releasing"
**Solution**: Always call release()
```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (player != null) {
        player.release();
        player = null;
    }
}
```

### Issue: "Subtitles not showing"
**Solution**: Check track selection
```java
TrackSelector trackSelector = player.getTrackSelector();
if (trackSelector instanceof DefaultTrackSelector) {
    ((DefaultTrackSelector) trackSelector).setParameters(
        trackSelector.getParameters()
            .buildUpon()
            .setPreferredTextLanguage("en")
            .build()
    );
}
```

### Issue: "Audio out of sync"
**Solution**: Enable audio sync
```java
player.setAudioAttributes(
    new AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .setUsage(C.USAGE_MEDIA)
        .build(),
    true  // Handle audio focus
);
```

---

## 🔗 Useful Links

- [Media3 Documentation](https://developer.android.com/media/media3)
- [ExoPlayer GitHub](https://github.com/google/ExoPlayer)
- [Android Developer Guides](https://developer.android.com/guide)
- [Material Design](https://material.io/)

---

**Last Updated**: November 22, 2025
**Version**: 1.0
**Branch**: technical-documentation
