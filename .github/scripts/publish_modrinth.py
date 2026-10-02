#!/usr/bin/env python3
"""Publish the built Discord2FA jars to Modrinth: one version per platform jar.

Usage: publish_modrinth.py --version 2.2.0 [--changelog-file notes.md] [--version-type release] [--dry-run]

Reads MODRINTH_TOKEN from the environment. Supported game versions are every Modrinth *release*
game version from MIN_GAME_VERSION up to the newest one at publish time.
"""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

API = "https://api.modrinth.com/v2"
PROJECT_ID = "twlCs2Yd"  # modrinth.com/plugin/discord2fa
MIN_GAME_VERSION = "1.16"
USER_AGENT = "sieadev/Discord2FA release workflow (github.com/sieadev/Discord2FA)"

# jar module -> Modrinth loaders
PLATFORMS = {
    "spigot": ["bukkit", "paper", "purpur", "spigot"],
    "velocity": ["velocity"],
    "bungeecord": ["bungeecord"],
}


def request(method, path, token=None, body=None, content_type=None):
    req = urllib.request.Request(API + path, data=body, method=method)
    req.add_header("User-Agent", USER_AGENT)
    if token:
        req.add_header("Authorization", token)
    if content_type:
        req.add_header("Content-Type", content_type)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.loads(resp.read() or b"null")
    except urllib.error.HTTPError as e:
        sys.exit(f"Modrinth {method} {path} failed: HTTP {e.code}: {e.read().decode(errors='replace')}")


def version_key(v):
    return tuple(int(p) for p in v.split("."))


def supported_game_versions():
    tags = request("GET", "/tag/game_version")
    releases = [t["version"] for t in tags if t["version_type"] == "release"]
    floor = version_key(MIN_GAME_VERSION)
    return sorted((v for v in releases if version_key(v) >= floor), key=version_key)


def multipart(fields, files):
    boundary = uuid.uuid4().hex
    out = bytearray()
    for name, value in fields.items():
        out += f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n'.encode()
        out += b"Content-Type: application/json\r\n\r\n" + value.encode() + b"\r\n"
    for name, path in files.items():
        out += (f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"; filename="{path.name}"\r\n'
                "Content-Type: application/java-archive\r\n\r\n").encode()
        out += path.read_bytes() + b"\r\n"
    out += f"--{boundary}--\r\n".encode()
    return bytes(out), f"multipart/form-data; boundary={boundary}"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True, help="plugin version, e.g. 2.2.0")
    parser.add_argument("--changelog-file")
    parser.add_argument("--version-type", default="release", choices=["release", "beta", "alpha"])
    parser.add_argument("--root", default=".", help="repository root containing the module target/ folders")
    parser.add_argument("--dry-run", action="store_true", help="print what would be published")
    args = parser.parse_args()

    token = os.environ.get("MODRINTH_TOKEN")
    if not token and not args.dry_run:
        sys.exit("MODRINTH_TOKEN is not set")

    changelog = Path(args.changelog_file).read_text(encoding="utf-8").strip() if args.changelog_file else ""
    game_versions = supported_game_versions()
    print(f"Game versions ({len(game_versions)}): {game_versions[0]} .. {game_versions[-1]}")

    existing = {(v["version_number"], tuple(sorted(v["loaders"])))
                for v in request("GET", f"/project/{PROJECT_ID}/version")}

    for module, loaders in PLATFORMS.items():
        jar = Path(args.root, module, "target", f"discord2fa-{module}-{args.version}.jar")
        if not jar.is_file():
            sys.exit(f"Missing jar: {jar}")
        if (args.version, tuple(sorted(loaders))) in existing:
            print(f"{module}: {args.version} already on Modrinth, skipping")
            continue

        data = {
            "project_id": PROJECT_ID,
            "name": f"Discord2FA {args.version}",
            "version_number": args.version,
            "changelog": changelog,
            "dependencies": [],
            "game_versions": game_versions,
            "version_type": args.version_type,
            "loaders": loaders,
            "featured": False,
            "file_parts": ["jar"],
            "primary_file": "jar",
        }
        if args.dry_run:
            print(f"{module}: would upload {jar} ({jar.stat().st_size} bytes) with loaders {loaders}")
            continue
        body, content_type = multipart({"data": json.dumps(data)}, {"jar": jar})
        created = request("POST", "/version", token, body, content_type)
        print(f"{module}: published https://modrinth.com/plugin/discord2fa/version/{created['id']}")


if __name__ == "__main__":
    main()
