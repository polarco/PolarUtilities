# Changelog

## 0.4.1 - 2026-05-04

- Corrigido o update checker para vir pre-configurado com a URL oficial do GitHub.
- Adicionado preenchimento automatico da URL oficial quando `update-checker.url` estiver vazio.
- Documentado que o opt-out correto e desligar `update-checker.enabled`.

## 0.4.0 - 2026-05-04

- Preparado o projeto para publicacao no GitHub.
- Adicionados guias passo a passo para primeiro upload, configuracao do update checker e publicacao de novas releases.
- Adicionados workflows do GitHub Actions para build automatico e criacao de releases por tag.
- Atualizado o template de `release/update.json` para a versao atual.

## 0.3.0 - 2026-05-04

- Adicionado update checker assincrono no boot do servidor.
- Adicionados avisos bonitos de versao desatualizada no console e para admins ao entrar.
- Adicionado `/polarutilities updates` para forcar checagem manual.
- Adicionadas settings do update checker na GUI e por comando.
- Adicionado template `release/update.json` para publicar a versao mais recente do plugin.

## 0.2.0 - 2026-05-04

- Alterado o prefixo de chat de `[Polar]` para `[PolarUtilities]`.
- Expandido `/polarutilities` com help admin, debug, reload, settings e atalhos para comandos administrativos.
- Adicionada GUI de settings para configurar opcoes do plugin em jogo, com salvamento no `config.yml`.
- Adicionada a opcao `tpa.allow-self-request` para permitir TPA para si proprio durante testes.

## 0.1.0 - 2026-05-04

- Criado o plugin Paper `PolarUtilities` para Minecraft/Paper 26.1.2.
- Adicionado sistema modular de TPA com aceitar, negar, cancelar, toggle e mensagens clicaveis no chat.
- Adicionado sistema de homes com `/sethome`, `/home`, `/homes`, `/delhome` e menu GUI com cabecas e paineis de vidro.
- Adicionado sistema de warps com criacao/remocao para admins, lista clicavel e teleport por permissao.
- Adicionado sistema de spawn com `/setspawn` para admins e `/spawn` para jogadores.
- Criada base de servicos, armazenamento YAML, permissao e configuracao para facilitar novas features.
