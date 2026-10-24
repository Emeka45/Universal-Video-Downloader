# Universal Video Downloader

A lightweight Android browser and video downloader. It uses the Android System WebView already available on the phone rather than bundling a separate Chromium engine.

## Features

- Lightweight in-app browser for opening video websites.
- Monitors HTML5 video playback and detects exposed video sources.
- Shows available direct video source choices, including resolution labels when the website exposes them.
- Downloads supported direct video files using Android DownloadManager, including background progress notifications.
- Carries the browser's session cookie and referring page when starting a download, where available.
- Paste a direct HTTP/HTTPS video URL or receive one through Android's Share menu.
- Built-in native player for downloaded videos.
- Saves videos under `Movies/Universal Video Downloader`.
- Share or remove completed downloads.
- Light/dark system themes.
- No account, ad SDK, analytics SDK, or cloud backend.

## Detection limits

The detector can show only sources that a website exposes to its page or HTML5 video element. Some websites use encrypted/DRM playback, private APIs, authentication restrictions, or player-specific systems that do not expose downloadable source URLs. This app does not bypass those restrictions.

This lightweight version downloads direct video files (for example MP4 or WebM). HLS/DASH adaptive playlists (`.m3u8` / `.mpd`) require segment downloading and remuxing, so they are identified as streaming playlists and are not falsely presented as completed video downloads. A page may show the current playback resolution without exposing alternate downloadable resolutions.

## Build

GitHub Actions runs unit tests, builds a debug APK, and uploads it as a workflow artifact. The project uses Java 17, Android Gradle Plugin 8.7.3, Gradle 8.9, and compile SDK 35.
