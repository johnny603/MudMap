# MudMap

MudMap is a single-phone, offline “walk, talk, map” slice. Press the record
button to start recording and request a location fix; press it again to
save an app-private `.m4a`, decode it to 16 kHz mono PCM, transcribe it with
whisper.cpp, extract trail terms, and save the entry in Room. The map loads
saved entries on launch and after saving. Entries with coordinates have
pins; tapping a pin opens a bottom sheet with the transcript and tags.
Entries without a location fix remain saved but cannot have a geographic pin.

## Patterns used

- Adapter (`WhisperCppAdapter`): absorbs whisper.cpp's native-library,
  model-file, PCM-input, and JNI integration requirements.
- Port (`Transcriber`): absorbs the boundary between logging and concrete
  transcription, allowing the test to supply `FakeTranscriber`.
- Port (`EntityExtractor`): absorbs the boundary between logging and concrete
  extraction, allowing the test to supply `FakeExtractor`.

`RuleBasedExtractor` is the concrete extraction adapter behind its port.
`AudioDecoder` is plain codec/resampling code, not another pattern.
The vocabulary is plain data; its required 20 lines have no comment header.

## Deferred

- Sync: no entries or audio leave the phone.
- Ollama: no local language-model extraction is included.
- Laptop worker: all processing stays on the phone.
- Tier precedence: no competing extraction tiers are reconciled.
- Per-field override: extracted fields are not manually overridden.
- Purge: audio and database retention policies await the first hike.
- Delete: entry removal is not included.
- Undo: reversing saved entries is not included.
- State: no named lifecycle abstraction is introduced.
- Command: button presses directly call the controller and use case.
- Observer: Room is read explicitly without subscriptions.
- Fourth tier: no additional enrichment tier is included.
- Community tags: only the bundled vocabulary produces tags.
- Note field: the entity contains only its six specified fields.

## Human Intervention Used

- Create or use the Android Studio Empty Activity project with Kotlin,
  Views, and minSdk 26; use package `com.mudmap` or adjust packages.
- Add compatible Room runtime/compiler, MapLibre Native, Coroutines Android,
  and Play Services Location dependencies yourself; no versions are assumed.
- Configure Room annotation processing yourself.
- Copy production Kotlin files into the application's source directory.
- Keep the supplied test files in this delivery tree, but place them under
  `app/src/androidTest/kotlin/com/mudmap/test/` in the actual project.
  Add AndroidX Test, its runner, and JUnit dependencies and configure the runner.
- Copy `extractors/trail_words.txt` to `app/src/main/assets/trail_words.txt`;
  the Kotlin source directory is not an Android assets directory.
- Write the JNI C++ shim and CMakeLists.txt using the whisper.cpp Android
  example. Build `libmudmap_whisper.so` for the device ABI.
- Implement the instance JNI method for
  `com.mudmap.adapters.WhisperCppAdapter.whisperTranscribe`.
  Read the instance's `modelPath` String field, initialize whisper.cpp with
  that model, transcribe the supplied 16 kHz mono floats, and return text.
  Manage native contexts, JNI references, and errors in the human-written shim.
- Download `ggml-base.en.bin` and put it in `app/src/main/assets/`.
  The adapter copies it to app-private storage on first transcription.
- Download font glyphs to `assets/fonts/{fontstack}/{range}.pbf` and sprite
  JSON/PNG files to `assets/map/sprite.json` and `assets/map/sprite.png`;
  include high-resolution sprite variants if required by the installed SDK.
- Pre-cache a vector PMTiles archive as `assets/region.pmtiles`.
  The supplied style expects `water` and `transportation` source layers;
  verify the archive schema and adjust those layer names if necessary.
- Verify that the installed MapLibre Native version supports
  `pmtiles://file:///android_asset/region.pmtiles`.
  If it does not, convert the archive to MBTiles and change the tile-source
  URL line to the file-backed URL supported by that installation.
  MBTiles may itself require a native protocol integration; conversion alone
  does not guarantee support, and the human must supply that integration.
- Verify the installed MapLibre APIs used here, including marker annotations
  and local asset URL handling; no SDK version is guessed.
- Declare microphone and coarse/fine location permissions in the manifest
  yourself, and grant permissions on the device. Location denial is allowed
  and produces a saved entry with a nullable GPS point.
- Build and install yourself. No build, test, lint, formatting, installation,
  download, repository creation, or manifest editing was performed here.
- Keep this first slice in the foreground while recording or transcribing;
  background recording and foreground-service support are not included.
- Review and adjust the 20 vocabulary terms after the first hike.
  There is no stemming: “muddy” does not match “mud”.
- `recordedAt` is the save-time epoch timestamp created by the use case,
  not a separately captured recording-start timestamp.
