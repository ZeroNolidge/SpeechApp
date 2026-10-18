#!/usr/bin/env python3
"""
Makes Rory's voice clips: one small mp3 per spoken line, with OpenAI text-to-speech.

Run it on your own computer (never in the app, never in GitHub Actions):

    export OPENAI_API_KEY=sk-...        (or put OPENAI_API_KEY=sk-... in a file called .env here;
                                         .env is in .gitignore, so it never goes to GitHub)
    python3 tools/make_voice.py --dry-run     # see how many lines and roughly what it costs
    python3 tools/make_voice.py               # make the clips (only new or changed lines)

Then commit and push the new files in app/src/wasmJsMain/resources/audio/. The web app finds
them through audio/manifest.json and plays them instead of the browser voice; anything without
a clip (his name, your own words) still uses the browser voice. The offline cache picks up every
file in the build, so the clips play without internet.

Letter sounds ("ah", "fff", ...) are easy for a voice to get wrong:

    python3 tools/make_voice.py --list-letters          # lists them (and writes audio/letters.html
                                                        # so you can listen to each one)
    python3 tools/make_voice.py --redo fff --say "ffffff"   # remake one, sending different text
                                                            # to the voice (kept in
                                                            # tools/voice_overrides.json)

Other options: --voice nova|shimmer|coral|..., --model gpt-4o-mini-tts|tts-1, --force (remake all).

How lines are found: every string in the app's shared code (app/src/commonMain) is read, the
parts of a line that change (a name, a number, a word) are cut out, and each fixed piece becomes
its own clip; the changing parts get clips of their own (letters, numbers 0-20, friends, visitors,
lands and Pip's words are all strings in the code too). The app plays the pieces back to back.
The text is matched exactly as in logic/ClipText.kt: keep the two in step.
"""

import argparse
import concurrent.futures
import hashlib
import http.client
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app", "src", "commonMain", "kotlin")
OUT = os.path.join(ROOT, "app", "src", "wasmJsMain", "resources", "audio")
OVERRIDES = os.path.join(ROOT, "tools", "voice_overrides.json")
WHOLE_LINES = os.path.join(ROOT, "tools", "voice_whole_lines.txt")
CONTENT = os.path.join(SRC, "com", "zeronolidge", "speechapp", "data", "Content.kt")

INSTRUCTIONS = (
    "You are Rory, a warm, friendly, playful paper robot talking to a four-year-old who is "
    "learning to talk. Speak clearly, a little slowly, gently and cheerfully, at a normal speaking "
    "volume: never shout, never whisper, even for single sounds. Never rushed."
)

# ---------------------------------------------------------------- the same rules as ClipText.kt


def normalize(s):
    s = s.lower().replace("\u2019", "'")
    s = re.sub(r"[^a-z0-9' ]", " ", s)
    s = re.sub(r" +", " ", s).strip()
    return s.strip("'").strip()


def phrases(text):
    return [p.strip() for p in re.split(r"[.!?:;\u2026]+", text) if normalize(p)]


def clip_file(key, audio=None):
    """A clip's file name. A remade clip also carries a fingerprint of its sound, so tablets that
    saved the old one for offline use fetch the new one (they never re-check a saved clip)."""
    name = hashlib.sha1(key.encode("utf-8")).hexdigest()[:16]
    if audio is not None:
        name += "-" + hashlib.sha1(audio).hexdigest()[:6]
    return name + ".mp3"


# ---------------------------------------------------------------- reading the strings in the code


