# SUic Player - Detailed Code Analysis

## Complete File & Function Reference

### 1. SoiadMahediApplication.java - Deep Dive

**File Location**: `application/android/SoiadMahediApplication.java`

**Purpose**: Application-level initialization and global error handling

#### Complete Code Analysis

```java
package com.soiadmahedi.suicTh;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

public class SoiadMahediApplication extends Application {
    // Static context accessible throughout app
    private static Context mApplicationContext;
    
    // Original exception handler (backup)
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
```

**Key Variables**:
- `mApplicationContext`: Global application context for accessing system services
- `uncaughtExceptionHandler`: Stores default exception handler to chain calls

#### getContext() Method
```java
public static Context getContext() {
    return mApplicationContext;
}
```

**Purpose**: Provides global access to application context

**Usage Example**:
```java
// From anywhere in the app
Context ctx = SoiadMahediApplication.getContext();
Resources res = ctx.getResources();
String appName = res.getString(R.string.app_name);
```

#### onCreate() Method - Detailed Breakdown

```java
@Override
public void onCreate() {
    // 1. Store application context globally
    mApplicationContext = getApplicationContext();
    
    // 2. Get system's default exception handler
    this.uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
```

**Step 1**: Stores context for global access via static method

**Step 2**: Preserves default handler to maintain system behavior

```java
    // 3. Set custom exception handler
    Thread.setDefaultUncaughtExceptionHandler(
    new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            // 3a. Create intent for DebugActivity
            Intent intent = new Intent(getApplicationContext(), DebugActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("error", Log.getStackTraceString(throwable));
```

**Step 3a**: Creates intent to launch DebugActivity with error details
- `FLAG_ACTIVITY_CLEAR_TASK`: Clears all activities from task
- `Log.getStackTraceString()`: Converts exception to readable string

```java
            // 3b. Create pending intent for delayed restart
            PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 
                11111,  // Request code
                intent, 
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
            );
```

**Step 3b**: Wraps intent in PendingIntent for AlarmManager
- `FLAG_ONE_SHOT`: Can only be used once
- `FLAG_IMMUTABLE`: Cannot be modified (security requirement Android 12+)
- Request code `11111`: Arbitrary unique identifier

```java
            // 3c. Schedule app restart after 1 second
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            am.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP, 
                1000,  // 1 second delay
                pendingIntent
            );
```

**Step 3c**: Schedules activity launch after 1 second
- `ELAPSED_REALTIME_WAKEUP`: Wakes device if sleeping
- `1000`: Milliseconds until execution

```java
            // 3d. Broadcast error log
            SoiadMahediLogger.broadcastLog(Log.getStackTraceString(throwable));
            
            // 3e. Kill current process
            Process.killProcess(Process.myPid());
            System.exit(1);
```

**Step 3d**: Sends error to external log viewers via broadcast

**Step 3e**: Forcibly terminates app
- `killProcess()`: Immediate termination
- `System.exit(1)`: Non-zero = abnormal termination

```java
            // 3f. Chain to original handler
            uncaughtExceptionHandler.uncaughtException(thread, throwable);
        }
    });
```

