"""Moves old voice clips that nothing uses any more out of the app.

Run it on your computer (python tools/tidy_clips.py) if GitHub Desktop shows thousands of
new .mp3 files after a voice run. Those are leftovers from earlier runs. The app only uses
the clips listed in audio/manifest.json, so every other .mp3 in the audio folder is moved
(not deleted) to a folder next to the project, "SpeechApp-old-clips".
--dry-run only lists what would move.
"""
import json
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
AUDIO = os.path.join(ROOT, "app", "src", "wasmJsMain", "resources", "audio")
SPARE = os.path.join(os.path.dirname(ROOT), "SpeechApp-old-clips")


def main():
    dry = "--dry-run" in sys.argv
    manifest = json.load(open(os.path.join(AUDIO, "manifest.json"), encoding="utf-8"))
    used = set(manifest.get("clips", {}).values())
    old = sorted(f for f in os.listdir(AUDIO) if f.endswith(".mp3") and f not in used)
    missing = sorted(f for f in used if not os.path.exists(os.path.join(AUDIO, f)))
    if not old:
        print("No old clips to move.")
    elif dry:
        print("Would move %d old clips to %s" % (len(old), SPARE))
    else:
        os.makedirs(SPARE, exist_ok=True)
        for f in old:
            shutil.move(os.path.join(AUDIO, f), os.path.join(SPARE, f))
        print("Moved %d old clips to %s" % (len(old), SPARE))
    print("The app uses %d clips; %d of them are missing." % (len(used), len(missing)))


if __name__ == "__main__":
    main()
