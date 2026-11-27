"""Second wave of items: new weapons (with the bow's pull stages), foods and summoning items."""
from design_items import *

# ============================================================ weapons
def void_reaver():
    steel = blade(ramp('#07040f', '#130a24', '#231240', '#391c63', '#53298a', '#7c43bd'), 5, 33, seed=201)
    void = glowing(STORM[:5], 1.1, scale=1.2)
    dark = metal(DSTEEL)
    edge = glowing(ramp('#8a1fb8', '#c33ff0', '#e88bff', '#ffd9ff'), 1.6, .3)
    els = [
        El((5.6, 7, 7.3), (10.4, 24, 8.7), steel),
        El((6.2, 24, 7.4), (10.4, 28, 8.6), steel), El((7.4, 28, 7.5), (10.4, 31, 8.5), steel), El((8.8, 31, 7.6), (10.4, 32, 8.4), edge, glow=True),
        El((10.4, 11, 7.45), (11, 31, 8.55), edge, glow=True),
        El((7.5, 9, 7.05), (8.5, 22, 8.95), void, glow=True),
    ]
    for y in (11.5, 16, 20.5):
        els.append(El((3.6, y, 7.55), (5.8, y + 2.2, 8.45), steel, rot=('z', 45, (5.6, y, 8))))
        els.append(El((3.2, y + 1.9, 7.7), (4.4, y + 3.1, 8.3), edge, rot=('z', 45, (5.6, y, 8)), glow=True))
    claw = [
        El((10, 4.8, 6.9), (14, 6.8, 9.1), dark, rot=('z', 22.5, (10.5, 5.5, 8))),
        El((12.8, 6.4, 7.2), (14.4, 11, 8.8), dark, rot=('z', -22.5, (13.6, 6.6, 8))),
        El((13.9, 10.4, 7.5), (14.9, 13.4, 8.5), edge, rot=('z', -22.5, (13.6, 6.6, 8)), glow=True),
    ]
    for e in claw:
        els += [e, mirror_x(e)]
    els += [
        El((5.6, 3.8, 6.3), (10.4, 8, 9.7), dark, decals={'*': [trim(AMETHYST[1:])]}),
        El((6.7, 4.7, 5.7), (9.3, 7.1, 10.3), void, rot=('z', 45, (8, 6, 8)), glow=True),
        El((7, -1.5, 7), (9, 4, 9), wrap(LEATHER_BLACK, AMETHYST[2:], 3, .8)),
        El((6.5, -3.2, 6.5), (9.5, -1.4, 9.5), dark),
        El((6.8, -6, 6.8), (9.2, -3.6, 9.2), void, rot=('z', 45, (8, -4.8, 8)), glow=True),
    ]
    return ItemModel(els, S=2, frames=8, frametime=3), held(12, 40, 1)

def arcane_staff():
    wood = noise(ramp('#171034', '#261a52', '#392975', '#503c9a'), .9, .5, 1.2)
    gold = metal(GOLD)
    orb = crystal(ramp('#1c2f9e', '#2f5ae0', '#4f94ff', '#8fd0ff', '#e6fbff'), 1.8, seed=205, glow=.95)
    spark = glowing(FROSTGLOW, 1.4, .2, scale=1.0)
    els = [
        El((7.1, -8, 7.1), (8.9, 20, 8.9), wood),
        El((6.6, -9, 6.6), (9.4, -7, 9.4), gold), El((6.7, -1, 6.7), (9.3, 3.5, 9.3), wrap(LEATHER_NAVY, GOLD, 3, .8)),
        El((6.7, 9, 6.7), (9.3, 10.4, 9.3), gold), El((6.4, 18, 6.4), (9.6, 20.4, 9.6), gold),
        El((5.6, 22.6, 5.6), (10.4, 27.4, 10.4), orb, rot=('y', 45, (8, 25, 8)), glow=True),
        El((6.6, 27.4, 6.6), (9.4, 28.6, 9.4), orb, glow=True),
        El((7.4, 30.4, 7.4), (8.6, 31.8, 8.6), spark, rot=('y', 45, (8, 31, 8)), glow=True),
    ]
    arm = [
        El((9.4, 19.6, 7.2), (13.2, 21.2, 8.8), gold, rot=('z', 22.5, (9.4, 20.4, 8))),
        El((12.2, 21.2, 7.3), (13.6, 27.4, 8.7), gold),
        El((11.4, 26.6, 7.4), (12.8, 30, 8.6), gold, rot=('z', 22.5, (12.8, 27, 8))),
        El((13.6, 23, 7.5), (14.6, 25.4, 8.5), spark, rot=('z', -22.5, (13.6, 23, 8)), glow=True),
    ]
    for e in arm:
        els += [e, mirror_x(e)]
    els += [El((1.6, 28, 7.5), (3, 29.4, 8.5), spark, rot=('z', 45, (2.3, 28.7, 8)), glow=True),
            El((13.4, 29.6, 7.6), (14.4, 30.6, 8.4), spark, rot=('z', 45, (13.9, 30.1, 8)), glow=True)]
    return ItemModel(els, S=2, frames=8, frametime=3), held(11.5, 41, 1)

