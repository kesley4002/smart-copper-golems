#!/usr/bin/env python3
"""Build Bedrock standalone .mcaddon with behavior+resource packs."""
import json
import pathlib
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "BedrockOffline"
OUTPUT = ROOT / "build" / "SmartCopperGolems_Offline_0.1.0_alpha.mcaddon"
PACKS = ("behavior_pack", "resource_pack")

def validate():
    ids = set()
    for pack in PACKS:
        root = SOURCE / pack
        manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8"))
        assert manifest["format_version"] == 2
        assert any(m["type"] == ("data" if pack == "behavior_pack" else "resources")
                   for m in manifest["modules"])
        ids.add(manifest["header"]["uuid"])
        for f in root.rglob("*.json"):
            data = json.loads(f.read_text(encoding="utf-8"))
            assert data, f
    assert len(ids) == 2
    b = json.loads((SOURCE / "behavior_pack" / "manifest.json").read_text(encoding="utf-8"))
    assert any(d.get("uuid") in ids for d in b["dependencies"])
    assert (SOURCE / "behavior_pack" / "scripts" / "main.js").exists()
    categories = json.loads((SOURCE / "behavior_pack" / "scripts" / "category-index.json").read_text(encoding="utf-8"))
    assert len(categories) == 1413
    assert len(set(categories.values())) == 42
    assert "scg:reach" in json.loads((SOURCE / "behavior_pack" / "entities" / "smart_copper_golem.json").read_text(encoding="utf-8"))["minecraft:entity"]["description"]["properties"]

def main():
    validate()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(OUTPUT, "w", zipfile.ZIP_DEFLATED) as archive:
        for pack in PACKS:
            root = SOURCE / pack
            for f in sorted(root.rglob("*")):
                if f.is_file():
                    archive.write(f, f"{pack}/{f.relative_to(root).as_posix()}")
    with zipfile.ZipFile(OUTPUT) as archive:
        assert archive.testzip() is None
        assert "behavior_pack/manifest.json" in archive.namelist()
        assert "resource_pack/manifest.json" in archive.namelist()
    print(f"Built {OUTPUT} ({OUTPUT.stat().st_size} bytes).")

if __name__ == "__main__":
    main()
