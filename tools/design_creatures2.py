"""Second wave of creatures: Ember Drake, Spirit Fox and the Shadow Lich."""
from design_entities import *

# ============================================================ ember drake
# Uses the same skeleton names as the griffin, so both mounts share their animations.
def ember_drake():
    hide = ramp('#14060a', '#270a0e', '#411013', '#61181a', '#852320', '#aa3326', '#cc4a2e')
    dark = ramp('#07050a', '#110b12', '#1e141c', '#2e1f28', '#432e36')
    belly = ramp('#7a2a00', '#b34a00', '#e2740a', '#ff9d1f', '#ffc247', '#ffe58a')
    horn = metal(ramp('#2a1c12', '#53402c', '#82694a', '#b49a72', '#e0cca2', '#fbf0d2'), sheen=.14)
    body = scales(hide, 6, 4, seed=111, grad=(1, 6, 26))
    back = scales(dark, 5, 4, seed=113)
    gut = tiles(belly, 7, 3, seed=115, tip=False)
    ember = glowing(FIRE[2:], .8, scale=1.4)
    claw = noise(ramp('#0c0a0a', '#221c1a', '#403630', '#6a5c50'), .8, .5, 1.2)
    eye_pal = {'k': '#120300', 'y': ('#ffd21f', True), 'o': ('#ff6a00', True), 'w': ('#ffffff', True)}

    def eye(front_is_right):
        art = ['kkkkkk..', '.kyyyyk.', 'kwyoyyk.', '.kyoyk..', '..kkk...']
        return stamp(art, eye_pal, -2 if front_is_right else 1, 2, flip=not front_is_right)

    def membrane(zmax, xin, xout, tooth, depth):
        def f(c):
            ax = np.abs(c.x)
            t = np.clip((c.z + 5.0) / (zmax + 5.0), 0, 1)
            v = .16 + t * .5 + (vnoise(c.P, 2.6, 117) - .5) * .35 + (vnoise(c.P, .8, 118) - .5) * .12
            img = paint(c, v, ramp('#2a0306', '#52080a', '#84120d', '#b82a0e', '#e65510', '#ff8a1c', '#ffbc4a'))
            for slope in (0.15, 0.62, 1.35):
                line = np.abs((c.z + 5.0) - slope * (xout - ax)) < .6
                img[line] = paint(c, .25 + v * .4, dark)[line]
            return img
        def edge(c):
            ax = np.abs(c.x)
            k = np.mod(ax - xin, tooth) / tooth
            return c.z > zmax - depth * np.abs(k - .5) * 2 - np.clip((ax - xin) / (xout - xin), 0, 1) ** 3 * 4
        return Mat(f), cut(edge)

    def wing(s):
        n = 'r' if s < 0 else 'l'
        def bx(x, y0, y1, z0, z1, mat, decals=None):
            a, b = min(s * x[0], s * x[1]), max(s * x[0], s * x[1])
            return Box((a, y0, z0), (b, y1, z1), mat, decals)
        m1, c1 = membrane(13, 6, 18, 6.0, 4.5)
        m2, c2 = membrane(13, 18, 32, 7.0, 6.0)
        m3, c3 = membrane(8, 32, 42, 5.0, 5.0)
        inner = Part(n + '_wing', (s * 6, 21, -4), [
            bx((6, 18), 20, 23, -7, -4, back),
            bx((6, 18), 21.2, 22.2, -4.5, 13, m1, {'*': [c1]}),
            bx((11, 13), 22.5, 25.5, -7, -5.5, horn),
        ])
        outer = Part(n + '_wing_tip', (s * 18, 21.5, -5.5), [
            bx((18, 32), 20.5, 22.5, -7.5, -5, back),
            bx((18, 32), 21.3, 22.3, -5.5, 13, m2, {'*': [c2]}),
            bx((32, 42), 21, 22, -7, -5.5, back),
            bx((32, 42), 21.2, 22.2, -6, 8, m3, {'*': [c3]}),
            bx((19, 21), 22, 25, -9.5, -7, horn), bx((19.5, 20.5), 24.5, 27.5, -11, -9, horn),
            bx((41, 44), 21, 22, -8, -6.5, horn),
        ])
        inner.children.append(outer)
        return inner

    def front_leg(s):
        n = 'right' if s < 0 else 'left'
        x = s * 5
        B = lambda a, b, m: Box((x + a[0], a[1], a[2]), (x + b[0], b[1], b[2]), m)
        return Part(n + '_front_leg', (x, 13, -6), [
            B((-3, 7, -9), (3, 15, -3), body), B((-2, 2, -8), (2, 8, -4), back), B((-2.5, 0, -10), (2.5, 3, -4), back),
            B((-2.5, 0, -12), (-1.5, 1, -10), claw), B((-.5, 0, -13), (.5, 1, -10), claw), B((1.5, 0, -12), (2.5, 1, -10), claw),
            B((-.5, 8, -3.5), (.5, 11, -1.5), horn),
        ])

    def hind_leg(s):
        n = 'right' if s < 0 else 'left'
        x = s * 4.5
        B = lambda a, b, m: Box((x + a[0], a[1], a[2]), (x + b[0], b[1], b[2]), m)
        return Part(n + '_hind_leg', (x, 14, 8), [
            B((-3, 8, 3), (3, 17, 12), body), B((-2, 2, 7), (2, 9, 12), back), B((-2.5, 0, 5), (2.5, 3, 12), back),
            B((-2.5, 0, 3), (-1.5, 1, 5), claw), B((-.5, 0, 2), (.5, 1, 5), claw), B((1.5, 0, 3), (2.5, 1, 5), claw),
        ])

    def horn_part(n, s):
        return Part(n, (s * 3, 31, -13), [Box((s * 3 - 1.5, 30, -14), (s * 3 + 1.5, 33, -10), horn)], [
            Part(n + '_mid', (s * 3, 31.5, -10), [Box((s * 3 - 1, 30.5, -10.5), (s * 3 + 1, 32.5, -5.5), horn)], [
                Part(n + '_tip', (s * 3, 31.5, -5.5), [Box((s * 3 - .5, 31, -6), (s * 3 + .5, 32, -1), horn)], rot=(-24, 0, 0))], rot=(-16, s * 8, 0))],
            rot=(-18, s * 16, 0))

    head = Part('head', (0, 25, -12), [
        Box((-4, 24, -19), (4, 31, -11), body, {'left': [eye(True)], 'right': [eye(False)]}),
        Box((-4.5, 29, -19.5), (-2, 31.5, -14.5), back), Box((2, 29, -19.5), (4.5, 31.5, -14.5), back),
        Box((-3, 25, -26), (3, 29, -19), body, {'front': [stamp(['kk..kk', 'ko..ok'], eye_pal, None, 2)]}),
        Box((-1, 29, -25), (1, 30.5, -22), back), Box((-.5, 29, -26), (.5, 32, -25), horn),
        Box((-3, 24, -25.5), (-2, 25, -24.5), horn), Box((2, 24, -25.5), (3, 25, -24.5), horn),
        Box((-3, 24, -22.5), (-2, 25, -21.5), horn), Box((2, 24, -22.5), (3, 25, -21.5), horn),
    ], [
        Part('jaw', (0, 24, -18), [Box((-2.5, 22, -25), (2.5, 24, -18), gut), Box((-3, 21.5, -19), (3, 24, -14), body)]),
        horn_part('r_ear', -1), horn_part('l_ear', 1),
        Part('r_frill', (-4, 26, -12), [Box((-9, 23.5, -12.5), (-4, 28.5, -11.5), ember, {'*': [cut(lambda c: (c.y - 23.5) < (np.abs(c.x) - 4) * .8 - .4)]})], rot=(0, -32, 0)),
        Part('l_frill', (4, 26, -12), [Box((4, 23.5, -12.5), (9, 28.5, -11.5), ember, {'*': [cut(lambda c: (c.y - 23.5) < (np.abs(c.x) - 4) * .8 - .4)]})], rot=(0, 32, 0)),
        Part('crest', (0, 31, -14), [Box((-.5, 31, -14), (.5, 34, -12.5), horn), Box((-.5, 31, -11.5), (.5, 33, -10.5), horn)]),
    ])
    neck = Part('neck', (0, 20, -8), [
        Box((-3.5, 17, -13), (3.5, 25, -7), body),
        Box((-3, 22, -15), (3, 28, -10), body),
        Box((-2.5, 16.5, -13.5), (2.5, 22.5, -12.5), gut),
        Box((-2, 20, -15.6), (2, 24, -14.6), gut),
        Box((-.5, 25, -10), (.5, 29, -8.5), horn), Box((-.5, 25, -7.5), (.5, 27.5, -6), horn),
    ], [head])
    collar = Part('collar', (0, 17, -10), [
        Box((-4.5, 16, -14), (4.5, 18, -6), metal(GOLD), {'*': [trim(GOLD[:5])]}),
        Box((-1.5, 13.5, -14.5), (1.5, 16.5, -13.5), ember),
    ])
    saddle = Part('saddle', (0, 21, 0), [
        Box((-6.5, 20.6, -3), (6.5, 22.6, 6), noise(LEATHER_BLACK[1:], 1.4, .45, 1.6), {'top': [trim(GOLD)]}),
        Box((-2, 22.6, -3), (2, 24.6, -1), metal(GOLD)),
        Box((-4, 22.6, 4), (4, 24.6, 6), noise(LEATHER_RED[1:], 1.2, .4), {'*': [trim(GOLD, 1, 't')]}),
        Box((-7, 12, -2), (-6, 21, 4), noise(LEATHER_BLACK[1:], 1.4, .45), {'left': [trim(GOLD)]}),
        Box((6, 12, -2), (7, 21, 4), noise(LEATHER_BLACK[1:], 1.4, .45), {'right': [trim(GOLD)]}),
    ])
    spikes = [Part('spine_%d' % i, (0, 21, z), [Box((-.5, 20.5, z - 1), (.5, 20.5 + h, z + 1), horn), Box((-.5, 20.5 + h, z), (.5, 22 + h, z + 1), horn)])
              for i, (z, h) in enumerate(((-7, 3), (8, 3), (11, 2)))]
    torso = Part('body', (0, 16, 0), [
        Box((-6, 10, -10), (6, 21, 2), body), Box((-5, 10.5, 2), (5, 20, 13), body),
        Box((-4, 9, -9.5), (4, 11, 1), gut), Box((-3.5, 9.5, 1), (3.5, 11, 11), gut),
        Box((-6.3, 13, -7), (-5.8, 15, -2), ember), Box((5.8, 13, -7), (6.3, 15, -2), ember),
    ], [saddle, collar] + spikes)
    tail3 = Part('tail_tip', (0, 17, 27), [
        Box((-1.5, 15.5, 26), (1.5, 18.5, 35), body), Box((-.5, 18.5, 28), (.5, 20.5, 30), horn),
        Box((-5, 16.5, 34), (5, 17.5, 40), ember, {'*': [cut(lambda c: np.abs(c.x) > 5.2 - np.abs(c.z - 36.5) * 1.35)]}),
        Box((-.5, 16.5, 40), (.5, 17.5, 43), horn),
    ])
    tail2 = Part('tail_mid', (0, 17, 20), [Box((-2.5, 14.5, 19), (2.5, 19.5, 28), body), Box((-.5, 19.5, 22), (.5, 22, 24), horn)], [tail3], rot=(-18, 0, 0))
    tail = Part('tail', (0, 17, 12), [Box((-3.5, 13.5, 11.5), (3.5, 20, 21), body), Box((-.5, 20, 15), (.5, 23, 17), horn),
                                      Box((-2.5, 13, 12), (2.5, 14, 20), gut)], [tail2], rot=(12, 0, 0))
    parts = [torso, neck, front_leg(-1), front_leg(1), hind_leg(-1), hind_leg(1), tail, wing(-1), wing(1)]
    return EntityModel(parts, S=2, width=256)

