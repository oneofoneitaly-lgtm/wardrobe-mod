#!/usr/bin/env python3
"""Writes every model, texture, generated Java layer and metadata file of the modpack.
Run from anywhere:  python3 tools/make.py    (needs numpy and Pillow)"""
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from design_items import *          # noqa: E402,F403
from design_entities import *       # noqa: E402,F403

ROOT = Path(__file__).resolve().parent.parent
VERSION = '1.0.0'

MODS = {
    # source set: (mod id, name, description)
    'core': ('mythoria', 'Mythoria Market', 'Crowns, wallet and the fantasy market of the Mythoria modpack.'),
    'creatures': ('mythoria_creatures', 'Mythoria Creatures', 'The rideable Golden Griffin and the Crystal Golem boss.'),
    'weapons': ('mythoria_weapons', 'Mythoria Weapons', 'Three 3D fantasy swords with powers.'),
    'armory': ('mythoria_armory', 'Mythoria Armory', 'Dragon Armor with a 3D worn model.'),
    'feast': ('mythoria_feast', 'Mythoria Feast', 'Magical 3D food and drinks.'),
}


def res(s):
    return ROOT / 'src' / s / 'resources'


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(data, (dict, list)):
        path.write_text(json.dumps(data, indent=1) + '\n')
    else:
        path.write_text(data)


def item(source, name, builder):
    mod = MODS[source][0]
    model, display = builder()
    model.build()
    assets = res(source) / 'assets' / mod
    tex = assets / 'textures' / 'item' / (name + '.png')
    tex.parent.mkdir(parents=True, exist_ok=True)
    model.tex.save(tex)
    if model.frames > 1:
        write(Path(str(tex) + '.mcmeta'), {'animation': {'frametime': model.frametime, 'interpolate': True}})
    write(assets / 'models' / 'item' / (name + '.json'), model.json(f'{mod}:item/{name}', display))


def items():
    for source, group in ITEMS.items():
        for name, builder in group.items():
            item(source, name, builder)
    for name, pair in armour_icons().items():
        item('armory', name, lambda: pair)


def java(source, package, cls, model):
    folder = ROOT / 'src' / source / 'java' / Path(*package.split('.'))
    write(folder / (cls + '.java'), model.java(package, cls))


def entities():
    for name, cls, factory in (('golden_griffin', 'GriffinLayer', golden_griffin), ('crystal_golem', 'GolemLayer', crystal_golem)):
        model = factory().build()
        folder = res('creatures') / 'assets' / 'mythoria_creatures' / 'textures' / 'entity'
        folder.mkdir(parents=True, exist_ok=True)
        model.tex.save(folder / (name + '.png'))
        model.glow.save(folder / (name + '_glow.png'))
        java('creatures', 'it.adryh.mythoria.creatures.client.gen', cls, model)
    for slot, model in dragon_armour().items():
        model.build()
        folder = res('armory') / 'assets' / 'mythoria_armory' / 'textures' / 'armor'
        folder.mkdir(parents=True, exist_ok=True)
        model.tex.save(folder / f'dragon_{slot}.png')
        java('armory', 'it.adryh.mythoria.armory.client.gen', slot.capitalize() + 'Layer', model)


