#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
! rg 'com\.mudmap\.slice1|com\.mudmap\.patterns|EntryRepository' app/src
[[ ! -d app/src/main/kotlin/com/mudmap/test ]]
[[ "$(wc -l < app/src/main/assets/trail_words.txt)" -eq 20 ]]
diff app/src/main/assets/trail_words.txt \
    app/src/main/kotlin/com/mudmap/extractors/trail_words.txt
! rg 'https?://' app/src/main/kotlin/com/mudmap/MapViewController.kt
! rg 'StateFlow|Flow<|LiveData|observeForever' app/src/main/kotlin
while IFS= read -r path; do
    lines="$(wc -l < "$path")"
    [[ "$lines" -le 150 ]] || { echo "$path has $lines lines"; exit 1; }
done < <(rg --files app/src scripts docs tools gradle -g '*.kt' -g '*.java' \
    -g '*.cpp' -g '*.sh' -g '*.md' -g '*.xml' -g 'CMakeLists.txt' -g '*.properties')
echo "Scope checks passed."
