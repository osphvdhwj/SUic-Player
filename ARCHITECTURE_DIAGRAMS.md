# SUic Player - Architecture & Flow Diagrams

## System Architecture Overview

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                       PRESENTATION LAYER                                  ┃
┃                                                                           ┃
┃  ┏────────────────┐   ┏────────────────┐   ┏────────────────┐  ┃
┃  │  MainActivity   │   │ PlayerActivity │   │  DebugActivity  │  ┃
┃  │ (File Browser) │   │  (Playback UI)  │   │ (Error Screen) │  ┃
┃  └────────┬───────┘   └────────┬───────┘   └────────┬───────┘  ┃
┃           │                  │                  │           ┃
┗━━━━━━━━━━━┴━━━━━━━━━━━━━━━━┴━━━━━━━━━━━━━━━━━━┴━━━━━━━━━━━┛
            │                  │                  │
┏━━━━━━━━━━━┴━━━━━━━━━━━━━━━━┴━━━━━━━━━━━━━━━━━━┴━━━━━━━━━━━┓
┃                        BUSINESS LOGIC LAYER                               ┃
┃                                                                           ┃
┃  ┏────────────────────────────────────────────────────────┐  ┃
┃  │              Media3 ExoPlayer Framework                      │  ┃
┃  │                                                              │  ┃
┃  │  ┌───────────────┐   ┌───────────────┐           │  ┃
┃  │  │  ExoPlayer     │   │  MediaSession  │           │  ┃
┃  │  │  (Playback)    │   │  (Background)  │           │  ┃
┃  │  └───────┬───────┘   └───────┬───────┘           │  ┃
┃  │          │                  │                       │  ┃
┃  └──────────┴──────────────────┴───────────────────────┘  ┃
┃             │                  │                              ┃
┗━━━━━━━━━━━━━┴━━━━━━━━━━━━━━━━━━┴━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
              │                  │
┏━━━━━━━━━━━━━┴━━━━━━━━━━━━━━━━━━┴━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                         DATA ACCESS LAYER                                 ┃
┃                                                                           ┃
┃  ┌───────────────────┐   ┌───────────────────┐           ┃
┃  │  MediaStore API   │   │  Network Streams  │           ┃
┃  │  (Local Files)    │   │  (HTTP/RTSP)      │           ┃
┃  └─────────┬─────────┘   └─────────┬─────────┘           ┃
┃            │                    │                       ┃
┗━━━━━━━━━━━━┴━━━━━━━━━━━━━━━━━━━━┴━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
             │                    │
             └────────┬─────────┘
                      │
         ┏━━━━━━━━━━━┴━━━━━━━━━━┓
         ┃   UTILITY LAYER     ┃
         ┃                     ┃
         ┃ SoiadMahediUtils   ┃
         ┃ SoiadMahediLogger  ┃
         ┗━━━━━━━━━━━━━━━━━━━━━━━┛
```

---

## Detailed Component Interaction

### Video Playback Sequence

```
User Selects Video
      │
      ↓
┌───────────────────────────────────────┐
│       MainActivity.onItemClick()       │
│   - Get URI from selected item         │
│   - Create Intent with URI             │
│   - Start PlayerActivity               │
└─────────────────┬──────────────────────┘
                 │
                 ↓
┌───────────────────────────────────────┐
│      PlayerActivity.onCreate()         │
│   - Set content view                   │
│   - Get video URI from intent          │
│   - Setup UI components                │
└─────────────────┬──────────────────────┘
                 │
                 ↓
┌───────────────────────────────────────┐
│      PlayerActivity.onStart()          │
│   - Call initializePlayer()            │
└─────────────────┬──────────────────────┘
                 │
                 ↓
┌───────────────────────────────────────┐
│   PlayerActivity.initializePlayer()   │
│                                       │
│   1. Create ExoPlayer.Builder         │
│      - Set audio attributes           │
│      - Configure track selector       │
│      - Build player instance          │
│                                       │
│   2. Attach player to PlayerView      │
│      playerView.setPlayer(player)     │
│                                       │
│   3. Create MediaItem from URI        │
│      MediaItem.fromUri(videoUri)      │
│                                       │
│   4. Set media item                   │
│      player.setMediaItem(mediaItem)   │
│                                       │
│   5. Prepare player                   │
│      player.prepare()                 │
│                                       │
│   6. Start playback                   │
│      player.play()                    │
└─────────────────┬──────────────────────┘
                 │
                 ↓
      ┌──────────────────┐
      │  ExoPlayer      │
      │  Processing     │
      └────────┬────────┘
               │
       ┌───────┴───────┐
       │               │
       ↓               ↓