LANG = {
    'core': {
        'itemGroup.mythoria': 'Mythoria',
        'item.mythoria.crown': 'Crown',
        'item.mythoria.crown.tooltip': 'Use it, or open the Market, to put your Crowns in the wallet',
        'key.mythoria.market': 'Open the Market',
        'key.categories.mythoria': 'Mythoria',
        'mythoria.welcome': 'Welcome to Mythoria! Press M or type /market to open the Market. Earn Crowns by defeating monsters and mining.',
        'mythoria.wallet.deposit': '+%s Crowns  -  wallet: %s',
        'mythoria.wallet.received': 'You received %s Crowns. Wallet: %s',
        'mythoria.wallet.balance': 'Your wallet holds %s Crowns.',
        'mythoria.market.title': 'Mythoria Market',
        'mythoria.market.crowns': 'Crowns',
        'mythoria.market.buy': 'BUY  -  %s',
        'mythoria.market.bought': 'Purchased: %s',
        'mythoria.market.poor': 'Not enough Crowns',
        'mythoria.category.creatures': 'Creatures',
        'mythoria.category.weapons': 'Weapons',
        'mythoria.category.armor': 'Armor',
        'mythoria.category.feast': 'Feast',
    },
    'creatures': {
        'item.mythoria_creatures.griffin_egg': 'Griffin Egg',
        'item.mythoria_creatures.griffin_egg.tooltip': 'Use it on the ground: your own griffin hatches',
        'item.mythoria_creatures.golem_core': 'Golem Core',
        'item.mythoria_creatures.golem_core.tooltip': 'Use it on the ground to wake the boss. Be ready!',
        'entity.mythoria_creatures.golden_griffin': 'Golden Griffin',
        'entity.mythoria_creatures.crystal_golem': 'Crystal Golem',
        'market.mythoria_creatures.griffin_egg': 'Golden Griffin',
        'market.mythoria_creatures.griffin_egg.desc': 'An egg that hatches your mount. Ride it and fly!',
        'market.mythoria_creatures.golem_core': 'Crystal Golem',
        'market.mythoria_creatures.golem_core.desc': 'Summons the boss. Defeat it for a rich reward.',
        'mythoria.griffin.wild': 'Offer raw or cooked meat to tame this griffin',
        'mythoria.griffin.tamed': 'The griffin trusts you now!',
        'mythoria.griffin.sit': 'Your griffin waits here',
        'mythoria.griffin.follow': 'Your griffin follows you',
        'mythoria.griffin.ride': 'Space: climb  -  look down + W: dive  -  Shift: dismount',
        'mythoria.golem.peaceful': 'The Golem cannot wake up in Peaceful difficulty',
    },
    'weapons': {
        'item.mythoria_weapons.phoenix_blade': 'Phoenix Blade',
        'item.mythoria_weapons.phoenix_blade.hit': 'Hit: sets enemies on fire',
        'item.mythoria_weapons.phoenix_blade.use': 'Right click: Flame Wave',
        'item.mythoria_weapons.frost_fang': 'Frost Fang',
        'item.mythoria_weapons.frost_fang.hit': 'Hit: freezes and slows',
        'item.mythoria_weapons.frost_fang.use': 'Right click: Frost Nova',
        'item.mythoria_weapons.storm_blade': 'Storm Blade',
        'item.mythoria_weapons.storm_blade.hit': 'Hit: may call lightning',
        'item.mythoria_weapons.storm_blade.use': 'Right click: strike where you look',
        'market.mythoria_weapons.phoenix_blade': 'Phoenix Blade',
        'market.mythoria_weapons.phoenix_blade.desc': 'Burns enemies. Right click for a Flame Wave.',
        'market.mythoria_weapons.frost_fang': 'Frost Fang',
        'market.mythoria_weapons.frost_fang.desc': 'Slows enemies. Right click for a Frost Nova.',
        'market.mythoria_weapons.storm_blade': 'Storm Blade',
        'market.mythoria_weapons.storm_blade.desc': 'Right click to strike with lightning.',
    },
    'armory': {
        'item.mythoria_armory.dragon_helmet': 'Dragon Helmet',
        'item.mythoria_armory.dragon_chestplate': 'Dragon Chestplate',
        'item.mythoria_armory.dragon_leggings': 'Dragon Leggings',
        'item.mythoria_armory.dragon_boots': 'Dragon Boots',
        'item.mythoria_armory.set_bonus': 'Full set: fire immunity',
        'market.mythoria_armory.dragon_set': 'Dragon Armor (full set)',
        'market.mythoria_armory.dragon_set.desc': 'All four pieces. Full set: fire immunity.',
        'market.mythoria_armory.dragon_helmet': 'Dragon Helmet',
        'market.mythoria_armory.dragon_helmet.desc': 'Horned helmet with a burning gem.',
        'market.mythoria_armory.dragon_chestplate': 'Dragon Chestplate',
        'market.mythoria_armory.dragon_chestplate.desc': 'Scales, spiked shoulders and little wings.',
        'market.mythoria_armory.dragon_leggings': 'Dragon Leggings',
        'market.mythoria_armory.dragon_leggings.desc': 'Golden belt and armored knees.',
        'market.mythoria_armory.dragon_boots': 'Dragon Boots',
        'market.mythoria_armory.dragon_boots.desc': 'Clawed boots of a true dragon.',
    },
    'feast': {
        'item.mythoria_feast.moon_apple': 'Moon Apple',
        'item.mythoria_feast.moon_apple.tooltip': 'Night Vision and Absorption',
        'item.mythoria_feast.fairy_cake': 'Fairy Cake',
        'item.mythoria_feast.fairy_cake.tooltip': 'Jump Boost and Slow Falling',
        'item.mythoria_feast.dragon_drumstick': 'Dragon Drumstick',
        'item.mythoria_feast.dragon_drumstick.tooltip': 'Strength and Fire Resistance - griffins love it',
        'item.mythoria_feast.elven_mead': 'Elven Mead',
        'item.mythoria_feast.elven_mead.tooltip': 'Speed and Regeneration',
        'market.mythoria_feast.moon_apple': 'Moon Apple',
        'market.mythoria_feast.moon_apple.desc': 'See in the dark, with extra hearts.',
        'market.mythoria_feast.fairy_cake': 'Fairy Cake',
        'market.mythoria_feast.fairy_cake.desc': 'Jump high and fall like a feather.',
        'market.mythoria_feast.dragon_drumstick': 'Dragon Drumstick',
        'market.mythoria_feast.dragon_drumstick.desc': 'A hearty meal: strength and fire resistance.',
        'market.mythoria_feast.elven_mead': 'Elven Mead',
        'market.mythoria_feast.elven_mead.desc': 'A sweet drink: speed and regeneration.',
    },
}


