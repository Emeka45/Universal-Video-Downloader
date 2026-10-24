# Universal Video Downloader Design

**Goal:** Build a very lightweight Android app focused on downloading permitted video files and watching completed downloads, with an optional browser kept out of the core download path.

## Architecture

The app uses native Android APIs and Java with no third-party runtime dependencies. `DownloadManager` handles background downloads and notifications; `VideoView` handles local playback; `SharedPreferences` stores a tiny index of download IDs and labels. The optional browser is an isolated future feature and is not required by the core APK.

## Core experience

1. User pastes a direct media URL or shares a URL to the app.
2. The app checks that it is an HTTP(S) URL and performs a lightweight HEAD/metadata check when possible.
3. If the URL appears to point to downloadable video/audio media, the user can start the download.
4. Downloads are stored under the public Movies/Universal Video Downloader directory.
5. The Downloads screen shows queued/running/completed/failed items.
6. Completed video items open in an in-app native `VideoView` player.
7. The user can share or remove completed downloads.

## Boundaries

The first version supports direct downloadable media URLs. It does not bypass DRM, authentication, paywalls, private content, or site access controls. Site-specific extraction can be added later only where permitted.

## Lightweight constraints

No ads SDK, analytics SDK, account system, cloud backend, media framework dependency, large UI framework, or WebView dependency is included in the core build. The UI uses platform `View` classes.

## Testing

A small local JVM test suite covers URL acceptance and safe filename generation. GitHub Actions builds the debug APK with the Android SDK and runs unit tests.
