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
import com.zeronolidge.speechapp.ui.components.detectKidTaps
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
import com.zeronolidge.speechapp.ui.components.drawRoryHand
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
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
import com.zeronolidge.speechapp.logic.IslandStory
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
    /** The pointing hand at the next stop: where, its size, and which way it points. */
    pointer: @Composable (Modifier, Dp, MapAreas.HandDir) -> Unit = { _, _, _ -> },
    /** The crystal cave is open (Pip was found): a glowing paper cave stop just past the Forest. */
    cave: Boolean = false,
    /** The cave has new tries waiting (just unlocked by finding Pip): it glows. */
    caveFresh: Boolean = cave,
    onCave: () -> Unit = {},
    /** The voice stories' stops (id, emoji, its land, finished), each a round paper stop by its land. */
    stories: List<MapStoryStop> = emptyList(),
    onStory: (String) -> Unit = {},
    /** A finished stop's ⭐ was tapped: replay a story there (the trail doesn't move). */
    onDone: (Int) -> Unit = {},
    /**
     * Island story: places already fixed (full colour, one small looping motion). Every other
     * place the child can reach is "broken": sleepy, at about 60% of its own colour, with one
     * small sad detail (never red, cracks, gray or a cross; mist still means "not reached yet").
     */
    fixed: Set<Land> = Land.entries.toSet(),
    /**
     * The fix moment playing now: [fixLand] (shown broken in [fixed] until the sweep reaches it)
     * and how far in it is, 0 to 1 over [IslandStory.FIX_MS]: a colour sweep out from the place's
     * centre (the first [IslandStory.SWEEP_MS]), a soft sparkle, and Rory beside it with his new
     * costume popping on. No text.
     */
    fixLand: Land? = null,
    fixT: Float = 0f,
    /** The island party, 0 to 1 over [IslandStory.PARTY_MS] (negative when there's none): the castle bounces. */
    party: Float = -1f,
    /** What Rory wears on the map (his latest island costume). */
    roryOutfit: Outfit = Outfit(),
) {
    val t = rememberInfiniteTransition(label = "island")
    val slow by t.loop(0f, 1f, 24000, mode = RepeatMode.Restart, easing = LinearEasing, label = "slow")
    val mid by t.loop(0f, 1f, 9000, mode = RepeatMode.Restart, easing = LinearEasing, label = "mid")
    val fast by t.loop(0f, 1f, 2400, mode = RepeatMode.Restart, easing = LinearEasing, label = "fast")
    val flap by t.loop(0f, 1f, 350, label = "flap")
    val pulse by t.loop(1f, 1.1f, 550, label = "pulse")
    // Stories not played yet bob gently, so they read as things to tap.
    val bob by t.loop(0f, 1f, 1600, mode = RepeatMode.Restart, easing = LinearEasing, label = "bob")
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
    var pressedDone by remember { mutableStateOf<Int?>(null) }
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
        val label = MapAreas.labelSp(unit.value)
        val onArea by rememberUpdatedState(onElsewhere)
        val onStopTap by rememberUpdatedState(onStop)
        val onCaveTap by rememberUpdatedState(onCave)
        val onStoryTap by rememberUpdatedState(onStory)
        val onDoneTap by rememberUpdatedState(onDone)
        val dens = LocalDensity.current.density
        // The next stop: same physical size on every screen. Where it and every other round stop
        // go comes from the one shared rule (MapAreas.layout, also checked by MapLayoutTest):
        // clear of every place's label, by its own land.
        val node = 96.dp
        // (Phone: smaller story stops, so the places' own art shows; taps never under 44 dp.)
        val storyNode = MapAreas.storyNode(w.value).dp
        val caveNode = node * 0.8f
        val sticker = unit * 0.085f
        val starTap = MapAreas.starTap(unit.value).dp
        val labelHalfWPx = with(LocalDensity.current) { MapAreas.labelHalfW(label).dp.toPx() }
        val labelHPx = with(LocalDensity.current) { MapAreas.labelH(label).dp.toPx() }
        val layout = with(LocalDensity.current) {
            val wPx = w.toPx()
            val hPx = h.toPx()
            remember(current, lap, cave, stories.map { it.id }, wPx, hPx, labelHPx) {
                MapAreas.layout(
                    current, lap, wPx, hPx, (node / 2).toPx(), if (cave) (caveNode / 2).toPx() else null, (storyNode / 2).toPx(),
                    stories.map { it.id to it.land }, (starTap / 2).toPx(), labelHalfWPx, labelHPx,
                    stopEdge = MapAreas.stopEdge(node.value).dp.toPx(),
                )
            }
        }
        val stopPx = Offset(layout.stop.x, layout.stop.y)
        val stopDp = with(LocalDensity.current) { DpOffset(stopPx.x.toDp(), stopPx.y.toDp()) }
        val cavePx = layout.cave?.let { Offset(it.x, it.y) }
        val storySpots = layout.stories
        val starSpots = layout.done
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
                        storySpots.joinToString("") { sp -> " STORY-${(sp.target as MapTarget.Story).id}@${(o.x + sp.x).toInt()},${(o.y + sp.y).toInt()}" } +
                        starSpots.joinToString("") { sp -> " DONE-${(sp.target as MapTarget.Done).stop}@${(o.x + sp.x).toInt()},${(o.y + sp.y).toInt()}" }
                    islandOrigin = o
                    if (line + marks != lastAreaLog) {
                        lastAreaLog = line + marks
                        println("MAPAREAS $line")
                        println("MAPMARKS $marks")
                    }
                }
                .pointerInput(stopPx, cavePx, storySpots, starSpots, label) {
                    // Everything on the island goes through MapAreas.target: each area's one zone
                    // (picture, label and the gap between) and the next-stop button.
                    fun targetAt(p: Offset): MapTarget = MapAreas.target(
                        p.x, p.y, size.width.toFloat(), size.height.toFloat(),
                        labelHalfW = labelHalfWPx, labelH = labelHPx,
                        stopX = stopPx.x, stopY = stopPx.y, stopR = (node / 2).toPx(),
                        caveX = cavePx?.x ?: 0f, caveY = cavePx?.y ?: 0f, caveR = if (cavePx != null) (caveNode / 2).toPx() else 0f,
                        spots = storySpots + starSpots,
                    )
                    // Feedback the moment the finger touches (press-down, crinkle, wiggle); the stop
                    // opens when the finger lifts, if it didn't drag the map (detectKidTaps: each
                    // touch on its own, so a lost lift can never open a stop later).
                    var pending: MapTarget? = null
                    detectKidTaps(
                        onPress = { p0 ->
                            val t = targetAt(p0)
                            pending = t
                            pressedLand = (t as? MapTarget.Area)?.land
                            pressedStop = t == MapTarget.Stop
                            pressedCave = t == MapTarget.Cave
                            pressedStory = (t as? MapTarget.Story)?.id
                            pressedDone = (t as? MapTarget.Done)?.stop
                            // The stop button crinkles by itself (PaperButton); everything else here too.
                            if (pressedLand != null || pressedCave || pressedStory != null || pressedDone != null) app.sfx.crinkle()
                            if (t != MapTarget.Sea && !still) {
                                wiggled = t
                                scope.launch {
                                    for (a in listOf(9f, -8f, 5f, -3f, 0f)) wiggleTurn.animateTo(a, tween(55))
                                }
                            }
                        },
                        onRelease = {
                            pressedLand = null
                            pressedStop = false
                            pressedCave = false
                            pressedStory = null
                            pressedDone = null
                        },
                        onTap = { p0 ->
                            val t = pending ?: targetAt(p0)
                            pending = null
                            println("MAPTAP ${p0.x.toInt()},${p0.y.toInt()} -> ${when (t) { is MapTarget.Area -> t.land.name; MapTarget.Stop -> "STOP"; MapTarget.Cave -> "CAVE"; is MapTarget.Story -> "STORY-${t.id}"; is MapTarget.Done -> "DONE-${t.stop}"; MapTarget.Sea -> "sea" }}")
                            if (t == MapTarget.Stop || (t is MapTarget.Area && t.land == Trail.landAt(current))) guide = false
                            when (t) {
                                is MapTarget.Area -> onArea(t.land)
                                MapTarget.Stop -> onStopTap()
                                MapTarget.Cave -> onCaveTap()
                                is MapTarget.Story -> onStoryTap(t.id)
                                is MapTarget.Done -> onDoneTap(t.stop)
                                MapTarget.Sea -> app.puff(islandOrigin.x + p0.x, islandOrigin.y + p0.y)
                            }
                        },
                    )
                },
        ) {
            // The island, baked with its broken and fixed places. During a fix moment the same
            // island with that place fixed is baked too, and shown inside a circle that grows from
            // the place's centre: the colour sweep.
            BakedLayer(listOf("map-island", current, lap, fixedKey(fixed)), depth = 4.dp) { islandArt(current, lap, fixed) }
            val sweepLand = fixLand?.takeIf { it != Land.CLOUDS }
            if (sweepLand != null) {
                val sweep = sweepOf(fixT)
                Box(Modifier.fillMaxSize().drawWithContent {
                    val r = size.minDimension * 0.2f * sweep
                    clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(at(sweepLand), r)) }) { this@drawWithContent.drawContent() }
                }) {
                    BakedLayer(listOf("map-island", current, lap, fixedKey(fixed + sweepLand)), depth = 4.dp) { islandArt(current, lap, fixed + sweepLand) }
                }
            }
            val shown = if (sweepLand != null && sweepOf(fixT) >= 1f) fixed + sweepLand else fixed
            // Each fixed place's one small looping motion, drawn live over the baked art.
            Canvas(Modifier.fillMaxSize()) {
                riverFlow(fast)
                val m = if (still) 0f else 1f
                if (Land.FOREST in shown) forestNotes(Land.FOREST, slow * m)
                if (Land.MEADOW in shown) meadowBalloons(Land.MEADOW, mid * m)
                if (Land.LAGOON in shown) { lagoonGlint(Land.LAGOON, fast); pipBoat(Land.LAGOON, mid * m) }
                if (Land.MARKET in shown) marketGlint(Land.MARKET, fast * m)
                if (Land.PEAKS in shown) peaksFlag(Land.PEAKS, fast * m)
                if (Land.FACTORY in shown) { factoryGears(Land.FACTORY, slow * m); factorySmoke(Land.FACTORY, fast) }
                if (Land.CAVES in shown) { caveGlow(Land.CAVES, fast * m); caveSparkles(Land.CAVES, fast) }
                // The fix moment's soft sparkle, round the place as its colour comes back.
                if (fixLand != null && fixLand != Land.CLOUDS) fixSparkle(at(fixLand), size.minDimension * 0.16f, fixT)
            }
            // The castle floats on its cloud, baked in its own small box.
            val castleBox = unit * 0.3f
            Box(
                Modifier
                    .offset(x = w * Land.CLOUDS.mapX - castleBox / 2, y = h * Land.CLOUDS.mapY - castleBox / 2)
                    .size(castleBox)
                    .graphicsLayer {
                        translationY = sin(mid * 2 * PI).toFloat() * size.height * 0.03f
                        // The island party: the castle hops for joy.
                        if (party in 0f..1f) {
                            val hop = kotlin.math.abs(sin(party * 2 * PI * 5)).toFloat() * (1f - party) * 0.12f
                            translationY -= hop * size.height
                            scaleX = 1f + hop * 0.5f
                            scaleY = 1f + hop * 0.5f
                        }
                    },
            ) {
                val castleFixed = Land.CLOUDS in fixed
                BakedLayer(listOf("map-castle", castleFixed), depth = 4.dp) {
                    sleepy(!castleFixed) { castleArt(center, size.minDimension / 4.4f, flags = false) }
                }
                if (fixLand == Land.CLOUDS) {
                    Box(Modifier.fillMaxSize().drawWithContent {
                        val r = size.minDimension * 0.75f * sweepOf(fixT)
                        clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, r)) }) { this@drawWithContent.drawContent() }
                    }) { BakedLayer(listOf("map-castle", true), depth = 4.dp) { castleArt(center, size.minDimension / 4.4f, flags = false) } }
                }
                // Fixed: the flags go up and wave (drawn live).
                val flagsUp = castleFixed || (fixLand == Land.CLOUDS && sweepOf(fixT) >= 1f)
                if (flagsUp) Canvas(Modifier.fillMaxSize()) { castleFlags(center, size.minDimension / 4.4f, if (still) 0f else fast) }
                if (fixLand == Land.CLOUDS) Canvas(Modifier.fillMaxSize()) { fixSparkle(center, size.minDimension * 0.45f, fixT) }
            }

            // Mist over lands the trail hasn't reached, each patch baked once and drifting.
            // test47: if a land still has an open/playable story stop, thin the mist so that stop
            // stays visible; locked lands with no playable stop keep full cloud cover.
            Land.entries.forEach { land ->
                val playableStory = stories.any { it.land == land }
                val strength = when {
                    land == newLand -> 1f - clearing.value
                    // The land the trail reaches next starts to peek through its mist.
                    land == peekLand -> 0.5f
                    !reached(land) && playableStory -> 0.22f
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
                if (guide && fixLand == null) {
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
                        .then(if (!app.testMode) Modifier else Modifier.onSizeChanged { sz ->
                            // For checking: each label's real width against the room every stop leaves it.
                            println("MAPLABEL ${land.name} w=${sz.width} h=${sz.height} room=${(2 * labelHalfWPx).toInt()}x${labelHPx.toInt()}")
                        })
                        .paperCard(PaperStyle.chip, 999.dp)
                        .padding(horizontal = 10.dp, vertical = 1.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlaceIcon(land, label)
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
                    Critter(Species.ROBOT, Modifier.fillMaxSize(), Color(0xFFFFCC80), outfit = roryOutfit, mood = Mood.CHEER, talking = true, mirrored = sx < from.mapX)
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
                androidx.compose.foundation.Canvas(
                    Modifier
                        .offset(x = w * rx + 52.dp + if (left) -rh * 0.45f else rh * 0.7f, y = h * ry)
                        .zIndex(4f)
                        .size(rh * 0.5f)
                        .graphicsLayer { alpha = fade; rotationZ = if (left) -90f else 90f },
                ) { drawRoryHand() }
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

        // The stops, the buddy and the pointer ride on the island layer. During a fix moment they
        // fade right back, so the place itself is what he sees change.
        val stopsAlpha = if (fixLand == null) 1f else 1f - 0.85f * (fixT / 0.08f).coerceAtMost(1f) * ((1f - fixT) / 0.12f).coerceIn(0f, 1f)
        Box(Modifier.fillMaxSize().drift(1f).graphicsLayer { alpha = stopsAlpha }) {
            // Finished stops: one star per place, each tappable (a replay). At a fixed place it's
            // just a small gold star, no badge, so the island reads as a fixed world (Ruby, test52);
            // its tap circle stays 44 dp.
            starSpots.forEach { sp ->
                val stop = (sp.target as MapTarget.Done).stop
                val sd = with(LocalDensity.current) { DpOffset(sp.x.toDp(), sp.y.toDp()) }
                val plain = Trail.landAt(stop) in fixed
                val size = if (plain) starTap * 0.62f else sticker
                Box(
                    Modifier
                        .offset(x = sd.x - size / 2, y = sd.y - size / 2)
                        .size(size)
                        .graphicsLayer {
                            val k = if (pressedDone == stop) 0.9f else 1f
                            scaleX = k
                            scaleY = k
                            rotationZ = ((stop * 37) % 21 - 10).toFloat() + if (wiggled == MapTarget.Done(stop)) wiggleTurn.value else 0f
                        }
                        .then(if (plain) Modifier else Modifier.paperCard(Color(0xFFFFF8E1), circle = true)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (plain) Canvas(Modifier.fillMaxSize()) { goldStar() }
                    else Text("⭐", fontSize = (sticker.value * 0.55f).sp)
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
                val phase = stories.indexOf(st) * 0.33f
                StoryStop(
                    Modifier.offset(x = sd.x - storyNode / 2, y = sd.y - storyNode / 2).size(storyNode).zIndex(2f).popIn("story-$id"),
                    id, st.emoji, done = st.done, pressed = pressedStory == id,
                    wiggle = { if (wiggled == MapTarget.Story(id)) wiggleTurn.value else 0f },
                    bob = { if (st.done || still) 0f else sin((bob + phase) * 2f * PI.toFloat()) },
                    twinkle = { if (st.done || still) -1f else (bob + phase) % 1f },
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
            val hand = node * 0.55f
            val dn = LocalDensity.current
            // The hand: beside the stop on its outer side, else above, below or at a corner of it:
            // wherever it covers no label and stays on the map (MapAreas.handSpot, checked by
            // MapLayoutTest). The buddy steps aside when it has to go on his side.
            val spot = with(dn) {
                val heroL = (if (heroOnRight) w * cx + node * 0.55f else w * cx - node * 1.2f).toPx()
                val heroT = (h * cy - node * 0.6f).toPx()
                MapAreas.handSpot(
                    stopPx.x, stopPx.y, (node / 2).toPx() * MapAreas.STOP_PULSE, hand.toPx(), heroOnRight,
                    w.toPx(), h.toPx(), labelHalfWPx, labelHPx,
                    hero = floatArrayOf(heroL, heroT, heroL + (node * 0.7f).toPx(), heroT + (node * 0.85f).toPx()),
                )
            }
            val heroB = with(dn) { MapAreas.heroBox(stopPx.x, stopPx.y, node.toPx(), heroOnRight, spot, hand.toPx()) }
            hero(
                Modifier
                    .offset(x = with(dn) { heroB[0].toDp() }, y = with(dn) { heroB[1].toDp() })
                    .size(width = node * 0.7f, height = node * 0.85f)
                    .zIndex(3f),
            )
            // Never during the fix moment or the party (Ruby, test53): only once all is still.
            if (fixLand == null && party < 0f) {
                pointer(
                    Modifier.offset(x = with(dn) { spot.x.toDp() }, y = with(dn) { spot.y.toDp() }).zIndex(4f),
                    hand,
                    spot.dir,
                )
            }
        }
        Box(Modifier.fillMaxSize().drift(1f)) {
            // The fix moment: Rory beside the place, his new costume popping on as the colour
            // sweep reaches the edge. No text.
            if (fixLand != null) {
                // Right beside the place, clear of every label and picture (MapAreas.roryBeside).
                val rh = MapAreas.roryH(unit.value).dp
                val dn = LocalDensity.current
                // ...and off the next stop's ring, which fades back in as he goes (test53).
                val ring = layout.stop.copy(r = layout.stop.r * MapAreas.STOP_PULSE)
                val (rx, ry) = with(dn) { MapAreas.roryBeside(fixLand, w.toPx(), h.toPx(), (rh * 0.72f).toPx(), rh.toPx(), labelHalfWPx, labelHPx, avoid = listOf(ring)) }
                val x = with(dn) { rx.toDp() }
                val y = with(dn) { ry.toDp() }
                val onLeft = x + rh * 0.36f < w * fixLand.mapX
                val dressed = fixT * IslandStory.FIX_MS >= IslandStory.SWEEP_MS
                Box(
                    Modifier.offset(x = x, y = y).size(rh * 0.72f, rh).zIndex(5f)
                        .graphicsLayer {
                            alpha = (fixT / 0.06f).coerceAtMost(1f) * ((1f - fixT) / 0.1f).coerceAtMost(1f)
                            val k = popOf(fixT)
                            scaleX = k
                            scaleY = k
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                        },
                ) {
                    Critter(
                        Species.ROBOT, Modifier.fillMaxSize(), Color(0xFFFFCC80),
                        outfit = if (dressed) IslandStory.place(fixLand).costume.asOutfit() else roryOutfit,
                        mood = Mood.CHEER, mirrored = !onLeft,
                    )
                    if (dressed) Canvas(Modifier.fillMaxSize()) { fixSparkle(Offset(size.width / 2, size.height * 0.2f), size.width * 0.45f, ((fixT * IslandStory.FIX_MS - IslandStory.SWEEP_MS) / 700f).coerceIn(0f, 1f)) }
                }
            }
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
private fun StoryStop(
    modifier: Modifier, id: String, emoji: String, done: Boolean, pressed: Boolean, wiggle: () -> Float,
    /** -1 to 1: a gentle up-and-down while it hasn't been played yet. */
    bob: () -> Float = { 0f },
    /** 0 to 1 round the badge for its sparkles; below 0 for none. */
    twinkle: () -> Float = { -1f },
) {
    Box(
        modifier.graphicsLayer {
            val s = if (pressed) 0.92f else 1f
            scaleX = s
            scaleY = s
            rotationZ = wiggle()
            translationY = bob() * 5.dp.toPx()
        },
        contentAlignment = Alignment.Center,
    ) {
        // Not played yet: three paper sparkles circle it.
        if (!done) Canvas(Modifier.fillMaxSize()) {
            val tw = twinkle()
            if (tw >= 0f) {
                val r = size.minDimension * 0.68f
                for (i in 0 until 3) {
                    val ang = tw * 6.2832f + i * 2.0944f
                    star(Offset(center.x + r * cos(ang), center.y + r * sin(ang)), size.minDimension * 0.16f, Color(0xFFFFC107))
                }
            }
        }
        if (id == "owl") {
            // Sleepy Owl's Morning: its own paper picture (a sleepy owl in front of a rising sun on
            // a night-blue badge), so it never looks like any other owl in the game.
            Box(Modifier.fillMaxSize().paperCard(Color(0xFF283593), circle = true).border(4.dp, Color(0xFFFFCA28), CircleShape)) {
                Canvas(Modifier.fillMaxSize().clip(CircleShape)) { sleepyOwlBadge() }
            }
        } else {
            Box(Modifier.fillMaxSize().paperCard(Color(0xFFFFF8E1), circle = true).border(4.dp, Color(0xFFFFCA28), CircleShape), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 30.sp)
            }
        }
        if (done) Text("⭐", fontSize = 18.sp, modifier = Modifier.align(Alignment.TopEnd))
        // A small green ▶ on the badge, so it looks like something to tap and play.
        else Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 6.dp).size(26.dp)
                .paperCard(Color(0xFF43A047), circle = true),
            contentAlignment = Alignment.Center,
        ) { ArrowIcon(12.dp) }
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
private fun fixedKey(fixed: Set<Land>) = Land.entries.filter { it in fixed }.joinToString(",") { it.name }

/** The whole island with its places, each broken (sleepy) or fixed. */
private fun DrawScope.islandArt(current: Int, lap: Int, fixed: Set<Land>) {
    island()
    riverBase()
    trail(current, lap)
    fun place(land: Land, art: DrawScope.(Boolean) -> Unit) = sleepy(land !in fixed) { art(land in fixed) }
    place(Land.FOREST) { forest(Land.FOREST); forestStory(Land.FOREST, it) }
    place(Land.MEADOW) { meadow(Land.MEADOW); meadowStory(Land.MEADOW, it) }
    place(Land.LAGOON) { lagoon(Land.LAGOON); lagoonDock(Land.LAGOON) }
    place(Land.MARKET) { market(Land.MARKET, open = it) }
    place(Land.FACTORY) { factory(Land.FACTORY); if (!it) factoryGears(Land.FACTORY, 0f) }
    place(Land.PEAKS) { peaks(Land.PEAKS); peaksStory(Land.PEAKS, it) }
    place(Land.CAVES) { caves(Land.CAVES, lit = it) }
}

/** The fix moment's colour sweep, 0 to 1 over its first [IslandStory.SWEEP_MS] (eased). */
private fun sweepOf(fixT: Float): Float {
    val t = (fixT * IslandStory.FIX_MS / IslandStory.SWEEP_MS).coerceIn(0f, 1f)
    return 1f - (1f - t) * (1f - t)
}

/** A small flat gold star (a fixed place's finished stop), filling the box. */
private fun DrawScope.goldStar() {
    val c = Offset(size.width / 2, size.height * 0.53f)
    val ro = size.minDimension * 0.5f
    val ri = ro * 0.45f
    val path = Path().apply {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) ro else ri
            val a = (-90f + i * 36f) * PI.toFloat() / 180f
            val pt = Offset(c.x + cos(a) * r, c.y + sin(a) * r)
            if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
        }
        close()
    }
    drawPath(path, Color(0x55000000), style = androidx.compose.ui.graphics.drawscope.Stroke(width = ro * 0.22f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    drawPath(path, Color(0xFFFFC62E))
    drawPath(path, Color(0xFFE09A00), style = androidx.compose.ui.graphics.drawscope.Stroke(width = ro * 0.1f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
}

/** Rory's pop as his costume goes on, right after the sweep: a quick springy grow and settle. */
private fun popOf(fixT: Float): Float {
    val ms = fixT * IslandStory.FIX_MS - IslandStory.SWEEP_MS
    return if (ms !in 0f..450f) 1f else 1f + 0.22f * sin(ms / 450f * PI).toFloat()
}

/** A soft sparkle: pale stars drifting out round [c] (radius about [r]), [t] 0 to 1, gone at the ends. */
private fun DrawScope.fixSparkle(c: Offset, r: Float, t: Float) {
    if (t <= 0f || t >= 1f) return
    val a = sin(t * PI).toFloat()
    for (i in 0 until 7) {
        val ang = i * 0.8976f + t * 1.2f
        val rr = r * (0.55f + 0.5f * t) * (if (i % 2 == 0) 1f else 0.8f)
        star(Offset(c.x + rr * cos(ang), c.y + rr * sin(ang)), r * (if (i % 2 == 0) 0.09f else 0.065f), (if (i % 3 == 0) Color(0xFFFFF59D) else Color.White).copy(alpha = 0.85f * a))
    }
}

/** The castle's three flags, up and waving ([t] loops 0 to 1); its towers as in [castleArt]. */
private fun DrawScope.castleFlags(c: Offset, s: Float, t: Float) {
    listOf(-0.75f, 0.75f, 0f).forEachIndexed { i, dx ->
        val tall = if (i == 2) 1.3f else 1.0f
        val x = c.x + dx * s
        val top = c.y - s * tall - s * 0.8f
        val wave = sin((t + i * 0.33f) * 2 * PI).toFloat() * s * 0.06f
        val flag = Path().apply {
            moveTo(x, top)
            quadraticTo(x + s * 0.17f, top + wave, x + s * 0.34f, top + s * 0.09f + wave * 0.6f)
            quadraticTo(x + s * 0.17f, top + s * 0.18f + wave, x, top + s * 0.2f)
            close()
        }
        drawPath(flag, Color(if (i == 2) 0xFFF06292 else 0xFFFFD54F))
        drawPath(flag, Color.White.copy(alpha = 0.7f), style = Stroke(s * 0.02f))
    }
}

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

private const val CX = MapAreas.ISLAND_CX
private const val CY = MapAreas.ISLAND_CY
private const val RX = MapAreas.ISLAND_RX
private const val RY = MapAreas.ISLAND_RY
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
private fun DrawScope.market(land: Land, open: Boolean = true) {
    val c = at(land)
    val s = u * 0.06f
    listOf(-0.75f to 0xFF7D4C96, 0.75f to 0xFF1E88E5).forEachIndexed { k, (dx, stripe) ->
        val x = c.x + dx * s
        val y = c.y + (if (k == 0) 0.1f else -0.15f) * s
        // Posts and counter.
        paperRect(Color(0xFF8D6E63), Offset(x - s * 0.62f, y - s * 0.5f), Size(s * 0.1f, s * 0.95f))
        paperRect(Color(0xFF8D6E63), Offset(x + s * 0.52f, y - s * 0.5f), Size(s * 0.1f, s * 0.95f))
        paperRect(Color(0xFFD7A26B), Offset(x - s * 0.7f, y + s * 0.15f), Size(s * 1.4f, s * 0.35f))
        if (!open) {
            // Island story, broken: the awning rolled up shut into a striped tube, a plain
            // canvas blind pulled down over the empty counter.
            paperRect(Color(0xFFE8C28A), Offset(x - s * 0.62f, y - s * 0.62f), Size(s * 1.24f, s * 0.95f))
            for (k in 1..3) drawLine(Color(0xFFC99A5B), Offset(x - s * 0.58f, y - s * 0.62f + k * s * 0.24f), Offset(x + s * 0.58f, y - s * 0.62f + k * s * 0.24f), s * 0.03f)
            for (i in 0 until 5) {
                val col = if (i % 2 == 0) Color(stripe) else Color.White
                paperRoundRect(col, Offset(x - s * 0.75f + i * s * 0.3f, y - s * 0.84f), Size(s * 0.3f, s * 0.3f), CornerRadius(s * 0.12f))
            }
            return@forEachIndexed
        }
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
    // (Island story: the factory's paper is a clear steel blue, not gray, so its sleepy broken
    // state can never read as the gray "done" look.)
    paperRect(Color(0xFF6F93C0), Offset(c.x + s * 0.4f, c.y - s * 1.0f), Size(s * 0.3f, s * 0.7f))
    paperRect(Color(0xFF22A699), Offset(c.x + s * 0.4f, c.y - s * 1.0f), Size(s * 0.3f, s * 0.1f))
    paperRect(Color(0xFF8DB0DA), Offset(c.x - s * 0.9f, c.y - s * 0.4f), Size(s * 1.8f, s * 0.8f))
    // Saw-tooth roof.
    for (k in 0..2) {
        val x0 = c.x - s * 0.9f + k * s * 0.6f
        val roof = Path().apply {
            moveTo(x0, c.y - s * 0.4f)
            lineTo(x0 + s * 0.6f, c.y - s * 0.4f)
            lineTo(x0 + s * 0.6f, c.y - s * 0.75f)
            close()
        }
        paperPath(roof, Color(0xFF4F79AD))
    }
    for (k in 0..2) paperRect(Color(0xFFFFF59D), Offset(c.x - s * 0.7f + k * s * 0.55f, c.y - s * 0.15f), Size(s * 0.3f, s * 0.25f))
    // (The gears are drawn by factoryGears: baked and still when broken, turning live when fixed.)
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

private fun DrawScope.caves(land: Land, lit: Boolean = true) {
    val c = at(land)
    val s = u * 0.07f
    // (Island story: lilac stone rather than plain gray, so the sleepy cave never reads as "done".)
    blob(Offset(c.x, c.y), s * 1.4f, s * 1.0f, Color(0xFF9D90BF), 11)
    blob(Offset(c.x - s * 0.2f, c.y - s * 0.2f), s * 0.9f, s * 0.6f, Color(0xFFB7ACD6), 12)
    val mouth = if (lit) Color(0xFF4A2F6B) else Color(0xFF2E2A3D)
    paperArc(mouth, 180f, 180f, true, Offset(c.x - s * 0.5f, c.y - s * 0.2f), Size(s, s * 0.9f))
    paperRect(mouth, Offset(c.x - s * 0.5f, c.y + s * 0.25f), Size(s, s * 0.3f))
    if (lit) {
        // Fixed: a warm glow deep in the mouth, crystals catching it.
        drawOval(Brush.radialGradient(listOf(Color(0xFFFFE082), Color(0xFFFFB74D).copy(alpha = 0.7f), Color.Transparent), Offset(c.x, c.y + s * 0.3f), s * 0.55f),
            Offset(c.x - s * 0.45f, c.y - s * 0.1f), Size(s * 0.9f, s * 0.65f))
        star(Offset(c.x - s * 0.2f, c.y + s * 0.15f), s * 0.1f, Color(0xFFB388FF))
        star(Offset(c.x + s * 0.22f, c.y + s * 0.3f), s * 0.08f, Color(0xFF80DEEA))
    }
    // Two lanterns on little posts either side of the mouth: dark glass when broken, lit when fixed.
    listOf(-0.78f, 0.78f).forEach { dx ->
        val x = c.x + dx * s
        val top = c.y + s * 0.05f
        paperRect(Color(0xFF6D4C41), Offset(x - s * 0.04f, top + s * 0.25f), Size(s * 0.08f, s * 0.42f))
        paperRoundRect(Color(0xFF5D4037), Offset(x - s * 0.17f, top - s * 0.02f), Size(s * 0.34f, s * 0.08f), CornerRadius(s * 0.03f))
        paperRoundRect(if (lit) Color(0xFFFFE082) else Color(0xFF4B4560), Offset(x - s * 0.13f, top + s * 0.05f), Size(s * 0.26f, s * 0.22f), CornerRadius(s * 0.04f))
        paperRoundRect(Color(0xFF5D4037), Offset(x - s * 0.17f, top + s * 0.26f), Size(s * 0.34f, s * 0.06f), CornerRadius(s * 0.03f))
        drawArc(Color(0xFF5D4037), 180f, 180f, false, Offset(x - s * 0.08f, top - s * 0.12f), Size(s * 0.16f, s * 0.2f), style = Stroke(s * 0.03f))
        if (lit) drawCircle(Color(0xFFFFF59D), s * 0.05f, Offset(x, top + s * 0.16f))
    }
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
private fun DrawScope.castleArt(c: Offset, s: Float, flags: Boolean = true) {
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
        // Island story, broken: bare flagpoles (a round knob on top) until the castle is fixed.
        if (!flags) { paperCircle(Color(0xFFFFD54F), s * 0.05f, Offset(x, c.y - s * tall - s * 0.82f)); return@forEachIndexed }
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

// ---------------------------------------------------------------- island story: broken / fixed

/** A broken place is sleepy, not scary: its own colours at about 60% saturation. */
private val SleepyPaint = androidx.compose.ui.graphics.Paint().apply {
    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0.6f) },
    )
}

private inline fun DrawScope.sleepy(on: Boolean, block: DrawScope.() -> Unit) {
    if (!on) { block(); return }
    drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(-size.width, -size.height, size.width * 2f, size.height * 2f), SleepyPaint)
    block()
    drawContext.canvas.restore()
}

/** A little paper songbird sitting at [p], facing right if [right]. */
private fun DrawScope.songbird(p: Offset, b: Float, color: Color, right: Boolean) {
    val d = if (right) 1f else -1f
    paperOval(color, Offset(p.x - b * 0.55f, p.y - b * 0.75f), Size(b * 1.1f, b * 0.8f))
    paperCircle(color, b * 0.33f, Offset(p.x + d * b * 0.42f, p.y - b * 0.85f))
    val beak = Path().apply {
        moveTo(p.x + d * b * 0.7f, p.y - b * 0.92f); lineTo(p.x + d * b * 0.98f, p.y - b * 0.84f); lineTo(p.x + d * b * 0.7f, p.y - b * 0.76f); close()
    }
    paperPath(beak, Color(0xFFFFA726))
    drawCircle(Color(0xFF3A2E5C), b * 0.08f, Offset(p.x + d * b * 0.5f, p.y - b * 0.9f))
    val tail = Path().apply {
        moveTo(p.x - d * b * 0.45f, p.y - b * 0.5f); lineTo(p.x - d * b * 0.95f, p.y - b * 0.75f); lineTo(p.x - d * b * 0.85f, p.y - b * 0.4f); close()
    }
    paperPath(tail, color)
}

/** The forest's owl on a branch at [p]: eyes open when fixed, closed (asleep) when broken. */
private fun DrawScope.forestOwl(p: Offset, o: Float, awake: Boolean) {
    val body = Color(0xFF9C7A5B)
    paperOval(body, Offset(p.x - o * 0.5f, p.y - o * 1.2f), Size(o, o * 1.2f))
    paperOval(Color(0xFFE6D2B5), Offset(p.x - o * 0.3f, p.y - o * 0.7f), Size(o * 0.6f, o * 0.62f))
    listOf(-1f, 1f).forEach { d ->
        val ear = Path().apply { moveTo(p.x + d * o * 0.42f, p.y - o * 1.0f); lineTo(p.x + d * o * 0.4f, p.y - o * 1.38f); lineTo(p.x + d * o * 0.15f, p.y - o * 1.12f); close() }
        paperPath(ear, body)
        val e = Offset(p.x + d * o * 0.2f, p.y - o * 0.88f)
        if (awake) {
            drawCircle(Color.White, o * 0.17f, e)
            drawCircle(Color(0xFFFFB300), o * 0.11f, e)
            drawCircle(Color(0xFF3A2E5C), o * 0.07f, e)
        } else {
            drawArc(Color(0xFF3A2E5C), 20f, 140f, false, Offset(e.x - o * 0.14f, e.y - o * 0.1f), Size(o * 0.28f, o * 0.18f), style = Stroke(o * 0.06f, cap = StrokeCap.Round))
        }
    }
    val beak = Path().apply { moveTo(p.x - o * 0.07f, p.y - o * 0.78f); lineTo(p.x + o * 0.07f, p.y - o * 0.78f); lineTo(p.x, p.y - o * 0.64f); close() }
    paperPath(beak, Color(0xFFFFA726))
}

/** Forest: the branches (bare, with a sleeping owl, when broken; birds and an awake owl when fixed). */
private fun DrawScope.forestStory(land: Land, fixed: Boolean) {
    val c = at(land)
    val s = u * 0.06f
    val wood = Color(0xFF6D4C41)
    // Three branches reaching out of the canopies (their perch points).
    val branches = listOf(
        Triple(Offset(-1.45f, 0.0f), Offset(-2.05f, -0.12f), false),
        Triple(Offset(1.25f, 0.1f), Offset(1.95f, 0.0f), true),
        Triple(Offset(0.9f, -1.1f), Offset(1.55f, -1.25f), true),
    )
    branches.forEach { (a, b, _) ->
        val pa = Offset(c.x + a.x * s, c.y + a.y * s); val pb = Offset(c.x + b.x * s, c.y + b.y * s)
        drawLine(wood, pa, pb, s * 0.1f, cap = StrokeCap.Round)
        // a little twig fork
        val mid = Offset((pa.x + pb.x) / 2, (pa.y + pb.y) / 2)
        drawLine(wood, mid, Offset(mid.x + (pb.x - pa.x) * 0.25f, mid.y - s * 0.22f), s * 0.06f, cap = StrokeCap.Round)
    }
    fun perch(i: Int) = branches[i].let { (a, b, _) -> Offset(c.x + (a.x * 0.35f + b.x * 0.65f) * s, c.y + (a.y * 0.35f + b.y * 0.65f) * s) }
    forestOwl(perch(1), s * 0.62f, awake = fixed)
    if (fixed) {
        songbird(perch(0), s * 0.42f, Color(0xFF29B6F6), right = false)
        songbird(perch(2), s * 0.42f, Color(0xFFFFB300), right = true)
    }
}

/** Fixed forest's loop: little music notes floating up from the singing birds. */
private fun DrawScope.forestNotes(land: Land, t: Float) {
    val c = at(land)
    val s = u * 0.06f
    for (k in 0..1) {
        val f = (t * 3f + k * 0.5f) % 1f
        val base = if (k == 0) Offset(c.x - 1.8f * s, c.y - 0.6f * s) else Offset(c.x + 1.4f * s, c.y - 1.8f * s)
        val q = Offset(base.x + sin(f * 6.28f) * s * 0.15f, base.y - f * s * 0.9f)
        val a = (1f - f) * 0.9f
        drawCircle(Color(0xFF3A2E5C).copy(alpha = a), s * 0.09f, q)
        drawLine(Color(0xFF3A2E5C).copy(alpha = a), Offset(q.x + s * 0.08f, q.y), Offset(q.x + s * 0.08f, q.y - s * 0.3f), s * 0.035f)
    }
}

/** Meadow: the balloons' peg in the grass; when broken, one loose string with no balloon. */
private fun DrawScope.meadowStory(land: Land, fixed: Boolean) {
    val c = at(land)
    val s = u * 0.07f
    val peg = Offset(c.x + s * 0.95f, c.y + s * 0.15f)
    paperRect(Color(0xFF8D6E63), Offset(peg.x - s * 0.05f, peg.y - s * 0.1f), Size(s * 0.1f, s * 0.25f))
    if (!fixed) {
        val str = Path().apply {
            moveTo(peg.x, peg.y - s * 0.08f)
            cubicTo(peg.x + s * 0.3f, peg.y - s * 0.3f, peg.x - s * 0.15f, peg.y - s * 0.55f, peg.x + s * 0.2f, peg.y - s * 0.85f)
        }
        drawPath(str, Color(0xFF5D4037), style = Stroke(s * 0.035f, cap = StrokeCap.Round))
        paperCircle(Color(0xFFF48FB1), s * 0.06f, Offset(peg.x + s * 0.2f, peg.y - s * 0.86f))
    }
}

/** Fixed meadow's loop: five balloons tied to the peg, bobbing over the meadow. */
private fun DrawScope.meadowBalloons(land: Land, t: Float) {
    val c = at(land)
    val s = u * 0.07f
    val peg = Offset(c.x + s * 0.95f, c.y + s * 0.05f)
    val cols = listOf(0xFFFF8A65, 0xFFFFCA28, 0xFF4FC3F7, 0xFFBA68C8, 0xFFF06292)
    val spots = listOf(-0.55f to -1.55f, 0.05f to -1.85f, 0.6f to -1.6f, -0.25f to -1.2f, 0.35f to -1.25f)
    spots.forEachIndexed { i, (dx, dy) ->
        val bob = sin((t + i * 0.21f) * 2 * PI).toFloat() * s * 0.06f
        val b = Offset(peg.x + dx * s, peg.y + dy * s + bob)
        drawLine(Color(0xFF5D4037).copy(alpha = 0.8f), peg, Offset(b.x, b.y + s * 0.26f), s * 0.025f)
        drawOval(Color(cols[i]), Offset(b.x - s * 0.2f, b.y - s * 0.24f), Size(s * 0.4f, s * 0.5f))
        drawOval(Color(0x33000000), Offset(b.x - s * 0.2f, b.y - s * 0.24f), Size(s * 0.4f, s * 0.5f), style = Stroke(s * 0.02f))
        drawCircle(Color.White.copy(alpha = 0.6f), s * 0.05f, Offset(b.x - s * 0.08f, b.y - s * 0.1f))
        val knot = Path().apply { moveTo(b.x - s * 0.04f, b.y + s * 0.3f); lineTo(b.x + s * 0.04f, b.y + s * 0.3f); lineTo(b.x, b.y + s * 0.24f); close() }
        drawPath(knot, Color(cols[i]))
    }
}

/** Lagoon: a little wooden dock out into the water (empty when broken; Pip's boat ties up when fixed). */
private fun DrawScope.lagoonDock(land: Land) {
    val c = at(land)
    val s = u * 0.07f
    val x = c.x + s * 0.35f
    paperRect(Color(0xFFB07C4F), Offset(x - s * 0.14f, c.y - s * 0.1f), Size(s * 0.28f, s * 0.62f))
    for (k in 1..4) drawLine(Color(0xFF7A5232), Offset(x - s * 0.14f, c.y - s * 0.1f + k * s * 0.124f), Offset(x + s * 0.14f, c.y - s * 0.1f + k * s * 0.124f), s * 0.025f)
    listOf(-0.17f, 0.17f).forEach { d -> paperRect(Color(0xFF6D4C41), Offset(x + d * s - s * 0.04f, c.y + s * 0.42f), Size(s * 0.08f, s * 0.16f)) }
}

/** Fixed lagoon's loop: Pip's paper boat, Pip aboard, rocking at the end of the dock. */
private fun DrawScope.pipBoat(land: Land, t: Float) {
    val c = at(land)
    val s = u * 0.07f
    val b = Offset(c.x - s * 0.2f, c.y + s * 0.55f)
    withTransform({ rotate(sin(t * 2 * PI).toFloat() * 5f, b) }) {
        // Pip, a brown puppy, peeking over the side.
        drawCircle(Color(0xFFB07C4F), s * 0.17f, Offset(b.x - s * 0.05f, b.y - s * 0.25f))
        drawOval(Color(0xFF6D4C41), Offset(b.x - s * 0.26f, b.y - s * 0.38f), Size(s * 0.1f, s * 0.2f))
        drawOval(Color(0xFF6D4C41), Offset(b.x + s * 0.06f, b.y - s * 0.38f), Size(s * 0.1f, s * 0.2f))
        drawCircle(Color(0xFF3A2E5C), s * 0.025f, Offset(b.x - s * 0.1f, b.y - s * 0.27f))
        drawCircle(Color(0xFF3A2E5C), s * 0.025f, Offset(b.x + s * 0.0f, b.y - s * 0.27f))
        drawCircle(Color(0xFF3A2E5C), s * 0.035f, Offset(b.x - s * 0.05f, b.y - s * 0.2f))
        // The folded paper boat.
        val hull = Path().apply {
            moveTo(b.x - s * 0.55f, b.y - s * 0.12f); lineTo(b.x + s * 0.55f, b.y - s * 0.12f)
            lineTo(b.x + s * 0.38f, b.y + s * 0.12f); lineTo(b.x - s * 0.38f, b.y + s * 0.12f); close()
        }
        drawPath(hull, Color(0xFFFFFDF5))
        drawPath(hull, Color(0xFFE0A030), style = Stroke(s * 0.035f))
        val sail = Path().apply { moveTo(b.x + s * 0.12f, b.y - s * 0.12f); lineTo(b.x + s * 0.12f, b.y - s * 0.62f); lineTo(b.x + s * 0.45f, b.y - s * 0.12f); close() }
        drawPath(sail, Color(0xFFFFF3C4))
        drawPath(sail, Color(0xFFE0A030), style = Stroke(s * 0.03f))
    }
}

/** Fixed market's loop: a coin twinkling over the open stalls. */
private fun DrawScope.marketGlint(land: Land, t: Float) {
    val c = at(land)
    val s = u * 0.06f
    val a = ((sin(t * 2 * PI) + 1) / 2).toFloat()
    drawCircle(Color(0xFFFFC107), s * 0.16f, Offset(c.x, c.y - s * 1.15f))
    drawCircle(Color(0xFFFFE082), s * 0.1f, Offset(c.x, c.y - s * 1.15f))
    star(Offset(c.x + s * 0.18f, c.y - s * 1.3f), s * 0.09f * (0.5f + a), Color.White.copy(alpha = 0.5f + 0.5f * a))
}

/** Peaks: the stone path (laid up the big peak when fixed; stones scattered at its foot when broken). */
private fun DrawScope.peaksStory(land: Land, fixed: Boolean) {
    val c = at(land)
    val s = u * 0.07f
    val stone = listOf(Color(0xFFB98A57), Color(0xFF9C6E45), Color(0xFFD1A673))
    if (fixed) {
        val pts = listOf(-0.3f to 0.5f, 0.0f to 0.38f, 0.3f to 0.22f, 0.1f to 0.04f, -0.18f to -0.12f, 0.02f to -0.3f, 0.18f to -0.48f, 0.02f to -0.66f, -0.06f to -0.84f, 0.02f to -1.02f)
        pts.forEachIndexed { i, (dx, dy) ->
            val k = 1f - i * 0.045f
            paperOval(stone[i % 3], Offset(c.x + dx * s - s * 0.16f * k, c.y + dy * s - s * 0.085f * k), Size(s * 0.32f * k, s * 0.17f * k))
        }
    } else {
        listOf(-1.25f to 0.62f, -0.85f to 0.78f, -0.4f to 0.68f, 0.15f to 0.8f, 0.55f to 0.66f, 1.05f to 0.8f, 1.5f to 0.64f).forEachIndexed { i, (dx, dy) ->
            val q = Offset(c.x + dx * s, c.y + dy * s)
            withTransform({ rotate((i * 47 % 60 - 30).toFloat(), q) }) {
                paperOval(stone[i % 3], Offset(q.x - s * 0.19f, q.y - s * 0.1f), Size(s * 0.38f, s * 0.2f))
            }
        }
    }
}

/** Fixed peaks' loop: a little flag waving at the top of the path. */
private fun DrawScope.peaksFlag(land: Land, t: Float) {
    val c = at(land)
    val s = u * 0.07f
    val top = Offset(c.x + s * 0.02f, c.y - s * 1.05f)
    drawLine(Color(0xFF5D4037), top, Offset(top.x, top.y - s * 0.5f), s * 0.04f)
    val w = sin(t * 2 * PI).toFloat() * s * 0.05f
    val flag = Path().apply { moveTo(top.x, top.y - s * 0.5f); quadraticTo(top.x + s * 0.15f, top.y - s * 0.5f + w, top.x + s * 0.32f, top.y - s * 0.42f); lineTo(top.x, top.y - s * 0.32f); close() }
    drawPath(flag, Color(0xFFFFA726))
}

/** The factory's two gears, meshing; [turn] 0..1 spins them (fixed), 0 leaves them still (broken). */
private fun DrawScope.factoryGears(land: Land, turn: Float) {
    val c = at(land)
    val s = u * 0.07f
    fun gear(g: Offset, r: Float, teeth: Int, angle: Float, col: Color) {
        val p = Path()
        val n = teeth * 2
        for (i in 0 until n) {
            val a0 = angle + i * PI.toFloat() * 2f / n
            val a1 = angle + (i + 1) * PI.toFloat() * 2f / n
            val rr = if (i % 2 == 0) r else r * 0.78f
            val x0 = g.x + rr * cos(a0); val y0 = g.y + rr * sin(a0)
            if (i == 0) p.moveTo(x0, y0) else p.lineTo(x0, y0)
            p.lineTo(g.x + rr * cos(a1), g.y + rr * sin(a1))
        }
        p.close()
        paperPath(p, col)
        drawCircle(Color(0xFF4F79AD), r * 0.3f, g)
    }
    val a = turn * 2f * PI.toFloat() * 6f
    gear(Offset(c.x - s * 0.95f, c.y + s * 0.4f), s * 0.3f, 8, a, Color(0xFFFFB300))
    gear(Offset(c.x - s * 0.5f, c.y + s * 0.66f), s * 0.2f, 6, -a * 1.5f + 0.3f, Color(0xFFFF8A3D))
}

/** Fixed caves' loop: the warm glow in the cave mouth breathing. */
private fun DrawScope.caveGlow(land: Land, t: Float) {
    val c = at(land)
    val s = u * 0.07f
    val a = 0.25f + 0.2f * ((sin(t * 2 * PI) + 1) / 2).toFloat()
    drawCircle(Color(0xFFFFE082).copy(alpha = a), s * 0.75f, Offset(c.x, c.y + s * 0.2f))
}
