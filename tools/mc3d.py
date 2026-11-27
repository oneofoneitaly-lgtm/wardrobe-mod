"""Small authoring engine: cube models + procedural pixel textures + preview renderer.
Everything is authored y-up, front = -z, in model units (16 = one block)."""
import json, math, os
import numpy as np
from PIL import Image, ImageFilter

FACES = ('front', 'back', 'left', 'right', 'top', 'bottom')  # front=-z, left=-x, right=+x

def rgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], float)

def ramp(*cols):
    return np.stack([rgb(c) for c in cols])

def hash3(p, seed=0):
    p = np.asarray(p, float)
    v = np.sin(p[..., 0] * 127.1 + p[..., 1] * 311.7 + p[..., 2] * 74.7 + seed * 53.3) * 43758.5453
    return v - np.floor(v)

def vnoise(p, scale, seed=0):
    q = np.asarray(p, float) / scale
    i = np.floor(q); f = q - i; f = f * f * (3 - 2 * f)
    out = 0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (f[..., 0] if dx else 1 - f[..., 0]) * (f[..., 1] if dy else 1 - f[..., 1]) * (f[..., 2] if dz else 1 - f[..., 2])
                out = out + w * hash3(i + np.array([dx, dy, dz]), seed)
    return out

class Ctx:
    def __init__(self, face, P, S, t, size):
        self.face, self.P, self.S, self.t, self.size = face, P, S, t, size
        self.H, self.W = P.shape[:2]
        self.u, self.v = np.meshgrid(np.arange(self.W), np.arange(self.H))
        self.x, self.y, self.z = P[..., 0], P[..., 1], P[..., 2]

def paint(ctx, value, colors):
    idx = np.clip((value * len(colors)).astype(int), 0, len(colors) - 1)
    out = np.zeros((ctx.H, ctx.W, 4))
    out[..., :3] = colors[idx]; out[..., 3] = 255
    return out

def bevel(ctx, img, k=1.0):
    if ctx.H >= 3:
        img[0, :, :3] *= 1 + .16 * k; img[-1, :, :3] *= 1 - .20 * k
    if ctx.W >= 3:
        img[:, 0, :3] *= 1 + .08 * k; img[:, -1, :3] *= 1 - .12 * k
    img[..., :3] = np.clip(img[..., :3], 0, 255)
    return img

class Mat:
    def __init__(self, fn, glow=None):
        self.fn, self.glow = fn, glow
    def __call__(self, ctx):
        return self.fn(ctx)

def noise(colors, scale=1.6, grain=.35, bv=1.0, grad=None, seed=1):
    """value noise quantised to a ramp; grad=(axis, lo, hi) adds a position gradient"""
    def f(c):
        v = vnoise(c.P, scale, seed) * (1 - grain) + hash3(np.floor(c.P * c.S), seed + 3) * grain
        if grad:
            a, lo, hi = grad
            g = np.clip((c.P[..., a] - lo) / (hi - lo), 0, 1)
            v = g * .68 + v * .32
        return bevel(c, paint(c, v, colors), bv)
    return Mat(f)

def metal(colors, grad=None, seed=2, bv=1.4, sheen=.5):
    def f(c):
        diag = np.mod((c.x + c.y * 1.0 + c.z) / 5.0, 1.0)
        s = np.exp(-((diag - .5) ** 2) / .012) * sheen
        base = .42 + (vnoise(c.P, 2.2, seed) - .5) * .35 + s
        if grad:
            a, lo, hi = grad
            base = np.clip((c.P[..., a] - lo) / (hi - lo), 0, 1) * .65 + s * .6 + (hash3(np.floor(c.P * c.S), seed) - .5) * .12 + .1
        return bevel(c, paint(c, base, colors), bv)
    return Mat(f)

