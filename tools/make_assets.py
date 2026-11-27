#!/usr/bin/env python3
"""Generates the Aura textures and copies the fonts. By @Adryh01."""
import json
import math
import shutil
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
MENU = ROOT / "src/menu/resources"
GUI = MENU / "assets/minecraft/textures/gui"
SPR = GUI / "sprites"
S = 4  # texture pixels per GUI pixel (also the anti-aliasing resolution)


def rounded(w, h, radius, fill, outline=None, top_light=0, ss=4):
    """Anti-aliased rounded rectangle, sizes in texture pixels."""
    big = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    box = [0, 0, w * ss - 1, h * ss - 1]
    d.rounded_rectangle(box, radius * ss, fill=outline or fill)
    if outline:
        t = S * ss
        d.rounded_rectangle([t, t, w * ss - 1 - t, h * ss - 1 - t], max(0, radius * ss - t), fill=fill)
    img = big.resize((w, h), Image.LANCZOS)
    if top_light:
        px = img.load()
        for y in range(h):
            k = max(0.0, 1 - y / (h * 0.7)) * top_light
            for x in range(w):
                r, g, b, a = px[x, y]
                px[x, y] = (min(255, int(r + k)), min(255, int(g + k)), min(255, int(b + k)), a)
    return img


def sprite(name, img, w, h, border=None):
    path = SPR / (name + ".png")
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, optimize=True)
    if border is not None:
        meta = {"gui": {"scaling": {"type": "nine_slice", "width": w, "height": h, "border": border}}}
        Path(str(path) + ".mcmeta").write_text(json.dumps(meta))


def box(name, w, h, fill, outline, border=5, light=0):
    sprite(name, rounded(w * S, h * S, 4 * S, fill, outline, light), w, h, border)


def widgets():
    normal, edge = (14, 16, 26, 170), (255, 255, 255, 46)
    box("widget/button", 200, 20, normal, edge, light=14)
    box("widget/button_highlighted", 200, 20, (52, 58, 86, 215), (255, 255, 255, 200), light=22)
    box("widget/button_disabled", 200, 20, (10, 10, 14, 120), (255, 255, 255, 18))
    box("widget/slider", 200, 20, (8, 9, 16, 190), edge)
    box("widget/slider_highlighted", 200, 20, (8, 9, 16, 210), (255, 255, 255, 150))
    box("widget/slider_handle", 8, 20, (170, 176, 205, 240), (255, 255, 255, 220), border=3)
    box("widget/slider_handle_highlighted", 8, 20, (255, 255, 255, 255), (255, 255, 255, 255), border=3)
    box("widget/text_field", 200, 20, (6, 7, 12, 225), (255, 255, 255, 60))
    box("widget/text_field_highlighted", 200, 20, (6, 7, 12, 235), (255, 255, 255, 210))

    # hotbar used when the modern HUD bar is switched off
    hot = rounded(182 * S, 22 * S, 5 * S, (12, 14, 22, 165), (255, 255, 255, 50), 10)
    d = ImageDraw.Draw(hot, "RGBA")
    for i in range(1, 9):
        x = (1 + i * 20) * S
        d.rectangle([x - 1, 5 * S, x + 1, 17 * S], fill=(255, 255, 255, 28))
    sprite("hud/hotbar", hot, 182, 22)
    sel = rounded(24 * S, 23 * S, 6 * S, (255, 255, 255, 255))
    hole = rounded(20 * S, 19 * S, 4 * S, (255, 255, 255, 255))
    alpha = sel.getchannel("A")
    inner = Image.new("L", sel.size, 0)
    inner.paste(hole.getchannel("A"), (2 * S, 2 * S))
    sel.putalpha(Image.composite(Image.new("L", sel.size, 0), alpha, inner))
    sprite("hud/hotbar_selection", sel, 24, 23)
    sprite("hud/hotbar_offhand_left", rounded(29 * S, 24 * S, 5 * S, (12, 14, 22, 150)), 29, 24)
    sprite("hud/hotbar_offhand_right", rounded(29 * S, 24 * S, 5 * S, (12, 14, 22, 150)), 29, 24)


def flat(path, rgba, size=16):
    path.parent.mkdir(parents=True, exist_ok=True)
    Image.new("RGBA", (size, size), rgba).save(path, optimize=True)


def menu_backgrounds():
    for prefix in ("", "inworld_"):
        flat(GUI / f"{prefix}menu_background.png", (6, 8, 16, 70))
        flat(GUI / f"{prefix}menu_list_background.png", (4, 5, 10, 140))
    for prefix in ("", "inworld_"):
        for name in ("header_separator", "footer_separator"):
            img = Image.new("RGBA", (32, 2), (255, 255, 255, 40))
            img.save(GUI / f"{prefix}{name}.png")


def radial(path, size, curve):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c) / c
            px[x, y] = (255, 255, 255, int(255 * max(0.0, min(1.0, curve(d)))))
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, optimize=True)
    Path(str(path) + ".mcmeta").write_text('{"texture": {"blur": true, "clamp": true}}')


