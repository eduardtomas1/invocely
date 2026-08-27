<p align="center">
  <img src="docs/assets/invocely-icon.svg" alt="Invoicely app icon" width="108">
</p>

<h1 align="center">Invoicely</h1>

<p align="center">
  <strong>Invoices and quotes, without the fuss.</strong><br>
  A calm, private desktop app for freelancers and small businesses.
</p>

<p align="center">
  <a href="#build-from-source"><strong>Build Invoicely</strong></a>
  &nbsp;&middot;&nbsp;
  <a href="#what-it-does">What it does</a>
  &nbsp;&middot;&nbsp;
  <a href="#privacy-and-your-files">Privacy</a>
  &nbsp;&middot;&nbsp;
  <a href="#built-with-real-checks">Quality</a>
</p>

<p align="center">
  <a href="https://github.com/eduardtomas1/invocely/actions/workflows/ci.yml"><img alt="CI status" src="https://github.com/eduardtomas1/invocely/actions/workflows/ci.yml/badge.svg"></a>
  <img alt="Apache 2.0 license" src="https://img.shields.io/badge/license-Apache%202.0-536DFE?style=flat-square">
  <img alt="Works offline" src="https://img.shields.io/badge/works-offline-111827?style=flat-square">
  <img alt="English, Spanish and Catalan" src="https://img.shields.io/badge/languages-EN%20%C2%B7%20ES%20%C2%B7%20CA-536DFE?style=flat-square">
</p>

![An invoice being prepared in Invoicely](docs/screenshots/invoice.png)

## Paperwork should feel lighter

Invoicely keeps the everyday job simple: enter who is billing whom, describe the work, check the totals, and export a polished document. There is no account to create, no subscription, and no cloud dashboard to learn.

It is deliberately smaller than an accounting suite. Invoicely makes invoices and quotes; it does not try to run the rest of your business.

## What it does

- Creates invoices and quotes in one focused desktop app.
- Calculates quantities, prices, line discounts, tax, and totals.
- Exports finished documents as PDF, Excel, or CSV.
- Saves editable drafts as XML and opens them again later.
- Remembers company defaults and reusable customers or suppliers.
- Places your own PNG or JPEG logo on exported documents.
- Works in English, Spanish, and Catalan.

### Invoice clearly

Add the business and customer, enter each line, and let Invoicely calculate the subtotal, discounts, tax, and final amount.

### Quote confidently

Set an explicit validity date, payment terms, notes, and optional tax before sharing the proposal.

![A quote with validity, payment terms, and notes](docs/screenshots/quote.png)

## Download for macOS

The native build supports Apple-silicon Macs running macOS 11 or newer.

The hardened build will be published as **v1.1.0** after this work is reviewed. Until that release appears, build from source below; the older v1.0.0 download does not include these fixes.

For v1.1.0 and later:

1. Open the [latest release](https://github.com/eduardtomas1/invocely/releases/latest) and confirm it is v1.1.0 or newer.
2. Download the Apple-silicon DMG.
3. Download `SHA256SUMS` and verify the DMG before opening it:

   ```bash
   grep 'Invoicely-.*-macos-apple-silicon.dmg' SHA256SUMS | shasum -a 256 --check
   ```

4. Open the DMG and drag **Invoicely** to **Applications**.

The v1.1.x release workflow requires a Developer ID Application signature and successful Apple notarization; it will not create a draft release from an ad-hoc-signed fallback. The older v1.0.0 DMG is not covered by that guarantee. Do not bypass a macOS warning or use a download obtained anywhere other than this repository's Releases page.

The release also includes a Java 11-compatible JAR, a CycloneDX SBOM, SHA-256 checksums, and GitHub build-provenance and SBOM attestations for the JAR and DMG. Its core behavior is tested on Linux with Java 11 and the current LTS JDK; Windows packaging and the Windows desktop experience are not yet verified.

```bash
java -jar invocely-1.1.0-standalone.jar
```

> Invoicely helps create business documents. It does not replace professional accounting or tax advice, and legal requirements vary by country.

## Privacy and your files

Invoicely works offline and contains no accounts, analytics, telemetry, or remote database.

That does not mean the files are encrypted. Business partners and saved defaults are plaintext under `~/.invocely`. Draft XML files and PDF, Excel, or CSV exports are stored in the folders you choose. The app also uses Java's operating-system preferences store for language, last-used folders, and the selected logo path.

Protect the computer account and enable full-disk encryption when the documents are sensitive.

### Back up or reset

- Back up `~/.invocely` plus any folders where you keep drafts and exports.
- To reset local partners and defaults without immediately deleting them, close Invoicely and rename `~/.invocely` to `~/.invocely.backup`.
- Removing the app does not remove documents or local data automatically.

## Built with real checks

The test suite focuses on promises that matter to someone sending a document:

| Promise | What the automated check proves |
|---|---|
| Totals are trustworthy | Quantities, fractional-cent line rounding, discounts, tax, and final totals reconcile with the values shown. |
| Exports actually work | Real PDF, XLSX, and CSV files are generated and inspected. |
| Quotes keep their meaning | Validity, payment terms, and notes render in English, Spanish, and Catalan. |
| Long invoices stay readable | Totals appear once, on the final page of a multi-page invoice. |
| Imports fail safely | Malformed, oversized, and external-entity XML files are rejected instead of being silently accepted. |
| Local data survives failures | Interrupted writes do not replace the last good file, and corrupt address books are not overwritten. |
| Spreadsheet exports are safer | Formula-like text is neutralized in CSV output. |
| macOS builds stay usable | CI mounts the DMG, checks its bundle and signature, then generates PDF, XLSX, and CSV files through the bundled runtime. |

CI runs those checks on the Java 11 baseline and a current LTS JDK. It also reviews dependency changes, while Dependabot proposes weekly version updates and CodeQL runs extended source-security queries.

## Build from source

You need Java 11 or newer. The included Maven wrapper downloads the pinned Maven version and verifies its checksum.

On a Mac where Java came from Homebrew, make it available in the current terminal first:

```bash
brew install openjdk
export PATH="$(brew --prefix openjdk)/bin:$PATH"
```

```bash
./mvnw -B -ntp verify
./mvnw exec:java
# Or run the packaged artifact:
java -jar target/invocely-1.1.0-standalone.jar
```

To create a self-contained macOS installer, use a JDK that includes `jpackage` (JDK 17 or newer):

```bash
./scripts/package-macos.sh
```

This local command does not use release credentials, so its DMG is not Developer ID signed or Apple-notarized. Maintainers should follow [the trusted release runbook](docs/RELEASING.md) for releases.

See [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request. Please report sensitive issues according to [SECURITY.md](SECURITY.md).

## License

Invoicely is free and open source under the [Apache License 2.0](LICENSE). Distributed builds also contain third-party components under their own terms; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
