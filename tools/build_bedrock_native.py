#!/usr/bin/env python3
"""Validate and package a clean native Bedrock behavior+resource add-on."""
from pathlib import Path
import json
import zipfile

root = Path(__file__).resolve().parents[1]
base = root / "BedrockNative"
out = root / "build" / "SmartCopperGolems_Native_0.0.1_Test.mcaddon"
behavior = json.loads((base / "behavior_pack/manifest.json").read_text())
resource = json.loads((base / "resource_pack/manifest.json").read_text())
assert behavior["header"]["uuid"] != resource["header"]["uuid"]
assert any(d.get("uuid") == resource["header"]["uuid"] for d in behavior["dependencies"])
assert any(d.get("module_name") == "@minecraft/server" for d in behavior["dependencies"])
assert any(m["type"] == "script" and m["entry"] == "scripts/main.js" for m in behavior["modules"])
assert "minecraft:copper_golem" in (base / "behavior_pack/scripts/main.js").read_text()
for file in base.rglob("*.json"):
    json.loads(file.read_text(encoding="utf-8"))
out.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(out, "w", compression=zipfile.ZIP_DEFLATED) as z:
    for part in ("behavior_pack", "resource_pack"):
        for f in sorted((base / part).rglob("*")):
            if f.is_file():
                z.write(f, (Path(part) / f.relative_to(base / part)).as_posix())
with zipfile.ZipFile(out) as z:
    assert z.testzip() is None
    assert "behavior_pack/manifest.json" in z.namelist()
    assert "resource_pack/manifest.json" in z.namelist()
print(f"Created {out} ({out.stat().st_size} bytes)")