def tiles(colors, tw, th, seed=5, grad=None, bv=.8, tip=True):
    """overlapping rounded scales / feathers, tw x th in texels, hanging downwards"""
    def f(c):
        vv, uu = c.v.astype(float), c.u.astype(float)
        row = np.floor(vv / th)
        off = np.mod(row, 2) * (tw / 2)
        col = np.floor((uu + off) / tw)
        fx = (np.mod(uu + off, tw) + .5) / tw
        fy = (np.mod(vv, th) + .5) / th
        r = hash3(np.stack([col, row, col * 0], -1), seed)
        val = .30 + fy * .45 + r * .18
        if grad:
            a, lo, hi = grad
            val = np.clip((c.P[..., a] - lo) / (hi - lo), 0, 1) * .6 + fy * .3 + r * .1
        edge = (fy > 1 - .5 * np.abs(fx - .5) * 2 * .8 - 1.01 / th) if tip else (fy > 1 - 1.01 / th)
        side = (np.mod(uu + off, tw) < 1) & (fy > .35)
        val = np.where(edge | side, val - .38, val)
        mid = (np.abs(fx - .5) < .6 / tw) & (fy > .2) & (fy < .8) & (tw >= 4)
        val = np.where(mid, val + .18, val)
        return bevel(c, paint(c, val, colors), bv)
    return Mat(f)

def fur(colors, seed=9, grad=None):
    def f(c):
        q = c.P * np.array([1.0, .28, .55])
        v = vnoise(q, .75, seed) * .55 + vnoise(c.P, 3, seed + 1) * .45
        if grad:
            a, lo, hi = grad
            v = v * .55 + np.clip((c.P[..., a] - lo) / (hi - lo), 0, 1) * .5
        return bevel(c, paint(c, v, colors), .8)
    return Mat(f)

def crystal(colors, cell=3.2, seed=11, glow=0.0):
    """faceted voronoi in 3d, bright seams"""
    def f(c):
        q = c.P / cell
        base = np.floor(q)
        d1 = np.full((c.H, c.W), 9.0); d2 = d1.copy(); val = np.zeros((c.H, c.W))
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    cel = base + np.array([dx, dy, dz])
                    pt = cel + np.stack([hash3(cel, seed), hash3(cel, seed + 1), hash3(cel, seed + 2)], -1)
                    d = np.linalg.norm(q - pt, axis=-1)
                    closer = d < d1
                    d2 = np.where(closer, d1, np.minimum(d2, d))
                    val = np.where(closer, hash3(cel, seed + 4), val)
                    d1 = np.minimum(d1, d)
        v = val * .62 + .12 + np.sin((c.x + c.y * 1.4) * .9 + c.t * 6.283) * .07
        v = np.where(d2 - d1 < .11, v + .38, v)
        img = bevel(c, paint(c, v, colors), 1.1)
        return img
    m = Mat(f)
    if glow:
        m.glow = lambda c, img: np.clip((img[..., :3].mean(-1) - 120) / 110, 0, 1) * glow
    return m

def stone(colors, seed=17, crack=True):
    def f(c):
        v = vnoise(c.P, 3.4, seed) * .6 + vnoise(c.P, 1.1, seed + 1) * .25 + hash3(np.floor(c.P * c.S), seed) * .15
        v = (v - .5) * 1.35 + .52
        if crack:
            n = vnoise(c.P, 4.2, seed + 7)
            v = np.where(np.abs(n - .5) < .018, v - .45, v)
            v = np.where(np.abs(n - .535) < .012, v + .22, v)
        return bevel(c, paint(c, v, colors), 1.5)
    return Mat(f)

def solid(color, bv=1.0):
    col = rgb(color)
    def f(c):
        img = np.zeros((c.H, c.W, 4)); img[..., :3] = col; img[..., 3] = 255
        return bevel(c, img, bv)
    return Mat(f)

def glowing(colors, speed=1.0, phase=0.0, scale=2.0, seed=23):
    """animated emissive energy"""
    def f(c):
        v = vnoise(c.P + np.array([0, -c.t * 8 * speed, 0]) * 1.0, scale, seed)
        v2 = vnoise(c.P + np.array([0, (1 - c.t) * 8 * speed, 3]), scale, seed)
        v = v * (1 - c.t) + v2 * c.t
        v = (v - .5) * 1.5 + .62 + .12 * math.sin(6.283 * (c.t + phase))
        return paint(c, v, colors)
    return Mat(f, glow=lambda c, img: np.ones((c.H, c.W)))

# ---------------------------------------------------------------- decals
def px(img, x, y, color, a=255):
    if 0 <= y < img.shape[0] and 0 <= x < img.shape[1]:
        img[y, x, :3] = rgb(color); img[y, x, 3] = a

