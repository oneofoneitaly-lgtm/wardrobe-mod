#!/usr/bin/env python3
"""Writes the textures, item models, texts and metadata of Doodle Pals."""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/doodlepals'
MOD = 'doodlepals'
VERSION = '1.0.0'


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(data if isinstance(data, str) else json.dumps(data, indent=1) + '\n')


def sprite(rows, colours):
    img = Image.new('RGBA', (len(rows[0]), len(rows)))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.':
                img.putpixel((x, y), tuple(int(colours[ch][i:i + 2], 16) for i in (0, 2, 4)) + (255,))
    return img


BOOK = [
    "................",
    "..kkkkkkkkkkk.y.",
    "..kPPPPPPPPPpkyy",
    "..kPwwwwwwwPpyyk",
    "..kPwrrwwwwPyyk.",
    "..kPwrrrwwwyyk..",
    "..kPwwrrrwyydk..",
    "..kPwgwwwyywpk..",
    "..kPwggwoywwpk..",
    "..kPwgggwwwwpk..",
    "..kPwwwwbbwwpk..",
    "..kPwwwbbbbwpk..",
    "..kPPPPPPPPPpk..",
    "..kppppppppppk..",
    "...kkkkkkkkkk...",
    "................",
]
COLOURS = {'k': '2a1f45', 'P': '7d5ad6', 'p': '55399e', 'w': 'f6f1e3', 'r': 'e8433c', 'g': '52c04a',
           'b': '3f70e4', 'y': 'ffd93b', 'o': 'f7902e', 'd': '3a2b18'}

LANG = {
    'item.doodlepals.sketchbook': 'Sketchbook',
    'item.doodlepals.sketchbook.tooltip': 'Right click: draw a creature and bring it to life',
    'item.doodlepals.doodle_page': 'Doodle Page',
    'item.doodlepals.doodle_page.named': '%s (Doodle Page)',
    'item.doodlepals.doodle_page.tooltip': 'Use it on the ground to wake your pal',
    'entity.doodlepals.pal': 'Doodle Pal',
    'key.doodlepals.draw': 'Open Sketchbook',
    'key.categories.doodlepals': 'Doodle Pals',
    'doodlepals.sit': '%s waits here',
    'doodlepals.follow': '%s follows you',
    'doodlepals.called': 'Pals called back: %s',
    'doodlepals.too_small': 'Draw at least %s pixels',
    'doodlepals.too_many': 'You already have %s pals. Shift + right click one to store it.',
    'doodlepals.no_ink': 'You need an Ink Sac to bring a drawing to life',
    'doodlepals.sketch.title': 'Doodle Pals  -  Sketchbook',
    'doodlepals.sketch.cost': 'Costs 1 Ink Sac',
    'doodlepals.sketch.free': 'Creative: free',
    'doodlepals.sketch.name': 'Name your pal...',
    'doodlepals.sketch.empty': 'Blank page',
    'doodlepals.sketch.hint': 'Paint a creature, or press Idea. Colours give powers!',
    'doodlepals.sketch.nopower': 'No power: cover more of it with one colour.',
    'doodlepals.sketch.body': 'Health %s   Attack %s',
    'doodlepals.sketch.create': 'Bring to Life!',
    'doodlepals.tool.brush': 'Brush', 'doodlepals.tool.brush.info': 'Paint. Right button erases.',
    'doodlepals.tool.erase': 'Erase', 'doodlepals.tool.erase.info': 'Rub out pixels.',
    'doodlepals.tool.fill': 'Fill', 'doodlepals.tool.fill.info': 'Fill a whole area with the colour.',
    'doodlepals.tool.eyes': 'Eyes', 'doodlepals.tool.eyes.info': 'Place up to 4 eyes on painted pixels. Click an eye to remove it.',
    'doodlepals.tool.size': 'Size %s', 'doodlepals.tool.size.info': 'Brush size.',
    'doodlepals.tool.mirror': 'Mirror', 'doodlepals.tool.mirror.info': 'Paint both sides at once.',
    'doodlepals.tool.undo': 'Undo', 'doodlepals.tool.undo.info': 'Undo (Ctrl+Z).',
    'doodlepals.tool.clear': 'Clear', 'doodlepals.tool.clear.info': 'Start over.',
    'doodlepals.tool.idea': 'Idea', 'doodlepals.tool.idea.info': 'Load a sample drawing to start from.',
}
POWERS = {
    'fire': ('Fire', 'Hits set enemies on fire. The pal cannot burn.'),
    'brave': ('Brave', 'The pal hits much harder.'),
    'spark': ('Spark', 'Gives you speed.'),
    'nature': ('Nature', 'Makes nearby crops grow.'),
    'frost': ('Frost', 'Hits slow and freeze enemies.'),
    'tide': ('Tide', 'Breathe and swim fast under water. The pal cannot drown.'),
    'magic': ('Magic', 'The pal shoots magic bolts from afar.'),
    'heart': ('Heart', 'Heals you when you are badly hurt.'),
    'air': ('Air', 'Slows your falls. The pal is light and quick.'),
    'shadow': ('Shadow', 'Sneak to turn invisible. Hits weaken enemies.'),
    'rock': ('Rock', 'You take less damage. The pal has more health.'),
    'iron': ('Iron', 'You mine faster. The pal wears armour.'),
}
for key, (name, info) in POWERS.items():
    LANG[f'doodlepals.power.{key}'] = name
    LANG[f'doodlepals.power.{key}.info'] = info


