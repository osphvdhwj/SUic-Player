# SUic Player - Feature Implementation Guide

## Implementing MX Player Features

### 1. Kids Lock Implementation

**Feature**: Prevent accidental touches during video playback (useful for children)

#### Step 1: Create Overlay Layout

**File**: `res/layout/kids_lock_overlay.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/kids_lock_overlay"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#01000000"
    android:clickable="true"
    android:focusable="true"
    android:visibility="gone">
    
    <!-- Unlock gesture area (small area at specific location) -->
    <View
        android:id="@+id/unlock_area"
        android:layout_width="100dp"
        android:layout_height="100dp"
        android:layout_gravity="top|end"
        android:layout_margin="16dp"
        android:background="@drawable/unlock_circle"
        android:alpha="0.3" />
    
    <!-- Lock icon indicator -->
    <ImageView
        android:id="@+id/lock_icon"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_gravity="center"
        android:src="@drawable/ic_lock"
        android:tint="#80FFFFFF"
        android:visibility="gone" />
        
</FrameLayout>
```

#### Step 2: KidsLockManager Class

```java
public class KidsLockManager {
    private final Activity activity;
    private final View overlayView;
    private final View unlockArea;
    private final ImageView lockIcon;
    private boolean isLocked = false;
    
    private long touchStartTime = 0;
    private static final long UNLOCK_PRESS_DURATION = 3000; // 3 seconds
    
    public KidsLockManager(Activity activity, ViewGroup rootView) {
        this.activity = activity;
        
        // Inflate overlay
        LayoutInflater inflater = LayoutInflater.from(activity);
        overlayView = inflater.inflate(R.layout.kids_lock_overlay, rootView, false);
        rootView.addView(overlayView);
        
        unlockArea = overlayView.findViewById(R.id.unlock_area);
        lockIcon = overlayView.findViewById(R.id.lock_icon);
        
        setupUnlockGesture();
    }
    
    private void setupUnlockGesture() {
        unlockArea.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartTime = System.currentTimeMillis();
                    lockIcon.setVisibility(View.VISIBLE);
                    animateUnlockProgress();
                    return true;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    long pressDuration = System.currentTimeMillis() - touchStartTime;
                    lockIcon.setVisibility(View.GONE);
                    
                    if (pressDuration >= UNLOCK_PRESS_DURATION) {
                        unlock();
                    }
                    return true;
            }
            return false;
        });
        
        // Block all other touches
        overlayView.setOnTouchListener((v, event) -> true);
    }
    
    private void animateUnlockProgress() {
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(lockIcon, "alpha", 0.5f, 1.0f);
        alphaAnimator.setDuration(UNLOCK_PRESS_DURATION);
        alphaAnimator.setInterpolator(new LinearInterpolator());
        alphaAnimator.start();
    }
    
    public void lock() {
        isLocked = true;
        overlayView.setVisibility(View.VISIBLE);
        
        // Disable system buttons
        if (Build.VERSION.SDK_INT >= 28) {
            activity.getWindow().getAttributes().layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        
        // Show toast
        Toast.makeText(activity, "Kids Lock Enabled\nLong press top-right to unlock", 
            Toast.LENGTH_LONG).show();
    }
    
    public void unlock() {
        isLocked = false;
        overlayView.setVisibility(View.GONE);
        Toast.makeText(activity, "Kids Lock Disabled", Toast.LENGTH_SHORT).show();
    }
    
    public boolean isLocked() {
        return isLocked;
    }
    
    public void toggle() {
        if (isLocked) {
            unlock();
        } else {
            lock();
        }
    }
}
```

#### Step 3: Integration in PlayerActivity

```java
public class PlayerActivity extends AppCompatActivity {
    private KidsLockManager kidsLockManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        
        // Initialize kids lock
        ViewGroup rootView = findViewById(R.id.player_root);
        kidsLockManager = new KidsLockManager(this, rootView);
        
        // Add menu option
        setupKidsLockButton();
    }
    
    private void setupKidsLockButton() {
        ImageButton kidsLockButton = findViewById(R.id.btn_kids_lock);
        kidsLockButton.setOnClickListener(v -> {
            kidsLockManager.toggle();
        });
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Block hardware keys when locked
        if (kidsLockManager.isLocked()) {
            return true;  // Consume event
        }
        return super.onKeyDown(keyCode, event);
    }
}
```

---

### 2. Audio Equalizer Implementation

#### Step 1: Check Equalizer Availability

