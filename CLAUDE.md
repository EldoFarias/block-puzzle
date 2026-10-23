# Prisma Blocks — contexto do projeto

Jogo de blocos 8x8 (estilo Block Blast) para Android, só Play Store. Kotlin + Jetpack Compose, sem engine.
Dono: Eldo (EldoFarias). Comunicação em português. Monetização: AdMob (banner, intersticial, premiado).

## Como compilar e rodar

```bash
./gradlew installDebug        # instala no celular conectado (pacote com.farias.prismablocks.debug)
adb logcat -s PrismaBlocks AndroidRuntime Vibrator VibratorManagerService
```

- Debug usa sempre IDs de **teste** do AdMob; release lê os IDs reais de variáveis/secrets (`ADMOB_*`).
- `applicationId` = `com.farias.prismablocks` (não mudar depois de publicar).
- Push na `main` é só backup. O APK de teste (Releases → `teste`) sai do `.github/workflows/android.yml`, que agora é
  manual: `gh workflow run android.yml` quando o Eldo for testar pelo celular. Erros de compilação aparecem como anotações do job.
- O APK do CI e o instalado daqui têm assinaturas de debug diferentes: trocar de um para o outro exige desinstalar (perde o progresso).
- `.github/workflows/screenshots.yml` (manual): abre o app no emulador em 4 tamanhos e publica os prints em Releases → `telas`.
- Atalhos só no debug: `adb shell am start -n com.farias.prismablocks.debug/com.farias.prismablocks.MainActivity --es screen game`
  (`home`, `chest`, `game`, `adventure_game`, `map`, `themes`, `stats`; `--ez landscape true`; `--ei level 41` para a fase; `continue` retoma a partida salva; `--ez shots true` sem anúncios, para prints).

## Arquitetura

| Pasta | O que tem |
|---|---|
| `game/GameEngine.kt` | Regras puras (sem Android): tabuleiro, peças, linhas, combo, gemas, gelo, pedra, cadeado, bomba-relógio, desfazer, bomba, salvar/restaurar (v4). Testável em JVM. |
| `game/Shapes.kt` | 39 formatos de peça com pesos. |
| `game/Adventure.kt` | Fases infinitas geradas pelo número: mundos de 20 fases (cada um traz uma mecânica: pedra, gelo, cadeado, bomba-relógio; depois "Cosmos" mistura duas), chefe a cada 10 (dois objetivos), baú na trilha a cada 5, baú de estrelas a cada 30, dificuldade em ondas, limite de jogadas nas fases difíceis, objetivos gemas/linhas/pontos/gelo/desenho/limpar blocos/combo. Calibrado por simulação (`tools/AdventureBench.kt`). |
| `game/GameViewModel.kt` | Estado da partida para a UI, modos CLASSIC/DAILY/ADVENTURE, poderes, efeitos, missões. |
| `data/Progress.kt` | Tudo que persiste (SharedPreferences): moedas, temas, recordes, sequência, missões, baú, fases. |
| `data/Missions.kt` | 3 missões diárias determinísticas por data. |
| `ui/` | Telas Compose: Splash, Home, Game, Adventure (mapa em LazyColumn), AdventureUi (ícones/textos de objetivo e obstáculo), Themes, Stats, Overlays (pausa/fim/vitória), Rewards (missões/baú). |
| `ui/GemPainter.kt`, `Fx.kt`, `Background.kt` | Desenho dos blocos, partículas/textos flutuantes, fundo aurora. |
| `ads/AdsManager.kt` | AdMob + consentimento UMP; intersticial a cada 2 partidas e ≥90 s. |
| `audio/` | `SoundManager` (SoundPool), `MusicPlayer` (loop), `Haptics` (vibração). |
| `notify/DailyReminder.kt` | Lembrete diário 7h30 via WorkManager. |
| `tools/` | Scripts que geram ícone, sons, música e traduções (`translations.py`), testes da lógica. |

Telas se adaptam: compacto (altura < 660 dp), tablet (escala 1.3 nos controles) e layout deitado (largura > altura).
`android:appCategory="game"` mantém a orientação retrato em tablets no Android 16.

## Convenções

- Textos em 8 idiomas: `values` (en), `pt`, `es`, `fr`, `de`, `tr`, `in`, `hi`. Ao criar string nova, adicionar em en/pt/es
  e nos dicionários de `tools/translations.py`, depois rodar `python3 tools/translations.py app/src/main/res`.
- Gradientes radiais com raio 0 derrubam o app: sempre proteger tamanhos < 1 px antes de desenhar.
- No Windows, rodar os scripts Python com `PYTHONUTF8=1` (senão o `translations.py` quebra nos emojis e símbolos).
- Testes e simulações da lógica sem Android Studio: `sh tools/kt.sh tools/EngineTest.kt` e
  `sh tools/kt.sh tools/AdventureBench.kt [fases...]` (usa o compilador Kotlin do cache do Gradle).
  Ao mexer na geração de fases, rodar os dois: o teste confere as 300 primeiras fases.
- Visual: manter o estilo atual (vidro, blocos "gema", aurora). Obstáculos novos seguem `ui/GemPainter.kt`.

## Pendências

1. ~~Vibração no celular do Eldo~~ — **resolvida** (Xiaomi, HyperOS 1.0, API 33). Sem atributos a vibração saía como
   `Usage=TOUCH` e era descartada (`ignored_for_settings`), porque a vibração ao toque está desligada no aparelho.
   `Haptics.kt` agora declara `USAGE_MEDIA` (API 33+) / `USAGE_GAME` (26–32). Diagnóstico: `adb shell dumpsys vibrator_manager`.
2. **Publicado em produção (2026-10-23), em revisão do Google**: versão 18 (1.1.0), 177 países, conta pessoal
   ESF-FARIAS (Play Console `/u/1` no Chrome do Eldo; `/u/0` é outra conta). Falta adicionar as traduções da página da loja
   (en-US, es-ES, fr-FR, de-DE, tr-TR, id, hi-IN) com textos de `store/listings/` e prints gerados por
   `store/make_screenshots.py` (cópia em `Desktop/PrismaBlocks-PlayStore/screenshots`). Roteiro em `store/PUBLICAR.md`.
   Já feito (2026-10-23): IDs reais do AdMob nos secrets `ADMOB_*` (conta `ca-app-pub-1312827233427949`);
   keystore de upload (alias `upload`) nos secrets `KEYSTORE_*`, original guardado pelo Eldo fora do repositório;
   política em https://eldofarias.github.io/prisma-blocks/privacy.html (repo `eldofarias.github.io`, link nas
   Configurações do jogo) e `app-ads.txt` na raiz desse site. Release assinado testado localmente.
3. Fase 2: login com Google Play Games (salvamento na nuvem, ranking, conquistas).
