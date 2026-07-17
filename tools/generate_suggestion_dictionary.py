"""Build KAI Board's redistributable offline suggestion asset.

Requires wordfreq 3.1.1. The generated word-frequency data is distributed under
CC BY-SA 4.0; see THIRD_PARTY_NOTICES.md.
"""

from pathlib import Path
import re

from wordfreq import top_n_list


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "app/src/main/assets/suggestions.tsv"
VALID_WORD = re.compile(r"^[^\W\d_]+(?:['’][^\W\d_]+)?$", re.UNICODE)


def main() -> None:
    seen: set[str] = set()
    rows: list[tuple[str, str]] = []
    for language, limit in (("vi", 11_000), ("en", 12_000)):
        for word in top_n_list(language, limit):
            word = word.strip()
            folded = word.casefold()
            if 1 < len(word) <= 32 and VALID_WORD.fullmatch(word) and folded not in seen:
                seen.add(folded)
                rows.append((language, word))

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(
        "# language\tword; ranked by wordfreq 3.1.1\n"
        + "".join(f"{language}\t{word}\n" for language, word in rows),
        encoding="utf-8",
        newline="\n",
    )
    print(f"Wrote {len(rows)} entries to {OUTPUT}")


if __name__ == "__main__":
    main()