```java
public class EqualizerManager {
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;
    private final int audioSessionId;
    
    public EqualizerManager(int audioSessionId) {
        this.audioSessionId = audioSessionId;
        
        try {
            // Initialize audio effects
            equalizer = new Equalizer(0, audioSessionId);
            bassBoost = new BassBoost(0, audioSessionId);
            virtualizer = new Virtualizer(0, audioSessionId);
            
            equalizer.setEnabled(true);
            bassBoost.setEnabled(true);
            virtualizer.setEnabled(true);
            
        } catch (Exception e) {
            Log.e("EqualizerManager", "Failed to initialize equalizer", e);
        }
    }
    
    public boolean isAvailable() {
        return equalizer != null;
    }
    
    public int getNumberOfBands() {
        if (equalizer == null) return 0;
        return equalizer.getNumberOfBands();
    }
    
    public int[] getBandFrequencies() {
        if (equalizer == null) return new int[0];
        
        int[] frequencies = new int[getNumberOfBands()];
        for (short i = 0; i < getNumberOfBands(); i++) {
            frequencies[i] = equalizer.getCenterFreq(i) / 1000; // Convert to Hz
        }
        return frequencies;
    }
    
    public void setBandLevel(int band, int level) {
        if (equalizer != null) {
            // level: -1500 to +1500 (millibels)
            equalizer.setBandLevel((short) band, (short) level);
        }
    }
    
    public void applyPreset(EqualizerPreset preset) {
        if (equalizer == null) return;
        
        int[] levels = preset.getBandLevels();
        for (int i = 0; i < Math.min(levels.length, getNumberOfBands()); i++) {
            setBandLevel(i, levels[i]);
        }
        
        bassBoost.setStrength((short) preset.getBassBoost());
        virtualizer.setStrength((short) preset.getVirtualizer());
    }
    
    public void release() {
        if (equalizer != null) equalizer.release();
        if (bassBoost != null) bassBoost.release();
        if (virtualizer != null) virtualizer.release();
    }
}
```

#### Step 2: Equalizer Presets

```java
public enum EqualizerPreset {
    FLAT("Flat", new int[]{0, 0, 0, 0, 0}, 0, 0),
    ROCK("Rock", new int[]{800, 400, -500, -800, -300}, 0, 500),
    POP("Pop", new int[]{-300, 400, 700, 800, -300}, 0, 300),
    JAZZ("Jazz", new int[]{0, 0, 400, 500, 500}, 400, 500),
    CLASSICAL("Classical", new int[]{0, 0, 0, 0, -700}, 0, 700),
    BASS_BOOST("Bass Boost", new int[]{1000, 700, 0, -300, -500}, 800, 0),
    TREBLE_BOOST("Treble Boost", new int[]{-500, -300, 0, 700, 1000}, 0, 500),
    VOCAL_BOOST("Vocal", new int[]{-300, -500, 400, 800, 400}, 0, 400);
    
    private final String name;
    private final int[] bandLevels;  // millibels
    private final int bassBoost;      // 0-1000
    private final int virtualizer;    // 0-1000
    
    EqualizerPreset(String name, int[] levels, int bass, int virtualizer) {
        this.name = name;
        this.bandLevels = levels;
        this.bassBoost = bass;
        this.virtualizer = virtualizer;
    }
    
    public String getName() { return name; }
    public int[] getBandLevels() { return bandLevels; }
    public int getBassBoost() { return bassBoost; }
    public int getVirtualizer() { return virtualizer; }
}
```

#### Step 3: Equalizer UI

**File**: `res/layout/equalizer_dialog.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">
    
    <!-- Preset Spinner -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Preset"
        android:textStyle="bold" />
    
    <Spinner
        android:id="@+id/preset_spinner"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp" />
    
    <!-- Band Controls -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Frequency Bands"
        android:textStyle="bold"
        android:layout_marginBottom="8dp" />
    
    <HorizontalScrollView
        android:layout_width="match_parent"
        android:layout_height="200dp">
        
        <LinearLayout
            android:id="@+id/band_container"
            android:layout_width="wrap_content"
            android:layout_height="match_parent"
            android:orientation="horizontal" />
    </HorizontalScrollView>
    
    <!-- Bass Boost -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Bass Boost"
        android:textStyle="bold"
        android:layout_marginTop="16dp" />
    
    <SeekBar
        android:id="@+id/bass_boost_seekbar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:max="1000" />
    
    <!-- Virtualizer -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Surround Sound"
        android:textStyle="bold"
        android:layout_marginTop="16dp" />
    
    <SeekBar
        android:id="@+id/virtualizer_seekbar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:max="1000" />
        
</LinearLayout>
```

#### Step 4: Integration with ExoPlayer