def stamp(pattern, palette, ox=None, oy=None, flip=False):
    """pattern: list of strings, '.' = keep. centred unless ox/oy given (negative = from far edge)."""
    def f(ctx, img, glow):
        rows = [r[::-1] if flip else r for r in pattern]
        h, w = len(rows), len(rows[0])
        x0 = (ctx.W - w) // 2 if ox is None else (ox if ox >= 0 else ctx.W - w + ox + 1)
        y0 = (ctx.H - h) // 2 if oy is None else (oy if oy >= 0 else ctx.H - h + oy + 1)
        for j, r in enumerate(rows):
            for i, ch in enumerate(r):
                if ch == '.':
                    continue
                col, emissive = palette[ch] if isinstance(palette[ch], tuple) else (palette[ch], False)
                px(img, x0 + i, y0 + j, col)
                if emissive and 0 <= y0 + j < ctx.H and 0 <= x0 + i < ctx.W:
                    glow[y0 + j, x0 + i] = 1
    return f

def trim(colors, width=1, sides='tblr'):
    """metal rim around a face"""
    def f(ctx, img, glow):
        m = np.zeros((ctx.H, ctx.W), bool)
        if 't' in sides: m[:width] = True
        if 'b' in sides: m[-width:] = True
        if 'l' in sides: m[:, :width] = True
        if 'r' in sides: m[:, -width:] = True
        v = .35 + .5 * np.mod((ctx.u + ctx.v) / 7.0, 1.0)
        col = paint(ctx, v, colors)
        img[m] = col[m]
    return f

def cut(fn):
    """transparency: fn(ctx) -> bool mask of texels to remove"""
    def f(ctx, img, glow):
        m = fn(ctx)
        img[m] = 0; glow[m] = 0
    return f

# ---------------------------------------------------------------- geometry
def face_points(face, a, b, tw, th, space):
    """3d centre of every texel of a face, oriented like Minecraft maps it"""
    x0, y0, z0 = a; x1, y1, z1 = b
    s = (np.arange(tw) + .5) / tw
    r = (np.arange(th) + .5) / th
    S, R = np.meshgrid(s, r)
    TL, TR, BL = [np.array(c, float) for c in face_corners(face, a, b, space)[:3]]
    TL_, TR_, BL_ = TL, TR, BL
    return TL_ + S[..., None] * (TR_ - TL_) + R[..., None] * (BL_ - TL_)

def face_corners(face, a, b, space):
    """(texel TL, TR, BL, BR) corners in authoring space"""
    x0, y0, z0 = a; x1, y1, z1 = b
    if space == 'item':
        c = {'front': ((x1, y1, z0), (x0, y1, z0), (x1, y0, z0), (x0, y0, z0)),
             'back': ((x0, y1, z1), (x1, y1, z1), (x0, y0, z1), (x1, y0, z1)),
             'left': ((x0, y1, z0), (x0, y1, z1), (x0, y0, z0), (x0, y0, z1)),
             'right': ((x1, y1, z1), (x1, y1, z0), (x1, y0, z1), (x1, y0, z0)),
             'top': ((x0, y1, z0), (x1, y1, z0), (x0, y1, z1), (x1, y1, z1)),
             'bottom': ((x0, y0, z1), (x1, y0, z1), (x0, y0, z0), (x1, y0, z0))}
    else:  # entity box uv (vanilla ModelPart.Cube), authoring space is y-up
        c = {'front': ((x0, y1, z0), (x1, y1, z0), (x0, y0, z0), (x1, y0, z0)),
             'back': ((x1, y1, z1), (x0, y1, z1), (x1, y0, z1), (x0, y0, z1)),
             'left': ((x0, y1, z1), (x0, y1, z0), (x0, y0, z1), (x0, y0, z0)),
             'right': ((x1, y1, z0), (x1, y1, z1), (x1, y0, z0), (x1, y0, z1)),
             'top': ((x0, y1, z1), (x1, y1, z1), (x0, y1, z0), (x1, y1, z0)),
             'bottom': ((x0, y0, z1), (x1, y0, z1), (x0, y0, z0), (x1, y0, z0))}
    return c[face]

def rot_matrix(rx, ry, rz):
    a, b, c = [math.radians(v) for v in (rx, ry, rz)]
    X = np.array([[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]])
    Y = np.array([[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]])
    Z = np.array([[math.cos(c), -math.sin(c), 0], [math.sin(c), math.cos(c), 0], [0, 0, 1]])
    return Z @ Y @ X

