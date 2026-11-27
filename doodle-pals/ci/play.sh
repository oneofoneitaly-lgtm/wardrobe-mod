#!/usr/bin/env bash
# Starts a real Forge client with Doodle Pals, draws, brings pals to life and takes screenshots.
# Screen 1280x720, GUI scale 2 (screen coordinates are twice the GUI ones). Run from doodle-pals/.
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
rclick() { xdotool mousedown 3; sleep 0.25; xdotool mouseup 3; sleep 0.6; }
key() { xdotool keydown "$1"; sleep 0.2; xdotool keyup "$1"; sleep 0.8; }
cmd() { key t; xdotool type --delay 20 -- "$1"; sleep 0.4; key Return; }
check() { cmd "/execute $1 run say $2"; }
# canvas cell -> screen
cx() { echo $((366 + 16 * $1)); }
cy() { echo $((176 + 16 * $1)); }
drag() { # x0 y0 x1 y1 (cells)
  xdotool mousemove "$(cx $1)" "$(cy $2)"; sleep 0.3; xdotool mousedown 1; sleep 0.2
  for i in 1 2 3 4; do
    xdotool mousemove "$(( $(cx $1) + ($(cx $3) - $(cx $1)) * i / 4 ))" "$(( $(cy $2) + ($(cy $4) - $(cy $2)) * i / 4 ))"; sleep 0.15
  done
  xdotool mouseup 1; sleep 0.3
}
swatch() { click $((374 + 32 * $1)) 574; }
tool() { click 304 $((186 + 40 * $1)); }   # 0 brush 1 erase 2 fill 3 eyes 4 size 5 mirror 6 undo 7 clear 8 idea
create() { click 886 568; sleep 1; }
finish() {
  cp "$log" "$out/latest.log" 2>/dev/null
  cp -r run/crash-reports "$out/" 2>/dev/null
  grep -iE "doodlepals.*(exception|unable|missing|failed|error)|(unable|missing|failed|error).*doodlepals" "$log" > "$out/problems.txt" 2>/dev/null
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
click 640 295; shot 02-create-world 6
click 640 322                       # Allow commands
click 482 684
sleep 8
waitfor "joined the game" 420 || { shot fail; finish "could not join a world" 1; }
sleep 40
cmd "/gamerule sendCommandFeedback false"
cmd "/gamerule doDaylightCycle false"
cmd "/gamerule doMobSpawning false"
cmd "/weather clear"
cmd "/time set 5500"
cmd "/execute at @s run fill ~-20 199 ~-20 ~20 199 ~20 grass_block"
cmd "/execute at @s run tp @s ~ 200 ~ 0 15"
sleep 12
cmd "/execute at @s run summon marker ~ ~ ~ {Tags:[\"centre\"]}"
cmd "/scoreboard objectives add pals dummy"
P="@e[type=doodlepals:pal]"

# ---- the sketchbook
cmd "/clear @s"
cmd "/item replace entity @s weapon.mainhand with doodlepals:sketchbook"
cmd "/give @s ink_sac 5"
key 1
shot 03-holding-book 3
rclick; shot 10-sketch-empty 2
tool 8; shot 11-idea-dragon 1
tool 8; shot 12-idea-slime 1
tool 8; shot 13-idea-cat 1
tool 8; shot 14-idea-ghost 1
# by hand: a green blob with red feet
tool 7
swatch 3
tool 4          # brush size 3
tool 5          # mirror
drag 8 7 10 7
for row in 9 12 15; do drag 6 $row 10 $row; done
drag 5 14 5 16
swatch 0
drag 7 19 7 20
swatch 2
drag 10 3 10 5
tool 3
click "$(cx 8)" "$(cy 10)"
click 886 384; xdotool type --delay 60 -- "Pippo"
xdotool mousemove 886 130
shot 15-hand-drawn 2
xdotool mousemove 374 574; shot 16-colour-tooltip 1
create; shot 20-born-first-person 1
check "if entity @e[type=doodlepals:pal,name=Pippo]" doodle-born-ok
check "if entity @s[nbt={Inventory:[{id:\"minecraft:ink_sac\",count:4}]}]" doodle-ink-ok

# close up: still, in front of the player and facing them
key F1
cmd "/effect give $P slowness 600 255 true"
cmd "/execute at @s rotated ~ 0 run tp $P ^ ^ ^2.4 facing entity @s feet"
cmd "/tp @s ~ ~ ~ 0 18"
shot 21-pal-front 3
cmd "/tp @s ~1.9 ~ ~1.0 facing entity @e[type=doodlepals:pal,limit=1]"
shot 22-pal-quarter 3
key F1
cmd "/execute at @e[tag=centre,limit=1] run tp @s ~ ~ ~ 0 18"

# ---- three pals (creative: free)
cmd "/gamemode creative"
cmd "/clear @s"
key j; tool 8; shot 23-key-opens 1; create
key j; tool 8; tool 8; tool 8; create
cmd "/execute store result score @s pals if entity $P"
check "if score @s pals matches 3" doodle-three-ok
key j; create; shot 24-too-many 0.5     # the screen closes by itself
sleep 2
cmd "/effect clear $P"
cmd "/execute at @e[tag=centre,limit=1] run tp $P ~ ~ ~3.2"
sleep 1
cmd "/effect give $P slowness 600 255 true"
i=-2
for k in 1 2 3; do
  cmd "/execute at @e[tag=centre,limit=1] run tp @e[type=doodlepals:pal,limit=1,sort=random,tag=!placed] ~$i ~ ~3.4 180 0"
  cmd "/execute at @e[tag=centre,limit=1] positioned ~$i ~ ~3.4 run tag @e[type=doodlepals:pal,limit=1,sort=nearest] add placed"
  i=$((i + 2))
done
cmd "/execute at @e[tag=centre,limit=1] run tp @s ~ ~ ~ 0 16"
key F1; shot 30-three-pals 4
cmd "/tp @s ~-3 ~ ~2 facing ~3 ~-1 ~1.6"
shot 31-three-pals-side 3
cmd "/execute at @e[tag=centre,limit=1] run tp @s ~ ~ ~ 0 16"
cmd "/time set midnight"
shot 32-night-glow 4
cmd "/time set 5500"
key F1
check "if entity @s[nbt={active_effects:[{id:\"minecraft:speed\"}]}]" doodle-speed-ok

# ---- they walk and fight for their owner
cmd "/effect clear $P"
cmd "/gamemode survival"
cmd "/effect give @s resistance 600 4 true"
cmd "/execute at @e[tag=centre,limit=1] run tp @s ~ ~ ~-6 0 16"
sleep 1; shot 33-following-walking 0.2
sleep 4
cmd "/execute at @s run summon husk ~ ~ ~2.2 {NoAI:1b,PersistenceRequired:1b}"
cmd "/tp @s ~ ~ ~ 0 20"
sleep 1
xdotool mousedown 1; sleep 0.15; xdotool mouseup 1; sleep 0.8; xdotool mousedown 1; sleep 0.15; xdotool mouseup 1
shot 40-fight 0.6
shot 41-fight-later 1
sleep 10
check "unless entity @e[type=husk]" doodle-fight-ok
cmd "/kill @e[type=husk]"
cmd "/kill @e[type=item,nbt=!{Item:{id:\"doodlepals:doodle_page\"}}]"

# ---- a fallen pal leaves its page; the page wakes it again
cmd "/clear @s"
cmd "/execute at @s run kill @e[type=doodlepals:pal,limit=2,sort=nearest]"
sleep 2
cmd "/execute at @s run tp @e[type=item] @s"
sleep 3
check "if entity @s[nbt={Inventory:[{id:\"doodlepals:doodle_page\"}]}]" doodle-page-ok
key 1
shot 50-page-first-person 3
key e; shot 51-pages-in-inventory 2
xdotool mousemove 496 560; shot 52-page-tooltip 1
key Escape
cmd "/tp @s ~ ~ ~ 0 40"
sleep 1; rclick; sleep 1
cmd "/execute store result score @s pals if entity $P"
check "if score @s pals matches 2" doodle-wake-ok
cmd "/tp @s ~ ~ ~ 0 18"
shot 53-woken 1

# ---- shift + right click stores a pal
cmd "/clear @s"
cmd "/kill @e[type=doodlepals:pal,limit=1,sort=furthest]"
cmd "/kill @e[type=item]"
cmd "/effect give $P slowness 600 255 true"
cmd "/execute at @s rotated ~ 0 run tp $P ^ ^ ^1.8 facing entity @s feet"
cmd "/tp @s ~ ~ ~ 0 32"
sleep 1; xdotool keydown Shift_L; sleep 0.6; rclick; xdotool keyup Shift_L; sleep 1
check "unless entity $P" doodle-store-ok
shot 54-stored 1
key j; shot 55-sketch-again 2
key Escape

alive || finish "client crashed" 1
if ls run/crash-reports/* >/dev/null 2>&1; then finish "crash report found" 1; fi
if grep -q "failed to load a valid ResourcePackInfo" "$log"; then finish "bad pack metadata" 1; fi
for check in born ink three speed fight page wake store; do
  check="doodle-$check-ok"
  if grep -q "$check" "$log"; then step "passed $check"; else step "FAILED $check"; fi
done
finish "done" 0