def icons():
    """White 64 px line icons for the Aura Studio sidebar."""
    def new():
        img = Image.new("RGBA", (256, 256), (255, 255, 255, 0))
        return img, ImageDraw.Draw(img)
    white, clear, w = (255, 255, 255, 255), (255, 255, 255, 0), 20
    out = {}
    img, d = new()  # painter's palette
    d.ellipse([16, 28, 240, 228], outline=white, width=w)
    for x, y in ((80, 88), (140, 72), (188, 112), (78, 150)):
        d.ellipse([x - 17, y - 17, x + 17, y + 17], fill=white)
    d.ellipse([130, 140, 196, 200], outline=white, width=w)
    out["menu"] = img
    img, d = new()  # screen with widgets
    d.rounded_rectangle([14, 36, 242, 220], 34, outline=white, width=w)
    d.rounded_rectangle([50, 74, 116, 112], 12, fill=white)
    d.rounded_rectangle([140, 74, 206, 96], 10, fill=white)
    d.rounded_rectangle([50, 158, 206, 184], 12, fill=white)
    out["hud"] = img
    img, d = new()  # crescent and stars
    d.ellipse([30, 38, 206, 214], fill=white)
    d.ellipse([88, 10, 250, 172], fill=clear)
    for x, y, r in ((190, 62, 30), (226, 150, 18)):
        d.polygon([(x, y - r), (x + r * .28, y - r * .28), (x + r, y), (x + r * .28, y + r * .28), (x, y + r),
                   (x - r * .28, y + r * .28), (x - r, y), (x - r * .28, y - r * .28)], fill=white)
    out["sky"] = img
    img, d = new()  # crosshair
    d.ellipse([48, 48, 208, 208], outline=white, width=w)
    for box_ in ([116, 8, 140, 92], [116, 164, 140, 248], [8, 116, 92, 140], [164, 116, 248, 140]):
        d.rounded_rectangle(box_, 12, fill=white)
    out["fx"] = img
    img, d = new()  # backpack
    d.arc([84, 14, 172, 110], 180, 360, fill=white, width=w)
    d.rounded_rectangle([42, 62, 214, 238], 48, outline=white, width=w)
    d.rounded_rectangle([82, 140, 174, 196], 18, outline=white, width=w)
    d.line([64, 114, 192, 114], fill=white, width=w)
    out["inventory"] = img
    folder = MENU / "assets/auramenu/textures/gui"
    folder.mkdir(parents=True, exist_ok=True)
    for name, img in out.items():
        path = folder / f"icon_{name}.png"
        img.resize((64, 64), Image.LANCZOS).save(path, optimize=True)
        Path(str(path) + ".mcmeta").write_text('{"texture": {"blur": true, "clamp": true}}')


def fonts():
    out = MENU / "assets/auramenu/font"
    out.mkdir(parents=True, exist_ok=True)
    for src, name in (("Inter-Medium.otf", "inter"), ("Inter-Bold.otf", "inter_bold")):
        shutil.copy(f"/usr/share/fonts/opentype/inter/{src}", out / f"{name}.otf")
        (out / f"{name}.json").write_text(json.dumps({"providers": [
            {"type": "ttf", "file": f"auramenu:{name}.otf", "size": 9.0, "oversample": 6.0, "shift": [0.0, 0.5]},
            {"type": "reference", "id": "minecraft:default"}]}))
    text = Path("/usr/share/doc/fonts-inter/copyright").read_text()
    (out / "OFL-Inter.txt").write_text("Font: Inter (https://github.com/rsms/inter)\n\n" + text)


def main():
    widgets()
    menu_backgrounds()
    fonts()
    icons()
    radial(MENU / "assets/auramenu/textures/gui/glow.png", 128, lambda d: math.exp(-4.2 * d * d) - 0.015)
    radial(ROOT / "src/fx/resources/assets/aurafx/textures/gui/vignette.png", 256,
           lambda d: ((d - 0.45) / 0.75) ** 1.6 if d > 0.45 else 0)
    for mod, name, desc in (("menu", "Aura Menu", "Menu principale moderno, pulsanti in vetro e Aura Studio."),
                            ("hud", "Aura HUD", "Barra moderna e riquadri informativi spostabili."),
                            ("sky", "Aura Sky", "Cielo, aurora, stelle, nebbia e orario visivo."),
                            ("fx", "Aura FX", "Mirino, contorno blocchi, zoom, particelle e vignetta."),
                            ("inventory", "Aura Inventory", "Inventari scuri, animazioni, rarita' e tooltip.")):
        res = ROOT / "src" / mod / "resources"
        (res / "META-INF").mkdir(parents=True, exist_ok=True)
        modid = "aura" + mod
        toml = f'''modLoader="javafml"
loaderVersion="[52,)"
license="All Rights Reserved - @Adryh01"
clientSideOnly=true

[[mods]]
modId="{modid}"
version="1.0.0"
displayName="{name}"
authors="@Adryh01"
displayTest="IGNORE_ALL_VERSION"
description="{desc}"

[[dependencies.{modid}]]
    modId="forge"
    mandatory=true
    versionRange="[52,)"
    ordering="NONE"
    side="CLIENT"

[[dependencies.{modid}]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.21.1]"
    ordering="NONE"
    side="CLIENT"
'''
        if mod != "menu":
            toml += f'''
[[dependencies.{modid}]]
    modId="auramenu"
    mandatory=true
    versionRange="[1.0.0,)"
    ordering="AFTER"
    side="CLIENT"
'''
        (res / "META-INF/mods.toml").write_text(toml)
        (res / "pack.mcmeta").write_text(json.dumps({"pack": {"description": name + " by @Adryh01", "pack_format": 34}}, indent=2))
    print("assets ok")


if __name__ == "__main__":
    main()