class Packer:
    def __init__(self, w, h):
        self.w, self.h = w, h
    def pack(self, sizes):
        order = sorted(range(len(sizes)), key=lambda i: (-sizes[i][1], -sizes[i][0]))
        used = np.zeros((self.h, self.w), bool)
        out = [None] * len(sizes)
        for i in order:
            w, h = sizes[i]
            if w > self.w or h > self.h:
                return None
            # first fit using an integral image
            I = np.zeros((self.h + 1, self.w + 1), np.int32)
            I[1:, 1:] = used.cumsum(0).cumsum(1)
            s = I[h:, w:] - I[:-h, w:] - I[h:, :-w] + I[:-h, :-w]
            free = np.argwhere(s == 0)
            if len(free) == 0:
                return None
            y, x = free[0]
            used[y:y + h, x:x + w] = True
            out[i] = (int(x), int(y))
        return out

def paint_face(mat, decals, face, a, b, tw, th, S, t, space):
    P = face_points(face, a, b, tw, th, space)
    ctx = Ctx(face, P, S, t, tuple(np.subtract(b, a)))
    img = mat(ctx)
    glow = np.zeros((th, tw))
    if mat.glow:
        glow = np.array(mat.glow(ctx, img), float)
    for d in decals:
        d(ctx, img, glow)
    return np.clip(img, 0, 255), glow

# ---------------------------------------------------------------- entity / armour models
class Box:
    def __init__(self, a, b, mat, decals=None, inflate=0.0, faces=None):
        self.a, self.b = tuple(map(float, a)), tuple(map(float, b))
        self.mat, self.decals, self.inflate = mat, decals or {}, inflate
        self.faces = faces or {}   # per-face material override
        d = np.subtract(self.b, self.a)
        assert (d >= 0).all(), ('negative box', a, b)
        # entity texture layouts need whole sizes: snap the far corner
        self.b = tuple(float(v) for v in np.array(self.a) + np.floor(d + .5))

class Part:
    def __init__(self, name, pivot, boxes=(), children=(), rot=(0, 0, 0)):
        self.name, self.pivot, self.boxes, self.children, self.rot = name, tuple(map(float, pivot)), list(boxes), list(children), rot
    def walk(self):
        yield self
        for c in self.children:
            yield from c.walk()

