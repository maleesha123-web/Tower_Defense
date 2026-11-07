# Sound design (SOUNDS.md)

All 13 `.wav` files in this folder are **procedurally generated** by
`engine/SoundSynth.java` - rendered note-by-note in Java, then wrapped in a
standard WAV header. No downloads, no copyright issues, deterministic output
(the same bytes every run, verified by `TestRunner.testSoundSynth`).

Format: 8-bit mono PCM at 11025 Hz - tiny files (whole set is ~124 KB), crisp
enough for retro-style game SFX, and guaranteed decodable by every JRE.

## Effect list

| File | Trigger |
|---|---|
| `click.wav` | buttons / tower placed |
| `attack.wav` | tower fires (throttled: at most one per 120 ms) |
| `hit.wav` | projectile impact / enemy damaged |
| `correct.wav` | correct math answer (rising arpeggio) |
| `wrong.wav` | wrong math answer / timeout (falling tone) |
| `wave.wav` | new wave started / wave cleared |
| `gameover.wav` | base destroyed |
| `victory.wav` | all waves cleared (fanfare) |
| `build.wav` | tower placed |
| `upgrade.wav` | tower upgraded |
| `leak.wav` | enemy leaked through to the base |
| `challenge.wav` | math challenge panel opened |
| `music.wav` | 8 s calm C-G-Am-F loop (music toggle, now ON by default) |

## Regeneration

```
java -cp bin com.mathtowerdefense.tests.SoundAssetExporter [target-dir]
```

Default target is this folder. Adding a new effect = 3 steps:

1. Add a recipe in `SoundSynth.specFor(...)`.
2. Add the enum value in `SoundManager.Sound` (same file base name).
3. Run the exporter (the new `.wav` appears automatically).
4. Fire it from the engine with `SoundManager.getInstance().play(...)`.

## Fallback guarantee

Delete any (or all) `.wav` files and the game keeps sounding the same:
`SoundManager` renders the IDENTICAL recipe live from `SoundSynth` when a
file is missing, and becomes a silent no-op when the audio device itself is
unavailable. Sound can never crash the game.
