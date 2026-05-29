#!/usr/bin/env python3
"""
yt-dlp transcript downloader for Religious Texts Platform.
Reads channel config from channels.properties and downloads transcripts
for /videos, /streams, and/or /shorts per channel.

Usage:
    python3 fetch_transcripts.py                    # process all channels
    python3 fetch_transcripts.py --channel shamsi   # process one channel by key
    python3 fetch_transcripts.py --dry-run          # show what would run
"""

import argparse
import configparser
import os
import subprocess
import sys
from pathlib import Path

SCRIPT_DIR    = Path(__file__).parent
CHANNELS_FILE = SCRIPT_DIR / "channels.properties"
TRANSCRIPTS_DIR = SCRIPT_DIR / "transcripts"
PROCESSED_FILE  = TRANSCRIPTS_DIR / "downloaded.txt"  # yt-dlp video ID archive

# Content types to fetch per channel — can be overridden per channel in properties
DEFAULT_TYPES = ["videos", "streams"]

YTDLP_BASE = [
    "yt-dlp",
    "--write-auto-sub",
    "--skip-download",
    "--sub-lang", "en",
    "--sleep-requests", "2",
    "--sleep-subtitles", "2",
    "--ignore-errors",
    "--no-warnings",
    "--download-archive", str(TRANSCRIPTS_DIR / "downloaded.txt"),
]


def load_channels():
    """
    Parse channels.properties.
    Format:
        [channels]
        shamsi = https://www.youtube.com/@DUSDawah
        shamsi.types = videos,streams
        godlogic = https://www.youtube.com/@GodLogicApologetics
        # godlogic.types defaults to videos,streams
    """
    config = configparser.ConfigParser()
    config.read(CHANNELS_FILE)

    if "channels" not in config:
        print(f"ERROR: No [channels] section in {CHANNELS_FILE}")
        sys.exit(1)

    channels = {}
    for key, value in config["channels"].items():
        if "." in key:
            continue  # skip sub-keys like shamsi.types — handled below
        base_url = value.strip().rstrip("/")
        types_key = f"{key}.types"
        if types_key in config["channels"]:
            types = [t.strip() for t in config["channels"][types_key].split(",")]
        else:
            types = DEFAULT_TYPES
        channels[key] = {"url": base_url, "types": types}

    return channels


def fetch(channel_key, base_url, content_type, dry_run=False):
    """Run yt-dlp for a channel + content type combination."""
    url = f"{base_url}/{content_type}"

    output_template = str(
        TRANSCRIPTS_DIR / "%(channel)s" / content_type /
        "%(upload_date)s_%(title)s.%(ext)s"
    )

    cmd = YTDLP_BASE + ["--output", output_template, url]

    print(f"  ▶  {url}")
    if dry_run:
        print(f"     DRY RUN: {' '.join(cmd)}")
        return

    result = subprocess.run(cmd)
    if result.returncode == 0:
        print(f"  ✅  Done: {url}")
    else:
        print(f"  ❌  Failed (exit {result.returncode}): {url}")


def main():
    parser = argparse.ArgumentParser(description="Fetch YouTube transcripts for Religious Texts Platform")
    parser.add_argument("--channel", help="Process only this channel key")
    parser.add_argument("--dry-run", action="store_true", help="Show commands without running")
    parser.add_argument("--types", help="Override content types (comma-separated: videos,streams,shorts)")
    args = parser.parse_args()

    channels = load_channels()

    if args.channel:
        if args.channel not in channels:
            print(f"ERROR: Channel '{args.channel}' not found in {CHANNELS_FILE}")
            print(f"Available: {', '.join(channels.keys())}")
            sys.exit(1)
        channels = {args.channel: channels[args.channel]}

    if args.types:
        override_types = [t.strip() for t in args.types.split(",")]
        for ch in channels.values():
            ch["types"] = override_types

    print(f"Processing {len(channels)} channel(s)...\n")

    for key, cfg in channels.items():
        print(f"[{key}] {cfg['url']} → {cfg['types']}")
        for content_type in cfg["types"]:
            fetch(key, cfg["url"], content_type, dry_run=args.dry_run)
        print()

    print("Done.")


if __name__ == "__main__":
    main()