def main():
    write(ASSETS / 'lang/en_us.json', LANG)
    (ASSETS / 'textures/entity').mkdir(parents=True, exist_ok=True)
    Image.new('RGBA', (16, 16), (255, 255, 255, 255)).save(ASSETS / 'textures/entity/white.png')
    (ASSETS / 'textures/item').mkdir(parents=True, exist_ok=True)
    book = sprite(BOOK, COLOURS)
    book.save(ASSETS / 'textures/item/sketchbook.png')
    write(ASSETS / 'models/item/sketchbook.json', {'parent': 'item/generated', 'textures': {'layer0': f'{MOD}:item/sketchbook'}})
    write(ASSETS / 'models/item/doodle_page.json', {'parent': 'builtin/entity', 'gui_light': 'front', 'display': {
        'ground': {'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
        'fixed': {'rotation': [0, 180, 0], 'scale': [0.9, 0.9, 0.9]},
        'thirdperson_righthand': {'translation': [0, 3, 1], 'scale': [0.55, 0.55, 0.55]},
        'firstperson_righthand': {'rotation': [0, -25, 0], 'translation': [1.5, 3, 0], 'scale': [0.32, 0.32, 0.32]},
    }})
    write(RES / 'data/doodlepals/recipe/sketchbook.json', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': [{'item': 'minecraft:book'}, {'item': 'minecraft:ink_sac'}, {'item': 'minecraft:feather'}],
        'result': {'id': f'{MOD}:sketchbook', 'count': 1}})
    description = 'Draw a creature, bring it to life: its colours become its powers.'
    write(RES / 'META-INF/mods.toml', (
        'modLoader="javafml"\nloaderVersion="[52,)"\nlicense="All Rights Reserved"\n\n'
        f'[[mods]]\nmodId="{MOD}"\nversion="{VERSION}"\ndisplayName="Doodle Pals"\nauthors="@Adryh01"\n'
        f'logoFile="doodlepals_logo.png"\ndescription="{description}"\n\n'
        f'[[dependencies.{MOD}]]\nmodId="forge"\nmandatory=true\nversionRange="[52,)"\nordering="NONE"\nside="BOTH"\n\n'
        f'[[dependencies.{MOD}]]\nmodId="minecraft"\nmandatory=true\nversionRange="[1.21.1,1.21.2)"\nordering="NONE"\nside="BOTH"\n'))
    write(RES / 'pack.mcmeta', {'pack': {'pack_format': 34, 'description': 'Doodle Pals - @Adryh01'}})
    book.resize((160, 160), Image.NEAREST).save(RES / 'doodlepals_logo.png')
    print('assets written')


if __name__ == '__main__':
    main()
