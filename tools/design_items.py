from palette import *

def held(centre_y, length, grip_y):
    """display transforms for a vertically built 3d sword"""
    gs = min(.66, 22.0 / length)
    t = -gs * (centre_y - 8) * .7071
    g = grip_y - 8
    def hand(T0, R_dir, s, vs):
        d = vs * -5 - s * g * .7071
        return [round(T0[i] + d * R_dir[i], 2) for i in range(3)]
    third = hand((0, 4, .5), (0, 1.393, -.245), .7, .85)
    first = hand((1.13, 3.2, 1.13), (0, 1.329, .483), .52, .68)
    return {
        'thirdperson_righthand': {'rotation': [0, -90, 10], 'translation': third, 'scale': [.7] * 3},
        'thirdperson_lefthand': {'rotation': [0, 90, -10], 'translation': third, 'scale': [.7] * 3},
        'firstperson_righthand': {'rotation': [0, -90, -20], 'translation': first, 'scale': [.52] * 3},
        'firstperson_lefthand': {'rotation': [0, 90, 20], 'translation': first, 'scale': [.52] * 3},
        'gui': {'rotation': [0, 0, -45], 'translation': [round(t, 2), round(t, 2), 0], 'scale': [round(gs, 3)] * 3},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [.45] * 3},
        'fixed': {'rotation': [0, 180, -45], 'translation': [round(t, 2), round(t, 2), 0], 'scale': [round(gs, 3)] * 3},
    }

def compact(scale=1.0, gui_rot=(25, 40, 0), gui_scale=1.0, gui_y=0.0):
    return {
        'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 2.5, .5], 'scale': [.45 * scale] * 3},
        'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 2.5, .5], 'scale': [.45 * scale] * 3},
        'firstperson_righthand': {'rotation': [0, 35, 0], 'translation': [1, 3, 0], 'scale': [.55 * scale] * 3},
        'firstperson_lefthand': {'rotation': [0, -35, 0], 'translation': [1, 3, 0], 'scale': [.55 * scale] * 3},
        'gui': {'rotation': list(gui_rot), 'translation': [0, gui_y, 0], 'scale': [gui_scale] * 3},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [.4 * scale] * 3},
        'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [.9 * scale] * 3},
        'head': {'rotation': [0, 0, 0], 'translation': [0, 14, 0], 'scale': [.8] * 3},
    }

def mirror_x(e):
    a = (16 - e.b[0], e.a[1], e.a[2]); b = (16 - e.a[0], e.b[1], e.b[2])
    rot = None
    if e.rot:
        axis, ang, o = e.rot
        rot = (axis, -ang if axis in 'zy' else ang, (16 - o[0], o[1], o[2]))
    return El(a, b, e.mat, e.decals, rot, e.faces, e.glow)

