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
    await page.mouse.click(640, 400);
    await new Promise((r) => setTimeout(r, 4000));
    console.log('The app started.');
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
console.log(`Console errors: ${errors}, crashes: ${crashes}, failed checks: ${failedChecks}`);
process.exit(started && crashes === 0 && failedChecks === 0 ? 0 : 1);
