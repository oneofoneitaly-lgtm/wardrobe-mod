import os
from PIL import ImageDraw, ImageFont
from design_items import *
from design_entities import *
OUT = os.environ.get('MYTHORIA_PREVIEWS', 'previews'); os.makedirs(OUT, exist_ok=True)
FB = '/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf'; FR = '/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf'

def backdrop(w, h):
    yy, xx = np.mgrid[0:h, 0:w]
    r = np.sqrt(((xx - w / 2) / (w * .72)) ** 2 + ((yy - h * .45) / (h * .72)) ** 2)
    t = np.clip(r, 0, 1)[..., None]
    c = np.array([62, 50, 92]) * (1 - t) + np.array([14, 11, 24]) * t
    c += (hash3(np.stack([xx, yy, xx * 0], -1)) - .5)[..., None] * 5
    im = Image.fromarray(np.clip(c, 0, 255).astype(np.uint8), 'RGB').convert('RGBA')
    return im

def shadow(im, cx, cy, rw, rh, a=120):
    s = Image.new('RGBA', im.size, (0, 0, 0, 0))
    ImageDraw.Draw(s).ellipse((cx - rw, cy - rh, cx + rw, cy + rh), fill=(0, 0, 0, a))
    im.alpha_composite(s.filter(ImageFilter.GaussianBlur(rh * .55)))

def text(im, xy, s, size, bold=True, col=(255, 214, 106), anchor='mm'):
    d = ImageDraw.Draw(im); f = ImageFont.truetype(FB if bold else FR, size)
    d.text((xy[0] + 2, xy[1] + 3), s, font=f, fill=(0, 0, 0, 200), anchor=anchor)
    d.text(xy, s, font=f, fill=col, anchor=anchor)

def trimmed(img):
    bb = img.getchannel('A').point(lambda v: 255 if v > 40 else 0).getbbox()
    return img.crop(img.getbbox()), bb

def place(im, quads, box, mirror=False, floor=True, **k):
    x, y, w, h = box
    r = render(quads, (w, h), mirror=mirror, **k)
    if floor:
        bb = r.getchannel('A').point(lambda v: 255 if v > 200 else 0).getbbox()
        if bb:
            shadow(im, x + (bb[0] + bb[2]) // 2, y + bb[3] - 6, int((bb[2] - bb[0]) * .46), max(10, int((bb[3] - bb[1]) * .045)))
    im.alpha_composite(r, (x, y))

def title(im, s, sub):
    w = im.width
    text(im, (w // 2, 52), s, 46)
    text(im, (w // 2, 100), sub, 22, False, (214, 204, 240))
    ImageDraw.Draw(im).line((w // 2 - 260, 126, w // 2 + 260, 126), fill=(160, 120, 40), width=2)

def creatures():
    g = golden_griffin().build(); m = crystal_golem().build()
    im = backdrop(1600, 1000); title(im, 'MYTHORIA  -  Creatures', 'Golden Griffin and Crystal Golem')
    place(im, g.quads(GRIFFIN_SHOW), (0, 150, 920, 700), True, yaw=34, pitch=11, margin=.03)
    place(im, m.quads(), (900, 130, 680, 740), True, yaw=-24, pitch=7, margin=.04)
    text(im, (460, 890), 'Golden Griffin', 34); text(im, (460, 935), 'tame it, ride it, fly it', 21, False, (214, 204, 240))
    text(im, (1240, 890), 'Crystal Golem', 34); text(im, (1240, 935), 'boss  -  drops Crowns', 21, False, (214, 204, 240))
    im.convert('RGB').save(OUT + '/1_creatures.png')
    im = backdrop(1600, 760); title(im, 'Creatures  -  all sides', 'front, side, back')
    for i, yaw in enumerate((0, 90, 150)):
        place(im, g.quads(GRIFFIN_SHOW), (i * 533, 140, 533, 310), True, yaw=yaw, pitch=8, margin=.03)
        place(im, m.quads(), (i * 533, 440, 533, 310), True, yaw=yaw, pitch=8, margin=.03)
    im.convert('RGB').save(OUT + '/2_creatures_sides.png')

def weapons():
    im = backdrop(1600, 960); title(im, 'MYTHORIA  -  3D Swords', 'each one has its own power')
    info = [('phoenix_blade', 'Phoenix Blade', 'burns enemies, throws fire'), ('frost_fang', 'Frost Fang', 'freezes and slows'), ('storm_blade', 'Storm Blade', 'calls lightning')]
    for i, (n, label, sub) in enumerate(info):
        m, _ = ITEMS['weapons'][n](); m.build()
        R = rot_matrix(0, 0, 24)
        place(im, m.quads(M=R, T=np.zeros(3)), (i * 533 + 6, 140, 520, 710), floor=False, yaw=24, pitch=5, margin=.025, ss=3)
        text(im, (i * 533 + 266, 878), label, 32); text(im, (i * 533 + 266, 920), sub, 20, False, (214, 204, 240))
    im.convert('RGB').save(OUT + '/3_swords.png')

def armour():
    parts = dragon_armour(); q = mannequin().build().quads()
    for m in parts.values():
        m.build(); q += m.quads()
    im = backdrop(1600, 960); title(im, 'MYTHORIA  -  Dragon Armor', 'horns, wings, claws  -  worn 3D model')
    place(im, q, (0, 140, 640, 720), True, yaw=28, pitch=8, margin=.03)
    place(im, q, (630, 190, 460, 640), True, yaw=0, pitch=4, margin=.03)
    place(im, q, (1070, 190, 530, 640), True, yaw=152, pitch=10, margin=.03)
    icons = armour_icons()
    for i, (n, (m, _)) in enumerate(icons.items()):
        m.build(); place(im, m.quads(), (480 + i * 160, 820, 150, 120), floor=False, yaw=26, pitch=14, margin=.02, ss=3)
    text(im, (250, 880), 'Inventory icons:', 22, False, (214, 204, 240))
    im.convert('RGB').save(OUT + '/4_dragon_armor.png')

def food():
    im = backdrop(1600, 1000); title(im, 'MYTHORIA  -  Feast, Coins and Eggs', 'all items are real 3D models')
    rows = [[('feast', 'moon_apple', 'Moon Apple', 'night vision'), ('feast', 'fairy_cake', 'Fairy Cake', 'jump + slow fall'),
             ('feast', 'dragon_drumstick', 'Dragon Drumstick', 'strength + fire immunity'), ('feast', 'elven_mead', 'Elven Mead', 'speed + regeneration')],
            [('core', 'crown', 'Crown', 'the market coin'), ('creatures', 'griffin_egg', 'Griffin Egg', 'hatches your griffin'), ('creatures', 'golem_core', 'Golem Core', 'summons the boss')]]
    for r, row in enumerate(rows):
        x0 = (1600 - len(row) * 400) // 2
        for i, (g, n, label, sub) in enumerate(row):
            m, _ = ITEMS[g][n](); m.build()
            y = 150 + r * 420
            place(im, m.quads(), (x0 + i * 400 + 40, y, 320, 320), yaw=28, pitch=14, margin=.04, ss=3)
            text(im, (x0 + i * 400 + 200, y + 345), label, 27); text(im, (x0 + i * 400 + 200, y + 380), sub, 18, False, (214, 204, 240))
    im.convert('RGB').save(OUT + '/5_food_coins_eggs.png')

import sys
for fn in (sys.argv[1:] or ['creatures', 'weapons', 'armour', 'food']):
    globals()[fn](); print('ok', fn, flush=True)
