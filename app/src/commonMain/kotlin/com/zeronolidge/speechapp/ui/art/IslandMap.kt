package com.zeronolidge.speechapp.ui.art

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.platform.LocalDensity
import com.zeronolidge.speechapp.logic.MapAreas
import com.zeronolidge.speechapp.ui.components.popIn
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.logic.MapTarget
import com.zeronolidge.speechapp.logic.MapSpot
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import com.zeronolidge.speechapp.logic.Species
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.logic.Trail
import com.zeronolidge.speechapp.ui.components.ArrowIcon
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.PaperButton
import com.zeronolidge.speechapp.ui.components.unfold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The world map as an illustrated paper island: a forest, a meadow, a lagoon, a market town,
 * mountains with a rope bridge, a factory, caves, and a castle on a floating cloud. A paper
 * trail winds between them. Finished stops wear a star sticker; the next stop is a big raised
 * paper button. Places the trail hasn't reached yet hide under drifting mist. The scenery is
 * muted so the one thing to tap stands out.
 */
@Composable
fun IslandMap(
    chapter: Int,
    reached: (Land) -> Boolean,
    /** Play the unfold animation on the current stop (it was just earned). */
    unfoldNew: Boolean,
    onUnfolded: () -> Unit,
    onStop: () -> Unit,
    onElsewhere: (Land) -> Unit,
    /** A misted land that was just tapped: it wiggles and sparkles run from it to the next stop. */
    hintFrom: Land? = null,
    hintKey: Int = 0,
    modifier: Modifier = Modifier,
    /** Drawn next to the current stop, like a "you are here" marker. */
    hero: @Composable (Modifier) -> Unit = {},
    /**
     * Drawn beside the current stop, e.g. a pointing hand; `fromLeft` says which side it sits on
     * (the side away from the buddy), so it never covers a label above the stop.
     */
    pointer: @Composable (Modifier, Dp, Boolean) -> Unit = { _, _, _ -> },
    /** The crystal cave is open (Pip was found): a glowing paper cave stop just past the Forest. */
    cave: Boolean = false,
    /** The cave has new tries waiting (just unlocked by finding Pip): it glows. */
    caveFresh: Boolean = cave,
    onCave: () -> Unit = {},
    /** The voice stories' stops (id, emoji, its land, finished), each a round paper stop by its land. */
    stories: List<MapStoryStop> = emptyList(),
    onStory: (String) -> Unit = {},
) {
    val t = rememberInfiniteTransition(label = "island")
    val slow by t.loop(0f, 1f, 24000, mode = RepeatMode.Restart, easing = LinearEasing, label = "slow")
    val mid by t.loop(0f, 1f, 9000, mode = RepeatMode.Restart, easing = LinearEasing, label = "mid")
    val fast by t.loop(0f, 1f, 2400, mode = RepeatMode.Restart, easing = LinearEasing, label = "fast")
    val flap by t.loop(0f, 1f, 350, label = "flap")
    val pulse by t.loop(1f, 1.1f, 550, label = "pulse")
    val still = LocalReduceMotion.current

    val current = Trail.stopOf(chapter)
    val lap = chapter / Trail.size
    // A land the trail has just reached: its mist clears as the new stop unfolds.
    val newLand = if (unfoldNew && Trail.opensLand(chapter)) Trail.landOf(chapter) else null
    // On a land's last stop, the next land starts to show through its mist.
    val peekLand = if (!Trail.opensLand(chapter)) Trail.lands.getOrNull(Trail.lands.indexOf(Trail.landOf(chapter)) + 1) else null
    val clearing = remember(newLand) { Animatable(if (newLand != null && !still) 0f else 1f) }
    LaunchedEffect(newLand) { if (newLand != null) clearing.animateTo(1f, tween(MaxMotionMs)) }

    val hint = remember(hintKey) { Animatable(0f) }
    LaunchedEffect(hintKey) {
        if (hintKey > 0) {
            hint.snapTo(0f)
            // Rory walks from the tapped place to the next stop along a sparkly trail, then waits
            // there a moment. (With reduce motion he simply stands at the next stop.)
            hint.animateTo(1f, tween(3200, easing = LinearEasing))
        }
    }

    // Drag the map a little and its paper layers drift at different speeds: the sea slowly,
    // the island with its stops, the clouds in front fastest. It springs back on letting go.
    val scope = rememberCoroutineScope()
    val pan = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    // The area under a finger right now (its label sinks), and the last logged label positions.
    var pressedLand by remember { mutableStateOf<Land?>(null) }
    var pressedStop by remember { mutableStateOf(false) }
    var pressedCave by remember { mutableStateOf(false) }
    var pressedStory by remember { mutableStateOf<String?>(null) }
    /** The stop just tapped wiggles (with the crinkle), so every first tap visibly lands. */
    var wiggled by remember { mutableStateOf<MapTarget?>(null) }
    val wiggleTurn = remember { Animatable(0f) }
    /** The place to go glows and sparkles until he taps it (or its stop button). */
    var guide by remember(current) { mutableStateOf(true) }
    var lastAreaLog by remember { mutableStateOf("") }
    var islandOrigin by remember { mutableStateOf(Offset.Zero) }
    val app = LocalApp.current

    BoxWithConstraints(
        modifier.pointerInput(still) {
            if (still) return@pointerInput
            val limit = minOf(size.width, size.height) * 0.05f
            detectDragGestures(
                onDragEnd = { scope.launch { pan.animateTo(Offset.Zero, tween(380)) } },
                onDragCancel = { scope.launch { pan.animateTo(Offset.Zero, tween(380)) } },
            ) { change, drag ->
                change.consume()
                val next = pan.value + drag * 0.4f
                scope.launch { pan.snapTo(Offset(next.x.coerceIn(-limit, limit), next.y.coerceIn(-limit, limit))) }
            }
        },
    ) {
        val w = maxWidth
        val h = maxHeight
        val unit = minOf(w, h)
        fun Modifier.drift(f: Float) = graphicsLayer {
            translationX = pan.value.x * f
            translationY = pan.value.y * f
        }

        // Layer 1, the sea: baked paper with live waves, the boat, fish and the castle's shadow.
        Box(Modifier.fillMaxSize().drift(0.35f)) {
            BakedLayer("map-sea", depth = 0.dp, edge = false) { ocean() }
            Canvas(Modifier.fillMaxSize()) {
                waves(mid, fast)
                boat(fast)
                fish(mid)
                castleShadow(Land.CLOUDS, mid)
            }
        }

        // Layer 2, the island and everything on it, moving together so the stops stay put on it.
        // One tap handler for every area: the tap point is matched against the shared list of
        // map areas (the same positions the labels are drawn at), and the result is logged.
        val label = (unit.value * 0.055f).coerceIn(20f, 30f)
        val onArea by rememberUpdatedState(onElsewhere)
        val onStopTap by rememberUpdatedState(onStop)
        val onCaveTap by rememberUpdatedState(onCave)
        val onStoryTap by rememberUpdatedState(onStory)
        val dens = LocalDensity.current.density
        // The next stop: same physical size on every screen, placed clear of every area's tap zone.
        val node = 96.dp
        val stopPx = with(LocalDensity.current) {
            val (fx, fy) = Trail.position(current)
            val wPx = w.toPx()
            val hPx = h.toPx()
            val labelH = (label * 1.45f + 6f).dp.toPx()
            remember(current, wPx, hPx, labelH) {
                val (x, y) = MapAreas.stopSpot(Trail.landAt(current), fx, fy, wPx, hPx, (node / 2).toPx(), 64.dp.toPx(), labelH)
                Offset(x, y)
            }
        }
        val stopDp = with(LocalDensity.current) { DpOffset(stopPx.x.toDp(), stopPx.y.toDp()) }
        // The voice stories' stops, each by its land, then the crystal cave just past the Forest:
        // all clear of every area, of the next stop and of each other.
        val storyNode = node * 0.7f
        val caveNode = node * 0.8f
        val storySpots = with(LocalDensity.current) {
            val wPx = w.toPx()
            val hPx = h.toPx()
            val labelH = (label * 1.45f + 6f).dp.toPx()
            remember(stories.map { it.id }, stopPx, wPx, hPx, labelH) {
                val placed = mutableListOf(MapSpot(MapTarget.Stop, stopPx.x, stopPx.y, (node / 2).toPx()))
                stories.forEach { st ->
                    val r = (storyNode / 2).toPx()
                    val (x, y) = MapAreas.spotBy(st.land, wPx, hPx, r, placed, 64.dp.toPx(), labelH)
                    placed += MapSpot(MapTarget.Story(st.id), x, y, r)
                }
                placed.drop(1)
            }
        }
        val cavePx = with(LocalDensity.current) {
            val wPx = w.toPx()
            val hPx = h.toPx()
            val labelH = (label * 1.45f + 6f).dp.toPx()
            remember(cave, stopPx, storySpots, wPx, hPx, labelH) {
                if (!cave) null
                else MapAreas.caveSpot(wPx, hPx, (caveNode / 2).toPx(), stopPx.x, stopPx.y, (node / 2).toPx(), 64.dp.toPx(), labelH, storySpots).let { (x, y) -> Offset(x, y) }
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .drift(1f)
                .onGloballyPositioned { c ->
                    // For checking: where each area's label is on screen (the smoke test taps these).
                    val o = c.positionInWindow()
                    val line = MapAreas.all.joinToString(" ") { a ->
                        "${a.land}@${(o.x + a.labelX * c.size.width).toInt()},${(o.y + a.labelY * c.size.height + label * dens * 0.6f).toInt()}"
                    }
                    // ...and where each area's picture and the next-stop button are.
                    val marks = MapAreas.all.joinToString(" ") { a ->
                        "${a.land}@${(o.x + a.markX * c.size.width).toInt()},${(o.y + a.markY * c.size.height).toInt()}"
                    } + " STOP@${(o.x + stopPx.x).toInt()},${(o.y + stopPx.y).toInt()}" +
                        (cavePx?.let { " CAVE@${(o.x + it.x).toInt()},${(o.y + it.y).toInt()}" } ?: "") +
                        storySpots.joinToString("") { sp -> " STORY-${(sp.target as MapTarget.Story).id}@${(o.x + sp.x).toInt()},${(o.y + sp.y).toInt()}" }
                    islandOrigin = o
                    if (line + marks != lastAreaLog) {
                        lastAreaLog = line + marks
                        println("MAPAREAS $line")
                        println("MAPMARKS $marks")
                    }
                }
                .pointerInput(stopPx, cavePx, storySpots, label) {
                    // Everything on the island goes through MapAreas.target: each area's one zone
                    // (picture, label and the gap between) and the next-stop button.
                    fun targetAt(p: Offset): MapTarget = MapAreas.target(
                        p.x, p.y, size.width.toFloat(), size.height.toFloat(),
                        labelHalfW = 64.dp.toPx(), labelH = (label * 1.45f + 6f).dp.toPx(),
                        stopX = stopPx.x, stopY = stopPx.y, stopR = (node / 2).toPx(),
                        caveX = cavePx?.x ?: 0f, caveY = cavePx?.y ?: 0f, caveR = if (cavePx != null) (caveNode / 2).toPx() else 0f,
                        spots = storySpots,
                    )
                    // A forgiving tap (young fingers wobble): it counts if the finger comes up
                    // within a finger's width of where it went down, even if dragging the map
                    // noticed the wobble first. A plain tap detector dropped those first taps.
                    val slop = 28.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val p0 = down.position
                        when (val t = targetAt(p0)) {
                            is MapTarget.Area -> pressedLand = t.land
                            MapTarget.Stop -> pressedStop = true
                            MapTarget.Cave -> pressedCave = true
                            is MapTarget.Story -> pressedStory = t.id
                            MapTarget.Sea -> {}
                        }
                        // The stop button crinkles by itself (PaperButton); everything else here too.
                        if (pressedLand != null || pressedCave || pressedStory != null) app.sfx.crinkle()
                        var up: Offset? = null
                        while (true) {
                            val e = awaitPointerEvent()
                            val c = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) {
                                if ((c.position - p0).getDistance() <= slop) up = c.position
                                c.consume()
                                break
                            }
                        }
                        pressedLand = null
                        pressedStop = false
                        pressedCave = false
                        pressedStory = null
                        val p = up ?: return@awaitEachGesture
                        run {
                            val t = targetAt(p0)
                            println("MAPTAP ${p0.x.toInt()},${p0.y.toInt()} -> ${when (t) { is MapTarget.Area -> t.land.name; MapTarget.Stop -> "STOP"; MapTarget.Cave -> "CAVE"; is MapTarget.Story -> "STORY-${t.id}"; MapTarget.Sea -> "sea" }}")
                            if (t != MapTarget.Sea && !still) {
                                wiggled = t
                                scope.launch {
                                    for (a in listOf(9f, -8f, 5f, -3f, 0f)) wiggleTurn.animateTo(a, tween(55))
                                }
                            }
                            if (t == MapTarget.Stop || (t is MapTarget.Area && t.land == Trail.landAt(current))) guide = false
                            when (t) {
                                is MapTarget.Area -> onArea(t.land)
                                MapTarget.Stop -> onStopTap()
                                MapTarget.Cave -> onCaveTap()
                                is MapTarget.Story -> onStoryTap(t.id)
                                MapTarget.Sea -> app.puff(islandOrigin.x + p.x, islandOrigin.y + p.y)
                            }
                        }
                    }
                },
        ) {
            BakedLayer(listOf("map-island", current, lap), depth = 4.dp) {
                island()
                riverBase()
                trail(current, lap)
                forest(Land.FOREST)
                meadow(Land.MEADOW)
                lagoon(Land.LAGOON)
                market(Land.MARKET)
                factory(Land.FACTORY)
                peaks(Land.PEAKS)
                caves(Land.CAVES)
            }
            Canvas(Modifier.fillMaxSize()) {
                riverFlow(fast)
                lagoonGlint(Land.LAGOON, fast)
                factorySmoke(Land.FACTORY, fast)
                caveSparkles(Land.CAVES, fast)
            }
            // The castle floats on its cloud, baked in its own small box.
            val castleBox = unit * 0.3f
            Box(
                Modifier
                    .offset(x = w * Land.CLOUDS.mapX - castleBox / 2, y = h * Land.CLOUDS.mapY - castleBox / 2)
                    .size(castleBox)
                    .graphicsLayer { translationY = sin(mid * 2 * PI).toFloat() * size.height * 0.03f },
            ) {
                BakedLayer("map-castle", depth = 4.dp) { castleArt(center, size.minDimension / 4.4f) }
            }

            // Mist over lands the trail hasn't reached, each patch baked once and drifting.
            Land.entries.forEach { land ->
                val strength = when {
                    land == newLand -> 1f - clearing.value
                    // The land the trail reaches next starts to peek through its mist.
                    land == peekLand -> 0.5f
                    !reached(land) -> 1f
                    else -> 0f
                }
                if (strength > 0.01f) {
                    val box = unit * 0.34f
                    // "Not yet!": the tapped land's mist gives a big shake for about a second.
                    val wiggle = if (!still && land == hintFrom && hint.value in 0.001f..0.3f) sin(hint.value * 110f) * 1.6f * (1f - hint.value / 0.3f) else 0f
                    Box(
                        Modifier
                            .offset(x = w * land.mapX - box / 2, y = h * land.mapY - box / 2)
                            .size(box)
                            .graphicsLayer {
                                val sway = sin(slow * 2 * PI + land.ordinal).toFloat()
                                translationX = (sway * 0.04f + wiggle * 0.05f) * size.width
                                alpha = strength
                            },
                    ) {
                        BakedLayer("map-mist", depth = 0.dp, edge = false, grain = false) { mist() }
                    }
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                if (hintFrom != null && hint.value in 0.001f..0.999f) sparklePath(at(hintFrom), Offset(stopPx.x, stopPx.y), hint.value, still)
                birds(slow, flap)
                // Where to go: a soft golden ring that breathes around the next stop's land, with
                // three sparkles going round it, until he taps it.
                if (guide) {
                    val a = MapAreas.of(Trail.landAt(current))
                    val c = Offset(a.markX * size.width, a.markY * size.height)
                    val r = size.minDimension * MapAreas.LANDMARK_RADIUS * (if (still) 1f else 0.95f + (pulse - 1f))
                    drawCircle(Color(0xFFFFE082).copy(alpha = 0.25f), r, center = c)
                    drawCircle(Color(0xFFFFD54F), r, center = c, style = Stroke(4.dp.toPx()))
                    for (i in 0 until 3) {
                        val ang = (if (still) 0f else fast * 6.2832f) + i * 2.0944f
                        star(Offset(c.x + r * kotlin.math.cos(ang), c.y + r * kotlin.math.sin(ang)), 7.dp.toPx(), Color(0xFFFFF59D))
                    }
                }
            }
            // A soft wash so the scenery sits back and the raised stop button wins.
            Canvas(Modifier.fillMaxSize()) { drawRect(PaperStyle.sceneryWash, Offset(-3000f, -3000f), Size(size.width + 6000f, size.height + 6000f)) }

            // Labels are drawn from the one shared list of map areas (logic/MapAreas.kt); taps on the
            // island are matched against the same list (see the tap handler on this layer), so a
            // label or picture always opens its own area. The pressed area's label sinks a little.
            Land.entries.forEach { land ->
                val area = MapAreas.of(land)
                val open = reached(land)
                val fade = when {
                    land == newLand -> 0.65f + 0.35f * clearing.value
                    open -> 1f
                    land == peekLand -> 0.85f
                    else -> 0.65f
                }
                Box(
                    Modifier
                        .centeredAt(w * area.labelX, h * area.labelY, w)
                        .graphicsLayer {
                            alpha = fade
                            val k = if (pressedLand == land) 0.92f else 1f
                            scaleX = k
                            scaleY = k
                        }
                        .paperCard(PaperStyle.chip, 999.dp)
                        .padding(horizontal = 10.dp, vertical = 1.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(land.emoji, fontSize = label.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            land.shortName, fontSize = (label * 0.6f).sp, maxLines = 1, softWrap = false,
                            fontWeight = FontWeight.ExtraBold, color = PaperStyle.ink,
                        )
                    }
                }
            }

            // Rory walks from a misty place to the next stop, along the sparkly trail.
            val from = hintFrom
            if (from != null && hint.value in 0.001f..0.999f) {
                val t = hint.value
                val walk = if (still) 1f else ((1 - cos((t / 0.6f).coerceAtMost(1f) * PI)) / 2).toFloat()
                val sx = stopDp.x / w
                val sy = stopDp.y / h
                val rx = from.mapX + (sx - from.mapX) * walk
                val ry = from.mapY + (sy - from.mapY) * walk
                val rh = unit * 0.16f
                val step = if (still || walk >= 1f) 0f else sin(t * 60f)
                Box(
                    Modifier
                        // He walks a little below the places' pictures, clear of their name labels.
                        .offset(x = w * rx + 52.dp, y = h * ry - rh * 0.25f)
                        .size(rh * 0.72f, rh)
                        .zIndex(3f)
                        .graphicsLayer {
                            alpha = ((1f - t) / 0.12f).coerceIn(0f, 1f)
                            rotationZ = step * 6f
                            translationY = -kotlin.math.abs(step) * size.height * 0.06f
                        },
                ) {
                    Critter(Species.ROBOT, Modifier.fillMaxSize(), Color(0xFFFFCC80), mood = Mood.CHEER, talking = true, mirrored = sx < from.mapX)
                }
                // Seen as well as heard (a muted iPad): a "Not yet!" chip with a stop hand by the
                // misty place, and Rory pointing the way along the sparkly trail.
                val fade = ((1f - t) / 0.12f).coerceIn(0f, 1f)
                if (t < 0.6f) {
                    PaperChip(
                        "✋ Not yet!",
                        Modifier
                            // Below the land's picture, clear of its name label above.
                            .offset(x = w * from.mapX - 74.dp, y = h * from.mapY + unit * 0.09f)
                            .zIndex(4f)
                            .popIn(hintKey),
                        fontSize = 22.sp,
                        maxLines = 1,
                    )
                }
                val left = sx < from.mapX
                Text(
                    if (left) "👈" else "👉",
                    fontSize = (rh.value * 0.42f).sp,
                    modifier = Modifier
                        .offset(x = w * rx + 52.dp + if (left) -rh * 0.45f else rh * 0.7f, y = h * ry)
                        .zIndex(4f)
                        .graphicsLayer { alpha = fade },
                )
            }
        }

        // Layer 3, paper clouds in front over the sea, drifting fastest. Behind every button.
        Box(Modifier.fillMaxSize().drift(1.7f)) {
            listOf(Triple(0.0f, 0.02f, 1f), Triple(0.78f, 0.86f, 0.8f), Triple(0.9f, 0.5f, 0.7f)).forEach { (x, y, k) ->
                val cw = unit * 0.24f * k
                Box(Modifier.offset(x = w * x, y = h * y).size(cw, cw * 0.5f)) {
                    BakedLayer("map-cloud", depth = 2.dp, grain = false) { frontCloud() }
                }
            }
        }

        // The stops, the buddy and the pointer ride on the island layer.
        Box(Modifier.fillMaxSize().drift(1f)) {
            // Finished stops: flat star stickers on the trail.
            val sticker = unit * 0.075f
            (0 until Trail.size).forEach { s ->
                val done = lap > 0 || s < current
                if (done && s != current) {
                    val (fx, fy) = Trail.position(s)
                    Box(
                        Modifier
                            .offset(x = w * fx - sticker / 2, y = h * fy - sticker / 2)
                            .size(sticker)
                            .rotate(((s * 37) % 21 - 10).toFloat())
                            .paperCard(Color(0xFFFFF8E1), circle = true),
                        contentAlignment = Alignment.Center,
                    ) { Text("⭐", fontSize = (sticker.value * 0.55f).sp) }
                }
            }

            // The next stop: a big raised paper button that unfolds when it is new, ringed in
            // tomato red (the colour kept for things to tap). Its taps (ring and all) are handled
            // by the island's one tap handler above; it only draws itself pressed.
            val cx = stopDp.x / w
            val cy = stopDp.y / h
            Box(
                Modifier
                    .offset(x = stopDp.x - node / 2, y = stopDp.y - node / 2)
                    .size(node)
                    .zIndex(2f)
                    .unfold(unfoldNew, onUnfolded),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(node)
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                        }
                        .border(6.dp, PaperColors.tomato, CircleShape),
                )
                PaperButton(Color(0xFFFFD54F), Modifier.size(node * 0.78f).graphicsLayer { rotationZ = if (wiggled == MapTarget.Stop) wiggleTurn.value else 0f }, circle = true, pressedBy = pressedStop, onClick = onStop) {
                    ArrowIcon(node * 0.36f, color = Color(0xFF2E7D32))
                }
            }
            // The voice stories' stops (taps through the island's one tap handler).
            storySpots.forEach { sp ->
                val id = (sp.target as MapTarget.Story).id
                val st = stories.first { it.id == id }
                val sd = with(LocalDensity.current) { DpOffset(sp.x.toDp(), sp.y.toDp()) }
                StoryStop(
                    Modifier.offset(x = sd.x - storyNode / 2, y = sd.y - storyNode / 2).size(storyNode).zIndex(2f),
                    id, st.emoji, done = st.done, pressed = pressedStory == id,
                    wiggle = { if (wiggled == MapTarget.Story(id)) wiggleTurn.value else 0f },
                )
            }
            // The crystal cave stop (its taps go through the island's one tap handler too).
            cavePx?.let { cp ->
                val cd = with(LocalDensity.current) { DpOffset(cp.x.toDp(), cp.y.toDp()) }
                CaveStop(
                    Modifier.offset(x = cd.x - caveNode / 2, y = cd.y - caveNode / 2).size(caveNode).zIndex(2f)
                        .graphicsLayer { rotationZ = if (wiggled == MapTarget.Cave) wiggleTurn.value else 0f },
                    pressed = pressedCave,
                    // It glows while it has new tries waiting (after finding Pip), and stays put after.
                    glow = { if (caveFresh) pulse else 0f },
                )
            }
            val heroOnRight = cx < 0.5f
            hero(
                Modifier
                    .offset(x = if (heroOnRight) w * cx + node * 0.55f else w * cx - node * 1.2f, y = h * cy - node * 0.6f)
                    .size(width = node * 0.7f, height = node * 0.85f)
                    .zIndex(3f),
            )
            val hand = node * 0.55f
            pointer(
                Modifier
                    .offset(
                        x = if (heroOnRight) w * cx - node / 2 - hand * 1.35f else w * cx + node / 2 + hand * 0.1f,
                        y = h * cy - hand * 0.65f,
                    )
                    .zIndex(4f),
                hand,
                heroOnRight,
            )
        }
    }
}

