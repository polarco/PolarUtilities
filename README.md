# PolarUtilities

[![Build](https://github.com/polarco/PolarUtilities/actions/workflows/build.yml/badge.svg)](https://github.com/polarco/PolarUtilities/actions/workflows/build.yml)
[![Latest release](https://img.shields.io/github/v/release/polarco/PolarUtilities?label=release)](https://github.com/polarco/PolarUtilities/releases/latest)
[![Paper](https://img.shields.io/badge/Paper-26.1.2-1f8acb)](https://papermc.io/)

PolarUtilities is a modular utilities plugin for Paper 26.1.2. It provides
teleport requests, homes, admin warps, spawn management, individual player
difficulty, a central player menu, an in-game admin panel and a built-in
official auto-updater.

## Features

- Clickable TPA flow with accept, deny, cancel and toggle controls.
- Homes with command access and an inventory GUI using player heads and glass panes.
- Admin-managed warps with clickable lists.
- Server spawn control with `/setspawn` and `/spawn`.
- Per-player difficulty GUI with EASY, NORMAL, HARD and individual Keep Inventory.
- Central `/menu` GUI with shortcuts to the main player utilities.
- `/polarutilities` admin hub with debug, reload, update checks and settings.
- Official auto-updater locked to PolarUtilities GitHub releases.
- YAML storage and feature folders designed for future expansion.

## Installation

1. Download the latest `PolarUtilities-*.jar` from the [Releases](https://github.com/polarco/PolarUtilities/releases/latest) page.
2. Put the JAR in your Paper server `plugins/` folder.
3. Restart the server.
4. Optional: open `/polarutilities settings` in game to adjust the plugin.

The plugin is built for Paper 26.1.2 and Java 25+.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/menu` | Open the central player menu. | `polarutilities.menu.use` |
| `/tpa <player>` | Request to teleport to another player. | `polarutilities.tpa.use` |
| `/tpahere <player>` | Request another player to teleport to you. | `polarutilities.tpa.use` |
| `/tpaccept [player]` | Accept a pending teleport request. | `polarutilities.tpa.use` |
| `/tpdeny [player]` | Deny a pending teleport request. | `polarutilities.tpa.use` |
| `/tpacancel [player]` | Cancel outgoing teleport requests. | `polarutilities.tpa.use` |
| `/tptoggle` | Enable or disable incoming TPA requests. | `polarutilities.tpa.use` |
| `/sethome [name]` | Save a home at your current location. | `polarutilities.home.set` |
| `/home [name]` | Teleport to a saved home. | `polarutilities.home.use` |
| `/homes [gui\|list]` | Open or list saved homes. | `polarutilities.home.use` |
| `/delhome <name>` | Delete a saved home. | `polarutilities.home.set` |
| `/setwarp <name>` | Create or update a warp. | `polarutilities.warp.admin` |
| `/delwarp <name>` | Delete a warp. | `polarutilities.warp.admin` |
| `/warp <name>` | Teleport to a warp. | `polarutilities.warp.use` |
| `/warps` | List available warps. | `polarutilities.warp.use` |
| `/setspawn` | Set the main spawn. | `polarutilities.spawn.set` |
| `/spawn` | Teleport to the main spawn. | `polarutilities.spawn.use` |
| `/dificuldade [status]` | Open the personal difficulty GUI or show your current status. | `polarutilities.difficulty.use` |
| `/dificuldade admin <enable\|disable\|reload>` | Toggle or reload the difficulty module at runtime. | `polarutilities.difficulty.admin` |
| `/polarutilities` | Open the admin command hub. | `polarutilities.admin` |

## Admin Settings

Admins can configure the plugin in game:

```text
/polarutilities settings
```

Numeric options use left click to increase and right click to decrease. Holding
shift applies larger steps. Boolean options toggle on click. Text options can be
edited with:

```text
/polarutilities settings set <path> <value>
```

Useful examples:

```text
/polarutilities settings set tpa.allow-self-request true
/polarutilities settings set menu.gui-title Menu do Servidor
/polarutilities settings set module.enabled false
/polarutilities settings set update-checker.auto-download false
/polarutilities settings set update-checker.enabled false
/polarutilities updates
/polarutilities debug
```

## Auto Updater

Official builds are preconfigured to read:

```text
https://raw.githubusercontent.com/polarco/PolarUtilities/main/release/update.json
```

Server owners can opt out at any time:

```text
/polarutilities settings set update-checker.enabled false
```

Automatic downloads can be disabled while keeping manual update checks:

```text
/polarutilities settings set update-checker.auto-download false
```

The metadata URL and download URL are locked in the plugin code for official
builds. They are not exposed in the admin GUI or settings command.

The update metadata lives in [`release/update.json`](release/update.json). When
a new version is published, `latest`, `downloadUrl`, `changelogUrl` and
`message` should be updated before tagging the release. The auto-updater uses
the official release artifact pattern:

```text
https://github.com/polarco/PolarUtilities/releases/download/vX.Y.Z/PolarUtilities-X.Y.Z.jar
```

## Build From Source

```bash
./gradlew clean build
```

The plugin JAR is generated at:

```text
build/libs/PolarUtilities-0.7.0.jar
```

## Project Structure

```text
src/main/java/br/com/polarutilities/
|-- feature/
|   |-- admin/
|   |-- difficulty/
|   |-- home/
|   |-- menu/
|   |-- spawn/
|   |-- tpa/
|   |-- update/
|   `-- warp/
|-- storage/
|-- teleport/
`-- util/
```

New gameplay modules should implement `PluginFeature`, register commands in
`enable()` and keep storage isolated behind a service class.

## Maintainers

- Release workflow: [docs/RELEASE_PROCESS.md](docs/RELEASE_PROCESS.md)
- Local maintenance workflow: [docs/MAINTAINING.md](docs/MAINTAINING.md)
- Changelog: [CHANGELOG.md](CHANGELOG.md)
