package com.zeronolidge.speechapp.audio

import kotlin.js.JsAny
import kotlin.js.js

// ---------------------------------------------------------------- talking (Web Speech synthesis)

private fun jsSpeakOne(text: String, rate: Double, onEnd: () -> Unit): Unit = js(
    """(() => {
        const s = window.speechSynthesis;
        if (!s) { onEnd(); return; }
        const u = new SpeechSynthesisUtterance(text);
        u.rate = rate; u.pitch = 1.1; u.lang = 'en-US';
        const vs = s.getVoices();
        const v = vs.find(v => v.lang === 'en-US' && /Google|Samantha|Natural/.test(v.name))
            || vs.find(v => v.lang && v.lang.startsWith('en-US'))
            || vs.find(v => v.lang && v.lang.startsWith('en'));
        if (v) u.voice = v;
        // Keep a reference so the browser doesn't drop the utterance (and its callbacks) early.
        window.__tlUtterances = window.__tlUtterances || [];
        window.__tlUtterances.push(u);
        let done = false;
        const fin = () => {
            if (done) return;
            done = true;
            window.__tlUtterances = window.__tlUtterances.filter(x => x !== u);
            onEnd();
        };
        u.onend = fin; u.onerror = fin;
        // Chrome can leave the synthesizer paused (e.g. after switching tabs); wake it first.
        try { s.resume(); } catch (e) {}
        s.speak(u);
    })()"""
)

private fun jsCancelSpeech(): Unit = js("(() => { if (window.speechSynthesis) window.speechSynthesis.cancel(); })()")

/** The browser's own voice. [QueueSpeaker] handles the queue and time limits on top. */
class WebVoiceEngine : VoiceEngine {
    override fun speakOne(text: String, rate: Float, onEnd: () -> Unit) =
        jsSpeakOne(text, rate.toDouble().coerceIn(0.3, 2.0), onEnd)

    override fun cancel() = jsCancelSpeech()
    override fun shutdown() = cancel()
}

// ---------------------------------------------------------------- listening (Web Speech recognition)

private fun jsRecognitionAvailable(): Boolean = js("!!(window.SpeechRecognition || window.webkitSpeechRecognition)")

private fun jsListen(hint: String, onResult: (String) -> Unit): JsAny = js(
    """(() => {
        const R = window.SpeechRecognition || window.webkitSpeechRecognition;
        const r = new R();
        r.lang = 'en-US'; r.interimResults = false; r.maxAlternatives = 5; r.continuous = false;
        let sent = false;
        const send = (s) => { if (!sent) { sent = true; onResult(s); } };
        r.onresult = (e) => {
            const res = e.results[0];
            const out = [];
            for (let i = 0; i < res.length; i++) out.push(res[i].transcript);
            send(out.join('\n'));
        };
        r.onerror = () => send('');
        r.onend = () => send('');
        try { r.start(); } catch (e) { send(''); }
        return r;
    })()"""
)

private fun jsStopListening(r: JsAny): Unit = js(
    "(() => { try { r.onresult = null; r.onerror = null; r.onend = null; r.abort(); } catch (e) {} })()"
)

/** Listening in the browser (Chrome on Android supports this; it usually needs internet). */
class WebListener : Listener {
    private var current: JsAny? = null
    override val available: Boolean get() = jsRecognitionAvailable()

    override fun listen(hint: String, onLevel: (Float) -> Unit, onResult: (List<String>) -> Unit) {
        cancel()
        current = jsListen(hint) { joined ->
            current = null
            onResult(joined.split('\n').map { it.trim() }.filter { it.isNotEmpty() })
        }
    }

    override fun cancel() {
        current?.let { jsStopListening(it) }
        current = null
    }
}

// ---------------------------------------------------------------- the parrot (MediaRecorder)

private fun jsRecordThenPlay(seconds: Int, onPlaying: () -> Unit, onDone: () -> Unit): JsAny = js(
    """(() => {
        const st = { stopped: false, rec: null, audio: null, stream: null };
        if (!navigator.mediaDevices || !window.MediaRecorder) { onDone(); return st; }
        navigator.mediaDevices.getUserMedia({ audio: true }).then(stream => {
            if (st.stopped) { stream.getTracks().forEach(t => t.stop()); return; }
            st.stream = stream;
            const chunks = [];
            const rec = new MediaRecorder(stream);
            st.rec = rec;
            rec.ondataavailable = e => chunks.push(e.data);
            rec.onstop = () => {
                stream.getTracks().forEach(t => t.stop());
                if (st.stopped) return;
                const url = URL.createObjectURL(new Blob(chunks, { type: rec.mimeType }));
                const a = new Audio(url);
                st.audio = a;
                a.onended = () => onDone();
                a.onerror = () => onDone();
                a.play().then(() => onPlaying()).catch(() => onDone());
            };
            rec.start();
            setTimeout(() => { if (rec.state !== 'inactive') rec.stop(); }, seconds * 1000);
        }).catch(() => onDone());
        return st;
    })()"""
)

private fun jsStopParrot(st: JsAny): Unit = js(
    """(() => {
        st.stopped = true;
        try { if (st.rec && st.rec.state !== 'inactive') st.rec.stop(); } catch (e) {}
        try { if (st.audio) st.audio.pause(); } catch (e) {}
        try { if (st.stream) st.stream.getTracks().forEach(t => t.stop()); } catch (e) {}
    })()"""
)

class WebParrot : Parrot {
    private var current: JsAny? = null

    override fun recordThenPlay(seconds: Int, onPlaying: () -> Unit, onDone: () -> Unit) {
        stop()
        current = jsRecordThenPlay(seconds, onPlaying) {
            current = null
            onDone()
        }
    }

    override fun stop() {
        current?.let { jsStopParrot(it) }
        current = null
    }
}
