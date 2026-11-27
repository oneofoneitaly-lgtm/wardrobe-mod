from palette import *
from design_items import SCALE

# ============================================================ golden griffin
def golden_griffin():
    plume = ramp('#3d2008', '#693a0e', '#9a5c16', '#c98422', '#eaae3a', '#ffd666', '#ffeda6')
    white = ramp('#c3b8a2', '#d9d0bd', '#e9e3d5', '#f7f3e9', '#ffffff')
    tawny = ramp('#5e3c14', '#85591f', '#ab792f', '#c99844', '#e3b862', '#f4d388')
    beakc = ramp('#7a4700', '#b87400', '#eea40c', '#ffc933', '#ffe37d')
    claw = ramp('#120d0a', '#2a211b', '#463a30', '#6a5a4a')
    legc = ramp('#7d4d08', '#ad7311', '#d99f20', '#f4c340', '#ffe07c')
    body = tiles(plume, 5, 6, grad=(1, 8, 23))
    ruff = tiles(white, 4, 6, seed=7, grad=(1, 7, 24))
    headm = tiles(white, 4, 4, seed=9, grad=(1, 18, 32))
    lion = fur(ramp('#4a2c0e', '#6e4515', '#966422', '#b98534', '#d8a74c', '#f0c56c', '#ffdf95'), grad=(1, 1, 21))
    beak = metal(beakc, grad=(1, 18, 29), sheen=.18, bv=1.0)
    talon = noise(claw, .8, .5, 1.2)
    shank = scales(legc, 4, 3, seed=12)
    gold = metal(GOLD)
    saddle = noise(LEATHER_RED[1:], 1.4, .45, 1.6)
    gem = glowing(FROSTGLOW[:4], .4)
    brow = tiles(plume[:5], 3, 3, seed=3)

    eye_pal = {'k': '#1c0f02', 'y': ('#ffd21f', True), 'o': ('#ff8c00', True), 'w': ('#ffffff', True), 'b': '#4a2c08'}
    def eye(front_is_right):
        art = ['bbbbbb..', '.kkkkkb.', 'kwyyok..', 'kyykok..', 'kooook..', '.kkkk...']
        return stamp(art, eye_pal, -3 if front_is_right else 2, 3, flip=not front_is_right)

    def wing_cut(zmax, xin, xout, tooth, depth):
        def f(c):
            ax = np.abs(c.x)
            k = np.mod(ax - xin, tooth) / tooth
            edge = zmax - depth * np.abs(k - .5) * 2 - np.clip((ax - xin) / (xout - xin), 0, 1) ** 3 * 3
            return c.z > edge
        return cut(f)

    def membrane(zmax, xin, xout, seed):
        def f(c):
            z0 = -5.0
            t = np.clip((c.z - z0) / (zmax - z0), 0, 1)
            ax = np.abs(c.x)
            q = ax / 2.4
            lane = np.floor(q)
            fx = q - lane
            row = np.where(t < .3, 0, np.where(t < .62, 1, 2))
            r = hash3(np.stack([lane, row, lane * 0], -1), seed)
            v = np.where(row == 0, .5, np.where(row == 1, .66, .32)) + r * .14
            local = np.where(row == 0, t / .3, np.where(row == 1, (t - .3) / .32, (t - .62) / .38))
            v = v + local * .2
            v = np.where((fx < .13) & (local > .2), v - .3, v)
            v = np.where(np.abs(fx - .55) < .09, v + .17, v)
            v = np.where((local > .86 - np.abs(fx - .5) * .35) & (row < 2), v - .3, v)
            img = paint(c, v, plume)
            tips = (t > .84) & (ax > xin + (xout - xin) * .0)
            img[tips] = paint(c, v + .4, ramp('#7a4a06', '#b57c10', '#eab32a', '#ffd95a', '#fff0aa'))[tips]
            return img
        return Mat(f)

    def wing(side):
        s = side  # -1 = right wing at negative x
        n = 'r' if s < 0 else 'l'
        X = lambda a, b: (min(s * a, s * b), max(s * a, s * b))
        def bx(x, y0, y1, z0, z1, mat, decals=None):
            a, b = X(*x)
            return Box((a, y0, z0), (b, y1, z1), mat, decals)
        mem1 = membrane(10, 6, 18, 3)
        mem2 = membrane(11, 18, 30, 4)
        c1 = wing_cut(10, 6, 18, 2.4, 2.6)
        c2 = wing_cut(11, 18, 30, 2.4, 3.4)
        c3 = wing_cut(8, 30, 38, 2.0, 4.2)
        inner = Part(n + '_wing', (s * 6, 21, -4), [
            bx((6, 18), 20, 23, -7, -4, body),
            bx((6, 18), 22, 23, -4, 1, tiles(plume[1:], 4, 5, seed=21)),
            bx((6, 18), 20.5, 21.5, -5, 10, mem1, {'*': [c1]}) if False else bx((6, 18), 21, 22, -5, 10, mem1, {'*': [c1]}),
        ])
        outer = Part(n + '_wing_tip', (s * 18, 21.5, -5.5), [
            bx((18, 30), 20.5, 22.5, -7, -5, body),
            bx((18, 30), 21.2, 22.2, -5.5, 11, mem2, {'*': [c2]}),
            bx((30, 38), 21.1, 22.1, -6.5, 8, membrane(8, 30, 38, 5), {'*': [c3]}),
        ])
        inner.children.append(outer)
        return inner

    def front_leg(s):
        n = 'right' if s < 0 else 'left'
        x = s * 4.5
        def B(a, b, mat, d=None):
            return Box((x + a[0], a[1], a[2]), (x + b[0], b[1], b[2]), mat, d)
        return Part(n + '_front_leg', (x, 13, -6), [
            B((-2.5, 7, -8.5), (2.5, 14, -3.5), body),
            B((-1.5, 2, -8), (1.5, 8, -5), shank),
            B((-2, .0, -8.5), (2, 2, -4.5), shank),
            B((-2.7, 0, -11.5), (-1.7, 1, -8.5), shank), B((-.5, 0, -12.5), (.5, 1, -8.5), shank), B((1.7, 0, -11.5), (2.7, 1, -8.5), shank),
            B((-.5, 0, -4.5), (.5, 1, -2.5), shank),
            B((-2.7, 0, -12.5), (-1.7, 1, -11.5), talon), B((-.5, 0, -13.5), (.5, 1, -12.5), talon), B((1.7, 0, -12.5), (2.7, 1, -11.5), talon),
        ])

    def hind_leg(s):
        n = 'right' if s < 0 else 'left'
        x = s * 4
        def B(a, b, mat, d=None):
            return Box((x + a[0], a[1], a[2]), (x + b[0], b[1], b[2]), mat, d)
        toes = stamp(['..k..k..', '..k..k..'], {'k': '#3b240a'}, None, -1)
        return Part(n + '_hind_leg', (x, 14, 8), [
            B((-2.5, 7, 4), (2.5, 16, 11), lion),
            B((-3, 11, 3.5), (3, 17, 10.5), lion) if False else B((s * 2.5 - .6, 9, 5), (s * 2.5 + .4, 15, 10), lion),
            B((-1.5, 2, 7.5), (1.5, 8, 11.5), lion),
            B((-2, 0, 5.5), (2, 3, 11.5), lion, {'front': [toes]}),
            B((-1.7, 0, 4.5), (-.7, 1, 5.5), talon), B((.7, 0, 4.5), (1.7, 1, 5.5), talon),
        ])

    head = Part('head', (0, 25, -12), [
        Box((-4, 22, -18), (4, 30, -10), headm, {'left': [eye(True)], 'right': [eye(False)],
            'front': [stamp(['kk..........kk', 'kyk........kyk', 'kyok......koyk', '.kkk......kkk.'], eye_pal, None, 4)]}),
        Box((-4.5, 27, -18.5), (-3.5, 29, -13.5), brow), Box((3.5, 27, -18.5), (4.5, 29, -13.5), brow),
        Box((-2, 23, -22), (2, 27, -18), beak, {'left': [stamp(['kk'], {'k': '#3a2200'}, -2, 2)], 'right': [stamp(['kk'], {'k': '#3a2200'}, 2, 2)]}),
        Box((-1.5, 21, -23.5), (1.5, 25, -21.5), beak),
        Box((-.5, 19.5, -23.5), (.5, 21.5, -22.5), metal(beakc[:3], sheen=0)),
    ], [
        Part('jaw', (0, 23, -17), [Box((-1.5, 21, -21), (1.5, 23, -18), metal(beakc[:4], sheen=0))]),
        Part('r_ear', (-3, 29.5, -12.5), [Box((-3.5, 29, -13), (-2.5, 35, -11), tiles(plume[2:], 3, 3)), Box((-3.5, 35, -12.5), (-2.5, 37, -11.5), gold)], rot=(38, 0, 22)),
        Part('l_ear', (3, 29.5, -12.5), [Box((2.5, 29, -13), (3.5, 35, -11), tiles(plume[2:], 3, 3)), Box((2.5, 35, -12.5), (3.5, 37, -11.5), gold)], rot=(38, 0, -22)),
        Part('crest', (0, 30, -14), [Box((-1, 29.5, -15), (1, 31.5, -8), tiles(plume[3:], 3, 4)), Box((-.5, 30.5, -13), (.5, 34.5, -8), tiles(plume[2:], 3, 3, seed=4))], rot=(18, 0, 0)),
        Part('forehead_gem', (0, 28.5, -18), [Box((-1, 27.5, -18.6), (1, 29.5, -17.6), gem)], rot=(0, 0, 45)),
    ])
    neck = Part('neck', (0, 20, -8), [
        Box((-3.5, 17, -13), (3.5, 26, -7), ruff),
        Box((-4.5, 17, -12.5), (4.5, 21, -6.5), ruff),
    ], [head])
    collar = Part('collar', (0, 17, -10), [
        Box((-5, 17, -13.5), (5, 19, -6), gold, {'*': [trim(GOLD[:5])]}),
        Box((-1.5, 14.5, -14), (1.5, 17.5, -13), gem),
    ])
    saddle_p = Part('saddle', (0, 21, 0), [
        Box((-6.5, 20.6, -3), (6.5, 22.6, 6), saddle, {'top': [trim(GOLD)]}),
        Box((-2, 22.6, -3), (2, 24.6, -1), gold),
        Box((-4, 22.6, 4), (4, 24.6, 6), saddle, {'*': [trim(GOLD, 1, 't')]}),
        Box((-6.8, 12, -2), (-5.8, 21, 4), saddle, {'left': [trim(GOLD)]}),
        Box((5.8, 12, -2), (6.8, 21, 4), saddle, {'right': [trim(GOLD)]}),
        Box((-6.4, 9.4, -.5), (6.6, 10.4, 1.5), noise(LEATHER_BROWN[1:], 1, .5)),
    ])
    tail3 = Part('tail_tip', (0, 17, 26), [
        Box((-2.5, 14.5, 25), (2.5, 19.5, 30), tiles(plume[2:], 4, 5, seed=15)),
        Box((-1.5, 15.5, 30), (1.5, 18.5, 33), tiles(GOLD[2:], 3, 4, seed=16)),
        Box((-.5, 16.5, 33), (.5, 17.5, 36), gold),
    ])
    tail2 = Part('tail_mid', (0, 17, 19), [Box((-1, 16, 18.5), (1, 18, 26), lion)], [tail3], rot=(-22, 0, 0))
    tail = Part('tail', (0, 17, 12), [Box((-1.5, 15.5, 11.5), (1.5, 18.5, 19.5), lion)], [tail2], rot=(12, 0, 0))
    torso = Part('body', (0, 16, 0), [
        Box((-6, 10, -10), (6, 21, 1), body),
        Box((-5, 10.5, 1), (5, 19.5, 12), lion),
        Box((-5.5, 9, -11.5), (5.5, 18, -8.5), ruff),
        Box((-3.5, 8, -11), (3.5, 10, -7), ruff),
    ], [saddle_p, collar])
    parts = [torso, neck, front_leg(-1), front_leg(1), hind_leg(-1), hind_leg(1), tail, wing(-1), wing(1)]
    return EntityModel(parts, S=2, width=256)

