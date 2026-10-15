# Talk & Learn

An adventure game for a ~5-year-old early reader that teaches speech, letters, early reading,
numbers, coins, patterns and Cubetto-style coding. Your child picks a cartoon buddy and follows a
paper trail across an island, one short story chapter at a time. Everything is spoken, so they
never need to read. Mistakes just fold away with a gentle "try again".

**Look:** a paper cut-out style. Anything your child can tap is a thick, raised paper cut-out
that presses down under a finger; the scenery behind is flatter and softer, so it's obvious what
to tap. Text uses the rounded Sniglet font (SIL Open Font License, see `FONT_LICENSE_Sniglet_OFL.txt`).

**Same on every screen:** 🏠 top-left goes back to the map, and the big 🔊 top-right says the
instructions again. Every screen speaks when it opens, and there are never more than 3 or 4 big
choices at a time. No ads, links, purchases, timers, lives, streaks or buzzers.

## How it plays

1. **Pick a buddy.** Kid, puppy, kitty, dino, unicorn, robot or bunny.
2. **Follow the trail.** The map has 16 stops across 8 places. Tap the big yellow button where
   the hand points. Each stop is a 3 to 5 minute story chapter that ends with "see you next
   time"; then the next stop unfolds. After the last stop the trail starts a new lap. Nothing
   ever locks or goes backwards.

   | Place | What's new there |
   |---|---|
   | 🌳 Talking Forest | Speaking |
   | 🌻 Number Meadow | Counting, more/less, adding, taking away |
   | 🏝️ Letter Lagoon | Capital and small letters, first sounds |
   | 🏪 Market Town | Paying with coins |
   | ⛰️ Puzzle Peaks | Patterns (fix the rope bridge) |
   | 🏭 Robot Factory | "Plan, then Go" robot puzzles |
   | 🕯️ Word Caves | Sounding out and reading words |
   | 🏰 Cloud Castle | A bit of everything |

3. **Story chapters.** Each chapter is one of 8 illustrated stories with a random friend (Benny
   Bunny, Pip the Penguin…). Obstacles are drawn as scenes: a river, a broken bridge, a locked
   door. Solving a challenge changes the picture. Before the twist, someone is fast asleep, and
   your child wakes them by calling their name.
4. **Talking makes the story happen.** Saying "open" opens the door, and "jump" gets everyone
   across the river. Each word is played slowly with its picture, then "3, 2, 1, your turn!" and
   the microphone listens for about 4 seconds. It only listens for a voice, not the exact word, so
   it works with young voices and on iPhone and iPad. Voice heard: a celebration and a shiny
   sticker. Nothing heard: "Let's say it together!", and the story carries on. No microphone:
   Rory says "Say it out loud to me!" and waits for a tap. The ▶ button always skips.
5. **Gems and buddy pieces.** Every word your child tries to say puts a gem in the backpack (for
   trying, not for being right). A full backpack gives the buddy a new paper piece (party hat,
   fairy wings, hero cape, a pet…), and there's a "welcome back" present on the first visit of a
   new day. No levels or scores. Try pieces on in the closet (tap your buddy, top-left of the map).
   Wrong answers are gentle: a wiggle and "try again", then the right answer glows, then it's shown.
6. **Sticker book.** Every word practised becomes a sticker in 📒. Shiny stickers are words the
   microphone heard clearly.

**🎯 Practice** on the map has every activity as free play.

**⚙️ Grown-ups**: tap the small gear, bottom-left of the map, then tap the three numbers written
on the screen. You can:
- see which words your child has tried, grouped as going well, still tricky and just started (no scores)
- add up to 3 of your own words, such as family names or favourite things, with a picture
- set your child's name, pick speech sounds to focus on, change the voice speed, turn off the microphone
- make a **backup code** (and restore it) so a cleared browser or a new tablet doesn't start over
- reset everything

If there's no microphone (or permission is denied), everything still works: the word is played,
your child says it, then taps the green arrow.

## Getting the app

**The web app is the main version.** It always has the latest changes:

**https://zeronolidge.github.io/SpeechApp/**

1. Open the link in **Chrome** on the tablet. Wait for the green ▶ and tap it.
2. Chrome menu (⋮) → **Add to Home screen** (or **Install app**). It then opens full-screen
   like a normal app, works offline after the first visit, and updates itself whenever a new
   version is published.
3. The first time it listens, Chrome asks to use the microphone: tap **Allow**.

Progress is saved in the browser on that device. The web and Android versions keep separate
progress. To move progress to another device (or keep it safe if the browser's data is cleared),
use **Back up progress** on the grown-ups page.

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

All words, phrases, sounds, tips and pictures (emoji) are in
`app/src/commonMain/kotlin/com/zeronolidge/speechapp/data/Content.kt`. Story plots, scenes and
the "story words" (like "open" for a door) are in `logic/Plots.kt`, and friends and challenge
lines are in `logic/Story.kt`. The scene pictures are drawn in `ui/art/SetPieces.kt`. Buddies,
paper pieces, lands and the trail are in `logic/Rpg.kt`. The easiest way to add a word for your
child is the grown-ups page.
The cartoon drawing code is in `ui/art/Critter.kt`. For example, to practice
more words for a sound your child's speech therapist is working on, add a `Word("...", "emoji")`
to that sound's list.

> This app is for practice and fun. It doesn't replace a speech-language pathologist.
