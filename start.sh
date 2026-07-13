#!/bin/bash
# GenExPlus local run helper.
# Usage: ./start.sh <report.conf> [application.properties]

set -euo pipefail

if [ $# -lt 1 ] || [ $# -gt 2 ]; then
    echo "Usage: $0 <report.conf> [application.properties]"
    exit 1
fi

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG="$1"

resolve_default_properties() {
    if [ -f "$DIR/target/additional_resources/application.properties" ]; then
        echo "$DIR/target/additional_resources/application.properties"
    elif [ -f "$DIR/additional_resources/application.properties" ]; then
        echo "$DIR/additional_resources/application.properties"
    elif [ -f "$DIR/application.properties" ]; then
        echo "$DIR/application.properties"
    else
        echo ""
    fi
}

PROPS="${2:-$(resolve_default_properties)}"

JAR=""
if [ -f "$DIR/genexplus.jar" ]; then
    JAR="$DIR/genexplus.jar"
else
    for candidate in "$DIR"/target/genexplus-*.jar; do
        if [ -f "$candidate" ]; then
            JAR="$candidate"
            break
        fi
    done
fi

if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
    echo "Error: genexplus JAR not found. Run 'mvn package' first."
    exit 1
fi

if [ -z "$PROPS" ] || [ ! -f "$PROPS" ]; then
    echo "Error: properties file not found."
    echo "Run 'mvn package' or pass an explicit path:"
    echo "  $0 $CONFIG target/additional_resources/application.properties"
    exit 1
fi

if [ ! -f "$CONFIG" ]; then
    echo "Error: report config not found: $CONFIG"
    exit 1
fi

LIB_DIR="$DIR/lib"
RESOURCES_DIR="$DIR/additional_resources"
if [ ! -d "$LIB_DIR" ]; then
    LIB_DIR="$DIR/target/lib"
fi
if [ ! -d "$RESOURCES_DIR" ]; then
    RESOURCES_DIR="$DIR/target/additional_resources"
fi

if [ ! -d "$LIB_DIR" ]; then
    echo "Error: lib/ directory not found. Run 'mvn package' first."
    exit 1
fi

if [ ! -d "$RESOURCES_DIR" ]; then
    echo "Error: additional_resources/ directory not found. Run 'mvn package' first."
    exit 1
fi

echo "Starting GenExPlus..."
exec java -cp "$LIB_DIR/*:$JAR:$RESOURCES_DIR" \
    io.github.stefanbln.genexplus.report.Main \
    --properties "$PROPS" \
    --config "$CONFIG"
