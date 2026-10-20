// Opens the built web app in headless Chrome and checks it really starts (the "Loading…"
// message goes away). Prints every console message and page error, so a broken build shows
// up in the CI log instead of on the tablet. Exits with 1 if the app doesn't start.
//
// Usage (from a folder where puppeteer-core is installed):
//   node smoke_web.mjs http://localhost:8123/
import puppeteer from 'puppeteer-core';

const url = process.argv[2] || 'http://localhost:8123/';
const browser = await puppeteer.launch({
    executablePath: process.env.CHROME || '/usr/bin/google-chrome',
    headless: true,
    args: ['--no-sandbox', '--autoplay-policy=no-user-gesture-required', '--use-fake-ui-for-media-stream', '--use-fake-device-for-media-stream'],
});
const page = await browser.newPage();
await page.setViewport({ width: 1280, height: 800 });
let errors = 0;
// Crashes (a Kotlin exception) and failed CHECK lines fail the build: a page that stays on
// its splash still "starts" as far as the loading message is concerned.
let crashes = 0;
let failedChecks = 0;
const logs = [];
const check = (line) => { if (line.includes(': FAIL')) failedChecks++; console.log(line); };
page.on('console', (m) => {
    if (m.type() === 'error') errors++;
    if (m.type() === 'error' && /Exception|Error:/.test(m.text())) crashes++;
    logs.push(m.text());
    // Map-area positions are long; keep the log readable.
    if (!m.text().startsWith('MAPAREAS')) console.log(`[console.${m.type()}] ${m.text()}`);
});
page.on('pageerror', (e) => { errors++; crashes++; console.log(`[pageerror] ${e.stack || e.message}`); });
let clipFetches = 0;
page.on('response', (r) => { if (/\/audio\/[0-9a-f-]+\.mp3$/.test(r.url()) && r.ok()) clipFetches++; });
page.on('requestfailed', (r) => console.log(`[requestfailed] ${r.url()} ${r.failure()?.errorText}`));

