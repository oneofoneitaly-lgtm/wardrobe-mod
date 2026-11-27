#!/usr/bin/env bash
# prints the public/protected API of the classes the mod touches
set -u
CP=$(cat build/classpath.txt)
while read -r cls; do
  [ -z "$cls" ] && continue
  javap -protected -cp "$CP" "$cls" 2>/dev/null || echo "MISSING $cls"
done < ci/classes.txt