// ---------------------------------------------------------------- helpers

/** A voice story's stop on the map. */
data class MapStoryStop(val id: String, val emoji: String, val land: Land, val done: Boolean)

/**
 * A voice story's stop: a round raised paper badge with the story's picture, a gold edge (not
 * tomato red, which stays the next stop's ring) and a little star once he has played it.
 */
@Composable
private fun StoryStop(modifier: Modifier, id: String, emoji: String, done: Boolean, pressed: Boolean, wiggle: () -> Float) {
    Box(
        modifier.graphicsLayer {
            val s = if (pressed) 0.92f else 1f
            scaleX = s
            scaleY = s
            rotationZ = wiggle()
        },
        contentAlignment = Alignment.Center,
    ) {
        if (id == "owl") {
            // Sleepy Owl's Morning: its own paper picture (a sleepy owl in front of a rising sun on
            // a night-blue badge), so it never looks like any other owl in the game.
            Box(Modifier.fillMaxSize().paperCard(Color(0xFF283593), circle = true).border(4.dp, Color(0xFFFFCA28), CircleShape)) {
                Canvas(Modifier.fillMaxSize()) { sleepyOwlBadge() }
            }
        } else {
            Box(Modifier.fillMaxSize().paperCard(Color(0xFFFFF8E1), circle = true).border(4.dp, Color(0xFFFFCA28), CircleShape), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 30.sp)
            }
        }
        if (done) Text("⭐", fontSize = 18.sp, modifier = Modifier.align(Alignment.TopEnd))
    }
}