def string_pieces(code):
    """Yields (line_text, [fixed pieces]) for every "..." string, cut at ${...} and $name."""
    i, n = 0, len(code)
    line_start = 0
    while i < n:
        c = code[i]
        if c == "\n":
            line_start = i + 1
            i += 1
        elif code.startswith("//", i):
            j = code.find("\n", i)
            i = n if j < 0 else j
        elif code.startswith("/*", i):
            j = code.find("*/", i + 2)
            i = n if j < 0 else j + 2
        elif code.startswith('"""', i):
            j = code.find('"""', i + 3)
            i = n if j < 0 else j + 3
        elif c == "'":
            # A char literal like 'a' or '\n'.
            j = i + 1
            if j < n and code[j] == "\\":
                j += 2
            else:
                j += 1
            i = j + 1 if j < n and code[j] == "'" else i + 1
        elif c == '"':
            pieces, cur, j = [], [], i + 1
            while j < n and code[j] != '"':
                ch = code[j]
                if ch == "\\" and j + 1 < n:
                    esc = code[j + 1]
                    cur.append({"n": " ", "t": " "}.get(esc, esc))
                    j += 2
                elif ch == "$" and j + 1 < n and code[j + 1] == "{":
                    pieces.append("".join(cur))
                    cur = []
                    depth, j = 1, j + 2
                    while j < n and depth:
                        if code[j] == "{":
                            depth += 1
                        elif code[j] == "}":
                            depth -= 1
                        elif code[j] == '"':
                            # A string inside the template: skip it.
                            j += 1
                            while j < n and code[j] != '"':
                                j += 2 if code[j] == "\\" else 1
                        j += 1
                elif ch == "$" and j + 1 < n and (code[j + 1].isalpha() or code[j + 1] == "_"):
                    pieces.append("".join(cur))
                    cur = []
                    j += 1
                    while j < n and (code[j].isalnum() or code[j] in "_."):
                        j += 1
                else:
                    cur.append(ch)
                    j += 1
            pieces.append("".join(cur))
            line_end = code.find("\n", i)
            yield code[line_start:line_end if line_end >= 0 else n], pieces
            i = j + 1
        else:
            i += 1


SKIP_LINE = re.compile(r"println|prefs\.|putInt|getInt|getString|putString|Regex\(|BakedLayer|^\s*(import|package)\b|label = \"|js\(")
STORY_BLANK = re.compile(r"\{[A-Za-z]+\}")
NOT_SPEECH = re.compile(r"[_/@#%<>=\\{}|]|\.kt\b|^[a-z]+[A-Z]")


def collect_lines():
    """Returns {key: text to send to the voice}, in a stable order."""
    lines = {}

    def add(text):
        for p in phrases(text):
            key = normalize(p)
            if key and re.search(r"[a-z]", key) and key not in lines:
                lines[key] = p.strip(" ,")

    for d, _, files in os.walk(SRC):
        for f in sorted(files):
            if not f.endswith(".kt"):
                continue
            code = open(os.path.join(d, f), encoding="utf-8").read()
            for line, pieces in string_pieces(code):
                if SKIP_LINE.search(line):
                    continue
                # Story text has its own blanks too ({f} = the friend, {hero} = his name, ...).
                pieces = [q for piece in pieces for q in STORY_BLANK.split(piece)]
                for piece in pieces:
                    # "{f}'s kite": the 's goes with the name (see the possessive names below).
                    if piece.startswith("'s "):
                        piece = piece[3:]
                    if not piece.strip() or NOT_SPEECH.search(piece):
                        continue
                    add(piece)
                    # A name like "Millie Mouse" or "Talking Forest" is also used a word at a
                    # time ("Millie!", "Follow me to Forest!").
                    words = piece.split()
                    if 2 <= len(words) <= 3 and all(w[:1].isupper() for w in words):
                        for w in words:
                            add(w)
                        if len(words) == 2 and words[0] != "The" and all(w.isalpha() for w in words):
                            add(piece.strip() + "'s")  # "Benny Bunny's kite"
    # Story sentences with their blanks filled in, recorded whole so they don't sound stitched
    # together (made by tools/whole_lines/WholeLines.kt).
    if os.path.exists(WHOLE_LINES):
        for raw in open(WHOLE_LINES, encoding="utf-8"):
            if raw.strip():
                add(raw.strip())
    # Letter names, said on their own ("A says ah").
    for ch in "ABCDEFGHIJKLMNOPQRSTUVWXYZ":
        lines.setdefault(normalize(ch), ch)
    return dict(sorted(lines.items()))


