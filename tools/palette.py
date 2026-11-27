from mc3d import *

GOLD = ramp('#4a2a00', '#7d4e04', '#b57a0a', '#e3a81c', '#ffd13f', '#ffeb8a', '#fffbd6')
SILVER = ramp('#2b3444', '#4c5a70', '#74859c', '#a3b4c8', '#cfdcea', '#f2f8ff')
DSTEEL = ramp('#0f0e1a', '#1e1c30', '#302d4a', '#454066', '#625a88', '#8a80b4')
FIRE = ramp('#7a0c00', '#c21f00', '#f04a00', '#ff7e0a', '#ffb21e', '#ffdc4d', '#fff6bd')
FIREMETAL = ramp('#3d0503', '#7c0f05', '#b52308', '#e0450c', '#f87512', '#ffa826', '#ffd75a')
ICE = ramp('#123a6e', '#1d63a8', '#2f95d4', '#5cc3ee', '#9ee3fb', '#d6f6ff', '#ffffff')
FROSTGLOW = ramp('#2aa6e8', '#55ccff', '#8fe6ff', '#c6f6ff', '#ffffff')
STORM = ramp('#4a12b8', '#7430f0', '#9d5cff', '#c496ff', '#e7d4ff', '#ffffff')
AMETHYST = ramp('#220a4a', '#3d1578', '#5e24ab', '#8640d8', '#b06cf5', '#dcaaff')
RUBY = ramp('#7a0014', '#c00a24', '#ff2b3d', '#ff7a72', '#ffd0c8')
EMERALD = ramp('#064a24', '#0c7a3a', '#18b455', '#4ee889', '#c8ffdc')
LEATHER_RED = ramp('#240508', '#420a10', '#621018', '#841a22', '#a52a30')
LEATHER_NAVY = ramp('#070c1e', '#0f1a36', '#1a2a52', '#27406f', '#385a92')
LEATHER_BLACK = ramp('#07060b', '#111019', '#1d1b28', '#2b2839', '#3d384e')
LEATHER_BROWN = ramp('#1c0d05', '#321a0a', '#4d2a12', '#6b3d1c', '#8a5429')

def wrap(base, band, period=4, width=1, seed=3):
    """leather grip with diagonal metal wire"""
    def f(c):
        v = vnoise(c.P, 1.0, seed) * .5 + .25
        img = paint(c, v, base)
        d = np.mod((c.y * c.S) + (c.u * .9), period * c.S / 1.0)
        m = d < width * c.S / 1.0
        hi = paint(c, .45 + .4 * (c.u / max(c.W, 1)), band)
        img[m] = hi[m]
        return bevel(c, img, 1.2)
    return Mat(f)

def scales(colors, tw=6, th=4, seed=61, grad=None):
    """overlapping rounded scales (upper row covers the one below)"""
    def f(c):
        uu, vv = c.u + .5, c.v + .5
        val = np.zeros((c.H, c.W)); done = np.zeros((c.H, c.W), bool)
        base_row = np.floor(vv / th)
        for k in (-1, 0, 1):
            row = base_row + k
            off = np.mod(row, 2) * (tw / 2)
            col = np.floor((uu + off) / tw)
            dx = (uu + off - (col + .5) * tw) / (tw * .56)
            dy = (vv - row * th) / (th * 1.25)
            d = np.sqrt(dx * dx + np.clip(dy, 0, None) ** 2)
            inside = (d <= 1) & (dy >= -.05) & ~done
            r = hash3(np.stack([col, row, col * 0], -1), seed)
            v = .28 + r * .16 + np.clip(dy, 0, 1) * .38 - np.clip(dx, -1, 1) * .08
            v = np.where(d > .74, v - .34, v)
            v = np.where((np.abs(dx + .2) < .3) & (dy > .35) & (dy < .62), v + .2, v)
            val = np.where(inside, v, val); done |= inside
        val = np.where(done, val, .2)
        if grad:
            a, lo, hi = grad
            val = val * .8 + (np.clip((c.P[..., a] - lo) / (hi - lo), 0, 1) - .5) * .35 + .08
        return bevel(c, paint(c, val, colors), .7)
    return Mat(f)

def blade(colors, y0, y1, seed=4, flame=False):
    """blade steel: gradient along the length, bright cutting edges, fuller shading"""
    def f(c):
        g = np.clip((c.y - y0) / (y1 - y0), 0, 1)
        v = g * .55 + .22 + (vnoise(c.P * np.array([1, .45, 1]), 1.1, seed) - .5) * (.55 if flame else .25)
        img = paint(c, v, colors)
        if c.face in ('front', 'back') and c.W >= 3:
            for col, add in ((0, .34), (c.W - 1, .22), (1, .12)):
                img[:, col] = paint(c, v + add, colors)[:, col]
        return img
    return Mat(f)
