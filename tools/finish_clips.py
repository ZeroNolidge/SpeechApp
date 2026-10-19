#!/usr/bin/env python3
"""
Finishes Rory's voice clips (run by Claude in its container, or anywhere with ffmpeg; the owner
doesn't need it): for every clip in app/src/wasmJsMain/resources/audio/manifest.json that isn't
finished yet, it

  - trims the quiet at the start to 0.03 s and at the end to 0.3 s (some clips had up to 2 s),
  - evens out the loudness, so every clip is at a normal speaking level (a whispery "thhhh" comes
    up; nothing shouts), with the peaks kept safe,
  - saves it under a new name (the old name plus "-f1"), since the app keeps clips for good once
    downloaded and only fetches a clip again when its name changes,

then updates manifest.json and removes the old files. Finished clips are listed in the manifest
("finished"), so running it again only touches new ones (from the owner's next batch).

    python3 tools/finish_clips.py            # finish new clips
    python3 tools/finish_clips.py --check    # only list clips that are quiet or have long tails
"""
import argparse
import json
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "wasmJsMain", "resources", "audio")
RATE = 24000
LEAD = 0.03
TAIL = 0.3
TARGET_RMS = 0.1   # a normal speaking level for these clips (their median before finishing)
MAX_PEAK = 0.9
SOUND = 0.02       # anything above this counts as sound when trimming
SUFFIX = "-f1"     # change this if the finishing changes, so tablets fetch the new clips
# Long held sounds start and stop abruptly; they get a short fade in and out (seconds).
FADE_KEYS = {"thhhh", "thhhhh", "sssss", "ssss", "fffff", "shhhhh", "mmmmm", "rrrrr", "rrrr"}
# Fade in only (the hum starts softly) and fade out only (the buzz ends softly).
FADE_IN_KEYS = {"mmm", "nnn", "lll"}
FADE_OUT_KEYS = {"zzz"}
FADE_IN, FADE_OUT = 0.06, 0.12
FADE_SUFFIX = "-fd"


def decode(path):
    import numpy as np
    raw = subprocess.run(["ffmpeg", "-v", "quiet", "-i", path, "-f", "s16le", "-ac", "1", "-ar", str(RATE), "-"],
                         capture_output=True, check=True).stdout
    return np.frombuffer(raw, np.int16).astype(np.float32) / 32768


def encode(samples, path):
    import numpy as np
    pcm = (np.clip(samples, -1, 1) * 32767).astype(np.int16).tobytes()
    subprocess.run(["ffmpeg", "-v", "quiet", "-y", "-f", "s16le", "-ar", str(RATE), "-ac", "1", "-i", "-",
                    "-codec:a", "libmp3lame", "-b:a", "64k", path], input=pcm, check=True)


def measure(x):
    import numpy as np
    idx = np.where(np.abs(x) > SOUND)[0]
    if len(idx) == 0:
        return None
    seg = x[idx[0]:idx[-1] + 1]
    return idx[0], idx[-1], float(np.sqrt((seg ** 2).mean())), float(np.abs(seg).max())


def finish(x):
    m = measure(x)
    if m is None:
        return None
    s, e, rms, peak = m
    start = max(0, s - int(LEAD * RATE))
    end = min(len(x), e + int(TAIL * RATE))
    gain = min(TARGET_RMS / max(rms, 1e-6), MAX_PEAK / max(peak, 1e-6))
    return x[start:end] * gain


def fade(x, fade_in=True, fade_out=True):
    import numpy as np
    n_in, n_out = int(FADE_IN * RATE), int(FADE_OUT * RATE)
    y = x.copy()
    if len(y) > n_in + n_out:
        if fade_in:
            # Fade in from where the sound starts (the clip keeps a short silent lead).
            m = measure(y)
            s = m[0] if m else 0
            y[:s] = 0
            y[s:s + n_in] *= np.linspace(0, 1, len(y[s:s + n_in]))
        if fade_out:
            # Fade out over the end of the sound, not the silent tail after it.
            m = measure(y)
            end = m[1] + 1 if m else len(y)
            a = max(0, end - n_out)
            y[a:end] *= np.linspace(1, 0, end - a)
            y[end:] = 0
    return y


def fade_new(clips, done_fades):
    """Fades the long held sounds that haven't been faded yet (renamed, so tablets fetch them)."""
    for k in sorted((FADE_KEYS | FADE_IN_KEYS | FADE_OUT_KEYS) & set(clips)):
        if k in done_fades:
            continue
        old = clips[k]
        y = fade(decode(os.path.join(OUT, old)), fade_in=k not in FADE_OUT_KEYS, fade_out=k not in FADE_IN_KEYS)
        new = old[:-4] + FADE_SUFFIX + ".mp3"
        encode(y, os.path.join(OUT, new))
        os.remove(os.path.join(OUT, old))
        clips[k] = new
        done_fades.add(k)
        print("  faded: %s" % k)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true", help="only list quiet clips and long tails")
    args = ap.parse_args()
    path = os.path.join(OUT, "manifest.json")
    manifest = json.load(open(path, encoding="utf-8"))
    clips = manifest["clips"]
    done = set(manifest.get("finished", []))
    todo = [k for k in sorted(clips) if k not in done]
    if args.check:
        for k in sorted(clips):
            x = decode(os.path.join(OUT, clips[k]))
            m = measure(x)
            if m is None:
                print("silent:", k)
                continue
            s, e, rms, _ = m
            tail = (len(x) - e) / RATE
            if rms < 0.04 or tail > TAIL + 0.15:
                print("%-30s loudness %.3f, quiet at the end %.2f s" % (k[:30], rms, tail))
        return
    print("Finishing %d clips..." % len(todo))
    for n, k in enumerate(todo, 1):
        old = clips[k]
        y = finish(decode(os.path.join(OUT, old)))
        if y is None:
            print("  (silent, left as it is: %s)" % k)
            continue
        base = old[:-4]
        new = (base if base.endswith(SUFFIX) else base + SUFFIX) + ".mp3"
        encode(y, os.path.join(OUT, new))
        if new != old:
            os.remove(os.path.join(OUT, old))
        clips[k] = new
        done.add(k)
        if n % 100 == 0:
            print("  %d / %d" % (n, len(todo)))
    manifest["finished"] = sorted(done)
    fades = set(k for k in manifest.get("faded", []) if k in done)
    fade_new(clips, fades)
    manifest["faded"] = sorted(fades)
    json.dump(manifest, open(path, "w", encoding="utf-8"), indent=0, sort_keys=True)
    print("Done.")


if __name__ == "__main__":
    sys.exit(main())
