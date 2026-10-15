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
page.on('console', (m) => {
    if (m.type() === 'error') errors++;
    console.log(`[console.${m.type()}] ${m.text()}`);
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
            });
            await page.reload({ waitUntil: 'load' });
            await page.waitForFunction(() => !document.getElementById('loading'), { timeout: 45000 });
            await wait(3000);
            await page.mouse.click(640, 400);
            await wait(4000);
            await shot('map');
            await page.mouse.click(640, 752);
            await wait(2500);
            await shot('story');
            await wait(2000);
            await page.mouse.click(300, 520);
            await wait(3500);
            await shot('challenge');
        } catch (e) {
            console.log(`Screenshots stopped: ${e.message}`);
        }
    }
}
await browser.close();
console.log(`Console errors: ${errors}`);
process.exit(started ? 0 : 1);
