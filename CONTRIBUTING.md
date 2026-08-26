# Contributing to Invoicely

Small, focused pull requests are welcome.

## Before opening a pull request

1. Use Java 11 or newer.
2. Run `./mvnw -B -ntp verify`.
3. Add a regression test for behavior changes. A bug-fix test should fail before the fix and pass after it.
4. For report changes, check invoice and quote output in English, Spanish, and Catalan. Exercise every affected export format.
5. Keep Invoicely offline-first: do not add accounts, analytics, telemetry, or network calls without an explicit design discussion.
6. Never commit real invoices, customer details, credentials, signing keys, or other private data.

The CI checks the Java 11 baseline, a current LTS JDK, real report exports, dependency changes, macOS packaging, and CodeQL security queries. Formatting-only tests and arbitrary coverage targets are intentionally not substitutes for behavior checks.

## Running the app

```bash
./mvnw -B -ntp verify
./mvnw exec:java
```

On macOS with a JDK that includes `jpackage`:

```bash
./scripts/package-macos.sh
```

## Scope

Keep each pull request easy to review. Explain the user-visible result, the risk being addressed, and the exact verification performed.