# ============================================================ swords
def phoenix_blade():
    steel = blade(FIREMETAL, 6, 31, flame=True)
    fire = glowing(FIRE[2:], speed=1.2, scale=1.4)
    gold = metal(GOLD)
    els = [
        El((6.5, 7, 7.4), (9.5, 26, 8.6), steel),
        El((7, 26, 7.45), (9, 29, 8.55), steel),
        El((7.5, 29, 7.5), (8.5, 32, 8.5), glowing(FIRE[3:], 1.5), glow=True),
        El((7.6, 8, 7.15), (8.4, 24, 8.85), fire, glow=True),
        El((7.2, 12, 7.2), (8.8, 13, 8.8), fire, glow=True),
        El((7.2, 18, 7.2), (8.8, 19, 8.8), fire, glow=True),
    ]
    for y, h, w in ((9.5, 4.5, 1.8), (15, 4.5, 1.6), (20.5, 4, 1.4)):
        t = El((8.7, y - .8, 7.55), (9.4 + w, y + h, 8.45), blade(FIREMETAL, y - 8, y + h, flame=True), rot=('z', -22.5, (9.5, y, 8)))
        s = El((9.6, y + h - 1, 7.7), (10.4, y + h + 1.8, 8.3), glowing(FIRE[2:], 1.4, .3), rot=('z', -22.5, (9.5, y, 8)), glow=True)
        for e in (t, s):
            els += [e, mirror_x(e)]
    wing = [
        El((9.5, 4.2, 6.9), (13.5, 6.6, 9.1), gold, rot=('z', 22.5, (9.5, 5, 8))),
        El((12, 5.6, 7.2), (16, 7.4, 8.8), gold, rot=('z', 45, (12.5, 6, 8))),
        El((13.6, 8.2, 7.4), (15, 12.5, 8.6), metal(GOLD, grad=(1, 7, 14)), rot=('z', -22.5, (14.3, 8.5, 8))),
        El((11.4, 6.4, 7.5), (12.6, 10.4, 8.5), metal(GOLD, grad=(1, 5, 11)), rot=('z', -22.5, (12, 6.6, 8))),
        El((14.6, 11.8, 7.6), (15.4, 14.2, 8.4), glowing(FIRE[2:], 1.0, .5), rot=('z', -22.5, (14.3, 8.5, 8)), glow=True),
    ]
    for e in wing:
        els += [e, mirror_x(e)]
    els += [
        El((5.8, 4, 6.4), (10.2, 7.6, 9.6), gold, decals={'*': [trim(GOLD)]}),
        El((6.9, 4.8, 5.9), (9.1, 7, 10.1), glowing(RUBY, .5), glow=True),
        El((7, -1.5, 7), (9, 4, 9), wrap(LEATHER_RED, GOLD, 3, .8)),
        El((6.6, 1, 6.6), (9.4, 1.8, 9.4), gold),
        El((6.4, -3.6, 6.4), (9.6, -1.5, 9.6), gold),
        El((7.2, -3.1, 6), (8.8, -2, 10), glowing(RUBY, .5, .5), glow=True),
        El((7.4, -8, 7.6), (8.6, -3.6, 8.4), glowing(FIRE[1:6], 1.3), glow=True),
    ]
    tail = El((8.4, -7, 7.7), (9.4, -3.4, 8.3), glowing(FIRE[1:5], 1.3, .4), rot=('z', 22.5, (8.9, -3.6, 8)), glow=True)
    els += [tail, mirror_x(tail)]
    return ItemModel(els, S=2, frames=8, frametime=3), held(12, 40, 1)

def frost_fang():
    ice = lambda y0, y1: blade(ICE, y0, y1, seed=8)
    glow = glowing(FROSTGLOW, .8, scale=1.2)
    silver = metal(SILVER)
    els = []
    segs = [(7, 12, 5.5, 10.3), (12, 17, 5.8, 10.6), (17, 21.5, 6.4, 11.1), (21.5, 25.5, 7.3, 11.5), (25.5, 29, 8.5, 11.8), (29, 31.8, 9.9, 12)]
    for i, (y0, y1, x0, x1) in enumerate(segs):
        d = .62 - i * .03
        els.append(El((x0, y0, 8 - d), (x1, y1, 8 + d), ice(2, 30)))
        if i < 5:
            cx = (x0 + x1) / 2 - .2
            els.append(El((cx - .35, y0 + .3, 7.1), (cx + .35, y1 + .3, 8.9), glow, glow=True))
    for y, x, h in ((10, 4.4, 3.6), (15.5, 5, 3.2), (20.5, 6, 2.8)):
        els.append(El((x, y, 7.6), (x + 1.4, y + h, 8.4), crystal(ICE[2:], 1.6), rot=('z', 45, (x + 1.2, y + .6, 8))))
    els += [
        El((3, 5, 7), (13, 6.6, 9), silver),
        El((3.6, 5.25, 7.15), (12.4, 6.35, 8.85), silver, rot=('z', 45, (8, 5.8, 8))),
        El((3.6, 5.25, 7.25), (12.4, 6.35, 8.75), silver, rot=('z', -45, (8, 5.8, 8))),
        El((6.2, 4, 6.3), (9.8, 7.6, 9.7), metal(SILVER), decals={'*': [trim(SILVER)]}),
        El((7, 4.8, 5.8), (9, 6.8, 10.2), glowing(FROSTGLOW[:4], .4), glow=True),
    ]
    spike = [
        El((12.4, 2.2, 7.4), (13.6, 5.2, 8.6), crystal(ICE[2:], 1.4, glow=.0)),
        El((12.7, .8, 7.7), (13.3, 2.2, 8.3), crystal(ICE[4:], 1.0)),
        El((12.6, 6.4, 7.5), (13.4, 8.8, 8.5), crystal(ICE[3:], 1.2), rot=('z', -22.5, (13, 6.4, 8))),
    ]
    for e in spike:
        els += [e, mirror_x(e)]
    els += [
        El((7, -1.5, 7), (9, 4, 9), wrap(LEATHER_NAVY, SILVER, 3, .8)),
        El((6.6, .9, 6.6), (9.4, 1.7, 9.4), silver),
        El((6.5, -2.8, 6.5), (9.5, -1.4, 9.5), silver),
        El((6.7, -5.8, 6.9), (9.3, -3.2, 9.1), crystal(ICE[1:], 1.3, glow=.8), rot=('z', 45, (8, -4.5, 8))),
        El((7.5, -8, 7.5), (8.5, -5.8, 8.5), crystal(ICE[3:], 1.0)),
    ]
    return ItemModel(els, S=2, frames=8, frametime=4), held(12, 40, 1)