DRAKE_SHOW = {'r_wing': (8, -8, -28), 'l_wing': (8, 8, 28), 'r_wing_tip': (0, -10, -20), 'l_wing_tip': (0, 10, 20)}

# ============================================================ spirit fox
def spirit_fox():
    coat = ramp('#3d63a8', '#5a8bc9', '#7cb4e2', '#a6d9f2', '#cfeffb', '#eefbff', '#ffffff')
    fur_m = fur(coat, seed=131, grad=(1, 0, 15))
    pale = fur(coat[3:], seed=133)
    sock = noise(ramp('#1b2b66', '#2c4694', '#4a6cc0', '#7098dc'), 1.0, .4)
    glow = glowing(FROSTGLOW[1:], .7, scale=1.2)
    gold = metal(GOLD)
    eye_pal = {'k': '#0b1236', 'c': ('#5ff6ff', True), 'w': ('#ffffff', True), 'm': ('#8df0ff', True)}
    face = stamp(['....mm....', '....mm....', '..........', 'kkk....kkk', 'kwck..kcwk', '.kk....kk.'], eye_pal, None, 1)
    flank = stamp(['m.......', '.mm.....', '...m....', '...mm..m', '.....mm.'], eye_pal, None, 2)

    def tail(n, rot, length):
        base = Part(n, (0, 9, 5), [Box((-1.5, 7.5, 4.5), (1.5, 10.5, 9.5), fur_m)], rot=rot)
        mid = Part(n + '_mid', (0, 9, 9.5), [Box((-2.5, 6.5, 9), (2.5, 11.5, 9 + length), pale)], rot=(24, 0, 0))
        tip = Part(n + '_tip', (0, 9, 9 + length), [Box((-1.5, 7.5, 8.8 + length), (1.5, 10.5, 11.8 + length), glow),
                                                    Box((-.5, 8.5, 11.8 + length), (.5, 9.5, 13.8 + length), glow)], rot=(22, 0, 0))
        mid.children.append(tip)
        base.children.append(mid)
        return base

    def leg(n, s, z):
        x = s * 2
        B = lambda a, b, m: Box((x + a[0], a[1], a[2]), (x + b[0], b[1], b[2]), m)
        return Part(n, (x, 6, z), [B((-1, 2, -1), (1, 7, 1), fur_m), B((-1, 0, -1.5), (1, 2, 1.5), sock)])

    def ear(n, s):
        return Part(n, (s * 3, 14, -7), [
            Box((s * 3 - 1.5, 13.5, -7.5), (s * 3 + 1.5, 16.5, -6.5), fur_m, {'front': [stamp(['.mm.', '.mm.'], eye_pal, None, 2)]}),
            Box((s * 3 - .5, 16.5, -7.4), (s * 3 + .5, 19.5, -6.6), sock),
        ], rot=(-6, 0, -s * 16))

    head = Part('head', (0, 11, -5), [
        Box((-4, 8, -11), (4, 14, -5), fur_m, {'front': [face]}),
        Box((-2, 8, -14), (2, 11, -11), pale, {'front': [stamp(['kk', '..'], eye_pal, None, 0)]}),
        Box((-6, 8, -9), (-4, 11, -7), pale), Box((4, 8, -9), (6, 11, -7), pale),
        Box((-.5, 13.8, -11.3), (.5, 15.8, -10.3), glow),
    ], [ear('r_ear', -1), ear('l_ear', 1)])
    torso = Part('body', (0, 8, 0), [
        Box((-3, 5, -5), (3, 11, 6), fur_m, {'left': [flank], 'right': [flank]}),
        Box((-3.5, 5.5, -6), (3.5, 11.5, -2), pale),
        Box((-3.8, 9.5, -6.3), (3.8, 11, -3.5), gold), Box((-1, 8, -6.8), (1, 10, -5.8), glow),
    ])
    parts = [torso, head, tail('tail_c', (34, 0, 0), 8), tail('tail_r', (20, -38, 0), 7), tail('tail_l', (20, 38, 0), 7),
             leg('right_front_leg', -1, -3), leg('left_front_leg', 1, -3), leg('right_hind_leg', -1, 4), leg('left_hind_leg', 1, 4)]
    return EntityModel(parts, S=2, width=128)

