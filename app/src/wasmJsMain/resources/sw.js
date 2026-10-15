// Talk & Learn service worker.
// Network first, so the app is always the latest version when online; falls back to the
// cached copy when offline. CI replaces BUILD_ID on every deploy so new versions take over.
const CACHE = 'talkandlearn-BUILD_ID';
// Version-named files (the .wasm) live here across versions; old ones are tidied up below.
const STATIC = 'talkandlearn-static';

// Every file of this build (CI fills this in), so the whole app works offline after one visit.
const PRECACHE = PRECACHE_LIST;

self.addEventListener('install', (e) => {
    self.skipWaiting();
    e.waitUntil(Promise.all([caches.open(CACHE), caches.open(STATIC)]).then(([c, s]) =>
        Promise.all(['./'].concat(PRECACHE).map((f) =>
            // One file failing (e.g. a flaky connection) doesn't stop the rest being saved.
            (f.endsWith('.wasm') ? s : c).add(f).catch(() => {})
        ))
    ));
});

self.addEventListener('activate', (e) => {
    e.waitUntil(
        caches.keys()
            .then((keys) => Promise.all(keys.filter((k) => k !== CACHE && k !== STATIC).map((k) => caches.delete(k))))
            // Keep only the .wasm files this version uses.
            .then(() => fetch('./talkandlearn.js').then((r) => r.text()).then((js) => caches.open(STATIC).then((c) =>
                c.keys().then((reqs) => Promise.all(reqs.filter((r) => !js.includes(new URL(r.url).pathname.split('/').pop())).map((r) => c.delete(r))))
            )).catch(() => {}))
            .then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (e) => {
    const req = e.request;
    const url = new URL(req.url);
    if (req.method !== 'GET' || url.origin !== self.location.origin) return;
    // The big .wasm files have a new name for every version and never change, so use the saved
    // copy straight away (only fetch when it's a new version). Saves ~12 MB on every launch.
    if (url.pathname.endsWith('.wasm')) {
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
