#!/usr/bin/env python3
"""
Build vi_social.tsv with ALL phrase sources (full merge):

1. collocations_vi.txt          — curated (tiêu đề, hoàng hôn, …)  [first]
2. Viet74K multi-token compounds — duyet/vietnamese-wordlist      [second]
3. OPUS OpenSubtitles VI bigrams — frequency-ranked               [fill]

FrequencyWords (vi_50k) is unigram-only → word dictionary via WordDictionaryPack,
not this phrase TSV. See THIRD_PARTY_NOTICES.md.
"""
from __future__ import annotations

import gzip
import hashlib
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / "phrase-packs" / "raw"
# User OK with 5–20 MB download packs; lag is avoided by warmUp on a worker, not by shrinking data.
OUT_CAP = 1_200_000
WORD = re.compile(r"^[\w\u00C0-\u024F\u1E00-\u1EFFĐđ]+$", re.UNICODE)
STOP_SINGLE = {"x", "xx"}


def clean_token(t: str) -> str | None:
    t = (t or "").strip().lower()
    if not t or len(t) > 32 or t in STOP_SINGLE:
        return None
    if any(c.isdigit() for c in t):
        return None
    if not WORD.match(t):
        return None
    return t


def add_pair(
    ordered: list[tuple[str, str]],
    seen: set[tuple[str, str]],
    left: str,
    right: str,
) -> bool:
    a, b = clean_token(left), clean_token(right)
    if not a or not b or a == b:
        return False
    key = (a, b)
    if key in seen:
        return False
    if len(ordered) >= OUT_CAP:
        return False
    seen.add(key)
    ordered.append(key)
    return True


def load_collocations(path: Path, ordered: list, seen: set) -> int:
    if not path.is_file():
        return 0
    before = len(ordered)
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if "\t" in line:
            a, b = line.split("\t", 1)
            parts_a = a.replace("_", " ").split()
            parts_b = b.replace("_", " ").split()
            if parts_a and parts_b:
                add_pair(ordered, seen, parts_a[0], parts_b[0])
        else:
            parts = line.replace("_", " ").split()
            if len(parts) >= 2:
                add_pair(ordered, seen, parts[0], parts[1])
    return len(ordered) - before


def load_viet74k(path: Path, ordered: list, seen: set) -> int:
    if not path.is_file():
        return 0
    before = len(ordered)
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        t = line.strip()
        if not t or t.startswith("#"):
            continue
        parts = re.split(r"[\s\-]+", t)
        parts = [p for p in parts if p]
        if len(parts) < 2:
            continue
        # Skip heavy proper-name stacks
        upperish = sum(1 for p in parts if p[:1].isupper())
        if upperish >= max(2, len(parts) - 1) and len(parts) > 2:
            continue
        for i in range(len(parts) - 1):
            if not add_pair(ordered, seen, parts[i], parts[i + 1]):
                if len(ordered) >= OUT_CAP:
                    return len(ordered) - before
    return len(ordered) - before


def count_opensub_bigrams(path: Path, max_lines: int = 5_000_000) -> Counter[tuple[str, str]]:
    counts: Counter[tuple[str, str]] = Counter()
    opener = gzip.open if path.suffix == ".gz" else open
    n = 0
    with opener(path, "rt", encoding="utf-8", errors="replace") as f:
        for line in f:
            n += 1
            if n > max_lines:
                break
            toks = [clean_token(t) for t in line.split()]
            toks = [t for t in toks if t]
            for i in range(len(toks) - 1):
                a, b = toks[i], toks[i + 1]
                if a != b:
                    counts[(a, b)] += 1
            if n % 250_000 == 0:
                print(f"  opensub lines {n:,} unique {len(counts):,}")
    print(f"  opensub done lines={min(n, max_lines):,} unique={len(counts):,}")
    return counts


def merge_opensub(counts: Counter[tuple[str, str]], ordered: list, seen: set, min_count: int = 2) -> int:
    before = len(ordered)
    for (a, b), c in counts.most_common():
        if c < min_count:
            break
        if not add_pair(ordered, seen, a, b):
            if len(ordered) >= OUT_CAP:
                break
    return len(ordered) - before


def main() -> None:
    seen: set[tuple[str, str]] = set()
    ordered: list[tuple[str, str]] = []

    # --- FULL merge order: curated → wordlist compounds → corpus co-occurrence ---
    c1 = load_collocations(ROOT / "phrase-packs" / "collocations_vi.txt", ordered, seen)
    print("1 collocations", c1, "total", len(ordered))

    c2 = load_viet74k(RAW / "Viet74K.txt", ordered, seen)
    print("3 Viet74K compounds", c2, "total", len(ordered))

    opensub = RAW / "vi_opensub.txt.gz"
    if not opensub.is_file():
        opensub = RAW / "vi.txt.gz"
    if opensub.is_file():
        print("1 counting OpenSubtitles…")
        counts = count_opensub_bigrams(opensub)
        c3 = merge_opensub(counts, ordered, seen, min_count=2)
        print("1 OpenSubtitles merged", c3, "total", len(ordered))
    else:
        print("WARN: missing OpenSubtitles at", opensub)

    selected = ordered[:OUT_CAP]
    data = "".join(f"{a}\t{b}\n" for a, b in selected).encode("utf-8")
    digest = hashlib.sha256(data).hexdigest()
    print("written", len(selected), "bytes", len(data), "kb", len(data) // 1024)
    print("EXPECTED_SHA256=", digest)

    need = {
        ("tiêu", "đề"),
        ("hoàng", "hôn"),
        ("xin", "chào"),
        ("cảm", "ơn"),
        ("bình", "minh"),
        ("sân", "bay"),
    }
    print("key pairs:", {p: p in set(selected) for p in need})

    # Coverage report
    if (RAW / "Viet74K.txt").is_file():
        vset: set[tuple[str, str]] = set()
        for line in (RAW / "Viet74K.txt").read_text(encoding="utf-8", errors="replace").splitlines():
            parts = re.split(r"[\s\-]+", line.strip())
            parts = [p for p in parts if p]
            if len(parts) < 2:
                continue
            for i in range(len(parts) - 1):
                a, b = clean_token(parts[i]), clean_token(parts[i + 1])
                if a and b and a != b:
                    vset.add((a, b))
        sel = set(selected)
        hit = sum(1 for p in vset if p in sel)
        print(f"Viet74K coverage in pack: {hit}/{len(vset)} ({100 * hit / max(1, len(vset)):.1f}%)")

    for folder in (ROOT / "phrase-packs", ROOT / "app/src/main/assets/phrase_packs"):
        folder.mkdir(parents=True, exist_ok=True)
        (folder / "vi_social.tsv").write_bytes(data)


if __name__ == "__main__":
    main()
