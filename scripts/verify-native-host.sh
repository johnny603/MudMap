#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="$ROOT/app/build/native-host"
[[ -f "$ROOT/app/src/main/assets/ggml-base.en.bin" ]] || {
    echo "Prepare the whisper model first." >&2; exit 1;
}
cmake -S "$ROOT/tools/native-smoke" -B "$BUILD" -G Ninja \
    -DCMAKE_BUILD_TYPE=Release
cmake --build "$BUILD" --target mudmap_whisper -j2
mkdir -p "$BUILD/classes"
javac -d "$BUILD/classes" "$ROOT/tools/native-smoke/WhisperCppAdapter.java"
ffmpeg -y -v error -i "$ROOT/third_party/whisper.cpp/samples/jfk.wav" \
    -ar 16000 -ac 1 -f f32le "$BUILD/jfk.f32"
java -cp "$BUILD/classes" com.mudmap.adapters.WhisperCppAdapter \
    "$BUILD/native/libmudmap_whisper.so" "$ROOT/app/src/main/assets/ggml-base.en.bin" \
    "$BUILD/jfk.f32"