class EntityModel:
    def __init__(self, parts, S=2, width=128):
        self.parts, self.S, self.width = parts, S, width

    def boxes(self):
        for p in self.parts:
            for q in p.walk():
                for b in q.boxes:
                    yield b

    def build(self):
        bs = list(self.boxes())
        sizes = []
        for b in bs:
            w, h, d = [int(round(v)) for v in np.subtract(b.b, b.a)]
            sizes.append((2 * d + 2 * w, d + h))
        for H in (32, 64, 128, 256, 512):
            pos = Packer(self.width, H).pack(sizes)
            if pos:
                break
        assert pos, 'atlas too small'
        self.height = H
        S = self.S
        tex = np.zeros((H * S, self.width * S, 4)); gl = np.zeros((H * S, self.width * S))
        for b, (u, v) in zip(bs, pos):
            w, h, d = [int(round(x)) for x in np.subtract(b.b, b.a)]
            b.uv = (u, v)
            rects = {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'left': (u, v + d, d, h),
                     'front': (u + d, v + d, w, h), 'right': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}
            b.rects = rects
            for face, (fu, fv, fw, fh) in rects.items():
                if fw == 0 or fh == 0:
                    continue
                img, glow = paint_face(b.faces.get(face, b.mat), b.decals.get(face, []) + b.decals.get('*', []), face, b.a, b.b, fw * S, fh * S, S, 0.0, 'entity')
                tex[fv * S:(fv + fh) * S, fu * S:(fu + fw) * S] = img
                gl[fv * S:(fv + fh) * S, fu * S:(fu + fw) * S] = glow
        self.tex = Image.fromarray(tex.astype(np.uint8), 'RGBA')
        g = tex.copy(); g[gl < .35] = 0; g[..., :3] *= gl[..., None]
        self.glow = Image.fromarray(g.astype(np.uint8), 'RGBA')
        self.has_glow = bool((gl >= .35).any())
        return self

    def quads(self, pose=None, hidden=()):
        pose = pose or {}
        tex = np.asarray(self.tex, float); gl = np.asarray(self.glow)
        out = []
        def visit(p, M, T):
            if p.name in hidden:
                return
            R = rot_matrix(*pose.get(p.name, p.rot))
            piv = np.array(p.pivot)
            # world = M @ (R @ (v - piv) + piv) + T
            M2 = M @ R; T2 = M @ (piv - R @ piv) + T
            for b in p.boxes:
                a = np.array(b.a) - b.inflate; bb = np.array(b.b) + b.inflate
                for face, (fu, fv, fw, fh) in b.rects.items():
                    if fw == 0 or fh == 0:
                        continue
                    TL, TR, BL, BR = face_corners(face, a, bb, 'entity')
                    S = self.S
                    u0, v0, u1, v1 = fu * S, fv * S, (fu + fw) * S, (fv + fh) * S
                    uvs = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
                    verts = [M2 @ np.array(c) + T2 for c in (TL, TR, BR, BL)]
                    out.append((np.array(verts), np.array(uvs, float), tex, gl[..., 3] > 0))
            for c in p.children:
                visit(c, M2, T2)
        for p in self.parts:
            visit(p, np.eye(3), np.zeros(3))
        return out

    def java(self, package, cls):
        L = []
        def f(v):
            s = ('%.4f' % v).rstrip('0').rstrip('.')
            if s in ('-0', ''):
                s = '0'
            return s + 'F'
        counter = [0]
        def emit(p, parent_var, parent_piv):
            counter[0] += 1
            var = 'p%d' % counter[0]
            piv = p.pivot
            cl = 'CubeListBuilder.create()'
            for b in p.boxes:
                w, h, d = [int(round(v)) for v in np.subtract(b.b, b.a)]
                x = b.a[0] - piv[0]; y = -(b.b[1] - piv[1]); z = b.a[2] - piv[2]
                cl += '\n                .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s, new CubeDeformation(%s))' % (
                    b.uv[0], b.uv[1], f(x), f(y), f(z), f(w), f(h), f(d), f(b.inflate))
            if parent_piv is None:   # root level: model origin is 24 units above the ground
                ox, oy, oz = piv[0], 24 - piv[1], piv[2]
            else:
                ox, oy, oz = piv[0] - parent_piv[0], -(piv[1] - parent_piv[1]), piv[2] - parent_piv[2]
            rx, ry, rz = [math.radians(v) for v in p.rot]
            L.append('        PartDefinition %s = %s.addOrReplaceChild("%s", %s,\n                PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s));' % (
                var, parent_var, p.name, cl, f(ox), f(oy), f(oz), f(-rx), f(ry), f(-rz)))
            for c in p.children:
                emit(c, var, piv)
        for p in self.parts:
            emit(p, 'root', None)
        return ('package %s;\n\nimport net.minecraft.client.model.geom.PartPose;\nimport net.minecraft.client.model.geom.builders.*;\n\n'
                '/** Generated by tools/make.py - do not edit. */\npublic final class %s {\n    private %s() {}\n\n'
                '    public static LayerDefinition create() {\n        MeshDefinition mesh = new MeshDefinition();\n        PartDefinition root = mesh.getRoot();\n%s\n'
                '        return LayerDefinition.create(mesh, %d, %d);\n    }\n}\n') % (package, cls, cls, '\n'.join(L), self.width, self.height)

# ---------------------------------------------------------------- item models (json elements)
class El:
    def __init__(self, a, b, mat, decals=None, rot=None, faces=None, glow=False):
        self.a, self.b, self.mat, self.decals = tuple(map(float, a)), tuple(map(float, b)), mat, decals or {}
        self.rot = rot          # (axis, angle, origin)
        self.faces = faces or {}
        self.glow = glow
        assert all(-16 <= v <= 32 for v in self.a + self.b), ('element out of range', a, b)
        if rot:
            assert rot[1] in (-45, -22.5, 0, 22.5, 45)

JFACE = {'front': 'north', 'back': 'south', 'left': 'west', 'right': 'east', 'top': 'up', 'bottom': 'down'}