GRIFFIN_SHOW = {
    'r_wing': (8, -8, -26), 'l_wing': (8, 8, 26),
    'r_wing_tip': (0, -12, -18), 'l_wing_tip': (0, 12, 18),
}

# ============================================================ crystal golem
def crystal_golem():
    rock = stone(ramp('#0c0e17', '#171a29', '#242940', '#363d58', '#4d5675', '#6b7596', '#8f98b6'))
    rock2 = stone(ramp('#14172a', '#222845', '#343d63', '#4a5584', '#6973a3'), seed=44)
    cyan = ramp('#06344f', '#08607e', '#0d92ad', '#18c4d2', '#5aeee9', '#b8fff9', '#ffffff')
    cry = crystal(cyan[:6], 2.6, seed=21, glow=.65)
    cry2 = crystal(AMETHYST, 2.4, seed=25, glow=.55)
    core = glowing(cyan[2:], .9, scale=1.8)
    eyes = glowing(FROSTGLOW, .7)
    vein_pal = {'c': ('#37e6ee', True), 'd': ('#12929f', True)}
    veins = [
        stamp(['c.......', 'cc......', '.cd.....', '..cc....', '...ccd..', '....cc..', '.....c..', '.....cc.', '......d.'], vein_pal, 2, 3),
        stamp(['.....c', '....cc', '...dc.', '..cc..', '.cc...', '.c....', 'cd....'], vein_pal, -3, -3),
    ]
    def shard(name, base, size, h, rot, mat, tipm=None):
        x, y, z = base; s = size
        return Part(name, base, [
            Box((x - s / 2, y - 1, z - s / 2), (x + s / 2, y + h, z + s / 2), mat),
            Box((x - (s - 2) / 2, y + h, z - (s - 2) / 2), (x + (s - 2) / 2, y + h + max(2, h // 3), z + (s - 2) / 2), tipm or mat),
        ], rot=rot)

    head = Part('head', (0, 38, -4), [
        Box((-5, 37, -11), (5, 46, -2), rock2, {'front': [stamp(['kkkkkkkkkkkkkkkkkk', 'kwcccckkkkkkccccwk', 'kccccckkkkkkccccck', '.kkkkkkkkkkkkkkkk.'], {'k': '#05060c', 'c': ('#57f4f4', True), 'w': ('#ffffff', True)}, None, 6)]}),
        Box((-6, 43, -12), (6, 46, -9), rock),
        Box((-4, 35, -11.5), (4, 39, -8), rock),
        Box((-1, 41, -12.6), (1, 43, -11.6), core),
    ], [
        shard('crown_c', (0, 46, -6), 4, 6, (8, 45, 0), cry),
        shard('crown_r', (-4, 45, -6), 3, 4, (0, 30, 28), cry2),
        shard('crown_l', (4, 45, -6), 3, 4, (0, -30, -28), cry2),
        shard('crown_b', (0, 45, -3), 3, 3, (35, 45, 0), cry2),
    ])
    torso = Part('torso', (0, 20, 0), [
        Box((-5, 16, -4), (5, 23, 4), rock),
        Box((-9, 22, -6), (9, 27, 6), rock2, {'front': veins[:1]}),
        Box((-12, 26, -7), (12, 39, 7), rock, {'front': veins, 'back': veins, 'left': veins[:1], 'right': veins[1:]}),
        Box((-6, 27.5, -8), (6, 37.5, -7), rock2),
        Box((-3, 29.5, -9), (3, 35.5, -8), core),
        Box((-13, 36, -8), (-5, 40, 8), rock2), Box((5, 36, -8), (13, 40, 8), rock2),
    ], [
        head,
        Part('core_frame', (0, 32.5, -9), [Box((-2, 30.5, -10), (2, 34.5, -9), cry)], rot=(0, 0, 45)),
        shard('back_a', (-5, 37, 5), 6, 12, (-30, 25, 18), cry),
        shard('back_b', (5, 36, 6), 5, 10, (-38, -20, -24), cry2),
        shard('back_c', (0, 30, 7), 5, 9, (-60, 45, 0), cry),
        shard('shoulder_r', (-10, 39, 1), 5, 8, (-8, 20, 30), cry),
        shard('shoulder_l', (10, 39, 1), 4, 6, (-8, -20, -34), cry),
        shard('shoulder_r2', (-7, 39, 4), 4, 5, (-22, 45, 8), cry2),
        shard('shoulder_l2', (8, 39, -2), 4, 5, (10, 45, -12), cry2),
    ])
    def arm(s):
        n = 'right' if s < 0 else 'left'
        x = s * 16.5
        def B(a, b, mat, d=None):
            return Box((x + a[0], a[1], a[2]), (x + b[0], b[1], b[2]), mat, d)
        kids = [
            shard(n + '_elbow', (x + s * 3, 22, 3), 4, 6, (-40, 20, -s * 40), cry),
            shard(n + '_fist', (x + s * 3.5, 15, -3), 3, 4, (20, 45, -s * 55), cry2),
        ]
        return Part(n + '_arm', (s * 13, 35, 0), [
            B((-4, 30, -5), (4, 38, 5), rock2),
            B((-3, 21, -3.5), (3, 31, 3.5), rock, {'front': veins[1:]}),
            B((-5, 8, -5.5), (5, 22, 5.5), rock, {'front': veins[:1], 'left': veins[1:], 'right': veins[1:]}),
            B((-5.5, 17, -6), (5.5, 19, 6), cry),
            B((-4.5, 4, -6.5), (4.5, 9, 3.5), rock2),
            B((-4, 3, -7), (-2, 6, -5), cry), B((-1, 3, -7), (1, 6, -5), cry), B((2, 3, -7), (4, 6, -5), cry),
        ], kids)
    def leg(s):
        n = 'right' if s < 0 else 'left'
        x = s * 6
        def B(a, b, mat, d=None):
            return Box((x + a[0], a[1], a[2]), (x + b[0], b[1], b[2]), mat, d)
        return Part(n + '_leg', (s * 5, 17, 0), [
            B((-3.5, 8, -3.5), (3.5, 18, 3.5), rock),
            B((-4.5, 0, -5.5), (4.5, 9, 4.5), rock2, {'front': veins[:1]}),
            B((-2, 8, -5), (2, 11, -3), cry),
        ])
    return EntityModel([torso, arm(-1), arm(1), leg(-1), leg(1)], S=2, width=256)

# ============================================================ dragon armour (worn)
def dragon_armour():
    sc = scales(SCALE, 6, 4, seed=61)
    scd = scales(SCALE[:6], 5, 4, seed=71)
    gold = metal(GOLD)
    horn = metal(ramp('#4a3520', '#7a5c38', '#ab8a5c', '#d6b886', '#f4e3ba', '#fff8e4'), grad=(1, 22, 40), sheen=.16)
    claw = metal(ramp('#4a3822', '#8d7048', '#d4b988', '#f4e2bc'), sheen=.1)
    gem = glowing(FIRE[2:], .6)
    def hole(ctx, img, glow):
        W, H = ctx.W, ctx.H
        x, y = ctx.x, ctx.y
        m = ((np.abs(x) < 3.2) & (y < 29.2) & (y > 24.0)) | ((np.abs(x) < 1.0) & (y < 24.5))
        nose = (np.abs(x) < .55) & (y > 26.2)
        m = m & ~nose
        rim = np.zeros_like(m)
        rim[1:, :] |= m[:-1, :]; rim[:-1, :] |= m[1:, :]; rim[:, 1:] |= m[:, :-1]; rim[:, :-1] |= m[:, 1:]
        rim &= ~m
        img[rim] = np.append(rgb('#e8ac1c'), 255)
        img[nose] = np.append(rgb('#ffd13f'), 255)
        img[m] = 0; glow[m] = 0
    open_bottom = cut(lambda c: np.ones((c.H, c.W), bool))

    def side(s):
        return lambda a, b: ((min(s * a, s * b)), (max(s * a, s * b)))

    helmet = [Part('head', (0, 24, 0), [
        Box((-4, 24, -4), (4, 32, 4), sc, {'front': [hole, trim(GOLD, 1, 't')], 'bottom': [open_bottom], 'left': [trim(GOLD, 1, 'b')], 'right': [trim(GOLD, 1, 'b')], 'back': [trim(GOLD, 1, 'b')]}, inflate=1.0),
    ], [
        Part('crest', (0, 32, 0), [Box((-1, 32.4, -5.6), (1, 34.4, 4.4), gold), Box((-.5, 34.4, -3), (.5, 36.4, -1), scd), Box((-.5, 34.4, 0), (.5, 36.4, 2), scd), Box((-.5, 34.4, 3), (.5, 35.4, 5), scd)]),
        Part('brow', (0, 30, -5), [Box((-5, 29.6, -5.8), (5, 30.6, -4.8), gold), Box((-1, 30.2, -6.4), (1, 32.2, -5.4), gem)]),
        Part('r_horn', (-5, 30, 0), [Box((-8, 29, -1.5), (-5, 32, 1.5), horn)], [
            Part('r_horn_mid', (-8, 30.5, 0), [Box((-9, 30, -1), (-7, 36, 1), horn)], [
                Part('r_horn_tip', (-8, 36, 0), [Box((-8.5, 35.5, -.5), (-7.5, 40.5, .5), horn)], rot=(-28, 0, 14))], rot=(-14, 0, 8))]),
        Part('l_horn', (5, 30, 0), [Box((5, 29, -1.5), (8, 32, 1.5), horn)], [
            Part('l_horn_mid', (8, 30.5, 0), [Box((7, 30, -1), (9, 36, 1), horn)], [
                Part('l_horn_tip', (8, 36, 0), [Box((7.5, 35.5, -.5), (8.5, 40.5, .5), horn)], rot=(-28, 0, -14))], rot=(-14, 0, -8))]),
        Part('r_fin', (-5, 26, 1), [Box((-6.4, 24, 0), (-5.4, 28, 5), scd)], rot=(0, -28, 0)),
        Part('l_fin', (5, 26, 1), [Box((5.4, 24, 0), (6.4, 28, 5), scd)], rot=(0, 28, 0)),
    ])]

    def membrane(s):
        def f(c):
            ax = np.abs(c.x)
            u = np.clip((ax - 2) / 18.0, 0, 1)
            v = vnoise(c.P, 3.0, 81) * .45 + vnoise(c.P, 1.0, 83) * .15 + .08 + u * .3
            img = paint(c, v, ramp('#220308', '#44070d', '#6e0e14', '#9c1b1d', '#c63526', '#ec6234', '#ff9a4e'))
            top = 22.5 + np.minimum(ax - 2, 12) * .9 - np.clip(ax - 14, 0, None) * 1.2
            rim = (c.y > top - 1.6)
            img[rim] = paint(c, .3 + u * .55, GOLD)[rim]
            for slope in (.38, -.18, -.75):
                line = np.abs((c.y - 22.5) - slope * (ax - 2)) < .55
                img[line] = paint(c, .25 + u * .5, GOLD)[line]
            return img
        def edge(c):
            ax = np.abs(c.x)
            top = 22.5 + np.minimum(ax - 2, 12) * .9 - np.clip(ax - 14, 0, None) * 1.2
            k = np.mod(ax - 2, 6.0) / 6.0
            bot = 11.5 + (ax - 2) * .55 + (1 - np.abs(k - .5) * 2) * 4.5
            return (c.y > top) | (c.y < bot)
        a, b = (-21, -3) if s < 0 else (3, 21)
        return Part(('r' if s < 0 else 'l') + '_back_wing', (s * 2, 21, 3.6), [
            Box((a, 11, 3.5), (b, 34, 3.5), Mat(f), {'*': [cut(edge)]}),
            Box((min(s * 2, s * 4), 20.5, 3), (max(s * 2, s * 4), 22.5, 4), gold),
        ], rot=(0, -s * 16, 0))

    chest_body = Part('body', (0, 24, 0), [
        Box((-4, 12, -2), (4, 24, 2), sc, {'front': [trim(GOLD, 1, 't')], 'top': [open_bottom]}, inflate=1.0),
    ], [
        Part('breastplate', (0, 20, -3), [Box((-4, 18, -3.8), (4, 23, -2.8), gold, {'front': [trim(GOLD[:5])]}), Box((-2, 16.4, -3.6), (2, 18.4, -2.8), gold), Box((-.5, 13, -3.4), (.5, 16.4, -2.8), gold)]),
        Part('heart_gem', (0, 20.5, -4), [Box((-1.5, 19, -4.6), (1.5, 22, -3.6), gem)], rot=(0, 0, 45)),
        Part('spine', (0, 18, 3), [Box((-1, 21, 2.8), (1, 23, 4.8), horn), Box((-.5, 17.5, 2.8), (.5, 19.5, 3.8), horn), Box((-.5, 14.5, 2.8), (.5, 16.5, 3.8), horn)]),
        membrane(-1), membrane(1),
    ])
    def arm(s):
        n = 'right' if s < 0 else 'left'
        B = lambda x0, x1, y0, y1, z0, z1, m, d=None, g=0: Box((min(s * x0, s * x1), y0, z0), (max(s * x0, s * x1), y1, z1), m, d, g)
        return Part(n + '_arm', (s * 5, 22, 0), [
            B(4, 8, 12, 24, -2, 2, sc, {'*': []}, .9),
        ], [
            Part(n + '_pauldron', (s * 6.5, 23, 0), [
                B(4.5, 11.5, 22, 26, -4, 4, sc, {'*': [trim(GOLD, 1, 'b')]}),
                B(5.5, 10.5, 26, 27.4, -3, 3, gold),
                B(10.5, 12.5, 20.5, 24, -3, 3, scd, {'*': [trim(GOLD, 1, 'b')]}),
            ], [
                Part(n + '_spike', (s * 9.5, 27, 0), [B(8.5, 10.5, 26.5, 30, -1, 1, horn), B(9, 10, 30, 32.5, -.5, .5, horn)], rot=(0, 0, -s * 24)),
            ], rot=(0, 0, -s * 8)),
            Part(n + '_bracer', (s * 6, 15, 0), [B(3, 9, 12.6, 17, -3.4, 3.4, gold, {'*': [trim(GOLD[:5])]}) if False else B(3, 9, 13.4, 17, -3, 3, gold, {'*': [trim(GOLD[:5])]}), B(8.8, 10.8, 14, 16, -1, 1, claw)]),
        ])
    legs_body = Part('body', (0, 24, 0), [Box((-4, 12, -2), (4, 15, 2), gold, {'*': [trim(GOLD[:5])], 'top': [open_bottom], 'bottom': [open_bottom]}, inflate=.7)],
                     [Part('belt_gem', (0, 13.5, -3), [Box((-1, 12.5, -3.6), (1, 14.5, -2.6), gem)], rot=(0, 0, 45)),
                      Part('tabard', (0, 12.5, -2.8), [Box((-2, 7.5, -3.3), (2, 12.5, -2.8) if False else (2, 12.5, -2.3), scd, {'front': [trim(GOLD, 1, 'blr')]})], rot=(8, 0, 0))])
    def leg(s):
        n = 'right' if s < 0 else 'left'
        x = s * 1.9
        B = lambda x0, x1, y0, y1, z0, z1, m, d=None, g=0: Box((x + x0, y0, z0), (x + x1, y1, z1), m, d, g)
        return Part(n + '_leg', (x, 12, 0), [B(-2, 2, 0, 12, -2, 2, sc, {'bottom': [open_bottom], 'top': [open_bottom], 'front': [cut(lambda c: c.y < 3.0)], 'back': [cut(lambda c: c.y < 3.0)], 'left': [cut(lambda c: c.y < 3.0)], 'right': [cut(lambda c: c.y < 3.0)]}, .55)], [
            Part(n + '_knee', (x, 6, -3), [B(-2, 2, 4.5, 7.5, -3.4, -2.4, gold, {'*': [trim(GOLD[:5])], 'front': [trim(GOLD[:5]), stamp(['.rr.', 'rwrr', 'rrrr', '.rr.'], {'r': ('#ff5a1f', True), 'w': ('#ffe08a', True)})]})]),
        ])
    def boot(s):
        n = 'right' if s < 0 else 'left'
        x = s * 1.9
        B = lambda x0, x1, y0, y1, z0, z1, m, d=None, g=0: Box((x + x0, y0, z0), (x + x1, y1, z1), m, d, g)
        cutf = lambda c: c.y > 6.0
        return Part(n + '_leg', (x, 12, 0), [B(-2, 2, 0, 12, -2, 2, scd, {'top': [open_bottom], 'front': [cut(cutf)], 'back': [cut(cutf)], 'left': [cut(cutf)], 'right': [cut(cutf)]}, .95)], [
            Part(n + '_boot_trim', (x, 6, 0), [B(-2.5, 2.5, 5, 6, -3.4, 3.4, gold) if False else B(-3, 3, 5.2, 6.2, -3.3, 2.7 + .6 if False else 3.7, gold) if False else B(-3, 3, 5.2, 6.2, -3, 3, gold)]),
            Part(n + '_toes', (x, 1, -3), [B(-3, 3, -.9, 2.1, -4.8, -2.8, scd, {'front': [trim(GOLD)]}), B(-2.5, -1.5, -.9, .1, -6.8, -4.8, claw), B(-.5, .5, -.9, .1, -7.2, -4.2) if False else B(-.5, .5, -.9, .1, -6.8, -4.8, claw), B(1.5, 2.5, -.9, .1, -6.8, -4.8, claw)]),
            Part(n + '_heel_fin', (x + s * 3, 4, 0), [B(*((2.8, 3.8) if s > 0 else (-3.8, -2.8)), 3, 8, 0, 3, gold)], rot=(28, 0, 0)),
        ])
    pieces = {
        'helmet': helmet + [Part('body', (0, 24, 0)), Part('right_arm', (-5, 22, 0)), Part('left_arm', (5, 22, 0)), Part('right_leg', (-1.9, 12, 0)), Part('left_leg', (1.9, 12, 0)), Part('hat', (0, 24, 0))],
        'chestplate': [Part('head', (0, 24, 0)), Part('hat', (0, 24, 0)), chest_body, arm(-1), arm(1), Part('right_leg', (-1.9, 12, 0)), Part('left_leg', (1.9, 12, 0))],
        'leggings': [Part('head', (0, 24, 0)), Part('hat', (0, 24, 0)), legs_body, Part('right_arm', (-5, 22, 0)), Part('left_arm', (5, 22, 0)), leg(-1), leg(1)],
        'boots': [Part('head', (0, 24, 0)), Part('hat', (0, 24, 0)), Part('body', (0, 24, 0)), Part('right_arm', (-5, 22, 0)), Part('left_arm', (5, 22, 0)), boot(-1), boot(1)],
    }
    return {k: EntityModel(v, S=2, width=128) for k, v in pieces.items()}

def mannequin():
    wood = noise(ramp('#3a2a1c', '#54402c', '#6e5740', '#8a7156', '#a68d70'), 1.6, .35, 1.2)
    face = stamp(['k....k', 'k....k'], {'k': '#1c130c'}, None, 8)
    B = lambda a, b, d=None: Box(a, b, wood, d)
    return EntityModel([
        Part('head', (0, 24, 0), [B((-4, 24, -4), (4, 32, 4), {'front': [face]})]),
        Part('body', (0, 24, 0), [B((-4, 12, -2), (4, 24, 2))]),
        Part('right_arm', (-5, 22, 0), [B((-8, 12, -2), (-4, 24, 2))]), Part('left_arm', (5, 22, 0), [B((4, 12, -2), (8, 24, 2))]),
        Part('right_leg', (-1.9, 12, 0), [B((-3.9, 0, -2), (.1, 12, 2))]), Part('left_leg', (1.9, 12, 0), [B((-.1, 0, -2), (3.9, 12, 2))]),
    ], S=2, width=64)
