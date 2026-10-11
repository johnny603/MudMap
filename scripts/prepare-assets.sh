#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ASSETS="$ROOT/app/src/main/assets"
mkdir -p "$ASSETS/fonts/Open Sans Semibold"
MODEL="$ASSETS/ggml-base.en.bin"
MODEL_HASH=a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002
checksum() {
    if command -v sha256sum >/dev/null; then sha256sum "$1" | cut -d' ' -f1
    else shasum -a 256 "$1" | cut -d' ' -f1; fi
}
if [[ ! -f "$MODEL" ]] || [[ "$(checksum "$MODEL")" != "$MODEL_HASH" ]]; then
    curl --fail --location --retry 3 \
        "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin" \
        --output "$MODEL.part"
    [[ "$(checksum "$MODEL.part")" == "$MODEL_HASH" ]] || {
        echo "Whisper model checksum mismatch." >&2; exit 1;
    }
    mv "$MODEL.part" "$MODEL"
fi
curl --fail --location --retry 3 \
    "https://demotiles.maplibre.org/font/Open%20Sans%20Semibold/0-255.pbf" \
    --output "$ASSETS/fonts/Open Sans Semibold/0-255.pbf"
if [[ ! -f "$ASSETS/region.pmtiles" ]]; then
    PMTILES="${PMTILES:-pmtiles}"
    command -v "$PMTILES" >/dev/null || {
        echo "Install the pmtiles CLI, or set PMTILES to its absolute path." >&2
        echo "https://github.com/protomaps/go-pmtiles/releases" >&2
        exit 1
    }
    # A compact San Francisco DEMO extract, not a promise of hike coverage.
    SOURCE="${PMTILES_SOURCE:-https://build.protomaps.com/20261009.pmtiles}"
    BOUNDS="${BBOX:--122.53,37.70,-122.35,37.83}"
    "$PMTILES" extract "$SOURCE" "$ASSETS/region.pmtiles.part" \
        "--bbox=$BOUNDS" --maxzoom=14 --download-threads=4
    "$PMTILES" verify "$ASSETS/region.pmtiles.part"
    mv "$ASSETS/region.pmtiles.part" "$ASSETS/region.pmtiles"
fi
echo "Offline assets ready. Replace the demo region before an out-of-area hike."
