#!/usr/bin/env bash
# Starts a real Forge client with Inventory Styles, opens every inventory and takes screenshots.
# Screen 1280x720, GUI scale 2 (screen coordinates are twice the GUI ones). Run from inventory-styles/.
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
finish() {
  cp "$log" "$out/latest.log" 2>/dev/null
  cp -r run/crash-reports "$out/" 2>/dev/null
  cp run/config/invstyles.properties "$out/" 2>/dev/null
  grep -iE "invstyles.*(exception|unable|missing|failed|error|could not)|(unable|missing|failed|error).*invstyles|mixin.*(error|fail)" "$log" > "$out/problems.txt" 2>/dev/null
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
# the style screen (GUI 640x360, panel left 120 top 66)
group() { click 380 $((194 + 40 * $1)); }        # 0 all, 1 inventory ... 9 hotbar
card() { click $((547 + 106 * $1)) 482; }        # 0 vanilla 1 glass 2 fantasy 3 neon 4 pastel
colour() { click $((590 + 40 * $1)) 558; }       # 0 violet 1 cyan 2 emerald 3 gold 4 rose 5 red
done_button() { click 972 558; }

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
cmd "/execute at @s run tp @s ~ 200 ~ 0 10"
sleep 12
cmd "/execute at @s run summon marker ~ ~ ~ {Tags:[\"centre\"]}"
home() { cmd "/execute at @e[tag=centre,limit=1] run tp @s ~ ~ ~ 0 ${1:-10}"; }

# things to carry, to judge how items read on every style
for item in "diamond_sword" "golden_apple 12" "torch 64" "ender_pearl 8" "oak_log 32" "iron_pickaxe" \
  "bread 16" "bow" "arrow 64"; do cmd "/give @s $item"; done
for item in "emerald 40" "redstone 64" "coal 33" "iron_ingot 20" "book 6" "lapis_lazuli 30"; do cmd "/give @s $item"; done
cmd "/xp add @s 40 levels"

# ---- the default look, hotbar included
shot 03-hotbar-default 3
key e; shot 04-inventory-default 2
xdotool mousemove 90 690; click 40 24; shot 05-menu-from-button 2
done_button; sleep 1; shot 06-back-to-inventory 1
key Escape

# ---- the menu
key i; shot 10-menu 2
group 1
for s in 0 1 2 3 4; do card $s; shot 11-menu-inventory-$s 1.5; done
group 3; card 3; colour 1; shot 12-menu-chests-neon-cyan 2
group 9; card 2; shot 13-menu-hotbar 1.5
group 0; shot 14-menu-all-mixed 1.5
card 1; colour 0; shot 15-menu-all-glass 2
done_button

# ---- every inventory in every style
open_block() { # name block [nbt]
  home 35
  cmd "/execute at @e[tag=centre,limit=1] run setblock ~ ~ ~2 air"
  cmd "/execute at @e[tag=centre,limit=1] run setblock ~ ~ ~2 $2$3"
  sleep 0.6; rclick; shot "$1" 2.5; key Escape; sleep 0.5
}
CHEST='{Items:[{Slot:0b,id:"minecraft:diamond",count:12},{Slot:4b,id:"minecraft:cake",count:1},{Slot:12b,id:"minecraft:netherite_sword",count:1},{Slot:22b,id:"minecraft:amethyst_shard",count:40},{Slot:26b,id:"minecraft:nether_star",count:3}]}'
FURNACE='{BurnTime:3000s,CookTime:100s,CookTimeTotal:200s,Items:[{Slot:0b,id:"minecraft:raw_iron",count:20},{Slot:1b,id:"minecraft:coal",count:9},{Slot:2b,id:"minecraft:iron_ingot",count:4}]}'
BREW='{BrewTime:200s,Fuel:12b,Items:[{Slot:3b,id:"minecraft:nether_wart",count:3},{Slot:0b,id:"minecraft:potion",count:1},{Slot:4b,id:"minecraft:blaze_powder",count:5}]}'

colour_of=(0 0 3 1 4)   # violet, violet, gold, cyan, rose
names=(vanilla glass fantasy neon pastel)
for s in 1 2 3 4; do
  n=${names[$s]}
  home
  key i; group 0; card $s; colour ${colour_of[$s]}; done_button
  cmd "/gamemode survival"
  shot 20-$n-hotbar 2
  key e; shot 21-$n-inventory 2
  click 692 334; shot 22-$n-recipe-book 2     # the recipe book button
  click 846 334; key Escape
  open_block 23-$n-chest chest "$CHEST"
  open_block 24-$n-crafting crafting_table
  open_block 25-$n-furnace furnace "$FURNACE"
  open_block 26-$n-anvil anvil
  open_block 27-$n-enchanting enchanting_table
  open_block 28-$n-brewing brewing_stand "$BREW"
  cmd "/gamemode creative"
  key e; shot 29-$n-creative 2.5; key Escape
done

# ---- the rarer ones, in two styles
more() { # style-name
  cmd "/execute at @e[tag=centre,limit=1] run setblock ~1 ~ ~2 chest[type=right,facing=north]"
  open_block 30-$1-double-chest "chest[type=left,facing=north]" "$CHEST"
  cmd "/execute at @e[tag=centre,limit=1] run setblock ~1 ~ ~2 air"
  open_block 31-$1-shulker shulker_box "$CHEST"
  open_block 32-$1-dispenser dispenser
  open_block 33-$1-hopper hopper
  open_block 34-$1-smoker smoker
  open_block 35-$1-stonecutter stonecutter
  open_block 36-$1-loom loom
  open_block 37-$1-cartography cartography_table
  open_block 38-$1-smithing smithing_table
  open_block 39-$1-grindstone grindstone
  open_block 40-$1-beacon beacon
  open_block 41-$1-crafter crafter
  home 15
  cmd "/execute at @e[tag=centre,limit=1] run summon villager ~ ~ ~2 {NoAI:1b,VillagerData:{profession:\"minecraft:librarian\",level:3,type:\"minecraft:plains\"},Offers:{Recipes:[{buy:{id:\"minecraft:emerald\",count:3},sell:{id:\"minecraft:book\",count:1},maxUses:9},{buy:{id:\"minecraft:paper\",count:24},sell:{id:\"minecraft:emerald\",count:1},maxUses:9}]}}"
  sleep 1; rclick; shot 42-$1-villager 2.5; key Escape
  cmd "/kill @e[type=villager]"
  cmd "/execute at @e[tag=centre,limit=1] run summon horse ~ ~ ~2.5 {NoAI:1b,Tame:1b}"
  sleep 1; xdotool keydown Shift_L; sleep 0.5; rclick; xdotool keyup Shift_L; shot 43-$1-horse 2.5; key Escape
  cmd "/tp @e[type=horse] ~ -80 ~"
  cmd "/kill @e[type=item]"
  cmd "/execute at @e[tag=centre,limit=1] run setblock ~ ~ ~2 air"
}
cmd "/gamemode creative"
home; key i; group 0; card 1; colour 0; done_button
more glass
home; key i; group 0; card 2; done_button
more fantasy

# ---- commands, mixed styles and going back to vanilla
home
cmd "/invstyle set all vanilla"
cmd "/gamemode survival"
key e; shot 50-vanilla-restored 2; key Escape
cmd "/invstyle set inventory neon"
cmd "/invstyle set hotbar pastel"
cmd "/invstyle colour emerald"
shot 51-mixed-hotbar 2
key e; shot 52-mixed-inventory 2; key Escape
open_block 53-mixed-chest-vanilla chest "$CHEST"
cmd "/invstyle"; shot 54-menu-command 2
done_button

# ---- reloading resources keeps the styles
home
xdotool keydown F3; sleep 0.4; key t; xdotool keyup F3
sleep 25
key e; shot 60-after-reload 3; key Escape
shot 61-after-reload-hotbar 1

alive || finish "client crashed" 1
if ls run/crash-reports/* >/dev/null 2>&1; then finish "crash report found" 1; fi
if grep -q "failed to load a valid ResourcePackInfo" "$log"; then finish "bad pack metadata" 1; fi
passed() { if grep -qE "$2" "$log"; then step "passed $1"; else step "FAILED $1"; fi; }
passed start "\[invstyles\] restyled [1-9][0-9]+ textures of all"
passed menu-neon "restyled [1-9][0-9]* textures of chests \(neon"
passed command "restyled [1-9][0-9]* textures of inventory \(neon"
if grep -q "Could not restyle" "$log"; then step "FAILED restyle-errors"; else step "passed restyle-errors"; fi
if grep -q "hotbar=pastel" run/config/invstyles.properties && grep -q "colour=emerald" run/config/invstyles.properties; then
  step "passed saved"; else step "FAILED saved"; fi
finish "done" 0
