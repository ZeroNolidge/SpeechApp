# Talk & Learn: notes for Claude

An Android learning game the owner is building for their son (almost 5, has speech issues,
**can't read yet**). README.md describes features; this file holds the decisions and working
rules behind them.

## Who it's for: design rules

These are the owner's rules for all work:

- **Never rely on reading.** Every screen speaks its instruction when it opens (`SpeakOnOpen` /
  `app.instruct`), and the big 🔊 "hear it again" button, top-right on every screen, repeats it
  (`app.instruction`). Point by picture and motion: "Tap the big yellow button by the trees, where
  the hand is pointing!". Use the bouncing 👆/👇 `PointingHand` and the green ▶ `GoButton`.
- **Big, well-spaced tap targets, 2 to 4 choices at a time.** Use `PagedChoices` (3 or 4 per
  page, with big ◀ ▶) instead of grids. No double-taps and no long-presses.
- **No dead ends.** 🏠 is top-left on every screen (`ScreenFrame`) and always goes to the map.
  Grown-up controls (settings, reset) sit behind `ParentGateScreen`: three numbers written as
  words, never spoken.
- **No ads, outside links, buy buttons, timers, lives, streaks or buzzer sounds.** Gentle only:
  wrong answers fold away with "try again".
- Must keep working as a static site on GitHub Pages.
- **Continue button:** one per step, drawn by `ContinueHost` (Guide.kt): same size
  (`ContinueSize`), pinned bottom-right inside the screen. Steps register it with `ContinueAction`.
  Narration shows it ~3 s after it starts (and a tap on the scene moves on), whatever the audio does.
- **Mic permission** is asked only on the web start screen or the grown-ups page ("Allow
  microphone"), never mid-story. `MicPermission.status` is ON / BLOCKED / NOT_ASKED; the grown-ups
  page shows it with steps to re-enable (iPad Settings → Safari → Microphone).
- **Speech:** spoken words make the story happen (`SpeakMoment`). The word is played slowly, then
  "Your turn!" and ~4 s of listening; quiet → "Let's say it together!", replay, one more listen
  (max 2 tries), then move on. Whispers/partial words count (generous `VoiceMeter`). No mic: Rory +
  big "I said it!" button. Listening is **voice detection by loudness**
  (`VoiceDetector` + `logic/VoiceMeter.kt`), NOT speech recognition (unreliable on iPhone/iPad and
  with young kids). Voice heard: celebrate + shiny sticker. Quiet: "Let's say it together!", replay,
  carry on. No mic / denied: Rory says "Say it out loud to me!" and waits for a tap. ▶ always skips.
- **Wrong answers, one pattern everywhere** (`Misses` in `Common.kt`): 1st miss wiggle + "Try
  again!", 2nd miss the right answer glows and is spoken, 3rd miss show it and move on.
- **Growth without numbers:** no levels, XP or star counts. Gems are for words he *tried* to say
  (not right answers); they fill the 🎒 backpack (`Progress.addGem`), and a full backpack gives a
  paper piece. Practice shows ~3 games that rotate daily (🔄 for others).
- **Speech never blocks:** all talking goes through `QueueSpeaker` (one utterance at a time, a time
  limit on each). If speech doesn't finish in time the flow carries on and a big replay 🔊 appears
  (`AudioTroubleButton`). Chrome sometimes never fires `onend`; that used to freeze the story.
- **Layout must hold at 360px wide.** Never put a fixed-size button (especially ▶) at the end of
  a row of fixed-size things: give it its own space first and let the rest take `weight`. Narrow
  screens (< 520dp) show 2 choices; story/challenge sit side by side from 768dp or in landscape.
  In `BoxWithConstraints`, read `maxWidth`/`maxHeight` into vals at the top (nested Row/Column
  can't use them implicitly; CI fails).
- Back: ⬅️ next to 🏠 only where back differs from home (inside Practice, buddy picker from closet).
- Only use symbols that are emoji (they're in the web emoji font) or in Sniglet. Plain symbols like
  → ➜ ✓ ⌫ ▲ ● show as boxes on the web. Draw arrows if needed.
- Not a substitute for a speech-language pathologist; keep that note on the grown-ups page.

## Art style (decided)

- **Paper cut-out**, warm and bright colours (`PaperColors`, `Kid` in `ui/theme/Theme.kt`).
- **One shared style** (`PaperStyle` in `ui/art/Paper.kt`, components in `ui/components/PaperUi.kt`):
  - Anything tappable is a thick raised cut-out with a drop shadow that presses down
    (`PaperButton`, `PaperIconButton`, `ChoiceTile`).
  - Scenery is flatter and muted: `SceneryWash` puts a soft wash and the paper grain over the
    background, never over text or buttons.
  - Words, letters and numbers go on plain chips (`PaperChip`) in dark ink.
  - Animations behave like paper and stay short: `popIn`, `unfold`, `flip`, `paperEnter`.
- Draw shapes with the paper helpers (`paperCircle`, `paperOval`, `paperRect`, `paperRoundRect`,
  `paperArc`, `paperPath`, `Modifier.paperCard`), not raw `drawCircle` etc. They give rough torn
  edges (deterministic, so no flicker) and soft shadows. Strokes stay plain.
- Font: Sniglet (OFL, `FONT_LICENSE_Sniglet_OFL.txt`).
- Characters are drawn in code (`ui/art/Critter.kt`). Moods: HAPPY, CHEER, THINK, SAD, SLEEP.
  Outfits: hat, cape, wings, glasses, colour, pet.

## How the game works

- **Trail** (`Trail` in `logic/Rpg.kt`): 8 lands × 2 stops = 16 stops. Each stop is one chapter
  (`ChapterScreen`, 3 to 5 minutes) ending in "see you next time". Finishing unfolds the next stop.
  After stop 16 a new lap starts, so there's always a next chapter. Nothing locks or goes back.
  New lands bring new skills: Market Town = coins, Puzzle Peaks = patterns on a rope bridge.
- **Buddy pieces:** from a full backpack (4 gems, one per word tried) and a "welcome back" piece
  on the first visit of a new day (`Progress.checkIn`). Order is `Catalog.awardOrder`. Never taken
  away. New pieces pop up on `GiftOverlay`.
- **Stickers:** every practised word becomes a sticker (`WordBook` in `logic/Words.kt`); a clear
  mic catch makes it shiny. The sticker book shows 4 per page.
- **Speech plan** (`StoryGen.sayKinds` / `practiceWord`): story words that make a scene happen
  (`StoryWords` in `logic/Plots.kt`, ladders of 1 syllable, 2 syllables, phrase), sound words at
  the current step, words from earlier days (`WordBook.dueForReview`, trickiest first), the
  grown-up's own words, and calling the sleeping visitor's name before the twist.
  `Progress.speechResult` quietly steps difficulty: 3 clear catches up, 2 misses down.
- **Grown-ups page:** settings, words grouped as going well / still tricky / just started (no
  scores), up to 3 of the grown-up's own words with a picture, and a **backup code**
  (`Progress.exportCode` / `importCode`, format in `logic/Backup.kt`) so a cleared browser or a new
  tablet doesn't start over.
- **Map:** tapping a misty land wiggles it, Rory pops up ("Visit X first!") and sparkles run to the
  next stop. A big "▶ Adventure!" button starts the next chapter. The island keeps a 1.75:1 shape.

## Work in progress / agreed next steps

1. **Pieces 2 and 3 done:** paper look and story trail, then speech inside the story. Waiting on
   owner feedback and screenshots.
2. **Art round 2:** replace emoji in UI icons, story props, map decor and counting items with
   cut-paper drawings (~50). Plan: a `Pic(key, size)` composable that draws paper art for a
   key (the emoji string), falling back to the emoji, so emoji can be replaced bit by bit.
3. **Art round 3+:** redraw all ~150 word pictures in batches, speech-sound words first.
4. The PWA (GitHub Pages, https://zeronolidge.github.io/SpeechApp/) is the main, continuously
   updated version, so give the owner that link rather than APK zips.

## Code map

Kotlin Multiplatform module `app` with three source sets:
- `commonMain/kotlin/...`: nearly everything. `logic/` is plain Kotlin and unit-tested:
  `Rpg.kt` (species, paper pieces, powers, lands, the trail), `Plots.kt` (8 story plots, set
  pieces, story words), `Story.kt` (chapter generator and speech plan), `Words.kt` (stickers and
  word history), `Games.kt` (incl. market coins), `Robot.kt` (sim and solver).
  `data/Content.kt` holds words, sounds, tips and emoji. `ui/art/`: Critter, Scenery,
  SetPieces, IslandMap, Paper. `ui/screens/`: Map, Chapter, Talk (`SpeakMoment`), Practice, Closet, StickerBook, ParentGate, GrownUps.
  `ui/App.kt` is the shared entry and navigation. `state/Progress.kt` sits on `Prefs`.
- Platform seams (interfaces/expect): `audio/Audio.kt` (`Speaker`, `VoiceEngine`, `MicLevels`, `Parrot`;
  `QueueSpeaker` and `VoiceDetector` sit on top in common code),
  `state/Prefs.kt`, `state/Clock.kt` (`today()`, local day number), `ui/platform/Platform.kt` (mic permission, back handler), and
  `drawPaper` in `ui/art/Paper.kt`.
- `androidMain`: `MainActivity`, `AndroidVoiceEngine` (TTS), `AndroidMicLevels` (AudioRecord), `AndroidParrot`, `AndroidPrefs`
  (SharedPreferences "progress"), `PaperAndroid.kt` (DiscretePathEffect), manifest and res.
- `wasmJsMain`: `Main.kt` (tap-to-start screen, emoji font preload), `WebAudio.kt` (Web Speech
  synthesis, mic loudness via getUserMedia + AnalyserNode, MediaRecorder via `js()` interop), `WebPrefs.kt` (localStorage),
  `PaperWeb.kt` (jittered outlines), and `resources/` (index.html, manifest, sw.js, icons).
- Fonts are Compose resources (`commonMain/composeResources/font`). The web build also ships
  `wasmJsMain/composeResources/font/noto_color_emoji.ttf`, a trimmed Noto Color Emoji, because
  browsers running Compose have no emoji. CI regenerates it with `tools/make_emoji_font.py`.
- Keep `commonMain` free of `java.*`/`android.*` (use `kotlin.random`, no `String.format`,
  no `System`).

## Building and testing

- **This container can't reach Google's Maven/SDK servers, so the APK can't be built here.**
  A desktop (JVM) Compose build doesn't work either: Compose's runtime comes from Google Maven too
  (`maven.google.com` redirects to the blocked `dl.google.com`).
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
