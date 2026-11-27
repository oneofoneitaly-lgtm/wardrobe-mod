#!/usr/bin/env bash
# Starts a real Forge client with the five jars, drives it like a player and takes screenshots.
# Screen 1280x720, GUI scale 2 (GUI coordinates are half of these).
out="$PWD/report"
mkdir -p "$out/shots" run/mods run/config
cp jars/*.jar run/mods/
printf '%s\n' onboardAccessibility:false skipMultiplayerWarning:true tutorialStep:none \
  pauseOnLostFocus:false guiScale:2 renderDistance:6 simulationDistance:5 maxFps:30 \
  soundCategory_master:0.0 narrator:0 fullscreen:false > run/options.txt
printf 'earlyWindowControl = false\n' > run/config/fml.toml

portablemc --main-dir "$PWD/mc" --work-dir "$PWD/run" start -u Tester --jvm "$JAVA_HOME/bin/java" \
  --resolution 1280x720 forge:1.21.1-52.1.20 > "$out/client.log" 2>&1 &
game=$!
log=run/logs/latest.log

step() { echo "STEP $*" | tee -a "$out/steps.txt"; }
shot() { sleep "${2:-2}"; import -window root "$out/shots/$1.png"; step "shot $1"; }
alive() { kill -0 $game 2>/dev/null; }
click() { xdotool mousemove "$1" "$2"; sleep 0.7; xdotool mousedown 1; sleep 0.15; xdotool mouseup 1; sleep 0.8; }
key() { xdotool keydown "$1"; sleep 0.2; xdotool keyup "$1"; sleep 0.8; }
cmd() { key t; xdotool type --delay 25 -- "$1"; sleep 0.4; key Return; }
finish() {
  cp "$log" "$out/latest.log" 2>/dev/null
  cp -r run/crash-reports "$out/" 2>/dev/null
  cp -r run/config/aura "$out/config" 2>/dev/null
  alive && kill $game
  pkill -f net.minecraftforge 2>/dev/null
  step "$1"
  exit "$2"
}
waitfor() { # pattern seconds
  for _ in $(seq 1 $(($2 / 2))); do
    grep -qE "$1" "$log" 2>/dev/null && return 0
    alive || return 1
    sleep 2
  done
  return 1
}

waitfor "Sound engine started|OpenAL initialized|Error starting SoundSystem" 600 || { shot fail; finish "client did not start" 1; }
sleep 45
win=$(xdotool search --name Minecraft | tail -1)
[ -n "$win" ] && xdotool windowmove "$win" 0 0 windowactivate "$win" 2>/dev/null
xdotool mousemove 40 650
shot 01-title 8
xdotool mousemove 640 310; shot 02-title-hover
click 527 526; shot 02b-options
key Escape
click 640 472                       # Aura Studio
shot 03-studio-menu
click 302 248; shot 04-studio-hud
click 302 316; shot 05-studio-sky
click 302 384; shot 06-studio-fx
click 302 452; shot 07-studio-inventory
click 302 180
click 1060 214; click 1060 214; shot 07b-studio-theme-green
click 1024 616; shot 07c-title-green
click 640 472; click 836 214; click 836 214
xdotool mousemove 700 400; for _ in 1 2 3 4 5 6 7 8; do xdotool click 5; sleep 0.2; done; shot 08-studio-menu-bottom
click 1024 616                      # Fatto
shot 09-title
click 640 310; shot 10-create-world 6
click 640 322                       # Allow commands
shot 11-create-world-commands
click 482 684
waitfor "joined the game" 420 || { shot fail; finish "could not join a world" 1; }
sleep 40
shot 12-ingame-first
cmd "/gamerule doDaylightCycle false"
cmd "/gamerule doMobSpawning false"
cmd "/weather clear"
cmd "/time set 6000"
cmd "/item replace entity @s armor.head with diamond_helmet"
cmd "/item replace entity @s armor.chest with netherite_chestplate"
cmd "/item replace entity @s armor.legs with iron_leggings"
cmd "/give @s diamond_sword[enchantments={levels:{sharpness:5}}]"
cmd "/give @s golden_apple 12"
cmd "/give @s enchanted_golden_apple 2"
cmd "/give @s nether_star 3"
cmd "/give @s oak_log 64"
cmd "/effect give @s speed 600 1"
cmd "/effect give @s resistance 600 0"
cmd "/xp add @s 23 levels"
cmd "/damage @s 7"
cmd "/tp @s ~ ~ ~ 180 -8"
sleep 12
shot 13-hud-day 6
cmd "/time set 12600"; sleep 4; shot 14-sky-sunset
cmd "/time set 18000"
cmd "/tp @s ~ ~ ~ 180 -32"; sleep 6; shot 15-sky-night
shot 16-sky-night-later 7
cmd "/time set 1000"
cmd "/tp @s ~ ~ ~ 0 55"; sleep 4; shot 17-block-outline
xdotool keydown c; sleep 2; shot 18-zoom 1; xdotool keyup c
cmd "/tp @s ~ ~ ~ 0 5"
key F5; shot 19-third-person 4; key F5; key F5
cmd "/summon pig ~ ~ ~2 {NoAI:1b}"
cmd "/tp @s ~ ~ ~ 0 30"
sleep 2; xdotool click 1; shot 19b-hit-particles 0.4
cmd "/kill @e[type=pig]"
cmd "/tp @s ~ ~ ~ 0 5"
key e; shot 20-inventory 3
xdotool mousemove 496 494; shot 21-inventory-tooltip
key Escape
cmd "/setblock ~ ~1 ~2 chest"
cmd "/tp @s ~ ~ ~ 0 18"
sleep 2; xdotool click 3; shot 22-chest 3
key Escape
cmd "/gamemode creative"
key e; shot 23-creative 3
key Escape
cmd "/gamemode survival"
key Shift_R; shot 24-studio-ingame 3
click 302 248; shot 25-studio-hud-ingame
click 1030 406; shot 26-hud-editor
xdotool mousemove 80 100; xdotool mousedown 1; sleep 0.3; xdotool mousemove 200 180; sleep 0.3; xdotool mousemove 330 230; sleep 0.3; xdotool mouseup 1
shot 27-hud-editor-moved
key Escape
click 302 384; click 1042 314; shot 27b-color-picker
click 1042 314
click 302 316; shot 28-studio-sky-ingame
xdotool keydown Tab; shot 29-preview 1; xdotool keyup Tab
key Escape
key Escape; shot 30-pause 3
click 640 443; sleep 15; xdotool mousemove 40 650; shot 31-back-to-title 6
alive || finish "client crashed" 1
if ls run/crash-reports/* >/dev/null 2>&1; then finish "crash report found" 1; fi
if grep -q "failed to load a valid ResourcePackInfo" "$log"; then finish "bad pack metadata" 1; fi
finish "done" 0