**Step 3f**: Calls original handler (probably won't execute due to process kill)

```java
    // 4. Initialize logging system
    SoiadMahediLogger.startLogging();
    
    // 5. Call super
    super.onCreate();
}
```

**Step 4**: Starts logging broadcast service

**Step 5**: Calls Android Application.onCreate()

#### Flow Diagram

```
App Launches
    │
    ↓
SoiadMahediApplication.onCreate()
    │
    ├──→ Store global context
    ├──→ Setup exception handler
    ├──→ Start logging
    └──→ Continue to MainActivity
    
    
[If exception occurs anywhere]
    │
    ↓
Custom Exception Handler Triggered
    │
    ├──→ Create DebugActivity intent
    ├──→ Schedule via AlarmManager (1s)
    ├──→ Broadcast error log
    ├──→ Kill process
    │
    ↓
[1 second later]
    │
    ↓
App Restarts
    │
    ↓
DebugActivity Shows Error
```

---

### 2. SoiadMahediUtils.java - Complete Function Reference

**File Location**: `application/android/SoiadMahediUtils.java`

**Lines**: 400+

**Purpose**: Comprehensive utility library for the entire app

#### A. File Management Functions

##### fileExists()
```java
public static boolean fileExists(final Context context, final Uri uri) {
    final String scheme = uri.getScheme();
    
    if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
        // content:// URI (from MediaStore or SAF)
        try {
            final InputStream inputStream = context.getContentResolver().openInputStream(uri);
            inputStream.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    } else {
        // file:// URI or plain path
        String path;
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            path = uri.getPath();
        } else {
            path = uri.toString();
        }
        final File file = new File(path);
        return file.exists();
    }
}
```

**How it works**:
1. Checks URI scheme (content://, file://, or plain path)
2. For content URIs: Attempts to open input stream
3. For file URIs: Uses standard File.exists()

**Use Cases**:
- Validate user-selected video before playback
- Check if subtitle file exists
- Verify downloaded file integrity

**Example Usage**:
```java
Uri videoUri = Uri.parse("content://media/external/video/123");
if (SoiadMahediUtils.fileExists(context, videoUri)) {
    player.setMediaItem(MediaItem.fromUri(videoUri));
} else {
    Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
}
```

##### getFileName()
```java
public static String getFileName(Context context, Uri uri) {
    String result = null;
    try {
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            // Query content provider for DISPLAY_NAME
            try (Cursor cursor = context.getContentResolver().query(
                    uri, 
                    new String[]{OpenableColumns.DISPLAY_NAME}, 
                    null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    final int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (columnIndex > -1)
                        result = cursor.getString(columnIndex);
                }
            }
        }
        
        if (result == null) {
            // Fallback: Extract from path
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        
        // Remove extension
        if (result.indexOf(".") > 0)
            result = result.substring(0, result.lastIndexOf("."));
            
    } catch (Exception e) {
        result = uri.getLastPathSegment();
        e.printStackTrace();
    }
    return result;
}
```

**Logic Flow**:
1. If content URI → Query provider for display name
2. If null → Extract from path
3. Strip extension
4. Handle exceptions gracefully

**Example**:
```java
Uri uri1 = Uri.parse("content://media/external/video/123");
String name1 = getFileName(context, uri1);  // "Summer_Vacation_2024"

Uri uri2 = Uri.parse("/storage/emulated/0/Movies/movie.mp4");
String name2 = getFileName(context, uri2);  // "movie"
```

##### isSupportedNetworkUri()
```java
public static boolean isSupportedNetworkUri(final Uri uri) {
    if (uri == null)
        return false;
    final String scheme = uri.getScheme();
    if (scheme == null)
        return false;
    
    return scheme.startsWith("http")  // http:// or https://
        || scheme.equals("rtsp");      // rtsp://
}
```

**Supported Protocols**:
- `http://` - Standard web streaming
- `https://` - Secure web streaming
- `rtsp://` - Real Time Streaming Protocol

**Not Supported** (could be added):
- `rtmp://` - Real Time Messaging Protocol
- `mms://` - Microsoft Media Server
- `mmsh://` - MMS over HTTP

**Enhancement**:
```java
public static boolean isSupportedNetworkUri(final Uri uri) {
    if (uri == null) return false;
    final String scheme = uri.getScheme();
    if (scheme == null) return false;
    
    return scheme.startsWith("http")
        || scheme.equals("rtsp")
        || scheme.equals("rtmp")     // ADD
        || scheme.equals("mms")      // ADD
        || scheme.equals("mmsh");    // ADD
}
```

#### B. UI Utility Functions

##### Screen Dimension Functions
```java
public static int getScreenWidth(Context context, boolean includeNavBar) {
    int width = context.getResources().getDisplayMetrics().widthPixels;
    if (includeNavBar) {
        width += getNavigationBarHeight(context);
    }
    return width;
}

public static int getScreenHeight(Context context, boolean includeNavBar) {
    int height = context.getResources().getDisplayMetrics().heightPixels;
    if (includeNavBar) {
        height += getNavigationBarHeight(context);
    }
    return height;
}
```

**Why Two Parameters?**
- `includeNavBar=false`: Usable screen space (for layout)
- `includeNavBar=true`: Total physical screen (for gesture detection)

**Example Usage**:
```java
// Layout calculation
int usableHeight = getScreenHeight(context, false);
view.setLayoutParams(new LayoutParams(MATCH_PARENT, usableHeight));

// Gesture edge detection
int totalHeight = getScreenHeight(context, true);
if (touchY > totalHeight - 100) {
    // User touched bottom edge (even if nav bar present)
}
```

##### hasNavigationBar()
```java
public static boolean hasNavigationBar(Context context) {
    boolean result = true;
    
    if (VERSION.SDK_INT >= 17) {
        // Modern method (API 17+)
        Display defaultDisplay = getWindowManager(context).getDefaultDisplay();
        Point actualSize = new Point();
        Point availableSize = new Point();
        
        defaultDisplay.getSize(availableSize);      // Visible area
        defaultDisplay.getRealSize(actualSize);     // Total area
        
        // If they match, no navigation bar
        if (actualSize.x == availableSize.x && actualSize.y == availableSize.y) {
            result = false;
        }
    } else {
        // Legacy method (API < 17)
        boolean hasPermanentMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        
        // If device has hardware buttons, no on-screen nav
        if (hasPermanentMenuKey || hasBackKey) {
            result = false;
        }
    }
    
    return result;
}
```

**Logic**:
1. **API 17+**: Compare actual screen size vs available size
2. **API < 17**: Check for hardware menu/back buttons

**Device Examples**:
- **Pixel 8** (no nav bar, gesture navigation) → `false`
- **Galaxy S21** (nav bar enabled) → `true`
- **Old phone with hardware buttons** → `false`

##### getNavigationBarHeight()
```java
public static int getNavigationBarHeight(Context context) {
    if (!hasNavigationBar(context)) {
        return 0;
    }
    
    Resources resources = context.getResources();
    int resourceId = resources.getIdentifier(
        "navigation_bar_height", 
        "dimen", 
        "android"
    );
    
    return resources.getDimensionPixelSize(resourceId);
}
```

**How it works**:
1. First checks if device has navigation bar
2. Retrieves height from system resources
3. Returns 0 if no nav bar

**Typical Values**:
- **48dp** (~132px on xxhdpi) - Standard 3-button nav
- **24dp** (~66px on xxhdpi) - Gesture bar
- **0** - No nav bar

##### dp2px() / sp2px()
```java
public static int dp2px(Context context, float dp) {
    return (int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 
        dp, 
        context.getResources().getDisplayMetrics()
    );
}

public static int sp2px(Context context, float sp) {
    return (int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, 
        sp, 
        context.getResources().getDisplayMetrics()
    );
}
```

**Difference**:
- **dp** (Density-independent Pixels): For dimensions (width, height, margin)
- **sp** (Scale-independent Pixels): For text size (respects user font size setting)

**Example**:
```java
// Set button size to 48dp (standard touch target)
int buttonSize = dp2px(context, 48);
button.setLayoutParams(new LayoutParams(buttonSize, buttonSize));

// Set text size to 14sp
int textSize = sp2px(context, 14);
textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
```

##### toggleSystemUi()
```java
public static void toggleSystemUi(final Activity activity, final View viewSection, final boolean show) {
    if (Build.VERSION.SDK_INT >= 31) {
        // Android 12+ (API 31)
        Window window = activity.getWindow();
        if (window != null) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                if (show) {
                    controller.show(WindowInsets.Type.systemBars());
                } else {
                    controller.hide(WindowInsets.Type.systemBars());
                }
            }
        }
    } else {
        // Legacy method (API < 31)
        if (show) {
            viewSection.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        } else {
            viewSection.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );
        }
    }
}
```

**Flags Explained**:

**Show Mode**:
- `LAYOUT_STABLE`: Keep layout stable during transitions
- `LAYOUT_HIDE_NAVIGATION`: Layout behind nav bar
- `LAYOUT_FULLSCREEN`: Layout behind status bar

**Hide Mode (Immersive)**:
- `LOW_PROFILE`: Dim navigation buttons
- `FULLSCREEN`: Hide status bar
- `IMMERSIVE_STICKY`: Auto-hide system UI, reappear on swipe
- `HIDE_NAVIGATION`: Hide navigation bar

**Usage in Player**:
```java
// Enter fullscreen
SoiadMahediUtils.toggleSystemUi(this, playerView, false);

// Exit fullscreen (when user taps screen)
SoiadMahediUtils.toggleSystemUi(this, playerView, true);
```

##### isEdge()
```java
public static boolean isEdge(Context context, MotionEvent event) {
    int edgeThreshold = dp2px(context, 40.0f);  // 40dp from edge
    float touchX = event.getRawX();
    float touchY = event.getRawY();
    
    int screenWidth = getScreenWidth(context, true);
    int screenHeight = getScreenHeight(context, true);
    
    // Check if touch is within 40dp of any edge
    return touchX < edgeThreshold                          // Left edge
        || touchX > (screenWidth - edgeThreshold)          // Right edge
        || touchY < edgeThreshold                          // Top edge
        || touchY > (screenHeight - edgeThreshold);        // Bottom edge
}
```

**Purpose**: Prevent accidental gesture triggers near screen edges

**Example Usage**:
```java
@Override
public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
        if (SoiadMahediUtils.isEdge(this, event)) {
            // Ignore touch near edge (might be accidental)
            return false;
        }
        
        // Process gesture normally
        handleGesture(event);
    }
    return true;
}
```

#### C. Video Format Functions

##### isRotated()
```java
public static boolean isRotated(final Format format) {
    return format.rotationDegrees == 90 || format.rotationDegrees == 270;
}
```

**Explanation**: Videos shot in portrait mode on phone may have 90° or 270° rotation metadata

##### isPortrait()
```java
public static boolean isPortrait(final Format format) {
    if (isRotated(format)) {
        // If rotated, width/height are swapped
        return format.width > format.height;
    } else {
        if (format.width == format.height) {
            return true;  // Square = portrait for layout purposes
        } else {
            return format.height > format.width;
        }
    }
}
```

**Logic**:
- **No rotation**: height > width = portrait
- **Rotated 90°/270°**: width > height = portrait (dimensions are swapped)
- **Square**: Treated as portrait

**Example**:
```
Video 1: 1920x1080, rotation=0    → isPortrait()=false (landscape)
Video 2: 1080x1920, rotation=0    → isPortrait()=true  (portrait)
Video 3: 1920x1080, rotation=90   → isPortrait()=true  (portrait, rotated)
Video 4: 1080x1080, rotation=0    → isPortrait()=true  (square)
```

##### getRational()
```java
public static Rational getRational(final Format format) {
    if (isRotated(format))
        return new Rational(format.height, format.width);
    else
        return new Rational(format.width, format.height);
}
```

**Purpose**: Returns correct aspect ratio accounting for rotation

**Example**:
```java
Format format = player.getVideoFormat();
Rational aspectRatio = SoiadMahediUtils.getRational(format);

// Set video surface aspect ratio
videoView.setAspectRatio(aspectRatio);
```

#### D. Audio Functions

##### isVolumeMin()
```java
public static boolean isVolumeMin(final AudioManager audioManager) {
    int min = Build.VERSION.SDK_INT >= 28 
        ? audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC) 
        : 0;
    
    return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == min;
}
```

**Why needed?**: 
- API 28+ supports non-zero minimum volume
- Prevents going below system minimum

##### getVolume() [Samsung-specific]
```java
private static int getVolume(final Context context, final boolean max, final AudioManager audioManager) {
    if (Build.VERSION.SDK_INT >= 30 && Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
        try {
            // Samsung "Fine Volume" feature (150 steps instead of 15)
            Class<?> clazz = Class.forName("com.samsung.android.media.SemSoundAssistantManager");
            Constructor<?> constructor = clazz.getConstructor(Context.class);
            
            Method getMediaVolumeInterval = clazz.getDeclaredMethod("getMediaVolumeInterval");
            Object result = getMediaVolumeInterval.invoke(constructor.newInstance(context));
            
            if (result instanceof Integer) {
                int mediaVolumeInterval = (int) result;
                
                if (mediaVolumeInterval < 10) {
                    // Fine volume is enabled
                    Method semGetFineVolume = AudioManager.class.getDeclaredMethod("semGetFineVolume", int.class);
                    Object fineVolumeResult = semGetFineVolume.invoke(audioManager, AudioManager.STREAM_MUSIC);
                    
                    if (fineVolumeResult instanceof Integer) {
                        int fineVolume = (int) fineVolumeResult;
                        
                        if (max) {
                            return 150 / mediaVolumeInterval;
                        } else {
                            return fineVolume / mediaVolumeInterval;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fall through to standard method
        }
    }
    
    // Standard Android volume
    if (max) {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    } else {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }
}
```

**Samsung Fine Volume**:
- Standard Android: 15 volume steps
- Samsung Fine Volume: 150 steps (10x precision)
- This function handles both

#### E. Format Utility Functions

##### convertMicrosecondsToDuration()
```java
public static String convertMicrosecondsToDuration(long microseconds) {
    StringBuilder result = new StringBuilder();
    
    // Handle negative
    if (microseconds < 0) {
        microseconds = -microseconds;
        result.append("-");
    }
    
    // Calculate components
    int hours = (int) (microseconds / (1000 * 60 * 60));
    int minutes = (int) ((microseconds % (1000 * 60 * 60)) / (1000 * 60));
    int seconds = (int) ((microseconds % (1000 * 60)) / 1000);
    int milliseconds = (int) (microseconds % 1000);
    
    // Format output
    if (hours > 0) {
        result.append(hours).append(":");
    }
    
    result.append(String.format("%02d", minutes)).append(":");
    result.append(String.format("%02d", seconds));
    
    if (milliseconds > 0) {
        result.append(",").append(milliseconds);
    }
    
    return result.toString().trim();
}
```

**Format Examples**:
```
30,000,000 µs  → "00:30"
90,000,000 µs  → "01:30"
3,661,000,000 µs → "1:01:01"
5,500,000 µs  → "00:05,500"
```

**Note**: Uses comma (not period) for milliseconds, following SRT subtitle convention

##### formatBitrate()
```java
public static String formatBitrate(int bitrate) {
    String[] suffixes = {"bps", "Kbps", "Mbps", "Gbps"};
    double bitrate_bps = bitrate;
    
    int i = 0;
    while (bitrate_bps >= 1024 && i < suffixes.length - 1) {
        bitrate_bps /= 1024;
        i++;
    }
    
    return String.format("%.1f %s", bitrate_bps, suffixes[i]);
}
```

**Examples**:
```
128000 bps      → "125.0 Kbps"
5000000 bps     → "4.8 Mbps"
25000000 bps    → "23.8 Mbps"
1000000000 bps  → "953.7 Mbps"
```

##### isSubtitleType()
```java
public static boolean isSubtitleType(String mimeType) {
    if (mimeType != null) {
        // Check standard subtitle MIME types
        for (String mime : supportedMimeTypesSubtitle) {
            if (mimeType.equals(mime)) {
                return true;
            }
        }
        
        // Check additional subtitle MIME types
        if (mimeType.equals("text/plain") 
            || mimeType.equals("text/3gpp-tt")
            || mimeType.equals("text/x-ssa")
            || mimeType.equals("application/octet-stream")
            || mimeType.equals("application/ass")
            || mimeType.equals("application/ssa")
            || mimeType.equals("application/vtt")) {
            return true;
        }
    }
    return false;
}
```

**Supported MIME Types**:
- `application/x-subrip` (SRT)
- `text/vtt` (WebVTT)
- `text/x-ssa` (SSA)
- `application/ttml+xml` (TTML)
- `text/plain` (Generic text)
- `application/octet-stream` (Binary, needs detection)

#### F. Localization Functions

##### getDeviceLanguages()
```java
public static String[] getDeviceLanguages() {
    final List<String> locales = new ArrayList<>();
    
    if (Build.VERSION.SDK_INT >= 24) {
        // Modern method (API 24+): Get all preferred languages
        final LocaleList localeList = Resources.getSystem().getConfiguration().getLocales();
        for (int i = 0; i < localeList.size(); i++) {
            locales.add(localeList.get(i).getISO3Language());
        }
    } else {
        // Legacy method: Single language only
        final Locale locale = Resources.getSystem().getConfiguration().locale;
        locales.add(locale.getISO3Language());
    }
    
    return locales.toArray(new String[0]);
}
```

**Purpose**: Returns user's preferred languages for subtitle/audio track selection

**Example Output**:
```java
// User prefers English, then Spanish, then French
String[] languages = getDeviceLanguages();
// Result: ["eng", "spa", "fra"]
```

**Usage**:
```java
String[] preferredLanguages = SoiadMahediUtils.getDeviceLanguages();

// Auto-select best subtitle track
for (Track subtitleTrack : availableSubtitles) {
    for (String lang : preferredLanguages) {
        if (subtitleTrack.language.equals(lang)) {
            player.selectTrack(subtitleTrack);
            return;
        }
    }
}
```

##### getLanguageFullName()
```java
public static String getLanguageFullName(String languageCode) {
    Locale locale = new Locale(languageCode);
    return locale.getDisplayLanguage(Locale.ENGLISH);
}
```

**Examples**:
```
"en"  → "English"
"es"  → "Spanish"
"fr"  → "French"
"bn"  → "Bengali"
"hi"  → "Hindi"
```

##### normalizeFontScale()
```java
public static float normalizeFontScale(float fontScale, boolean small) {
    float newScale;
    
    if (fontScale > 1.01f) {
        if (fontScale >= 1.99f) {
            // User has "Largest" text (2.0x)
            newScale = small ? 1.15f : 1.2f;
        } else {
            // User has "Large" text (1.5x)
            newScale = small ? 1.0f : 1.1f;
        }
    } else if (fontScale < 0.99f) {
        if (fontScale <= 0.26f) {
            // User has "Smallest" text (0.25x)
            newScale = small ? 0.65f : 0.8f;
        } else {
            // User has "Small" text (0.5x)
            newScale = small ? 0.75f : 0.9f;
        }
    } else {
        // User has "Normal" text (1.0x)
        newScale = small ? 0.85f : 1.0f;
    }
    
    return newScale;
}
```

**Purpose**: Adjusts subtitle size relative to user's system font size

**Logic**:
- Prevents subtitles from being too large (covering screen)
- Prevents subtitles from being too small (unreadable)
- Follows BBC subtitle guidelines

**Parameters**:
- `fontScale`: User's system font scale (0.25 to 2.0)
- `small`: true = smaller text (descriptions), false = larger text (dialogue)

**Example**:
```java
float systemScale = context.getResources().getConfiguration().fontScale;
float subtitleScale = SoiadMahediUtils.normalizeFontScale(systemScale, false);

subtitleView.setTextSize(
    TypedValue.COMPLEX_UNIT_SP, 
    16 * subtitleScale  // Base size 16sp
);
```

---

### 3. SoiadMahediLogger.java Analysis

**Purpose**: Broadcast-based logging system for external log viewers

#### Expected Implementation

```java
public class SoiadMahediLogger {
    private static final String LOG_ACTION = "com.soiadmahedi.suicTh.LOG";
    private static boolean isLogging = false;
    
    public static void startLogging() {
        if (!isLogging) {
            // Install custom log printer
            Log.setLogger(new LogBroadcaster());
            isLogging = true;
        }
    }
    
    public static void broadcastLog(String message) {
        Intent intent = new Intent(LOG_ACTION);
        intent.putExtra("message", message);
        intent.putExtra("timestamp", System.currentTimeMillis());
        SoiadMahediApplication.getContext().sendBroadcast(intent);
    }
    
    private static class LogBroadcaster implements Log.TeePrinter {
        @Override
        public void println(int priority, String tag, String msg) {
            // Broadcast to external log viewers
            broadcastLog(String.format("[%s] %s: %s", 
                getPriorityString(priority), tag, msg));
            
            // Also print to logcat
            android.util.Log.println(priority, tag, msg);
        }
        
        private String getPriorityString(int priority) {
            switch (priority) {
                case Log.VERBOSE: return "V";
                case Log.DEBUG: return "D";
                case Log.INFO: return "I";
                case Log.WARN: return "W";
                case Log.ERROR: return "E";
                case Log.ASSERT: return "A";
                default: return "?";
            }
        }
    }
}
```

**Usage**:
1. App broadcasts all logs
2. External log viewer receives broadcasts
3. Useful for:
   - Remote debugging
   - Real-time log monitoring
   - Log analysis tools

**External Log Viewer Example**:
```java
public class LogViewerActivity extends AppCompatActivity {
    private BroadcastReceiver logReceiver;
    
    @Override
    protected void onResume() {
        super.onResume();
        
        logReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra("message");
                long timestamp = intent.getLongExtra("timestamp", 0);
                
                // Display in UI
                logTextView.append(message + "\n");
            }
        };
        
        IntentFilter filter = new IntentFilter("com.soiadmahedi.suicTh.LOG");
        registerReceiver(logReceiver, filter);
    }
}
```

---

## Detailed Implementation Patterns

### Pattern 1: Activity Lifecycle with ExoPlayer

```java
public class PlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        
        playerView = findViewById(R.id.player_view);
    }
    
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
    
    private void initializePlayer() {
        if (player == null) {
            player = new ExoPlayer.Builder(this)
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .build();
            playerView.setPlayer(player);
            
            // Load media
            Uri uri = getIntent().getData();
            MediaItem mediaItem = MediaItem.fromUri(uri);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        }
    }
    
    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
```

**Why Different for API 24?**
- API 24+ (Android 7.0): Multi-window support
- Need to maintain player in onStop() for multi-window
- Release earlier on older versions to save memory

---

## Summary

The codebase demonstrates:

1. **Robust Error Handling**: Custom exception handler with automatic restart
2. **Comprehensive Utilities**: 25+ helper functions covering all aspects
3. **Platform Compatibility**: Handles API differences gracefully
4. **Samsung Optimization**: Special handling for Samsung-specific features
5. **Logging Infrastructure**: Broadcast-based logging for external tools
6. **Best Practices**: Proper resource management, null safety, exception handling

The utility library is the foundation that makes complex features (gestures, subtitles, multi-format support) possible with minimal code duplication.