/** The owl story's badge art: dawn sky, a half sun rising behind a hill, a sleepy paper owl. */
private fun DrawScope.sleepyOwlBadge() {
    val w = size.width
    val h = size.height
    // The sun coming up, and a little star still out.
    // A pink-orange dawn glow low in the sky, and a big sun peeking up at the right, beside the owl.
    paperOval(Color(0xFFFF8A65), Offset(-w * 0.1f, h * 0.5f), Size(w * 1.2f, h * 0.4f))
    paperCircle(Color(0xFFFFD54F), w * 0.2f, Offset(w * 0.8f, h * 0.68f))
    paperCircle(Color(0xFFFFF59D), w * 0.035f, Offset(w * 0.24f, h * 0.24f))
    // The hill (a branch for the owl), across the bottom.
    paperOval(Color(0xFF2E7D32), Offset(-w * 0.1f, h * 0.7f), Size(w * 1.2f, h * 0.5f))
    // The owl: a round brown body with ear tufts, cream face, closed sleepy eyes and a beak.
    val cx = w * 0.5f
    val tuft = Path().apply {
        moveTo(cx - w * 0.17f, h * 0.36f); lineTo(cx - w * 0.2f, h * 0.2f); lineTo(cx - w * 0.06f, h * 0.31f); close()
        moveTo(cx + w * 0.17f, h * 0.36f); lineTo(cx + w * 0.2f, h * 0.2f); lineTo(cx + w * 0.06f, h * 0.31f); close()
    }
    paperPath(tuft, Color(0xFF6D4C41))
    paperOval(Color(0xFF8D6E63), Offset(cx - w * 0.21f, h * 0.27f), Size(w * 0.42f, h * 0.5f))
    paperOval(Color(0xFFFFE0B2), Offset(cx - w * 0.16f, h * 0.33f), Size(w * 0.32f, h * 0.2f))
    val lid = Stroke(width = w * 0.025f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawArc(Color(0xFF3E2723), 20f, 140f, false, Offset(cx - w * 0.13f, h * 0.37f), Size(w * 0.1f, h * 0.07f), style = lid)
    drawArc(Color(0xFF3E2723), 20f, 140f, false, Offset(cx + w * 0.03f, h * 0.37f), Size(w * 0.1f, h * 0.07f), style = lid)
    val beak = Path().apply { moveTo(cx - w * 0.03f, h * 0.47f); lineTo(cx + w * 0.03f, h * 0.47f); lineTo(cx, h * 0.53f); close() }
    paperPath(beak, Color(0xFFFFA000))
    // Feet on the hill.
    paperOval(Color(0xFFFFA000), Offset(cx - w * 0.11f, h * 0.74f), Size(w * 0.08f, h * 0.04f))
    paperOval(Color(0xFFFFA000), Offset(cx + w * 0.03f, h * 0.74f), Size(w * 0.08f, h * 0.04f))
}

/**
 * The crystal cave on the map: a purple paper cave with a dark mouth and gold crystals, in a
 * soft gold glow that pulses ([glow]). It bounces in when it first appears and presses down
 * while tapped. Not tomato red: the stop button stays the one red-ringed thing.
 */
@Composable
private fun CaveStop(modifier: Modifier, pressed: Boolean, glow: () -> Float) {
    val still = LocalReduceMotion.current
    val bounce = remember { Animatable(if (still) 1f else 0.3f) }
    LaunchedEffect(Unit) {
        if (!still) bounce.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = 500f))
    }
    Box(
        modifier.graphicsLayer {
            val s = bounce.value * if (pressed) 0.92f else 1f
            scaleX = s
            scaleY = s
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // The glow.
            drawCircle(Color(0xFFFFE082).copy(alpha = 0.55f), w * 0.5f * glow(), center)
            // The cave: a rounded purple hill with a dark arched mouth.
            paperOval(Color(0xFF7E57C2), Offset(w * 0.1f, h * 0.2f), Size(w * 0.8f, h * 0.75f))
            paperRect(Color(0xFF7E57C2), Offset(w * 0.1f, h * 0.58f), Size(w * 0.8f, h * 0.24f))
            paperOval(Color(0xFF311B92), Offset(w * 0.32f, h * 0.45f), Size(w * 0.36f, h * 0.5f))
            paperRect(Color(0xFF311B92), Offset(w * 0.32f, h * 0.68f), Size(w * 0.36f, h * 0.14f))
            // Gold crystals round the mouth.
            fun crystal(x: Float, y: Float, s: Float) {
                val path = Path().apply {
                    moveTo(x, y - s)
                    lineTo(x + s * 0.5f, y)
                    lineTo(x, y + s * 0.6f)
                    lineTo(x - s * 0.5f, y)
                    close()
                }
                paperPath(path, Color(0xFFFFD54F))
            }
            crystal(w * 0.25f, h * 0.42f, w * 0.13f)
            crystal(w * 0.5f, h * 0.3f, w * 0.15f)
            crystal(w * 0.75f, h * 0.42f, w * 0.13f)
        }
    }
}