await page.goto(url, { waitUntil: 'load', timeout: 60000 });
let started = false;
try {
    await page.waitForFunction(() => !document.getElementById('loading'), { timeout: 45000 });
    started = true;
} catch (e) {
    console.log('The app did not get past "Loading…" within 45 seconds.');
}
if (started) {
    // Tap the start screen once (speech needs a tap), then let the first screen settle.
    await new Promise((r) => setTimeout(r, 3000));
    // How fast the start tap answers, on a CPU slowed 4x (about a tablet): the time from the tap
    // to Rory's wave on screen (STARTFEEDBACK). It should be well under half a second.
    const stamps = {};
    const stamp = (m) => { const t = m.text(); if (t === 'STARTTAP' || t === 'STARTFEEDBACK') stamps[t] = Date.now(); };
    page.on('console', stamp);
    await page.emulateCPUThrottling(4);
    const tapAt = Date.now();
    await page.mouse.click(640, 400);
    for (let i = 0; i < 100 && !stamps.STARTFEEDBACK; i++) await new Promise((r) => setTimeout(r, 50));
    await page.emulateCPUThrottling(1);
    page.off('console', stamp);
    const fb = stamps.STARTFEEDBACK ? stamps.STARTFEEDBACK - tapAt : -1;
    console.log(`START TAP (4x slower CPU): tap seen ${stamps.STARTTAP ? stamps.STARTTAP - tapAt : -1} ms, wave on screen ${fb} ms`);
    check(`CHECK start tap answers within 1 s: ${fb >= 0 && fb < 1000 ? 'ok' : 'FAIL'}`);
    await new Promise((r) => setTimeout(r, 4000));
    console.log('The app started.');
    // Rory's voice clips: the list loads, a clip decodes as real audio, and the first screen's
    // spoken line was played from clips (not the browser voice).
    const clips = await page.evaluate(async () => {
        const st = window.__tlClips;
        const map = st && st.map;
        if (!map) return { count: 0 };
        const files = Object.values(map);
        let decoded = false;
        try {
            const b = await (await fetch('audio/' + files[0])).arrayBuffer();
            const buf = await new (window.AudioContext || window.webkitAudioContext)().decodeAudioData(b);
            decoded = buf.duration > 0.1;
        } catch (e) {}
        return { count: files.length, decoded };
    });
    if (clips.count === 0) console.log('No voice clips (audio/manifest.json missing), so the browser voice is used.');
    else check(`CHECK voice clips: ${clips.count} listed, decodes ${clips.decoded}, ${clipFetches} played on the first screen: ${clips.decoded && clipFetches > 0 ? 'ok' : 'FAIL'}`);
    // With --shots, print a few small screenshots into the log (one "SHOT name base64" line
    // each), so the art can be checked without a device.
    if (process.argv.includes('--shots')) {
        const wait = (ms) => new Promise((r) => setTimeout(r, ms));
        const shot = async (name) => {
            const full = await page.screenshot({ type: 'png', encoding: 'base64' });
            const small = await page.evaluate(async (b64) => {
                const img = new Image();
                img.src = 'data:image/png;base64,' + b64;
                await img.decode();
                const c = document.createElement('canvas');
                c.width = img.width / 2;
                c.height = img.height / 2;
                c.getContext('2d').drawImage(img, 0, 0, c.width, c.height);
                return c.toDataURL('image/jpeg', 0.6).split(',')[1];
            }, full);
            console.log(`SHOT ${name} ${small}`);
        };
        try {
            await shot('first-launch');
            // A fresh start: only the Forest reached. The stop button and the buddy should be at
            // the Forest (with its golden glow), and the Meadow still under mist.
            await page.evaluate(() => {
                localStorage.setItem('talkandlearn.micAsked', 'true');
                localStorage.setItem('talkandlearn.hero', 'KID');
                localStorage.setItem('talkandlearn.chapter', '0');
                localStorage.setItem('talkandlearn.unfoldedSeen', '0');
            });
            await page.reload({ waitUntil: 'load' });
            await page.waitForFunction(() => !document.getElementById('loading'), { timeout: 45000 });
            await wait(3000);
            await page.mouse.click(640, 400);
            await wait(4000);
            await shot('map-fresh');
            {
                const marks = [...logs].reverse().find((t) => t.startsWith('MAPMARKS'));
                const pos = {};
                if (marks) for (const part of marks.split(' ').slice(1)) {
                    const [name, xy] = part.split('@');
                    const [x, y] = xy.split(',').map(Number);
                    pos[name] = { x, y };
                }
                const d = (a, b) => Math.hypot(a.x - b.x, a.y - b.y);
                const ok = pos.STOP && pos.FOREST && pos.MEADOW && d(pos.STOP, pos.FOREST) < d(pos.STOP, pos.MEADOW);
                check(`CHECK fresh start: stop button at the Forest: ${ok ? 'ok' : 'FAIL'} (${JSON.stringify(pos.STOP)} forest ${JSON.stringify(pos.FOREST)} meadow ${JSON.stringify(pos.MEADOW)})`);
                for (const t of logs.filter((t) => t.startsWith('MAPSCENE MEADOW'))) console.log('FRESH ' + t);
                // Tap the misty Meadow: "Not yet!" must show without sound too (shaking mist, a
                // "Not yet!" chip, Rory pointing along the sparkly trail).
                const labels = [...logs].reverse().find((t) => t.startsWith('MAPAREAS'));
                const lab = {};
                if (labels) for (const part of labels.split(' ').slice(1)) {
                    const [name, xy] = part.split('@');
                    const [x, y] = xy.split(',').map(Number);
                    lab[name] = { x, y };
                }
                if (lab.MEADOW) {
                    await page.mouse.click(lab.MEADOW.x, lab.MEADOW.y);
                    await wait(900);
                    await shot('map-not-yet');
                    await wait(3000);
                }
            }
            await page.evaluate(() => {
                localStorage.setItem('talkandlearn.micAsked', 'true');
                localStorage.setItem('talkandlearn.hero', 'KID');
                // Chapter 2: the last forest stop, with Number Meadow peeking through its mist.
                localStorage.setItem('talkandlearn.chapter', '1');
                localStorage.setItem('talkandlearn.unfoldedSeen', '1');
            });
            await page.reload({ waitUntil: 'load' });
            await page.waitForFunction(() => !document.getElementById('loading'), { timeout: 45000 });
            await wait(3000);
            await page.mouse.click(640, 400);
            await wait(4000);
            await shot('map');
            // Map taps, for real: read where each area's label is drawn (the app logs it from the
            // same list it uses for taps), tap Meadow's and Forest's labels, and check what opens.
            const areas = {};
            const line = [...logs].reverse().find((t) => t.startsWith('MAPAREAS'));
            if (line) for (const part of line.split(' ').slice(1)) {
                const [name, xy] = part.split('@');
                const [x, y] = xy.split(',').map(Number);
                areas[name] = { x, y };
            }
            console.log('Map label positions: ' + JSON.stringify(areas));
            const tapArea = async (name) => {
                const a = areas[name];
                if (!a) { check(`CHECK map tap ${name}: FAIL (no position logged)`); return; }
                const before = logs.length;
                await page.mouse.click(a.x, a.y);
                await wait(3000);
                const hit = logs.slice(before).find((t) => t.startsWith('MAPTAP'));
                check(`CHECK map tap ${name}: ${hit && hit.endsWith('-> ' + name) ? 'ok' : 'FAIL'} (${hit})`);
                await shot('tap-' + name.toLowerCase());
                await page.mouse.click(54, 54); // 🏠 back to the map
                await wait(3000);
            };
            await tapArea('MEADOW');
            await tapArea('FOREST');
            // The reported bug: with the next stop at Number Meadow (chapter 3), tapping the
            // sunflower started that story. Every tap on the island now goes through one rule;
            // tap the sunflower's lower half, the gap above it and the stop button.
            await page.evaluate(() => localStorage.setItem('talkandlearn.chapter', '2'));
            await page.reload({ waitUntil: 'load' });
            await page.waitForFunction(() => !document.getElementById('loading'), { timeout: 45000 });
            await wait(3000);
            await page.mouse.click(640, 400);
            await wait(4000);
            await shot('map-meadow-stop');
            for (const t of logs.filter((t) => t.startsWith('MAPSCENE'))) console.log('AREA LIST ' + t.slice(9));
            const marksLine = [...logs].reverse().find((t) => t.startsWith('MAPMARKS'));
            const marks = {};
            if (marksLine) for (const part of marksLine.split(' ').slice(1)) {
                const [name, xy] = part.split('@');
                const [x, y] = xy.split(',').map(Number);
                marks[name] = { x, y };
            }
            console.log('Map pictures and stop: ' + JSON.stringify(marks));
            const tapAt = async (what, x, y, expect) => {
                const before = logs.length;
                await page.mouse.click(x, y);
                await wait(3000);
                const hit = logs.slice(before).find((t) => t.startsWith('MAPTAP'));
                check(`CHECK map tap ${what}: ${hit && hit.endsWith('-> ' + expect) ? 'ok' : 'FAIL'} (${hit})`);
                await page.mouse.click(54, 54); // 🏠 back to the map
                await wait(3000);
            };
            if (marks.MEADOW && marks.STOP && areas.MEADOW) {
                await tapAt('sunflower (lower half)', marks.MEADOW.x, marks.MEADOW.y + 20, 'MEADOW');
                await tapAt('Meadow gap (label to picture)', marks.MEADOW.x, Math.round((areas.MEADOW.y + marks.MEADOW.y) / 2), 'MEADOW');
                // A finished stop's star replays a story there.
                if (marks['DONE-0']) await tapAt('finished star (replay)', marks['DONE-0'].x, marks['DONE-0'].y, 'DONE-0');
                else check('CHECK map tap finished star (replay): FAIL (no DONE-0 logged)');
                await tapAt('next-stop button', marks.STOP.x, marks.STOP.y, 'STOP');
            } else {
                check('CHECK map tap sunflower: FAIL (positions not logged)');
            }
            await page.mouse.click(640, 752);
            await wait(1500);
            await shot('story-1s');
            await wait(3000);
            await shot('story');
            await wait(1000);
            await page.mouse.click(300, 520);
            await wait(3500);
            await shot('challenge');
            // Layout check: the same game at iPad sizes (both ways up) and on a narrow phone.
            // Over 700 wide the picture should be on the left and the answers on the right.
            const open = async (hash, w, h, name) => {
                await page.setViewport({ width: w, height: h });
                await page.goto(url + hash, { waitUntil: 'load' });
                await page.reload({ waitUntil: 'load' });
                await page.waitForFunction(() => !document.getElementById('loading'), { timeout: 45000 });
                await wait(3000);
                await page.mouse.click(w / 2, h / 2);
                await wait(4000);
                await shot(name);
            };
            await open('#test=ADD', 820, 1180, 'ipad-portrait-add');
            await open('#test=ADD', 1180, 820, 'ipad-landscape-add');
            await open('#test=ADD', 400, 860, 'phone-add');
            await open('#test=land-MEADOW', 1180, 820, 'meadow-games');
            await open('#test=talk', 1180, 820, 'talk-ipad-landscape');
            // Let the word play out (the fake microphone hums, so a voice is "heard"): next comes
            // "Tap the P!" with 2 or 3 letters.
            await wait(16000);
            // No microphone in the test browser, so Rory waits for the big "I said it!" button
            // (bottom-right); after the praise comes "Tap the P!".
            await page.mouse.click(1180 - 70, 820 - 60);
            await wait(9000);
            await shot('talk-letters');
            // Full size (not halved), at a Samsung tablet's landscape size, while Rory listens:
            // so the listening look can be checked properly.
            await page.setViewport({ width: 1280, height: 800 });
            await page.goto(url + '#test=talk', { waitUntil: 'load' });
            await page.reload({ waitUntil: 'load' });
            await page.waitForFunction(() => !document.getElementById('loading'), { timeout: 45000 });
            await wait(3000);
            await page.mouse.click(640, 400);
            await wait(4500);
            console.log(`SHOT tablet-listening-full ${await page.screenshot({ type: 'jpeg', quality: 80, encoding: 'base64' })}`);
            // The listening look: wait (up to 30 s) for his turn to start, then photograph it.
            const listenShot = async (hash, w, h, name) => {
                await page.setViewport({ width: w, height: h });
                await page.goto(url + hash, { waitUntil: 'load' });
                await page.reload({ waitUntil: 'load' });
                await page.waitForFunction(() => !document.getElementById('loading'), { timeout: 45000 });
                await wait(3000);
                const before = logs.length;
                await page.mouse.click(w / 2, h / 2);
                const t0 = Date.now();
                while (Date.now() - t0 < 30000 && !logs.slice(before).includes('LISTENING true')) await wait(250);
                const ok = logs.slice(before).includes('LISTENING true');
                await wait(900);
                console.log(`LISTEN ${name}: ${ok ? 'reached his turn' : 'never reached his turn in 30 s'}`);
                await shot(name);
            };
            await listenShot('#test=say', 1180, 820, 'listen-story-ipad');
            await listenShot('#test=say', 400, 860, 'listen-story-phone');
            await listenShot('#test=talk', 1180, 820, 'listen-talk-ipad');
            await listenShot('#test=pip', 1180, 820, 'listen-pip-ipad');
            await listenShot('#test=cave', 1180, 820, 'listen-cave-ipad');
            // The voice stories: each at his first turn (listening look), and the boat on a phone.
            await listenShot('#test=story-boat', 1180, 820, 'listen-story-boat');
            await listenShot('#test=story-owl', 1180, 820, 'listen-story-owl');
            await listenShot('#test=story-balloon', 1180, 820, 'listen-story-balloon');
            // Every talk turn gets the listening look: the balloon's "blow" and "down", a chapter's "climb".
            await listenShot('#test=story-balloon@3', 1180, 820, 'listen-balloon-down');
            await listenShot('#test=saybeat-climb', 1180, 820, 'listen-climb');
            await listenShot('#test=saybeat-climb', 1280, 800, 'listen-climb-tablet');
            await listenShot('#test=story-boat', 400, 860, 'listen-story-boat-phone');
            // Then "I said it!" (bottom-right): the action happens, and the next problem.
            await page.mouse.click(400 - 70, 860 - 60);
            await wait(2500);
            await shot('story-boat-phone-acted');
            await open('#test=talk', 820, 1180, 'talk-ipad-portrait');
            await open('#test=talk', 400, 860, 'talk-phone');
            await open('#test=BIG_SMALL', 820, 1180, 'letters-ipad-portrait');
            await open('#test=pip', 1180, 820, 'pip-ipad-landscape');
            await open('#test=pip', 400, 860, 'pip-phone');
            await wait(9000);
            await shot('pip-phone-called');
            await open('#test=cave', 1180, 820, 'cave-ipad-landscape');
            await open('#test=cave', 400, 860, 'cave-phone');
            await wait(12000);
            await page.mouse.click(400 - 70, 860 - 60);
            await wait(5000);
            await shot('cave-phone-lit');
            await open('#test=rest', 1180, 820, 'rest-ipad-landscape');
            await open('#test=say', 1180, 820, 'say-ipad-landscape');
            await open('#test=say', 820, 1180, 'say-ipad-portrait');
            await open('#test=say', 400, 860, 'say-phone');
            // Left alone in a story for 95 s: the page must stay put (no reload back to the
            // title screen). Prints the memory used along the way, to spot a leak.
            {
                await page.setViewport({ width: 1180, height: 820 });
                await page.goto(url + '#test=story-boat', { waitUntil: 'load' });
                await page.reload({ waitUntil: 'load' });
                await page.waitForFunction(() => !document.getElementById('loading'), { timeout: 45000 });
                await wait(3000);
                await page.mouse.click(590, 410);
                await page.evaluate(() => { window.__tlMark = 1; });
                const mem = [];
                for (let i = 0; i < 19; i++) {
                    await wait(5000);
                    mem.push(await page.evaluate(() => (performance.memory ? Math.round(performance.memory.usedJSHeapSize / 1e6) : -1)));
                }
                const kept = await page.evaluate(() => window.__tlMark === 1 && !document.getElementById('loading'));
                console.log(`IDLE story 95 s: ${kept ? 'kept its place' : 'PAGE RELOADED'}; heap MB every 5 s: ${mem.join(' ')}`);
                await shot('idle-story-95s');
            }
            // Word listening: Vosk and its model load in a real browser (it's optional, so this
            // never fails the build; it just reports).
            {
                await page.setViewport({ width: 1180, height: 820 });
                await page.goto(url + '#test=talk+words', { waitUntil: 'load' });
                await page.reload({ waitUntil: 'load' });
                await page.waitForFunction(() => !document.getElementById('loading'), { timeout: 45000 });
                await wait(3000);
                const before = logs.length;
                await page.mouse.click(590, 410);
                const t0 = Date.now();
                const seen = () => logs.slice(before).find((t) => t.startsWith('WORDS '));
                while (Date.now() - t0 < 150000 && !seen()) await wait(500);
                console.log(`WORD LISTENING: ${seen() ? seen() : 'no answer in 150 s'} (after ${Math.round((Date.now() - t0) / 1000)} s)`);
                for (const t of logs.slice(before).filter((t) => t.startsWith('WORDTRY'))) console.log(t);
            }
            // Offline: once the service worker has saved every file, switch the network off,
            // reload, and the app must still start.
            await page.setViewport({ width: 1280, height: 800 });
            await page.goto(url, { waitUntil: 'load' });
            await page.evaluate(() => navigator.serviceWorker.ready.then(() => true));
            await wait(8000);
            await page.setOfflineMode(true);
            await page.reload({ waitUntil: 'load' });
            let offline = false;
            try {
                await page.waitForFunction(() => !document.getElementById('loading'), { timeout: 45000 });
                offline = true;
            } catch (e) {}
            check(`CHECK offline start: ${offline ? 'ok' : 'FAIL'}`);
            const saved = await page.evaluate(async () => (await (await caches.open('talkandlearn-static')).keys()).filter((r) => r.url.endsWith('.mp3')).length);
            console.log(`Voice clips saved for offline: ${saved}`);
            if (offline) {
                await wait(2500);
                await page.mouse.click(640, 400);
                await wait(3000);
                await shot('offline');
            }
            await page.setOfflineMode(false);
        } catch (e) {
            console.log(`Screenshots stopped: ${e.message}`);
        }
    }
}
await browser.close();
// Words Rory's clips didn't cover (said by the browser voice instead, so the voice changes).
const gaps = {};
for (const t of logs.filter((t) => t.startsWith('VOICEGAP '))) { const g = t.slice(9, t.indexOf('] in:') + 1); gaps[g] = (gaps[g] || 0) + 1; }
console.log(`VOICE GAPS: ${Object.keys(gaps).length} different`);
for (const [g, n] of Object.entries(gaps).sort((a, b) => b[1] - a[1])) console.log(`VOICE GAP ${n}x ${g}  e.g. ${logs.find((t) => t.startsWith('VOICEGAP ' + g)).slice(10 + g.length)}`);
console.log(`Console errors: ${errors}, crashes: ${crashes}, failed checks: ${failedChecks}`);
process.exit(started && crashes === 0 && failedChecks === 0 ? 0 : 1);
