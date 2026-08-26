# Security policy

## Supported version

Security fixes are made on the latest release and the `main` branch.

## Reporting a vulnerability

Private vulnerability reporting is not enabled yet. Open a minimal issue asking the maintainer for a private contact channel. Do not publish customer data, exploit details, secrets, or a working exploit in the issue.

For ordinary bugs without security impact, use the [issue tracker](https://github.com/eduardtomas1/invocely/issues).

## Security boundaries

Invoicely is an offline document generator, not an encrypted vault. Saved business partners and defaults are plaintext files under `~/.invocely`; drafts and exports are stored wherever the user chooses. Protect the computer account and disk accordingly.
