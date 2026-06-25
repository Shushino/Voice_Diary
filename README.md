#  VoiceDiary

A private, encrypted voice and text diary app for Android. Write entries, record your thoughts, track your mood, and reflect on your journey — all stored securely on your device.

---

## About This Project:

I'm a 300-level Software Engineering student, and this is my first vibe-coded Android app.

I noticed most diary apps don't have proper voice recording built in, so I decided to build one that does — with encryption, mood tracking, reminders, and more. Also wanted to see how far I could push vibe coding as a development approach. Turns out, pretty far.

---

## Features:

- **Voice Recording** — Attach voice notes to any diary entry using your microphone
- **Speech-to-Text** — Generate transcripts for your voice recordings (on-device)
- **Encrypted Storage** — All entries are encrypted locally with SQLCipher. Your data never leaves your device.
- **PIN & Biometric Lock** — Protect your diary with a PIN and optional fingerprint unlock
- **Mood Tracking** — Tag entries with a mood and visualise your emotional patterns over time
- **Photos** — Attach images from your gallery to your diary entries
- **Color Palettes** — Choose from several color presets (Ocean, Forest, Sunset, Rose, Slate) or use Dynamic Color
- **Calendar View** — Browse past entries by date on an interactive monthly calendar
- **Search** — Search across entry titles, body text, and tags instantly
- **Writing Stats** — Track your total entries, word count, writing streak, and mood distribution
- **Notifications & Reminders** — Set a daily reminder to write and receive a weekly summary
- **Themes** — Light, Dark, AMOLED, and System modes with font size options
- **Backup & Restore** — Export all data as a `.vdiary` archive and import it back seamlessly
- **Export** — Share entries as text or export as PDF
- **Trash** — Deleted entries go to trash first, with restore and permanent delete options

---

## Tech Stack:

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| Dependency Injection | Hilt |
| Database | Room + SQLCipher (encrypted) |
| Audio Playback | ExoPlayer (Media3) |
| Audio Recording | MediaRecorder |
| Background Work | WorkManager |
| Preferences | DataStore |
| Async | Kotlin Coroutines + Flow |

---

## 🚀 Getting Started

### Prerequisites
- Android 8.0 (API 26) or higher
- ~50MB free storage

### Install via APK
1. Download the latest APK from the [Releases](../../releases) page
2. On your Android device, enable **Install from unknown sources** in Settings
3. Open the downloaded APK and install

---

## License:

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## Libraries Used:

Built with the following open source libraries:
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room](https://developer.android.com/training/data-storage/room)
- [SQLCipher for Android](https://www.zetetic.net/sqlcipher/open-source/)
- [Hilt](https://dagger.dev/hilt/)
- [ExoPlayer / Media3](https://developer.android.com/media/media3/exoplayer)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Coil](https://coil-kt.github.io/coil/) (Image loading)
- [Accompanist](https://google.github.io/accompanist/) (Permissions)


[![RepoRanker](https://reporanker.com/repos/Shushino/Voice_Diary/badge)](https://reporanker.com/repos/Shushino/Voice_Diary)
