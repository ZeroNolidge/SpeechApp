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
CONTENT = os.path.join(SRC, "com", "zeronolidge", "speechapp", "data", "Content.kt")

INSTRUCTIONS = (
    "You are Rory, a warm, friendly, playful paper robot talking to a four-year-old who is "
    "learning to talk. Speak clearly, a little slowly, gently and cheerfully. Never rushed."
)

# ---------------------------------------------------------------- the same rules as ClipText.kt


def normalize(s):
    s = s.lower().replace("\u2019", "'")
    s = re.sub(r"[^a-z0-9' ]", " ", s)
    s = re.sub(r" +", " ", s).strip()
    return s.strip("'").strip()


def phrases(text):
    return [p.strip() for p in re.split(r"[.!?:;\u2026]+", text) if normalize(p)]


def clip_file(key):
    return hashlib.sha1(key.encode("utf-8")).hexdigest()[:16] + ".mp3"


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
                for piece in pieces:
                    if not piece.strip() or NOT_SPEECH.search(piece):
                        continue
                    add(piece)
                    # A name like "Millie Mouse" or "Talking Forest" is also used a word at a
                    # time ("Millie!", "Follow me to Forest!").
                    words = piece.split()
                    if 2 <= len(words) <= 3 and all(w[:1].isupper() for w in words):
                        for w in words:
                            add(w)
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
        key = getpass.getpass("Paste your OpenAI API key (it won't show, and isn't saved), then press Enter: ").strip()
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
            raise SystemExit("Couldn't reach OpenAI (%s). Check the internet connection and run it again." % e.reason)


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
    args = ap.parse_args()

    os.makedirs(OUT, exist_ok=True)
    manifest_path = os.path.join(OUT, "manifest.json")
    old = json.load(open(manifest_path, encoding="utf-8")) if os.path.exists(manifest_path) else {}
    voice = args.voice or old.get("voice") or "nova"
    model = args.model or old.get("model") or "gpt-4o-mini-tts"
    overrides = json.load(open(OVERRIDES, encoding="utf-8")) if os.path.exists(OVERRIDES) else {}

    lines = collect_lines()
    clips = {k: clip_file(k) for k in lines}

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

    redo = {normalize(r) for r in args.redo}
    for r in redo:
        if r not in lines:
            sys.exit("No spoken line %r. Try --list-letters, or --dry-run to see all lines." % r)
    changed_voice = old and (old.get("voice") != voice or old.get("model") != model)
    todo = [k for k in lines if args.force or changed_voice or k in redo or not os.path.exists(os.path.join(OUT, clips[k]))]
    if args.redo and not args.force:
        todo = [k for k in todo if k in redo]

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
        audio = speak(key, overrides.get(k, lines[k]), voice, state)
        with open(os.path.join(OUT, clips[k]), "wb") as f:
            f.write(audio)
        return k

    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        for _ in pool.map(make, todo):
            done += 1
            if done % 25 == 0 or done == len(todo):
                print("  %d / %d" % (done, len(todo)))

    # Clips for lines that are gone from the app are removed, so the offline cache stays small.
    keep = set(clips.values()) | {"manifest.json", "letters.html"}
    for f in os.listdir(OUT):
        if f.endswith(".mp3") and f not in keep:
            os.remove(os.path.join(OUT, f))
    json.dump({"voice": voice, "model": state["model"], "clips": clips}, open(manifest_path, "w", encoding="utf-8"), indent=0, sort_keys=True)
    write_letters_page(clips)
    print("Done. Commit and push app/src/wasmJsMain/resources/audio/ (and tools/voice_overrides.json if you used --say).")


if __name__ == "__main__":
    main()
