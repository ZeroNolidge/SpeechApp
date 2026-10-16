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
  words, never spoken, and no 🔊 button there (`ScreenFrame(showHear = false)`).
- **Reward pop-ups** (`GiftOverlay`, e.g. "Hero cape") close by themselves after 4 s or on any tap.
- **No ads, outside links, buy buttons, timers, lives, streaks or buzzer sounds.** Gentle only:
  wrong answers fold away with "try again".
- Must keep working as a static site on GitHub Pages.
- **Continue button:** one per step, drawn by `ContinueHost` (Guide.kt): same size
  (`ContinueSize`), pinned bottom-right inside the screen. Steps register it with `ContinueAction`.
  Narration shows it 2.5 s after it starts (always before 3 s) and, from then on, a tap anywhere
  empty moves on (`ContinueAction(delayMs = 2500, tapAnywhere = true)`), whatever the audio does.
  Quiz steps move on by answering (3rd miss shows the answer), so they have no skip button. Steps leave
  `ContinueReserve` free at the bottom-right for the button and the 👉 hand beside it.
- **Hands and hints never cover tiles, keys or headers:** they sit in empty space beside the
  target and hide as soon as anything is tapped (the continue hand, the map hand, which comes
  back after 8 s of quiet).
- **Mic permission** is asked up front, once, on the first-launch "Can Rory hear you?" screen
  (`MicHelloScreen`, after a tap; `Progress.micAsked`), later on the web start tap if the browser
  forgot, or from the grown-ups page ("🎤 Turn on listening", shown whenever listening is off,
  including after "Not now"). Never mid-story. `MicPermission.status` is ON / BLOCKED / NOT_ASKED; the grown-ups
  page shows it with steps to re-enable (iPad Settings → Safari → Microphone).
- **Speech:** spoken words make the story happen (`SpeakMoment`). The word is played slowly, then
  "Your turn!" and ~4 s of listening; quiet → "Let's say it together!", replay, one more listen
  (max 2 tries), then move on. Whispers/partial words count (generous `VoiceMeter`). No mic: Rory +
  big "I said it!" button. Listening is **voice detection by loudness**
  (`VoiceDetector` + `logic/VoiceMeter.kt`), NOT speech recognition (unreliable on iPhone/iPad and
  with young kids). Voice heard: celebrate + shiny sticker. Quiet: "Let's say it together!", replay,
  carry on. No mic / denied: Rory says "Say it out loud to me!" and waits for a tap. ▶ always skips.
- **Wrong answers, one pattern everywhere** (`Misses`, `AnswerRow`, `TryAgainSlot` in
  `Common.kt`; `MoreView` and `RobotView` follow it too): every tap is checked against the answer
  first, and tile colours come from one unit-tested rule (`tileLook` in `logic/Quiz.kt`): only the
  correct answer can ever be green or glow. 1st miss: wiggle + "Try again!" spoken and written (in a reserved slot above the
  answers). 2nd miss: Rory pops up in that slot and says a game-specific hint out loud (`hint`:
  "Count with me: one, two, three", "Listen: buh, buh, ball") while the right answer glows. 3rd
  miss: wrong tiles grey out, the right one is shown and spoken, then it moves on by itself. A
  shown answer reports `revealed = true` (`ChallengeView.onDone(firstTry, revealed)`) so the story
  doesn't cheer and no star is given. Never celebrate a wrong answer. Every puzzle starts clean
  (`key(challenge)` in `ChallengeView`, `key(roundNo)` in practice): no leftover bubble or glow.
- **Growth without numbers:** no levels, XP or star counts. Gems are for words he *tried* to say
  (not right answers); they fill the 🎒 backpack (`Progress.addGem`), and a full backpack gives a
  paper piece. Practice shows ~3 games that rotate daily (🔄 for others).
- **Rory's voice is pre-made OpenAI clips** (web): `tools/make_voice.py` (run by the owner on
  their computer with `OPENAI_API_KEY` from the environment or a gitignored `.env`; never in the
  app or CI) reads every string in commonMain, cuts out the changing parts, and saves one mp3 per
  fixed piece in `app/src/wasmJsMain/resources/audio/` (filename = sha1 of the normalized text)
  plus `manifest.json`. `ClipVoiceEngine` (common) plans each line with `logic/ClipText.kt`
  (normalize, split at . ! ? : ; …, fewest clips per phrase) and plays clip runs gaplessly via
  `WebClips` (Web Audio, trimmed silence; slow = audio element, pitch kept); anything without a
  clip (his name, own words) uses the browser voice. **ClipText.kt and make_voice.py must use the
  same rules.** No manifest = browser voice as before. The offline cache includes the clips
  automatically; sw.js keeps clips (like the .wasm) in the long-lived cache, cache-first, so they
  download once. A remade clip gets a new name (`-` + sound fingerprint); manifest.json is the only
  key-to-file record. Letter sounds: `--list-letters` (and audio/letters.html), `--redo X --say Y`
  (tools/voice_overrides.json). Android still uses its own TTS.
