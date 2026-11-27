#!/usr/bin/env bash
# Starts a real Forge client with the five jars, plays through the content and takes screenshots.
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
rclick() { xdotool mousedown 3; sleep 0.25; xdotool mouseup 3; sleep 0.6; }
key() { xdotool keydown "$1"; sleep 0.2; xdotool keyup "$1"; sleep 0.8; }
cmd() { key t; xdotool type --delay 20 -- "$1"; sleep 0.4; key Return; }
hold() { cmd "/item replace entity @s weapon.mainhand with $1"; }
finish() {
  cp "$log" "$out/latest.log" 2>/dev/null
  cp -r run/crash-reports "$out/" 2>/dev/null
  grep -iE "mythoria.*(exception|unable|missing|failed|error)|(unable|missing|failed|error).*mythoria" "$log" > "$out/problems.txt" 2>/dev/null
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
sleep 8; shot 02b-after-create 0
waitfor "joined the game" 420 || { shot fail; finish "could not join a world" 1; }
sleep 40
shot 03-welcome 1
cmd "/gamerule sendCommandFeedback false"
cmd "/gamerule doDaylightCycle false"
cmd "/gamerule doMobSpawning false"
cmd "/weather clear"
cmd "/time set 5500"
cmd "/execute at @s run fill ~-24 199 ~-24 ~24 199 ~24 grass_block"
cmd "/execute at @s run tp @s ~ 200 ~ 0 8"
sleep 12

# ---- market (GUI panel starts at 116,64; screen pixels are double)
key m; shot 10-market-creatures 3
click 466 326; shot 11-market-golem
click 542 208; shot 12-market-weapons
click 466 386; shot 13-market-storm
click 738 208; shot 14-market-armor-set
click 466 326; shot 14b-market-helmet
click 934 208; shot 15-market-feast
click 466 386; shot 15b-market-drumstick
click 862 542; shot 16-market-poor 1
key Escape
cmd "/crowns give @s 5000"
cmd "/market"; sleep 2
click 542 208; click 466 266; click 862 542; shot 17-market-bought 1
click 738 208; click 862 542
click 346 208; click 862 542; click 466 326; click 862 542
click 934 208; click 862 542
shot 18-market-after-shopping 5
key Escape
key e; shot 19-bought-items 3
key Escape

# ---- every item in the inventory
cmd "/clear @s"
for i in mythoria_weapons:phoenix_blade mythoria_weapons:frost_fang mythoria_weapons:storm_blade \
  mythoria_creatures:griffin_egg mythoria_creatures:golem_core mythoria:crown \
  mythoria_armory:dragon_helmet mythoria_armory:dragon_chestplate mythoria_armory:dragon_leggings \
  mythoria_armory:dragon_boots mythoria_feast:moon_apple mythoria_feast:fairy_cake \
  mythoria_feast:dragon_drumstick mythoria_feast:elven_mead; do cmd "/give @s $i"; done
key 1
key e; shot 20-all-icons 3
xdotool mousemove 496 560; shot 21-tooltip
key Escape

# ---- swords
cmd "/clear @s"
key 1
n=0
for sword in phoenix_blade frost_fang storm_blade; do
  n=$((n + 1))
  hold "mythoria_weapons:$sword"
  cmd "/tp @s ~ ~ ~ 0 5"
  shot 3$n-$sword-first-person 3
  key F5; key F5; shot 3$n-$sword-front 3; key F5
  cmd "/summon zombie ~ ~ ~4 {NoAI:1b,PersistenceRequired:1b}"
  sleep 1; rclick; shot 3$n-$sword-power 0.3
  sleep 2
  cmd "/kill @e[type=zombie]"
  cmd "/kill @e[type=item]"
done

# sword seen from the side, to check the grip
cmd "/clear @s"
cmd "/summon armor_stand ~ ~ ~2.5 {ShowArms:1b,Rotation:[90f,0f],HandItems:[{id:\"mythoria_weapons:frost_fang\",count:1},{}]}"
cmd "/tp @s ~ ~ ~ 0 18"
key F1; shot 34-sword-side 3; key F1
cmd "/kill @e[type=armor_stand]"
cmd "/kill @e[type=item]"

# ---- armour
cmd "/clear @s"
cmd "/item replace entity @s armor.head with mythoria_armory:dragon_helmet"
cmd "/item replace entity @s armor.chest with mythoria_armory:dragon_chestplate"
cmd "/item replace entity @s armor.legs with mythoria_armory:dragon_leggings"
cmd "/item replace entity @s armor.feet with mythoria_armory:dragon_boots"
hold "mythoria_weapons:phoenix_blade"
cmd "/tp @s ~ ~ ~ 0 5"
key F1
key F5; shot 40-armor-back 3
key F5; shot 41-armor-front 3
key F5
cmd "/summon armor_stand ~-1.2 ~ ~3 {Rotation:[150f,0f],ArmorItems:[{id:\"mythoria_armory:dragon_boots\",count:1},{id:\"mythoria_armory:dragon_leggings\",count:1},{id:\"mythoria_armory:dragon_chestplate\",count:1},{id:\"mythoria_armory:dragon_helmet\",count:1}]}"
cmd "/summon armor_stand ~1.2 ~ ~3 {Rotation:[20f,0f],ArmorItems:[{id:\"mythoria_armory:dragon_boots\",count:1},{id:\"mythoria_armory:dragon_leggings\",count:1},{id:\"mythoria_armory:dragon_chestplate\",count:1},{id:\"mythoria_armory:dragon_helmet\",count:1}]}"
cmd "/tp @s ~ ~ ~ 0 15"
shot 42-armor-stands 4
key F1
cmd "/kill @e[type=armor_stand]"

# ---- food
cmd "/clear @s"
cmd "/tp @s ~ ~ ~ 0 5"
n=0
for food in moon_apple fairy_cake dragon_drumstick elven_mead; do
  n=$((n + 1))
  hold "mythoria_feast:$food"
  shot 5$n-$food 3
done
xdotool mousedown 3; shot 55-drinking 0.8; sleep 2.5; xdotool mouseup 3
shot 56-effects 1
cmd "/effect clear @s"
cmd "/clear @s"

# ---- griffin
G="@e[type=mythoria_creatures:golden_griffin]"
cmd "/tp @s ~ ~ ~ 0 12"
key F1
cmd "/summon mythoria_creatures:golden_griffin ~ ~ ~5 {Rotation:[145f,0f]}"
cmd "/effect give $G slowness 600 255 true"
shot 60-griffin-wild 5
cmd "/kill $G"
sleep 1.5
cmd "/summon mythoria_creatures:golden_griffin ~ ~ ~5 {Rotation:[90f,0f],NoAI:1b,OnGround:1b}"
shot 61-griffin-side 4
cmd "/tp $G ~ ~-300 ~"
sleep 2
cmd "/summon mythoria_creatures:golden_griffin ~ ~2.5 ~7 {NoAI:1b,NoGravity:1b,Rotation:[160f,0f]}"
cmd "/tp @s ~ ~ ~ 0 -6"
shot 62-griffin-spread-wings 4
cmd "/tp $G ~ ~-300 ~"
sleep 2
key F1
hold "mythoria_creatures:griffin_egg"
cmd "/tp @s ~ ~ ~ 0 42"
sleep 1; rclick; sleep 1
cmd "/tp @s ~ ~ ~ 0 18"
shot 63-griffin-hatched 2
cmd "/clear @s"
# right click on a griffin that walks around is a matter of luck for a script: make it stand still first
cmd "/execute as $G at @p rotated ~ 0 run tp @s ^ ^ ^2.5"
cmd "/effect give $G slowness 5 255 true"
cmd "/tp @s ~ ~ ~ 0 22"
sleep 1; rclick; sleep 1
cmd "/execute on vehicle if entity @s[type=mythoria_creatures:golden_griffin] run say mythoria-ride-click-ok"
cmd "/execute on vehicle run tag @p add mythoria_rider"
cmd "/execute unless entity @s[tag=mythoria_rider] run ride @s mount @e[type=mythoria_creatures:golden_griffin,limit=1,sort=nearest]"
sleep 1
cmd "/execute on vehicle if entity @s[type=mythoria_creatures:golden_griffin] run say mythoria-riding-ok"
cmd "/effect clear $G"
# no teleporting from here on (it would dismount): look around with the mouse
xdotool mousemove_relative -- 0 -110; shot 64-griffin-mounted 5
key F5; shot 65-riding-back 3
xdotool keydown space; sleep 3.5; shot 66-flying-up 0.1
xdotool keydown w; sleep 1.2; shot 67-flying 0.1; xdotool keyup w; xdotool keyup space
cmd "/execute if entity @s[y=204,dy=200] run say mythoria-flight-ok"
xdotool keydown space; sleep 1.5; key F5; shot 68-flying-front 0.1; xdotool keyup space
key F5
# jump off in mid air
xdotool keydown space; sleep 1.5; xdotool keyup space
xdotool keydown Shift_L; sleep 0.5; xdotool keyup Shift_L
shot 69-dismount 0.5
cmd "/execute if entity @s[nbt={active_effects:[{id:\"minecraft:slow_falling\"}]}] run say mythoria-slowfall-ok"
cmd "/gamemode creative"
cmd "/kill $G"
cmd "/tp @s ~ 200 ~ 0 8"
cmd "/gamemode survival"
cmd "/effect clear @s"
sleep 3
cmd "/kill @e[type=item]"

# ---- golem
B="@e[type=mythoria_creatures:crystal_golem]"
N="@e[type=mythoria_creatures:crystal_golem,limit=1,sort=nearest]"
key F1
cmd "/summon mythoria_creatures:crystal_golem ~ ~ ~7 {NoAI:1b,Rotation:[180f,0f],PersistenceRequired:1b}"
cmd "/tp @s ~ ~ ~ 0 -4"
shot 70-golem-front-nohud 5
key F1; shot 71-golem-bossbar 2
cmd "/kill $B"
sleep 3
cmd "/kill @e[type=item]"
cmd "/summon mythoria_creatures:crystal_golem ~ ~ ~7 {NoAI:1b,Rotation:[135f,0f],PersistenceRequired:1b}"
shot 72-golem-quarter 4
cmd "/data merge entity $N {NoAI:0b}"
cmd "/effect give @s resistance 600 4 true"
cmd "/effect give @s regeneration 600 4 true"
cmd "/tp @s ~ ~ ~ 0 0"
shot 73-golem-attack 2
shot 74-golem-fight 1
shot 75-golem-fight-later 3
cmd "/data merge entity $N {NoAI:1b}"
cmd "/execute at $N run tp @s ~ 200 ~-6 0 25"
cmd "/damage $N 1000 minecraft:player_attack by @s"
shot 76-golem-rewards 3
cmd "/execute at @s run tp @e[type=item] @s"
sleep 3
cmd "/summon zombie ~ ~ ~3 {NoAI:1b}"
cmd "/damage @e[type=zombie,limit=1,sort=nearest] 100 minecraft:player_attack by @s"
shot 77-zombie-coins 3
cmd "/market"; shot 78-market-wallet 3
key Escape
shot 79-final 2
key Escape; sleep 1
click 640 443; sleep 15; shot 80-back-to-title 4   # not essential
alive || finish "client crashed" 1
if ls run/crash-reports/* >/dev/null 2>&1; then finish "crash report found" 1; fi
if grep -q "failed to load a valid ResourcePackInfo" "$log"; then finish "bad pack metadata" 1; fi
for check in mythoria-riding-ok mythoria-flight-ok mythoria-slowfall-ok; do
  if grep -q "$check" "$log"; then step "passed $check"; else step "FAILED $check"; fi
done
finish "done" 0