def life_staff():
    bark = noise(ramp('#23140a', '#3e2611', '#5e3c1b', '#805728', '#a47737'), .8, .55, 1.3)
    leaf = noise(ramp('#0c4a22', '#168038', '#2cb452', '#66de7c', '#b6f7b0'), 1.1, .35, 1.2)
    gem = crystal(ramp('#07532c', '#0e9248', '#22d468', '#7cf7a4', '#e2ffe9'), 1.6, seed=209, glow=.95)
    petal = glowing(ramp('#d8538f', '#ff82b4', '#ffb4d2', '#ffe6f1'), .6, .4)
    els = [
        El((7, -8, 7), (9, 8, 9), bark), El((7.4, 7, 7.2), (9.4, 17, 9), bark), El((6.8, 16, 7), (8.8, 22, 8.8), bark),
        El((6.5, -1, 6.6), (9.5, 3.5, 9.4), wrap(LEATHER_BROWN, EMERALD[1:], 3, .8)),
        El((6.4, -9, 6.4), (9.6, -7.4, 9.6), metal(GOLD)),
        El((4.4, 20.4, 7.2), (6.8, 22.2, 8.8), bark, rot=('z', -22.5, (6.8, 21, 8))),
        El((3, 21.4, 7.3), (4.8, 28, 8.7), bark), El((3.8, 26.8, 7.4), (5.4, 30.6, 8.6), bark, rot=('z', -22.5, (4.2, 27, 8))),
        El((8.8, 19.8, 7.1), (12, 21.6, 8.9), bark, rot=('z', 22.5, (8.8, 20.6, 8))),
        El((11, 21, 7.2), (12.8, 27, 8.8), bark), El((10.2, 26, 7.4), (11.8, 30.4, 8.6), bark, rot=('z', 22.5, (11.8, 26, 8))),
        El((6, 23, 6.4), (10, 28, 9.6), gem, rot=('y', 45, (8, 25, 8)), glow=True),
        El((7, 28, 7), (9, 30.4, 9), gem, rot=('y', 45, (8, 25, 8)), glow=True),
        El((.4, 25.6, 7.5), (3.2, 28.4, 8.5), leaf, rot=('z', 45, (3.2, 27, 8))),
        El((12.8, 22.6, 7.5), (15.6, 25.4, 8.5), leaf, rot=('z', 45, (12.8, 24, 8))),
        El((9.2, 11.4, 7.4), (12.6, 13.4, 8.6), leaf, rot=('z', 22.5, (9.2, 12.4, 8))),
        El((3.6, 15.2, 7.4), (7.2, 17, 8.6), leaf, rot=('z', -22.5, (7.2, 16, 8))),
        El((3.2, 30, 6.8), (5, 31.8, 9.2), petal, rot=('z', 45, (4.1, 30.9, 8)), glow=True),
        El((10.6, 29.8, 6.9), (12.2, 31.4, 9.1), petal, rot=('z', 45, (11.4, 30.6, 8)), glow=True),
    ]
    return ItemModel(els, S=2, frames=8, frametime=4), held(11.5, 41, 1)

