// Talk & Learn service worker.
// Network first, so the app is always the latest version when online; falls back to the
// cached copy when offline. CI replaces BUILD_ID on every deploy so new versions take over.
const CACHE = 'talkandlearn-BUILD_ID';
// Files whose name changes whenever their content does (the .wasm, Rory's voice clips) live here
// across versions, so they're only downloaded once; old ones are tidied up below.
const STATIC = 'talkandlearn-static';

// Every file of this build (CI fills this in), so the whole app works offline after one visit.
const PRECACHE = PRECACHE_LIST;

const lasting = (path) => path.endsWith('.wasm') || /audio\/[0-9a-f-]+\.mp3$/.test(path);

self.addEventListener('install', (e) => {
    self.skipWaiting();
    e.waitUntil(Promise.all([caches.open(CACHE), caches.open(STATIC)]).then(([c, s]) =>
        Promise.all(['./'].concat(PRECACHE).map((f) =>
            // One file failing (e.g. a flaky connection) doesn't stop the rest being saved.
            // Long-lived files already saved by an earlier version aren't downloaded again.
            lasting(f) ? s.match(f).then((hit) => hit || s.add(f)).catch(() => {}) : c.add(f).catch(() => {})
        ))
    ));
});

self.addEventListener('activate', (e) => {
    e.waitUntil(
        caches.keys()
            .then((keys) => Promise.all(keys.filter((k) => k !== CACHE && k !== STATIC).map((k) => caches.delete(k))))
            // Keep only the .wasm files and voice clips this version uses.
            .then(() => Promise.all([
                fetch('./talkandlearn.js').then((r) => r.text()),
                fetch('./audio/manifest.json').then((r) => r.ok ? r.text() : '').catch(() => ''),
            ]).then(([js, clips]) => caches.open(STATIC).then((c) =>
                c.keys().then((reqs) => Promise.all(reqs.filter((r) => {
                    const name = new URL(r.url).pathname.split('/').pop();
                    return !(name.endsWith('.mp3') ? clips : js).includes(name);
                }).map((r) => c.delete(r))))
            )).catch(() => {}))
            .then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (e) => {
    const req = e.request;
    const url = new URL(req.url);
    if (req.method !== 'GET' || url.origin !== self.location.origin) return;
    // The big .wasm files and the voice clips have a new name whenever they change, so use the
    // saved copy straight away (only fetch new ones). Saves ~12 MB of app and ~35 MB of clips per launch.
    if (lasting(url.pathname)) {
        e.respondWith(
            caches.match(req).then((hit) => hit || fetch(req).then((res) => {
                if (res.ok) {
                    const copy = res.clone();
                    caches.open(STATIC).then((c) => c.put(req, copy));
                }
                return res;
            }))
        );
        return;
    }
    e.respondWith(
        fetch(req)
            .then((res) => {
                if (res.ok) {
                    const copy = res.clone();
                    caches.open(CACHE).then((c) => c.put(req, copy));
                }
                return res;
            })
            .catch(() => caches.match(req).then((hit) => hit || caches.match('index.html')))
    );
});
