"""Elven Armor and Frost Armor: worn 3d models and inventory icons."""
from design_entities import *
from design_items import mirror_x, compact

LEAF = ramp('#06281a', '#0b4429', '#14653a', '#22894a', '#3fb05c', '#74d674', '#b6f29a')
MOSS = ramp('#10201a', '#1a3428', '#264b36', '#366448', '#4d805a')
WOOD = ramp('#24150a', '#412812', '#62401d', '#865b2b', '#aa7b3f', '#cc9e5c')
PLATE = ramp('#1c3a66', '#2c5a92', '#4682bc', '#70addb', '#a8d6f2', '#dbf2ff', '#ffffff')
NAVY = ramp('#081230', '#0f1f4c', '#18306c', '#244590', '#3860b4')
SNOW = ramp('#9fb6cc', '#c3d5e6', '#e0ecf6', '#f4f9ff', '#ffffff')

STYLES = {
    'elven': {
        'base': lambda: scales(LEAF, 6, 5, seed=161),
        'under': lambda: noise(MOSS, 1.6, .35),
        'metal': SILVER,
        'accent': lambda: noise(WOOD, 1.0, .5, 1.2),
        'gem': lambda: glowing(EMERALD[1:], .5),
        'cape': (LEAF, MOSS),
        'eye': '#8dffab',
    },
    'frost': {
        'base': lambda: metal(PLATE, grad=(1, -4, 34), seed=171, sheen=.12),
        'under': lambda: noise(NAVY, 1.8, .3),
        'metal': SILVER,
        'accent': lambda: crystal(ICE[2:], 1.8, seed=173, glow=.45),
        'gem': lambda: glowing(FROSTGLOW[:4], .5),
        'cape': (NAVY, ICE),
        'eye': '#a8f4ff',
    },
}

nothing = lambda c: np.ones((c.H, c.W), bool)


def _hole(trim_color):
    def hole(ctx, img, glow):
        x, y = ctx.x, ctx.y
        m = ((np.abs(x) < 3.2) & (y < 29.2) & (y > 24.0)) | ((np.abs(x) < 1.6) & (y < 24.6))
        rim = np.zeros_like(m)
        rim[1:, :] |= m[:-1, :]; rim[:-1, :] |= m[1:, :]; rim[:, 1:] |= m[:, :-1]; rim[:, :-1] |= m[:, 1:]
        rim &= ~m
        img[rim] = np.append(rgb(trim_color), 255)
        img[m] = 0; glow[m] = 0
    return hole


