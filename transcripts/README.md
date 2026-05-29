# Transcript Pipeline

This directory contains YouTube transcripts used to pre-populate
verse-specific arguments and commentary in the Religious Texts Platform.

---

## How Transcripts Are Fetched

Transcripts are downloaded using [yt-dlp](https://github.com/yt-dlp/yt-dlp).

### Install

```bash
brew install yt-dlp
pip3 install webvtt-py anthropic
```

### Download transcripts for a channel

```bash
yt-dlp --write-auto-sub --skip-download --sub-lang en \
  --output "transcripts/%(channel)s/%(upload_date)s_%(title)s" \
  "https://www.youtube.com/@ChannelHandle/videos"
```

### Download all configured channels at once

```bash
cd /Volumes/VMs/data/Claude/religious-texts

yt-dlp --write-auto-sub --skip-download --sub-lang en \
  --output "transcripts/%(channel)s/%(upload_date)s_%(title)s" \
  "https://www.youtube.com/@Drzakirchannel/videos" \
  "https://www.youtube.com/@AliDawah/videos" \
  "https://www.youtube.com/@DUSDawah/videos" \
  "https://www.youtube.com/@MohammedHijab/videos" \
  "https://www.youtube.com/@apologeticsroadshow/videos" \
  "https://www.youtube.com/@GodLogicApologetics/videos" \
  "https://www.youtube.com/@HatunTashDCCIMinistries/videos" \
  "https://www.youtube.com/@ShamounianExplains/videos" \
  "https://www.youtube.com/@JihadWatchVideo/videos" \
  "https://www.youtube.com/@RaymondIbrahim-HW/videos"
```

### Add a new channel

1. Find the channel handle on YouTube
2. Add it to the bulk download command above
3. Add it to `CHANNEL_TRADITION` in `extract_arguments.py`
4. Run the download command for that channel only
5. Run `extract_arguments.py` — it resumes from where it left off

---

## How Arguments Are Extracted

```bash
export ANTHROPIC_API_KEY=your_key_here
python3 extract_arguments.py
```

The script (`extract_arguments.py` in project root):
- Iterates all channel folders in `transcripts/`
- Skips obvious non-argument videos (reactions, news, live streams)
- Extracts verse references from filenames where possible
- Converts each `.vtt` to clean plain text (deduplicates rolling captions)
- Sends to Claude API to extract: verse refs, argument summary, argument type
- Saves incrementally to `transcripts/arguments.json`
- **Resumable** — already-processed files are skipped on re-run

Output schema per entry in `arguments.json`:
```json
{
  "source_file": "Shamounian Explains/20240902_Does Jesus say...",
  "channel": "Shamounian Explains",
  "tradition": "Christian",
  "argument_type": "contextual",
  "argument_summary": "Shamoun argues Luke 19:27 is part of a parable...",
  "verse_refs": [
    { "type": "bible", "code": "LUK", "chapter": 19, "verse": 27 }
  ],
  "useful": true
}
```

`argument_type` values:
- `contextual` — argues the verse is misunderstood without context
- `translation` — argument depends on a specific translation choice
- `prophecy` — OT prophecy / NT fulfilment argument
- `theological` — doctrinal argument (Trinity, prophethood, atonement etc.)
- `comparative` — cross-tradition comparison (Bible vs Quran)
- `historical` — historical/manuscript argument

---

## Channels

| Channel | Tradition | Handle |
|---|---|---|
| Dr Zakir Naik | Islamic | @Drzakirchannel |
| Ali Dawah | Islamic | @AliDawah |
| DUS Dawah | Islamic | @DUSDawah |
| Mohammed Hijab | Islamic | @MohammedHijab |
| Apologetics Roadshow | Christian | @apologeticsroadshow |
| GodLogic Apologetics | Christian | @GodLogicApologetics |
| Hatun Tash DCCI Ministries | Christian | @HatunTashDCCIMinistries |
| Shamounian Explains | Christian | @ShamounianExplains |
| JihadWatch | Critical | @JihadWatchVideo |
| Raymond Ibrahim | Critical | @RaymondIbrahim-HW |

---

## Notes

- Transcripts are **not committed to git** (see `.gitignore`)
- They are reference material only — the extracted `arguments.json` is what matters
- `arguments.json` **should** be committed once generated
- Re-running `extract_arguments.py` after adding a new channel only processes new files
- API.Bible rate limit does not apply here — this uses the Anthropic API directly
- Estimated cost: ~$0.002 per video at Claude Sonnet pricing