def storm_blade():
    steel = blade(ramp('#2a2545', '#413a68', '#5d5590', '#7f76b8', '#a79fdc', '#d5cffa'), 4, 34, seed=13)
    bolt = glowing(STORM[1:], 2.0, scale=1.0)
    dark = metal(DSTEEL)
    gold = metal(GOLD)
    els = [
        El((5.8, 7, 7.3), (10.2, 21, 8.7), steel),
        El((5.2, 20, 7.4), (7.2, 26, 8.6), steel),
        El((5.7, 26, 7.5), (6.7, 29, 8.5), steel),
        El((8.8, 20, 7.4), (10.8, 28, 8.6), steel),
        El((9.2, 28, 7.5), (10.4, 30.5, 8.5), steel),
        El((9.5, 30.5, 7.6), (10.1, 32, 8.4), bolt, glow=True),
        El((5.9, 29, 7.6), (6.5, 30.4, 8.4), bolt, glow=True),
    ]
    zig = [(8.1, 8, 3), (6.9, 10.4, 3), (8.1, 12.8, 3), (6.9, 15.2, 3), (8.1, 17.6, 3)]
    for x, y, h in zig:
        els.append(El((x, y, 7.05), (x + .9, y + h, 8.95), bolt, glow=True))
    for x, y in ((7.4, 10.4), (7.4, 12.8), (7.4, 15.2), (7.4, 17.6), (7.4, 20)):
        els.append(El((x, y, 7.1), (x + 1.2, y + .7, 8.9), bolt, glow=True))
    els += [
        El((7.1, 23, 7.2), (8.9, 24.8, 8.8), glowing(STORM[2:], 2.5, .5), rot=('z', 45, (8, 23.9, 8)), glow=True),
        El((7.6, 25.8, 7.6), (8.4, 26.6, 8.4), glowing(STORM[3:], 3, .2), rot=('z', 45, (8, 26.2, 8)), glow=True),
    ]
    g = [
        El((9.5, 4.6, 6.8), (13, 7.2, 9.2), dark, decals={'*': [trim(GOLD)]}),
        El((12, 5.5, 7.1), (15.5, 7.2, 8.9), gold, rot=('z', 22.5, (12.5, 6, 8))),
        El((13.2, 6.6, 7.3), (15, 10.6, 8.7), crystal(AMETHYST[1:], 1.5, glow=.9), rot=('z', -45, (14.1, 7, 8))),
        El((11.2, 2.4, 7.4), (12.6, 5, 8.6), crystal(AMETHYST[1:], 1.2, glow=.7), rot=('z', 22.5, (11.9, 5, 8))),
        El((10.2, 7, 7.2), (11.6, 9.6, 8.8), dark, rot=('z', -22.5, (10.9, 7, 8))),
    ]
    for e in g:
        els += [e, mirror_x(e)]
    els += [
        El((5.8, 4, 6.3), (10.2, 8, 9.7), dark, decals={'*': [trim(GOLD)]}),
        El((6.8, 4.9, 5.8), (9.2, 7.1, 10.2), glowing(STORM[1:5], .6), rot=('z', 45, (8, 6, 8)), glow=True),
        El((7, -1.5, 7), (9, 4, 9), wrap(LEATHER_BLACK, AMETHYST, 3, .8)),
        El((6.6, .9, 6.6), (9.4, 1.7, 9.4), gold),
        El((6.4, -3, 6.4), (9.6, -1.4, 9.6), dark, decals={'*': [trim(GOLD)]}),
        El((6.8, -6, 6.8), (9.2, -3.4, 9.2), glowing(STORM[:5], .7, .3), rot=('z', 45, (8, -4.7, 8)), glow=True),
        El((7.5, -8, 7.5), (8.5, -6.2, 8.5), gold),
    ]
    return ItemModel(els, S=2, frames=8, frametime=2), held(12, 40, 1)

