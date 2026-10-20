#!/usr/bin/env python3
"""Build a reproducible Bedrock .mcpack without any local editor."""
import json
import pathlib
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "BedrockPack"
DEST = ROOT / "build" / "SmartCopperGolems_Bedrock_0.9.8.mcpack"

def main():
    files = [p for p in SOURCE.rglob("*") if p.is_file() and p.suffix == ".json"]
    for path in files:
        json.loads(path.read_text(encoding="utf-8"))
    assert any(p.name == "manifest.json" for p in files)
    DEST.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(DEST, "w", zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(files):
            archive.write(path, path.relative_to(SOURCE).as_posix())
    with zipfile.ZipFile(DEST) as archive:
        assert archive.testzip() is None
        assert {"manifest.json", "entity/copper_golem.entity.json",
                "animations/smart_copper_golem.animation.json"} <= set(archive.namelist())
    print("Bedrock pack:", DEST, "with", len(files), "JSON files")

if __name__ == "__main__":
    main()
