# Carbon Device Sync

Run `run-sync.ps1` to open the offline Windows companion. It communicates with one authorized Android device over USB/ADB and manages four additive libraries:

| Library | Phone directory | Formats |
| --- | --- | --- |
| Gallery | `/sdcard/CarbonSync/Gallery` | JPG, PNG, WebP, GIF, MP4, MKV |
| Notes | `/sdcard/CarbonSync/Notes` | Markdown, text |
| PDF | `/sdcard/CarbonSync/Books/PDF` | PDF |
| EPUB | `/sdcard/CarbonSync/Books/EPUB` | EPUB |

The desktop library defaults to `library/` and can be changed with `sync_library` in `config.json`. Transfers never delete files. The custom Android app forks will treat these directories as their managed import/export contract.