┌──────────┐  ┌──────────┐
│  Media   │  │  Track   │
│  Source  │  │Selection│
└───┬──────┘  └───┬──────┘
    │           │
    │           │
    ↓           ↓
┌─────────────────────────┐
│   Media Rendering       │
│                         │
│   ┌───────────────┐   │
│   │ Video Decoder │   │
│   └───────┬───────┘   │
│            │            │
│            ↓            │
│   ┌───────────────┐   │
│   │ Audio Decoder │   │
│   └───────┬───────┘   │
│            │            │
└────────────┴────────────┘
             │
             ↓
    ┌─────────────────┐
    │  Surface/Display  │
    │  Audio Output     │
    └─────────────────┘
```

---

## Gesture Control Flow

```
         User Touches Screen
                │
                ↓
     ┌────────────────────────┐
     │  onTouchEvent()      │
     │  Capture coordinates │
     └──────────┬─────────────┘
                │
                ↓
       Is Near Edge?
      (isEdge() check)
                │
        ┌───────┴───────┐
        │              │
      Yes              No
        │              │
        ↓              ↓
  Ignore Touch   Detect Gesture
                       │
         ┌───────────┴───────────┐
         │                        │
   Horizontal Swipe      Vertical Swipe
         │                        │
         ↓                        ↓
    ┌──────────┐          ┌──────────┐
    │   SEEK    │          │ Left/Right │
    │           │          │    Side?   │
    │ Calculate │          └────┬──────┘
    │  delta X  │               │
    │           │       ┌──────┴──────┐
    │  Convert  │       │            │
    │to time ms │     Left        Right
    │           │       │            │
    │   Seek    │       ↓            ↓
    │  player   │  Brightness   Volume
    └──────────┘    Adjust      Adjust
                    Window      Audio
                 Brightness   Manager


  Long Press Detection
          │
          ↓
    ┌───────────────┐
    │ Check Position │
    └───────┬────────┘
            │
    ┌───────┴───────┐
    │                │
  Left            Right
    │                │
    ↓                ↓
  Speed           Speed
  0.5x            2.0x


  Pinch Gesture
       │
       ↓
┌────────────────┐
│ ScaleGesture  │
│   Detector    │
└───────┬────────┘
        │
        ↓
  Scale Factor
        │
        ↓
┌────────────────┐
│ Apply to Video │
│ Surface Scale │
└────────────────┘
```

---

## Error Handling Flow

```
  Exception Thrown
  (Anywhere in App)
         │
         ↓
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ UncaughtExceptionHandler.         ┃
┃ uncaughtException()                ┃
┃                                    ┃
┃ 1. Get stack trace string         ┃
┃ 2. Create DebugActivity intent    ┃
┃ 3. Add error as extra              ┃
┃ 4. Wrap in PendingIntent           ┃
┗━━━━━━━━━━━━━┬━━━━━━━━━━━━━━━━━━━━┛
              │
              ↓
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ AlarmManager.set()                 ┃
┃ Schedule restart in 1 second       ┃
┗━━━━━━━━━━━━━┬━━━━━━━━━━━━━━━━━━━━┛
              │
              ↓
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ SoiadMahediLogger.broadcastLog()  ┃
┃ Send error to external viewers    ┃
┗━━━━━━━━━━━━━┬━━━━━━━━━━━━━━━━━━━━┛
              │
              ↓
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ Process.killProcess()             ┃
┃ Terminate app immediately          ┃
┗━━━━━━━━━━━━━┬━━━━━━━━━━━━━━━━━━━━┛
              │
              │ (1 second delay)
              │
              ↓
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ AlarmManager triggers              ┃
┃ PendingIntent                      ┃
┗━━━━━━━━━━━━━┬━━━━━━━━━━━━━━━━━━━━┛
              │
              ↓
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ App Restarts                       ┃
┗━━━━━━━━━━━━━┬━━━━━━━━━━━━━━━━━━━━┛
              │
              ↓
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ DebugActivity.onCreate()          ┃
┃                                    ┃
┃ - Get error from intent            ┃
┃ - Display stack trace in TextView  ┃
┃ - Show "Report" button              ┃
┃ - Show "Restart" button             ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

---

This comprehensive documentation provides a complete understanding of the SUic Player architecture, code structure, and implementation details. Use these documents as a reference for modifying and extending the application.