# ============================================================ foods
def moon_apple():
    skin = noise(ramp('#241a52', '#3d2f82', '#5c4cb0', '#8272d6', '#a99cef', '#d3ccff', '#f4f2ff'), 2.6, .22, 1.0, grad=(1, -1, 13))
    star = {'s': ('#ffffff', True), 't': ('#d9f6ff', True)}
    moon = stamp(['....mmmm..', '..mmm.....', '.mmm......', 'mmm.......', 'mmm.......', 'mmmm.....m', '.mmmmmmmm.', '...mmmm...'],
                 {'m': ('#fff3b0', True)}, 2, 1)
    spk = stamp(['..t..', '.....', 't.s.t', '.....', '..t..'], star, -3, 3)
    dot = stamp(['s'], star, 4, -4)
    stem = noise(LEATHER_BROWN[1:], 1, .5)
    leaf = noise(EMERALD[:4], 1.2, .3, 1.2)
    els = [
        El((4, 3.5, 4), (12, 10, 12), skin, decals={'back': [spk, dot], 'left': [spk, dot], 'right': [spk]}),
        El((4.8, 2, 4.8), (11.2, 3.5, 11.2), skin), El((6, 1, 6), (10, 2, 10), skin),
        El((4.8, 10, 4.8), (11.2, 11.3, 11.2), skin, decals={'top': [dot]}),
        El((5.4, 11.3, 5.4), (7.3, 12.2, 10.6), skin), El((8.7, 11.3, 5.4), (10.6, 12.2, 10.6), skin),
        El((7.3, 11.3, 5.4), (8.7, 11.9, 7), skin), El((7.3, 11.3, 9), (8.7, 11.9, 10.6), skin),
        El((3.4, 5, 5), (4, 9, 11), skin), El((12, 5, 5), (12.6, 9, 11), skin),
        El((5, 4.4, 3.4), (11, 9.4, 4), skin, decals={'front': [moon]}),
        El((5.6, 3, 4.1), (10.4, 4.4, 4.8), skin), El((5.6, 9.4, 4.1), (10.4, 10.6, 4.8), skin),
        El((5.6, 3, 11.2), (10.4, 4.4, 11.9), skin), El((4.1, 3, 5.6), (4.8, 4.4, 10.4), skin), El((11.2, 3, 5.6), (11.9, 4.4, 10.4), skin),
        El((5, 5, 12), (11, 9, 12.6), skin),
        El((7.5, 11.2, 7.5), (8.5, 15, 8.5), stem, rot=('z', -22.5, (8, 11.5, 8))),
        El((9, 13.2, 6.6), (13.4, 13.8, 9.4), leaf, rot=('z', 22.5, (9, 13.4, 8))),
        El((9.4, 13.8, 7.8), (12.6, 14, 8.2), glowing(EMERALD[2:], .3), rot=('z', 22.5, (9, 13.4, 8)), glow=True),
    ]
    return ItemModel(els, S=2), compact(gui_scale=.98, gui_y=.2)

