Bundled sound effects (procedurally generated - no downloads, no copyright issues)
==================================================================================
12 game SFX + 1 background-music loop, all rendered by SoundSynth.java:

  music.wav      - 8 s calm C-G-Am-F background loop (now ON by default)
  click.wav      - button click
  attack.wav     - tower attack (throttled: max ~8/s so it never spams)
  hit.wav        - enemy hit
  correct.wav    - correct math answer (rising arpeggio)
  wrong.wav      - wrong math answer (falling tone)
  wave.wav       - wave completed
  gameover.wav   - game over (descending)
  victory.wav    - victory (fanfare)
  build.wav      - tower built
  upgrade.wav    - tower upgraded
  leak.wav       - enemy leaked through to the base
  challenge.wav  - math challenge appeared

Regenerate them at any time with:
  java -cp bin com.mathtowerdefense.tests.SoundAssetExporter

Delete any file and the game keeps working: SoundManager renders the IDENTICAL
composition live from the same SoundSynth recipes (see resources/sounds/SOUNDS.md).
