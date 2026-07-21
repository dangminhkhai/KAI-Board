# Phrase packs (full merge)

`vi_social.tsv` — offline bigrams for next-word / mid-word.

## What’s inside (full)

| # | Source | Role |
|---|--------|------|
| 1 | **OPUS OpenSubtitles VI** | Co-occurrence bigrams (frequency ≥ 2) |
| 2 | **collocations_vi.txt** | Curated (`tiêu đề`, `hoàng hôn`, …) |
| 3 | **Viet74K** multi-token | Compound words → bigrams (~99% coverage) |
| 4 | **FrequencyWords** | **Not** in this TSV — unigram word dictionary via `WordDictionaryPack` |

Merge order: collocations → Viet74K → OpenSubtitles (fill to cap).

## Ranking (IME)

1. Personal (`PhraseLearningStore`, decay 21d)  
2. This pack  

No APK seed catalog.

## Rebuild

```powershell
# raw (gitignored):
#   phrase-packs/raw/vi_opensub.txt.gz
#   phrase-packs/raw/Viet74K.txt

py -3 tools\build_phrase_pack.py
```

Update `PhrasePack.EXPECTED_SHA256` to the printed hash.

Size target: **5–20 MB download is OK**. Typing must not parse the file — only RAM lookup after `warmUpAsync`. Cap: 1.2M pairs / 24 MB.