/** Places this thing with its top centred on ([x], [y]), kept inside [maxX] (the map's width). */
private fun Modifier.centeredAt(x: Dp, y: Dp, maxX: Dp) = layout { m, c ->
    val p = m.measure(c.copy(minWidth = 0, minHeight = 0))
    layout(p.width, p.height) {
        val left = (x.roundToPx() - p.width / 2).coerceIn(0, (maxX.roundToPx() - p.width).coerceAtLeast(0))
        p.place(left, y.roundToPx())
    }
}

private fun DrawScope.p(fx: Float, fy: Float) = Offset(size.width * fx, size.height * fy)
private val DrawScope.u get() = size.minDimension
private fun DrawScope.at(land: Land) = p(land.mapX, land.mapY)

/** A smooth closed curve through [pts] (Catmull-Rom turned into Bezier curves). */
private fun smooth(pts: List<Offset>): Path {
    val path = Path()
    val n = pts.size
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 0 until n) {
        val p0 = pts[(i - 1 + n) % n]
        val p1 = pts[i]
        val p2 = pts[(i + 1) % n]
        val p3 = pts[(i + 2) % n]
        val c1 = p1 + (p2 - p0) / 6f
        val c2 = p2 - (p3 - p1) / 6f
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    path.close()
    return path
}

