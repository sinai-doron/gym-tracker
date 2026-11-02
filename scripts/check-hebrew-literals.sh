#!/bin/sh
# Fails when a Kotlin string literal under app/src/main contains Hebrew: interface text belongs in
# res/values*/strings*.xml, so the English setting can show it in English. Comments don't count.
#
# Allowed, with the reason:
#   a line marked "hebrew-ok: <reason>"  checked by hand, e.g. a language listed by its own name
#   data/seed/TrainingPlanData.kt  the owner's seeded exercise names (the exercise library replaces them)
#   data/db/GymDatabase.kt         an old migration's SQL, which must never change
#   data/RunAnnouncements.kt       the spoken Hebrew voice; an English voice needs its own grammar
set -eu
cd "$(dirname "$0")/.."

found=$(find app/src/main/java -name '*.kt' \
    ! -path '*/data/seed/TrainingPlanData.kt' \
    ! -path '*/data/db/GymDatabase.kt' \
    ! -path '*/data/RunAnnouncements.kt' \
    -exec perl -CSD -ne '
        next if /^\s*(\/\/|\/?\*)/;          # a comment line
        next if /hebrew-ok/;                # allowed on purpose, the reason is on the line
        s/\s\/\/.*$//;                       # a comment at the end of a line
        print "$ARGV:$.: $_" if /"[^"\n]*\p{Hebrew}/;
        close ARGV if eof;                   # restart line numbers for the next file
    ' {} +)

if [ -n "$found" ]; then
    echo "Hebrew in Kotlin string literals; move it to res/values*/strings*.xml:"
    echo "$found"
    exit 1
fi
echo "No Hebrew string literals in Kotlin."
