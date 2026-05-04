# GitHub Setup

This repository is already configured for GitHub under:

```text
https://github.com/polarco/PolarUtilities
```

The default branch is `main`, releases are created from version tags and the
official update checker reads:

```text
https://raw.githubusercontent.com/polarco/PolarUtilities/main/release/update.json
```

## First-Time Clone

For a new machine:

```bash
git clone https://github.com/polarco/PolarUtilities.git
cd PolarUtilities
./gradlew clean build
```

## Push Existing Local Changes

```bash
git status
git add .
git commit -m "Describe the change"
git push origin main
```

Use a short commit message that describes what changed. Examples:

```text
Improve homes menu layout
Fix TPA request expiration
Update release documentation
```

## Release Tags

Release tags use this format:

```text
v0.4.3
```

Pushing a tag starts the GitHub Actions release workflow and attaches the built
JAR to the GitHub Release.