def fairy_cake():
    def flute(c):
        a = np.where(np.isin(c.face, ('left', 'right')), c.z, c.x)
        v = np.where(np.mod(np.floor(a * 1.0), 2) == 0, .75, .3) + (c.y / 14) * .15
        return bevel(c, paint(c, v, ramp('#4a1f6b', '#7437a0', '#9c57c9', '#c98af0', '#eac4ff', '#fbefff')), .8)
    wrapper = Mat(flute)
    sponge = noise(ramp('#8a4d12', '#bd7522', '#e19c38', '#f4bf5c', '#ffdd8c'), 1.2, .55)
    def swirl(c):
        v = np.mod((c.x + c.z) * .22 + c.y * .55 + vnoise(c.P, 2.0, 3) * .5, 1.0)
        return bevel(c, paint(c, .15 + v * .75, ramp('#c2327a', '#e9559a', '#ff7db6', '#ffa8d0', '#ffd2e8', '#fff0f8')), 1.2)
    frost = Mat(swirl)
    gold = metal(GOLD)
    wingm = noise(ramp('#7fd8ff', '#a9ebff', '#d2f7ff', '#ffffff'), 1.0, .4, 1.0)
    def wing_shape(c):
        u = c.u / max(c.W - 1, 1); v = c.v / max(c.H - 1, 1)
        inner = u if c.face != 'back' else u
        far = np.clip((12 - np.abs(c.x - 8)) , 0, 12)
        dx = (np.abs(c.x - 8) - 3.4) / 4.6
        dy = (c.y - 8.6) / 3.6
        lobe1 = ((dx - .55) / .55) ** 2 + ((dy - .35) / .65) ** 2 < 1
        lobe2 = ((dx - .3) / .36) ** 2 + ((dy + .5) / .45) ** 2 < 1
        return ~(lobe1 | lobe2)
    wc = cut(wing_shape)
    els = [
        El((5, 0, 5), (11, 3, 11), wrapper), El((4.4, 3, 4.4), (11.6, 6, 11.6), wrapper),
        El((4, 6, 4), (12, 7, 12), sponge),
        El((3.8, 7, 3.8), (12.2, 8.6, 12.2), frost), El((4.8, 8.6, 4.8), (11.2, 10.2, 11.2), frost),
        El((5.8, 10.2, 5.8), (10.2, 11.6, 10.2), frost), El((6.8, 11.6, 6.8), (9.2, 12.8, 9.2), frost),
        El((7.2, 13.1, 7.5), (8.8, 14.7, 8.5), glowing(GOLD[3:], .6), glow=True),
        El((7.2, 13.1, 7.6), (8.8, 14.7, 8.4), glowing(GOLD[4:], .6, .4), rot=('z', 45, (8, 13.9, 8)), glow=True),
        El((0, 5.5, 7.8), (5, 12.5, 8.2), wingm, decals={'*': [wc]}),
        El((11, 5.5, 7.8), (16, 12.5, 8.2), wingm, decals={'*': [wc]}),
    ]
    cols = [FROSTGLOW, GOLD[3:], EMERALD[2:], STORM[2:]]
    spr = [(4.6, 8.6, 5.4), (10.4, 8.6, 4.4), (6.2, 10.2, 4.5), (9.8, 10.2, 9.9), (5.4, 11.6, 8.8), (9.4, 11.6, 6), (11.4, 8.6, 9.5), (4.3, 8.6, 10), (7.4, 12.8, 7), (8, 8.6, 3.6)]
    for i, (x, y, z) in enumerate(spr):
        els.append(El((x, y, z), (x + .8, y + .6, z + .8), glowing(cols[i % 4], .5, i / 10), glow=True))
    return ItemModel(els, S=2, frames=8, frametime=4), compact(gui_scale=.95, gui_rot=(22, 25, 0), gui_y=.1)

def dragon_drumstick():
    def roast(c):
        v = vnoise(c.P, 2.4, 31) * .6 + vnoise(c.P, .9, 32) * .4
        img = paint(c, (v - .5) * 1.4 + .5, ramp('#2e0a04', '#541406', '#7e260a', '#a73f10', '#cc641c', '#e89032', '#ffc261'))
        stripe = np.mod(c.x * .9 + c.y * .35 + c.z * .2, 3.4) < .7
        img[stripe, :3] *= .52
        shine = hash3(np.floor(c.P * c.S), 41) > .965
        img[shine, :3] = rgb('#ffe3a0')
        herb = hash3(np.floor(c.P * c.S), 77) > .975
        img[herb, :3] = rgb('#5d8f2a')
        return bevel(c, img, 1.2)
    meat = Mat(roast)
    bone = noise(ramp('#9c8a68', '#c7b58e', '#e3d6b4', '#f6eed6', '#fffaec'), 1.4, .3, 1.4)
    rot = ('z', 45, (8, 8, 8))
    R = lambda a, b, m, **k: El(a, b, m, rot=rot, **k)
    els = [
        R((1.5, 4, 4.4), (9, 12, 11.6), meat), R((2.5, 3, 5.2), (7.5, 13, 10.8), meat),
        R((2.5, 5, 3.4), (7.5, 11, 12.6), meat), R((.2, 5.4, 5.6), (1.5, 10.6, 10.4), meat),
        R((9, 5.2, 5.4), (10.4, 10.8, 10.6), meat), R((10.4, 6.4, 6.4), (11.4, 9.6, 9.6), meat),
        R((11, 7, 7), (14.8, 9, 9), bone), R((14.2, 8, 6.7), (16.4, 10.4, 9.3), bone), R((14.2, 5.6, 6.7), (16.4, 8, 9.3), bone),
        R((11.2, 6.6, 6.6), (11.9, 9.4, 9.4), metal(GOLD)),
    ]
    return ItemModel(els, S=2), compact(gui_scale=.86, gui_rot=(0, 25, 0))