def worn(style):
    st = STYLES[style]
    elf = style == 'elven'
    base, under, acc, gem = st['base'](), st['under'](), st['accent'](), st['gem']()
    steel = metal(st['metal'])
    rim = trim(st['metal'][1:])
    ob = cut(nothing)
    B = lambda x0, x1, y0, y1, z0, z1, m, d=None, g=0.0: Box((min(x0, x1), y0, min(z0, z1)), (max(x0, x1), y1, max(z0, z1)), m, d, g)

    # ---------------------------------------------------------- helmet
    head_kids = []
    if elf:
        for s, n in ((-1, 'r'), (1, 'l')):
            head_kids.append(Part(n + '_antler', (s * 4, 31, 0), [B(s * 4, s * 6, 30, 32, -.5, .5, acc)], [
                Part(n + '_antler_up', (s * 5.5, 31.5, 0), [B(s * 5, s * 6, 31, 38, -.5, .5, acc), B(s * 5, s * 6, 37.5, 39.5, -.5, .5, gem)], [
                    Part(n + '_twig_a', (s * 5.5, 34, 0), [B(s * 5, s * 6, 34, 38, -.5, .5, acc)], rot=(0, 0, -s * 40)),
                    Part(n + '_twig_b', (s * 5.5, 32.5, 0), [B(s * 5, s * 6, 32.5, 35.5, -.5, .5, acc)], rot=(40, 0, 0)),
                ], rot=(-12, 0, -s * 14))]))
            leaf_cut = cut(lambda c, s=s: np.abs(c.y - (27 + (np.abs(c.z) * .45))) > 2.2 - np.abs(c.z - 1.5) * .45)
            head_kids.append(Part(n + '_ear_leaf', (s * 5, 27, -1), [B(s * 5.1, s * 6.1, 24, 31, -2, 6, base, {'*': [leaf_cut]})], rot=(0, s * 18, 0)))
        head_kids.append(Part('circlet', (0, 30, -5), [B(-5.2, 5.2, 29.4, 30.4, -5.4, -4.4, steel), B(-1, 1, 29.4, 32.4, -5.8, -4.8, gem), B(-3, -2, 30.4, 31.4, -5.4, -4.4, steel), B(2, 3, 30.4, 31.4, -5.4, -4.4, steel)]))
    else:
        for i, (x, h, tilt) in enumerate(((0, 8, 0), (-3, 5, 14), (3, 5, -14))):
            head_kids.append(Part('spire_%d' % i, (x, 32, -3), [B(x - 1, x + 1, 31.5, 31.5 + h, -4.5, -2.5, acc), B(x - .5, x + .5, 31.5 + h, 33.5 + h, -4, -3, acc)], rot=(-8, 0, tilt)))
        for s, n in ((-1, 'r'), (1, 'l')):
            head_kids.append(Part(n + '_wing', (s * 5, 29, 0), [B(s * 5, s * 6, 28, 31, -2, 2, steel), B(s * 5.1, s * 6.1, 30, 35, 1, 3, acc), B(s * 5.1, s * 6.1, 29, 33, 3.5, 5.5, acc)], rot=(14, s * 16, 0)))
        head_kids.append(Part('brow', (0, 30, -5), [B(-5, 5, 29.6, 30.8, -5.8, -4.8, steel), B(-1, 1, 29.2, 31.2, -6.4, -5.4, gem)]))
    helmet = [Part('head', (0, 24, 0), [
        Box((-4, 24, -4), (4, 32, 4), base, {'front': [_hole('#cfdcea'), trim(st['metal'][1:], 1, 't')], 'bottom': [ob],
            'left': [trim(st['metal'][1:], 1, 'b')], 'right': [trim(st['metal'][1:], 1, 'b')], 'back': [trim(st['metal'][1:], 1, 'b')]}, inflate=1.0),
    ], head_kids)]

    # ---------------------------------------------------------- chestplate
    c0, c1 = st['cape']

    def cape_mat(c):
        t = np.clip((22 - c.y) / 18.0, 0, 1)
        if elf:
            col = np.floor((c.x + 20) / 3.0); row = np.floor((c.y + (np.mod(col, 2) * 2)) / 4.0)
            r = hash3(np.stack([col, row, col * 0], -1), 181)
            fy = np.mod(c.y + np.mod(col, 2) * 2, 4.0) / 4.0
            v = .2 + r * .45 + fy * .3
            v = np.where(np.abs(np.mod(c.x + 20, 3.0) - 1.5) < .35, v + .2, v)
            return paint(c, v, c0)
        v = .25 + (1 - t) * .15 + vnoise(c.P * np.array([1, .35, 1]), 1.6, 183) * .5
        img = paint(c, v, c0)
        low = c.y < 9.5 + vnoise(c.P, 1.2, 185) * 4
        img[low] = paint(c, .35 + vnoise(c.P, 1.4, 187) * .6, c1)[low]
        return img

    def cape_edge(c):
        k = np.abs(np.mod(c.x + 20, 4.0) / 4.0 - .5) * 2
        return (c.y < 3.5 + k * 3.5) | (np.abs(c.x) > 4.5 + (22 - c.y) * .2)

    chest_kids = [
        Part('breastplate', (0, 20, -3), [B(-4, 4, 19, 23, -3.8, -2.8, steel if not elf else acc, {'front': [rim]}), B(-1.5, 1.5, 16.5, 19, -3.6, -2.8, steel), B(-.5, .5, 13, 16.5, -3.4, -2.8, steel)]),
        Part('heart_gem', (0, 21, -4), [B(-1.5, 1.5, 19.5, 22.5, -4.6, -3.6, gem)], rot=(0, 0, 45)),
        Part('cape', (0, 23, 3.4), [B(-8, 8, 3, 23, 3.4, 3.4, Mat(cape_mat), {'*': [cut(cape_edge)]})], rot=(-11, 0, 0)),
        Part('cape_pin', (0, 23, 3.2), [B(-4.5, -2.5, 21.8, 23.8, 2.6, 3.8, gem), B(2.5, 4.5, 21.8, 23.8, 2.6, 3.8, gem)]),
    ]
    if not elf:
        fur_m = fur(SNOW, seed=189)
        chest_kids.append(Part('collar_fur', (0, 23, 0), [B(-5.5, 5.5, 22.6, 25, 1.5, 4.5, fur_m), B(-6, -3, 22.4, 24.6, -3.4, 2, fur_m), B(3, 6, 22.4, 24.6, -3.4, 2, fur_m)]))

    def arm(s):
        n = 'right' if s < 0 else 'left'
        pk = []
        if elf:
            for i, (dy, zr) in enumerate(((0, -16), (-2, -30), (1.6, -2))):
                w = 3.6 - i * .4
                pk.append(Part(n + '_leaf_%d' % i, (s * 6, 23.5 + dy, 0), [
                    B(s * 5, s * 10, 23 + dy, 24 + dy, -w, w, base), B(s * 10, s * 12, 23 + dy, 24 + dy, -w + 1.5, w - 1.5, base),
                    B(s * 12, s * 13, 23 + dy, 24 + dy, -.5, .5, base),
                ], rot=(0, 0, s * zr * .7)))
            pk.append(Part(n + '_bud', (s * 7, 25, 0), [B(s * 5.8, s * 7.8, 24.6, 26.6, -1, 1, gem)], rot=(0, 45, 0)))
        else:
            pk.append(Part(n + '_shoulder', (s * 7, 23, 0), [B(s * 4.5, s * 11, 21.5, 25.5, -3.8, 3.8, base, {'*': [trim(st['metal'][1:], 1, 'b')]})], [
                Part(n + '_ice_a', (s * 8, 25, 0), [B(s * 7, s * 9.5, 24.5, 30.5, -1.2, 1.2, acc), B(s * 7.6, s * 8.9, 30.5, 33, -.6, .6, acc)], rot=(0, 25, -s * 20)),
                Part(n + '_ice_b', (s * 10, 24.5, 1), [B(s * 9, s * 11, 24, 28, 0, 2, acc)], rot=(-22, 0, -s * 42)),
                Part(n + '_ice_c', (s * 10, 24, -2), [B(s * 9.5, s * 11, 23.5, 26.5, -2.8, -1.3, acc)], rot=(18, 0, -s * 55)),
            ], rot=(0, 0, -s * 8)))
        pk.append(Part(n + '_bracer', (s * 6, 15, 0), [B(s * 3, s * 9, 13.4, 17.6, -3, 3, acc if elf else steel, {'*': [rim]}), B(s * 8.8, s * 9.8, 14.4, 16.6, -1, 1, gem)]))
        return Part(n + '_arm', (s * 5, 22, 0), [B(s * 4, s * 8, 12, 24, -2, 2, under if elf else base, {'*': []}, .9)], pk)

    chest_body = Part('body', (0, 24, 0), [Box((-4, 12, -2), (4, 24, 2), under if elf else base, {'front': [trim(st['metal'][1:], 1, 't')], 'top': [ob]}, inflate=1.0)], chest_kids)

    # ---------------------------------------------------------- leggings
    tab_cut = cut(lambda c: c.y < 6.2 + np.abs(c.x) * 1.1)
    legs_body = Part('body', (0, 24, 0), [Box((-4, 12, -2), (4, 15, 2), acc if elf else steel, {'*': [rim], 'top': [ob], 'bottom': [ob]}, inflate=.7)], [
        Part('belt_gem', (0, 13.5, -3), [B(-1, 1, 12.5, 14.5, -3.6, -2.6, gem)], rot=(0, 0, 45)),
        Part('tabard', (0, 12.5, -2.8), [B(-2.5, 2.5, 5.5, 12.5, -3.2, -2.2, base if elf else under, {'*': [tab_cut]})], rot=(8, 0, 0)),
        Part('back_flap', (0, 12.5, 2.6), [B(-3.5, 3.5, 6, 12.5, 2.2, 3.2, base if elf else under, {'*': [tab_cut]})], rot=(-9, 0, 0)),
    ])
    low = lambda lim: cut(lambda c: c.y < lim)

    def leg(s):
        n = 'right' if s < 0 else 'left'
        x = s * 1.9
        knee = [Part(n + '_knee', (x, 6, -3), [B(x - 2, x + 2, 4.5, 7.5, -3.4, -2.4, acc if elf else steel, {'*': [rim]}), B(x - 1, x + 1, 5, 7, -3.9, -3.4, gem)])]
        if not elf:
            knee.append(Part(n + '_knee_ice', (x, 7, -3), [B(x - .5, x + .5, 7.2, 10, -3.6, -2.6, acc)], rot=(24, 0, 0)))
        return Part(n + '_leg', (x, 12, 0), [Box((x - 2, 0, -2), (x + 2, 12, 2), under if elf else base,
                    {'bottom': [ob], 'top': [ob], 'front': [low(3.0)], 'back': [low(3.0)], 'left': [low(3.0)], 'right': [low(3.0)]}, .55)], knee)

    # ---------------------------------------------------------- boots
    def boot(s):
        n = 'right' if s < 0 else 'left'
        x = s * 1.9
        high = cut(lambda c: c.y > 6.0)
        kids = [
            Part(n + '_boot_trim', (x, 6, 0), [B(x - 3, x + 3, 5.2, 6.4, -3, 3, steel if not elf else acc)]),
            Part(n + '_toes', (x, 1, -3), [B(x - 2.5, x + 2.5, -.9, 1.6, -4.8, -2.8, acc if elf else base, {'front': [rim]}), B(x - 1, x + 1, -.9, .6, -6.4, -4.8, acc)]),
        ]
        if elf:
            wc = cut(lambda c: np.abs((c.y - 3) - (c.z * .55)) > 2.4 - np.abs(c.z - 2.5) * .5)
            kids.append(Part(n + '_foot_leaf', (x + s * 3, 4, 0), [B(x + s * 2.8, x + s * 3.8, 1, 9, -1, 6, base, {'*': [wc]})], rot=(10, s * 14, 0)))
        else:
            kids.append(Part(n + '_heel_ice', (x + s * 3, 3, 1), [B(x + s * 2.6, x + s * 4, 2, 7.5, .2, 1.8, acc), B(x + s * 2.8, x + s * 3.8, 2, 5.5, 2.4, 3.4, acc)], rot=(26, 0, -s * 12)))
        return Part(n + '_leg', (x, 12, 0), [Box((x - 2, 0, -2), (x + 2, 12, 2), under if elf else base,
                    {'top': [ob], 'front': [high], 'back': [high], 'left': [high], 'right': [high]}, .95)], kids)

    empty = lambda *names: [Part(n, p) for n, p in (('head', (0, 24, 0)), ('hat', (0, 24, 0)), ('body', (0, 24, 0)), ('right_arm', (-5, 22, 0)),
                                                    ('left_arm', (5, 22, 0)), ('right_leg', (-1.9, 12, 0)), ('left_leg', (1.9, 12, 0))) if n in names]
    pieces = {
        'helmet': helmet + empty('hat', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg'),
        'chestplate': empty('head', 'hat', 'right_leg', 'left_leg') + [chest_body, arm(-1), arm(1)],
        'leggings': empty('head', 'hat', 'right_arm', 'left_arm') + [legs_body, leg(-1), leg(1)],
        'boots': empty('head', 'hat', 'body', 'right_arm', 'left_arm') + [boot(-1), boot(1)],
    }
    return {k: EntityModel(v, S=2, width=128) for k, v in pieces.items()}


def icons(style):
    st = STYLES[style]
    elf = style == 'elven'
    base, under, acc, gem = st['base'](), st['under'](), st['accent'](), st['gem']()
    steel = metal(st['metal'])
    rim = trim(st['metal'][1:])
    dark = noise(MOSS[:3] if elf else NAVY[:3], 1, .5)
    eye = st['eye']
    visor = stamp(['dddddddddddd', 'dyyyddddyyyd', 'ddyyddddyydd', 'dddddddddddd', '.dddddddddd.', '.dddddddddd.'],
                  {'d': '#06100c' if elf else '#060d22', 'y': (eye, True)}, None, -1)

    def pair(e):
        return [e, mirror_x(e)]
    helm = [El((4, 3, 4), (12, 11, 12), base, decals={'front': [trim(st['metal'][1:], 1, 't'), visor]}),
            El((4.8, 11, 4.8), (11.2, 12.2, 11.2), base), El((3.6, 8.6, 3.6), (12.4, 9.8, 12.4), steel),
            El((7, 9.4, 3.1), (9, 11.8, 3.6), gem, glow=True)]
    if elf:
        for e in (El((12, 9, 7.2), (14, 10.4, 8.8), acc), El((13, 9.8, 7.4), (14.2, 15.4, 8.6), acc, rot=('z', -22.5, (13.6, 10, 8))),
                  El((13.8, 11.8, 7.5), (14.8, 15, 8.5), acc, rot=('z', -45, (14.2, 12, 8))), El((12, 3.4, 5), (13.4, 8, 10), base)):
            helm += pair(e)
    else:
        helm += [El((7, 12, 5), (9, 15.6, 7), acc), El((7.5, 15.6, 5.5), (8.5, 17, 6.5), acc)]
        for e in (El((10, 11, 5.2), (11.8, 14, 7), acc, rot=('z', -22.5, (10.8, 11, 6))), El((12, 7.6, 6), (13, 13, 8), acc, rot=('z', -22.5, (12.5, 8, 7))),
                  El((12, 3.4, 5), (13, 9, 10), steel)):
            helm += pair(e)
    chest = [El((4.2, 2, 5.5), (11.8, 12, 10.5), under if elf else base), El((3.8, 2, 5.2), (12.2, 3.6, 10.8), acc if elf else steel),
             El((5.2, 11.4, 5.1), (10.8, 13, 10.9), steel), El((6.6, 6.6, 4.7), (9.4, 9.4, 5.6), gem, rot=('z', 45, (8, 8, 5)), glow=True),
             El((7.6, 3.6, 5.1), (8.4, 6.4, 5.5), steel)]
    if elf:
        sh = (El((11.4, 9.6, 4.8), (16, 11.2, 11.2), base), El((11.2, 11.2, 5.6), (14.8, 12.8, 10.4), base), El((12.2, 5, 6), (14.2, 9.6, 10), under),
              El((12.8, 12.6, 7), (14.8, 14.4, 9), gem, rot=('z', 45, (13.8, 13.5, 8))))
    else:
        sh = (El((11.4, 9.2, 4.8), (15.4, 12.6, 11.2), base, decals={'*': [trim(st['metal'][1:], 1, 'b')]}), El((12.6, 12.6, 7), (14.4, 16.2, 9), acc, rot=('z', -22.5, (13.5, 12.6, 8))),
              El((14, 11, 6), (15.4, 14.4, 7.6), acc, rot=('z', -45, (14.5, 11.4, 7))), El((12.2, 5, 5.8), (14.4, 9.2, 10.2), under))
    for e in sh:
        chest += pair(e)
    legs = [El((3.8, 10.8, 5.4), (12.2, 13.6, 10.6), acc if elf else steel), El((6.8, 10.6, 4.8), (9.2, 13.2, 5.4), gem, rot=('z', 45, (8, 12, 5)), glow=True)]
    for e in (El((8.6, 1.5, 5.8), (12, 10.8, 10.2), under if elf else base), El((8.3, 5.2, 5), (12.3, 7.6, 5.8), acc if elf else steel),
              El((9.5, 5.6, 4.4), (11.1, 7.2, 5), gem, rot=('z', 45, (10.3, 6.4, 5))), El((11.6, 7, 5.4), (12.8, 11.6, 10.6), base)):
        legs += pair(e)
    boots = []
    extra = El((13.2, 3.4, 7), (14.4, 10, 10), base, rot=('z', -22.5, (13.6, 4.5, 8.5))) if elf else El((13.2, 4.5, 7.5), (14.6, 9.4, 9.5), acc, rot=('z', -22.5, (13.6, 4.5, 8.5)))
    for e in (El((8.8, 2, 6), (13.2, 9, 11), under if elf else base), El((8.5, 7.8, 5.7), (13.5, 9.4, 11.3), acc if elf else steel),
              El((8.8, 1, 2.6), (13.2, 4.4, 11), base, decals={'front': [rim]}), El((8.5, 0, 2.3), (13.5, 1.2, 11.3), dark),
              El((10, 1.2, .8), (12, 3, 2.6), acc), extra):
        boots += pair(e)
    mk = lambda els: ItemModel(els, S=2, frames=1)
    return {
        'helmet': (mk(helm), compact(gui_scale=.86, gui_rot=(15, 30, 0), gui_y=-.9)),
        'chestplate': (mk(chest), compact(gui_scale=.9, gui_rot=(10, 22, 0), gui_y=-.9)),
        'leggings': (mk(legs), compact(gui_scale=.98, gui_rot=(12, 24, 0), gui_y=.4)),
        'boots': (mk(boots), compact(gui_scale=.92, gui_rot=(20, 35, 0), gui_y=1.8)),
    }