class ItemModel:
    def __init__(self, els, S=2, frames=1, frametime=3):
        self.els, self.S, self.frames, self.frametime = els, S, frames, frametime

    def build(self):
        S = self.S
        faces = []
        for i, e in enumerate(self.els):
            dx, dy, dz = np.subtract(e.b, e.a)
            dims = {'front': (dx, dy), 'back': (dx, dy), 'left': (dz, dy), 'right': (dz, dy), 'top': (dx, dz), 'bottom': (dx, dz)}
            for face, (w, h) in dims.items():
                if w <= 0 or h <= 0:
                    continue
                faces.append((i, face, max(1, int(round(w * S))), max(1, int(round(h * S)))))
        for A in (16, 32, 64, 128, 256, 512):
            pos = Packer(A, A).pack([(f[2], f[3]) for f in faces])
            if pos:
                break
        assert pos
        self.atlas = A
        self.layout = [(i, face, x, y, w, h) for (i, face, w, h), (x, y) in zip(faces, pos)]
        sheet = np.zeros((A * self.frames, A, 4)); gsheet = np.zeros((A * self.frames, A))
        for fr in range(self.frames):
            for i, face, x, y, w, h in self.layout:
                e = self.els[i]
                img, glow = paint_face(e.faces.get(face, e.mat), e.decals.get(face, []) + e.decals.get('*', []), face, e.a, e.b, w, h, S, fr / self.frames, 'item')
                sheet[fr * A + y:fr * A + y + h, x:x + w] = img
                gsheet[fr * A + y:fr * A + y + h, x:x + w] = np.maximum(glow, 1.0 if e.glow else 0)
        self.tex = Image.fromarray(sheet.astype(np.uint8), 'RGBA')
        self.gsheet = gsheet
        return self

    def quads(self, frame=0, M=None, T=None):
        A = self.atlas
        tex = np.asarray(self.tex, float)[frame * A:(frame + 1) * A]
        gm = self.gsheet[frame * A:(frame + 1) * A] > .5
        out = []
        for i, face, x, y, w, h in self.layout:
            e = self.els[i]
            TL, TR, BL, BR = [np.array(c, float) for c in face_corners(face, e.a, e.b, 'item')]
            verts = [TL, TR, BR, BL]
            if e.rot:
                axis, ang, org = e.rot
                R = rot_matrix(*[ang if k == axis else 0 for k in 'xyz'])
                verts = [R @ (v - org) + org for v in verts]
            if M is not None:
                verts = [M @ v + T for v in verts]
            uvs = [(x, y), (x + w, y), (x + w, y + h), (x, y + h)]
            out.append((np.array(verts), np.array(uvs, float), tex, gm))
        return out

    def json(self, tex_id, display):
        A = self.atlas
        r = lambda v: round(float(v), 4)
        els = []
        for i, e in enumerate(self.els):
            fs = {}
            emissive = e.glow or (e.mat.glow is not None and e.mat.glow.__name__ == '<lambda>' and getattr(e.mat, 'full', False))
            for j, face, x, y, w, h in self.layout:
                if j == i:
                    fs[JFACE[face]] = {'uv': [r(x * 16 / A), r(y * 16 / A), r((x + w) * 16 / A), r((y + h) * 16 / A)], 'texture': '#t'}
            d = {'from': [r(v) for v in e.a], 'to': [r(v) for v in e.b], 'faces': fs}
            if e.rot:
                axis, ang, org = e.rot
                d['rotation'] = {'origin': [r(v) for v in org], 'axis': axis, 'angle': ang}
            if emissive:
                d['forge_data'] = {'block_light': 15, 'sky_light': 15}
                d['shade'] = False
            els.append(d)
        return {'credit': 'Mythoria by @Adryh01', 'gui_light': 'front', 'textures': {'t': tex_id, 'particle': tex_id}, 'elements': els, 'display': display}

