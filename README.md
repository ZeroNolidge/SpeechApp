# Talk & Learn

An Android adventure game for a ~5-year-old that teaches speech, letters, early reading, numbers,
patterns and Cubetto-style coding. Your child picks a cartoon hero, explores a world map,
and helps friends in randomly generated story quests, levelling up for new powers and outfits.
Everything is spoken, so they don't need to read. Mistakes just fade away with a gentle
"try again".

**Look:** a paper cut-out style. Every shape is drawn as coloured construction paper with
rough edges and soft shadows (`ui/art/Paper.kt`), there's paper grain over the whole app, and
text uses the rounded Sniglet font (SIL Open Font License, see `FONT_LICENSE_Sniglet_OFL.txt`).

## How it plays

1. **Pick a hero.** Kid, puppy, kitty, dino, unicorn, robot or bunny. They're all cartoon
   characters drawn by the app, so they bounce, blink and cheer.
2. **Explore the world map.** Each land teaches one thing:

   | Land | Subject | Opens with |
   |---|---|---|
   | 🌳 Talking Forest | Speech sounds | Open from the start |
   | 🌻 Number Meadow | Counting, more/less, adding, taking away | Open from the start |
   | 🏝️ Letter Lagoon | Capital and small letters, first sounds | 🏊 Swim (level 2) |
   | 🏭 Robot Factory | Cubetto-style "plan, then Go" robot puzzles | 🦘 Super jump (level 3) |
   | ⛰️ Puzzle Peaks | Patterns and odd one out | 🧗 Climb (level 4) |
   | 🕯️ Word Caves | Sounding out and reading words | 🏮 Lantern (level 5) |
   | 🏰 Cloud Castle | A bit of everything | 🦸 Fly (level 6) |

   **⭐ Adventure!** on the map starts a surprise quest that mixes subjects.
3. **Go on quests.** Each quest is one of 8 illustrated stories, with a random cartoon friend
   (Benny Bunny, Pip the Penguin, Daisy Dino…) and random challenges: the lost kite, the
   birthday party, the treasure map, missing Teddy, the fallen star, the lost key, the picnic
   and the runaway balloon. Every story has a beginning, 3–5 obstacles drawn as scenes
   (a river, a broken bridge, a locked door, a tall tree, a dark cave…), a twist where someone
   new turns up, and a happy ending. Solving a challenge changes the picture: the stepping
   stones appear, the bridge is fixed, the door swings open. Every quest also includes some
   talking practice.
4. **Level up.** Challenges give XP and 💎 gems, with a bonus for getting it right first time.
   Levelling up teaches new powers:
   - **Story powers** (Swim, Jump, Climb, Lantern, Fly) open new lands and show up in the story.
   - **Helper powers** can each be used once per challenge: 🦉 Owl repeats the question slowly,
     🪄 Magic wand removes a wrong answer, 🗺️ Treasure map shows the robot's next move.
5. **Dress up.** Spend gems in 🎒 Dress up on hats, capes, glasses, colors and pet sidekicks.
   Some items are free gifts at certain levels. You can switch heroes there too.

**🎯 Practice** on the map has every activity as endless free play: speech sounds with 🦜
record-and-playback, the ABC and 1–20 explorers, and all the challenge types.

**⚙️ Grown-ups**: press and hold the gear on the map. You can:
- set your child's name, so the story says it
- pick the **speech sounds to focus on**, which stories will then use
- open every land without waiting for levels
- change the voice speed or turn off the microphone
- see progress and reset everything

### The speech practice

Each word has these buttons:
- **🔊 Hear it**
- **🐢 Slowly**: syllable by syllable
- **👄 How?**: where the lips and tongue go
- **🎤 Microphone**: switches on by itself after "Your turn!" (with a big pulsing mic so the child knows to talk), and scores generously. Tap it to stop or start listening again
- **👍 Said it!**: a grown-up gives the star

The recognizer finds young voices hard, so it also gives a star for effort after three tries.

## Getting the app

**The web app is the main version.** It always has the latest changes:

**https://zeronolidge.github.io/SpeechApp/**

1. Open the link in **Chrome** on the tablet. Wait for the green ▶ and tap it.
2. Chrome menu (⋮) → **Add to Home screen** (or **Install app**). It then opens full-screen
   like a normal app, works offline after the first visit, and updates itself whenever a new
   version is published.
3. The first time it listens, Chrome asks to use the microphone: tap **Allow**.

Progress is saved in the browser on that device. The web and Android versions keep separate
progress.

### Android APK (optional)

GitHub also builds an APK on every push (Actions → latest **Build** run → **SpeechApp-apk**,
or the `latest-build` release). If Android says **"App not installed"**, uninstall the old
copy first. Builds are signed with a permanent key, so normally a new APK installs over the
old one and keeps progress.

### Signing key

CI signs builds with a permanent key stored in two repository secrets, `SIGNING_KEYSTORE`
(the base64-encoded PKCS12 keystore, alias `talkandlearn`) and `SIGNING_PASSWORD`. Without
them, builds use a throwaway debug key, and each new build has to be uninstalled and
reinstalled.

Requirements: a recent Chrome (web app) or Android 8.0+ (APK). Talking uses the device's
text-to-speech voice; if it's silent, install or update **Speech Services by Google** from the
Play Store. In the browser, speech recognition works best in Chrome and usually needs internet.

## How it's built

One Kotlin codebase (Compose Multiplatform) makes both apps:

- `app/src/commonMain`: almost everything (game logic, stories, art, screens).
- `app/src/androidMain`: Android speech, microphone, recorder, storage, paper edges.
- `app/src/wasmJsMain`: the same for the browser (Web Speech, MediaRecorder, localStorage),
  plus the PWA files (`resources/index.html`, `manifest.webmanifest`, `sw.js`, icons).

```
./gradlew :app:testDebugUnitTest            # logic tests (stories, levels, robot, patterns, math, speech)
./gradlew :app:assembleDebug                # Android APK
./gradlew :app:wasmJsBrowserDistribution    # web app -> app/build/dist/wasmJs/productionExecutable
```

GitHub Actions builds both on every push and publishes the web app to GitHub Pages.

## Changing the content

All words, sounds, tips and pictures (emoji) are in
`app/src/commonMain/kotlin/com/zeronolidge/speechapp/data/Content.kt`. Story plots and scenes are in `logic/Plots.kt`, and friends and challenge lines
are in `logic/Story.kt`. The scene pictures are drawn in `ui/art/SetPieces.kt`. Heroes, outfits, powers, lands and the level curve are in `logic/Rpg.kt`.
The cartoon drawing code is in `ui/art/Critter.kt`. For example, to practice
more words for a sound your child's speech therapist is working on, add a `Word("...", "emoji")`
to that sound's list.

> This app is for practice and fun. It doesn't replace a speech-language pathologist.
