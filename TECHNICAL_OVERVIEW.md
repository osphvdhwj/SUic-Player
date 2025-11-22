# SUic Player - Complete Technical Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture Analysis](#architecture-analysis)
3. [Core Components](#core-components)
4. [Feature Implementation](#feature-implementation)
5. [How the App Works](#how-the-app-works)
6. [File Structure & Functions](#file-structure--functions)
7. [Modification Guide](#modification-guide)
8. [Current Limitations](#current-limitations)
9. [MX Player & PipePipe Feature Comparison](#mx-player--pipepipe-feature-comparison)
10. [Enhancement Roadmap](#enhancement-roadmap)

---

## Project Overview

**SUic Player** is a powerful Android media player built on Google's **AndroidX Media3** (ExoPlayer) framework. It provides:
- Advanced codec support (AC3, E-AC3, DTS, DTS-HD, TrueHD)
- Bluetooth audio synchronization
- Comprehensive gesture controls
- Multi-format playback (video, audio, streaming)
- Subtitle management
- Picture-in-Picture (PiP) mode
- Background audio playback

### Technical Stack
- **Language**: Java
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 35 (Android 15)
- **Core Framework**: AndroidX Media3 v1.5.0
- **Build System**: Gradle with Android Plugin

---

## Architecture Analysis

### High-Level Architecture

```
┌─────────────────────────────────────────────────────┐
│              Application Layer                       │
│  (SoiadMahediApplication.java)                      │
│  - Global context management                        │
│  - Exception handling                               │
│  - Logging initialization                           │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│           Activity Layer                             │
│  - MainActivity (file browser)                      │
│  - PlayerActivity (video playback)                  │
│  - AudioPlayerActivity (audio playback)             │
│  - DebugActivity (error reporting)                  │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│         Media3 ExoPlayer Layer                       │
│  - ExoPlayer instance                               │
│  - MediaItem management                             │
│  - Track selection                                  │
│  - Subtitle rendering                               │
│  - Audio/Video synchronization                      │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│            Utility Layer                             │
│  - SoiadMahediUtils.java                            │
│  - SoiadMahediLogger.java                           │
│  - File management                                  │
│  - UI helpers                                       │
└─────────────────────────────────────────────────────┘
```

### Media3 ExoPlayer Integration

The app leverages multiple Media3 modules:

**Core Modules**:
- `media3-exoplayer`: Base player implementation
- `media3-ui`: Pre-built UI components (PlayerView)
- `media3-common`: Common utilities and interfaces

**Streaming Protocols**:
- `media3-exoplayer-dash`: DASH adaptive streaming
- `media3-exoplayer-hls`: HLS adaptive streaming
- `media3-exoplayer-smoothstreaming`: Smooth Streaming
- `media3-exoplayer-rtsp`: RTSP protocol

**Advanced Features**:
- `media3-exoplayer-midi`: MIDI file support
- `media3-exoplayer-ima`: IMA ads integration
- `media3-datasource-okhttp`: Network streaming
- `media3-datasource-rtmp`: RTMP streaming

**Additional Components**:
- `media3-session`: Background playback
- `media3-cast`: Chromecast support
- `media3-transformer`: Media transformation
- `media3-effect`: Visual effects

---

## Core Components

### 1. SoiadMahediApplication.java

**Purpose**: Main application class that initializes the app and handles global state.

**Key Functions**:
```java
public class SoiadMahediApplication extends Application {
    private static Context mApplicationContext;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    
    @Override
    public void onCreate() {
        // Initialize application context
        mApplicationContext = getApplicationContext();
        
        // Set up global exception handler
        Thread.setDefaultUncaughtExceptionHandler(
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    // Launch DebugActivity with error details
                    // Log stack trace
                    // Restart app with AlarmManager
                }
            });
        
        // Start logging system
        SoiadMahediLogger.startLogging();
        super.onCreate();
    }
}
```

**Responsibilities**:
- Provides global application context
- Catches uncaught exceptions and displays error screen
- Initializes logging system
- Schedules app restart on fatal errors

### 2. SoiadMahediUtils.java

**Purpose**: Comprehensive utility class with 400+ lines of helper functions.

**Key Function Categories**:

#### File Management
```java
// Check if file exists (content:// or file:// URI)
public static boolean fileExists(Context context, Uri uri)

// Get display name from URI
public static String getFileName(Context context, Uri uri)

// Check if URI is network stream
public static boolean isSupportedNetworkUri(Uri uri)
```

#### UI Utilities
```java
// Convert dp to pixels
public static int dp2px(Context context, float dp)

// Get screen dimensions
public static int getScreenWidth(Context context, boolean includeNavBar)
public static int getScreenHeight(Context context, boolean includeNavBar)

// Navigation bar detection
public static boolean hasNavigationBar(Context context)
public static int getNavigationBarHeight(Context context)

// Status bar dimensions
public static int getStatusBarHeight(Context context)

// System UI visibility control
public static void toggleSystemUi(Activity activity, View view, boolean show)
```

#### Video Format Utilities
```java
// Check if video format is portrait
public static boolean isPortrait(Format format)

// Check if video is rotated
public static boolean isRotated(Format format)

// Get aspect ratio
public static Rational getRational(Format format)
```

#### Audio Management
```java
// Check if volume is at minimum
public static boolean isVolumeMin(AudioManager audioManager)

// Samsung-specific fine volume control
private static int getVolume(Context context, boolean max, AudioManager audioManager)
```

#### Format Detection
```java
// Supported video extensions
public static final String[] supportedExtensionsVideo = 
    {"3gp", "m4v", "mkv", "mov", "mp4", "ts", "webm"};

// Supported subtitle extensions
public static final String[] supportedExtensionsSubtitle = 
    {"srt", "ssa", "ass", "vtt", "ttml", "dfxp", "xml"};

// Check if MIME type is subtitle
public static boolean isSubtitleType(String mimeType)
```

#### Formatting Utilities
```java
// Convert microseconds to readable duration (HH:MM:SS,ms)
public static String convertMicrosecondsToDuration(long microseconds)

// Format bitrate (bps, Kbps, Mbps, Gbps)
public static String formatBitrate(int bitrate)

// Get system time (HH:mm)
public static String getCurrentSystemTime()

// Get full language name from ISO code
public static String getLanguageFullName(String languageCode)
```

#### Gesture Detection
```java
// Detect if touch is near screen edge
public static boolean isEdge(Context context, MotionEvent event)
```

#### Localization
```java
// Get device language list
public static String[] getDeviceLanguages()

// Normalize font scale for subtitles
public static float normalizeFontScale(float fontScale, boolean small)
```

### 3. SoiadMahediLogger.java

**Purpose**: Custom logging system with broadcast functionality.

**Key Features**:
- Captures all Log statements
- Broadcasts logs via Intent
- Supports external log viewers
- Enables real-time debugging

---

## How the App Works

### Application Lifecycle

```
1. App Launch
   ├─→ SoiadMahediApplication.onCreate()
   │   ├─→ Initialize context
   │   ├─→ Set exception handler
   │   └─→ Start logging
   │
   ├─→ MainActivity (File Browser)
   │   ├─→ Scan media files
   │   ├─→ Display video/audio lists
   │   └─→ Handle file selection
   │
2. Media Playback
   ├─→ PlayerActivity (Video) or AudioPlayerActivity (Audio)
   │   ├─→ Create ExoPlayer instance
   │   ├─→ Load MediaItem from URI
   │   ├─→ Configure track selection
   │   ├─→ Set up UI controls
   │   └─→ Start playback
   │
3. Background/PiP
   ├─→ Media3 Session service
   │   ├─→ Continue audio playback
   │   ├─→ Show notification controls
   │   └─→ Handle media button events
   │
4. Error Handling
   └─→ DebugActivity
       ├─→ Display stack trace
       ├─→ Allow error reporting
       └─→ Restart app
```

### Media Playback Flow

```
┌─────────────────────────────────────────────────────┐
│  1. User selects media file                         │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│  2. Create MediaItem from URI                       │
│     - Parse file path or content URI                │
│     - Extract MIME type                             │
│     - Set metadata (title, artist, etc.)            │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│  3. Initialize ExoPlayer                            │
│     - Build player with Media3 modules              │
│     - Configure audio attributes                    │
│     - Set up track selector                         │
│     - Attach to PlayerView                          │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│  4. Prepare & Play                                  │
│     - Load media source                             │
│     - Buffer initial segments                       │
│     - Select best tracks                            │
│     - Start rendering                               │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│  5. Playback Loop                                   │
│     - Decode audio/video frames                     │
│     - Synchronize A/V streams                       │
│     - Render to surface                             │
│     - Handle user interactions                      │
│     - Update UI (progress, time, etc.)              │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│  6. Cleanup                                         │
│     - Release player resources                      │
│     - Clear surface                                 │
│     - Save playback position                        │
└─────────────────────────────────────────────────────┘
```

### Bluetooth Audio Sync Mechanism

One of SUic Player's key features is proper Bluetooth audio synchronization:

```java
// The app likely uses AudioTrack offset adjustment
// Media3 handles this automatically with:

private void configureAudioAttributes() {
    AudioAttributes audioAttributes = new AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .setUsage(C.USAGE_MEDIA)
        .build();
    
    player.setAudioAttributes(audioAttributes, true);
    
    // Media3 automatically compensates for Bluetooth latency
    // by adjusting audio presentation timestamp (PTS)
}
```

### Gesture Control System

The app implements comprehensive gesture controls:

**Horizontal Swipe**: Seek (forward/backward)
```java
// Detect horizontal swipe on PlayerView
float deltaX = currentX - startX;
long seekDelta = (long) (deltaX / screenWidth * totalDuration);
player.seekTo(currentPosition + seekDelta);
```

**Vertical Swipe (Right Side)**: Volume
```java
// Right side vertical swipe adjusts volume
float deltaY = currentY - startY;
if (x > screenWidth / 2) {
    int volumeChange = (int) (deltaY / screenHeight * maxVolume);
    audioManager.setStreamVolume(STREAM_MUSIC, currentVolume + volumeChange, 0);
}
```

**Vertical Swipe (Left Side)**: Brightness
```java
// Left side vertical swipe adjusts brightness
float deltaY = currentY - startY;
if (x < screenWidth / 2) {
    float brightnessChange = deltaY / screenHeight;
    LayoutParams lp = window.getAttributes();
    lp.screenBrightness = Math.max(0, Math.min(1, currentBrightness + brightnessChange));
    window.setAttributes(lp);
}
```

**Long Press**: Playback speed
```java
// Long press left = slow (0.5x), right = fast (2.0x)
if (longPressLeft) {
    player.setPlaybackSpeed(0.5f);
} else if (longPressRight) {
    player.setPlaybackSpeed(2.0f);
}
// Release = restore normal (1.0x)
```

**Pinch-to-Zoom**: Video zoom
```java
// Scale video surface based on pinch gesture
float scaleFactor = detector.getScaleFactor();
videoSurface.setScaleX(currentScaleX * scaleFactor);
videoSurface.setScaleY(currentScaleY * scaleFactor);
```

---

## File Structure & Functions

### Root Directory
```
SUic-Player/
├── README.md                    # Project documentation
├── build.gradle                 # Root build configuration
├── settings.gradle              # Project settings
├── gradle.properties            # Gradle properties
│
├── application/                 # Application code
│   ├── android/                 # Android-specific code
│   │   ├── SoiadMahediApplication.java   # App initialization
│   │   ├── SoiadMahediUtils.java         # Utility functions
│   │   ├── SoiadMahediLogger.java        # Logging system
│   │   └── app/                          # App module
│   │       ├── build.gradle              # App build config
│   │       ├── proguard-rules.pro        # Obfuscation rules
│   │       └── src/                      # Source code
│   │           ├── main/
│   │           │   ├── java/             # Java source files
│   │           │   ├── res/              # Resources
│   │           │   └── AndroidManifest.xml
│   │           └── debug/                # Debug resources
│   ├── linux/                   # Future Linux support
│   └── windows/                 # Future Windows support
│
├── assets/                      # App assets (images, fonts, etc.)
├── language-strings/            # Localization files (20+ languages)
├── screenshots/                 # App screenshots
└── webpage/                     # Web page resources
```

### Key Files Analysis

#### build.gradle (app)
```gradle
android {
    compileSdk 35
    namespace "com.soiadmahedi.suicTh"
    
    defaultConfig {
        applicationId "com.soiadmahedi.suicTh"
        minSdk 24          // Android 7.0+
        targetSdk 34       // Android 14
        versionCode 10
        versionName "1.9 SM"
        multiDexEnabled true
    }
    
    buildTypes {
        release {
            minifyEnabled true      // Enable ProGuard
            shrinkResources true    // Remove unused resources
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 
                         'proguard-rules.pro'
        }
    }
}

dependencies {
    // AndroidX Core
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.multidex:multidex:2.0.1'
    
    // Media3 ExoPlayer (Complete Suite)
    implementation "androidx.media3:media3-exoplayer:1.5.0"
    implementation "androidx.media3:media3-ui:1.5.0"
    implementation "androidx.media3:media3-session:1.5.0"
    // ... all other Media3 modules
    
    // Networking
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    
    // Image Loading
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    
    // Firebase & Ads
    implementation 'com.google.firebase:firebase-database:21.0.0'
    implementation 'com.google.android.gms:play-services-ads:23.6.0'
    
    // Utilities
    implementation 'com.google.code.gson:gson:2.11.0'
}
```

**Key Dependencies Breakdown**:

1. **Media3 ExoPlayer Suite** (1.5.0):
   - Core player with advanced features
   - All streaming protocol support
   - Complete codec support via FFmpeg extension
   - UI components and session management

2. **OkHttp** (4.12.0):
   - HTTP/HTTPS networking
   - Used by Media3 for streaming

3. **Glide** (4.16.0):
   - Image loading for thumbnails
   - Video frame extraction

4. **Google Play Services**:
   - AdMob integration for monetization
   - Firebase for analytics/database

5. **Material Design** (1.12.0):
   - Modern UI components
   - Theming support

---

## Modification Guide

### Adding New Features

#### 1. Add a New Gesture Control

**Location**: Create new gesture detector in PlayerActivity

```java
public class PlayerActivity extends AppCompatActivity {
    private GestureDetector gestureDetector;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Add custom gesture
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // Example: Double tap to skip 10 seconds
                long currentPos = player.getCurrentPosition();
                player.seekTo(currentPos + 10000);
                return true;
            }
            
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // Example: Fast swipe to next video
                if (Math.abs(velocityX) > 1000) {
                    playNextVideo();
                    return true;
                }
                return false;
            }
        });
    }
}
```

#### 2. Add Network Stream Support

**Location**: Update SoiadMahediUtils.java

```java
// Add new streaming protocol support
public static boolean isSupportedNetworkUri(Uri uri) {
    if (uri == null) return false;
    String scheme = uri.getScheme();
    if (scheme == null) return false;
    
    // Add more protocols
    return scheme.startsWith("http") 
        || scheme.equals("rtsp")
        || scheme.equals("rtmp")    // NEW
        || scheme.equals("mms")     // NEW
        || scheme.equals("mmsh");   // NEW
}
```

#### 3. Implement Video Downloader

**Steps**:
1. Add download permission to AndroidManifest.xml
2. Create DownloadService.java
3. Use OkHttp for downloading
4. Integrate with Media3's DownloadManager

```java
public class DownloadService extends Service {
    private DownloadManager downloadManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Media3 DownloadManager
        DatabaseProvider databaseProvider = new StandaloneDatabaseProvider(this);
        Cache downloadCache = new SimpleCache(
            new File(getExternalFilesDir(null), "downloads"),
            new LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024),  // 500MB
            databaseProvider
        );
        
        downloadManager = new DownloadManager(
            this,
            databaseProvider,
            downloadCache,
            new OkHttpDataSource.Factory(new OkHttpClient())
        );
    }
    
    public void downloadVideo(Uri uri, String title) {
        DownloadRequest request = new DownloadRequest.Builder(uri.toString(), uri)
            .setMimeType(MimeTypes.VIDEO_MP4)
            .setData(title.getBytes())
            .build();
        
        downloadManager.addDownload(request);
    }
}
```

#### 4. Add PipePipe-Style Piping

**Concept**: Play video while browsing other content

```java
public class PipingActivity extends AppCompatActivity {
    private ExoPlayer backgroundPlayer;
    private PlayerView miniPlayer;
    private RecyclerView contentList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piping);
        
        // Mini player at top
        miniPlayer = findViewById(R.id.mini_player);
        contentList = findViewById(R.id.content_list);
        
        // Create background player
        backgroundPlayer = new ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .build();
        
        miniPlayer.setPlayer(backgroundPlayer);
        miniPlayer.setControllerAutoShow(false);
        
        // Allow browsing while video plays in mini window
        setupContentBrowsing();
    }
    
    private void setupContentBrowsing() {
        // Load content list (like PipePipe's feed)
        // User can scroll while video plays in mini player
    }
}
```

#### 5. Integrate Ad-Blocker

**Approach**: Use custom DataSource to filter ad requests

```java
public class AdBlockDataSource implements DataSource {
    private final DataSource wrappedSource;
    private final Set<String> adDomains;
    
    public AdBlockDataSource(DataSource wrapped) {
        this.wrappedSource = wrapped;
        this.adDomains = loadAdBlockList();
    }
    
    private Set<String> loadAdBlockList() {
        // Load ad domain blacklist
        return new HashSet<>(Arrays.asList(
            "googlesyndication.com",
            "doubleclick.net",
            "ads.youtube.com"
            // ... more ad domains
        ));
    }
    
    @Override
    public long open(DataSpec dataSpec) throws IOException {
        Uri uri = dataSpec.uri;
        String host = uri.getHost();
        
        // Block if ad domain
        if (adDomains.contains(host)) {
            throw new IOException("Blocked: " + host);
        }
        
        return wrappedSource.open(dataSpec);
    }
    
    // Implement other DataSource methods...
}

// Use in player:
DataSource.Factory dataSourceFactory = new DataSource.Factory() {
    @Override
    public DataSource createDataSource() {
        return new AdBlockDataSource(
            new DefaultHttpDataSource.Factory().createDataSource()
        );
    }
};

ExoPlayer player = new ExoPlayer.Builder(context)
    .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
    .build();
```

### Modifying UI/UX

#### Custom Player Controls

**Location**: Create custom_player_controls.xml in res/layout/

```xml
<?xml version="1.0" encoding="utf-8"?>
<merge xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Top Bar -->
    <LinearLayout
        android:id="@+id/top_bar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp">
        
        <ImageButton
            android:id="@+id/btn_back"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@drawable/ic_back" />
        
        <TextView
            android:id="@+id/video_title"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textColor="#FFFFFF"
            android:textSize="16sp" />
        
        <ImageButton
            android:id="@+id/btn_settings"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@drawable/ic_settings" />
    </LinearLayout>
    
    <!-- Center Controls -->
    <LinearLayout
        android:id="@+id/center_controls"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:orientation="horizontal">
        
        <ImageButton
            android:id="@+id/btn_rewind"
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:src="@drawable/ic_rewind_10" />
        
        <ImageButton
            android:id="@+id/btn_play_pause"
            android:layout_width="72dp"
            android:layout_height="72dp"
            android:layout_marginHorizontal="32dp"
            android:src="@drawable/ic_play" />
        
        <ImageButton
            android:id="@+id/btn_forward"
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:src="@drawable/ic_forward_10" />
    </LinearLayout>
    
    <!-- Bottom Bar -->
    <LinearLayout
        android:id="@+id/bottom_bar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        android:orientation="vertical"
        android:padding="16dp">
        
        <!-- Progress Bar -->
        <SeekBar
            android:id="@+id/progress_bar"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
        
        <!-- Time & Controls -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">
            
            <TextView
                android:id="@+id/current_time"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="#FFFFFF"
                android:text="00:00" />
            
            <Space
                android:layout_width="0dp"
                android:layout_height="1dp"
                android:layout_weight="1" />
            
            <ImageButton
                android:id="@+id/btn_subtitle"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:src="@drawable/ic_subtitle" />
            
            <ImageButton
                android:id="@+id/btn_audio_track"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:src="@drawable/ic_audio" />
            
            <ImageButton
                android:id="@+id/btn_quality"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:src="@drawable/ic_quality" />
            
            <Space
                android:layout_width="0dp"
                android:layout_height="1dp"
                android:layout_weight="1" />
            
            <TextView
                android:id="@+id/total_time"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="#FFFFFF"
                android:text="00:00" />
        </LinearLayout>
    </LinearLayout>
    
</merge>
```

### Performance Optimization

#### 1. Enable Hardware Acceleration

**AndroidManifest.xml**:
```xml
<application
    android:hardwareAccelerated="true"
    android:largeHeap="true">
    
    <activity
        android:name=".PlayerActivity"
        android:hardwareAccelerated="true" />
</application>
```

#### 2. Optimize ProGuard Rules

**proguard-rules.pro**:
```proguard
# Keep Media3 classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep ExoPlayer classes
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# Keep model classes
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Optimize aggressively
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
```

#### 3. Implement Caching

```java
public class CachedPlayerFactory {
    private static final long CACHE_SIZE = 100 * 1024 * 1024; // 100MB
    
    public static ExoPlayer createCachedPlayer(Context context) {
        // Setup cache
        DatabaseProvider databaseProvider = new StandaloneDatabaseProvider(context);
        Cache cache = new SimpleCache(
            new File(context.getCacheDir(), "media"),
            new LeastRecentlyUsedCacheEvictor(CACHE_SIZE),
            databaseProvider
        );
        
        // Create cached data source
        CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(
                new DefaultHttpDataSource.Factory()
                    .setUserAgent("SUicPlayer/1.9")
            );
        
        // Build player with cache
        return new ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                new DefaultMediaSourceFactory(cacheDataSourceFactory)
            )
            .build();
    }
}
```

---

## Current Limitations

### 1. Closed Source
**Issue**: Core application code is not included in repository
- Only utility classes and build files are available
- Main Activities (PlayerActivity, MainActivity, etc.) are missing
- Cannot see complete implementation details

**Impact**:
- Limited modification capability
- Cannot add features without reverse engineering APK
- Community contributions restricted

**Workaround**:
- Request source code from developer
- Decompile APK using jadx or similar tools
- Build features as separate modules

### 2. No CI/CD Pipeline
**Issue**: No automated build/test system
- No GitHub Actions workflow
- No automated testing
- Manual release process

**Impact**:
- Slower development cycle
- Potential for bugs in releases
- No automated quality checks

**Solution**:
```yaml
# .github/workflows/android.yml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    - name: Build with Gradle
      run: ./gradlew assembleDebug
    - name: Run tests
      run: ./gradlew test
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

### 3. Limited Documentation
**Issue**: Minimal code comments and documentation
- No inline documentation
- No architecture diagrams
- Limited API documentation

**Impact**:
- Hard for new contributors
- Difficult to understand complex logic
- Maintenance challenges

**Solution**: This documentation helps, but need:
- KDoc/JavaDoc comments in code
- Architecture decision records (ADRs)
- Developer wiki

### 4. No Testing Framework
**Issue**: No unit tests or UI tests
- No JUnit tests
- No Espresso UI tests
- No integration tests

**Impact**:
- Regression bugs possible
- Manual testing required
- Quality concerns

**Solution**:
```kotlin
// Example unit test
class SoiadMahediUtilsTest {
    @Test
    fun testFormatBitrate() {
        assertEquals("1.0 Kbps", SoiadMahediUtils.formatBitrate(1024))
        assertEquals("1.0 Mbps", SoiadMahediUtils.formatBitrate(1024 * 1024))
    }
    
    @Test
    fun testConvertMicrosecondsToDuration() {
        assertEquals("00:30", SoiadMahediUtils.convertMicrosecondsToDuration(30000000))
        assertEquals("01:30", SoiadMahediUtils.convertMicrosecondsToDuration(90000000))
    }
}
```

### 5. Android TV Support Issues
**Issue**: Limited Android TV optimization
- No D-pad navigation
- No leanback support
- No TV-specific UI

**Impact**:
- Poor TV experience
- Navigation difficult with remote
- Not on Google Play for TV

**Solution**:
```xml
<!-- AndroidManifest.xml -->
<manifest>
    <uses-feature
        android:name="android.software.leanback"
        android:required="false" />
    
    <uses-feature
        android:name="android.hardware.touchscreen"
        android:required="false" />
    
    <application
        android:banner="@drawable/tv_banner">
        
        <activity
            android:name=".TvPlayerActivity"
            android:theme="@style/Theme.Leanback">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### 6. No Playlist Management
**Issue**: Cannot create/save playlists
- No M3U support
- No playlist editor
- Cannot save queue

**Impact**:
- Manual file selection each time
- No continuous playback setup
- Poor user experience

**Solution**: Implement playlist database
```java
@Entity(tableName = "playlists")
public class Playlist {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    @ColumnInfo(name = "name")
    public String name;
    
    @ColumnInfo(name = "created_at")
    public long createdAt;
}

@Entity(tableName = "playlist_items")
public class PlaylistItem {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    @ColumnInfo(name = "playlist_id")
    public int playlistId;
    
    @ColumnInfo(name = "uri")
    public String uri;
    
    @ColumnInfo(name = "position")
    public int position;
}
```

### 7. No Cloud Integration
**Issue**: Cannot access cloud storage
- No Google Drive support
- No Dropbox integration
- No network shares (SMB/NFS)

**Impact**:
- Limited to local files
- Cannot access media servers
- No sync across devices

**Solution**: Add content providers
```java
public class CloudStorageManager {
    public List<MediaFile> getGoogleDriveFiles(GoogleSignInAccount account) {
        Drive driveService = new Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            new GsonFactory(),
            GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE_FILE))
        ).build();
        
        // List video files
        FileList result = driveService.files().list()
            .setQ("mimeType contains 'video/'")
            .setFields("files(id, name, mimeType, webViewLink)")
            .execute();
        
        return convertToMediaFiles(result.getFiles());
    }
}
```

### 8. Limited Subtitle Customization
**Issue**: Basic subtitle styling only
- Cannot change font
- Limited positioning
- No outline/shadow control

**Impact**:
- Subtitles may be hard to read
- No personalization
- Accessibility concerns

**Solution**: Enhance subtitle renderer
```java
public class CustomSubtitlePainter {
    private TextPaint textPaint;
    private Paint backgroundPaint;
    
    public void setSubtitleStyle(SubtitleStyle style) {
        textPaint.setTypeface(Typeface.create(style.fontFamily, Typeface.NORMAL));
        textPaint.setTextSize(style.fontSize);
        textPaint.setColor(style.textColor);
        textPaint.setShadowLayer(style.shadowRadius, 0, 0, style.shadowColor);
        textPaint.setStrokeWidth(style.outlineWidth);
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        
        backgroundPaint.setColor(style.backgroundColor);
        backgroundPaint.setAlpha(style.backgroundOpacity);
    }
}
```

---

## MX Player & PipePipe Feature Comparison

### MX Player Features

| Feature | SUic Player | Implementation Approach |
|---------|-------------|-------------------------|
| **Hardware Acceleration** | ✅ Yes (Media3) | Already supported via Media3's hardware decoder |
| **Multi-core Decoding** | ✅ Yes | Media3 handles automatically |
| **Pinch to Zoom** | ✅ Yes | Implemented in gesture controls |
| **Kids Lock** | ❌ No | Add overlay with input blocking |
| **Screen Rotation Lock** | ❌ No | Add orientation lock toggle |
| **Background Play** | ✅ Yes | Via Media3 session |
| **Speed Control** | ✅ Yes (gesture) | setPlaybackParameters() |
| **Subtitle Sync** | ❌ Limited | Add manual delay adjustment |
| **Network Streams** | ✅ Yes | RTSP, HTTP/S supported |
| **Chromecast** | ✅ Yes | Via media3-cast module |
| **Equalizer** | ❌ No | Add with AudioEffect API |

### PipePipe Features

| Feature | SUic Player | Implementation Approach |
|---------|-------------|-------------------------|
| **YouTube Integration** | ❌ No | Use NewPipe Extractor library |
| **SponsorBlock** | ❌ No | Integrate SponsorBlock API |
| **Return YouTube Dislike** | ❌ No | Integrate RYD API |
| **Login Support** | ❌ No | Cookie-based auth system |
| **Danmaku Comments** | ❌ No | Create overlay comment system |
| **AV1/VP9 Support** | ✅ Yes | Media3 supports both codecs |
| **Music Mode** | ⚠️ Partial | Enhance with music-specific UI |
| **Download Manager** | ❌ No | Use Media3 DownloadManager |
| **Playlist Download** | ❌ No | Batch download implementation |
| **Search Filters** | ❌ No | Add filter UI + logic |
| **Block Shorts** | ❌ No | Content filtering system |
| **Sleep Timer** | ❌ No | Add timer with auto-pause |
| **Swipe-to-Seek** | ✅ Yes | Already implemented |
| **Local Playlist** | ❌ No | Room database for playlists |

### Feature Priority Matrix

**High Priority** (User Demand + Easy Implementation):
1. ✅ Playlist Management (Room DB)
2. ✅ Download Manager (Media3 built-in)
3. ✅ Sleep Timer (Simple scheduling)
4. ✅ Equalizer (Android AudioEffect)
5. ✅ Kids Lock (Input blocking overlay)

**Medium Priority** (Useful + Moderate Complexity):
1. ⚠️ YouTube Integration (NewPipe Extractor)
2. ⚠️ Subtitle Customization (Enhanced rendering)
3. ⚠️ Cloud Storage (Google Drive API)
4. ⚠️ Music Mode UI (Separate layout)
5. ⚠️ Network Shares (SMB/NFS support)

**Low Priority** (Complex + Niche):
1. 🔴 Danmaku Comments (Heavy implementation)
2. 🔴 SponsorBlock (API + detection logic)
3. 🔴 Advanced AI features (High complexity)

---

## Enhancement Roadmap

### Phase 1: Core Improvements (Weeks 1-4)

#### Week 1: Documentation & Setup
- ✅ Complete technical documentation
- ⬜ Setup CI/CD pipeline
- ⬜ Add unit tests for utilities
- ⬜ Create developer wiki

#### Week 2: Essential Features
- ⬜ Implement playlist management
- ⬜ Add sleep timer
- ⬜ Create equalizer UI
- ⬜ Improve subtitle customization

#### Week 3: UI/UX Enhancements
- ⬜ Redesign player controls (MX Player-inspired)
- ⬜ Add kids lock mode
- ⬜ Implement screen rotation lock
- ⬜ Create settings screen

#### Week 4: Performance
- ⬜ Optimize ProGuard rules
- ⬜ Implement caching system
- ⬜ Add thumbnail generation
- ⬜ Reduce APK size

### Phase 2: Advanced Features (Weeks 5-8)

#### Week 5: Download Manager
- ⬜ Integrate Media3 DownloadManager
- ⬜ Create download queue UI
- ⬜ Add pause/resume functionality
- ⬜ Implement notification tracking

#### Week 6: Network Features
- ⬜ Add network stream browser
- ⬜ Implement SMB/NFS support
- ⬜ Create server discovery
- ⬜ Add bookmark management

#### Week 7: Cloud Integration
- ⬜ Google Drive integration
- ⬜ Dropbox support
- ⬜ OneDrive support
- ⬜ Cloud sync for settings

#### Week 8: Music Mode
- ⬜ Design music player UI
- ⬜ Add album art extraction
- ⬜ Create now playing screen
- ⬜ Implement music queue

### Phase 3: YouTube & Streaming (Weeks 9-12)

#### Week 9: YouTube Integration
- ⬜ Integrate NewPipe Extractor
- ⬜ Add YouTube search
- ⬜ Implement video browsing
- ⬜ Create subscriptions manager

#### Week 10: Advanced Streaming
- ⬜ Add SponsorBlock support
- ⬜ Integrate Return YouTube Dislike
- ⬜ Implement login system
- ⬜ Add live stream support

#### Week 11: PipePipe Features
- ⬜ Create piping mode UI
- ⬜ Add background browser
- ⬜ Implement mini player
- ⬜ Create recommendation feed

#### Week 12: Polishing
- ⬜ Fix all reported bugs
- ⬜ Optimize performance
- ⬜ Complete localization
- ⬜ Prepare release

### Phase 4: Pro Features (Ongoing)

#### Advanced Playback
- ⬜ Frame-by-frame control
- ⬜ A-B repeat
- ⬜ Bookmarks within videos
- ⬜ Chapter support

#### AI Features
- ⬜ Auto subtitle generation
- ⬜ Scene detection
- ⬜ Content recommendations
- ⬜ Smart playlists

#### Social Features
- ⬜ Watch party mode
- ⬜ Comments system
- ⬜ Sharing functionality
- ⬜ User profiles

---

## Development Best Practices

### 1. Code Organization
```
com.soiadmahedi.suicTh/
├── ui/
│   ├── activities/
│   │   ├── MainActivity
│   │   ├── PlayerActivity
│   │   └── SettingsActivity
│   ├── fragments/
│   │   ├── VideoListFragment
│   │   └── PlaylistFragment
│   └── adapters/
│       └── VideoAdapter
├── player/
│   ├── ExoPlayerManager
│   ├── SubtitleManager
│   └── GestureHandler
├── data/
│   ├── models/
│   │   ├── Video
│   │   └── Playlist
│   ├── database/
│   │   └── AppDatabase
│   └── repositories/
│       └── VideoRepository
├── network/
│   ├── NetworkManager
│   └── DownloadManager
└── utils/
    ├── SoiadMahediUtils
    └── FileUtils
```

### 2. Git Workflow
```bash
# Main branches
main          # Production-ready code
develop       # Development branch

# Feature branches
feature/playlist-manager
feature/download-manager
feature/youtube-integration

# Bugfix branches
bugfix/subtitle-sync
bugfix/bluetooth-audio

# Release branches
release/2.0
release/2.1
```

### 3. Version Naming
```
Version Format: MAJOR.MINOR INITIALS (BUILD)

Example:
2.0 SM (11)
│ │  │   └── Build number (sequential)
│ │  └── Developer initials (Soiad Mahedi)
│ └── Minor version (features)
└── Major version (breaking changes)
```

### 4. Testing Strategy
```kotlin
// Unit Tests
class SoiadMahediUtilsTest { }
class FormatUtilsTest { }

// Integration Tests
class PlayerIntegrationTest { }
class DownloadIntegrationTest { }

// UI Tests
class PlayerActivityTest { }
class PlaylistUITest { }

// Performance Tests
class VideoDecodingBenchmark { }
class MemoryLeakTest { }
```

### 5. Documentation Standards
```java
/**
 * Converts microseconds to human-readable duration format.
 * 
 * @param microseconds Time in microseconds (1/1,000,000 second)
 * @return Formatted string in HH:MM:SS,ms format
 * 
 * @example
 * convertMicrosecondsToDuration(90000000) // Returns "01:30"
 * convertMicrosecondsToDuration(3661000000) // Returns "1:01:01"
 */
public static String convertMicrosecondsToDuration(long microseconds) {
    // Implementation
}
```

---

## Conclusion

SUic Player is a well-architected media player built on Google's modern Media3 framework. While the current implementation covers essential playback features excellently, there's significant potential for expansion:

**Strengths**:
✅ Solid Media3 foundation
✅ Comprehensive codec support
✅ Gesture control system
✅ Multi-language support
✅ Bluetooth audio sync

**Areas for Improvement**:
❌ Closed source limits contributions
❌ Missing advanced features (playlists, downloads)
❌ No YouTube/streaming integration
❌ Limited testing and CI/CD
❌ Basic UI compared to MX Player

**Recommended Next Steps**:
1. Implement Phase 1 improvements (core features)
2. Add comprehensive testing
3. Set up CI/CD pipeline
4. Consider open-sourcing core components
5. Build community around the project

With the roadmap outlined in this document, SUic Player can evolve into a feature-complete media player that rivals MX Player while incorporating modern features from apps like PipePipe.

---

**Document Version**: 1.0
**Last Updated**: November 22, 2025
**Author**: Technical Documentation Team
**Branch**: technical-documentation
