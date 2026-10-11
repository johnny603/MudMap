# MudMap implementation and verification

The local implementation completes the requested minimal slice and produces
a signed debug APK. Build and host checks pass; physical-phone and Android
instrumentation validation are still required. Large model and map assets are
distributed in the source ZIP/APK rather than committed to Git.

## Repository baseline

Inspection started from commit `e32b0bf81d68b2c00c64f00b286bf76af1c5a069`
of the [MudMap repository](https://github.com/johnny603/MudMap).
The checkout had incorrect `com.mudmap.slice1` imports, production-directory
test sources, a missing fake extractor, no Gradle/manifest/native build wiring,
and conflicting alternate entry/pattern scaffolding outside the minimal slice.

## Completed locally

- Added a pinned Gradle wrapper, build configuration, permissions, and backup exclusions.
- Corrected packages, relocated instrumentation tests, and supplied the missing fake.
- Removed the alternate domain entity and excluded scaffolding from production.
- Kept the Room entity's six fields and both one-method ports intact.
- Included JNI/CMake integration and pinned whisper.cpp as a Git submodule.
- Added atomic model staging and actionable model/native-library errors.
- Prepared the SHA-256-verified English base model and verified local PMTiles.
- Bundled a San Francisco demo region, Latin glyphs, and valid empty sprites.
- Corrected the style's source-layer names for the prepared archive.
- Added lifecycle cleanup, system insets, foreground-only recording interruption,
  and a plain bottom-sheet file to preserve the 150-line source limit.
- Added host, Android codec/native/UI tests, and repeatable verification scripts.

## Passed checks

| Check | Result |
|---|---|
| `testDebugUnitTest` | 18 tests, zero failures or errors |
| Rule extraction tests | 6 passed |
| PCM conversion/resampling tests | 8 passed |
| Use-case/Room/converter tests | 4 passed |
| `assembleDebug` | Passed; arm64-v8a and x86_64 native libraries included |
| `assembleDebugAndroidTest` | Passed; runnable test APK generated |
| `lintDebug` | Passed with zero errors and 35 warnings |
| Scope script | Passed; checked authored files stay within 150 lines |
| `git diff --check` | Passed |
| APK signature | Verified APK Signature Scheme v2 |
| APK alignment | Passed `zipalign -c -P 16 -v 4` |
| Native arm64 library alignment | All three libraries use 0x4000 load alignment |
| Merged manifest | No INTERNET permission; mic and location declared |
| PMTiles structure | CLI verification passed |
| Model checksum | Matches the published SHA-256 |
| Production JNI host smoke test | Passed with the bundled base.en model |

The direct JNI test decoded the upstream JFK fixture to float PCM with ffmpeg
and returned: “And so my fellow Americans, ask not what your country can do
for you, ask what you can do for your country.” It validates the production
JNI symbol, instance modelPath field, model loading, inference, and UTF-8 return.
It does not exercise Android MediaCodec or Android microphone hardware.

Remaining lint warnings concern hardcoded English presentation text, missing
launcher artwork, optional Kotlin extensions, kapt, and newer dependency
versions. Deprecated marker-annotation compiler warnings remain; the pinned
SDK compiles those APIs. These were not hidden with a lint baseline.

## Runtime checks not claimed as passed

The sandbox has no KVM acceleration. An Android 9 x86_64 emulator booted, but
the large APK installation stalled beyond the bounded timeout, so Android
instrumentation did not run to completion. Runtime map rendering, marker taps,
MediaRecorder/MediaCodec behavior, and native Android inference are unverified.

The test APK includes the fake-port in-memory Room test, real AAC-to-native
transcription test, and map-pin/bottom-sheet and recorder checks. Run them on
a connected test device with `./gradlew connectedDebugAndroidTest`; grant
microphone permission first, for example with:

```sh
adb shell pm grant com.mudmap android.permission.RECORD_AUDIO
```

The map UI test intentionally inserts a sample entry into the app database.
Use a fresh test installation, not a phone database containing real hikes.

## First-phone checklist

- Install the debug APK and confirm the included demo region is suitable.
- Grant microphone permission; grant precise location if desired.
- Record “saw a heron by the creek, trail was muddy,” then stop.
- Expect `heron` and `creek`; do not expect `mud` from `muddy`.
- Repeat using the exact word “mud” and check the added condition.
- Wait for a GPS fix before stopping, tap the pin, and inspect its bottom sheet.
- Relaunch and confirm that the saved entry still appears.
- Deny location and confirm saving without a geographic pin.
- Disable connectivity and confirm local transcription/map behavior.
- Check actual-phone latency, battery use, audio quality, and permissions.

The San Francisco extract covers approximately -122.53 to -122.35 longitude
and 37.70 to 37.83 latitude through zoom 14; it is not general hike coverage.
The debug APK is not a production-signed release.