def elven_bow(stage=None):
    """Built upright: belly towards -x, string on the +x side. stage None = idle, 0..2 = pulling."""
    wood = noise(ramp('#35200c', '#563613', '#7d531f', '#a8752f', '#d09c49'), .8, .45, 1.3)
    silver = metal(SILVER)
    leaf = noise(ramp('#0c4a22', '#168038', '#2cb452', '#66de7c', '#b6f7b0'), 1.0, .35, 1.1)
    gem = glowing(EMERALD[1:], .6)
    string = solid('#e8fff4', .3)
    pull = {None: 0, 0: 1.6, 1: 3.4, 2: 5.2}[stage]
    els = [
        El((4.2, 4.6, 6.8), (6.6, 11.4, 9.2), wrap(LEATHER_NAVY, SILVER, 3, .8)),
        El((3.4, 7, 7.2), (4.4, 9, 8.8), gem, glow=True),
    ]
    upper = [
        El((4.6, 11.2, 7.1), (6.6, 14.8, 8.9), wood), El((5.8, 14.4, 7.2), (7.8, 17, 8.8), wood),
        El((7.2, 16.6, 7.3), (9.2, 18.6, 8.7), wood), El((8.8, 18, 7.4), (10.8, 19.4, 8.6), silver),
        El((10.2, 19, 7.5), (11.2, 20.8, 8.5), silver),
        El((4, 11, 6.9), (6.8, 12, 9.1), silver),
        El((2.6, 12.6, 7.5), (4.8, 15.6, 8.5), leaf, rot=('z', 22.5, (4.6, 12.8, 8))),
        El((4.2, 16.2, 7.6), (6.2, 18.8, 8.4), leaf, rot=('z', 45, (6, 16.4, 8))),
    ]
    for e in upper:
        els.append(e)
        a = (e.a[0], 16 - e.b[1], e.a[2]); b = (e.b[0], 16 - e.a[1], e.b[2])
        rot = None
        if e.rot:
            axis, ang, o = e.rot
            rot = (axis, -ang if axis in 'zx' else ang, (o[0], 16 - o[1], o[2]))
        els.append(El(a, b, e.mat, e.decals, rot, e.faces, e.glow))
    # the string: little steps from tip to tip, bent where the arrow nocks
    n = 16
    for i in range(n):
        y0 = -3.8 + i * 23.6 / n
        mid = y0 + 23.6 / n / 2
        x = 10.4 + pull * max(0.0, 1 - abs(mid - 8) / 11.8)
        els.append(El((x, y0, 7.8), (x + .45, y0 + 23.6 / n + .15, 8.2), string))
    if stage is not None:
        tip = -1.5 + pull
        els += [
            El((tip, 7.6, 7.65), (10.6 + pull, 8.4, 8.35), noise(LEATHER_BROWN[2:], .8, .5)),
            El((tip - 2.2, 7.1, 7.35), (tip, 8.9, 8.65), crystal(EMERALD[1:], 1.2, glow=.8), glow=True),
            El((8.6 + pull, 8.2, 7.8), (11 + pull, 9.8, 8.2), leaf), El((8.6 + pull, 6.2, 7.8), (11 + pull, 7.8, 8.2), leaf),
        ]
    # vanilla bow transforms, turned 45 degrees because this bow is modelled upright
    display = {
        'thirdperson_righthand': {'rotation': [-80, 260, -85], 'translation': [-1, -2, 2.5], 'scale': [.8, .8, .8]},
        'thirdperson_lefthand': {'rotation': [-80, -280, 85], 'translation': [-1, -2, 2.5], 'scale': [.8, .8, .8]},
        'firstperson_righthand': {'rotation': [0, -90, -20], 'translation': [1.13, 3.2, 1.13], 'scale': [.6, .6, .6]},
        'firstperson_lefthand': {'rotation': [0, 90, 20], 'translation': [1.13, 3.2, 1.13], 'scale': [.6, .6, .6]},
        'gui': {'rotation': [0, 0, -45], 'translation': [0, 0, 0], 'scale': [.66, .66, .66]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [.45, .45, .45]},
        'fixed': {'rotation': [0, 180, -45], 'translation': [0, 0, 0], 'scale': [.66, .66, .66]},
    }
    return ItemModel(els, S=2, frames=8, frametime=4), display

