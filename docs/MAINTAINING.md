# Maintaining The Repository

This is the normal workflow after changes are made locally.

## Regular Update

Use this for documentation updates, cleanup or internal changes that do not need
a public plugin release.

```bash
git status
git add .
git commit -m "Describe the change"
git push origin main
```

## Public Plugin Update

Use this when server owners should receive the update through the update checker.

1. Update `VERSION`.
2. Add an entry to `CHANGELOG.md`.
3. Update `release/update.json`.
4. Build locally:

```bash
./gradlew clean build
./gradlew --warning-mode all build
```

5. Commit and push:

```bash
git add .
git commit -m "Release 0.4.2"
git push origin main
```

6. Tag the release:

```bash
git tag v0.4.2
git push origin v0.4.2
```

After the tag is pushed, GitHub Actions publishes the JAR automatically.

## If Something Goes Wrong

Check:

- `git status`
- GitHub `Actions` tab
- GitHub `Releases` tab
- `release/update.json` raw URL

Raw update metadata:

```text
https://raw.githubusercontent.com/polarco/PolarUtilities/main/release/update.json
```