# ============================================================ shadow lich
def shadow_lich():
    robe_c = ramp('#08040f', '#130a22', '#201037', '#30184f', '#44226b', '#5c308a')
    robe = noise(robe_c, 2.2, .25, 1.2, grad=(1, 2, 34))
    inner = noise(robe_c[:3], 1.6, .4)
    bone = noise(ramp('#5a5340', '#8a8168', '#b5ac8e', '#d8d0b0', '#f2ecd4'), 1.3, .35, 1.4)
    gold = metal(ramp('#33230a', '#5e4310', '#8f6a18', '#c29526', '#ecc24a', '#fff09a'))
    soul = glowing(ramp('#0a5c3a', '#12a05a', '#2ee07e', '#7dffae', '#d4ffe6'), .9, scale=1.5)
    void = crystal(AMETHYST, 2.0, seed=141, glow=.8)
    pal = {'k': '#05030a', 'g': ('#4dff95', True), 'w': ('#eafff2', True), 't': '#2a2418'}
    skull = stamp(['................', '.kkkkk....kkkkk.', '.kgggk....kgggk.', '.kgwgk....kgwgk.', '.kkgkk....kkgkk.', '..kkk..kk..kkk..',
                   '.......kk.......', '................', '..tktktktktktk..', '..ktktktktktkt..'], pal, None, 3)
    runes = stamp(['...g....', '..g.g...', '.g...g..', '..ggg...', '...g....', '...g....', '.g.g.g..', '..ggg...'], {'g': ('#38e884', True)}, None, 4)

    def ragged(base, depth, period, seed):
        def f(c):
            a = c.x if c.face in ('front', 'back') else c.z
            col = np.floor((a + 40) * 1.0)
            r = hash3(np.stack([col, col * 0, col * 0], -1), seed)
            k = np.abs(np.mod(a + 40, period) / period - .5) * 2
            return c.y < base + depth * (k * .6 + r * .4)
        return cut(f)

    def sides(dec):
        return {'front': [dec], 'back': [dec], 'left': [dec], 'right': [dec], 'bottom': [cut(lambda c: np.ones((c.H, c.W), bool))]}

    skirt = Part('skirt', (0, 18, 0), [
        Box((-6, 11, -4), (6, 19, 4), robe, {'front': [stamp(['gg', '..', 'gg', '..', 'gg'], {'g': ('#38e884', True)}, None, 3)]}),
        Box((-7, 4, -5), (7, 12, 5), robe, sides(ragged(4, 6, 5.0, 143))),
        Box((-4, 1, -3), (4, 8, 3), inner, sides(ragged(1, 5, 3.5, 145))),
    ])
    head = Part('head', (0, 33, 0), [
        Box((-4, 33, -4), (4, 41, 4), bone, {'front': [skull]}),
        Box((-5, 34, -2), (5, 42, 5), robe, {'front': [cut(lambda c: (np.abs(c.x) < 4.2) & (c.y < 40.5))], 'bottom': [cut(lambda c: np.ones((c.H, c.W), bool))]}),
        Box((-4.5, 40, -4.6), (4.5, 42, -3.6), gold),
        Box((-1, 40.2, -5.2), (1, 42.2, -4.2), soul),
    ], [
        Part('crown_c', (0, 42, -4), [Box((-1, 42, -4.5), (1, 47, -3.5), gold), Box((-.5, 47, -4.5), (.5, 49, -3.5), soul)]),
        Part('crown_r', (-3.5, 42, -4), [Box((-4.5, 42, -4.5), (-3.5, 46, -3.5), gold)], rot=(0, 0, 16)),
        Part('crown_l', (3.5, 42, -4), [Box((3.5, 42, -4.5), (4.5, 46, -3.5), gold)], rot=(0, 0, -16)),
    ])
    torso = Part('torso', (0, 24, 0), [
        Box((-6, 18, -3), (6, 33, 3), robe, {'front': [stamp(['kkkkkkkk', 'kggggggk', 'kk....kk', 'kggggggk', 'kk....kk', '.kggggk.', '..kkkk..'], pal, None, 8)]}),
        Box((-2, 25, -3.8), (2, 29, -2.8), soul),
        Box((-6.5, 18, -3.5), (6.5, 20, 3.5), gold, {'*': [trim(GOLD[:5])]}),
        Box((-8, 28, 3), (8, 36, 5), robe, {'*': [ragged(28, 3, 4.0, 147)]}),
        Box((-9, 30.5, -4), (-4, 34, 4), bone), Box((4, 30.5, -4), (9, 34, 4), bone),
        Box((-8, 34, -1), (-7, 38, 1), bone), Box((7, 34, -1), (8, 38, 1), bone),
        Box((-5, 31, -4.2), (5, 32.5, -3), gold),
    ])
    cape = Part('cape', (0, 32, 4), [Box((-8, 4, 4.6), (8, 32, 5.6), robe, {'*': [ragged(4, 9, 6.0, 149)], 'back': [ragged(4, 9, 6.0, 149), runes, trim(GOLD[:4], 1, 'lr')]})], rot=(-9, 0, 0))

    def arm(s):
        n = 'right' if s < 0 else 'left'
        x = s * 9
        B = lambda a, b, m, d=None: Box((x + a[0], a[1], a[2]), (x + b[0], b[1], b[2]), m, d)
        boxes = [
            B((-2, 24, -2), (2, 32, 2), robe),
            B((-3, 18, -3), (3, 25, 3), robe, {'*': [trim(GOLD[:4], 1, 'b')]}),
            B((-1, 14, -1), (1, 19, 1), bone),
            B((-1.5, 12, -2), (1.5, 15, 1), bone), B((-1.5, 10, -2), (-.5, 12, -1), bone), B((.5, 10, -2), (1.5, 12, -1), bone),
        ]
        kids = []
        if s < 0:
            sx, sz = x, -3
            kids.append(Part('staff', (sx, 13, sz), [
                Box((sx - .5, 3, sz - .5), (sx + .5, 38, sz + .5), noise(LEATHER_BLACK[1:], .9, .6)),
                Box((sx - 2.5, 37, sz - .5), (sx + 2.5, 38, sz + .5), gold),
                Box((sx - 2.5, 38, sz - .5), (sx - 1.5, 43, sz + .5), bone), Box((sx + 1.5, 38, sz - .5), (sx + 2.5, 43, sz + .5), bone),
                Box((sx - 1.5, 39, sz - 1.5), (sx + 1.5, 42, sz + 1.5), soul),
                Box((sx - .5, 43, sz - .5), (sx + .5, 45, sz + .5), soul),
                Box((sx - 1, 2, sz - 1), (sx + 1, 4, sz + 1), gold),
            ]))
        return Part(n + '_arm', (s * 8, 31, 0), boxes, kids, rot=(-18 if s < 0 else 0, 0, -s * 7))

    shards = []
    for i, ang in enumerate((20, 140, 260)):
        a = math.radians(ang)
        px_, pz = round(math.sin(a) * 15), round(math.cos(a) * 15)
        shards.append(Part('shard_%d' % i, (px_, 24, pz), [
            Box((px_ - 1.5, 22, pz - 1.5), (px_ + 1.5, 27, pz + 1.5), void),
            Box((px_ - .5, 27, pz - .5), (px_ + .5, 30, pz + .5), void), Box((px_ - .5, 20, pz - .5), (px_ + .5, 22, pz + .5), void),
        ]))
    orbit = Part('orbit', (0, 24, 0), [], shards)
    return EntityModel([torso, head, skirt, cape, arm(-1), arm(1), orbit], S=2, width=256)
