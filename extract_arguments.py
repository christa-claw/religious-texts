#!/usr/bin/env python3
"""
Extract verse-specific arguments from YouTube transcripts.
Outputs structured JSON ready to import as pre-populated comments.

Usage:
    pip3 install webvtt-py anthropic
    export ANTHROPIC_API_KEY=your_key_here
    python3 extract_arguments.py

Output: transcripts/arguments.json
Resumable: already-processed files are skipped on re-run.
"""

import os
import re
import json
import glob
import time
import anthropic

TRANSCRIPTS_DIR = os.path.join(os.path.dirname(__file__), "transcripts")
OUTPUT_FILE     = os.path.join(TRANSCRIPTS_DIR, "arguments.json")

# Map channel folder names to tradition
CHANNEL_TRADITION = {
    "Dr Zakir Naik":              "Islamic",
    "Ali Dawah":                  "Islamic",
    "DUS Dawah":                  "Islamic",
    "Mohammed Hijab":             "Islamic",
    "Apologetics Roadshow":       "Christian",
    "GodLogic Apologetics":       "Christian",
    "Hatun Tash DCCI Ministries": "Christian",
    "Shamounian Explains":        "Christian",
    "JihadWatchVideo":            "Critical",
    "Raymond Ibrahim":            "Critical",
}

# Bible book name -> API.Bible code
BOOK_CODES = {
    "genesis":"GEN","exodus":"EXO","leviticus":"LEV","numbers":"NUM",
    "deuteronomy":"DEU","joshua":"JOS","judges":"JDG","ruth":"RUT",
    "1samuel":"1SA","2samuel":"2SA","1kings":"1KI","2kings":"2KI",
    "1chronicles":"1CH","2chronicles":"2CH","ezra":"EZR","nehemiah":"NEH",
    "esther":"EST","job":"JOB","psalms":"PSA","psalm":"PSA","proverbs":"PRO",
    "ecclesiastes":"ECC","songofsolomon":"SNG","isaiah":"ISA","jeremiah":"JER",
    "lamentations":"LAM","ezekiel":"EZK","daniel":"DAN","hosea":"HOS",
    "joel":"JOL","amos":"AMO","obadiah":"OBA","jonah":"JON","micah":"MIC",
    "nahum":"NAM","habakkuk":"HAB","zephaniah":"ZEP","haggai":"HAG",
    "zechariah":"ZEC","malachi":"MAL","matthew":"MAT","mark":"MRK",
    "luke":"LUK","john":"JHN","acts":"ACT","romans":"ROM",
    "1corinthians":"1CO","2corinthians":"2CO","galatians":"GAL",
    "ephesians":"EPH","philippians":"PHP","colossians":"COL",
    "1thessalonians":"1TH","2thessalonians":"2TH","1timothy":"1TI",
    "2timothy":"2TI","titus":"TIT","philemon":"PHM","hebrews":"HEB",
    "james":"JAS","1peter":"1PE","2peter":"2PE","1john":"1JN",
    "2john":"2JN","3john":"3JN","jude":"JUD","revelation":"REV",
}

QURAN_PATTERN = re.compile(
    r'(?:surah?|quran|chapter)\s*(\d+)[:\.\s]+(\d+)', re.IGNORECASE)

BIBLE_PATTERN = re.compile(
    r'((?:\d\s*)?[A-Za-z]+)\s*(\d+)[:\._]+(\d+)', re.IGNORECASE)

SKIP_KEYWORDS = [
    'reaction', 'reacts', 'news', 'breaking', 'piers morgan',
    'andrew tate', 'elon musk', 'candace', 'trending', 'viral',
    'podcast', 'compilation', 'highlights', 'live stream', 'livestream',
    'vlog', 'daily', 'weekly', 'update', 'announcement',
]


def vtt_to_text(vtt_path):
    """Convert VTT file to clean plain text, deduplicating rolling captions."""
    try:
        import webvtt
        seen = set()
        parts = []
        for caption in webvtt.read(vtt_path):
            text = re.sub(r'<[^>]+>', '', caption.text).strip()
            if text and text not in seen:
                seen.add(text)
                parts.append(text)
        return ' '.join(parts)
    except Exception as e:
        print(f"  Warning: could not parse {vtt_path}: {e}")
        return None


def extract_verse_from_filename(filename):
    """
    Try to extract a verse reference from the video filename.
    e.g. 'Does Jesus say to Slay Unbelievers in Luke 19_27'
         -> {'type': 'bible', 'code': 'LUK', 'chapter': 19, 'verse': 27}
    """
    name = os.path.basename(filename).replace('.en.vtt', '').replace('_', ' ')

    # Try Bible pattern
    for m in BIBLE_PATTERN.finditer(name):
        book_raw = m.group(1).lower().replace(' ', '')
        chapter  = int(m.group(2))
        verse    = int(m.group(3))
        code     = BOOK_CODES.get(book_raw)
        if code:
            return {'type': 'bible', 'code': code, 'chapter': chapter, 'verse': verse}

    # Try Quran pattern
    for m in QURAN_PATTERN.finditer(name):
        return {'type': 'quran', 'surah': int(m.group(1)), 'ayah': int(m.group(2))}

    return None