def elven_mead():
    def liquid(c):
        g = np.clip((c.y - 1) / 8, 0, 1)
        v = g * .6 + .15 + (vnoise(c.P + np.array([0, -c.t * 3, 0]), 1.6, 51) - .5) * .3
        img = paint(c, v, ramp('#7a3a00', '#b05e02', '#dd8a08', '#fbb51e', '#ffd94e', '#fff2a8'))
        bub = hash3(np.floor((c.P + np.array([0, -c.t * 4, 0])) * c.S * .75), 55) > .972
        img[bub, :3] = rgb('#fffbe0')
        if c.face in ('front', 'back', 'left', 'right') and c.W >= 6:
            img[1:-1, 1:3, :3] = np.clip(img[1:-1, 1:3, :3] * .35 + 170, 0, 255)
        return img
    liq = Mat(liquid, glow=lambda c, img: np.full((c.H, c.W), .42))
    glass = noise(ramp('#3c7f6e', '#5fa893', '#8fd0bc', '#c3efe2'), 1.4, .3, 1.8)
    gold = metal(GOLD)
    cork = noise(LEATHER_BROWN[2:], .8, .7)
    leaf = noise(EMERALD[:4], 1.0, .3, 1.2)
    vine = stamp(['..l.....', '.lll....', '..l.g...', '....gg..', '.....g.l', '.....gll', '....g.l.', '..gg....'],
                 {'l': '#37c966', 'g': '#157a3a'}, 1, -1)
    els = [
        El((4.8, 0, 4.8), (11.2, 1.2, 11.2), gold),
        El((4.2, 1.2, 4.2), (11.8, 8, 11.8), liq, decals={'front': [vine], 'right': [vine], 'left': [vine], 'back': [vine]}),
        El((5, 8, 5), (11, 9.2, 11), glass), El((6, 9.2, 6), (10, 10, 10), glass),
        El((6.8, 10, 6.8), (9.2, 12.4, 9.2), glass),
        El((6.4, 12.4, 6.4), (9.6, 13.4, 9.6), gold), El((7.1, 13.4, 7.1), (8.9, 15.4, 8.9), cork),
        El((4, 4.4, 4), (12, 5.6, 12), gold),
        El((7, 3.8, 3.5), (9, 6.2, 4), glowing(EMERALD[1:], .4), rot=('z', 45, (8, 5, 4)), glow=True),
        El((9.4, 12.6, 6.8), (13.4, 13.2, 9.2), leaf, rot=('z', 22.5, (9.4, 12.9, 8))),
        El((3, 11.4, 7), (6.6, 12, 9), leaf, rot=('z', -22.5, (6.6, 11.7, 8))),
    ]
    return ItemModel(els, S=2, frames=8, frametime=4), compact(gui_scale=.92, gui_rot=(22, 35, 0))

# ============================================================ coin, eggs
def crown_coin():
    gold = metal(GOLD[1:6], sheen=.22)
    rim = metal(GOLD[:5], sheen=.2)
    face = noise(GOLD[1:4], 1.5, .25, 1.2)
    emblem = ['..o....o....o..', '.oyo..oyo..oyo.', '.oyo..oyo..oyo.', '.oyyooyyyooyyo.', '.oyyyyyyyyyyyo.', '.oyyyyyyyyyyyo.', '.oyrryyggyyrro.', '.oyyyyyyyyyyyo.', '.ooooooooooooo.', '.oyyyyyyyyyyyo.', '.ooooooooooooo.']
    pal = {'o': '#5a3300', 'y': '#fff3a8', 'r': ('#ff3d4f', True), 'g': ('#47e08a', True)}
    els = [
        El((5, 1.8, 7.2), (11, 14.2, 8.8), gold), El((3.2, 3.2, 7.3), (12.8, 12.8, 8.7), gold),
        El((1.8, 5, 7.4), (14.2, 11, 8.6), rim),
        El((3.8, 4.2, 6.8), (12.2, 11.8, 7.2), face, decals={'front': [trim(GOLD[2:]), stamp(emblem, pal)]}),
        El((3.8, 4.2, 8.8), (12.2, 11.8, 9.2), face, decals={'back': [trim(GOLD[2:]), stamp(emblem, pal)]}),
    ]
    return ItemModel(els, S=2), compact(gui_scale=1.0, gui_rot=(0, 20, 0))

