# Rafi Music

Java Native Android music player starter.

## Features in this first build
- Local audio library scan using MediaStore
- Local playback queue
- Online playback from a direct HTTP(S) audio URL
- Media3 ExoPlayer + MediaSessionService for background playback and system media controls
- Premium dark native UI
- GitHub Actions debug APK build
- Windows CMD helper for GitHub CLI workflows

## Build on GitHub
Push changes to `main` or manually run the `Build Rafi Music APK` workflow.
The workflow installs Android SDK packages directly using `sdkmanager`; it does not call the obsolete `sdkmanager tools` package.

The APK is uploaded as the `RafiMusic-debug` artifact.

## Windows CMD setup
Install Git and GitHub CLI:

```cmd
winget install --id Git.Git -e --source winget
winget install --id GitHub.cli -e --source winget
```

Close and reopen CMD, then authenticate and clone:

```cmd
gh auth login
gh repo clone rafijordan4/player-music
cd player-music
```

Use the included helper:

```cmd
player-music.cmd login
player-music.cmd pull
player-music.cmd push
player-music.cmd build
player-music.cmd status
player-music.cmd apk
```

`push` commits and pushes local changes, which automatically triggers GitHub Actions.
`build` manually starts the workflow, waits for it, and downloads the APK into `dist`.

## Notes
- Online playback expects a direct audio URL or supported stream URL, not a normal webpage link.
- Only stream audio you own or are authorized to play.
- This is the first functional foundation. Playlist persistence, favorites, artwork extraction, search, and a full seek bar are planned next.
- Uses AndroidX Media3 1.11.1 and Android Gradle Plugin 8.13.0.
