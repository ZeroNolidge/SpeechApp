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
4. **Proposed, not yet approved:** Compose Multiplatform so one codebase builds the APK *and* a
   web/PWA version (GitHub Pages). Only 6 files touch Android APIs: `audio/Speaker.kt`,
   `audio/Listener.kt`, `state/Progress.kt`, `ui/art/Paper.kt`, `ui/screens/Talk.kt`,
   `MainActivity.kt`. If approved, do it before art round 2.

## Code map

- `logic/` is plain Kotlin and unit-tested: `Rpg.kt` (species, cosmetics, powers, lands, XP
  curve, map positions), `Plots.kt` (8 hand-written story plots, set pieces), `Story.kt`
  (quest generator, challenges), `Games.kt`, `Robot.kt` (Cubetto-style sim and solver).
- `data/Content.kt`: words, sounds, mouth tips, letters, emoji pictures.
- `ui/art/`: `Critter.kt`, `Scenery.kt`, `SetPieces.kt` (story scenes that change when solved),
  `IslandMap.kt` (illustrated world map, mist over locked lands, birds/fish/boat), `Paper.kt`.
- `ui/screens/`: Map, Quest (intro → beats → twist → ending → rewards), Talk, Practice,
  Wardrobe, GrownUps. `ui/challenge/Challenges.kt`: every challenge type and the powers.
- `state/Progress.kt`: SharedPreferences (hero, XP, gems, items, settings, visited lands).

## Building and testing

- **This container can't reach Google's Maven/SDK servers, so the APK can't be built here.**
  GitHub Actions (`.github/workflows/build.yml`) is the compile check. Push, then check the
  run with the GitHub MCP tools, read failing job logs, and fix.
- Logic tests can run locally with a scratch JVM Gradle project that points `srcDirs` at
  `app/src/main/java/com/zeronolidge/speechapp/{logic,data}` and `app/src/test/java`
  (`logic` and `data` must stay free of Android imports for this to work).
- CI uploads artifact `SpeechApp-apk` (zip with `TalkAndLearn.apk`) and updates the
  `latest-build` release. The owner's browser often fails the direct APK download, so give them
  the artifact link: `https://github.com/ZeroNolidge/SpeechApp/actions/runs/<run>/artifacts/<id>`.
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