private const val CX = 0.45f
private const val CY = 0.53f
private const val RX = 0.40f
private const val RY = 0.43f
private val wobble = listOf(1.0f, 0.95f, 1.05f, 0.98f, 0.92f, 1.04f, 1.0f, 0.94f, 1.06f, 0.97f, 1.02f, 0.93f, 1.05f, 0.99f, 0.96f)

private fun DrawScope.coast(scale: Float): Path = smooth(
    wobble.mapIndexed { i, f ->
        val a = i * 2 * PI / wobble.size
        p(CX + (RX * f * scale * cos(a)).toFloat(), CY + (RY * f * scale * sin(a)).toFloat())
    },
)

private fun DrawScope.blob(c: Offset, rx: Float, ry: Float, color: Color, seed: Int = 0) {
    val n = 9
    val pts = (0 until n).map { i ->
        val a = i * 2 * PI / n
        val f = 0.85f + 0.3f * (((i * 7 + seed * 13) % 10) / 10f)
        Offset(c.x + (rx * f * cos(a)).toFloat(), c.y + (ry * f * sin(a)).toFloat())
    }
    paperPath(smooth(pts), color)
}

// ---------------------------------------------------------------- sea

private fun DrawScope.ocean() {
    // Flat paper sea (reaching past the edges, for drifting), with a lighter band of shallow water.
    drawRect(Color(0xFF3FA9E0), Offset(-size.width, -size.height), Size(size.width * 3, size.height * 3))
    paperPath(coast(1.32f), Color(0xFF58BCEB))
}

