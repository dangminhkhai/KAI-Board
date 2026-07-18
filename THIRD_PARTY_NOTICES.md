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

## FrequencyWords / OpenSubtitles

The optional extended dictionary downloads Vietnamese and English frequency
lists from [hermitdave/FrequencyWords](https://github.com/hermitdave/FrequencyWords)
at commit `525f9b560de45753a5ea01069454e72e9aa541c6`. FrequencyWords derives these
lists from OpenSubtitles. The content is licensed under CC BY-SA 4.0. KAI Board
selects up to 40,000 valid Vietnamese entries and 15,000 valid English entries;
the data is not bundled in the APK.
