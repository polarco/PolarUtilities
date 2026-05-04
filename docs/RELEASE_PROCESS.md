# Publicando uma nova versao

Use este checklist toda vez que for soltar uma versao nova do PolarUtilities.

## 1. Escolher o tipo de versao

- Correcao pequena: aumenta `patch`, exemplo `0.4.0` para `0.4.1`.
- Feature nova: aumenta `minor`, exemplo `0.4.0` para `0.5.0`.
- Mudanca grande ou quebra de compatibilidade: aumenta `major`, exemplo `0.4.0` para `1.0.0`.

## 2. Atualizar arquivos

Atualize:

- `VERSION`
- `CHANGELOG.md`
- `release/update.json`
- qualquer documentacao afetada

No `release/update.json`, mantenha este formato:

```json
{
  "latest": "0.5.0",
  "downloadUrl": "https://github.com/SEU_USUARIO/PolarUtilities/releases/latest",
  "changelogUrl": "https://github.com/SEU_USUARIO/PolarUtilities/blob/main/CHANGELOG.md",
  "message": "Resumo curto e claro da nova versao.",
  "critical": false
}
```

Use `"critical": true` so quando a atualizacao for muito importante.

## 3. Testar localmente

```bash
./gradlew clean build
./gradlew --warning-mode all build
```

O JAR fica em:

```text
build/libs/PolarUtilities-VERSAO.jar
```

## 4. Enviar para o GitHub

Troque `0.5.0` pela versao real:

```bash
git add .
git commit -m "Release 0.5.0"
git push origin main
git tag v0.5.0
git push origin v0.5.0
```

Quando a tag for enviada, o workflow `.github/workflows/release.yml` cria a
Release automaticamente e anexa o JAR.

## 5. Conferir

No GitHub:

1. Abra a aba `Actions` e confirme se o build passou.
2. Abra a aba `Releases` e confirme se apareceu a nova versao.
3. Confira se o JAR esta anexado.
4. Abra o `release/update.json` pelo link raw e confirme se `latest` esta certo.
