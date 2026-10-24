# Universal Video Downloader Implementation Plan

> **For agentic workers:** Use the host's available task-by-task implementation workflow. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a lightweight native Android downloader that downloads permitted direct media URLs and plays completed videos locally.

**Architecture:** Java + Android platform APIs only. `DownloadManager` provides background downloading; a small `SharedPreferences` index maps download IDs to names/URLs; `VideoView` provides local playback. No third-party runtime libraries are required.

**Tech Stack:** Android Gradle Plugin 8.7.3, Gradle 8.9, Java 17, Android SDK 35, native Android Views, JUnit 4 for JVM tests, GitHub Actions for CI.

## Global Constraints

- Keep the core APK very lightweight.
- Focus on downloading and watching videos.
- Accept pasted/shared HTTP(S) URLs.
- Support background downloads and visible progress/status.
- Store completed files in public Movies/Universal Video Downloader.
- Play completed videos in-app with native `VideoView`.
- Do not include ads, analytics, accounts, cloud backends, Chromium/WebView, or large UI frameworks in the core build.
- Do not bypass DRM, authentication, paywalls, private-content restrictions, or access controls.
- The optional browser is architecturally deferred and must not be required by the downloader.

---

### Task 1: Android project and URL policy

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/emeka45/universalvideodownloader/UrlPolicy.java`
- Create: `app/src/test/java/com/emeka45/universalvideodownloader/UrlPolicyTest.java`

**Interfaces:**
- Produces `UrlPolicy.isHttpUrl(String)`, `UrlPolicy.isLikelyMediaUrl(String,String)`, and `UrlPolicy.safeFileName(String)`.

- [ ] **Step 1: Add focused failing tests** for accepted HTTP/HTTPS URLs, rejected non-HTTP schemes, video MIME types/extensions, and filename sanitization.
- [ ] **Step 2: Verify red** with `gradle test` after CI provisions Gradle; expected failures are missing `UrlPolicy` methods.
- [ ] **Step 3: Implement the minimum URL policy** with no network access and conservative extension/MIME checks.
- [ ] **Step 4: Verify green** with the same unit test command.
- [ ] **Step 5: Run Android compilation** with `gradle :app:assembleDebug`; expected successful debug APK creation.
- [ ] **Step 6: Commit** `feat: scaffold lightweight downloader core`.

### Task 2: Downloader UI and background downloads

**Files:**
- Create: `app/src/main/java/com/emeka45/universalvideodownloader/MainActivity.java`
- Create: `app/src/main/java/com/emeka45/universalvideodownloader/DownloadStore.java`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Modify: `AndroidManifest.xml`

**Interfaces:**
- `DownloadStore.add(long,String,String)`, `DownloadStore.all()`, and `DownloadStore.remove(long)`.
- `MainActivity` consumes shared text URLs and starts `DownloadManager.Request` downloads.

- [ ] **Step 1:** Add tests for store serialization/deserialization and duplicate-ID replacement.
- [ ] **Step 2:** Verify the focused tests fail before `DownloadStore` exists.
- [ ] **Step 3:** Implement the store and a compact single-screen UI with URL input, paste/share handling, download button, status list, and refresh.
- [ ] **Step 4:** Verify unit tests pass and Android debug compilation succeeds.
- [ ] **Step 5:** Exercise install/start, URL submission, invalid URL rejection, and DownloadManager queueing through an emulator/device or CI-compatible smoke check where available.
- [ ] **Step 6:** Commit `feat: add lightweight download manager UI`.

### Task 3: Local video playback and download actions

**Files:**
- Modify: `MainActivity.java`
- Create: `app/src/main/java/com/emeka45/universalvideodownloader/PlayerActivity.java`

**Interfaces:**
- `PlayerActivity` consumes a local `Uri` passed as `Intent.EXTRA_STREAM`.
- Completed DownloadManager rows produce Play, Share, and Delete actions.

- [ ] **Step 1:** Add tests for completed-item action selection at the policy/store boundary.
- [ ] **Step 2:** Verify focused tests fail for missing action mapping.
- [ ] **Step 3:** Implement `VideoView` playback with media controls, safe URI handling, Android share intent, and DownloadManager deletion.
- [ ] **Step 4:** Verify tests pass and compile the debug APK.
- [ ] **Step 5:** Smoke-test completed MP4 playback, share, deletion, and failed-download display.
- [ ] **Step 6:** Commit `feat: add local video playback and download actions`.

### Task 4: CI, privacy documentation, and release verification

**Files:**
- Create: `.github/workflows/android.yml`
- Create: `README.md`
- Create: `PRIVACY.md`
- Create: `LICENSE`

**Interfaces:**
- GitHub Actions runs JVM tests and `:app:assembleDebug` on pushes and pull requests.
- README documents supported direct-media URLs and limitations.
- Privacy policy states that the core app has no account, analytics, ad SDK, or backend.

- [ ] **Step 1:** Add workflow and documentation checks.
- [ ] **Step 2:** Verify workflow YAML structure and local documentation consistency.
- [ ] **Step 3:** Configure GitHub Actions with Java 17, Gradle 8.9, Android SDK 35, unit tests, and debug APK artifact upload.
- [ ] **Step 4:** Run final local/static checks and trigger CI.
- [ ] **Step 5:** Verify CI reports tests passing and APK artifact produced; if CI cannot be triggered through the available connector, report that limitation rather than claiming success.
- [ ] **Step 6:** Commit `ci: build and publish debug APK artifact`.

## Unresolved externally observable decisions

- The first release will use direct downloadable media URLs rather than site-specific extractors. Additional supported providers are a later product decision.
- The optional browser will remain outside the core implementation until a concrete browser requirement is approved.