def griffin_egg():
    shell = noise(ramp('#7a4a0c', '#b57a18', '#dea633', '#f4c95a', '#ffe59a', '#fff7d8'), 2.2, .2, .9, grad=(1, 0, 17))
    speck = Mat(lambda c: (lambda img: (img.__setitem__(hash3(np.floor(c.P * 1.2), 91) > .86, np.array([122, 72, 20, 255.])), img)[1])(shell(c)))
    straw = noise(LEATHER_BROWN[1:], .7, .7, 1.4)
    rune = ['...rr...', '..r..r..', '.r.rr.r.', 'r.r..r.r', '.r.rr.r.', '..r..r..', '...rr...']
    r = stamp(rune, {'r': ('#8ff0ff', True)})
    els = [
        El((5, 2.5, 5), (11, 3.5, 11), speck), El((4.2, 3.5, 4.2), (11.8, 9.5, 11.8), speck),
        El((4.8, 9.5, 4.8), (11.2, 11.8, 11.2), speck), El((5.6, 11.8, 5.6), (10.4, 13.6, 10.4), speck), El((6.6, 13.6, 6.6), (9.4, 14.8, 9.4), speck),
        El((3.6, 5, 5), (4.2, 8.5, 11), speck), El((11.8, 5, 5), (12.4, 8.5, 11), speck), El((5, 4.6, 3.6), (11, 9, 4.2), speck, decals={'front': [r]}), El((5, 4.6, 11.8), (11, 9, 12.4), speck, decals={'back': [r]}),
        El((2.4, .4, 2.6), (13.6, 2, 4.6), straw), El((2.4, .4, 11.4), (13.6, 2, 13.4), straw),
        El((2.6, 1.2, 2.4), (4.6, 2.8, 13.6), straw), El((11.4, 1.2, 2.4), (13.4, 2.8, 13.6), straw),
        El((3.5, 0, 3.5), (12.5, 2.4, 12.5), straw),
    ]
    feather = El((11.2, 2.6, 6.8), (12.6, 8.6, 7.2), tiles(GOLD[2:], 3, 3, grad=(1, 2, 9)), rot=('z', -22.5, (11.9, 2.6, 7)))
    els += [feather]
    return ItemModel(els, S=2), compact(gui_scale=.92, gui_rot=(18, 30, 0))

def golem_core():
    rock = stone(ramp('#12141f', '#1f2333', '#31374c', '#474f69', '#646e8c'))
    cry = lambda: crystal(ramp('#0a4a66', '#0d7c99', '#14b6c8', '#4fe3e8', '#aafbf6', '#ffffff'), 2.0, glow=.9)
    pur = crystal(AMETHYST[1:], 1.8, glow=.7)
    els = [
        El((3, 0, 3), (13, 2.4, 13), rock), El((4.5, 2.4, 4.5), (11.5, 3.6, 11.5), rock),
        El((5, 5.5, 5), (11, 11.5, 11), cry(), rot=('y', 45, (8, 8.5, 8)), glow=True),
        El((6.5, 11.5, 6.5), (9.5, 14.5, 9.5), cry(), rot=('y', 45, (8, 8, 8))),
        El((7.3, 14.5, 7.3), (8.7, 16.5, 8.7), cry(), rot=('y', 45, (8, 8, 8))),
        El((6.8, 3.2, 6.8), (9.2, 5.5, 9.2), cry(), rot=('y', 45, (8, 8, 8))),
        El((2.6, 2, 6.6), (5, 9, 9), pur, rot=('z', 22.5, (4, 2, 8))),
        El((11, 2, 7), (13.2, 10.5, 9.4), pur, rot=('z', -22.5, (12, 2, 8))),
        El((6.8, 2, 2.8), (9, 7.5, 5), pur, rot=('x', -22.5, (8, 2, 4))),
        El((6.6, 2, 11), (9.2, 8.5, 13.4), pur, rot=('x', 22.5, (8, 2, 12))),
    ]
    return ItemModel(els, S=2, frames=8, frametime=4), compact(gui_scale=.9, gui_rot=(25, 30, 0))

# ============================================================ armour icons
SCALE = ramp('#1a0306', '#37070d', '#5c0d15', '#85151d', '#ad2325', '#d33c2c', '#f1693a')

def dscale(tw=4, th=4, grad=None):
    return scales(SCALE, tw + 2, th, grad=grad)

