# Trusted releases

Invoicely's `v1.1.x` release workflow builds the Java 11-compatible standalone JAR and an Apple-silicon macOS DMG from the tagged commit. It creates a draft GitHub release; it never publishes the release automatically.

## One-time repository setup

Create a GitHub Actions environment named `trusted-release` before pushing a release tag.

1. Require a reviewer for the environment and restrict deployment tags to `v1.1.*`.
2. Add the environment variable `MACOS_SIGNING_IDENTITY` with the full `Developer ID Application: Name (TEAMID)` identity.
3. Add these environment secrets:
   - `MACOS_CERTIFICATE_BASE64`: the base64-encoded Developer ID Application certificate and private key exported as a password-protected PKCS#12 file.
   - `MACOS_CERTIFICATE_PASSWORD`: the PKCS#12 export password.
   - `APPLE_API_KEY_BASE64`: the base64-encoded App Store Connect API `.p8` private key used by Apple's notary service.
   - `APPLE_API_KEY_ID`: the App Store Connect API key ID.
   - `APPLE_API_ISSUER_ID`: the App Store Connect API issuer ID.

Never commit these values. Environment secrets are made available only to the tag-triggered release job after the environment's protection rules pass; pull-request workflows do not reference them.

The workflow deliberately has no unsigned release fallback. If the environment, any credential, the Developer ID identity, signing, hardened runtime, notarization, stapling, or Gatekeeper assessment is missing or invalid, the job fails before it creates a GitHub release.

## Prepare a v1.1.x release

1. Update `pom.xml` to the intended numeric patch version, such as `1.1.1`, through a reviewed pull request.
2. Confirm the resulting `main` commit passes CI.
3. Create and push an annotated tag with the exact corresponding name, such as `v1.1.1`.
4. Approve the `trusted-release` environment deployment after checking the tag and commit.

The workflow accepts only `v1.1.N` tags with a numeric `N`. It verifies that the tag resolves to the workflow commit, that the commit is on `main`, and that the tag version exactly matches Maven's `project.version`.

Do not move or reuse a tag after a failed attempt. Fix the problem through the normal review process and use a new patch version and tag.

## Review the draft

The draft contains:

- `invocely-VERSION-standalone.jar`
- `Invoicely-VERSION-macos-apple-silicon.dmg`
- `invocely-VERSION.cdx.json`, a CycloneDX SBOM
- `SHA256SUMS`

The workflow also records GitHub artifact provenance and SBOM attestations for the JAR and DMG. Before publishing the draft:

1. Confirm all workflow steps succeeded and the assets match the tag and architecture.
2. Verify `SHA256SUMS` against every asset.
3. Verify the DMG's Developer ID signature, notarization ticket, and Gatekeeper assessment on a separate Mac.
4. Verify provenance with `gh attestation verify PATH --repo eduardtomas1/invocely`, then verify the SBOM attestation with the same command plus `--predicate-type https://cyclonedx.org/bom`.
5. Review the generated release notes, then publish the draft manually.

The workflow refuses to replace an existing draft or published release for the same tag.

## Local packaging

`./scripts/package-macos.sh` still creates a development DMG without release credentials. That artifact is not Developer ID signed or notarized and must not be described or distributed as a trusted release. `--skip-build` is reserved for automation that has already produced the expected standalone JAR in `target/`.