# ============================================================ foods
def star_fruit():
    skin = noise(ramp('#a35a00', '#d98a00', '#ffb914', '#ffd94a', '#ffef8f', '#fffbd8'), 2.0, .2, 1.0, grad=(1, -2, 15))
    glint = glowing(GOLD[3:], .6, .3)
    leaf = noise(EMERALD[:4], 1.0, .3, 1.2)
    els = [
        El((5, 5, 5.6), (11, 11, 10.4), skin), El((5.8, 5.8, 4.8), (10.2, 10.2, 5.6), glint, glow=True), El((5.8, 5.8, 10.4), (10.2, 10.2, 11.2), glint, glow=True),
        # five points
        El((6.4, 11, 6.2), (9.6, 13, 9.8), skin), El((7.2, 13, 6.8), (8.8, 15.4, 9.2), skin),
        El((1.4, 7.8, 6.2), (5, 11, 9.8), skin, rot=('z', -22.5, (5, 9.4, 8))), El((-.4, 8.4, 6.8), (1.6, 10.2, 9.2), skin, rot=('z', -22.5, (5, 9.4, 8))),
        El((11, 7.8, 6.2), (14.6, 11, 9.8), skin, rot=('z', 22.5, (11, 9.4, 8))), El((14.4, 8.4, 6.8), (16.4, 10.2, 9.2), skin, rot=('z', 22.5, (11, 9.4, 8))),
        El((3.6, 2.6, 6.2), (7, 6, 9.8), skin, rot=('z', 22.5, (6, 5.4, 8))), El((2.4, .8, 6.8), (4.6, 3.2, 9.2), skin, rot=('z', 22.5, (6, 5.4, 8))),
        El((9, 2.6, 6.2), (12.4, 6, 9.8), skin, rot=('z', -22.5, (10, 5.4, 8))), El((11.4, .8, 6.8), (13.6, 3.2, 9.2), skin, rot=('z', -22.5, (10, 5.4, 8))),
        El((7.6, 15.2, 7.6), (8.4, 17, 8.4), noise(LEATHER_BROWN[1:], 1, .5)),
        El((8.4, 15.6, 7), (12, 16.2, 9), leaf, rot=('z', 22.5, (8.4, 15.9, 8))),
    ]
    return ItemModel(els, S=2, frames=8, frametime=4), compact(gui_scale=.9, gui_rot=(8, 20, 0), gui_y=-.2)

def frost_sorbet():
    cup = metal(SILVER)
    def scoop(cols, seed):
        return noise(ramp(*cols), 1.4, .45, 1.3)
    blue = scoop(('#3f8fd0', '#66b8ea', '#95d8f8', '#c6eeff', '#eefbff'), 1)
    violet = scoop(('#6a4cc4', '#8b6fe0', '#ad97f2', '#d2c4ff', '#f0eaff'), 2)
    mint = scoop(('#1f9c8a', '#38c4ac', '#6ee2cc', '#aef5e6', '#e2fff8'), 3)
    ice = crystal(ICE[2:], 1.2, glow=.8)
    els = [
        El((5.4, 0, 5.4), (10.6, 1, 10.6), cup), El((7.2, 1, 7.2), (8.8, 4, 8.8), cup), El((5, 4, 5), (11, 5, 11), cup), El((3.8, 5, 3.8), (12.2, 7.6, 12.2), cup, decals={'*': [trim(SILVER)]}),
        El((4.2, 7.6, 4.2), (8.8, 11.4, 8.8), blue), El((8, 7.6, 7.2), (12, 11, 11.4), violet), El((5.4, 10.4, 6.4), (10.6, 14.4, 11), mint),
        El((6.4, 14.4, 7.4), (9.4, 15.4, 10), mint),
        El((9.4, 11.6, 4.6), (10.8, 15.6, 6), ice, rot=('z', -22.5, (10, 11.6, 5.3)), glow=True),
        El((4.6, 11, 9.8), (5.8, 14, 11), ice, rot=('z', 22.5, (5.2, 11, 10.4)), glow=True),
        El((10.6, 9, 4.8), (11.6, 15.8, 5.4), noise(ramp('#8a1030', '#d0264a', '#ff6a80'), .7, .5), rot=('z', -22.5, (11, 9, 5))),
        El((6.6, 5.8, 3.4), (9.4, 7, 3.8), glowing(FROSTGLOW[:4], .4), glow=True),
    ]
    return ItemModel(els, S=2, frames=8, frametime=4), compact(gui_scale=.92, gui_rot=(22, 35, 0))

