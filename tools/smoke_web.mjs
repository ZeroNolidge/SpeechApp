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
const logs = [];
page.on('console', (m) => {
    if (m.type() === 'error') errors++;
    logs.push(m.text());
    // Map-area positions are long; keep the log readable.
    if (!m.text().startsWith('MAPAREAS')) console.log(`[console.${m.type()}] ${m.text()}`);
});
page.on('pageerror', (e) => { errors++; console.log(`[pageerror] ${e.stack || e.message}`); });
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
                if (!a) { console.log(`CHECK map tap ${name}: FAIL (no position logged)`); return; }
                const before = logs.length;
                await page.mouse.click(a.x, a.y);
                await wait(3000);
                const hit = logs.slice(before).find((t) => t.startsWith('MAPTAP'));
                console.log(`CHECK map tap ${name}: ${hit && hit.endsWith('-> ' + name) ? 'ok' : 'FAIL'} (${hit})`);
                await shot('tap-' + name.toLowerCase());
                await page.mouse.click(54, 54); // 🏠 back to the map
                await wait(3000);
            };
            await tapArea('MEADOW');
            await tapArea('FOREST');
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
            await open('#test=talk', 820, 1180, 'talk-ipad-portrait');
            await open('#test=talk', 400, 860, 'talk-phone');
            await open('#test=BIG_SMALL', 820, 1180, 'letters-ipad-portrait');
            await open('#test=pip', 1180, 820, 'pip-ipad-landscape');
            await open('#test=pip', 400, 860, 'pip-phone');
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
            console.log(`CHECK offline start: ${offline ? 'ok' : 'FAIL'}`);
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
console.log(`Console errors: ${errors}`);
process.exit(started ? 0 : 1);