private fun DrawScope.waves(mid: Float, fast: Float) {
    // Shimmering wave marks drifting slowly.
    for (i in 0 until 26) {
        val fx = ((i * 0.137f + mid * (0.04f + (i % 3) * 0.02f)) % 1f)
        val fy = ((i * 0.293f) % 1f)
        val a = ((sin((fast + i * 0.17f) * 2 * PI) + 1) / 2).toFloat()
        drawArc(
            Color.White.copy(alpha = 0.15f + 0.35f * a), 200f, 140f, false,
            p(fx, fy), Size(u * 0.05f, u * 0.025f),
            style = Stroke(u * 0.006f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.boat(fast: Float) {
    val c = p(0.06f, 0.88f)
    val bob = sin(fast * 2 * PI).toFloat() * u * 0.008f
    val s = u * 0.05f
    withTransform({ translate(0f, bob); rotate(sin(fast * 2 * PI).toFloat() * 4f, c) }) {
        val hull = Path().apply {
            moveTo(c.x - s, c.y)
            lineTo(c.x + s, c.y)
            lineTo(c.x + s * 0.7f, c.y + s * 0.4f)
            lineTo(c.x - s * 0.7f, c.y + s * 0.4f)
            close()
        }
        paperPath(hull, Color(0xFF8D6E63))
        drawLine(Color(0xFF5D4037), Offset(c.x, c.y), Offset(c.x, c.y - s * 1.4f), u * 0.005f)
        val sail = Path().apply {
            moveTo(c.x + s * 0.05f, c.y - s * 1.35f)
            lineTo(c.x + s * 0.8f, c.y - s * 0.15f)
            lineTo(c.x + s * 0.05f, c.y - s * 0.15f)
            close()
        }
        paperPath(sail, Color.White)
        val flag = Path().apply {
            moveTo(c.x, c.y - s * 1.4f)
            lineTo(c.x - s * 0.35f, c.y - s * 1.28f)
            lineTo(c.x, c.y - s * 1.16f)
            close()
        }
        paperPath(flag, Color(0xFFF2B630))
    }
}

/** Little fish swimming back and forth in open water. */
private fun DrawScope.fish(mid: Float) {
    data class Lane(val x0: Float, val x1: Float, val y: Float, val color: Long, val speed: Float, val phase: Float)
    val lanes = listOf(
        Lane(0.02f, 0.13f, 0.14f, 0xFFFFB347, 1f, 0f),
        Lane(0.01f, 0.08f, 0.32f, 0xFFFFD54F, 1.4f, 0.4f),
        Lane(0.14f, 0.3f, 0.97f, 0xFFF6C35B, 1.2f, 0.7f),
        Lane(0.88f, 0.98f, 0.44f, 0xFFFFB74D, 0.9f, 0.2f),
        Lane(0.82f, 0.97f, 0.92f, 0xFFF06292, 1.1f, 0.55f),
        Lane(0.55f, 0.72f, 0.985f, 0xFFFFD54F, 1.3f, 0.1f),
    )
    lanes.forEach { l ->
        // Swims one way, turns around, swims back.
        val t = (mid * l.speed + l.phase) % 1f
        val goingRight = t < 0.5f
        val k = if (goingRight) t * 2 else (1 - t) * 2
        val eased = (1 - cos(k * PI).toFloat()) / 2
        val c = p(l.x0 + (l.x1 - l.x0) * eased, l.y + 0.01f * sin(mid * 12 * PI + l.phase * 10).toFloat())
        val s = u * 0.016f
        val dir = if (goingRight) 1f else -1f
        val col = Color(l.color)
        paperOval(col, Offset(c.x - s, c.y - s * 0.55f), Size(s * 2, s * 1.1f))
        val tail = Path().apply {
            moveTo(c.x - dir * s * 0.8f, c.y)
            lineTo(c.x - dir * s * 1.6f, c.y - s * 0.6f)
            lineTo(c.x - dir * s * 1.6f, c.y + s * 0.6f)
            close()
        }
        paperPath(tail, col)
        paperCircle(Color(0xFF263238), s * 0.18f, Offset(c.x + dir * s * 0.5f, c.y - s * 0.1f))
    }
}

// ---------------------------------------------------------------- land

private fun DrawScope.island() {
    // Shallow water, then the beach, then the grass.
    paperPath(coast(1.07f), Color(0xFF81D4FA))
    paperPath(coast(1.0f), Color(0xFFFFE0B2))
    paperPath(coast(0.93f), Color(0xFF9CCC65))
    // Soft lighter patches so the grass isn't flat.
    blob(p(0.30f, 0.50f), u * 0.16f, u * 0.10f, Color(0xFFAED581), 1)
    blob(p(0.55f, 0.70f), u * 0.14f, u * 0.08f, Color(0xFFAED581), 2)
    blob(p(0.48f, 0.25f), u * 0.12f, u * 0.07f, Color(0xFFAED581), 3)
    // A few bushes dotted around.
    listOf(0.22f to 0.36f, 0.48f to 0.18f, 0.62f to 0.86f, 0.42f to 0.68f, 0.82f to 0.48f).forEach { (x, y) ->
        paperCircle(Color(0xFF7CB342), u * 0.018f, p(x, y))
        paperCircle(Color(0xFF8BC34A), u * 0.013f, p(x + 0.012f, y - 0.01f))
    }
}

private fun DrawScope.riverPath(): Path {
    val a = p(Land.PEAKS.mapX - 0.02f, Land.PEAKS.mapY + 0.05f)
    val c1 = p(0.64f, 0.52f)
    val c2 = p(0.58f, 0.68f)
    val b = p(Land.LAGOON.mapX + 0.03f, Land.LAGOON.mapY - 0.03f)
    return Path().apply {
        moveTo(a.x, a.y)
        cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y)
    }
}

/** The river's flowing ripples, drawn live over the baked river. */
private fun DrawScope.riverFlow(fast: Float) {
    drawPath(
        riverPath(), Color.White.copy(alpha = 0.5f),
        style = Stroke(u * 0.006f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(u * 0.02f, u * 0.05f), -fast * u * 0.07f)),
    )
}

private fun DrawScope.riverBase() {
    val a = p(Land.PEAKS.mapX - 0.02f, Land.PEAKS.mapY + 0.05f)
    val c1 = p(0.64f, 0.52f)
    val c2 = p(0.58f, 0.68f)
    val b = p(Land.LAGOON.mapX + 0.03f, Land.LAGOON.mapY - 0.03f)
    drawPath(riverPath(), Color(0xFF4FC3F7), style = Stroke(u * 0.028f, cap = StrokeCap.Round))
    // A little rope bridge where the patterns live.
    fun at(t: Float): Offset {
        val m = 1 - t
        return a * (m * m * m) + c1 * (3 * m * m * t) + c2 * (3 * m * t * t) + b * (t * t * t)
    }
    val q = at(0.22f)
    val dir = at(0.24f) - at(0.2f)
    val len = sqrt(dir.x * dir.x + dir.y * dir.y).coerceAtLeast(0.001f)
    val across = Offset(-dir.y / len, dir.x / len) * (u * 0.035f)
    val along = Offset(dir.x / len, dir.y / len) * (u * 0.011f)
    val planks = listOf(0xFFA1887F, 0xFF8D6E63)
    for (k in -2..2) {
        val c = q + along * (k * 1.25f)
        val plank = Path().apply {
            val p1 = c - across - along * 0.45f
            val p2 = c + across - along * 0.45f
            val p3 = c + across + along * 0.45f
            val p4 = c - across + along * 0.45f
            moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); lineTo(p4.x, p4.y); close()
        }
        paperPath(plank, Color(planks[(k + 2) % 2]))
    }
    listOf(-1f, 1f).forEach { side ->
        val r0 = q + across * side * 1.05f - along * 3f
        val r1 = q + across * side * 1.05f + along * 3f
        drawLine(Color(0xFF5D4037), r0, r1, u * 0.004f, cap = StrokeCap.Round)
    }
}

/** Two market stalls with striped paper awnings and fruit on the counters. */
private fun DrawScope.market(land: Land) {
    val c = at(land)
    val s = u * 0.06f
    listOf(-0.75f to 0xFF7D4C96, 0.75f to 0xFF1E88E5).forEachIndexed { k, (dx, stripe) ->
        val x = c.x + dx * s
        val y = c.y + (if (k == 0) 0.1f else -0.15f) * s
        // Posts and counter.
        paperRect(Color(0xFF8D6E63), Offset(x - s * 0.62f, y - s * 0.5f), Size(s * 0.1f, s * 0.95f))
        paperRect(Color(0xFF8D6E63), Offset(x + s * 0.52f, y - s * 0.5f), Size(s * 0.1f, s * 0.95f))
        paperRect(Color(0xFFD7A26B), Offset(x - s * 0.7f, y + s * 0.15f), Size(s * 1.4f, s * 0.35f))
        // Striped awning with a scalloped edge.
        for (i in 0 until 5) {
            val col = if (i % 2 == 0) Color(stripe) else Color.White
            val left = x - s * 0.75f + i * s * 0.3f
            paperRect(col, Offset(left, y - s * 0.78f), Size(s * 0.3f, s * 0.32f))
            paperArc(col, 0f, 180f, true, Offset(left, y - s * 0.6f), Size(s * 0.3f, s * 0.24f))
        }
        // Fruit on the counter.
        val bob = 0f
        listOf(0xFFE84A7F, 0xFFFFCA28, 0xFF66BB6A).forEachIndexed { i, col ->
            paperCircle(Color(col), s * 0.12f, Offset(x - s * 0.35f + i * s * 0.35f, y + s * 0.08f + bob))
        }
    }
}

/** A smooth open curve through [pts]. */
private fun openCurve(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 0 until pts.size - 1) {
        val p0 = pts[maxOf(i - 1, 0)]
        val p1 = pts[i]
        val p2 = pts[i + 1]
        val p3 = pts[minOf(i + 2, pts.size - 1)]
        val c1 = p1 + (p2 - p0) / 6f
        val c2 = p2 - (p3 - p1) / 6f
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return path
}

/** The paper trail through every stop: the part already walked is bold, the rest is faint. */
private fun DrawScope.trail(current: Int, lap: Int) {
    val pts = (0 until Trail.size).map { Trail.position(it).let { (x, y) -> p(x, y) } }
    val walkedTo = if (lap > 0) pts.size - 1 else current
    val ahead = openCurve(pts.drop(walkedTo))
    drawPath(
        ahead, Color(0x88FFF3E0),
        style = Stroke(u * 0.008f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(u * 0.012f, u * 0.022f))),
    )
    if (walkedTo > 0) {
        val walked = openCurve(pts.take(walkedTo + 1))
        drawPath(walked, Color(0xFFFFF3E0), style = Stroke(u * 0.016f, cap = StrokeCap.Round))
        drawPath(
            walked, Color(0xFFD7B98E),
            style = Stroke(u * 0.006f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(u * 0.012f, u * 0.02f))),
        )
    }
}

private fun DrawScope.tree(c: Offset, s: Float, dark: Boolean) {
    paperRect(Color(0xFF6D4C41), Offset(c.x - s * 0.12f, c.y), Size(s * 0.24f, s * 0.5f))
    paperCircle(if (dark) Color(0xFF388E3C) else Color(0xFF43A047), s * 0.5f, Offset(c.x, c.y - s * 0.15f))
    paperCircle(if (dark) Color(0xFF43A047) else Color(0xFF66BB6A), s * 0.28f, Offset(c.x - s * 0.15f, c.y - s * 0.3f))
}

private fun DrawScope.pine(c: Offset, s: Float) {
    paperRect(Color(0xFF6D4C41), Offset(c.x - s * 0.08f, c.y + s * 0.2f), Size(s * 0.16f, s * 0.3f))
    for (k in 0..2) {
        val top = c.y - s * 0.6f + k * s * 0.25f
        val half = s * (0.25f + k * 0.1f)
        val tri = Path().apply {
            moveTo(c.x, top)
            lineTo(c.x + half, top + s * 0.4f)
            lineTo(c.x - half, top + s * 0.4f)
            close()
        }
        paperPath(tri, Color(0xFF2E7D32))
    }
}

