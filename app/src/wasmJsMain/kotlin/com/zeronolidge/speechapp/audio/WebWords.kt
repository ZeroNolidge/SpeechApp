package com.zeronolidge.speechapp.audio

/**
 * Word recognition in the browser with Vosk (vosk-browser, a WebAssembly build, and the small
 * English model, about 40 MB). CI puts both in the site's vosk/ folder; they're downloaded once,
 * on first use (the script into its own cache, "talkandlearn-vosk"; the model unpacked by Vosk
 * into the device's storage, by its fixed address), so it works offline after that, and never
 * blocks anything: if loading fails, word listening just stays off.
 *
 * Each talk turn listens for a tiny word list (a grammar) on the same microphone stream as the
 * loudness check (WebAudio.kt hands its stream to window.__tlWords). Nothing is recorded: the
 * audio goes straight into the recogniser and only the picked words and their confidence come out.
 */
private const val VOSK_JS = "vosk/vosk-0.0.8.js"
private const val VOSK_MODEL = "vosk/model-small-en-us-0.15.tar.gz"

private fun jsWordsInstall(js: String, model: String, onStatus: (String) -> Unit): Unit = js(
    """(() => {
        if (window.__tlWords) return;
        const W = window.__tlWords = { status: 'loading', model: null, grammar: null, rec: null, node: null, heard: [], wanted: false, finishCb: null };
        const CACHE = 'talkandlearn-vosk';
        const abs = (p) => new URL(p, document.baseURI).href;
        // From the device's own cache, or downloaded once and kept there.
        const getBlob = (p) => caches.open(CACHE).then(c => c.match(abs(p)).then(hit => hit || fetch(abs(p)).then(r => {
            if (!r.ok) throw new Error('missing ' + p);
            return c.put(abs(p), r.clone()).then(() => r);
        }))).then(r => r.blob());
        // An old version's script is tidied away.
        caches.open(CACHE).then(c => c.keys().then(ks => ks.forEach(k => { if (k.url !== abs(js)) c.delete(k); }))).catch(() => {});
        const fail = (e) => { W.status = 'unavailable'; onStatus('unavailable ' + (e && e.message ? e.message : e)); };
        try {
            getBlob(js).then(b => new Promise((res, rej) => {
                const s = document.createElement('script');
                s.src = URL.createObjectURL(b);
                s.onload = res; s.onerror = () => rej(new Error('script'));
                document.head.appendChild(s);
            // The model: Vosk downloads it once and keeps it unpacked in the device's storage
            // (IndexedDB, by this address), so later launches load it from there, offline too.
            })).then(() => window.Vosk.createModel(abs(model), -2)).then(m => {
                W.model = m; W.status = 'ready'; onStatus('ready');
            }).catch(fail);
        } catch (e) { fail(e); }

        // The microphone (WebAudio.kt) calls these with its audio context and stream source.
        W.attach = (ctx, src) => {
            if (!W.model || !W.wanted || W.rec) return;
            try {
                const rec = new W.model.KaldiRecognizer(ctx.sampleRate, JSON.stringify(W.grammar));
                rec.setWords(true);
                rec.on('result', (m) => {
                    const r = (m && m.result && m.result.result) || [];
                    r.forEach(w => W.heard.push([w.word, w.conf]));
                    if (W.finishCb) { const cb = W.finishCb; W.finishCb = null; cb(); }
                });
                const node = ctx.createScriptProcessor(4096, 1, 1);
                node.onaudioprocess = (e) => { try { rec.acceptWaveform(e.inputBuffer); } catch (err) {} };
                src.connect(node);
                node.connect(ctx.destination);
                W.rec = rec; W.node = node; W.src = src;
            } catch (e) { W.rec = null; }
        };
        W.detach = () => {
            try { if (W.node) { W.src.disconnect(W.node); W.node.disconnect(); } } catch (e) {}
            W.node = null;
        };
        W.drop = () => {
            W.detach();
            try { if (W.rec) W.rec.remove(); } catch (e) {}
            W.rec = null; W.wanted = false; W.finishCb = null;
        };
    })()"""
)

private fun jsWordsStatus(): String = js("(window.__tlWords && window.__tlWords.status) || 'off'")

private fun jsWordsBegin(grammar: String): Unit = js(
    """(() => {
        const W = window.__tlWords;
        if (!W || !W.model) return;
        W.drop();
        W.grammar = JSON.parse(grammar);
        W.heard = [];
        W.wanted = true;
    })()"""
)

private fun jsWordsFinish(onDone: (String) -> Unit): Unit = js(
    """(() => {
        const W = window.__tlWords;
        const answer = () => {
            const out = (W && W.heard ? W.heard : []).map(h => h[0] + ':' + h[1]).join(';');
            if (W) W.drop();
            onDone(out);
        };
        if (!W || !W.rec) { if (W) W.drop(); onDone('-'); return; }
        W.detach();
        // The last words come with the final result; never wait more than a second for it.
        let done = false;
        const once = () => { if (!done) { done = true; answer(); } };
        W.finishCb = once;
        setTimeout(once, 1000);
        try { W.rec.retrieveFinalResult(); } catch (e) { once(); }
    })()"""
)

private fun jsWordsCancel(): Unit = js("(() => { if (window.__tlWords) window.__tlWords.drop(); })()")

class WebWords : WordListener {
    private var started = false

    override val status: WordListener.Status
        get() = when (jsWordsStatus()) {
            "ready" -> WordListener.Status.READY
            "loading" -> WordListener.Status.LOADING
            "unavailable" -> WordListener.Status.UNAVAILABLE
            else -> WordListener.Status.OFF
        }

    override val ready: Boolean get() = status == WordListener.Status.READY

    override fun prepare() {
        if (started) return
        started = true
        runCatching { jsWordsInstall(VOSK_JS, VOSK_MODEL) { println("WORDS $it") } }
    }

    override fun begin(grammar: List<String>) {
        val json = grammar.joinToString(",", "[", "]") { "\"" + it.replace("\"", "") + "\"" }
        runCatching { jsWordsBegin(json) }
    }

    override fun finish(onHeard: (List<Pair<String, Float>>?) -> Unit) {
        runCatching {
            jsWordsFinish { s ->
                if (s == "-") onHeard(null)
                else onHeard(s.split(";").filter { ':' in it }.mapNotNull { p ->
                    val i = p.lastIndexOf(':')
                    p.substring(i + 1).toFloatOrNull()?.let { p.substring(0, i) to it }
                })
            }
        }.onFailure { onHeard(null) }
    }

    override fun cancel() {
        runCatching { jsWordsCancel() }
    }
}
