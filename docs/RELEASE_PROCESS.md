# Release Process

Use this checklist every time a public version is published.

## 1. Choose Version Impact

| Change type | Version bump | Example |
| --- | --- | --- |
| Bug fix or polish | Patch | `0.4.1` -> `0.4.2` |
| New feature | Minor | `0.4.1` -> `0.5.0` |
| Breaking or broad change | Major | `0.4.1` -> `1.0.0` |

Use the highest impact level when a release contains multiple change types.

## 2. Update Release Files

Update these files before committing:

- `VERSION`
- `CHANGELOG.md`
- `release/update.json`
- Documentation affected by the change

`release/update.json` must stay valid JSON:

```json
{
  "latest": "0.4.2",
  "downloadUrl": "https://github.com/polarco/PolarUtilities/releases/latest",
  "changelogUrl": "https://github.com/polarco/PolarUtilities/blob/main/CHANGELOG.md",
  "message": "Short release summary.",
  "critical": false
}
```

Use `"critical": true` only for urgent updates.

## 3. Test Locally

```bash
./gradlew clean build
./gradlew --warning-mode all build
```

The final JAR is generated in:

```text
build/libs/
```

## 4. Commit And Push

```bash
git status
git add .
git commit -m "Release 0.4.2"
git push origin main
```

## 5. Create The Release

```bash
git tag v0.4.2
git push origin v0.4.2
```

GitHub Actions will build the plugin and create the release automatically.

## 6. Verify

Confirm:

- The `Build` workflow passed.
- The `Release` workflow passed.
- The GitHub Release exists.
- The release contains `PolarUtilities-<version>.jar`.
- The raw `release/update.json` shows the new `latest` value.