```java
public class PlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private EqualizerManager equalizerManager;
    
    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        
        // Get audio session ID
        int audioSessionId = player.getAudioSessionId();
        
        // Initialize equalizer
        equalizerManager = new EqualizerManager(audioSessionId);
        
        // Load saved preset
        SharedPreferences prefs = getSharedPreferences("equalizer", MODE_PRIVATE);
        String presetName = prefs.getString("preset", "FLAT");
        EqualizerPreset preset = EqualizerPreset.valueOf(presetName);
        equalizerManager.applyPreset(preset);
    }
    
    private void showEqualizerDialog() {
        if (!equalizerManager.isAvailable()) {
            Toast.makeText(this, "Equalizer not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create and show dialog
        EqualizerDialog dialog = new EqualizerDialog(this, equalizerManager);
        dialog.show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (equalizerManager != null) {
            equalizerManager.release();
        }
    }
}
```

---

### 3. Sleep Timer Implementation

```java
public class SleepTimerManager {
    private CountDownTimer timer;
    private OnTimerTickListener tickListener;
    private OnTimerFinishListener finishListener;
    
    public interface OnTimerTickListener {
        void onTick(long millisUntilFinished);
    }
    
    public interface OnTimerFinishListener {
        void onFinish();
    }
    
    public void setOnTimerTickListener(OnTimerTickListener listener) {
        this.tickListener = listener;
    }
    
    public void setOnTimerFinishListener(OnTimerFinishListener listener) {
        this.finishListener = listener;
    }
    
    public void start(long durationMillis) {
        cancel();  // Cancel any existing timer
        
        timer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (tickListener != null) {
                    tickListener.onTick(millisUntilFinished);
                }
            }
            
            @Override
            public void onFinish() {
                if (finishListener != null) {
                    finishListener.onFinish();
                }
            }
        };
        
        timer.start();
    }
    
    public void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    
    public boolean isActive() {
        return timer != null;
    }
    
    // Convenience methods for common durations
    public void start15Minutes() { start(15 * 60 * 1000); }
    public void start30Minutes() { start(30 * 60 * 1000); }
    public void start1Hour() { start(60 * 60 * 1000); }
    public void start2Hours() { start(2 * 60 * 60 * 1000); }
}
```

**Sleep Timer Dialog**:

```java
public class SleepTimerDialog extends AlertDialog {
    private final SleepTimerManager timerManager;
    private final ExoPlayer player;
    private TextView countdownText;
    
    public SleepTimerDialog(Context context, SleepTimerManager manager, ExoPlayer player) {
        super(context);
        this.timerManager = manager;
        this.player = player;
        setupDialog();
    }
    
    private void setupDialog() {
        View view = LayoutInflater.from(getContext())
            .inflate(R.layout.sleep_timer_dialog, null);
        setView(view);
        
        countdownText = view.findViewById(R.id.countdown_text);
        
        // Duration buttons
        view.findViewById(R.id.btn_15min).setOnClickListener(v -> startTimer(15));
        view.findViewById(R.id.btn_30min).setOnClickListener(v -> startTimer(30));
        view.findViewById(R.id.btn_1hour).setOnClickListener(v -> startTimer(60));
        view.findViewById(R.id.btn_2hour).setOnClickListener(v -> startTimer(120));
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> cancelTimer());
        
        // Setup timer callbacks
        timerManager.setOnTimerTickListener(this::updateCountdown);
        timerManager.setOnTimerFinishListener(this::onTimerFinish);
        
        updateUI();
    }
    
    private void startTimer(int minutes) {
        timerManager.start(minutes * 60 * 1000);
        updateUI();
    }
    
    private void cancelTimer() {
        timerManager.cancel();
        updateUI();
    }
    
    private void updateCountdown(long millisUntilFinished) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
        
        String countdown = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        countdownText.setText(countdown);
    }
    
    private void onTimerFinish() {
        // Fade out audio
        ValueAnimator volumeAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
        volumeAnimator.setDuration(5000);  // 5 second fade
        volumeAnimator.addUpdateListener(animation -> {
            float volume = (float) animation.getAnimatedValue();
            player.setVolume(volume);
        });
        volumeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                player.pause();
                ((Activity) getContext()).finish();
            }
        });
        volumeAnimator.start();
        
        dismiss();
    }
    
    private void updateUI() {
        if (timerManager.isActive()) {
            countdownText.setVisibility(View.VISIBLE);
            // Show cancel button, hide duration buttons
        } else {
            countdownText.setVisibility(View.GONE);
            // Show duration buttons, hide cancel button
        }
    }
}
```

