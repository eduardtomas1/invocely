# Security policy

## Supported versions

Security fixes are developed on `main`. Only `main` and the newest release are
supported; a fix for a released build may be delivered as a new release.

A published release can predate fixes that are already on `main`. Check the
README and release notes for current download guidance.

## Reporting a vulnerability

Do not disclose vulnerability details in a public issue or pull request.

1. Open the repository's [Security page](https://github.com/eduardtomas1/invocely/security).
2. If **Report a vulnerability** is available, submit the report there.
3. If it is unavailable, open a minimal issue asking the maintainer to enable
   private vulnerability reporting. Include no vulnerability details. Once it
   is enabled, return to the Security page and submit the private report.

Include the affected version or commit, the affected component, the expected
impact, reproduction steps or a minimal proof of concept, and any known
mitigation. Remove customer or business data, credentials, signing material,
and other secrets before submitting.

The project is maintained on a best-effort basis and does not promise a
response deadline. The maintainer will use the private report to investigate
and coordinate a fix and disclosure with the reporter.

For ordinary bugs without security impact, use the
[issue tracker](https://github.com/eduardtomas1/invocely/issues).

## Security boundaries

Invoicely is an offline document generator, not an encrypted vault. Saved
business partners and defaults are plaintext files under `~/.invocely`;
drafts and exports are stored wherever the user chooses. Protect the computer
account and disk accordingly.

Download packaged builds only from this repository's Releases page and check
the release notes for the current signing and notarization status.
