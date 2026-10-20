# Rory's Talk Trail: notes for Claude

(Called "Talk & Learn" until round 13; code names like `TalkAndLearnApp`, the `talkandlearn.`
saved-progress prefix, sw.js cache names and the repo/URL keep the old name on purpose: changing
the prefix would wipe his progress.)

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
  **Nothing ever takes over the screen when nobody taps:** the welcome-back present no longer pops
  up over the map (it waits in the closet: a 🎁 on the buddy badge, `AppServices.newPiece`); the
  map's only idle reminder is Rory peeking in and waving after ~20 s (`MapWave`, max 3, any touch
  tucks him away; touches counted at the app root, `AppServices.touches`). No spoken nags on the
  map. The closet has a green ▶ back to the map. Rory's wave is a real wave
  (`Critter(waving = true)`: hand up, side to side twice, down).
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
  "Your turn!" and ~4 s of listening; quiet → "Let's say it together!", replay, one more listen;
  still quiet → Rory asks once ("Say it out loud to me! Then tap ... I said it") and keeps
  listening until any sound or "I said it!". **A talk turn never moves on by itself** (Penny's
  "thumb" did). On his turn the continue button is "I said it!" (counts as a try); while Rory
  models it's the plain ▶ skip after 3 s. Whispers/partial words count (generous `VoiceMeter`). No mic: Rory +
  big "I said it!" button. Listening is **voice detection by loudness**
  (`VoiceDetector` + `logic/VoiceMeter.kt`), NOT speech recognition (unreliable on iPhone/iPad and
  with young kids). Voice heard: celebrate + shiny sticker. Quiet: "Let's say it together!", replay,
  carry on. No mic / denied: Rory says "Say it out loud to me!" and waits for a tap. ▶ always skips.
- **Listening look, one everywhere** (`ListeningStage` in Talk.kt): on every talk turn (story word
  cards, Talk, Pip, the crystal cave, voice stories; also while waiting for "I said it!" with no
  mic) the scene dims and Rory with his cupped ear grows to about a third of the scene's height and
  slides from the bottom-left to ONE spot (`listenSpot`: the middle, a little low, always; the
  owner chose centred over stepping aside), ear pulsing, ring growing with `app.listenLevel`.
  "I said it!" shows ONLY while listening (`ContinueHost` shows the plain ▶ otherwise, same action),
  so every "I said it!" turn has this look. A voice story's target glow shows while Rory names it
  and hides on his turn (never over his ear). In a story the story scene is the stage
  (`LocalHasStage`); in Talk the card is, laid out in bands: picture in the top third, the word in
  its own band, Rory in the lower half (`ListeningStage(lowerHalf)`), so he never covers either.
  The small Rory beside the buddy (cave, Pip, voice stories) only comes back once the big one has
  fully gone (`listenLookGone`): never both at once.
- **No double skips:** a newly shown "I said it!" ignores taps for 0.5 s (`LabelSettleMs`, so a
  start tap landing there can't count); a continue tap is ignored within 0.7 s of the last one taken (`SettleMs`,
  from the last tap, not from the button appearing: first taps on a new button always count), a
  word card's ▶ skip appears after 3 s, and the chapter moves on only from the step that asked
  (`next(from)`). Before this, tapping ▶ repeatedly skipped every word of Penny's all-words chapter.