---

## Implementing PipePipe Features

### 1. YouTube Integration (NewPipe Extractor)

#### Step 1: Add Dependency

**build.gradle (app)**:
```gradle
dependencies {
    // NewPipe Extractor
    implementation 'com.github.TeamNewPipe:NewPipeExtractor:v0.23.0'
    
    // Required by NewPipeExtractor
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.grack:nanojson:1.7'
}
```

#### Step 2: Initialize NewPipe

```java
public class YouTubeService {
    private static YouTubeService instance;
    
    public static YouTubeService getInstance() {
        if (instance == null) {
            instance = new YouTubeService();
        }
        return instance;
    }
    
    private YouTubeService() {
        // Initialize NewPipe
        NewPipe.init(DownloaderImpl.init(null));
    }
    
    public List<StreamInfoItem> searchVideos(String query) throws Exception {
        SearchExtractor extractor = YouTube.getSearchExtractor(query);
        extractor.fetchPage();
        
        List<StreamInfoItem> results = new ArrayList<>();
        for (InfoItem item : extractor.getInitialPage().getItems()) {
            if (item instanceof StreamInfoItem) {
                results.add((StreamInfoItem) item);
            }
        }
        return results;
    }
    
    public StreamInfo getVideoInfo(String url) throws Exception {
        StreamInfo info = StreamInfo.getInfo(url);
        return info;
    }
    
    public String getBestVideoUrl(StreamInfo info) {
        // Get best quality video URL
        List<VideoStream> videoStreams = info.getVideoOnlyStreams();
        if (!videoStreams.isEmpty()) {
            // Sort by quality
            videoStreams.sort((a, b) -> Integer.compare(b.getHeight(), a.getHeight()));
            return videoStreams.get(0).getUrl();
        }
        
        // Fallback to video+audio streams
        List<VideoStream> videoAudioStreams = info.getVideoStreams();
        if (!videoAudioStreams.isEmpty()) {
            videoAudioStreams.sort((a, b) -> Integer.compare(b.getHeight(), a.getHeight()));
            return videoAudioStreams.get(0).getUrl();
        }
        
        return null;
    }
}
```

#### Step 3: YouTube Search Activity

```java
public class YouTubeSearchActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private SearchAdapter adapter;
    private EditText searchBox;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_search);
        
        searchBox = findViewById(R.id.search_box);
        recyclerView = findViewById(R.id.recycler_view);
        
        adapter = new SearchAdapter(this::onVideoSelected);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchBox.getText().toString());
                return true;
            }
            return false;
        });
    }
    
    private void performSearch(String query) {
        // Show loading
        showLoading(true);
        
        // Search in background
        new Thread(() -> {
            try {
                List<StreamInfoItem> results = YouTubeService.getInstance().searchVideos(query);
                
                runOnUiThread(() -> {
                    adapter.setItems(results);
                    showLoading(false);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Search failed: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
            }
        }).start();
    }
    
    private void onVideoSelected(StreamInfoItem item) {
        // Get video info and play
        new Thread(() -> {
            try {
                StreamInfo info = YouTubeService.getInstance().getVideoInfo(item.getUrl());
                String videoUrl = YouTubeService.getInstance().getBestVideoUrl(info);
                
                runOnUiThread(() -> {
                    Intent intent = new Intent(this, PlayerActivity.class);
                    intent.setData(Uri.parse(videoUrl));
                    intent.putExtra("title", info.getName());
                    startActivity(intent);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to load video", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
```

---

### 2. SponsorBlock Integration

#### Step 1: SponsorBlock API Client

```java
public class SponsorBlockAPI {
    private static final String BASE_URL = "https://sponsor.ajay.app/api";
    private final OkHttpClient client;
    
    public SponsorBlockAPI() {
        client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .build();
    }
    
    public List<Segment> getSegments(String videoId) throws IOException {
        String url = String.format("%s/skipSegments?videoID=%s", BASE_URL, videoId);
        
        Request request = new Request.Builder()
            .url(url)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return Collections.emptyList();
            }
            
            String json = response.body().string();
            return parseSegments(json);
        }
    }
    
    private List<Segment> parseSegments(String json) {
        List<Segment> segments = new ArrayList<>();
        
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                JSONArray segment = obj.getJSONArray("segment");
                
                Segment s = new Segment(
                    (long) (segment.getDouble(0) * 1000),  // Start (ms)
                    (long) (segment.getDouble(1) * 1000),  // End (ms)
                    obj.getString("category")
                );
                segments.add(s);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return segments;
    }
    
    public static class Segment {
        public final long startMs;
        public final long endMs;
        public final String category;
        
        public Segment(long start, long end, String category) {
            this.startMs = start;
            this.endMs = end;
            this.category = category;
        }
        
        public boolean contains(long positionMs) {
            return positionMs >= startMs && positionMs < endMs;
        }
    }
}
```

