# Talk & Learn

An Android adventure game for a ~5-year-old that teaches speech, letters, early reading, numbers,
patterns and Cubetto-style coding. Your child picks a cartoon hero, explores a world map,
and helps friends in randomly generated story quests, levelling up for new powers and outfits.
Everything is spoken, so they don't need to read. Mistakes just fade away with a gentle
"try again".

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
3. **Go on quests.** Every quest is a new, randomly built story: a cartoon friend (Benny Bunny,
   Pip the Penguin, Daisy Dino…) has a problem, such as a lost kite, a birthday party or a treasure map.
   Your child solves 3–5 challenges along the trail to help. Every quest also includes some
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
- **🎤 My turn**: speech recognition, scored generously
- **👍 Said it!**: a grown-up gives the star

The recognizer finds young voices hard, so it also gives a star for effort after three tries.

## Installing on a phone or tablet

1. On the phone, open **https://github.com/ZeroNolidge/SpeechApp/releases/latest** and tap
   **TalkAndLearn.apk** under Assets.
2. Open the downloaded file. If Android asks, allow your browser (or My Files) to
   **install unknown apps**.
3. If **Google Play Protect** says it's from an unknown developer, tap **More details →
   Install anyway**. It warns about every app that doesn't come from the Play Store.
4. If it says **"App not installed"** or **"conflicts with an existing package"**, uninstall the
   old Talk & Learn first, then install again.

Requirements: Android 8.0 or newer. Talking uses the device's text-to-speech voice. If it's
silent, install or update **Speech Services by Google** from the Play Store. Speech
recognition also uses Google's service, and the offline English pack makes it faster.

## Building it yourself

Open the project in Android Studio and press Run, or from a terminal:

```
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest  # logic tests (stories, levels, robot, patterns, math, speech matching)
```

## Changing the content

All words, sounds, tips and pictures (emoji) are in
`app/src/main/java/com/zeronolidge/speechapp/data/Content.kt`. Story friends, problems and lines
are in `logic/Story.kt`. Heroes, outfits, powers, lands and the level curve are in `logic/Rpg.kt`.
The cartoon drawing code is in `ui/art/Critter.kt`. For example, to practice
more words for a sound your child's speech therapist is working on, add a `Word("...", "emoji")`
to that sound's list.

> This app is for practice and fun. It doesn't replace a speech-language pathologist.