def letter_sound_lines():
    """The letter-sound lines from data/Content.kt: LetterInfo sounds and each speech sound's model."""
    code = open(CONTENT, encoding="utf-8").read()
    out = []
    for m in re.finditer(r"LetterInfo\('(\w)', '\w', \"([^\"]+)\"", code):
        out.append((m.group(1), m.group(2)))
    for m in re.finditer(r"SpeechSound\(\s*\"[^\"]+\", \"([^\"]+)\", \"[^\"]+\", \"([^\"]+)\"", code):
        out.append((m.group(1), m.group(2)))
    return out


# ---------------------------------------------------------------- talking to OpenAI


def api_key():
    key = os.environ.get("OPENAI_API_KEY", "").strip()
    env = os.path.join(ROOT, ".env")
    if not key and os.path.exists(env):
        for raw in open(env, encoding="utf-8"):
            if raw.strip().startswith("OPENAI_API_KEY="):
                key = raw.split("=", 1)[1].strip().strip('"').strip("'")
    if not key:
        # Ask for it (typed or pasted, not shown on screen, never saved anywhere).
        import getpass
        print("Paste your OpenAI API key and press Enter. (Nothing shows while you paste; that's normal.")
        print("In the Windows Command Prompt, paste with a right-click.)")
        key = getpass.getpass("Key: ")
        # Keep only the characters a key can have (Ctrl+V in some windows adds a hidden character).
        key = "".join(ch for ch in key if ch.isalnum() or ch in "-_")
    if not key:
        sys.exit("No key given. Set OPENAI_API_KEY, put it in a .env file here, or paste it when asked.")
    return key


class SpeakError(Exception):
    def __init__(self, code, detail):
        super().__init__("%s %s" % (code, detail))
        self.code, self.detail = code, detail


def speak_once(key, text, voice, model):
    body = {"model": model, "voice": voice, "input": text, "response_format": "mp3"}
    if model.startswith("gpt-4o"):
        body["instructions"] = INSTRUCTIONS
    else:
        body["speed"] = 0.95
    req = urllib.request.Request(
        "https://api.openai.com/v1/audio/speech",
        data=json.dumps(body).encode("utf-8"),
        headers={"Authorization": "Bearer " + key, "Content-Type": "application/json"},
    )
    for attempt in range(6):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                return r.read()
        except urllib.error.HTTPError as e:
            if e.code in (429, 500, 502, 503, 504) and attempt < 5:
                time.sleep(2 ** attempt)
                continue
            detail = e.read().decode("utf-8", "replace")
            try:
                detail = json.loads(detail)["error"]["message"]
            except Exception:
                pass
            raise SpeakError(e.code, detail.strip() or e.reason)
        except urllib.error.URLError as e:
            if attempt < 5:
                time.sleep(2 ** attempt)
                continue
            raise NetError(str(e.reason))
        except (OSError, http.client.HTTPException) as e:
            # The connection dropped while the clip was downloading (Wi-Fi blip, timeout): try again.
            if attempt < 5:
                time.sleep(2 ** attempt)
                continue
            raise NetError("%s: %s" % (type(e).__name__, e))


class NetError(Exception):
    pass


HINTS = {
    401: "The key wasn't accepted. Copy it again from platform.openai.com (API keys) and paste the whole key.",
    429: "OpenAI says there's no credit or too many requests. Add a little credit under Settings, Billing, then run it again.",
    403: "This OpenAI account isn't allowed to use that voice model yet.",
}


