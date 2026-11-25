# Rafi Music

Java Native Android music player starter.

## Features in this first build
- Local audio library scan using MediaStore
- Local playback queue
- Online playback from a direct HTTP(S) audio URL
- Media3 ExoPlayer + MediaSessionService for background playback and system media controls
- Premium dark native UI
- GitHub Actions debug APK build

## Build
Push this folder to a GitHub repository. GitHub Actions runs on push or manually from Actions.
Download the `RafiMusic-debug` artifact from the workflow run.

## Notes
- Online playback expects a direct audio URL or supported stream URL, not a normal webpage link.
- Only stream audio you own or are authorized to play.
- This is the first functional foundation. Playlist persistence, favorites, artwork extraction, search, and a full seek bar are planned next.
- Uses AndroidX Media3 1.11.1 and Android Gradle Plugin 8.13.0.
