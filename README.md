# Universal Video Downloader

A lightweight native Android app for downloading permitted direct video files and watching them locally.

## Features

- Paste a direct HTTP/HTTPS video URL.
- Receive a video URL through Android's Share menu.
- Background downloads through Android DownloadManager.
- Download progress and completion status.
- Saves videos under `Movies/Universal Video Downloader`.
- Built-in native video playback.
- Share or remove completed downloads.
- Light and dark system themes.
- No account, ad SDK, analytics SDK, cloud backend, or WebView in the core app.

## Current scope

Version 0.1 focuses on direct downloadable media URLs such as MP4 and other formats that Android can play. A normal webpage URL is not automatically converted into a video download.

The app does not bypass DRM, authentication, paywalls, private-content restrictions, or other access controls. Only download media you are authorized to save.

## Build

GitHub Actions builds the debug APK and uploads it as a workflow artifact. The project uses Java 17, Android Gradle Plugin 8.7.3, Gradle 8.9, and compile SDK 35.
