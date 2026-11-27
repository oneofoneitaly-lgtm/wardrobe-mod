#!/usr/bin/env python3
"""Writes the texts, logo and metadata of Inventory Styles."""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / 'src/main/resources'
MOD = 'invstyles'
VERSION = '1.0.0'
DESCRIPTION = 'New looks for every inventory: Dark Glass, Fantasy, Neon and Pastel. Press I.'

LANG = {
    'invstyles.title': 'Inventory Styles',
    'invstyles.button': 'Style',
    'invstyles.colour': 'Colour',
    'invstyles.style.vanilla': 'Vanilla',
    'invstyles.style.glass': 'Dark Glass',
    'invstyles.style.fantasy': 'Fantasy',
    'invstyles.style.neon': 'Neon',
    'invstyles.style.pastel': 'Pastel',
    'invstyles.group.all': 'All inventories',
    'invstyles.group.inventory': 'Inventory',
    'invstyles.group.creative': 'Creative',
    'invstyles.group.chests': 'Chests',
    'invstyles.group.crafting': 'Crafting',
    'invstyles.group.furnaces': 'Furnaces',
    'invstyles.group.magic': 'Magic',
    'invstyles.group.smithing': 'Smithing',
    'invstyles.group.trading': 'Trading',
    'invstyles.group.hotbar': 'Hotbar',
    'key.invstyles.open': 'Open Inventory Styles',
    'key.categories.invstyles': 'Inventory Styles',
}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(data if isinstance(data, str) else json.dumps(data, indent=1) + '\n')


def logo():
    """Four little slots, one per style."""
    img = Image.new('RGBA', (32, 32), (22, 20, 36, 255))
    draw = ImageDraw.Draw(img)
    for x, y, edge, fill in ((3, 3, '9d6bff', '1b1a2b'), (17, 3, 'd9a83c', 'e3cb98'),
                             (3, 17, '35dcff', '07070e'), (17, 17, 'ff8fc8', 'fdf0fa')):
        draw.rectangle([x, y, x + 11, y + 11], fill='#' + fill, outline='#' + edge)
    return img.resize((160, 160), Image.NEAREST)


def main():
    write(RES / f'assets/{MOD}/lang/en_us.json', LANG)
    write(RES / 'META-INF/mods.toml', (
        'modLoader="javafml"\nloaderVersion="[52,)"\nlicense="All Rights Reserved"\nclientSideOnly=true\n\n'
        f'[[mods]]\nmodId="{MOD}"\nversion="{VERSION}"\ndisplayName="Inventory Styles"\nauthors="@Adryh01"\n'
        f'logoFile="{MOD}_logo.png"\ndescription="{DESCRIPTION}"\n\n'
        f'[[dependencies.{MOD}]]\nmodId="forge"\nmandatory=true\nversionRange="[52,)"\nordering="NONE"\nside="CLIENT"\n\n'
        f'[[dependencies.{MOD}]]\nmodId="minecraft"\nmandatory=true\nversionRange="[1.21.1,1.21.2)"\nordering="NONE"\nside="CLIENT"\n'))
    write(RES / 'pack.mcmeta', {'pack': {'pack_format': 34, 'description': 'Inventory Styles - @Adryh01'}})
    logo().save(RES / f'{MOD}_logo.png')
    print('assets written')


if __name__ == '__main__':
    main()
