#!/bin/sh
# Compiles the pure game rules (game/GameEngine, Shapes, Adventure) with a tool and runs it,
# using the Kotlin compiler already in the Gradle cache (no kotlinc needed).
#   sh tools/kt.sh tools/EngineTest.kt
#   sh tools/kt.sh tools/AdventureBench.kt 10 20 30
set -e
C="$HOME/.gradle/caches/modules-2/files-2.1"
jar() { find "$C/$1" -name "$2-*.jar" ! -name "*sources*" | sort -V | tail -1; }
SEP=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";"; C=$(cygpath -m "$C");; esac
STD=$(jar org.jetbrains.kotlin/kotlin-stdlib kotlin-stdlib)
CP="$(jar org.jetbrains.kotlin/kotlin-compiler-embeddable kotlin-compiler-embeddable)$SEP$STD"
for dep in "org.jetbrains.kotlin/kotlin-script-runtime kotlin-script-runtime" "org.jetbrains.kotlin/kotlin-reflect kotlin-reflect" \
    "org.jetbrains.kotlin/kotlin-daemon-embeddable kotlin-daemon-embeddable" "org.jetbrains.intellij.deps/trove4j trove4j" \
    "org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm kotlinx-coroutines-core-jvm" "org.jetbrains/annotations annotations"; do
    CP="$CP$SEP$(jar $dep)"
done
G=app/src/main/java/com/farias/prismablocks/game
OUT="${TMPDIR:-/tmp}/prisma-tools/$(basename "$1" .kt)"
[ "$SEP" = ";" ] && OUT=$(cygpath -m "$OUT")
rm -rf "$OUT"
java -cp "$CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -cp "$STD" -d "$OUT" -nowarn \
    $G/GameEngine.kt $G/Shapes.kt $G/Adventure.kt "$1"
MAIN=$(basename "$1" .kt)Kt
shift
java -cp "$OUT$SEP$STD" "$MAIN" "$@"