def speak(key, text, voice, state):
    """Makes one clip. If OpenAI turns down the newer model, switches the whole run to tts-1."""
    try:
        return speak_once(key, text, voice, state["model"])
    except SpeakError as e:
        if state["model"] != "tts-1" and e.code in (400, 403, 404):
            with state["lock"]:
                if state["model"] != "tts-1":
                    print("OpenAI turned down %s (%s: %s). Switching to tts-1, the same voices." % (state["model"], e.code, e.detail))
                    state["model"] = "tts-1"
            return speak(key, text, voice, state)
        raise SystemExit("OpenAI said %s for %r: %s\n%s" % (e.code, text, e.detail, HINTS.get(e.code, "")))


# ---------------------------------------------------------------- checking a clip has sound

MIN_SOUND = 0.2  # seconds of real sound a clip needs; less is a silent or broken clip


def sound_seconds(data):
    """How many seconds of an MP3 carry real sound, read from the file itself (no extra software).

    OpenAI's clips are MPEG-2 Layer III, mono. Every frame stores a global gain: speech frames
    sit at about 130 or more, quiet ones about 120 or less, and digital silence at 210 with no
    sound data. (Checked against the decoded audio of every clip: the same ten silent ones.)
    """
    i = 10 + ((data[6] << 21) | (data[7] << 14) | (data[8] << 7) | data[9]) if data[:3] == b"ID3" else 0
    loud = 0.0
    while i + 13 <= len(data):
        b1, b2, b3 = data[i + 1], data[i + 2], data[i + 3]
        ver, layer, bi, si = (b1 >> 3) & 3, (b1 >> 1) & 3, b2 >> 4, (b2 >> 2) & 3
        if data[i] != 0xFF or (b1 & 0xE0) != 0xE0 or layer != 1 or bi in (0, 15) or si == 3 or ver == 1:
            i += 1
            continue
        mpeg1 = ver == 3
        rate = ([44100, 48000, 32000] if mpeg1 else [22050, 24000, 16000] if ver == 2 else [11025, 12000, 8000])[si]
        kbps = ([0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320] if mpeg1 else
                [0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160])[bi]
        mono = (b3 >> 6) == 3
        side = data[i + 4 + (0 if b1 & 1 else 2):][:17]
        bits, n = int.from_bytes(side, "big"), len(side) * 8
        # Skip main_data_begin and private bits (and scfsi for MPEG-1), then part2_3_length and
        # big_values, to reach the first granule's global gain.
        pos = (9 + (5 if mono else 3) + (4 if mono else 8)) if mpeg1 else (8 + (1 if mono else 2))
        gain = (bits >> (n - (pos + 21) - 8)) & 0xFF
        if 128 <= gain < 210:
            loud += (1152 if mpeg1 else 576) / rate
        i += (144 if mpeg1 else 72) * kbps * 1000 // rate + ((b2 >> 1) & 1)
    return loud


def clip_ok(data):
    return sound_seconds(data) >= MIN_SOUND


class SilentClip(Exception):
    pass


# ---------------------------------------------------------------- main


def write_letters_page(manifest):
    rows = []
    for name, sound in letter_sound_lines():
        key = normalize(sound)
        f = manifest.get(key)
        player = '<audio controls preload="none" src="%s"></audio>' % f if f else "(no clip yet)"
        rows.append("<tr><td>%s</td><td>%s</td><td>%s</td><td><code>--redo &quot;%s&quot;</code></td></tr>" % (name, sound, player, key))
    html = (
        "<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width'>"
        "<title>Letter sounds</title><style>body{font-family:sans-serif;margin:16px}td{padding:6px 10px}</style>"
        "<h1>Letter sounds</h1><p>Listen to each one. To remake one that sounds wrong, run "
        "<code>python3 tools/make_voice.py --redo \"fff\" --say \"ffffff\"</code> (the --say text is what the voice reads).</p>"
        "<table>%s</table>" % "".join(rows)
    )
    open(os.path.join(OUT, "letters.html"), "w", encoding="utf-8").write(html)


