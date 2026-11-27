#!/usr/bin/env python3
"""Generates the Wardrobe clothes (by @Adryh01).

For every piece of clothing it writes:
  * a Blockbench file (.bbmodel) used directly by the mod
  * an OBJ + MTL + PNG for Blender
  * the preview sheet docs/catalogo.png

Coordinates are Blockbench ones: Y up, feet at y=0, the player looks to -Z,
the player's right arm is on +X.
"""
import base64
import io
import json
import math
import random
import shutil
import uuid
import zlib
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUTFITS = ROOT / "src/common/resources/assets/wardrobe/outfits"
BB_DIR = ROOT / "models/blockbench"
BLENDER_DIR = ROOT / "models/blender"
DOCS = ROOT / "docs"
PPU = 2          # texture pixels per model unit
ATLAS_W = 128
FACES = ("north", "south", "east", "west", "up", "down")


# ---------------------------------------------------------------- materials
def mat(color, style="cloth"):
    return (color, style)


def clamp(v):
    return max(0, min(255, int(round(v))))


def paint_material(img, material, face, seed):
    (r, g, b), style = material
    rnd = random.Random(seed)
    w, h = img.size
    light = {"up": 1.10, "down": 0.78, "north": 1.0, "south": 0.95, "east": 0.90, "west": 0.90}[face]
    px = img.load()
    for y in range(h):
        for x in range(w):
            k = light
            if style == "cloth":
                k *= 1 + rnd.uniform(-0.05, 0.05)
            elif style == "knit":
                k *= (1.09 if x % 3 == 0 else 0.97) + rnd.uniform(-0.03, 0.03)
            elif style == "denim":
                k *= (1.10 if (x + y) % 4 == 0 else 0.97) + rnd.uniform(-0.08, 0.08)
            elif style == "leather":
                k *= 1 + rnd.uniform(-0.09, 0.07) + (0.16 if (x * 3 + y) % 17 == 0 else 0)
            elif style == "metal":
                k *= 0.90 + 0.34 * max(0.0, math.sin((x + y) * 0.7)) + rnd.uniform(-0.03, 0.03)
            elif style == "straw":
                k *= (1.14 if (x // 2 + y // 2) % 2 == 0 else 0.88) + rnd.uniform(-0.05, 0.05)
            elif style == "gloss":
                k *= 1.35 if (x - y) % 9 in (0, 1) else 1.0
            elif style == "feather":
                k *= 1.0 - 0.14 * ((y + (x // 2) % 2 * 2) % 5 == 0) + rnd.uniform(-0.03, 0.02)
            if style not in ("metal", "gloss", "feather") and h > 3 and y == h - 1:
                k *= 0.86
            px[x, y] = (clamp(r * k), clamp(g * k), clamp(b * k), 255)


# ---------------------------------------------------------------- decals
def rect(x0, y0, x1, y1, color):
    """Rectangle in face fractions (0..1)."""
    def f(d, w, h):
        a, b = round(x0 * w), round(y0 * h)
        d.rectangle([a, b, max(a, round(x1 * w) - 1), max(b, round(y1 * h) - 1)], fill=color + (255,))
    return f


def pixels(rows, color, cx=0.5, cy=0.5):
    """Pixel-art stamp centred on the face."""
    def f(d, w, h):
        ox = int(round(cx * w - len(rows[0]) / 2))
        oy = int(round(cy * h - len(rows) / 2))
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch == "#":
                    d.point((ox + i, oy + j), fill=color + (255,))
    return f


LOGO_A = ["..##..",
          ".#..#.",
          "#....#",
          "######",
          "#....#",
          "#....#"]
STAR = ["...#...",
        "...#...",
        "#######",
        ".#####.",
        "..###..",
        ".##.##.",
        "#.....#"]


# ---------------------------------------------------------------- outfit
class Outfit:
    def __init__(self, oid, name, slot):
        self.id, self.name, self.slot = oid, name, slot
        self.cubes = []

    def cube(self, part, frm, to, material, inflate=0.0, rot=None, origin=None, decals=None):
        self.cubes.append(dict(part=part, frm=list(map(float, frm)), to=list(map(float, to)),
                               mat=material, inflate=inflate, rot=rot or [0, 0, 0],
                               origin=origin, decals=decals or {}))
        return self

    def mirrored(self, part_r, frm, to, material, inflate=0.0, rot=None, origin=None, decals=None):
        """Adds a cube on the right side and its mirror on the left side."""
        part_l = part_r.replace("right", "left")
        self.cube(part_r, frm, to, material, inflate, rot, origin, decals)
        mfrm = [-to[0], frm[1], frm[2]]
        mto = [-frm[0], to[1], to[2]]
        mrot = None if rot is None else [rot[0], -rot[1], -rot[2]]
        morg = None if origin is None else [-origin[0], origin[1], origin[2]]
        mdec = dict(decals or {})
        if "east" in mdec or "west" in mdec:
            mdec["east"], mdec["west"] = (decals or {}).get("west", []), (decals or {}).get("east", [])
        self.cube(part_l, mfrm, mto, material, inflate, mrot, morg, mdec)
        return self


def torso(o, m, inflate=0.35, front=None, back=None):
    o.cube("body", (-4, 12, -2), (4, 24, 2), m, inflate, decals={"north": front or [], "south": back or []})


def sleeves(o, m, bottom=12, inflate=0.35, cuff=None):
    o.mirrored("right_arm", (4, bottom, -2), (8, 24, 2), m, inflate)
    if cuff:
        o.mirrored("right_arm", (4, bottom, -2), (8, bottom + 1.2, 2), cuff, inflate + 0.15)


def legs(o, m, bottom=0.0, inflate=0.25, decals=None):
    o.mirrored("right_leg", (-0.1, bottom, -2), (3.9, 12, 2), m, inflate, decals=decals)


def build_outfits():
    out = []

    def new(oid, name, slot):
        o = Outfit(oid, name, slot)
        out.append(o)
        return o

    black, white = (28, 28, 32), (236, 236, 238)
    red, gold = (196, 34, 44), (240, 188, 48)
    navy, brown = (34, 46, 86), (112, 72, 40)
    grey = (120, 124, 132)

    # ---------------- hats
    o = new("cappellino_rosso", "Cappellino rosso", "hat")
    o.cube("head", (-4.6, 29.6, -4.6), (4.6, 32.6, 4.6), mat(red), decals={"north": [pixels(LOGO_A, white, 0.5, 0.5)]})
    o.cube("head", (-3.6, 32.6, -3.6), (3.6, 33.4, 3.6), mat(red))
    o.cube("head", (-0.7, 33.4, -0.7), (0.7, 33.9, 0.7), mat(white))
    o.cube("head", (-3.8, 29.6, -8.2), (3.8, 30.3, -4.6), mat((150, 22, 32)))

    o = new("cilindro", "Cilindro", "hat")
    o.cube("head", (-6.5, 31.6, -6.5), (6.5, 32.5, 6.5), mat(black))
    o.cube("head", (-4.2, 32.5, -4.2), (4.2, 40.5, 4.2), mat(black))
    o.cube("head", (-4.35, 32.5, -4.35), (4.35, 34.3, 4.35), mat(red, "gloss"))

    o = new("corona", "Corona d'oro", "hat")
    g = mat(gold, "metal")
    o.cube("head", (-4.6, 31.4, -4.6), (4.6, 33.4, -3.8), g)
    o.cube("head", (-4.6, 31.4, 3.8), (4.6, 33.4, 4.6), g)
    o.cube("head", (3.8, 31.4, -3.8), (4.6, 33.4, 3.8), g)
    o.cube("head", (-4.6, 31.4, -3.8), (-3.8, 33.4, 3.8), g)
    for x, z in [(-4.6, -4.6), (3.2, -4.6), (-4.6, 3.2), (3.2, 3.2), (-0.7, -4.6), (-0.7, 3.2), (-4.6, -0.7), (3.2, -0.7)]:
        o.cube("head", (x, 33.4, z), (x + 1.4, 35.0, z + 1.4), g)
        o.cube("head", (x + 0.35, 35.0, z + 0.35), (x + 1.05, 35.9, z + 1.05), g)
    o.cube("head", (-0.9, 31.7, -5.0), (0.9, 33.1, -4.6), mat((210, 20, 60), "gloss"))
    o.cube("head", (-3.4, 31.9, -4.9), (-2.2, 32.9, -4.6), mat((30, 110, 220), "gloss"))
    o.cube("head", (2.2, 31.9, -4.9), (3.4, 32.9, -4.6), mat((30, 170, 90), "gloss"))

    o = new("berretto_lana", "Berretto di lana", "hat")
    wool = (52, 88, 168)
    o.cube("head", (-4.7, 29.4, -4.7), (4.7, 31.2, 4.7), mat((36, 62, 122), "knit"))
    o.cube("head", (-4.45, 31.2, -4.45), (4.45, 33.2, 4.45), mat(wool, "knit"))
    o.cube("head", (-3.2, 33.2, -3.2), (3.2, 34.4, 3.2), mat(wool, "knit"))
    o.cube("head", (-1.6, 34.4, -1.6), (1.6, 37.0, 1.6), mat(white, "knit"))

    o = new("cappello_paglia", "Cappello di paglia", "hat")
    s = mat((222, 188, 104), "straw")
    o.cube("head", (-8, 30.8, -8), (8, 31.5, 8), s)
    o.cube("head", (-4.5, 31.5, -4.5), (4.5, 34.8, 4.5), s)
    o.cube("head", (-4.65, 31.5, -4.65), (4.65, 32.7, 4.65), mat(red))

    o = new("cappello_cowboy", "Cappello da cowboy", "hat")
    c = mat(brown, "leather")
    o.cube("head", (-6, 31.0, -7), (6, 31.8, 7), c)
    o.mirrored("head", (5.6, 31.4, -6), (8.4, 32.1, 6), c, rot=[0, 0, 28], origin=[5.6, 31.4, 0])
    o.cube("head", (-4.4, 31.8, -4.4), (4.4, 35.2, 4.4), c)
    o.mirrored("head", (0.8, 35.2, -4.0), (4.0, 36.2, 4.0), c)
    o.cube("head", (-4.55, 31.8, -4.55), (4.55, 33.0, 4.55), mat(black, "leather"))
    o.cube("head", (-1.0, 31.9, -4.95), (1.0, 32.9, -4.55), mat(gold, "metal"))

    # ---------------- tops
    o = new("felpa_adryh", "Felpa Adryh", "top")
    hoodie = mat((30, 30, 36), "knit")
    torso(o, hoodie, front=[pixels(LOGO_A, white, 0.5, 0.52)],
          back=[pixels(LOGO_A, white, 0.5, 0.4)])
    sleeves(o, hoodie, cuff=mat(white, "knit"))
    o.cube("body", (-3.2, 13.0, -3.0), (3.2, 16.4, -2.3), mat((44, 44, 52), "knit"))
    o.cube("body", (-4.5, 12, -2.5), (4.5, 13.1, 2.5), mat(white, "knit"))
    o.cube("body", (-3.8, 21.2, 2.3), (3.8, 25.2, 4.6), hoodie)
    o.mirrored("body", (0.8, 20.6, -2.8), (1.4, 23.6, -2.35), mat(white))

    o = new("giacca_pelle", "Giacca di pelle", "top")
    lt = mat((40, 28, 24), "leather")
    torso(o, lt, 0.4, front=[rect(0.28, 0.0, 0.72, 1.0, white), rect(0.25, 0.0, 0.29, 1.0, grey),
                             rect(0.71, 0.0, 0.75, 1.0, grey), rect(0.05, 0.6, 0.2, 0.63, grey),
                             rect(0.8, 0.6, 0.95, 0.63, grey)])
    sleeves(o, lt, inflate=0.4, cuff=mat(black, "leather"))
    o.mirrored("body", (1.4, 21.4, -3.1), (4.0, 24.4, -2.4), lt, rot=[0, 0, -22], origin=[2.7, 23, -2.7])
    o.cube("body", (-3.6, 23.6, 1.6), (3.6, 25.0, 2.9), lt)
    o.cube("body", (-4.55, 12.2, -2.6), (4.55, 13.4, 2.6), mat(black, "leather"))
    o.cube("body", (-1.0, 12.3, -2.95), (1.0, 13.3, -2.6), mat((200, 204, 212), "metal"))

    o = new("smoking", "Smoking", "top")
    sm = mat((22, 22, 28))
    torso(o, sm, front=[rect(0.33, 0.0, 0.67, 0.62, white), rect(0.40, 0.62, 0.60, 0.70, white),
                        rect(0.47, 0.16, 0.53, 0.62, black), rect(0.47, 0.76, 0.53, 0.80, gold),
                        rect(0.47, 0.88, 0.53, 0.92, gold), rect(0.74, 0.22, 0.90, 0.28, white)])
    sleeves(o, sm, cuff=mat(white))
    o.mirrored("body", (0.25, 22.3, -3.0), (2.1, 23.7, -2.4), mat(red, "gloss"))
    o.cube("body", (-0.45, 22.6, -3.15), (0.45, 23.4, -2.45), mat((140, 16, 26)))
    o.mirrored("body", (1.7, 17.5, -2.75), (2.9, 23.6, -2.4), mat((10, 10, 14), "gloss"), rot=[0, 0, -14], origin=[2.3, 23.6, -2.5])

    o = new("maglia_calcio", "Maglia da calcio 10", "top")
    az = mat((30, 96, 190))
    ten = ["..#...####.", ".##...#..#.", "..#...#..#.", "..#...#..#.", "..#...#..#.", "..#...#..#.", ".###..####."]
    torso(o, az, front=[rect(0.0, 0.0, 1.0, 0.08, white), pixels(STAR, gold, 0.5, 0.4)],
          back=[rect(0.0, 0.0, 1.0, 0.08, white), pixels(ten, white, 0.5, 0.45)])
    sleeves(o, az, bottom=18, cuff=mat(white))

    o = new("giubbotto", "Giubbotto tattico", "top")
    tac = mat((74, 82, 52))
    o.cube("body", (-4.4, 14.5, -2.9), (4.4, 22.6, 2.9), tac, decals={"south": [rect(0.15, 0.2, 0.85, 0.34, (30, 34, 24))]})
    o.mirrored("body", (1.8, 22.6, -2.6), (3.8, 24.7, 2.6), tac)
    for x in (-3.9, -1.2, 1.5):
        o.cube("body", (x, 14.9, -3.9), (x + 2.4, 18.2, -2.9), mat((56, 62, 40)))
        o.cube("body", (x, 17.4, -4.05), (x + 2.4, 18.3, -2.9), mat((40, 46, 30)))
    o.cube("body", (-3.6, 19.4, -3.4), (-0.6, 21.6, -2.9), mat((30, 34, 24)))
    o.cube("body", (2.2, 19.0, -3.8), (3.4, 23.4, -2.9), mat(black, "gloss"))
    o.cube("body", (-4.55, 14.0, -3.05), (4.55, 15.0, 3.05), mat(black, "leather"))

    o = new("piumino_rosso", "Piumino rosso", "top")
    pf = mat(red)
    for i, y in enumerate((12.0, 15.0, 18.0, 21.0)):
        puff = 0.75 if i % 2 == 0 else 0.62
        o.cube("body", (-4, y, -2), (4, y + 3, 2), pf, puff, decals={"north": [rect(0.47, 0, 0.53, 1, black)]})
    for y in (14.0, 17.5, 21.0):
        o.mirrored("right_arm", (4, y, -2), (8, y + 3, 2), pf, 0.65 if y == 17.5 else 0.5)
    o.mirrored("right_arm", (4, 12.6, -2), (8, 14.0, 2), mat(black, "knit"), 0.35)
    o.cube("body", (-3.6, 23.4, -3.0), (3.6, 25.4, 3.0), mat((120, 16, 24)))

    # ---------------- pants
    def waist(o, m):
        o.cube("body", (-4, 12, -2), (4, 13.4, 2), m, 0.27)

    o = new("jeans", "Jeans", "pants")
    dn = mat((56, 90, 150), "denim")
    legs(o, dn, decals={"north": [rect(0.1, 0.12, 0.9, 0.15, (200, 160, 80))]})
    waist(o, mat(brown, "leather"))
    o.cube("body", (-1.1, 12.1, -2.75), (1.1, 13.3, -2.27), mat(gold, "metal"))
    o.mirrored("right_leg", (0.6, 4.6, -2.55), (3.2, 6.0, -2.25), mat((44, 72, 124), "denim"))

    o = new("cargo", "Pantaloni cargo", "pants")
    cg = mat((88, 96, 62))
    legs(o, cg)
    waist(o, mat(black))
    o.mirrored("right_leg", (3.9, 4.4, -1.7), (5.0, 8.6, 1.7), mat((70, 78, 50)))
    o.mirrored("right_leg", (3.9, 7.8, -1.9), (5.2, 8.9, 1.9), mat((54, 60, 38)))

    o = new("tuta", "Tuta nera", "pants")
    legs(o, mat(black, "knit"), decals={"east": [rect(0.3, 0, 0.45, 1, white), rect(0.55, 0, 0.7, 1, white)]})
    waist(o, mat(black, "knit"))
    o.mirrored("right_leg", (-0.1, 0.0, -2), (3.9, 1.8, 2), mat((50, 50, 58), "knit"), 0.45)

    o = new("shorts", "Pantaloncini", "pants")
    sh = mat((214, 190, 148))
    legs(o, sh, bottom=6.0, inflate=0.3)
    waist(o, sh)
    o.mirrored("right_leg", (-0.1, 6.0, -2), (3.9, 7.0, 2), mat((180, 156, 116)), 0.5)

    # ---------------- shoes
    def sneaker(o, main, sole, accent):
        o.mirrored("right_leg", (-0.1, 1.0, -2), (3.9, 4.0, 2), mat(main, "leather"), 0.55,
                   decals={"east": [rect(0.1, 0.45, 0.9, 0.65, accent)]})
        o.mirrored("right_leg", (-0.4, 1.0, -3.9), (4.2, 2.6, -2.4), mat(main, "leather"))
        o.mirrored("right_leg", (-0.75, -0.3, -4.2), (4.55, 1.0, 2.75), mat(sole))
        o.mirrored("right_leg", (0.7, 2.6, -3.0), (3.1, 3.2, -2.5), mat(accent))
        o.mirrored("right_leg", (0.7, 3.6, -3.0), (3.1, 4.2, -2.5), mat(accent))

    o = new("sneakers_bianche", "Sneakers bianche e rosse", "shoes")
    sneaker(o, white, (200, 200, 204), red)

    o = new("sneakers_nere", "Sneakers nere e oro", "shoes")
    sneaker(o, black, white, gold)

    o = new("stivali", "Stivali marroni", "shoes")
    st = mat(brown, "leather")
    o.mirrored("right_leg", (-0.1, 1.0, -2), (3.9, 6.5, 2), st, 0.55)
    o.mirrored("right_leg", (-0.4, 0.8, -3.8), (4.2, 3.0, -2.4), st)
    o.mirrored("right_leg", (-0.1, 6.0, -2), (3.9, 7.2, 2), mat((150, 102, 62), "leather"), 0.75)
    o.mirrored("right_leg", (-0.75, -0.3, -4.1), (4.55, 0.9, 2.75), mat((46, 30, 20)))

    o = new("anfibi", "Anfibi neri", "shoes")
    an = mat(black, "leather")
    o.mirrored("right_leg", (-0.1, 1.2, -2), (3.9, 7.0, 2), an, 0.55,
               decals={"north": [rect(0.25, y, 0.75, y + 0.06, grey) for y in (0.1, 0.28, 0.46, 0.64)]})
    o.mirrored("right_leg", (-0.4, 1.0, -3.9), (4.2, 3.0, -2.4), an)
    o.mirrored("right_leg", (-0.85, -0.4, -4.3), (4.65, 1.2, 2.85), mat((16, 16, 18)))

    # ---------------- accessories
    o = new("occhiali_sole", "Occhiali da sole", "face")
    fr = mat(black, "gloss")
    o.mirrored("head", (0.5, 26.9, -5.0), (3.7, 29.0, -4.55), mat((26, 30, 44), "gloss"))
    o.cube("head", (-4.4, 28.7, -5.1), (4.4, 29.4, -4.55), fr)
    o.mirrored("head", (4.3, 28.5, -5.0), (4.75, 29.2, 0.6), fr)

    o = new("cuffie", "Cuffie gaming", "face")
    o.cube("head", (-5.4, 32.4, -1.0), (5.4, 33.4, 1.0), mat(black, "leather"))
    o.mirrored("head", (4.6, 28.0, -1.0), (5.5, 33.4, 1.0), mat(black, "leather"))
    o.mirrored("head", (4.4, 25.4, -2.1), (6.2, 29.8, 2.1), mat(black, "gloss"))
    o.mirrored("head", (6.2, 26.4, -1.1), (6.6, 28.8, 1.1), mat(red, "gloss"))
    o.mirrored("head", (4.9, 25.6, -5.2), (5.5, 26.2, -1.8), mat(black))
    o.cube("head", (3.2, 25.3, -5.5), (4.9, 26.5, -4.7), mat(red))

    o = new("sciarpa", "Sciarpa", "neck")
    sc = mat(red, "knit")
    o.cube("body", (-4.4, 22.4, -3.3), (4.4, 24.9, 3.3), sc, decals={"north": [rect(0, 0.4, 1, 0.65, white)]})
    o.cube("body", (0.9, 16.6, -3.9), (3.5, 22.8, -3.0), sc,
           decals={"north": [rect(0, 0.72, 1, 0.82, white), rect(0, 0.9, 1, 1, white)]})

    o = new("zaino", "Zaino", "back")
    bag = mat((224, 118, 30))
    o.cube("body", (-3.6, 13.6, 2.4), (3.6, 22.6, 6.0), bag)
    o.cube("body", (-3.0, 14.2, 6.0), (3.0, 18.4, 7.2), mat((186, 92, 20)))
    o.cube("body", (-3.7, 21.0, 2.5), (3.7, 23.0, 6.2), mat((186, 92, 20)))
    o.cube("body", (-0.8, 20.4, 6.2), (0.8, 21.6, 6.5), mat((200, 204, 212), "metal"))
    o.mirrored("body", (1.9, 15.0, -2.9), (3.1, 24.2, -2.35), mat(black))
    o.mirrored("body", (1.9, 23.6, -2.9), (3.1, 24.6, 2.8), mat(black))

    o = new("ali_angelo", "Ali d'angelo", "back")
    fw = mat((244, 246, 252), "feather")
    o.mirrored("body", (0.6, 20.0, 2.8), (7.0, 23.0, 3.6), fw, rot=[0, -22, 22], origin=[0.8, 21, 3.2])
    o.mirrored("body", (5.6, 22.8, 4.9), (14.0, 25.2, 5.6), fw, rot=[0, -26, 30], origin=[5.8, 23.5, 5.2])
    for i in range(6):
        x = 2.0 + i * 1.9
        top = 21.2 + i * 1.15
        length = 6.0 + i * 0.9
        z = 3.3 + i * 0.52
        o.mirrored("body", (x, top - length, z), (x + 1.8, top, z + 0.45), fw,
                   rot=[0, -18, 10 + i * 4], origin=[x + 0.9, top, z])

    o = new("mantello", "Mantello rosso", "back")
    cape = mat((168, 24, 36))
    o.cube("body", (-4.8, 4.0, 3.2), (4.8, 23.6, 3.9), cape, rot=[-9, 0, 0], origin=[0, 23.6, 3.2],
           decals={"south": [rect(0, 0.93, 1, 0.97, gold), pixels(LOGO_A, gold, 0.5, 0.35)]})
    o.cube("body", (-4.9, 23.0, 2.1), (4.9, 24.6, 3.9), cape)
    o.mirrored("body", (2.6, 22.4, -2.9), (4.2, 24.0, -2.3), mat(gold, "metal"))
    o.cube("body", (-2.6, 22.9, -2.8), (2.6, 23.4, -2.35), mat(gold, "metal"))
    return out


# ---------------------------------------------------------------- texture + geometry
def face_size(c, face):
    sx, sy, sz = (c["to"][i] - c["frm"][i] for i in range(3))
    return {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy),
            "up": (sx, sz), "down": (sx, sz)}[face]


def make_atlas(o):
    tiles = []
    for ci, c in enumerate(o.cubes):
        for face in FACES:
            w, h = face_size(c, face)
            pw, ph = max(1, int(math.ceil(w * PPU - 1e-6))), max(1, int(math.ceil(h * PPU - 1e-6)))
            img = Image.new("RGBA", (pw, ph))
            paint_material(img, c["mat"], face, zlib.crc32(f"{o.id}:{ci}:{face}".encode()))
            d = ImageDraw.Draw(img)
            for decal in c["decals"].get(face, []):
                decal(d, pw, ph)
            tiles.append((ci, face, img))
    x = y = row = 0
    placed = {}
    for ci, face, img in sorted(tiles, key=lambda t: -t[2].height):
        if x + img.width > ATLAS_W:
            x, y, row = 0, y + row + 1, 0
        placed[(ci, face)] = (x, y, img)
        x += img.width + 1
        row = max(row, img.height)
    height = 16
    while height < y + row:
        height *= 2
    atlas = Image.new("RGBA", (ATLAS_W, height), (0, 0, 0, 0))
    uvs = {}
    for key, (px, py, img) in placed.items():
        atlas.paste(img, (px, py))
        uvs[key] = [px, py, px + img.width, py + img.height]
    return atlas, uvs


def corners(c, face):
    """Quad corners in the order top-left, top-right, bottom-right, bottom-left."""
    i = c["inflate"]
    x1, y1, z1 = (v - i for v in c["frm"])
    x2, y2, z2 = (v + i for v in c["to"])
    return {
        "north": [(x2, y2, z1), (x1, y2, z1), (x1, y1, z1), (x2, y1, z1)],
        "south": [(x1, y2, z2), (x2, y2, z2), (x2, y1, z2), (x1, y1, z2)],
        "east": [(x2, y2, z2), (x2, y2, z1), (x2, y1, z1), (x2, y1, z2)],
        "west": [(x1, y2, z1), (x1, y2, z2), (x1, y1, z2), (x1, y1, z1)],
        "up": [(x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2)],
        "down": [(x1, y1, z2), (x2, y1, z2), (x2, y1, z1), (x1, y1, z1)],
    }[face]


def rotate(p, rot, origin):
    """Blockbench cube rotation: v' = Rz*Ry*Rx*(v-o) + o."""
    x, y, z = (p[i] - origin[i] for i in range(3))
    rx, ry, rz = (math.radians(a) for a in rot)
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return (x + origin[0], y + origin[1], z + origin[2])


def cube_origin(c):
    return c["origin"] or [(c["frm"][i] + c["to"][i]) / 2 for i in range(3)]


def quads(o, uvs, atlas):
    for ci, c in enumerate(o.cubes):
        for face in FACES:
            pts = [rotate(p, c["rot"], cube_origin(c)) for p in corners(c, face)]
            u1, v1, u2, v2 = uvs[(ci, face)]
            uv = [(u1, v1), (u2, v1), (u2, v2), (u1, v2)]
            uv = [(u / atlas.width, v / atlas.height) for u, v in uv]
            yield c["part"], pts, uv


PIVOTS = {"head": [0, 24, 0], "body": [0, 24, 0], "right_arm": [5, 22, 0], "left_arm": [-5, 22, 0],
          "right_leg": [1.9, 12, 0], "left_leg": [-1.9, 12, 0]}


def uid(*parts):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "wardrobe/" + "/".join(map(str, parts))))


def png_bytes(img):
    b = io.BytesIO()
    img.save(b, "PNG", optimize=True)
    return b.getvalue()


def write_bbmodel(o, atlas, uvs, path):
    elements, groups = [], {}
    for ci, c in enumerate(o.cubes):
        eid = uid(o.id, ci)
        elements.append({
            "name": f"{c['part']}_{ci}", "type": "cube", "box_uv": False, "uuid": eid,
            "from": c["frm"], "to": c["to"], "inflate": c["inflate"],
            "origin": cube_origin(c), "rotation": c["rot"],
            "faces": {f: {"uv": uvs[(ci, f)], "texture": 0} for f in FACES},
        })
        groups.setdefault(c["part"], []).append(eid)
    data = {
        "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
        "name": o.id,
        "wardrobe": {"name": o.name, "slot": o.slot, "author": "@Adryh01"},
        "resolution": {"width": atlas.width, "height": atlas.height},
        "elements": elements,
        "outliner": [{"name": part, "origin": PIVOTS[part], "uuid": uid(o.id, part), "children": kids}
                     for part, kids in groups.items()],
        "textures": [{"name": o.id + ".png", "id": "0", "uuid": uid(o.id, "tex"),
                      "width": atlas.width, "height": atlas.height,
                      "uv_width": atlas.width, "uv_height": atlas.height,
                      "source": "data:image/png;base64," + base64.b64encode(png_bytes(atlas)).decode()}],
    }
    path.write_text(json.dumps(data, separators=(",", ":")))


def write_obj(o, atlas, uvs, folder):
    folder.mkdir(parents=True, exist_ok=True)
    big = atlas.resize((atlas.width * 8, atlas.height * 8), Image.NEAREST)  # stays sharp in Blender
    big.save(folder / f"{o.id}.png", optimize=True)
    (folder / f"{o.id}.mtl").write_text(
        f"newmtl {o.id}\nKd 1 1 1\nKs 0 0 0\nillum 1\nmap_Kd {o.id}.png\n")
    lines = [f"# {o.name} - Wardrobe by @Adryh01", f"mtllib {o.id}.mtl"]
    by_part = {}
    for part, pts, uv in quads(o, uvs, atlas):
        by_part.setdefault(part, []).append((pts, uv))
    n = 0
    for part, items in by_part.items():
        lines += [f"o {o.id}_{part}", f"usemtl {o.id}"]
        for pts, uv in items:
            # 16 model units = 1 metre
            lines += ["v %.5f %.5f %.5f" % tuple(v / 16 for v in p) for p in pts]
            lines += ["vt %.6f %.6f" % (u, 1 - v) for u, v in uv]
            idx = [n + 4, n + 3, n + 2, n + 1]
            lines.append("f " + " ".join(f"{i}/{i}" for i in idx))
            n += 4
    (folder / f"{o.id}.obj").write_text("\n".join(lines) + "\n")


# ---------------------------------------------------------------- preview renderer
def mannequin():
    o = Outfit("mannequin", "mannequin", "none")
    skin, shirt, trousers = mat((196, 150, 118)), mat((150, 152, 160)), mat((110, 112, 122))
    o.cube("head", (-4, 24, -4), (4, 32, 4), skin,
           decals={"north": [rect(0.15, 0.45, 0.35, 0.58, (50, 50, 70)), rect(0.65, 0.45, 0.85, 0.58, (50, 50, 70))]})
    o.cube("body", (-4, 12, -2), (4, 24, 2), shirt)
    o.mirrored("right_arm", (4, 12, -2), (8, 24, 2), skin)
    o.mirrored("right_leg", (-0.1, 0, -2), (3.9, 12, 2), trousers)
    return o


def render(models, yaw, size=(240, 320), scale=6.0, center=(0, 19, 0), pitch=8.0):
    w, h = size
    color = np.zeros((h, w, 4), np.uint8)
    depth = np.full((h, w), np.inf)
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

    def project(p):
        x, y, z = (p[i] - center[i] for i in range(3))
        x, z = x * cy + z * sy, -x * sy + z * cy
        y, z = y * cp + z * sp, -y * sp + z * cp
        # the camera sits at -Z, so the player's right (+X) appears on the left
        return np.array([w / 2 - x * scale, h / 2 - y * scale, z])

    for o, atlas, uvs in models:
        tex = np.array(atlas)
        for _, pts, uv in quads(o, uvs, atlas):
            p = [project(v) for v in pts]
            a, b = p[1] - p[0], p[3] - p[0]
            nrm = np.cross(a * [1 / scale, 1 / scale, 1], b * [1 / scale, 1 / scale, 1])
            length = np.linalg.norm(nrm)
            shade = 0.62 + 0.38 * (abs(nrm[2]) / length if length else 1)
            for tri in ((0, 1, 2), (0, 2, 3)):
                v = np.array([p[i] for i in tri])
                t = np.array([uv[i] for i in tri])
                x0, x1 = max(0, int(v[:, 0].min())), min(w - 1, int(v[:, 0].max()) + 1)
                y0, y1 = max(0, int(v[:, 1].min())), min(h - 1, int(v[:, 1].max()) + 1)
                if x1 < x0 or y1 < y0:
                    continue
                gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
                den = ((v[1, 1] - v[2, 1]) * (v[0, 0] - v[2, 0]) + (v[2, 0] - v[1, 0]) * (v[0, 1] - v[2, 1]))
                if abs(den) < 1e-9:
                    continue
                l0 = ((v[1, 1] - v[2, 1]) * (gx - v[2, 0]) + (v[2, 0] - v[1, 0]) * (gy - v[2, 1])) / den
                l1 = ((v[2, 1] - v[0, 1]) * (gx - v[2, 0]) + (v[0, 0] - v[2, 0]) * (gy - v[2, 1])) / den
                l2 = 1 - l0 - l1
                inside = (l0 >= -1e-6) & (l1 >= -1e-6) & (l2 >= -1e-6)
                z = l0 * v[0, 2] + l1 * v[1, 2] + l2 * v[2, 2]
                u = l0 * t[0, 0] + l1 * t[1, 0] + l2 * t[2, 0]
                vv = l0 * t[0, 1] + l1 * t[1, 1] + l2 * t[2, 1]
                tx = np.clip((u * tex.shape[1]).astype(int), 0, tex.shape[1] - 1)
                ty = np.clip((vv * tex.shape[0]).astype(int), 0, tex.shape[0] - 1)
                sample = tex[ty, tx]
                reg_d = depth[y0:y1 + 1, x0:x1 + 1]
                reg_c = color[y0:y1 + 1, x0:x1 + 1]
                ok = inside & (z < reg_d) & (sample[..., 3] > 127)
                reg_d[ok] = z[ok]
                shaded = sample.astype(float)
                shaded[..., :3] *= shade
                reg_c[ok] = shaded[ok].astype(np.uint8)
    return Image.fromarray(color, "RGBA")


def catalog(built):
    DOCS.mkdir(exist_ok=True)
    man = mannequin()
    man_t = (man, *make_atlas(man))
    cols, cw, ch = 5, 440, 380
    rows = math.ceil(len(built) / cols)
    sheet = Image.new("RGB", (cols * cw, rows * ch + 70), (24, 24, 30))
    d = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("DejaVuSans-Bold.ttf", 22)
        big = ImageFont.truetype("DejaVuSans-Bold.ttf", 34)
    except OSError:
        font = big = ImageFont.load_default()
    d.text((20, 16), f"WARDROBE  -  {len(built)} capi 3D  -  by @Adryh01", font=big, fill=(240, 240, 244))
    for i, item in enumerate(built):
        x, y = (i % cols) * cw, (i // cols) * ch + 70
        d.rounded_rectangle([x + 8, y + 8, x + cw - 8, y + ch - 8], 14, fill=(44, 38, 34))
        for j, yaw in enumerate((-30, 150)):
            img = render([man_t, item], yaw, size=(210, 310), scale=6.2, center=(0, 20, 0))
            sheet.paste(img, (x + 10 + j * 210, y + 14), img)
        d.text((x + 20, y + ch - 42), item[0].name, font=font, fill=(240, 230, 214))
    sheet.save(DOCS / "catalogo.png", optimize=True)


def main():
    for folder in (OUTFITS, BB_DIR, BLENDER_DIR):
        shutil.rmtree(folder, ignore_errors=True)
        folder.mkdir(parents=True)
    index, built = [], []
    for o in build_outfits():
        atlas, uvs = make_atlas(o)
        write_bbmodel(o, atlas, uvs, OUTFITS / f"{o.id}.bbmodel")
        shutil.copy(OUTFITS / f"{o.id}.bbmodel", BB_DIR / f"{o.id}.bbmodel")
        write_obj(o, atlas, uvs, BLENDER_DIR / o.slot)
        index.append({"id": o.id, "name": o.name, "slot": o.slot})
        built.append((o, atlas, uvs))
    (OUTFITS / "index.json").write_text(json.dumps(index, indent=1, ensure_ascii=False))
    catalog(built)
    print(len(built), "outfits,", sum(len(o.cubes) for o, _, _ in built), "cubes")


if __name__ == "__main__":
    main()