def summarise_argument(client, text, channel, tradition, verse_ref):
    """
    Use Claude to extract the core argument being made about the verse.
    Returns a structured dict or None.
    """
    verse_hint = ""
    if verse_ref:
        if verse_ref['type'] == 'bible':
            verse_hint = (f"The video title references "
                         f"{verse_ref['code']} {verse_ref['chapter']}:{verse_ref['verse']}. ")
        elif verse_ref['type'] == 'quran':
            verse_hint = (f"The video title references "
                         f"Quran {verse_ref['surah']}:{verse_ref['ayah']}. ")

    prompt = f"""You are analysing a transcript from the YouTube channel "{channel}" ({tradition} perspective).
{verse_hint}
Extract structured argument data for a religious texts study platform.

TRANSCRIPT (first 3000 chars):
{text[:3000]}

Return ONLY a JSON object with these fields:
{{
  "verse_refs": [
    {{
      "type": "bible" or "quran",
      "code": "API.Bible code e.g. LUK, MAT, GEN (bible only)",
      "chapter": integer,
      "verse": integer,
      "surah": integer (quran only, omit for bible),
      "ayah": integer (quran only, omit for bible)
    }}
  ],
  "argument_summary": "2-3 sentence neutral summary of the argument being made",
  "tradition": "{tradition}",
  "channel": "{channel}",
  "argument_type": "contextual|translation|prophecy|theological|comparative|historical",
  "useful": true or false
}}

Set useful=false if this is a reaction video, news commentary, or not substantively about a specific verse argument."""

    try:
        response = client.messages.create(
            model="claude-sonnet-4-20250514",
            max_tokens=600,
            messages=[{"role": "user", "content": prompt}]
        )
        raw = response.content[0].text.strip()
        raw = re.sub(r'^```[a-z]*\n?', '', raw)
        raw = re.sub(r'\n?```$', '', raw)
        return json.loads(raw)
    except Exception as e:
        print(f"  API error: {e}")
        return None


def main():
    client = anthropic.Anthropic()

    results = []
    existing_files = set()

    # Resume from existing output
    if os.path.exists(OUTPUT_FILE):
        with open(OUTPUT_FILE) as f:
            results = json.load(f)
        existing_files = {r['source_file'] for r in results}
        print(f"Resuming: {len(existing_files)} already processed")

    processed = skipped = errors = 0

    for channel in sorted(os.listdir(TRANSCRIPTS_DIR)):
        channel_dir = os.path.join(TRANSCRIPTS_DIR, channel)
        if not os.path.isdir(channel_dir) or channel.startswith('.'):
            continue

        tradition = CHANNEL_TRADITION.get(channel, "Unknown")
        vtt_files = sorted(glob.glob(os.path.join(channel_dir, "*.vtt")))
        print(f"\n{channel} ({tradition}) — {len(vtt_files)} files")

        for vtt_file in vtt_files:
            rel_path = os.path.relpath(vtt_file, TRANSCRIPTS_DIR)

            if rel_path in existing_files:
                skipped += 1
                continue

            # Quick filter on filename
            if any(kw in os.path.basename(vtt_file).lower() for kw in SKIP_KEYWORDS):
                results.append({'source_file': rel_path, 'useful': False})
                existing_files.add(rel_path)
                skipped += 1
                continue

            verse_ref = extract_verse_from_filename(vtt_file)
            text      = vtt_to_text(vtt_file)

            if not text or len(text) < 200:
                skipped += 1
                continue

            print(f"  {os.path.basename(vtt_file)[:70]}...")
            result = summarise_argument(client, text, channel, tradition, verse_ref)

            if result:
                result['source_file']         = rel_path
                result['filename_verse_ref']  = verse_ref
                results.append(result)
                existing_files.add(rel_path)
                if result.get('useful'):
                    processed += 1
                    print(f"    ✅ [{result.get('argument_type','?')}] "
                          f"{result.get('argument_summary','')[:80]}")
                else:
                    skipped += 1
            else:
                errors += 1

            # Save incrementally every 10 files
            if (processed + skipped + errors) % 10 == 0:
                with open(OUTPUT_FILE, 'w') as f:
                    json.dump(results, f, indent=2, ensure_ascii=False)

            time.sleep(0.5)

    with open(OUTPUT_FILE, 'w') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)

    useful = [r for r in results if r.get('useful')]
    print(f"\nDone. Useful: {len(useful)}, Skipped: {skipped}, Errors: {errors}")
    print(f"Output: {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
