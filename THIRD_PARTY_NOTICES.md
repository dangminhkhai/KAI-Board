# Third-party data notices

## Universal Dependencies Vietnamese VTB

The optional phrase-suggestion package downloads [UD Vietnamese VTB](https://github.com/UniversalDependencies/UD_Vietnamese-VTB) at commit `3c971fd1c5d03561e00b267d3d0057b81a9bea84` and derives a compact bigram/trigram table locally on the device.

The data is licensed under [Creative Commons Attribution-ShareAlike 4.0](https://creativecommons.org/licenses/by-sa/4.0/). The corpus is not bundled in the APK; attribution is also shown beside the download control.

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
