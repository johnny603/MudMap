#!/usr/bin/env bash
set -euo pipefail

if [[ "$(uname)" == "Darwin" ]]; then
  brew list --versions openjdk@17 >/dev/null || brew install openjdk@17
  brew list --versions pmtiles   >/dev/null || brew install pmtiles
  brew list --versions ffmpeg    >/dev/null || brew install ffmpeg
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
else
  sudo apt-get update
  sudo apt-get install -y openjdk-17-jdk ffmpeg curl
  # pmtiles: grab the release binary
  curl -fsSL -o /tmp/pmtiles.tgz \
    https://github.com/protomaps/go-pmtiles/releases/latest/download/go-pmtiles_Linux_x86_64.tar.gz
  sudo tar -xzf /tmp/pmtiles.tgz -C /usr/local/bin pmtiles
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
fi

echo "export JAVA_HOME=$JAVA_HOME" >> ~/.profile
echo "Environment ready. Run: source ~/.profile"
