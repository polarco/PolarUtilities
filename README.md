# PolarUtilities

Plugin de utilidades para Paper 26.1.2, com comandos de teleporte, homes,
warps, spawn, painel admin e update checker integrado.

## Recursos

- TPA: `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpacancel`, `/tptoggle`.
- Homes: `/sethome`, `/home`, `/homes`, `/delhome`.
- Warps: `/setwarp`, `/delwarp`, `/warp`, `/warps`.
- Spawn: `/setspawn`, `/spawn`.
- Admin: `/polarutilities`, `/polarutilities debug`, `/polarutilities updates`, `/polarutilities settings`, atalhos admin de warp/spawn e reload.

## Build

```bash
./gradlew build
```

O JAR fica em:

```text
build/libs/PolarUtilities-0.4.1.jar
```

## Publicar no GitHub

Se voce nunca publicou um projeto no GitHub, siga o guia:

```text
docs/PUBLISHING_GITHUB.md
```

Resumo rapido:

```bash
git init
git add .
git commit -m "Primeira versao do PolarUtilities"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/PolarUtilities.git
git push -u origin main
```

O update checker oficial ja vem apontando para este repositorio. Admins podem
desligar pela GUI ou por comando se quiserem.

## Estrutura

Cada sistema fica em `src/main/java/br/com/polarutilities/feature/<nome>`.
Novas features devem implementar `PluginFeature`, registrar seus comandos no
`enable()` e salvar dados via `PluginStorage` quando precisarem de YAML.

## Permissoes principais

- `polarutilities.admin`
- `polarutilities.tpa.use`
- `polarutilities.home.use`
- `polarutilities.home.set`
- `polarutilities.home.gui`
- `polarutilities.warp.use`
- `polarutilities.warp.admin`
- `polarutilities.spawn.use`
- `polarutilities.spawn.set`

## Settings

Admins podem abrir a configuracao pelo jogo com:

```text
/polarutilities settings
```

Na GUI, clique esquerdo aumenta valores numericos, clique direito diminui,
shift altera em passos maiores e booleanos alternam ligado/desligado. Valores
de texto tambem podem ser alterados por comando:

```text
/polarutilities settings set homes.gui-title Minhas homes
```

## Update Checker

O plugin checa atualizacoes em segundo plano quando o servidor liga. A versao
oficial ja vem pre-configurada para ler:

```text
https://raw.githubusercontent.com/polarco/PolarUtilities/main/release/update.json
```

Para desligar, o admin pode usar a GUI `/polarutilities settings` ou o comando:

```text
/polarutilities settings set update-checker.enabled false
```

Se alguem fizer um fork, ai sim pode trocar `update-checker.url` para apontar
para outro `update.json`.

Template pronto:

```text
release/update.json
```

Exemplo:

```json
{
  "latest": "0.4.1",
  "downloadUrl": "https://github.com/polarco/PolarUtilities/releases/latest",
  "changelogUrl": "https://github.com/polarco/PolarUtilities/blob/main/CHANGELOG.md",
  "message": "Resumo curto da versao mais recente.",
  "critical": false
}
```

Antes de distribuir uma nova versao, atualize o `latest`, os links e a mensagem
desse arquivo publico. Servidores com versao menor recebem aviso no console e
admins com `polarutilities.admin` tambem sao avisados ao entrar.

## Release

O processo recomendado esta documentado em:

```text
docs/RELEASE_PROCESS.md
```