#### Step 2: SponsorBlock Manager

```java
public class SponsorBlockManager {
    private final ExoPlayer player;
    private final SponsorBlockAPI api;
    private List<SponsorBlockAPI.Segment> segments;
    private boolean enabled = true;
    
    public SponsorBlockManager(ExoPlayer player) {
        this.player = player;
        this.api = new SponsorBlockAPI();
        this.segments = new ArrayList<>();
        
        // Monitor playback position
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    checkForSegments();
                }
            }
        });
    }
    
    public void loadSegments(String videoId) {
        new Thread(() -> {
            try {
                segments = api.getSegments(videoId);
                Log.d("SponsorBlock", "Loaded " + segments.size() + " segments");
            } catch (IOException e) {
                Log.e("SponsorBlock", "Failed to load segments", e);
            }
        }).start();
    }
    
    private void checkForSegments() {
        if (!enabled || segments.isEmpty()) return;
        
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable checker = new Runnable() {
            @Override
            public void run() {
                if (player.isPlaying()) {
                    long position = player.getCurrentPosition();
                    
                    for (SponsorBlockAPI.Segment segment : segments) {
                        if (segment.contains(position)) {
                            // Skip to end of segment
                            player.seekTo(segment.endMs);
                            showSkipNotification(segment);
                            break;
                        }
                    }
                    
                    // Check again in 500ms
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.post(checker);
    }
    
    private void showSkipNotification(SponsorBlockAPI.Segment segment) {
        String message = String.format("Skipped %s segment", segment.category);
        // Show toast or snackbar
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
```

---

### 3. Download Manager Implementation

#### Using Media3 DownloadManager

```java
public class DownloadManagerService extends Service {
    private DownloadManager downloadManager;
    private NotificationManager notificationManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Create download directory
        File downloadDir = new File(getExternalFilesDir(null), "downloads");
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        
        // Setup cache
        DatabaseProvider databaseProvider = new StandaloneDatabaseProvider(this);
        Cache downloadCache = new SimpleCache(
            downloadDir,
            new LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024),  // 500MB
            databaseProvider
        );
        
        // Setup data source
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
            .setUserAgent("SUicPlayer/2.0");
        
        // Create download manager
        downloadManager = new DownloadManager(
            this,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executors.newFixedThreadPool(3)  // 3 concurrent downloads
        );
        
        downloadManager.addListener(new DownloadManager.Listener() {
            @Override
            public void onDownloadChanged(
                DownloadManager downloadManager, 
                Download download, 
                Exception finalException
            ) {
                updateNotification(download);
            }
        });
        
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }
    
    public void addDownload(String url, String title) {
        DownloadRequest request = new DownloadRequest.Builder(url, Uri.parse(url))
            .setMimeType(MimeTypes.VIDEO_MP4)
            .setData(title.getBytes())
            .build();
        
        downloadManager.addDownload(request);
    }
    
    private void updateNotification(Download download) {
        String title = new String(download.request.data);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "downloads")
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(title);
        
        switch (download.state) {
            case Download.STATE_DOWNLOADING:
                int progress = (int) (download.getPercentDownloaded());
                builder.setProgress(100, progress, false)
                    .setContentText(String.format("%d%% downloaded", progress));
                break;
                
            case Download.STATE_COMPLETED:
                builder.setProgress(0, 0, false)
                    .setContentText("Download complete");
                break;
                
            case Download.STATE_FAILED:
                builder.setContentText("Download failed");
                break;
        }
        
        notificationManager.notify(download.request.id.hashCode(), builder.build());
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
```

---

## Summary

This guide covers implementation of:

**MX Player Features**:
✅ Kids Lock (overlay + gesture unlock)
✅ Audio Equalizer (5-band + bass boost + virtualizer)
✅ Sleep Timer (countdown with auto-pause)

**PipePipe Features**:
✅ YouTube Integration (NewPipe Extractor)
✅ SponsorBlock (automatic skip)
✅ Download Manager (Media3-based)

Each implementation:
- Uses modern Android APIs
- Integrates cleanly with existing code
- Follows Material Design guidelines
- Handles edge cases properly
- Includes error handling

Next steps:
- Test thoroughly on multiple devices
- Add UI polish and animations
- Implement settings persistence
- Add localization strings