def metadata():
    for source, (mod, name, description) in MODS.items():
        write(res(source) / 'assets' / mod / 'lang' / 'en_us.json', LANG[source])
        deps = [('forge', '[52,)'), ('minecraft', '[1.21.1,1.21.2)')]
        if source != 'core':
            deps.append(('mythoria', '[1.0,)'))
        toml = (f'modLoader="javafml"\nloaderVersion="[52,)"\nlicense="All Rights Reserved"\n\n'
                f'[[mods]]\nmodId="{mod}"\nversion="{VERSION}"\ndisplayName="{name}"\nauthors="@Adryh01"\n'
                f'logoFile="{mod}_logo.png"\ndescription="{description}"\n')
        for dep, rng in deps:
            toml += (f'\n[[dependencies.{mod}]]\nmodId="{dep}"\nmandatory=true\nversionRange="{rng}"\n'
                     f'ordering="{"AFTER" if dep == "mythoria" else "NONE"}"\nside="BOTH"\n')
        write(res(source) / 'META-INF' / 'mods.toml', toml)
        write(res(source) / 'pack.mcmeta', {'pack': {'pack_format': 34, 'description': name + ' - @Adryh01'}})
    logos = {'core': ITEMS['core']['crown'], 'creatures': ITEMS['creatures']['golem_core'], 'weapons': ITEMS['weapons']['phoenix_blade'],
             'armory': lambda: armour_icons()['dragon_helmet'], 'feast': ITEMS['feast']['fairy_cake']}
    for source, builder in logos.items():
        model, _ = builder()
        model.build()
        quads = model.quads(M=rot_matrix(0, 0, 30), T=np.zeros(3)) if source == 'weapons' else model.quads()
        render(quads, (160, 160), yaw=26, pitch=12, margin=.03, ss=3).save(res(source) / (MODS[source][0] + '_logo.png'))
    write(res('creatures') / 'data' / 'mythoria_creatures' / 'forge' / 'biome_modifier' / 'golden_griffin.json', {
        'type': 'forge:add_spawns',
        'biomes': ['minecraft:plains', 'minecraft:sunflower_plains', 'minecraft:meadow', 'minecraft:savanna',
                   'minecraft:savanna_plateau', 'minecraft:windswept_hills', 'minecraft:windswept_savanna'],
        'spawners': {'type': 'mythoria_creatures:golden_griffin', 'weight': 6, 'minCount': 1, 'maxCount': 2}})
    write(res('creatures') / 'data' / 'mythoria_creatures' / 'forge' / 'biome_modifier' / 'crystal_golem.json', {
        'type': 'forge:add_spawns',
        'biomes': '#minecraft:is_overworld',
        'spawners': {'type': 'mythoria_creatures:crystal_golem', 'weight': 3, 'minCount': 1, 'maxCount': 1}})


if __name__ == '__main__':
    items()
    entities()
    metadata()
    print('assets written')
