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
build/libs/PolarUtilities-0.4.0.jar
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

Depois que o repositorio existir, configure a URL de updates no servidor:

```text
/polarutilities settings set update-checker.url https://raw.githubusercontent.com/SEU_USUARIO/PolarUtilities/main/release/update.json
```

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

O plugin checa atualizacoes em segundo plano quando o servidor liga. Para
funcionar para voce e para outros servidores, publique um arquivo `update.json`
em uma URL publica e configure essa URL no plugin.

Template pronto:

```text
release/update.json
```

Exemplo:

```json
{
  "latest": "0.3.0",
  "downloadUrl": "https://github.com/seuusuario/PolarUtilities/releases/latest",
  "changelogUrl": "https://github.com/seuusuario/PolarUtilities/blob/main/CHANGELOG.md",
  "message": "Update checker integrado e melhorias de configuracao.",
  "critical": false
}
```

Configure no servidor:

```text
/polarutilities settings set update-checker.url https://raw.githubusercontent.com/SEU_USUARIO/PolarUtilities/main/release/update.json
```

Antes de distribuir uma nova versao, atualize o `latest`, os links e a mensagem
desse arquivo publico. Servidores com versao menor recebem aviso no console e
admins com `polarutilities.admin` tambem sao avisados ao entrar.

## Release

O processo recomendado esta documentado em:

```text
docs/RELEASE_PROCESS.md
```