private fun DrawScope.forest(land: Land) {
    val c = at(land)
    val s = u * 0.06f
    listOf(-1.1f to 0.2f, 0.9f to 0.3f, 0f to -0.5f, -0.5f to 0.8f, 0.6f to -0.9f, 1.4f to -0.3f, -1.3f to -0.7f).forEachIndexed { i, (dx, dy) ->
        val q = Offset(c.x + dx * s, c.y + dy * s)
        if (i % 3 == 2) pine(q, s * 0.9f) else tree(q, s, i % 2 == 0)
    }
    paperCircle(Color(0xFFE2A72E), s * 0.12f, Offset(c.x + s * 0.3f, c.y + s * 0.9f))
    paperCircle(Color.White, s * 0.04f, Offset(c.x + s * 0.28f, c.y + s * 0.86f))
}

private fun DrawScope.meadow(land: Land) {
    val c = at(land)
    val s = u * 0.07f
    blob(c, s * 1.5f, s * 0.9f, Color(0xFFDCE775), 5)
    val petals = listOf(0xFFFF80AB, 0xFFFFFFFF, 0xFFB388FF, 0xFFFF8A65)
    for (i in 0 until 14) {
        val a = i * 2.4
        val r = s * (0.3f + 0.08f * (i % 7))
        val q = Offset(c.x + (r * cos(a)).toFloat() * 1.6f, c.y + (r * sin(a)).toFloat())
        paperCircle(Color(petals[i % petals.size]), s * 0.08f, q)
    }
    // A big sunflower in the middle.
    drawLine(Color(0xFF558B2F), c, Offset(c.x, c.y - s * 0.9f), s * 0.08f)
    val head = Offset(c.x, c.y - s * 0.9f)
    for (k in 0 until 10) {
        val a = k * 2 * PI / 10
        paperCircle(Color(0xFFFFCA28), s * 0.16f, Offset(head.x + (s * 0.28f * cos(a)).toFloat(), head.y + (s * 0.28f * sin(a)).toFloat()))
    }
    paperCircle(Color(0xFF6D4C41), s * 0.2f, head)
}

private fun DrawScope.palm(c: Offset, s: Float, lean: Float) {
    val trunk = Path().apply {
        moveTo(c.x, c.y)
        quadraticTo(c.x + lean * s * 0.3f, c.y - s * 0.5f, c.x + lean * s * 0.2f, c.y - s)
    }
    drawPath(trunk, Color(0xFF8D6E63), style = Stroke(s * 0.12f, cap = StrokeCap.Round))
    val top = Offset(c.x + lean * s * 0.2f, c.y - s)
    listOf(-150f, -110f, -70f, -30f, 10f).forEach { deg ->
        val a = deg * PI / 180
        val end = Offset(top.x + (s * 0.55f * cos(a)).toFloat(), top.y + (s * 0.55f * sin(a)).toFloat() + s * 0.15f)
        val leaf = Path().apply {
            moveTo(top.x, top.y)
            quadraticTo((top.x + end.x) / 2, top.y - s * 0.25f, end.x, end.y)
        }
        drawPath(leaf, Color(0xFF43A047), style = Stroke(s * 0.1f, cap = StrokeCap.Round))
    }
    paperCircle(Color(0xFF6D4C41), s * 0.07f, Offset(top.x + s * 0.05f, top.y + s * 0.06f))
}

private fun DrawScope.lagoon(land: Land) {
    val c = at(land)
    val s = u * 0.07f
    // A bay that opens out to the sea at the bottom of the island.
    blob(Offset(c.x, c.y + s * 0.3f), s * 1.5f, s * 0.9f, Color(0xFFFFE0B2), 7)
    blob(Offset(c.x, c.y + s * 0.4f), s * 1.25f, s * 0.7f, Color(0xFF26C6DA), 8)
    blob(Offset(c.x, c.y + s * 0.45f), s * 0.8f, s * 0.42f, Color(0xFF80DEEA), 9)
    palm(Offset(c.x - s * 1.2f, c.y + s * 0.1f), s, 1f)
    palm(Offset(c.x + s * 1.25f, c.y), s * 0.85f, -1f)
}

/** The lagoon's glint, drawn live over the baked lagoon. */
private fun DrawScope.lagoonGlint(land: Land, fast: Float) {
    val c = at(land)
    val s = u * 0.07f
    val glint = ((sin(fast * 2 * PI) + 1) / 2).toFloat()
    drawArc(Color.White.copy(alpha = 0.4f + 0.4f * glint), 200f, 140f, false, Offset(c.x - s * 0.3f, c.y + s * 0.3f), Size(s * 0.4f, s * 0.2f), style = Stroke(s * 0.05f))
}

/** Puffs of smoke drifting up from the factory chimney, drawn live. */
private fun DrawScope.factorySmoke(land: Land, fast: Float) {
    val c = at(land)
    val s = u * 0.07f
    for (k in 0..2) {
        val t = (fast + k / 3f) % 1f
        drawCircle(
            Color.White.copy(alpha = 0.7f * (1 - t)), s * (0.15f + 0.2f * t),
            Offset(c.x + s * 0.55f + t * s * 0.4f, c.y - s * 1.1f - t * s * 0.9f),
        )
    }
}

private fun DrawScope.factory(land: Land) {
    val c = at(land)
    val s = u * 0.07f
    paperRect(Color(0xFF78909C), Offset(c.x + s * 0.4f, c.y - s * 1.0f), Size(s * 0.3f, s * 0.7f))
    paperRect(Color(0xFF22A699), Offset(c.x + s * 0.4f, c.y - s * 1.0f), Size(s * 0.3f, s * 0.1f))
    paperRect(Color(0xFF90A4AE), Offset(c.x - s * 0.9f, c.y - s * 0.4f), Size(s * 1.8f, s * 0.8f))
    // Saw-tooth roof.
    for (k in 0..2) {
        val x0 = c.x - s * 0.9f + k * s * 0.6f
        val roof = Path().apply {
            moveTo(x0, c.y - s * 0.4f)
            lineTo(x0 + s * 0.6f, c.y - s * 0.4f)
            lineTo(x0 + s * 0.6f, c.y - s * 0.75f)
            close()
        }
        paperPath(roof, Color(0xFF607D8B))
    }
    for (k in 0..2) paperRect(Color(0xFFFFF59D), Offset(c.x - s * 0.7f + k * s * 0.55f, c.y - s * 0.15f), Size(s * 0.3f, s * 0.25f))
    paperCircle(Color(0xFFFFB300), s * 0.2f, Offset(c.x - s * 0.95f, c.y + s * 0.4f))
    paperCircle(Color(0xFF90A4AE), s * 0.08f, Offset(c.x - s * 0.95f, c.y + s * 0.4f))
}

private fun DrawScope.mountain(base: Offset, wide: Float, tall: Float, color: Color) {
    val m = Path().apply {
        moveTo(base.x - wide, base.y)
        lineTo(base.x, base.y - tall)
        lineTo(base.x + wide, base.y)
        close()
    }
    paperPath(m, color)
    val cap = Path().apply {
        moveTo(base.x, base.y - tall)
        lineTo(base.x + wide * 0.32f, base.y - tall * 0.68f)
        lineTo(base.x + wide * 0.12f, base.y - tall * 0.74f)
        lineTo(base.x - wide * 0.05f, base.y - tall * 0.66f)
        lineTo(base.x - wide * 0.32f, base.y - tall * 0.68f)
        close()
    }
    paperPath(cap, Color.White)
}

private fun DrawScope.peaks(land: Land) {
    val c = at(land)
    val s = u * 0.07f
    mountain(Offset(c.x - s * 0.8f, c.y + s * 0.5f), s * 0.9f, s * 1.4f, Color(0xFF9575CD))
    mountain(Offset(c.x + s * 0.9f, c.y + s * 0.55f), s * 0.8f, s * 1.2f, Color(0xFF7E57C2))
    mountain(Offset(c.x, c.y + s * 0.6f), s * 1.1f, s * 1.9f, Color(0xFF8E7CC3))
}

