#!/usr/bin/env python3
"""Packs the built jars as a CurseForge zip, a Modrinth pack and a plain mods zip."""
import json
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VERSION = "1.1.0"
jars = sorted((ROOT / "build/libs").glob("mythoria-*.jar"))
if len(jars) != 5:
    sys.exit(f"expected 5 jars, found {[j.name for j in jars]}")
dist = ROOT / "dist"
dist.mkdir(exist_ok=True)
readme = (ROOT / "INSTALL.txt").read_text()


def write(name, prefix, extra):
    with zipfile.ZipFile(dist / name, "w", zipfile.ZIP_DEFLATED) as z:
        for path, data in extra.items():
            z.writestr(path, data)
        for jar in jars:
            z.write(jar, prefix + jar.name)
        z.writestr("INSTALL.txt", readme)


write("Mythoria-1.21.1-CurseForge.zip", "overrides/mods/", {"manifest.json": json.dumps({
    "minecraft": {"version": "1.21.1", "modLoaders": [{"id": "forge-52.1.20", "primary": True}]},
    "manifestType": "minecraftModpack", "manifestVersion": 1, "name": "Mythoria", "version": VERSION,
    "author": "Adryh01", "files": [], "overrides": "overrides"}, indent=2)})
write("Mythoria-1.21.1.mrpack", "overrides/mods/", {"modrinth.index.json": json.dumps({
    "formatVersion": 1, "game": "minecraft", "versionId": VERSION, "name": "Mythoria",
    "summary": "Fantasy creatures, weapons, armor and food by @Adryh01", "files": [],
    "dependencies": {"minecraft": "1.21.1", "forge": "52.1.20"}}, indent=2)})
write("Mythoria-1.21.1-mods.zip", "mods/", {})
print("\n".join(f"{p.name}  {p.stat().st_size // 1024} KB" for p in sorted(dist.iterdir())))