def honey_pie():
    crust = noise(ramp('#5e3006', '#8d4e0e', '#bb7320', '#e09c3a', '#f7c266', '#ffdf9a'), 1.2, .5, 1.5)
    honey = noise(ramp('#a85a00', '#d88400', '#f8ac0c', '#ffcb3c', '#ffe88a', '#fffbe0'), 2.6, .12, 1.2)
    shine = glowing(GOLD[3:], .4)
    berry = noise(RUBY[:4], .8, .4, 1.4)
    dish = metal(SILVER)
    els = [
        El((2, 0, 2), (14, 1, 14), dish), El((2.8, 1, 2.8), (13.2, 5, 13.2), crust),
        El((3.8, 5, 3.8), (12.2, 5.8, 12.2), honey),
        El((2.4, 5, 2.4), (13.6, 6.4, 3.8), crust), El((2.4, 5, 12.2), (13.6, 6.4, 13.6), crust),
        El((2.4, 5, 3.8), (3.8, 6.4, 12.2), crust), El((12.2, 5, 3.8), (13.6, 6.4, 12.2), crust),
        El((3.8, 5.8, 5.4), (12.2, 6.5, 6.6), crust), El((3.8, 5.8, 9.4), (12.2, 6.5, 10.6), crust),
        El((5.4, 5.8, 3.8), (6.6, 6.6, 12.2), crust), El((9.4, 5.8, 3.8), (10.6, 6.6, 12.2), crust),
        El((7.2, 6.4, 7.2), (8.8, 7.8, 8.8), berry), El((4, 6.2, 3.8), (5.4, 7.4, 5.2), berry), El((10.8, 6.2, 10.8), (12.2, 7.4, 12.2), berry),
        El((7.2, 2, 2.4), (8.2, 5, 2.8), honey), El((10.6, 3, 2.4), (11.4, 5, 2.8), honey), El((13.2, 2.4, 9), (13.6, 5, 10), honey),
        El((8.2, 7.8, 7.8), (11.4, 8.4, 9), noise(EMERALD[:4], 1.0, .3, 1.2), rot=('z', 22.5, (8.2, 8, 8))),
        El((6.9, 6.45, 4.2), (8.3, 6.6, 5), shine, glow=True),
    ]
    return ItemModel(els, S=2), compact(gui_scale=.96, gui_rot=(32, 38, 0), gui_y=.8)

