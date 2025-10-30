#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR=$(cd "$(dirname "$0")" && pwd)
OUT_DIR="$PROJECT_DIR/target/classes"
JAR_FILE="$PROJECT_DIR/target/mcp-template.jar"

rm -rf "$PROJECT_DIR/target"
mkdir -p "$OUT_DIR"

SOURCE_FILES=$(find "$PROJECT_DIR/src/main/java" -name '*.java')
if [ -z "$SOURCE_FILES" ]; then
  echo "No source files found" >&2
  exit 1
fi

javac -d "$OUT_DIR" $SOURCE_FILES

(cd "$OUT_DIR" && jar cf "$JAR_FILE" .)

echo "Build successful: $JAR_FILE"
echo "Running demo client..."
java -cp "$OUT_DIR" com.example.mcp.client.ClientApplication