def main():
    ap = argparse.ArgumentParser(description="Make Rory's voice clips with OpenAI text-to-speech.")
    ap.add_argument("--voice", default=None, help="OpenAI voice (default nova, or the one used last time)")
    ap.add_argument("--model", default=None, help="gpt-4o-mini-tts (default) or tts-1")
    ap.add_argument("--dry-run", action="store_true", help="only list the lines and the rough cost")
    ap.add_argument("--force", action="store_true", help="remake every clip")
    ap.add_argument("--list-letters", action="store_true", help="list the letter-sound lines")
    ap.add_argument("--redo", action="append", default=[], help="remake this line (its text, e.g. fff)")
    ap.add_argument("--say", default=None, help="with one --redo: the text the voice should read instead")
    ap.add_argument("--check", action="store_true", help="only list clips that are silent or too short")
    args = ap.parse_args()

    os.makedirs(OUT, exist_ok=True)
    manifest_path = os.path.join(OUT, "manifest.json")
    old = json.load(open(manifest_path, encoding="utf-8")) if os.path.exists(manifest_path) else {}
    voice = args.voice or old.get("voice") or "nova"
    model = args.model or old.get("model") or "gpt-4o-mini-tts"
    overrides = json.load(open(OVERRIDES, encoding="utf-8")) if os.path.exists(OVERRIDES) else {}

    lines = collect_lines()
    # Which file holds each line: the last run's choice if that file is still there.
    old_clips = old.get("clips", {})
    clips = {k: old_clips[k] if k in old_clips and os.path.exists(os.path.join(OUT, old_clips[k])) else clip_file(k) for k in lines}

    if args.list_letters:
        print("Letter-sound lines (letter, line, clip file):")
        for name, sound in letter_sound_lines():
            k = normalize(sound)
            have = os.path.exists(os.path.join(OUT, clips.get(k, clip_file(k))))
            print("  %-3s %-22r %s%s" % (name, sound, clips.get(k, clip_file(k)), "" if have else "  (not made yet)"))
        write_letters_page(clips)
        print("Listen to them all in audio/letters.html (open it from the app's audio folder, or on the site).")
        return

    if args.say:
        if len(args.redo) != 1:
            sys.exit("--say goes with exactly one --redo.")
        overrides[normalize(args.redo[0])] = args.say
        json.dump(overrides, open(OVERRIDES, "w", encoding="utf-8"), indent=1, sort_keys=True)

    # Silent or too-short clips (OpenAI sometimes sends back silence) are always remade.
    bad = sorted(k for k in lines if os.path.exists(os.path.join(OUT, clips[k]))
                 and not clip_ok(open(os.path.join(OUT, clips[k]), "rb").read()))
    if bad:
        print("%d clips are silent or under %.1f s of sound, so they'll be remade: %s" % (len(bad), MIN_SOUND, ", ".join(bad)))
    if args.check:
        if not bad:
            print("All %d clips have sound." % len(lines))
        return

    redo = {normalize(r) for r in args.redo}
    for r in redo:
        if r not in lines:
            sys.exit("No spoken line %r. Try --list-letters, or --dry-run to see all lines." % r)
    changed_voice = old and (old.get("voice") != voice or old.get("model") != model)
    todo = [k for k in lines if args.force or changed_voice or k in redo or k in bad or not os.path.exists(os.path.join(OUT, clips[k]))]
    if args.redo and not args.force:
        todo = [k for k in todo if k in redo or k in bad]

    chars = sum(len(overrides.get(k, lines[k])) for k in todo)
    print("%d spoken lines, %d to make (%d characters, about $%.2f with %s)." % (len(lines), len(todo), chars, chars / 1e6 * (15 if model == "tts-1" else 12), model))
    if args.dry_run:
        with open(os.path.join(ROOT, "tools", "voice_lines.txt"), "w", encoding="utf-8") as f:
            for k in lines:
                f.write("%s\t%s\t%s\n" % (clips[k], k, overrides.get(k, lines[k])))
        print("All lines are listed in tools/voice_lines.txt (file, key, text sent to the voice).")
        return

    key = api_key()
    done = 0
    import threading
    state = {"model": model, "lock": threading.Lock()}

    def make(k):
        text = overrides.get(k, lines[k])
        # Check it has sound; try again if not, the last time with tts-1 (the newer model has
        # sent back silence for some single words and long sounds like "thhhh").
        for attempt in range(3):
            audio = speak(key, text, voice, state if attempt < 2 else dict(state, model="tts-1"))
            if clip_ok(audio):
                break
            print("  (%r came back silent, trying again)" % k)
        else:
            # Still silent: leave it out, so the app uses the browser voice for it.
            old_file = os.path.join(OUT, clips[k])
            if os.path.exists(old_file) and not clip_ok(open(old_file, "rb").read()):
                os.remove(old_file)
            raise SilentClip(k)
        remake = os.path.exists(os.path.join(OUT, clips[k]))
        name = clip_file(k, audio) if remake else clip_file(k)
        path = os.path.join(OUT, name)
        # Write to a temporary name first, so a stopped run never leaves half a clip behind.
        with open(path + ".part", "wb") as f:
            f.write(audio)
        os.replace(path + ".part", path)
        clips[k] = name
        return k

    print("Working... making %d clips. %s" % (len(todo), "Each one shows when it's done." if len(todo) <= 30 else "A count shows every 10 clips (about every 10-20 seconds)."))
    skipped = []
    still_silent = []

    def save_manifest():
        # Only clips that really exist go in the manifest, so a stopped run still leaves a working set.
        made = {k: f for k, f in clips.items() if os.path.exists(os.path.join(OUT, f))}
        # Clips trimmed and evened out by tools/finish_clips.py stay marked as finished while
        # their file is unchanged; remade ones get finished again next time.
        finished = sorted(k for k in old.get("finished", []) if k in made and made[k] == old_clips.get(k))
        faded = sorted(k for k in old.get("faded", []) if k in finished)
        json.dump({"voice": voice, "model": state["model"], "clips": made, "finished": finished, "faded": faded},
                  open(manifest_path, "w", encoding="utf-8"), indent=0, sort_keys=True)
        write_letters_page(made)

    try:
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
            futures = {pool.submit(make, k): k for k in todo}
            for fut in concurrent.futures.as_completed(futures):
                try:
                    fut.result()
                except SilentClip as e:
                    still_silent.append(str(e))
                    print("  (%r came back silent 3 times; the app will use the browser voice for it)" % str(e))
                except BaseException as e:
                    if not isinstance(e, NetError):
                        # A real stop (key refused, no credit, Ctrl+C): don't start the rest.
                        for f in futures:
                            f.cancel()
                        raise
                    skipped.append(futures[fut])
                    print("  (skipped one line for now, the connection dropped: %s)" % e)
                    if len(skipped) >= 20:
                        for f in futures:
                            f.cancel()
                        raise SystemExit("The internet connection keeps dropping. Check it, then run the same command again; it carries on where it stopped.")
                done += 1
                if len(todo) <= 30 or done % 10 == 0 or done == len(todo):
                    print("  %d / %d" % (done, len(todo)))
    finally:
        save_manifest()

    # Clips for lines that are gone from the app are removed, so the offline cache stays small.
    keep = set(clips.values()) | {"manifest.json", "letters.html"}
    for f in os.listdir(OUT):
        if f.endswith(".mp3") and f not in keep:
            os.remove(os.path.join(OUT, f))
    if still_silent:
        print("These came back silent every time: %s. Try --redo with --say, e.g. --redo \"%s\" --say \"%s, %s\"."
              % (", ".join(still_silent), still_silent[0], still_silent[0], still_silent[0]))
    if skipped:
        print("%d lines were skipped because the connection dropped. Run the same command again to make just those." % len(skipped))
        return
    print("Done. Commit and push app/src/wasmJsMain/resources/audio/ (and tools/voice_overrides.json if you used --say).")


if __name__ == "__main__":
    main()