private fun DrawScope.caves(land: Land) {
    val c = at(land)
    val s = u * 0.07f
    blob(Offset(c.x, c.y), s * 1.4f, s * 1.0f, Color(0xFF8D8D8D), 11)
    blob(Offset(c.x - s * 0.2f, c.y - s * 0.2f), s * 0.9f, s * 0.6f, Color(0xFFA1A1A1), 12)
    paperArc(Color(0xFF263238), 180f, 180f, true, Offset(c.x - s * 0.5f, c.y - s * 0.2f), Size(s, s * 0.9f))
    paperRect(Color(0xFF263238), Offset(c.x - s * 0.5f, c.y + s * 0.25f), Size(s, s * 0.3f))
}

/** Crystals twinkling round the cave mouth, drawn live. */
private fun DrawScope.caveSparkles(land: Land, fast: Float) {
    val c = at(land)
    val s = u * 0.07f
    listOf(-0.9f to -0.4f, 0.8f to -0.5f, 0.95f to 0.3f).forEachIndexed { i, (dx, dy) ->
        val a = ((sin((fast + i * 0.3f) * 2 * PI) + 1) / 2).toFloat()
        star(Offset(c.x + dx * s, c.y + dy * s), s * 0.18f, Color(0xFF80DEEA).copy(alpha = 0.5f + 0.5f * a))
    }
}

/** The castle's shadow on the sea far below shows it is up in the sky. */
private fun DrawScope.castleShadow(land: Land, mid: Float) {
    val c = at(land)
    val s = u * 0.065f
    val float = sin(mid * 2 * PI).toFloat() * s * 0.12f
    val shadowW = s * (2.6f - float / s)
    drawOval(Color(0x22003050), Offset(c.x - shadowW / 2, c.y + s * 3.4f), Size(shadowW, s * 0.55f))
}

/** The castle on its fluffy cloud, centred on [c] at scale [s]; baked once in its own box. */
private fun DrawScope.castleArt(c: Offset, s: Float) {
    // A little rainbow behind.
    listOf(0xFFF8BBD0, 0xFFFFE082, 0xFFA5D6A7, 0xFF90CAF9).forEachIndexed { i, col ->
        drawArc(
            Color(col).copy(alpha = 0.8f), 180f, 180f, false,
            Offset(c.x - s * (1.9f - i * 0.12f), c.y - s * (1.4f - i * 0.12f)), Size(s * (1.2f - i * 0.24f), s * (1.2f - i * 0.24f)),
            style = Stroke(s * 0.1f),
        )
    }
    // A fluffy cloud island.
    listOf(-1.0f to 0.55f, -0.4f to 0.75f, 0.3f to 0.75f, 0.95f to 0.55f, 0f to 0.45f).forEach { (dx, dy) ->
        paperCircle(Color.White, s * 0.55f, Offset(c.x + dx * s, c.y + dy * s))
    }
    // Towers with pointy roofs and flags.
    paperRect(Color(0xFFF8BBD0), Offset(c.x - s * 0.6f, c.y - s * 0.5f), Size(s * 1.2f, s * 0.9f))
    listOf(-0.75f, 0.75f, 0f).forEachIndexed { i, dx ->
        val tall = if (i == 2) 1.3f else 1.0f
        val x = c.x + dx * s
        paperRect(Color(0xFFF48FB1), Offset(x - s * 0.22f, c.y - s * tall), Size(s * 0.44f, s * tall + s * 0.4f))
        val roof = Path().apply {
            moveTo(x - s * 0.3f, c.y - s * tall)
            lineTo(x, c.y - s * tall - s * 0.5f)
            lineTo(x + s * 0.3f, c.y - s * tall)
            close()
        }
        paperPath(roof, Color(0xFF7E57C2))
        drawLine(Color(0xFF5D4037), Offset(x, c.y - s * tall - s * 0.5f), Offset(x, c.y - s * tall - s * 0.8f), s * 0.05f)
        val flag = Path().apply {
            moveTo(x, c.y - s * tall - s * 0.8f)
            lineTo(x + s * 0.3f, c.y - s * tall - s * 0.72f)
            lineTo(x, c.y - s * tall - s * 0.62f)
            close()
        }
        paperPath(flag, Color(0xFFFFD54F))
    }
    paperArc(Color(0xFF7E57C2), 180f, 180f, true, Offset(c.x - s * 0.2f, c.y - s * 0.1f), Size(s * 0.4f, s * 0.4f))
    paperRect(Color(0xFF7E57C2), Offset(c.x - s * 0.2f, c.y + s * 0.1f), Size(s * 0.4f, s * 0.3f))
}

/** A paper cloud filling its box, for the front layer. */
private fun DrawScope.frontCloud() {
    val r = size.height * 0.3f
    val c = Offset(size.width * 0.5f, size.height * 0.55f)
    paperCircle(Color.White, r, c)
    paperCircle(Color.White, r * 0.8f, Offset(c.x - r * 1.1f, c.y + r * 0.25f))
    paperCircle(Color.White, r * 0.85f, Offset(c.x + r * 1.1f, c.y + r * 0.2f))
    paperCircle(Color.White, r * 0.7f, Offset(c.x + r * 0.4f, c.y - r * 0.55f))
}

/** Sparkles that travel from [from] to [to] (twice), showing the way to the next stop. */
private fun DrawScope.sparklePath(from: Offset, to: Offset, t: Float, still: Boolean) {
    // A dotted guide line all the way, and a trail of sparkles following Rory as he walks
    // (the first 60% of the hint); everything fades at the end.
    val fadeAll = if (t > 0.85f) (1f - t) / 0.15f else 1f
    drawLine(
        Color.White.copy(alpha = 0.7f * fadeAll), from, to, u * 0.008f, cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(u * 0.01f, u * 0.02f)),
    )
    val head = if (still) 1f else ((1 - cos((t / 0.6f).coerceAtMost(1f) * PI)) / 2).toFloat()
    for (k in 0 until 10) {
        val f = head - k * 0.07f
        if (f < 0f) continue
        val q = from + (to - from) * f
        val lift = sin(f * PI * 3 + t * 20).toFloat() * u * 0.015f
        val fade = (1f - k / 10f) * fadeAll
        star(Offset(q.x, q.y - lift), u * (0.035f - k * 0.002f), Color(0xFFFFD54F).copy(alpha = fade.coerceIn(0f, 1f)))
    }
}

/** Soft mist filling its box, laid over a land that isn't open yet. */
private fun DrawScope.mist() {
    val c = center
    val s = size.minDimension / 4.8f
    listOf(-1.0f to 0.1f, -0.3f to -0.4f, 0.5f to -0.2f, 1.0f to 0.25f, 0f to 0.45f, -0.6f to 0.6f, 0.6f to 0.7f, 0f to -0.9f).forEachIndexed { i, (dx, dy) ->
        val r = s * (0.75f + 0.15f * (i % 3))
        drawCircle(Color(0xFFECEFF1).copy(alpha = 0.62f), r, Offset(c.x + dx * s, c.y + dy * s))
    }
}

/** Seagulls flapping across the sky. */
private fun DrawScope.birds(slow: Float, flap: Float) {
    listOf(Triple(0f, 0.12f, 1f), Triple(0.45f, 0.42f, 0.8f)).forEach { (phase, y, scale) ->
        val t = (slow * 1.6f + phase) % 1f
        val c = p(-0.05f + 1.1f * t, y + 0.03f * sin(t * 2 * PI * 3).toFloat())
        val s = u * 0.025f * scale
        val lift = s * (0.2f + 0.6f * flap)
        listOf(-1f, 1f).forEach { side ->
            val wing = Path().apply {
                moveTo(c.x, c.y)
                quadraticTo(c.x + side * s * 0.5f, c.y - lift, c.x + side * s, c.y - lift * 0.3f)
            }
            drawPath(wing, Color(0xFF37474F), style = Stroke(s * 0.22f, cap = StrokeCap.Round))
        }
    }
}
