"""English texts of the second wave of content."""
LANG2 = {'core': {'key.mythoria.mount_ability': 'Mount ability'}, 'creatures': {}, 'weapons': {}, 'armory': {}, 'feast': {}}


def goods(source, mod, key, name, tooltip=None, desc=None, kind='item'):
    LANG2[source][f'{kind}.{mod}.{key}'] = name
    if tooltip:
        LANG2[source][f'{kind}.{mod}.{key}.tooltip'] = tooltip
    if desc:
        LANG2[source][f'market.{mod}.{key}'] = name
        LANG2[source][f'market.{mod}.{key}.desc'] = desc


C = 'mythoria_creatures'
goods('creatures', C, 'drake_egg', 'Drake Egg', 'Use it on the ground: your own Ember Drake hatches')
goods('creatures', C, 'spirit_charm', 'Spirit Charm', 'Use it on the ground: a Spirit Fox answers')
goods('creatures', C, 'lich_phylactery', 'Lich Phylactery', 'Use it on the ground to call the boss. Be ready!')
LANG2['creatures'].update({
    f'entity.{C}.ember_drake': 'Ember Drake', f'entity.{C}.spirit_fox': 'Spirit Fox', f'entity.{C}.shadow_lich': 'Shadow Lich',
    f'market.{C}.drake_egg': 'Ember Drake', f'market.{C}.drake_egg.desc': 'A dragon mount. Flies fast and breathes fire (R).',
    f'market.{C}.spirit_charm': 'Spirit Fox', f'market.{C}.spirit_charm.desc': 'Fetches drops and lights up the dark for you.',
    f'market.{C}.lich_phylactery': 'Shadow Lich', f'market.{C}.lich_phylactery.desc': 'A boss that casts shadows and raises skeletons.',
    'mythoria.drake.wild': 'Offer raw or cooked meat to tame this drake',
    'mythoria.drake.tamed': 'The drake bows its head to you!',
    'mythoria.drake.sit': 'Your drake waits here',
    'mythoria.drake.follow': 'Your drake follows you',
    'mythoria.drake.ride': 'Space: climb  -  look down + W: dive  -  %s: breathe fire  -  Shift: dismount',
    'mythoria.fox.wild': 'Offer sweet berries or a Fairy Cake to befriend this fox',
    'mythoria.fox.tamed': 'The Spirit Fox is your friend now!',
    'mythoria.fox.sit': 'Your fox waits here',
    'mythoria.fox.follow': 'Your fox follows you',
    'mythoria.boss.peaceful': 'Bosses cannot appear in Peaceful difficulty',
})

W = 'mythoria_weapons'
for key, name, hit, use, desc in (
        ('void_reaver', 'Void Reaver', 'Hit: steals life', 'Right click: Shadow Step (dash through enemies)', 'Steals life. Right click to blink forward.'),
        ('elven_bow', 'Elven Longbow', 'Full draw: fires three arrows', 'Needs arrows', 'A full draw fires three arrows at once.'),
        ('arcane_staff', 'Arcane Staff', 'Right click: Arcane Bolt', 'Strikes far away and splashes nearby foes', 'Shoots arcane bolts. No ammo needed.'),
        ('life_staff', 'Life Staff', 'Right click: Bloom', 'Heals you, friends and pets - grows crops', 'Heals allies and makes crops grow.')):
    goods('weapons', W, key, name, desc=desc)
    LANG2['weapons'][f'item.{W}.{key}.hit'] = hit
    LANG2['weapons'][f'item.{W}.{key}.use'] = use

A = 'mythoria_armory'
for style, title, bonus, desc, pieces in (
        ('elven', 'Elven', 'Full set: Speed and Jump Boost', 'Leaves and antlers. Full set: speed and jump.',
         ('Antlered helmet of the forest.', 'Leaf mantle with a long cape.', 'Woven leaves with an emerald belt.', 'Light boots with leaf wings.')),
        ('frost', 'Frost', 'Full set: no freezing, breathe underwater', 'Ice crystals. Full set: no freezing, water breathing.',
         ('A crown of ice spires.', 'Crystal shoulders and a winter cape.', 'Frozen plates and knee spikes.', 'Walk on powder snow.'))):
    LANG2['armory'][f'market.{A}.{style}_set'] = f'{title} Armor (full set)'
    LANG2['armory'][f'market.{A}.{style}_set.desc'] = desc
    LANG2['armory'][f'item.{A}.{style}_set_bonus'] = bonus
    for slot, d in zip(('helmet', 'chestplate', 'leggings', 'boots'), pieces):
        goods('armory', A, f'{style}_{slot}', f'{title} {slot.capitalize()}', desc=d)
LANG2['armory'][f'item.{A}.dragon_set_bonus'] = 'Full set: fire immunity'

F = 'mythoria_feast'
for key, name, tip, desc in (
        ('star_fruit', 'Star Fruit', 'Haste and Luck', 'Mine faster and find better loot.'),
        ('frost_sorbet', 'Frost Sorbet', "Water Breathing and Dolphin's Grace", 'Swim and breathe like a fish.'),
        ('honey_pie', 'Honey Pie', 'Resistance and Health Boost', 'Tougher, with extra hearts.'),
        ('mana_potion', 'Mana Potion', 'Invisibility and Speed', 'Vanish and run.')):
    goods('feast', F, key, name, tip, desc)