- **Say-a-word card after the try:** it stays fully visible while Rory praises (taps do nothing);
  a faded, disabled card looked broken.
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
  app or CI) reads every string in commonMain, cuts out the changing parts (`${...}`, `$name` and the story
  blanks `{f}` `{hero}` `{land}` `{v}`; friends' names also get a possessive clip, "Benny Bunny's"), and saves one mp3 per
  fixed piece in `app/src/wasmJsMain/resources/audio/` (filename = sha1 of the normalized text)
  plus `manifest.json`. `ClipVoiceEngine` (common) plans each line with `logic/ClipText.kt`
  (normalize, split at . ! ? : ; …, fewest clips per phrase) and plays clip runs gaplessly via
  `WebClips` (Web Audio, trimmed silence; slow = audio element, pitch kept); anything without a
  clip (his name, own words) uses the browser voice; each such gap logs `VOICEGAP` and the smoke
  test lists them (`VOICE GAP` lines). Speech waits up to 2 s for the clip list at start. Story sentences whose blanks are filled
  from a small set (friends, items) are also recorded whole (`tools/voice_whole_lines.txt`, made by
  `tools/whole_lines/WholeLines.kt` in the scratch JVM test project; rerun it after changing story
  text), since stitched pieces sound choppy. Sentences in a clip run get a 0.25 s breath
  (`ClipText.PAUSE`). **ClipText.kt and make_voice.py must use the
  same rules.** No manifest = browser voice as before. The offline cache includes the clips
  automatically; sw.js keeps clips (like the .wasm) in the long-lived cache, cache-first, so they
  download once. A remade clip gets a new name (`-` + sound fingerprint); manifest.json is the only
  key-to-file record. `make_voice.py` checks every clip has sound (`sound_seconds`, read from the
  MP3 frames' global gain, no extra software; under 0.2 s = silent) and remakes silent ones (last
  try with tts-1); `--check` lists them. **After the owner's batch run, run
  `python3 tools/finish_clips.py` here** (it also fades the long held sounds in and out, `FADE_KEYS`; mmm nnn lll fade in only,
  zzz out only; manifest `faded`) (needs ffmpeg + numpy, which the owner may not have): it
  trims each clip to 0.03 s lead and 0.3 s tail, evens loudness to a normal speaking level (RMS
  0.1), renames to `-f1` and lists finished keys in manifest `finished` (make_voice keeps that list
  for unchanged files); then commit. The web player rejects clips under normal loudness (voiced
  RMS < 0.03), so they use the browser voice. The player loads and checks a whole line first: a missing,
  failed or silent clip makes that part fall back to the browser voice (`ClipVoiceEngine`), and is
  remembered as missing. Both players trim each clip to its sound (slow mode cuts the audio
  element at the trimmed end), so his turn starts right after Rory. Letter sounds: `--list-letters` (and audio/letters.html), `--redo X --say Y`
  (tools/voice_overrides.json). Android still uses its own TTS.
- **Room noise** (`VoiceDetector.room`): the room's loudness is saved from each talk turn (the
  quietest fifth of the readings that weren't his voice, `VoiceMeter.roomAfter`, following the room
  up as well as down; never updated while he's making a sound) and reused by the next
  (`VoiceMeter(knownRoom)`: no settling, only a 150 ms lead; if the lead is already over the bar at
  a room-like level, ≤ `LOUD_ROOM`, the bar rises at once). It's kept across page loads
  (`Progress.savedRoom`, capped at `SAVED_ROOM_MAX`). Sound that was there from the start of a turn
  and never dips for 2 s (`NOISE_MS`) is the room; a sound he starts (quiet before it) is his for up
  to 5 s (`HOLD_MAX_MS`), and listening waits while he holds it, so a long "sssss" (Rory's model is
  2.25 s) always counts. Only the first turn ever measures, with `checkRoom` (0.4 s), which waits
  until nothing is being said. Never measure while Rory talks.
- **Pops** (`VoiceMeter(pops = true)`, BURSTS mode, p b t d g k words): each burst is judged as a
  whole. Voiced (`periodic` ≥ 0.35, `band` ≥ 0.35) for 70 ms (`POP_MIN_MS`) = his word, counted at
  once; otherwise when it ends (`Pops.counts`): one or two short readings in the voice range,
  even with one quiet reading between (a puff split across readings) = a bare "p" puff (counts;
  an odd soft cough or sniff counting too is fine, the owner said); longer and holding its loudness = a
  (loud) whisper (counts); longer but dying away = a cough (no); a click outside the voice range =
  a clap or tap (no). Pip and the cave listen like the voice stories (a loud whisper counts), and
  keep the listening look and "I said it!" for the whole turn, through Rory's "together" re-model
  (the 1 s tap guard is only at the turn's start). The web mic (FFT 2048) and Android work out
  band and voicing per 50 ms reading.
- **Word listening (on-device recognition, listen-only by default)** (`logic/WordCheck.kt`,
  unit-tested; `audio/WordListener.kt`; web: `audio/WebWords.kt`, Vosk via vosk-browser 0.0.8 +
  vosk-model-small-en-us-0.15, downloaded by CI into the site's `vosk/` folder, never in the repo,
  not precached, left alone by sw.js; the script is kept in the "talkandlearn-vosk" cache and Vosk
  keeps the unpacked model in IndexedDB by its fixed URL, so it downloads once and works offline).
  Never downloads by itself: word listening is off until a grown-up turns it on
  (`Progress.wordListening`, default off), and the model then downloads only on Wi-Fi by default
  (`wordModelWifiOnly`; `AppHome.onMobileData`, unknown = Wi-Fi, so iPads download straight away;
  rechecks every minute; once saved, `wordModelSaved`, it loads from the device on any connection).
  Loads 5 s after start when allowed (`#test=...+words` loads at once; the smoke
  test prints `WORD LISTENING: ...`). Each talk turn (`SpeakMoment`, voice stories, Pip, cave)
  passes a tiny grammar (`Swaps.check`: target, up to 3 swaps by the sound table, phrase words,
  "[unk]") to `VoiceDetector.listenTry(words, onWords)`; the mic stream feeds the recogniser
  (`window.__tlWords.attach`), the answer comes ≤1 s after the loudness result (which still moves
  the story at once). The mic never starts while `speaker.speaking`. `Judge`: right (conf ≥ 0.55),
  near-miss (a swap, conf ≥ 0.7), else unsure (= pass); s→th counts as right; r l s z sh ch j th v
  and clusters are never scored (`Speech5`, `Swaps.inCluster`). `AppServices.wordHeard` saves one
  `TryRecord` (day, word, sound, start/middle/end, heard, conf, modeled or on his own, result,
  rating; max 1500; no audio, no text) and logs `WORDTRY`. Grown-ups page: switches (word
  listening, test mode, near-miss modelling), agreement (`Agreement`: 30 rated tries the app made a
  call on, 90%), sound cards after 20 tries (`SoundCards`: by position, 4-week paper trend, "App
  estimate, not a speech assessment.", 1-2 focus sounds with 3 words, see-a-therapist note). Test
  mode shows `WordRateBar` (App.kt) after each try: "👍 Right" / "✋ Off", 12 s. Modelling (only
  when allowed + on, `AppServices.modelingOn`): clear match = full-strength move; confident
  near-miss (≥ 0.8, scored sound, `Corrections`: ≤ 1 in 3 tries per sound, never twice in a row,
  off for the sitting once tries get 30% quieter or shorter) = move, then "Listen. <modelSound>.
  <word>." with `MouthCue` (paper lip shape) and one more try; otherwise Rory's usual echo. Practice
  words prefer ones with real-word swaps (`practiceWord`). Difficulty steps don't use recognition yet.
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
  playing. Rory's mouth is drawn live while he talks (`HEAD_TALK` parts + `robotMouth`): it opens
  with the loudness of the clip playing right now (`ClipPlayer.mouth`, read from the decoded clip
  at its play position, so the slow third model is slower) through `logic/LipSync.kt`: words are
  found by quiets and by dips ("pop pop pop" has only dips), and past the last word the text's
  words come round again (so "Pop! Pop! Pop!", three clips of the same text, closes each time);
  the lips close for 100 ms at the start of a p/b/m word and at the end of a word ending in one,
  and stay closed through a held "mmm";
  with the browser voice it chatters gently. The hero wears a big teal paper scarf (`scarf`, via `HeroWithPet`); Rory (the robot) has
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
  opens the Echo Cave reward stop (`Progress.echoReady`): a glowing paper-cave stop just past the
  Forest on the island (`MapAreas.caveSpot`/`MapTarget.Cave`, through the one tap handler, clear of
  the next stop; bounces in). It lives only on the map (no 💎 button) and stays there for good once
  Pip has been found (`pipTags > 0`), glowing while new tries wait (`echoReady`, cleared when he
  finishes them). His own words, then target-sound
  words, are the "ears" call at each spot ("Pip loves Grandma! Say: Grandma!"). Every try works
  (heard: Rory's `Echo`; quiet: "Pip heard you!"). The big "I called!" button is always shown in
  the same spot (ContinueAction label). Finishing adds a tag on Pip's collar (`Progress.pipTags`).
- **Voice stories are data** (`logic/VoiceStories.kt`): each beat = backdrop (`Backdrop`: a land's
  scenery + water/island/path/sky, reusable across stories), problem line, word at 3 levels, target
  `sound` (or null), `prop` + `move`, `success` line (full fixed sentence, no blanks, so clips are
  whole), optional `light` (DAWN/DUSK) and a `still` prop for company. Shared voice-driven moves
  (`Move`: RISE, SINK, SLIDE, POP, GROW, SHAKE; `pose()` in `ui/art/VoiceScenes.kt`) take how far
  (loudness x time, `VoicePush`), how big (loudest moment) and a live wobble; props are paper art
  in `PropArt` (each can show its own paper motion from the move's progress). A new story is words
  + maybe one new prop. His own word swaps into the fixed sentences at play time (`PropSwap`).
  "I said it!" = the same move at medium strength.
- **Voice stories** (`logic/VoiceStories.kt`, `ui/screens/VoiceStory.kt`, art in
  `ui/art/VoiceScenes.kt`): his word makes the plot happen, never a "magic word" password. Pattern
  for every new story: setup with one want; 6-7 beats (max 8), each: Rory names the problem, says
  the line 3 times (3rd slower), listening look, any voice triggers the action at once
  (`VoiceDetector.listenTry` `onVoice`, ~0.15 s; a soft try `VoiceTry.SOFT` = same end state, smaller
  flourish), Rory echoes + adds one ("Go! Go, boat!"); "I said it!" does the same action. His
  target sounds start >= 4 beats' words (later stories: word ends too); one prop can be his own word
  (`VoiceStories.ready`: a capitalised name for a character, else a thing); 3 line versions per
  beat picked by `speechStep`; calm goodbye beat, then a sticker (`Progress.finishVoiceStory`).
  **Voice-action feel** (`logic/VoicePush.kt`, unit-tested): his voice pushes the action live, every
  50 ms reading (`listenTry(onPush)`, `VoiceMeter.push`): louder = faster/bigger, holding keeps it
  moving, silence stops it (first reading of voice moves it, < 0.2 s). Any clear voice finishes
  (the rest gently); a quiet try nudges partway (never past 60%) + "Again, together!"; the 2nd try
  and "I said it!" always finish at a gentle speed, so no beat needs a loud voice. While listening
  the target glows with paper sparkles and three torn cream paper wind strips stream from Rory to it
  (`VoiceListenExtras`, `voiceTarget`, drawn above the dim via `ListeningStage(aboveDim)`).
  **Pops vs holds everywhere** (voice stories, chapter cards/Talk via `SpeakMoment`, Pip, cave;
  `listenTry(mode)`, `VoicePush.modeForLine`): pop words count as heard after one 50 ms burst
  (others 150 ms); in `SpeakMoment` the picture hops per pop and grows while held (`pushed`), in
  Pip each pop rustles the hiding spot, in the cave the next crystal glimmers; a whisper always
  gives a small nudge. Pip and the cave follow the voice-story pattern: quiet → "together" + one
  more listen → wait (listening) for any sound or "I said it!" (never "I called!"); the 🦜 copy-me
  parrot waits too (`Parrot.listenThenPlay`: plays back once he has spoken, keeps waiting through
  quiet, with the listening look). Rory's modelling is visible: the word card/chip pulses once
  per word he says (the slow third bigger), a small talking Rory sits in the card corner (in Talk, centred in the lower half where he then listens), and his
  echo + added word slides in on paper (`ExpandChip`). The slow echo finishes before the praise.
  **Pops vs holds** (`VoicePush.Mode`, `modeFor(beat.sound)`): p b t d g k words count each
  separate burst as one push ("pop! pop!" = 2, about a third each; 100 ms of quiet between
  bursts); s m f sh go by how long he holds the sound; others by loudness x time. After 2 quiet
  tries it waits (`Next.WAIT`), listening, for any sound or "I said it!", which always plays at
  least medium strength (`PRESSED`). One
  paper effect per beat (sail bellies out, bubble layers burst apart, leaves flip to their darker
  side, ...); every success ends with hole-punch confetti (`HolePunchConfetti`, 1.6 s).
  Stories: Pip's Boat Trip (Lagoon), Sleepy Owl's Morning (Forest), The Balloon Picnic (Meadow),
  each a round stop by its land on the map (`MapAreas.spotBy`, `MapTarget.Story`, unit-tested with
  the cave and next stop). Extra stops always sit on the island, grass or beach (`MapAreas.onIsland`, sharing
  `ISLAND_*` with IslandMap's coast); the cave is placed before the stories (the Forest's corner
  is tight), else it and the owl ended up in the sea. The owl story's stop is its own paper picture (sleepy owl before a
  rising sun on a night-blue badge, `sleepyOwlBadge`, clipped to its circle), never the 🦉 emoji used elsewhere; it appears only once the first Forest chapter is done (`owlOpen`, announced once). Existing chapters follow it too: no magic word, 3x model with the 3rd
  slower, and the set piece changes the moment he speaks (`SpeakMoment(onAction)`).
- **Echo Cave** (`logic/EchoCave.kt`, `ui/screens/EchoCave.kt`): 8 tries; Rory says a word (own
  words, then target-sound words, then everyday words at his speech step), he says it into the
  paper cave, the echo bounces back ("pig... pig... pig...") and a crystal lights up. Loudness only;
  "I said it!" always shown; no score/timer/fail. Ends back in the story (next chapter), or, at the
  end of a long play, at `RestScreen`.
- **Session length:** a sitting of play (`logic/PlaySession.kt`, saved in `Progress.playSession`,
  wall clock `nowMillis()`) survives reloads; over 30 min away starts a new one. After 12 minutes
  (`AppServices.sessionLong`, checked at natural pauses) Rory wraps up: a story jumps to its happy
  ending after the current step, then `RestScreen`; Pip jumps to the goodnight turn; the cave, Talk
  and practice games go to `RestScreen` after the current try/round. Once per sitting
  (`sessionClosed`). `RestScreen`: "Pip's sleepy, let's rest!", today's sticker. The grown-ups page lists 2-3 "Words to say today" (`TodayWords.pick`).
- **Quiet difficulty** (`logic/Ladder.kt`, `Progress.ladder*`): speech (word, two words "big
  pig", short sentence: `SpeechSteps`), numbers (count to 5, to 10, add one: `Numbers.forStep`)
  and patterns (AB, ABB, ABC: `Patterns.makeStep`) step up after 4 of the last 5 right across at
  least two sessions (`Progress.sessionNo`), and back after 2 misses in a row. Never shown.
- **Back on a new day:** the map says "I missed you!" (`Progress.daysAway` >= 1). No streaks.
- **"Tap the B!"**: after saying a word with a speech sound (`SpeakMoment`, sound != null), he
  finds its letter among 2 (speech step 0) or 3 (`AnswerKey.tapLetter`, `LetterTap` via
  `AnswerRow`, so the same try-again pattern). Skipping the word with ▶ skips the letter too.
- **Rory models then expands** (`logic/Echo.kt`): before his turn Rory says the word three times
  (slow, clearly, once more; twice for a phrase). When the mic hears him, Rory says the target word
  back slowly and clearly, then expands it (`Echo.expansion`: "A big red ball!", "Hi, Grandma!"). Loudness only, never speech recognition.
- **Answers live in one place** (`logic/AnswerKey.kt`); every quiz goes through `QuizState.tap`
  and `AnswerRow(key = AnswerKey.x(c))`. `PlayEveryQuizTest` plays every quiz kind with a right and
  wrong tap and fails if a wrong tap is ever marked right.
- **Every tap reacts; a tap counts on lift:** `PaperButton` uses `Modifier.kidTap`: the feedback
  starts on touch (crinkle, press-down; map stops wiggle) so it feels instant, and the tap counts
  when the finger lifts (`detectKidTaps`), slow presses too, unless it slid more than `TapSlop`
  (24dp, a map drag). Each touch is followed on its own and a new touch takes over, so a lift the
  browser lost can never fire later (it used to open the wrong map stop). Touches starting within
  `EdgeIgnore` (40dp) of the screen edge are noted at the app root (`EdgeTouches`, Initial pass):
  they still tap what they land on (🏠, 🔊 and "I said it!" count right out to their edges), but a
  resting edge finger never cancels another finger's tap. A press held over `TapMaxMs` (3 s, a
  palm) doesn't count. The map's one tap handler
  uses the same `detectKidTaps`. Leaving any screen ends its listening turn (`Screens`). Taps in the first second of a listening turn are ignored
  (`AppServices.tapsSettling`, `ListenTapGuardMs`: the continue/"I said it!" button and the Talk
  card). Never put a full-screen pointer layer on top (MapWave first had one and ate every
  map tap): watch touches at the app root instead; scenery decor wiggles
  (`WigglyDecor`); pictures that aren't buttons (the word picture, story characters, quiz pictures,
  Pip and its hiding spots) wiggle (`Modifier.wiggleOnTap`, watches only); any unclaimed tap leaves
  a paper puff and a pop (`PuffOverlay`). Listening dims the
  scene (`app.listening`) and Rory (`RoryListens`) cups his hand to a big glowing gold paper ear at
  his head, inside a ring that grows with his voice; with no mic he still listens that way beside
  the "I said it!" button. (The old ear was tiny and mid-body: it read as a grey dot on phones.)
- **Never back to the title when idle:** the screen keeps a wake lock while he plays (index.html,
  released after 10 min with no touch), and where he is (`Screen.placeKey`, a voice story with its
  beat) is saved; if the page reloads by itself within a sitting (30 min), the start tap (always
  shown: iPad Safari plays no sound before a tap) opens straight back there. The start tap answers at once: a tap anywhere on the start screen counts, Rory (already standing there) waves and ▶ pops away in the next frame, and the app is built two frames later (`starting` in Main.kt); when the map comes next it is already drawn hidden under the start screen (`WarmMap`), so it opens fast. The smoke test taps with a 4x slower CPU and prints `START TAP` (wave on screen under 1 s or the check fails). #test= links don't
  save places (`AppServices.testMode`) and survive a hard reload (the hash is kept in
  sessionStorage for the tab; a fresh device keeps the test screen after the buddy pick). The smoke test idles 95 s in a
  story and prints `IDLE story 95 s: ...` with heap sizes.
- **Offline + install:** the CI stamp step writes every file into sw.js `PRECACHE` except Rory's
  voice clips (about 30 MB) and vosk/: nothing extra downloads before a grown-up turns it on.
  Clips are kept once played (cache-first); the grown-ups switch "Save Rory's voice for offline
  play" (`Progress.saveVoiceOffline`, `AppHome.saveVoice`) saves them all. The mic switch shows off
  when the browser blocks the mic, with the steps under it. "Start over" and a one-time migration
  (`wordListeningChosen`) put word listening back to off; the grown-ups
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
  and phone sizes (`#test=ADD`, `#test=talk`, `#test=land-MEADOW`, `#test=story-balloon@3` (a voice story from beat 4), `#test=saybeat-climb` (the first chapter step saying that word) URL hooks in `Main.kt`) to check
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
- The owner's copy of the repo sits in OneDrive (Windows, GitHub Desktop), which can bring back
  clips git removed: if GitHub Desktop shows thousands of new .mp3s after a voice run, have them run
  `python tools/tidy_clips.py` (moves clips not in manifest.json to `../SpeechApp-old-clips`) before
  committing. Commit Summary for a voice run: "Rory's voice: new lines".
- **Voice clips are batched:** don't ask the owner to run `tools/make_voice.py` after each change
  (lines without a clip just use the browser voice meanwhile). Keep track of how many lines are
  waiting (`python3 tools/make_voice.py --dry-run` here, no key needed) and suggest one batch run
  when a lot have piled up or before they test something where Rory's voice matters.
- Not a substitute for a speech-language pathologist; keep that note in the grown-ups screen.