- **Speech never blocks:** all talking goes through `QueueSpeaker` (one utterance at a time, a time
  limit on each). If speech doesn't finish in time the flow carries on and the top-right 🔊 pulses
  in its own spot until tapped (`HearAgainButton`, `app.audioTrouble`); no overlay covers anything. Chrome sometimes never fires `onend`; that used to freeze the story.
- **Layout must hold at 360px wide.** Never put a fixed-size button (especially ▶) at the end of
  a row of fixed-size things: give it its own space first and let the rest take `weight`. Narrow
  screens (< 520dp) show 2 choices; story/challenge sit side by side over 700dp or in landscape,
  and practice games (`QuestionLayout`) put the picture left and answers in a column right over
  700dp (any way up, so iPad portrait too), and the say-a-word screen (`SpeakMoment`) puts its
  picture card left (~60%, the picture filling it) and its buttons in their own panel on the right
  (a gap between), over 700dp; under that the buttons sit in their own panel under the card. Story say-a-word steps on a
  screen 768dp or wider (`LocalScreenWidth`) put the story scene left (60%) and the word card and
  buttons right; buttons are dp-sized, so the same physical size as on a phone. Choice tiles put the word beside the picture when the tile is short or wide. The chapter
  header says "Adventure" and drops the footprints when there's no room; labels never cut off.
  In `BoxWithConstraints`, read `maxWidth`/`maxHeight` into vals at the top (nested Row/Column
  can't use them implicitly; CI fails).
- Back: ⬅️ next to 🏠 only where back differs from home (inside Practice, buddy picker from closet).
- Only use symbols that are emoji (they're in the web emoji font) or in Sniglet. Plain symbols like
  → ➜ ✓ ⌫ ▲ ● show as boxes on the web. Draw arrows if needed.
- Not a substitute for a speech-language pathologist; keep that note on the grown-ups page.

## Art style (decided)

- **Paper cut-out**, warm and bright colours (`PaperColors`, `Kid` in `ui/theme/Theme.kt`).
  **Decoration always stays behind buttons; if anything makes a button harder to see, the button
  wins** (scenery gets a light `PaperStyle.sceneryWash`).
- **Layered depth:** scenes are 3 to 5 stacked paper layers (`Scenery`: sky, drifting clouds, far
  hills, near hills, ground; the map: sea, island, front clouds), each with a soft shadow lit from
  the top left, deeper the nearer it is (2 to 4dp). Buttons get the deepest shadow
  (`PaperStyle.lift`, 8dp).
- **Paper feel:** faint paper-fibre grain (`PaperTexture`, one shared texture) on large coloured
  shapes and a thin white hand-cut edge (`PaperPen.edge`). Text, numbers and icons stay on smooth
  chips (`PaperChip`) with no texture.
- **Palette per area** (`Land.palette` in `Scenery.kt`, 4 or 5 bold colours each: Forest deep
  greens + mustard, Lagoon coral + teal, ...). **Tomato red (`PaperColors.tomato`,
  `PaperColorsUi.home`) is only for things he can tap**: 🏠, the Adventure button, the ring round
  the next map stop, the pulsing 🔊. Never in scenery or characters.
- **Bake, don't redraw** (`ui/art/Bake.kt`): paper art is drawn once into an image with its
  shadow, edge and grain (`BakedLayer`, `BakedPaperElement`, used by `paperCard` and the raised
  button base/face); only the images move. Keep animated state reads in draw/graphicsLayer
  lambdas, not in composition. The web caches rough outlines (`OutlineCache`).
- **Motion** (`ui/art/Motion.kt`): one-off animations stay under 0.5 s (`MaxMotionMs`): scene
  layers stand up like a pop-up book (`Modifier.popUp`), stickers peel on (`Modifier.peelIn`),
  confetti is a quick burst. Gentle loops use `InfiniteTransition.loop`. With the device's reduce
  motion setting on (`rememberReduceMotion`, `LocalReduceMotion`) nothing moves by itself. The map's
  layers drift at different speeds when it's dragged (no tilt sensors) and spring back.
- **Characters are paper puppets** (`ui/art/Critter.kt`): body, head and arms are separate baked
  cut-outs joined by brass split pins at the shoulders and neck; moods move the arms on their pins
  (HAPPY, CHEER, THINK, SAD, SLEEP), and `talking = true` gives a small hinge wiggle while speech is
  playing. The hero wears a big teal paper scarf (`scarf`, via `HeroWithPet`); Rory (the robot) has
  a stitched antenna with a gold star. Outfits: hat, cape, wings, glasses, colour, pet.
- Draw shapes with the paper helpers (`paperCircle`, `paperOval`, `paperRect`, `paperRoundRect`,
  `paperArc`, `paperPath`, `Modifier.paperCard`), not raw `drawCircle` etc. They give rough torn
  edges (deterministic, so no flicker). Strokes stay plain.
- Font: Sniglet (OFL, `FONT_LICENSE_Sniglet_OFL.txt`). **Letters he must recognise use Andika**
  (`LetterFont`, OFL, `FONT_LICENSE_Andika_OFL.txt`, made for early readers): Sniglet's rounded
  small letters read as other things (its v looked like a heart, g like 9, t like +). Tile text
  comes from `Letters.tileText` / `questionText` (unit-tested: only letters, one case per round).

## How the game works

- **Trail** (`Trail` in `logic/Rpg.kt`): 8 lands × 2 stops = 16 stops. Each stop is one chapter
  (`ChapterScreen`, 3 to 5 minutes) ending in "see you next time". Finishing unfolds the next stop.
  After stop 16 a new lap starts, so there's always a next chapter. Nothing locks or goes back.
  New lands bring new skills: Market Town = coins, Puzzle Peaks = patterns on a rope bridge.
- **Buddy pieces:** from a full backpack (4 gems, one per word tried) and a "welcome back" piece
  on the first visit of a new day (`Progress.checkIn`). Order is `Catalog.awardOrder`. Never taken
  away. New pieces pop up on `GiftOverlay`.
- **Lost Puppy Pip** (`logic/Pip.kt` `PipStory`, `ui/screens/Pip.kt`, Forest tile "Find Pip"): he
  calls "Pip!" / "Here, puppy!" (calls step up with `speechStep`); EVERY call makes Pip visibly
  hop one step closer (`hops`, 9 in all, `PipStory.HOPS`): from behind the bush, past the rock (Pip
  is drawn behind the spot it sits at, so it peeks over), to the log, then out in front; spots
  rustle and fold open as extras. One goodnight turn ends with Pip asleep: 10 turns. Finishing
  opens the Echo Cave reward stop (`Progress.echoReady`, the 💎 button on the map). His own words, then target-sound
  words, are the "ears" call at each spot ("Pip loves Grandma! Say: Grandma!"). Every try works
  (heard: Rory's `Echo`; quiet: "Pip heard you!"). The big "I called!" button is always shown in
  the same spot (ContinueAction label). Finishing adds a tag on Pip's collar (`Progress.pipTags`).
- **Echo Cave** (`logic/EchoCave.kt`, `ui/screens/EchoCave.kt`): 8 tries; Rory says a word (own
  words, then target-sound words, then everyday words at his speech step), he says it into the
  paper cave, the echo bounces back ("pig... pig... pig...") and a crystal lights up. Loudness only;
  "I said it!" always shown; no score/timer/fail. Ends back in the story (next chapter), or, at the
  end of a long play, at `RestScreen`.
- **Session length:** after `SESSION_MINUTES` (12) of play (`AppServices.sessionLong`), the
  chapter's goodbye goes to the Echo Cave and then `RestScreen` ("Pip's sleepy, let's rest!",
  today's sticker). The grown-ups page lists 2-3 "Words to say today" (`TodayWords.pick`).
- **Quiet difficulty** (`logic/Ladder.kt`, `Progress.ladder*`): speech (word, two words "big
  pig", short sentence: `SpeechSteps`), numbers (count to 5, to 10, add one: `Numbers.forStep`)
  and patterns (AB, ABB, ABC: `Patterns.makeStep`) step up after 4 of the last 5 right across at
  least two sessions (`Progress.sessionNo`), and back after 2 misses in a row. Never shown.
- **Back after days away:** the map says "I missed you!" (`Progress.daysAway` >= 2). No streaks.
- **"Tap the B!"**: after saying a word with a speech sound (`SpeakMoment`, sound != null), he
  finds its letter among 2 (speech step 0) or 3 (`AnswerKey.tapLetter`, `LetterTap` via
  `AnswerRow`, so the same try-again pattern). Skipping the word with ▶ skips the letter too.
- **Rory models then expands** (`logic/Echo.kt`): before his turn Rory says the word three times
  (slow, clearly, once more; twice for a phrase). When the mic hears him, Rory says the target word
  back slowly and clearly, then expands it (`Echo.expansion`: "A big red ball!", "Hi, Grandma!"). Loudness only, never speech recognition.
- **Answers live in one place** (`logic/AnswerKey.kt`); every quiz goes through `QuizState.tap`
  and `AnswerRow(key = AnswerKey.x(c))`. `PlayEveryQuizTest` plays every quiz kind with a right and
  wrong tap and fails if a wrong tap is ever marked right.
- **Every tap reacts:** `PaperButton` crinkles (`Sfx`) and presses down; scenery decor wiggles
  (`WigglyDecor`); pictures that aren't buttons (the word picture, story characters, quiz pictures,
  Pip and its hiding spots) wiggle (`Modifier.wiggleOnTap`, watches only); any unclaimed tap leaves
  a paper puff and a pop (`PuffOverlay`). Listening dims the
  scene (`app.listening`) and Rory (`RoryListens`) cups his hand to a big glowing gold paper ear at
  his head, inside a ring that grows with his voice; with no mic he still listens that way beside
  the "I said it!" button. (The old ear was tiny and mid-body: it read as a grey dot on phones.)
- **Offline + install:** the CI stamp step writes every file into sw.js `PRECACHE`; the grown-ups
  page shows "works offline" / "progress protected" (`AppHome`, `navigator.storage.persist()`)
  and an add-to-home-screen button with iPad steps.
- **Own words and target sounds** (grown-ups page, up to 8 words, 1 or 2 sounds) are used most of
  the time in the speech plan and the first-sound game (`Letters.targetsFor`).
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
- **Map:** every land wears a label chip above its landmark: picture + one-word name
  (`Land.shortName`), bright when open, paler under mist (the castle's sits to its left, clear of
  the 🔊). Open lands are never misty; on a land's last stop the next land peeks through half-mist.
  A land's second stop sits a third of the way to the next land. The map hand points at the stop
  from the side away from the buddy (👉/👈), never over a label. Tapping a
  misty land shakes its mist, shows a "✋ Not yet!" chip and Rory walks, pointing 👈/👉, along a
  sparkly trail from there to the next stop (seen without sound, for a muted iPad; "Follow me!"; about 2 s, an exception to the 0.5 s rule because it is the hint itself). **One shared list of map
  areas** (`MapAreas` in `logic/MapAreas.kt`, unit-tested) gives each land's label and landmark
  positions; labels are drawn from it and ONE tap handler on the island layer matches taps against
  it (`MapAreas.target`): each area has ONE zone (picture, label and the gap between); a picture
  or label always opens its area, then the next-stop button, then the gap. The stop button is
  drawn but takes no taps itself (`PaperButton(pressedBy = ...)`), and `MapAreas.stopSpot` moves
  it clear of every zone while keeping it clearly nearest its own land (`belongsTo`; a fresh start's
  button is at the Forest, never by the misty Meadow; unit-tested for all 16 stops). The next
  stop's land glows gold with three sparkles until he taps it or its button. The misty-land line is
  "Not yet! Follow me to Forest!" (`shortName`); the Meadow stays misty until the first Forest
  chapter is done. That fixed
  the sunflower starting a story: the stop button sat under it and took the tap. Logs `MAPTAP x,y
  -> LAND/STOP/sea`, `MAPAREAS` (labels), `MAPMARKS` (pictures + stop) and `MAPSCENE` (what each
  area opens), which the smoke test taps and prints as an AREA LIST. Tapping an open land, or the next land peeking through its mist, opens that
  area (`Screen.LandGames`, `gamesFor(land)`: Meadow = count, add, take away, more; Forest = its
  story, "Find Pip" + talking practice). The story
  starts from the next-stop marker (ring included) or the Adventure banner. A wide
  "▶ Adventure!" banner (tappable all over, presses down like every `PaperButton`) starts the next chapter. The island keeps a 1.75:1 shape.

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
- **Seeing the web app without a device:** the CI smoke test (`tools/smoke_web.mjs --shots`) prints
  small screenshots (first launch, map, story, next step) as `SHOT <name> <base64 jpeg>` lines in
  the web job log. Fetch the log with `get_job_logs` (return_content, ~400 tail lines; a subagent can
  decode the saved output into JPEGs) and look at them. It also taps the Meadow and Forest
  labels (printing `CHECK map tap ...: ok/FAIL`) and opens screens at iPad portrait, iPad landscape
  and phone sizes (`#test=ADD`, `#test=talk`, `#test=land-MEADOW` URL hooks in `Main.kt`) to check
  the two-column switch for real. GitHub Pages itself is blocked from here.
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
