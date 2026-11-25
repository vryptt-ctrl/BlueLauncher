# Security Policy

## Supported versions

| Version | Supported |
| ------- | --------- |
| 1.0.x   | yes       |

## Reporting a vulnerability

BlueLauncher is a fully offline launcher: it makes **no network requests**, collects **no analytics**, and runs **no background services**. Its attack surface is limited to local package queries, preference storage and an imported wallpaper image.

If you believe you have found a security issue, please **do not open a public issue**. Instead:

1. Use GitHub's **"Report a vulnerability"** (Security → Advisories) on this repository, or
2. Email the owner via the address on the GitHub profile.

Include steps to reproduce and, if possible, a proof of concept. You can expect an initial response within 7 days. Once a fix is released, the advisory will be published and you will be credited unless you prefer otherwise.

Please avoid testing against devices you do not own, and do not automate reports through scanners against the issue tracker.
