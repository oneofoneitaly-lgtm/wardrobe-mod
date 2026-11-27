#!/usr/bin/env bash
# Starts a real client on a virtual screen, creates a world and lets DevHook test the mod.
mc="$1"
mkdir -p report/shots run/config/wardrobe/modelli/cappelli
printf '%s\n' onboardAccessibility:false skipMultiplayerWarning:true tutorialStep:none \
  pauseOnLostFocus:false guiScale:2 renderDistance:4 simulationDistance:5 maxFps:30 \
  soundCategory_master:0.0 narrator:0 > run/options.txt
printf 'earlyWindowControl = false\n' > run/config/fml.toml
# a "user" model, to check the import folder
cp models/blockbench/cilindro.bbmodel run/config/wardrobe/modelli/cappelli/modello_utente.bbmodel
sed -i 's/"wardrobe":{[^}]*},//' run/config/wardrobe/modelli/cappelli/modello_utente.bbmodel

gradle runClient -Pmc="$mc" -Pautotest=true --no-daemon --stacktrace > report/client.log 2>&1 &
game=$!

shot() { import -window root "report/shots/$1.png"; }
click() { xdotool mousemove "$1" "$2"; sleep 1; xdotool click 1; }

stage=boot
declare -A done_marks
start=$SECONDS
last_boot_shot=0
while kill -0 $game 2>/dev/null; do
  sleep 2
  elapsed=$((SECONDS - start))
  if [ $elapsed -gt 900 ]; then echo "timeout"; shot timeout; kill $game; break; fi
  if [ $stage = boot ] && grep -qE "Sound engine started|Created: [0-9]+x[0-9]+x[0-9]+ minecraft:textures/atlas/gui|OpenAL initialized|Failed to get OpenAL|Error starting SoundSystem" report/client.log; then
    sleep 30
    win=$(xdotool search --name Minecraft | tail -1)
    [ -n "$win" ] && xdotool windowmove "$win" 0 0 windowactivate "$win" 2>/dev/null
    sleep 3; shot 00-title
    click 640 276                       # Singleplayer
    sleep 8; shot 00-create-world
    click 482 692                       # Create New World
    sleep 5; shot 00-loading
    stage=world; world_start=$SECONDS
  fi
  # take the screenshots the mod asks for
  while read -r name; do
    [ -n "${done_marks[$name]:-}" ] && continue
    done_marks[$name]=1
    sleep 2; shot "$name"
  done < <(grep -o "AUTOTEST:SHOT [a-z0-9-]*" report/client.log | awk '{print $2}')
  if [ -z "${done_marks[key]:-}" ] && grep -q "AUTOTEST:PRESSKEY" report/client.log; then
    done_marks[key]=1; xdotool key j
  fi
  if [ $stage = world ] && ! grep -q "AUTOTEST:JOINED" report/client.log && [ $((SECONDS - world_start)) -gt 150 ]; then
    shot stuck; echo "could not join a world"; kill $game; break
  fi
done
wait $game 2>/dev/null
cp run/logs/latest.log report/latest.log 2>/dev/null
cp -r run/crash-reports report/ 2>/dev/null
grep "AUTOTEST:" report/client.log | sed 's/.*AUTOTEST:/AUTOTEST:/' > report/result.txt
cat report/result.txt
grep -q "AUTOTEST:DONE" report/client.log
