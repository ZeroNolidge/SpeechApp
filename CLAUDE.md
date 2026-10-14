# Talk & Learn: notes for Claude

An Android learning game the owner is building for their son (almost 5, has speech issues,
**can't read yet**). README.md describes features; this file holds the decisions and working
rules behind them.

## Who it's for: design rules

- **Never rely on reading.** Every instruction is spoken (text-to-speech). Point at things by
  picture and motion, not by name: "Tap the big trees, where the hand is pointing!", not
  "Tap Talking Forest". Use the bouncing 👆/👇 hand (`PointingHand`), pulsing gold rings, and
  the big green ▶ `GoButton` for "continue". Repeat hints if idle (`Nudge`).
- **The microphone turns on by itself** after "Your turn! Say X!" (`SayPanel`). Kids can't
  find a talk button. Two silent turns stop auto-listening; three misses give an effort star.
  The 👍 "Said it!" button lets a grown-up reward a good try (recognizers struggle with kids).
- Always gentle: wrong answers fade with "try again", never negative feedback.
- Speech practice matters most. Every quest includes speech; grown-ups can pick focus sounds.
- Grown-up settings sit behind a long-press on ⚙️.

## Art style (decided)

- **Paper cut-out**, warm and bright colours (`PaperColors`, `Kid` in `ui/theme/Theme.kt`).
- Draw shapes with the paper helpers in `ui/art/Paper.kt` (`paperCircle`, `paperOval`,
  `paperRect`, `paperRoundRect`, `paperArc`, `paperPath`, `Modifier.paperCard`), not raw
  `drawCircle` etc. They give rough torn edges (deterministic `DiscretePathEffect`, so no
  flicker) and soft shadows. Strokes (lines, waves) stay plain. Paper grain overlays the app.
- Font: Sniglet (OFL, bundled in `res/font`, licence in `FONT_LICENSE_Sniglet_OFL.txt`).
- Characters are drawn in code (`ui/art/Critter.kt`), not images. Moods: HAPPY, CHEER, THINK, SAD.

## Work in progress / agreed next steps

1. **Art round 1 done** (paper style everywhere). Waiting on owner feedback (screenshots):
   edge roughness, shadow strength, font. Tune in `PaperInk` (`Paper.kt`).
2. **Art round 2:** replace emoji in UI icons, story props, map decor and counting items with
   cut-paper drawings (~50). Plan: a `Pic(key, size)` composable that draws paper art for a
   key (the emoji string), falling back to the emoji, so emoji can be replaced bit by bit.
3. **Art round 3+:** redraw all ~150 word pictures in batches, speech-sound words first.
   The owner chose "redraw everything".
4. **Done:** Compose Multiplatform. One codebase builds the Android APK and the web PWA. The
   PWA (GitHub Pages, https://zeronolidge.github.io/SpeechApp/) is the main, continuously
   updated version, so give the owner that link rather than APK zips.

## Code map

Kotlin Multiplatform module `app` with three source sets:
- `commonMain/kotlin/...`: nearly everything. `logic/` is plain Kotlin and unit-tested:
  `Rpg.kt` (species, cosmetics, powers, lands, XP curve, map positions), `Plots.kt` (8 story
  plots, set pieces), `Story.kt` (quest generator), `Games.kt`, `Robot.kt` (sim and solver).
  `data/Content.kt` holds words, sounds, tips and emoji. `ui/art/`: Critter, Scenery,
  SetPieces, IslandMap, Paper. `ui/screens/`: Map, Quest, Talk, Practice, Wardrobe, GrownUps.
  `ui/App.kt` is the shared entry and navigation. `state/Progress.kt` sits on `Prefs`.
- Platform seams (interfaces/expect): `audio/Audio.kt` (`Speaker`, `Listener`, `Parrot`),
  `state/Prefs.kt`, `ui/platform/Platform.kt` (mic permission, back handler), and
  `drawPaper` in `ui/art/Paper.kt`.
- `androidMain`: `MainActivity`, `AndroidSpeaker/Listener/Parrot`, `AndroidPrefs`
  (SharedPreferences "progress"), `PaperAndroid.kt` (DiscretePathEffect), manifest and res.
- `wasmJsMain`: `Main.kt` (tap-to-start screen, emoji font preload), `WebAudio.kt` (Web Speech
  synthesis and recognition, MediaRecorder via `js()` interop), `WebPrefs.kt` (localStorage),
  `PaperWeb.kt` (jittered outlines), and `resources/` (index.html, manifest, sw.js, icons).
- Fonts are Compose resources (`commonMain/composeResources/font`). The web build also ships
  `wasmJsMain/composeResources/font/noto_color_emoji.ttf`, a trimmed Noto Color Emoji, because
  browsers running Compose have no emoji. CI regenerates it with `tools/make_emoji_font.py`.
- Keep `commonMain` free of `java.*`/`android.*` (use `kotlin.random`, no `String.format`,
  no `System`).

## Building and testing

- **This container can't reach Google's Maven/SDK servers, so the APK can't be built here.**
  GitHub Actions (`.github/workflows/build.yml`) is the compile check. Push, then check the
  run with the GitHub MCP tools, read failing job logs, and fix.
- Logic tests can run locally with a scratch JVM Gradle project that points `srcDirs` at
  `app/src/commonMain/kotlin/com/zeronolidge/speechapp/{logic,data}` and `app/src/commonTest/kotlin`
  (uses kotlin.test; `logic` and `data` must stay platform-free).
- CI (`.github/workflows/build.yml`) has jobs `android` (tests, signed APK, `latest-build`
  release), `web` (emoji font, `wasmJsBrowserDistribution`, stamps `BUILD_ID` in sw.js) and
  `deploy` (GitHub Pages). Versions: Kotlin 2.2.20, Compose Multiplatform 1.9.0, AGP 8.7.3,
  Gradle 8.11.1, compileSdk 36. Release lint is off (it crashes on Kotlin 2.2 with this AGP).
- CI logs are long. The useful part of a failed Gradle step is usually about 100 lines above the
  end of `get_job_logs` output; the artifact log zip host is blocked from the container.
- The service worker is network-first, so the PWA updates on the next launch when online.
  Speech can't play until the first tap, hence the start screen.
- **Signing:** release builds use a permanent key from repo secrets `SIGNING_KEYSTORE`
  (base64 PKCS12, alias `talkandlearn`) and `SIGNING_PASSWORD`, so updates install over the old
  app and keep progress. Never regenerate or replace the key; that would force an uninstall
  and wipe the child's progress.
- Develop on the branch named in the session instructions; the owner hasn't asked for PRs.
- Emoji: avoid ones newer than Emoji 12 (they show as boxes on older Android).

## Working with the owner

- A parent, not a developer: plain language, short install steps, no jargon.
- They test on a Samsung tablet in landscape and send screenshots. Ask for screenshots of art
  changes, since art is drawn blind in code.
- They like being asked clarifying questions on big design choices (use multiple-choice).
- Not a substitute for a speech-language pathologist; keep that note in the grown-ups screen.
