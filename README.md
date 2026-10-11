# MudMap

MudMap is a single-phone, offline “walk, talk, map” slice. Press Record to
capture an app-private `.m4a` and request one location fix; press Stop to
decode AAC into 16 kHz mono float PCM, transcribe with whisper.cpp, extract
exact vocabulary terms, and save the six-field entry in Room. The map reads
entries on launch and after saves; pins open a transcript-and-tags bottom sheet.
Entries without coordinates remain saved but cannot have geographic pins.

## Build and run

Use Android Studio with JDK 17, SDK 35, Build Tools 35.0.0, NDK 27.0.12077973,
and CMake 3.22.1. The wrapper pins Gradle 8.11.1; the project pins AGP 8.9.2,
Kotlin 2.1.20, Room 2.7.1, MapLibre 12.3.1, Coroutines 1.10.2, and Location
21.3.0. AGP's toolchain requirements are documented by
[Android Developers](https://developer.android.com/build/releases/past-releases/agp-8-9-0-release-notes).
Production sources use package `com.mudmap`, Kotlin Views, and minSdk 26.

```sh
git submodule update --init --recursive
bash scripts/prepare-assets.sh
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
./gradlew connectedDebugAndroidTest
```

An optional Linux host JNI check uses JDK 17, CMake, Ninja, and ffmpeg:
`bash scripts/verify-native-host.sh`. It calls the production JNI code, but
does not substitute for Android microphone, codec, GPS, or map checks.

The source ZIP includes the pinned native source and prepared assets, so its
first two commands are unnecessary unless replacing or restoring those files.
Git checkout users must initialize the submodule and prepare ignored assets.
The asset script requires curl and the
[pmtiles CLI](https://github.com/protomaps/go-pmtiles/releases/latest).
Use `PMTILES=/absolute/path/to/pmtiles` if it is not on PATH.
Large model/archive files are intentionally ignored by Git, not downloaded
by the app. The APK already contains them.

## Offline assets and coverage

The included map is a San Francisco DEMO extract: longitude -122.53 to
-122.35, latitude 37.70 to 37.83, zooms through 14. Do not assume it covers
your hike. Prepare a replacement outside the app before leaving:

```sh
# Move the existing region.pmtiles aside first; the script preserves it.
BBOX="west,south,east,north" bash scripts/prepare-assets.sh
```

Use actual numeric coordinates. If changing regions, update the style's
initial center in `MapViewController.kt`; saved pins still center themselves.
The supplied archive uses Protomaps `water` and `roads` source layers.
The map displays OpenStreetMap attribution; respect the archive's ODbL
terms as described in [Protomaps downloads](https://docs.protomaps.com/basemaps/downloads).

All style resource URLs are local; the source is
`pmtiles://file:///android_asset/region.pmtiles`. MapLibre documents local
archives in its [PMTiles example](https://maplibre.org/maplibre-native/android/examples/data/PMTiles/).
The style intentionally has no text labels, so its bundled Latin glyph range
and empty transparent sprites are sufficient; it does not fetch missing fonts.

The English base model is staged atomically into app-private storage.
Its expected SHA-256 is
`a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002`,
from the [model file](https://huggingface.co/ggerganov/whisper.cpp/blob/main/ggml-base.en.bin).
The JNI implementation is included, with whisper.cpp pinned to v1.9.5,
commit `d1be6fde11ac6e0407606b4e42fe72d34add8037`; see
[upstream releases](https://github.com/ggml-org/whisper.cpp/releases).

## Patterns used

- Adapter (`WhisperCppAdapter`): absorbs native library, model, PCM, and JNI integration.
- Port (`Transcriber`): absorbs the logging/transcription boundary and permits a test fake.
- Port (`EntityExtractor`): absorbs the logging/extraction boundary and permits a test fake.

`RuleBasedExtractor` is the concrete extraction adapter behind its port.
`AudioDecoder`, resampling, Room, UI, and location code are plain files.
`EntryBottomSheet.kt` only separates display code to keep MainActivity under
the 150-line limit; it introduces no additional named abstraction.

## Behavior and limits

- Location is requested at record start, timed out at ten seconds, and frozen
  at stop. Stopping before a fix arrives saves null rather than waiting.
- Location denial is allowed; microphone denial prevents recording.
- Keep the app visible while recording. Leaving interrupts recording without
  logging an entry; retained audio is not automatically recovered or purged.
- Rotation is handled without recreating this minimal Activity.
- Saved `recordedAt` is save-time epoch milliseconds, not recording-start time.
- Matching ignores case and punctuation, matches whole phrases, and does not
  stem: `muddy` does not match `mud`. Review the 20 terms after the first hike.
- Processing failures retain audio and show an error; there is no retry screen.
- Record short observations; decoding keeps the entire recording's PCM in
  memory rather than doing streaming inference.
- Internet permission is removed, and cloud backup/device-transfer exclusions
  cover app data. Location services still operate under the system provider.
- Device builds target arm64-v8a and x86_64; no 32-bit ABI is bundled.

## Deferred

- Sync: nothing is uploaded or reconciled.
- Ollama: no language-model extraction is included.
- Laptop worker: processing stays on the phone.
- Tier precedence: there are no competing extraction tiers.
- Per-field override: manual tag replacement is not included.
- Purge: retention decisions await the first hike.
- Delete: entry removal is not included.
- Undo: reversing entries is not included.
- State: there is no named lifecycle abstraction.
- Command: the button directly calls the controller and use case.
- Observer: database reads are explicit, with no subscriptions.
- Fourth tier: additional enrichment is not included.
- Community tags: only the bundled vocabulary produces tags.
- Note field: the entity keeps exactly six fields.

## Human Intervention Required

- Open the finished project in Android Studio and install the supplied debug
  APK, or build your own; it is not a production-signed distribution.
- Confirm that the demo archive covers your actual hike, or replace it.
- Grant microphone permission and, optionally, precise location permission.
- Verify real microphone capture, GPS acquisition, pin taps, and transcription
  latency/battery use on your own phone; an emulator cannot prove hike behavior.
- If replacing MapLibre, verify `pmtiles://` and local assets again. A fallback
  to MBTiles needs a supported native protocol and a source URL change;
  converting the archive alone does not establish compatibility.
- Review `docs/verification.md` for checks actually run and their limits.
- The sandbox emulator booted, but installation exceeded the bounded timeout;
  Android instrumentation tests were built, not successfully executed here.
- The repository contains source and asset-preparation scripts; the prepared
  model and demo map remain ignored by Git and are included in the delivered ZIP/APK.
