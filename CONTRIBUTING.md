# Contributing

Thanks for improving PolarUtilities.

## Development

```bash
./gradlew clean build
```

Keep changes focused. Gameplay features should live under
`src/main/java/br/com/polarutilities/feature/<feature-name>` and implement
`PluginFeature`.

## Pull Requests

Before opening a pull request:

- Run `./gradlew clean build`.
- Update documentation when commands, permissions or config values change.
- Update `CHANGELOG.md` and `VERSION` when the change is intended for release.
- Keep unrelated formatting changes out of the PR.

## Release Ownership

Only maintainers should push version tags. Tags trigger the release workflow and
publish a new JAR.
