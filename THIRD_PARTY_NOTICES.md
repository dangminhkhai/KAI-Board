# Third-party data notices

## wordfreq 3.1.1

KAI Board's generated `suggestions.tsv` contains Vietnamese and English word
frequency rankings produced with [wordfreq](https://github.com/rspeer/wordfreq).

- wordfreq software: Copyright Robyn Speer and contributors, Apache License 2.0.
- word-frequency data: Creative Commons Attribution-ShareAlike 4.0 International
  (CC BY-SA 4.0).
- Changes: KAI Board selects the highest-ranked Vietnamese and English entries,
  removes duplicates, numbers, punctuation-only tokens, and tokens longer than
  32 characters, then stores only language and rank order.

The generated `app/src/main/assets/suggestions.tsv` is made available under the
same CC BY-SA 4.0 terms.
