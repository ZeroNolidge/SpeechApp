# Talk & Learn

An Android learning app for a ~5-year-old: speech practice, letters, early reading, numbers,
patterns and a Cubetto-style coding robot. Every instruction is spoken, so the child doesn't
need to read. Buttons are big, mistakes fade away with a gentle "try again", and every right
answer earns a star with a small celebration.

## What's inside

| Area | Activities |
|---|---|
| 🗣️ **Talk** | 14 speech sounds (P, B, M, T, D, K, G, F, S, L, R, Sh, Ch, Th), 6 picture words each. **🔊 Hear it**, **🐢 Slowly** (syllable by syllable), **👄 How?** (where the lips and tongue go), **🎤 My turn** (speech recognition, scored generously), **🦜 Copy me** (records 3 seconds and plays it back), **👍 Said it!** (a grown-up gives a star). |
| 🔤 **Letters** | ABC explorer (letter name, letter sound and a picture word). Match big → small and small → Big. "First sound" game. Look-alike letters (b/d/p/q) get mixed in once the basics are known. |
| 📖 **Read** | **Sound it out**: tap each letter to hear its sound, blend the sounds with 🐢, then pick the matching picture. **Which word?**: read the word that goes with the picture. |
| 🔢 **Numbers** | 1–20 number explorer, tap-to-count, "Which has more?", adding and taking away with pictures. Numbers go up to 10 as your child improves. |
| 🧩 **Patterns** | "What comes next?" (AB, AAB, ABB, ABC, AABB… harder patterns unlock over time) and "Odd one out". |
| 🤖 **Robot** | Plan a path, then press ▶️ Go. **Arrows** mode moves up, down, left and right. **Turn & go** mode uses forward, turn left and turn right like the Cubetto toy. Trees block the way. The hand-made levels come first, then random puzzles. |
| ⚙️ **Grown-ups** | Press and hold the gear on the home screen. Change the voice speed, turn the microphone on or off, see stars per area, read tips, and reset progress. |

## Installing on a phone or tablet

GitHub builds the app automatically on every push.

1. On GitHub, open the repo's **Actions** tab and click the latest green **Build APK** run.
2. Under **Artifacts**, download **SpeechApp-apk**. Unzip it to get `app-release.apk`.
3. Copy the APK to the Android device and open it. Android will ask you to allow installing
   from that source. Say yes, then install.

Requirements: Android 8.0 or newer. Talking uses the device's text-to-speech voice. If it's
silent, install or update **Speech Services by Google** from the Play Store. Speech
recognition also uses Google's service, and the offline English pack makes it faster.

## Building it yourself

Open the project in Android Studio and press Run, or from a terminal:

```
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest  # logic tests (robot, patterns, math, speech matching)
```

## Changing the content

All words, sounds, tips and pictures (emoji) are in
`app/src/main/java/com/zeronolidge/speechapp/data/Content.kt`. For example, to practice
more words for a sound your child's speech therapist is working on, add a `Word("...", "emoji")`
to that sound's list.

> This app is for practice and fun. It doesn't replace a speech-language pathologist.