# ---------------------------------------------------------------- renderer
def render(quads, size=(800, 800), yaw=30, pitch=15, fov=22, margin=.09, ss=2, bloom=True, mirror=False, frame_pts=None):
    """mirror=True for entity space (Minecraft draws entity models x-mirrored)"""
    W, H = size[0] * ss, size[1] * ss
    pts = np.concatenate([q[0] for q in quads]) if frame_pts is None else frame_pts
    lo, hi = pts.min(0), pts.max(0)
    centre = (lo + hi) / 2
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    # camera looks towards +z from the front; yaw turns the model, pitch looks down
    Ry = np.array([[cy, 0, -sy], [0, 1, 0], [sy, 0, cy]])
    Rx = np.array([[1, 0, 0], [0, cp, sp], [0, -sp, cp]])
    V = Rx @ Ry
    def view(v):
        q = (v - centre) @ V.T
        if not mirror:
            q = q.copy(); q[..., 0] *= -1   # camera right = -x
        return q
    all_v = view(pts)
    f = 1 / math.tan(math.radians(fov) / 2)
    ext = np.abs(all_v[:, :2]).max(0)
    dist = 0
    for v in all_v:   # smallest camera distance that keeps everything inside
        need = max(abs(v[0]) * f / ((1 - margin * 2) * W / H), abs(v[1]) * f / (1 - margin * 2))
        dist = max(dist, need - v[2])
    col = np.zeros((H, W, 4)); zb = np.full((H, W), 1e18); em = np.zeros((H, W, 3))
    light = np.array([.35, .85, -.45]); light /= np.linalg.norm(light)
    for verts, uvs, tex, gm in quads:
        n = np.cross(verts[3] - verts[0], verts[1] - verts[0])   # outward: TL->BL x TL->TR
        ln = np.linalg.norm(n)
        if ln < 1e-9:
            continue
        n /= ln
        if mirror:
            n = -n
        nv = n @ V.T
        side = nv[0] if mirror else -nv[0]
        shade = .60 + .30 * max(0, nv[1] * .8 - nv[2] * .35 - side * .25) + .16 * max(0, -nv[2])
        shade = min(shade, 1.08)
        q = view(verts)
        w = q[:, 2] + dist
        sx = W / 2 + q[:, 0] * f / w * H / 2
        sy_ = H / 2 - q[:, 1] * f / w * H / 2
        for tri in ((0, 1, 2), (0, 2, 3)):
            ax, ay = sx[list(tri)], sy_[list(tri)]
            x0, x1 = max(int(ax.min()), 0), min(int(ax.max()) + 2, W)
            y0, y1 = max(int(ay.min()), 0), min(int(ay.max()) + 2, H)
            if x0 >= x1 or y0 >= y1:
                continue
            gx, gy = np.meshgrid(np.arange(x0, x1) + .5, np.arange(y0, y1) + .5)
            den = (ay[1] - ay[2]) * (ax[0] - ax[2]) + (ax[2] - ax[1]) * (ay[0] - ay[2])
            if abs(den) < 1e-9:
                continue
            l0 = ((ay[1] - ay[2]) * (gx - ax[2]) + (ax[2] - ax[1]) * (gy - ay[2])) / den
            l1 = ((ay[2] - ay[0]) * (gx - ax[2]) + (ax[0] - ax[2]) * (gy - ay[2])) / den
            l2 = 1 - l0 - l1
            inside = (l0 >= -1e-6) & (l1 >= -1e-6) & (l2 >= -1e-6)
            if not inside.any():
                continue
            iw = np.stack([l0 / w[tri[0]], l1 / w[tri[1]], l2 / w[tri[2]]], -1)
            s = iw.sum(-1)
            depth = 1 / s
            uv = (iw @ uvs[list(tri)]) / s[..., None]
            tx = np.clip(uv[..., 0].astype(int), uvs[:, 0].min().astype(int), uvs[:, 0].max().astype(int) - 1)
            ty = np.clip(uv[..., 1].astype(int), uvs[:, 1].min().astype(int), uvs[:, 1].max().astype(int) - 1)
            texel = tex[ty, tx]
            sub = zb[y0:y1, x0:x1]
            ok = inside & (depth < sub - 1e-5) & (texel[..., 3] > 25)
            if not ok.any():
                continue
            e = gm[ty, tx]
            rgbv = texel[..., :3] * np.where(e, 1.04, shade)[..., None]
            c = col[y0:y1, x0:x1]
            c[ok, :3] = np.clip(rgbv[ok], 0, 255); c[ok, 3] = 255
            sub[ok] = depth[ok]
            em[y0:y1, x0:x1][ok] = np.where(e[ok][:, None], rgbv[ok], 0)
    img = Image.fromarray(col.astype(np.uint8), 'RGBA').resize(size, Image.LANCZOS)
    if bloom and em.any():
        g = Image.fromarray(np.clip(em, 0, 255).astype(np.uint8), 'RGB').resize(size, Image.LANCZOS)
        g1 = np.asarray(g.filter(ImageFilter.GaussianBlur(size[0] / 90)), float)
        g2 = np.asarray(g.filter(ImageFilter.GaussianBlur(size[0] / 30)), float)
        glow = np.clip(g1 * .7 + g2 * .75, 0, 255)
        a = np.clip(glow.max(-1) * .9, 0, 255)
        layer = np.dstack([np.clip(glow * 255 / np.maximum(glow.max(-1), 1)[..., None], 0, 255), a]).astype(np.uint8)
        img = Image.alpha_composite(img, Image.fromarray(layer, 'RGBA'))
    return img