def mana_potion():
    def liquid(c):
        g = np.clip((c.y - 1) / 9, 0, 1)
        v = g * .5 + .2 + (vnoise(c.P + np.array([0, -c.t * 3, 0]), 1.5, 211) - .5) * .45
        img = paint(c, v, ramp('#140a62', '#2416a8', '#3434e6', '#4c74ff', '#76b4ff', '#c8eeff'))
        bub = hash3(np.floor((c.P + np.array([0, -c.t * 4, 0])) * c.S * .75), 213) > .968
        img[bub, :3] = rgb('#eaffff')
        if c.face in ('front', 'back', 'left', 'right') and c.W >= 6:
            img[1:-1, 1:3, :3] = np.clip(img[1:-1, 1:3, :3] * .35 + 165, 0, 255)
        return img
    liq = Mat(liquid, glow=lambda c, img: np.full((c.H, c.W), .55))
    glass = noise(ramp('#47608a', '#6d8cba', '#9cbbe0', '#d0e6fb'), 1.4, .3, 1.8)
    gold = metal(GOLD)
    els = [
        El((4.6, 0, 4.6), (11.4, 1.2, 11.4), liq), El((3.2, 1.2, 3.2), (12.8, 8, 12.8), liq), El((4.6, 8, 4.6), (11.4, 9.4, 11.4), liq),
        El((6.4, 9.4, 6.4), (9.6, 12.4, 9.6), glass), El((5.8, 12.4, 5.8), (10.2, 13.6, 10.2), gold),
        El((6.9, 13.6, 6.9), (9.1, 15.6, 9.1), noise(LEATHER_BROWN[2:], .8, .7)),
        El((6.2, 9.6, 6.2), (9.8, 10.4, 9.8), gold),
        El((6.6, 3.2, 2.6), (9.4, 6, 3.2), glowing(FROSTGLOW, .8), rot=('z', 45, (8, 4.6, 3)), glow=True),
        El((11.8, 11.8, 7.5), (13.2, 13.2, 8.5), glowing(FROSTGLOW[1:], 1.2, .5), rot=('z', 45, (12.5, 12.5, 8)), glow=True),
        El((2.6, 10, 8.5), (3.8, 11.2, 9.5), glowing(FROSTGLOW[1:], 1.2, .1), rot=('z', 45, (3.2, 10.6, 9)), glow=True),
    ]
    return ItemModel(els, S=2, frames=8, frametime=3), compact(gui_scale=.92, gui_rot=(22, 35, 0))

# ============================================================ summoning items
def drake_egg():
    shell = scales(ramp('#12050a', '#260a10', '#431018', '#661a1c', '#8c2a1e', '#b2421f'), 5, 4, seed=215)
    lava = glowing(FIRE[2:], .8, scale=1.2)
    rock = stone(ramp('#0c0a10', '#18141c', '#28222e', '#3c3442'))
    crack = ['...o....', '..oo....', '..o.....', '.ooo....', '...oo...', '....o...', '...oo..o', '...o..oo']
    c = stamp(crack, {'o': ('#ff9a1f', True)})
    els = [
        El((5, 2.5, 5), (11, 3.5, 11), shell), El((4.2, 3.5, 4.2), (11.8, 9.5, 11.8), shell),
        El((4.8, 9.5, 4.8), (11.2, 11.8, 11.2), shell), El((5.6, 11.8, 5.6), (10.4, 13.6, 10.4), shell), El((6.6, 13.6, 6.6), (9.4, 14.8, 9.4), shell),
        El((3.6, 5, 5), (4.2, 8.5, 11), shell, decals={'left': [c]}), El((11.8, 5, 5), (12.4, 8.5, 11), shell, decals={'right': [c]}),
        El((5, 4.6, 3.6), (11, 9, 4.2), shell, decals={'front': [c]}), El((5, 4.6, 11.8), (11, 9, 12.4), shell, decals={'back': [c]}),
        El((2.6, 0, 2.6), (13.4, 2.6, 13.4), rock),
        El((2.2, 1.6, 6), (4, 4.6, 8.4), lava, rot=('z', 22.5, (3, 2, 7)), glow=True), El((11.8, 1.6, 8), (13.6, 4, 10), lava, rot=('z', -22.5, (13, 2, 9)), glow=True),
        El((6.4, 14.6, 7.6), (7.2, 17, 8.4), metal(ramp('#4a3520', '#8a6a44', '#d6b886', '#fff0d0')), rot=('z', 22.5, (6.8, 14.6, 8))),
    ]
    return ItemModel(els, S=2, frames=8, frametime=4), compact(gui_scale=.9, gui_rot=(18, 30, 0))

