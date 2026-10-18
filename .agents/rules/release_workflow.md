# Release & Versioning Workflow

## 1. Release Documentation Rules
- **Centralized Changelog Only**: `CHANGELOG.md` is the single source of truth for all version history and release changes.
- **No Standalone Release Note Files**: Do **NOT** create or commit individual `docs/RELEASE_NOTES_v*.md` files into the repository.
- **GitHub Release Publishing**: Pass release notes directly into `gh release create` (using `--notes` or dynamically reading the relevant section from `CHANGELOG.md`) without creating an extra file in git.
- **Legacy Cleanup**: On the next release, remove legacy `docs/RELEASE_NOTES_v*.md` files to keep the `docs/` folder lean.

## 2. Versioning Conventions
- Increment `versionCode` in `app/build.gradle.kts` for every release.
- Follow SemVer: Minor bump (`2.x.0`) for new user-facing features, Patch bump (`2.x.y`) for bug fixes.
