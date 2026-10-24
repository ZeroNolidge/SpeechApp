package com.zeronolidge.speechapp.audio

import kotlin.js.js

// A short burst of filtered noise sounds like paper being pressed; a quick falling tone is a pop.
// Made on the fly with Web Audio, so there are no sound files to download.
private fun jsCrinkle(): Unit = js("""(() => { try {
    const C = window.__tlAC || (window.__tlAC = new (window.AudioContext || window.webkitAudioContext)());
    if (C.state === 'suspended') C.resume();
    const n = Math.floor(C.sampleRate * 0.09), b = C.createBuffer(1, n, C.sampleRate), d = b.getChannelData(0);
    for (let i = 0; i < n; i++) { const t = i / n; d[i] = (Math.random() * 2 - 1) * (1 - t) * (1 - t) * (Math.random() < 0.3 ? 1 : 0.35); }
    const s = C.createBufferSource(); s.buffer = b;
    const f = C.createBiquadFilter(); f.type = 'bandpass'; f.frequency.value = 3200; f.Q.value = 0.8;
    const g = C.createGain(); g.gain.value = 0.16;
    s.connect(f); f.connect(g); g.connect(C.destination); s.start();
} catch (e) {} })()""")

private fun jsPop(): Unit = js("""(() => { try {
    const C = window.__tlAC || (window.__tlAC = new (window.AudioContext || window.webkitAudioContext)());
    if (C.state === 'suspended') C.resume();
    const o = C.createOscillator(), g = C.createGain(), t = C.currentTime;
    o.type = 'sine'; o.frequency.setValueAtTime(700, t); o.frequency.exponentialRampToValueAtTime(260, t + 0.12);
    g.gain.setValueAtTime(0.12, t); g.gain.exponentialRampToValueAtTime(0.001, t + 0.14);
    o.connect(g); g.connect(C.destination); o.start(t); o.stop(t + 0.15);
} catch (e) {} })()""")

// Rory's Kitchen's butter-cutting swipe (K7): a short filtered noise "tok" plus a quick low
// thump, like a wooden toy knife on a board -- shorter and lower than the paper crinkle/pop.
private fun jsChop(): Unit = js("""(() => { try {
    const C = window.__tlAC || (window.__tlAC = new (window.AudioContext || window.webkitAudioContext)());
    if (C.state === 'suspended') C.resume();
    const n = Math.floor(C.sampleRate * 0.04), b = C.createBuffer(1, n, C.sampleRate), d = b.getChannelData(0);
    for (let i = 0; i < n; i++) { const t = i / n; d[i] = (Math.random() * 2 - 1) * (1 - t); }
    const s = C.createBufferSource(); s.buffer = b;
    const f = C.createBiquadFilter(); f.type = 'bandpass'; f.frequency.value = 1400; f.Q.value = 0.9;
    const ng = C.createGain(); ng.gain.value = 0.14;
    s.connect(f); f.connect(ng); ng.connect(C.destination); s.start();
    const o = C.createOscillator(), g = C.createGain(), t = C.currentTime;
    o.type = 'sine'; o.frequency.setValueAtTime(180, t);
    g.gain.setValueAtTime(0.18, t); g.gain.exponentialRampToValueAtTime(0.001, t + 0.12);
    o.connect(g); g.connect(C.destination); o.start(t); o.stop(t + 0.12);
} catch (e) {} })()""")

class WebSfx : Sfx {
    override fun crinkle() = jsCrinkle()
    override fun pop() = jsPop()
    override fun chop() = jsChop()
}