def armour_icons():
    sc = dscale(4, 4)
    gold = metal(GOLD)
    horn = metal(ramp('#4a3822', '#7a6038', '#a8895a', '#d4b988', '#f4e2bc'), grad=(1, 8, 17))
    dark = noise(ramp('#0a0406', '#17080b', '#240d10'), 1, .5)
    gem = glowing(FIRE[2:], .6)
    visor = stamp(['dddddddddddd', 'dyyyddddyyyd', 'ddyyddddyydd', 'dddddggddddd', '.ddddggdddd.', '.ddddggdddd.'],
                  {'d': '#12050a', 'y': ('#ffcf3a', True), 'g': '#e3a81c'}, None, -1)
    def pair(e):
        return [e, mirror_x(e)]
    helm = [El((4, 3, 4), (12, 11, 12), sc, decals={'front': [trim(GOLD, 1, 't'), visor]}),
            El((4.8, 11, 4.8), (11.2, 12.2, 11.2), sc), El((7.3, 10.5, 3.4), (8.7, 13.6, 12.6), gold),
            El((3.6, 8.6, 3.6), (12.4, 9.8, 12.4), gold), El((7, 9.8, 3.1), (9, 11.4, 3.6), gem, glow=True)]
    for e in (El((12, 8, 7), (14.4, 10, 9), horn), El((13.2, 9.4, 7.2), (15, 13.4, 8.8), horn, rot=('z', -22.5, (14, 9.5, 8))),
              El((13.8, 12.8, 7.5), (15, 15.8, 8.5), horn), El((12, 3, 4.4), (13, 7, 9), sc)):
        helm += pair(e)
    chest = [El((4.2, 2, 5.5), (11.8, 12, 10.5), sc), El((3.8, 2, 5.2), (12.2, 3.6, 10.8), gold),
             El((5.2, 11.4, 5.1), (10.8, 13, 10.9), gold), El((6.6, 6.6, 4.7), (9.4, 9.4, 5.6), gem, rot=('z', 45, (8, 8, 5)), glow=True),
             El((7.6, 3.6, 5.1), (8.4, 6.4, 5.5), gold)]
    for e in (El((11.4, 9.2, 4.8), (15.4, 13, 11.2), sc, decals={'*': [trim(GOLD, 1, 'b')]}), El((12.4, 13, 5.8), (14.8, 14.4, 10.2), gold),
              El((13.8, 13.4, 7.2), (15, 16.2, 8.8), horn, rot=('z', -22.5, (14.4, 13.4, 8))), El((12.2, 5, 5.8), (14.4, 9.2, 10.2), sc),
              El((8.6, 6, 5), (11.6, 7, 5.5), gold, rot=('z', 22.5, (8.6, 6.5, 5)))):
        chest += pair(e)
    legs = [El((3.8, 10.8, 5.4), (12.2, 13.6, 10.6), gold), El((6.8, 10.6, 4.8), (9.2, 13.2, 5.4), gem, rot=('z', 45, (8, 12, 5)), glow=True)]
    for e in (El((8.6, 1.5, 5.8), (12, 10.8, 10.2), sc), El((8.3, 5.2, 5), (12.3, 7.6, 5.8), gold),
              El((9.5, 5.6, 4.4), (11.1, 7.2, 5), horn, rot=('z', 45, (10.3, 6.4, 5))), El((11.6, 8, 5.4), (12.8, 11.6, 10.6), sc)):
        legs += pair(e)
    boots = []
    for e in (El((8.8, 2, 6), (13.2, 9, 11), sc), El((8.5, 7.8, 5.7), (13.5, 9.4, 11.3), gold),
              El((8.8, 1, 2.6), (13.2, 4.4, 11), sc, decals={'front': [trim(GOLD)]}), El((8.5, 0, 2.3), (13.5, 1.2, 11.3), dark),
              El((9.2, 1.2, 1.2), (10.2, 2.4, 2.6), horn), El((11.8, 1.2, 1.2), (12.8, 2.4, 2.6), horn),
              El((13.2, 4.5, 7.5), (14.6, 7.8, 9.5), gold, rot=('z', -22.5, (13.6, 4.5, 8.5)))):
        boots += pair(e)
    mk = lambda els: ItemModel(els, S=2, frames=1)
    return {
        'dragon_helmet': (mk(helm), compact(gui_scale=.9, gui_rot=(15, 30, 0), gui_y=-.8)),
        'dragon_chestplate': (mk(chest), compact(gui_scale=.92, gui_rot=(10, 22, 0), gui_y=-.9)),
        'dragon_leggings': (mk(legs), compact(gui_scale=.98, gui_rot=(12, 24, 0), gui_y=.4)),
        'dragon_boots': (mk(boots), compact(gui_scale=.95, gui_rot=(20, 35, 0), gui_y=1.8)),
    }

ITEMS = {
    'weapons': {'phoenix_blade': phoenix_blade, 'frost_fang': frost_fang, 'storm_blade': storm_blade},
    'feast': {'moon_apple': moon_apple, 'fairy_cake': fairy_cake, 'dragon_drumstick': dragon_drumstick, 'elven_mead': elven_mead},
    'core': {'crown': crown_coin},
    'creatures': {'griffin_egg': griffin_egg, 'golem_core': golem_core},
}
