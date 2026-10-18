// Lists every story sentence that only exists once its blanks are filled in ("Benny Bunny
// found an old treasure map"), so tools/make_voice.py can record it whole instead of stitching
// "Benny Bunny" + "found an old treasure map" (which sounds choppy). Writes
// tools/voice_whole_lines.txt. Run it as a test in a scratch JVM Gradle project whose main
// sources are app/src/commonMain/kotlin/com/zeronolidge/speechapp/{logic,data} (see CLAUDE.md,
// "Building and testing"), after changing story text. Sentences with the hero's name are left
// out: his name has no clip.
import com.zeronolidge.speechapp.logic.*
import java.io.File
import kotlin.random.Random
import kotlin.test.Test

class WholeLines {
    @Test fun write() {
        val hero = "Zorbo"
        val out = sortedSetOf<String>()
        fun add(text: String) {
            for (ph in ClipText.phrases(text)) if (!ClipText.normalize(ph).contains(hero.lowercase())) out += ph.trim(' ', ',', '\'')
        }
        for (land in Land.values()) for (ch in 0..20) for (step in 0..2) for (seed in 0..4) {
            val q = StoryGen.make(land, StoryParams(heroName = hero, chapter = ch, speechStep = step, numbersStep = step, patternLevel = step), Random(seed * 1000 + ch))
            add(q.intro.text); add(q.twist.text); add(q.ending.text)
            q.beats.forEach { add(it.narration); add(it.scene.text) }
        }
        // The voice stories, with their own props (his own words have no clip anyway).
        for (st in VoiceStories.all.map { VoiceStories.ready(it, emptyList()) }) {
            add(st.setup); add(st.ending)
            st.beats.forEach { b -> add(b.problem); b.lines.forEach(::add); add(b.success) }
        }
        // The wake-up scene (ui/screens/Chapter.kt WakeScene).
        for (f in Friends.all) add("It's ${f.callName}! ${f.callName} is awake!")
        File(System.getenv("WHOLE_LINES_OUT") ?: "tools/voice_whole_lines.txt").writeText(out.joinToString("\n", postfix = "\n"))
    }
}
