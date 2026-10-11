# Sequence diagram

Location is requested at record start and frozen at record stop. Saving
proceeds even when the cached point is null.

```mermaid
sequenceDiagram
    actor User
    participant Main as MainActivity
    participant Recorder as RecorderController
    participant Location as LocationProvider
    participant Service as LogEntryUseCase
    participant Transcriber as Transcriber (port)
    participant Whisper as WhisperCppAdapter
    participant Decoder as AudioDecoder
    participant JNI as Human-written JNI
    participant Extractor as EntityExtractor (port)
    participant Rules as RuleBasedExtractor
    participant DAO as EntryDao
    participant Map as MapViewController

    Main->>DAO: getAll() on launch
    DAO-->>Main: saved entries
    Main->>Map: reload(entries)

    User->>Main: Press Record
    Main->>Recorder: start()
    Recorder-->>Main: recording started
    Main->>Location: requestOnce()
    Note over Location: Request high-accuracy fix; timeout after 10 seconds
    Note over Location: Cache fix if available, otherwise null

    User->>Main: Press Stop
    Main->>Recorder: stop()
    Recorder-->>Main: app-private .m4a path
    Main->>Location: last()
    Location-->>Main: GeoPoint or null
    Main->>Service: log(audioPath, geoPoint)
    Service->>Transcriber: transcribe(audioPath)
    Transcriber->>Whisper: concrete implementation
    Whisper->>Decoder: decode(audioPath)
    Decoder-->>Whisper: 16 kHz mono FloatArray
    Whisper->>JNI: whisperTranscribe(samples)
    JNI-->>Whisper: transcript
    Whisper-->>Transcriber: transcript
    Transcriber-->>Service: transcript
    Service->>Extractor: extract(transcript)
    Extractor->>Rules: concrete implementation
    Rules-->>Extractor: EntitySet
    Extractor-->>Service: species and conditions
    Service->>DAO: insert(entry)
    DAO-->>Service: generated id
    Service-->>Main: saved TrailEntry
    Main->>DAO: getAll()
    DAO-->>Main: saved entries
    Main->>Map: reload(entries)

    User->>Map: Tap entry pin
    Map->>Main: selected TrailEntry
    Main-->>User: Bottom sheet with transcript and tags
```
