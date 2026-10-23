# Ferramentas

Scripts que geram os recursos do jogo. Rode da raiz do projeto.

| Script | O que gera |
|---|---|
| `python3 tools/make_art.py .` | ícones do app (todas as densidades) e imagens da loja em `store/` |
| `python3 tools/make_sfx.py app/src/main/res/raw` | efeitos sonoros (.wav) |
| `python3 tools/make_music.py app/src/main/res/raw/music_chill.ogg` | música de fundo (precisa do ffmpeg) |
| `python3 tools/translations.py app/src/main/res` | traduções fr, de, tr, in, hi |

`EngineTest.kt` testa a lógica do jogo fora do Android e `AdventureBench.kt` simula fases da aventura com um robô
(taxa de vitória, jogadas, estrelas). Os dois rodam com o compilador Kotlin do cache do Gradle:

```bash
sh tools/kt.sh tools/EngineTest.kt
sh tools/kt.sh tools/AdventureBench.kt          # fases de exemplo
sh tools/kt.sh tools/AdventureBench.kt 20 40 60 # fases escolhidas
```

No Windows, rode os scripts Python com `PYTHONUTF8=1`.

Precisa de Python 3 com `numpy` e `Pillow`.
