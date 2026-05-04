# Publicando o PolarUtilities no GitHub

Este guia e para o primeiro upload do projeto.

## 1. Criar o repositorio no GitHub

1. Entre em `https://github.com`.
2. Clique em `New repository`.
3. Em `Repository name`, coloque `PolarUtilities`.
4. Escolha `Public` ou `Private`.
5. Nao marque `Add a README file`, porque este projeto ja tem README.
6. Clique em `Create repository`.

Depois disso, o GitHub vai mostrar uma URL parecida com:

```text
https://github.com/SEU_USUARIO/PolarUtilities.git
```

Guarde essa URL.

## 2. Preparar o projeto no computador

Esta pasta ja pode estar com Git inicializado. Para conferir:

```bash
git status
```

Se aparecer `On branch main`, pule direto para a parte de conectar ao GitHub.
Se aparecer erro dizendo que nao e um repositorio Git, rode estes comandos dentro da pasta `PolarUtilities`:

```bash
git init
git add .
git commit -m "Primeira versao do PolarUtilities"
git branch -M main
```

Agora conecte a pasta local ao repositorio do GitHub. Troque `SEU_USUARIO` pelo seu usuario real:

```bash
git remote add origin https://github.com/SEU_USUARIO/PolarUtilities.git
git push -u origin main
```

Se o GitHub pedir login, use seu usuario e um token do GitHub no lugar da senha.

## 3. Conferir se subiu certo

No GitHub, confirme se aparecem estes arquivos:

- `README.md`
- `CHANGELOG.md`
- `VERSION`
- `build.gradle.kts`
- `src/main/resources/plugin.yml`
- `.github/workflows/build.yml`
- `.github/workflows/release.yml`
- `release/update.json`

Nao precisa subir a pasta `build/`. Ela fica fora pelo `.gitignore`.

## 4. Conferir o update checker

O update checker oficial ja vem pre-configurado para:

```text
https://raw.githubusercontent.com/polarco/PolarUtilities/main/release/update.json
```

Para conferir no servidor Minecraft:

```text
/polarutilities settings get update-checker.url
```

Se quiser desligar os avisos de update:

```text
/polarutilities settings set update-checker.enabled false
```

So troque `update-checker.url` se voce fizer um fork ou quiser apontar para
outro arquivo `update.json`.

## 5. Criar a primeira release

Depois do primeiro push, rode:

```bash
git tag v0.4.1
git push origin v0.4.1
```

O GitHub Actions vai compilar o plugin e criar uma Release com o JAR
automaticamente.
