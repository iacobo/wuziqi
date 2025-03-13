#!/usr/bin/env sh

# Gradle Wrapper script for Unix-based systems

set -e

# Determine the directory of the script
DIR="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="Gradle Wrapper"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"

# Check if the Gradle distribution is already downloaded
if [ ! -f "$DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "Downloading Gradle..."
    mkdir -p "$DIR/gradle/wrapper"
    curl -L "https://services.gradle.org/distributions/gradle-8.11.1-bin.zip" -o "$DIR/gradle.zip"
    unzip -q "$DIR/gradle.zip" -d "$DIR/gradle/wrapper"
    rm "$DIR/gradle.zip"
fi

# Execute Gradle
exec "$DIR/gradle/wrapper/gradle" "$@"
