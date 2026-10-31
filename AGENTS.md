# LINEA Sovereign Agent Rules

## Mandatory Changelog Maintenance Rule
- **Always Update `CHANGELOG.md`**: Every single time changes, improvements, bug fixes, refactorings, or new features are introduced to the codebase, the agent MUST immediately update `CHANGELOG.md` under the `## [Unreleased]` section.
- **Keep a Changelog Standard**: Group changes under appropriate headings:
  - `### Added` for new features or capabilities.
  - `### Changed` for changes in existing functionality.
  - `### Fixed` for any bug fixes.
  - `### Removed` for now removed features.
- **Never Postpone**: Do not defer updating `CHANGELOG.md` to a later prompt or turn. Keep the changelog continuously up-to-date with every code modification.
- **Centralized Source of Truth**: `CHANGELOG.md` is the sole and authoritative historical log for the project. No separate release note files (`RELEASE_NOTES_v*.md`) should be created.

## Git & Remote Push Policy
- **Never Push to Remote without Explicit User Permission**: The agent must NEVER run `git push`, publish releases, or push tags/branches to GitHub or any remote repository without first asking the user for explicit confirmation and receiving clear approval. Keep all commits strictly local until explicitly instructed to push.

## Sovereign Privacy Constraints
- **Zero Cloud**: Never introduce network calls, cloud tracking, external telemetry, or un-consented disk logging.
- **Vault Contact Isolation**: Private vault contacts (`isPrivate = 1`) must remain strictly isolated. They must never be merged, suggested in duplicate scans, or exported in plain unencrypted formats.

