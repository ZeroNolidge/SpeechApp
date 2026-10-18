package com.zeronolidge.speechapp.audio

import kotlin.js.JsAny
import kotlin.js.js

// ---------------------------------------------------------------- talking (Web Speech synthesis)

private fun jsSpeakOne(text: String, rate: Double, onEnd: (Boolean) -> Unit): Unit = js(
    """(() => {
        const s = window.speechSynthesis;
        if (!s) { onEnd(false); return; }
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
        let done = false, started = false;
        const fin = (ok) => {
            if (done) return;
            done = true;
            window.__tlUtterances = window.__tlUtterances.filter(x => x !== u);
            onEnd(ok);
        };
        u.onstart = () => { started = true; };
        u.onend = () => fin(true);
        u.onerror = (e) => fin(started || (e && e.error === 'interrupted'));
        // If the voice hasn't started within a couple of seconds, it isn't going to: say so now
        // instead of making everyone wait.
        window.__tlLatest = u;
        setTimeout(() => {
            if (started || done) return;
            // Only cancel if nothing newer has started speaking since.
            if (window.__tlLatest === u) { try { s.cancel(); } catch (e) {} }
            fin(false);
        }, 2500);
        // Chrome can leave the synthesizer paused (e.g. after switching tabs); wake it first.
        try { s.resume(); } catch (e) {}
        s.speak(u);
    })()"""
)

private fun jsCancelSpeech(): Unit = js("(() => { if (window.speechSynthesis) window.speechSynthesis.cancel(); })()")

/** The browser's own voice. [QueueSpeaker] handles the queue and time limits on top. */
class WebVoiceEngine : VoiceEngine {
    override fun speakOne(text: String, rate: Float, onEnd: (Boolean) -> Unit) =
        jsSpeakOne(text, rate.toDouble().coerceIn(0.3, 2.0), onEnd)

    override fun cancel() = jsCancelSpeech()
    override fun shutdown() = cancel()
}

// ---------------------------------------------------------------- microphone loudness

private fun jsMicAvailable(): Boolean = js("!!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia && (window.AudioContext || window.webkitAudioContext))")

private fun jsMicStart(onLevel: (Double) -> Unit, onFail: () -> Unit): JsAny = js(
    """(() => {
        const st = { stopped: false, stream: null, timer: null, src: null };
        try {
            navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true } }).then(stream => {
                if (st.stopped) { stream.getTracks().forEach(t => t.stop()); return; }
                st.stream = stream;
                const C = window.AudioContext || window.webkitAudioContext;
                window.__tlAudio = window.__tlAudio || new C();
                const ctx = window.__tlAudio;
                try { ctx.resume(); } catch (e) {}
                const src = ctx.createMediaStreamSource(stream);
                const an = ctx.createAnalyser();
                an.fftSize = 1024;
                src.connect(an);
                st.src = src;
                // Word listening (WebWords.kt) hears the same stream, when a turn asked for it.
                try { if (window.__tlWords && window.__tlWords.attach) window.__tlWords.attach(ctx, src); } catch (e) {}
                const buf = new Float32Array(an.fftSize);
                st.timer = setInterval(() => {
                    an.getFloatTimeDomainData(buf);
                    let sum = 0;
                    for (let i = 0; i < buf.length; i++) sum += buf[i] * buf[i];
                    onLevel(Math.sqrt(sum / buf.length));
                }, 50);
            }).catch(() => { if (!st.stopped) onFail(); });
        } catch (e) { onFail(); }
        return st;
    })()"""
)

private fun jsMicStop(st: JsAny): Unit = js(
    """(() => {
        st.stopped = true;
        try { if (window.__tlWords && window.__tlWords.detach) window.__tlWords.detach(); } catch (e) {}
        try { if (st.timer) clearInterval(st.timer); } catch (e) {}
        try { if (st.src) st.src.disconnect(); } catch (e) {}
        try { if (st.stream) st.stream.getTracks().forEach(t => t.stop()); } catch (e) {}
    })()"""
)

/** Microphone loudness in the browser (getUserMedia and an analyser). Works on iPhone and iPad too. */
class WebMicLevels : MicLevels {
    private var current: JsAny? = null
    override val available: Boolean get() = jsMicAvailable()

    override fun start(onLevel: (Float) -> Unit, onFail: () -> Unit) {
        stop()
        current = jsMicStart({ onLevel(it.toFloat()) }, onFail)
    }

    override fun stop() {
        current?.let { jsMicStop(it) }
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