def spirit_charm():
    silver = metal(SILVER)
    orb = crystal(FROSTGLOW[:5], 1.6, seed=217, glow=.95)
    cord = noise(LEATHER_NAVY[1:], .8, .5)
    els = [
        El((4.6, 3, 7), (11.4, 10, 9), silver, decals={'*': [trim(SILVER)]}),
        El((5.4, 3.8, 6.4), (10.6, 9.2, 9.6), orb, rot=('z', 45, (8, 6.5, 8)), glow=True),
        # fox ears
        El((3.6, 9.4, 7.2), (6.2, 12, 8.8), silver, rot=('z', 22.5, (5, 10, 8))), El((9.8, 9.4, 7.2), (12.4, 12, 8.8), silver, rot=('z', -22.5, (11, 10, 8))),
        El((7.4, 10, 7.5), (8.6, 12.6, 8.5), silver), El((5.6, 12.4, 7.7), (10.4, 13.2, 8.3), cord),
        El((5, 13, 7.7), (5.8, 15.4, 8.3), cord), El((10.2, 13, 7.7), (11, 15.4, 8.3), cord), El((5.6, 15.2, 7.7), (10.4, 16, 8.3), cord),
        El((7.2, 0, 7.4), (8.8, 3, 8.6), orb, rot=('z', 45, (8, 1.8, 8)), glow=True),
        El((2.4, 2, 7.6), (3.6, 4.6, 8.4), orb, rot=('z', -22.5, (3, 3, 8)), glow=True), El((12.4, 2, 7.6), (13.6, 4.6, 8.4), orb, rot=('z', 22.5, (13, 3, 8)), glow=True),
    ]
    return ItemModel(els, S=2, frames=8, frametime=4), compact(gui_scale=.92, gui_rot=(5, 20, 0))

def lich_phylactery():
    gold = metal(ramp('#33230a', '#5e4310', '#8f6a18', '#c29526', '#ecc24a', '#fff09a'))
    soul = glowing(ramp('#0a5c3a', '#12a05a', '#2ee07e', '#7dffae', '#d4ffe6'), 1.0, scale=1.4)
    bone = noise(ramp('#8a8168', '#b5ac8e', '#d8d0b0', '#f2ecd4'), 1.2, .35, 1.4)
    skull = stamp(['.kk..kk.', '.kg..gk.', '....k...', '.k.k.k..'], {'k': '#0c0a06', 'g': ('#4dff95', True)})
    els = [
        El((4, 0, 4), (12, 1.6, 12), gold), El((5, 1.6, 5), (11, 3, 11), gold),
        El((5.4, 3, 5.4), (10.6, 9, 10.6), soul, glow=True),
        El((4.4, 3, 4.4), (5.8, 9.4, 5.8), gold), El((10.2, 3, 4.4), (11.6, 9.4, 5.8), gold), El((4.4, 3, 10.2), (5.8, 9.4, 11.6), gold), El((10.2, 3, 10.2), (11.6, 9.4, 11.6), gold),
        El((4, 9, 4), (12, 10.4, 12), gold),
        El((5.4, 10.4, 5.4), (10.6, 14.6, 10.6), bone, decals={'front': [skull]}),
        El((7.4, 14.6, 7.4), (8.6, 16.8, 8.6), soul, glow=True),
        El((3.4, 10, 7.4), (4.8, 13.6, 8.6), gold, rot=('z', 22.5, (4.4, 10.4, 8))), El((11.2, 10, 7.4), (12.6, 13.6, 8.6), gold, rot=('z', -22.5, (11.6, 10.4, 8))),
    ]
    return ItemModel(els, S=2, frames=8, frametime=3), compact(gui_scale=.9, gui_rot=(22, 28, 0))

NEW_ITEMS = {
    'weapons': {'void_reaver': void_reaver, 'arcane_staff': arcane_staff, 'life_staff': life_staff, 'elven_bow': elven_bow},
    'feast': {'star_fruit': star_fruit, 'frost_sorbet': frost_sorbet, 'honey_pie': honey_pie, 'mana_potion': mana_potion},
    'creatures': {'drake_egg': drake_egg, 'spirit_charm': spirit_charm, 'lich_phylactery': lich_phylactery},
}
