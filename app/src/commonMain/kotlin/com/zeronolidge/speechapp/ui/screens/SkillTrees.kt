package com.zeronolidge.speechapp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.SkillBranch
import com.zeronolidge.speechapp.logic.SkillNode
import com.zeronolidge.speechapp.logic.SkillState
import com.zeronolidge.speechapp.logic.SkillStatus
import com.zeronolidge.speechapp.logic.SkillTier
import com.zeronolidge.speechapp.logic.SkillTree
import com.zeronolidge.speechapp.logic.Mastery
import com.zeronolidge.speechapp.state.today
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.art.paperCircle
import com.zeronolidge.speechapp.ui.art.paperOval
import com.zeronolidge.speechapp.ui.art.paperPath
import com.zeronolidge.speechapp.ui.art.paperRect
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.PaperButton
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Test hooks: "#test=grownups-tree" opens the grown-ups page on its Skill tree tab, "&branch=LETTERS" picks a branch. */
object SkillTreeTest {
    var parentTab = 0
    var branch: SkillBranch? = null
}

private val inkSoft = Color(0xFF6D6255)
private val doneGrey = Color(0xFF9E9E9E)
private val workingYellow = Color(0xFFFFF3B0)

// ==================================================================== grown-ups: the Skill tree tab

/**
 * test59/test60: what's mastered, what's in progress and what's next, one branch at a time. A
 * sound fills only when you tap "Mastered"; its six dots are your own marks (on its own, start,
 * middle, end, phrases, sentences). Tap games master themselves at about 80% right on 2 days.
 * Mastered skills fade with a grey check (never green). Gender-neutral copy.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentSkillTree() {
    val p = LocalApp.current.progress
    var branch by remember { mutableStateOf(SkillTreeTest.branch ?: SkillBranch.TALKING) }
    val states = p.skills
    val day = remember { today() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("What's mastered, what's in progress, what's next.", fontSize = 15.sp, color = inkSoft)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkillBranch.entries.forEach { b ->
                val on = b == branch
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (on) Color(0xFF5D4037) else Color(0xFFF1E9DD))
                        .clickable { branch = b }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) { Text("${b.emoji} ${b.label}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else Kid.ink) }
            }
        }
        // The one-line week at the top.
        Text(
            SkillTree.weekSummary(branch, states, day),
            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Kid.ink,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF8E1)).padding(12.dp),
        )
        Text(
            when (branch) {
                SkillBranch.TALKING -> "Tap a dot when the sound can be said that way. Tap Mastered when the sound is solid; nothing fills by itself."
                SkillBranch.STORIES -> "A story is done when it is played to the end."
                SkillBranch.LETTERS -> "Games master themselves at about 80% right first time on 2 different days. ABC is yours to mark."
                SkillBranch.NUMBERS -> "Games master themselves at about 80% right first time on 2 different days. 1 2 3 is yours to mark."
                SkillBranch.THINKING -> "Games master themselves at about 80% right first time on 2 different days."
            },
            fontSize = 13.sp, color = inkSoft,
        )
        if (branch == SkillBranch.TALKING) Text("Dots: on its own · start · middle · end of words · phrases · sentences", fontSize = 13.sp, color = inkSoft)
        val list = SkillTree.of(branch)
        val tiers = if (branch == SkillBranch.STORIES) listOf(null) else SkillTier.entries.filter { t -> list.any { it.tier == t } }
        tiers.forEach { tier ->
            if (tier != null) Text(tier.label.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = inkSoft, modifier = Modifier.padding(top = 4.dp))
            val group = list.filter { tier == null || it.tier == tier }
            group.forEachIndexed { i, n ->
                SkillRow(n, states, last = i == group.lastIndex)
            }
        }
    }
}

@Composable
private fun SkillRow(n: SkillNode, states: Map<String, SkillState>, last: Boolean) {
    val p = LocalApp.current.progress
    val s = states[n.id]
    val status = SkillTree.status(n, states)
    val dashed = status == SkillStatus.UPCOMING && n.tier != SkillTier.START
    val mastered = status == SkillStatus.MASTERED
    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
        // The line that joins the skills, with a node on it.
        Box(Modifier.width(28.dp).fillMaxHeight()) {
            Canvas(Modifier.fillMaxSize()) {
                val x = size.width / 2
                drawLine(Color(0xFFD7CCC8), Offset(x, 0f), Offset(x, if (last) 26.dp.toPx() else size.height), 3.dp.toPx())
                val c = Offset(x, 26.dp.toPx())
                when (status) {
                    SkillStatus.MASTERED -> drawCircle(doneGrey, 9.dp.toPx(), c)
                    SkillStatus.CURRENT -> { drawCircle(Color(0xFFFFB300), 10.dp.toPx(), c); drawCircle(Color.White, 4.dp.toPx(), c) }
                    SkillStatus.UPCOMING -> {
                        drawCircle(Color.White, 8.dp.toPx(), c)
                        drawCircle(Color(0xFFBCAAA4), 8.dp.toPx(), c, style = Stroke(2.dp.toPx(), pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6f, 5f)) else null))
                    }
                }
            }
        }
        Column(
            Modifier.weight(1f).padding(bottom = 8.dp)
                .alpha(if (mastered) 0.6f else 1f)
                .clip(RoundedCornerShape(14.dp))
                .background(if (status == SkillStatus.CURRENT) workingYellow else Color.White)
                .drawBehind {
                    val r = CornerRadius(14.dp.toPx())
                    drawRoundRect(
                        if (status == SkillStatus.CURRENT) Color(0xFFFFB300) else Color(0xFFBCAAA4),
                        cornerRadius = r,
                        style = Stroke(if (status == SkillStatus.CURRENT) 2.5f.dp.toPx() else 1.5f.dp.toPx(), pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(14f, 9f)) else null),
                    )
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (mastered) {
                    GreyCheck(Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(n.label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Kid.ink, modifier = Modifier.weight(1f))
                val tries = s?.tries ?: 0
                Text(
                    when (status) {
                        SkillStatus.MASTERED -> "Mastered"
                        SkillStatus.CURRENT -> "Working on · ${tries} ${if (tries == 1) "try" else "tries"}"
                        SkillStatus.UPCOMING -> if (tries > 0) "${tries} ${if (tries == 1) "try" else "tries"}" else n.tier.label
                    },
                    fontSize = 13.sp, color = inkSoft,
                )
                if (mastered && n.mastery != Mastery.FINISHED) {
                    Spacer(Modifier.width(10.dp))
                    SmallAction("Not yet") { p.setSkillMastered(n.id, false) }
                }
            }
            if (status == SkillStatus.CURRENT && n.mastery != Mastery.FINISHED) {
                val prog = SkillTree.progress(n, s)
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFFE082))) {
                    Box(Modifier.fillMaxWidth(prog.coerceIn(0.02f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF8F00)))
                }
            }
            if (!mastered && n.mastery != Mastery.FINISHED) Row(verticalAlignment = Alignment.CenterVertically) {
                if (n.branch == SkillBranch.TALKING) {
                    // The six dots: big enough to tap, small to look at.
                    SkillTree.LEVELS.indices.forEach { lv ->
                        val on = lv in (s?.levels ?: emptySet())
                        Box(Modifier.size(34.dp).clip(CircleShape).clickable { p.toggleSkillLevel(n.id, lv) }, contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.size(14.dp).clip(CircleShape).background(if (on) Color(0xFF6D4C41) else Color.White)
                                    .drawBehind { drawCircle(Color(0xFF8D6E63), size.minDimension / 2, style = Stroke(1.5f.dp.toPx())) },
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                when (n.mastery) {
                    Mastery.GROWN_UP -> SmallAction("Mastered") { p.setSkillMastered(n.id, true) }
                    Mastery.TAP_GAMES -> Text("${SkillTree.goodDays(s ?: SkillState())} of ${SkillTree.AUTO_DAYS} days at 80%", fontSize = 12.sp, color = inkSoft)
                    Mastery.FINISHED -> {}
                }
            }
        }
    }
}

/** A grey tick in a grey circle (mastered is grey, never green). */
@Composable
private fun GreyCheck(modifier: Modifier) {
    Canvas(modifier) {
        drawCircle(doneGrey, size.minDimension / 2)
        val p = Path().apply {
            moveTo(size.width * 0.27f, size.height * 0.52f)
            lineTo(size.width * 0.44f, size.height * 0.69f)
            lineTo(size.width * 0.74f, size.height * 0.34f)
        }
        drawPath(p, Color.White, style = Stroke(size.minDimension * 0.13f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

@Composable
private fun SmallAction(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFEFEBE9)).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Kid.ink) }
}

// ==================================================================== the child's tree

/**
 * test59/test60: the tree for one area at a time (the 🌳 on the island map). Every try is a new
 * leaf and grows that skill's fruit by size, but tries alone stop at about 80% of full size: it
 * only ripens on mastery (the last 20%, the full warm colour, and a one-time soft glow). Grey
 * buds are what's next. Nothing shrinks, wilts or falls off on days with no practice; no
 * numbers, no locks. Taps only, big targets (even on small buds).
 */
@Composable
fun KidTreeScreen(nav: Nav) {
    val app = LocalApp.current
    val p = app.progress
    var branch by remember { mutableStateOf(SkillTreeTest.branch ?: SkillBranch.TALKING) }
    val states = p.skills
    KidScreen("🌳", nav.home, SkillTree.openLine(branch, states), onBack = nav.back) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth
            val h = maxHeight
            val btn = if (w < 600.dp) ((w - 16.dp * 4) / 5).coerceIn(56.dp, 76.dp) else 88.dp
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    TreeArt(branch, states) { n -> app.instruct(SkillTree.fruitLine(n, states)) }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(if (w < 600.dp) 12.dp else 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    SkillBranch.entries.forEach { b ->
                        val on = b == branch
                        PaperButton(
                            if (on) Color(0xFFFFD54F) else Color(0xFFF1E9DD),
                            Modifier.size(btn).graphicsLayer { val k = if (on) 1.08f else 1f; scaleX = k; scaleY = k },
                            onClick = {
                                branch = b
                                println("TREE branch=${b.name} leaves=${SkillTree.leaves(b, states)}")
                                app.instruct(SkillTree.openLine(b, states))
                            },
                        ) { Text(b.emoji, fontSize = (btn.value * 0.45f).sp) }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/** A gentle hash for steady leaf and fruit spots (the same every time the tree is opened). */
private fun rnd(i: Int, k: Int): Float {
    var x = i * 374761393 + k * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    return ((x xor (x ushr 16)) and 0xFFFF) / 65535f
}

/**
 * Fruit spots in the crown (dp from its middle) for [n] fruits of [size] dp: rows of fruit inside
 * the crown's oval, never touching each other, bottom rows first (the first skills hang lowest,
 * nearest the trunk), each row centred.
 */
private fun fruitSpots(n: Int, size: Float, rx: Float, ry: Float): List<Pair<Float, Float>>? {
    val gapX = size * 1.14f
    val gapY = size * 1.08f
    val ax = rx - size * 0.55f
    val ay = ry - size * 0.55f
    if (ax <= 0f || ay <= 0f) return null
    val rows = mutableListOf<List<Pair<Float, Float>>>()
    var y = ay - size * 0.1f
    while (y >= -ay) {
        val half = ax * kotlin.math.sqrt((1f - (y / ay) * (y / ay)).coerceAtLeast(0f))
        val count = ((2 * half) / gapX).toInt() + 1
        if (half >= 0f && count > 0) rows += (0 until count).map { k -> (k - (count - 1) / 2f) * gapX to y }
        y -= gapY
    }
    val all = rows.flatten()
    if (all.size < n) return null
    // Fill whole rows from the bottom; the last row used is centred on what it holds.
    val out = mutableListOf<Pair<Float, Float>>()
    for (r in rows) {
        val left = n - out.size
        if (left <= 0) break
        if (r.size <= left) out += r
        else { val y0 = r[0].second; out += (0 until left).map { k -> (k - (left - 1) / 2f) * gapX to y0 } }
    }
    return out
}

@Composable
private fun TreeArt(branch: SkillBranch, states: Map<String, SkillState>, onFruit: (SkillNode) -> Unit) {
    val nodes = SkillTree.of(branch)
    val leaves = SkillTree.leaves(branch, states).coerceAtMost(MAX_LEAVES)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value
        // The crown: a wide oval over a short trunk, as big as the space allows.
        val crownHalfW = minOf(w * 0.42f, h * 0.6f)
        val crownHalfH = minOf(h * 0.34f, crownHalfW * 1.1f)
        val cx = w / 2f
        val cy = crownHalfH + 10f
        val groundY = h - 8f
        // The biggest fruit that lets every skill hang clear of the others.
        val fit = (104 downTo 48 step 4).firstNotNullOfOrNull { sz -> fruitSpots(nodes.size, sz.toFloat(), crownHalfW, crownHalfH)?.let { sz.toFloat() to it } }
        val fruitSize = fit?.first ?: 48f
        val spots = fit?.second ?: fruitSpots(nodes.size, 40f, crownHalfW, crownHalfH).orEmpty()
        LaunchedEffect(branch) { println("TREE ${branch.name} leaves=$leaves fruits=${nodes.size} size=${fruitSize.roundToInt()} crown=${(crownHalfW * 2).roundToInt()}x${(crownHalfH * 2).roundToInt()}") }
        Canvas(Modifier.fillMaxSize()) {
            val k = density
            val ccx = cx * k
            val ccy = cy * k
            val rx = crownHalfW * k
            val ry = crownHalfH * k
            // Grass and the trunk with two branches into the crown.
            paperOval(Color(0xFF9CCC65), Offset(ccx - rx * 1.1f, groundY * k - 18 * k), Size(rx * 2.2f, 36 * k))
            val trunkW = rx * 0.22f
            val trunk = Path().apply {
                moveTo(ccx - trunkW * 0.7f, groundY * k)
                quadraticTo(ccx - trunkW * 0.35f, ccy + ry * 0.6f, ccx - trunkW * 0.5f, ccy + ry * 0.2f)
                lineTo(ccx + trunkW * 0.5f, ccy + ry * 0.2f)
                quadraticTo(ccx + trunkW * 0.35f, ccy + ry * 0.6f, ccx + trunkW * 0.7f, groundY * k)
                close()
            }
            paperPath(trunk, Color(0xFF8D6E63))
            // The crown: overlapping paper circles (always there; the leaves go on top).
            paperOval(Color(0xFF7CB342), Offset(ccx - rx, ccy - ry), Size(rx * 2f, ry * 2f))
            // Bumps round its edge, so it reads as a leafy crown, not an egg.
            val bump = minOf(rx, ry) * 0.3f
            for (i in 0 until 12) {
                val a = i * 6.2832f / 12f
                paperCircle(if (i % 2 == 0) Color(0xFF8BC34A) else Color(0xFF7CB342), bump, Offset(ccx + cos(a) * (rx - bump * 0.7f), ccy + sin(a) * (ry - bump * 0.7f)))
            }
            // One leaf per try, in steady spots; they never fall off.
            for (i in 0 until leaves) {
                val a = rnd(i, 1) * 6.2832f
                val d = kotlin.math.sqrt(rnd(i, 2)) * 0.95f
                val lx = ccx + cos(a) * d * rx
                val ly = ccy + sin(a) * d * ry
                val ls = (9f + rnd(i, 3) * 6f) * k
                val col = when ((rnd(i, 4) * 3).toInt()) { 0 -> Color(0xFF558B2F); 1 -> Color(0xFF689F38); else -> Color(0xFF9CCC65) }
                rotate(rnd(i, 5) * 360f, Offset(lx, ly)) {
                    paperOval(col, Offset(lx - ls, ly - ls * 0.45f), Size(ls * 2f, ls * 0.9f))
                }
            }
        }
        // The fruit: one per skill, each a big tap target.
        spots.forEachIndexed { i, (sx, sy) ->
            val n = nodes[i]
            val s = states[n.id]
            val status = SkillTree.status(n, states)
            val grow = SkillTree.fruitGrowth(s)
            val bump = remember(n.id) { Animatable(1f) }
            val scope = rememberCoroutineScope()
            val x = cx + sx
            val y = cy + sy
            Box(
                Modifier.offset((x - fruitSize / 2).dp, (y - fruitSize / 2).dp).size(fruitSize.dp)
                    .graphicsLayer { scaleX = bump.value; scaleY = bump.value }
                    .clickable {
                        scope.launch { bump.snapTo(1.15f); bump.animateTo(1f, tween(260)) }
                        println("TREEFRUIT ${n.id} status=$status grow=$grow")
                        onFruit(n)
                    },
                contentAlignment = Alignment.Center,
            ) { Fruit(n, status, grow, s?.tries ?: 0, fruitSize) }
        }
    }
}

private const val MAX_LEAVES = 160

/** Ids of ripe fruit that have already played their one-time glow in this sitting. */
private val glowedRipe = mutableSetOf<String>()

/**
 * A fruit as it is now. Tries grow it from a small fruit up to about 80% of full size (capped);
 * ripening adds the last 20%, the full warm colour, and a one-time soft glow. A grey bud is
 * what's next. The drawn fruit may be small; its tap target is always the full slot.
 */
@Composable
private fun Fruit(n: SkillNode, status: SkillStatus, grow: Float, tries: Int, sizeDp: Float) {
    val ripe = status == SkillStatus.MASTERED
    val bud = !ripe && tries == 0
    val t = (grow / SkillTree.GROW_CAP).coerceIn(0f, 1f)
    val scale = when {
        ripe -> 1f
        bud -> 0.34f
        else -> 0.34f + (SkillTree.GROW_CAP - 0.34f) * t
    }
    val glow = remember(n.id) { Animatable(0f) }
    LaunchedEffect(n.id, ripe) {
        if (ripe && glowedRipe.add(n.id)) {
            println("TREEFRUIT glow ${n.id} 1 s")
            glow.animateTo(1f, tween(250))
            glow.animateTo(0f, tween(750))
        }
    }
    val label = n.fruit
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val full = size.minDimension * 0.46f
            val r = full * scale
            val g = glow.value
            if (g > 0f) {
                val gr = r * 2.2f
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFF59D).copy(alpha = 0.9f * g), Color(0xFFFFE082).copy(alpha = 0.45f * g), Color(0x00FFE082)),
                        c, gr,
                    ),
                    gr, c,
                )
            }
            when {
                bud -> {
                    // A closed grey bud on a little stalk, small in the large tap slot.
                    drawLine(Color(0xFF6D4C41), Offset(c.x, c.y - r * 2.2f), Offset(c.x, c.y - r * 1.05f), 2.5f * density)
                    val b = Path().apply {
                        moveTo(c.x, c.y - r * 1.35f)
                        quadraticTo(c.x + r * 1.15f, c.y, c.x, c.y + r * 1.35f)
                        quadraticTo(c.x - r * 1.15f, c.y, c.x, c.y - r * 1.35f)
                        close()
                    }
                    paperPath(b, Color(0xFFBDBDBD))
                }
                else -> {
                    val col = if (ripe) Color(0xFFE53935) else androidx.compose.ui.graphics.lerp(Color(0xFFAED581), Color(0xFFFFCA28), t)
                    drawLine(Color(0xFF6D4C41), Offset(c.x, c.y - r * 1.05f), Offset(c.x + r * 0.12f, c.y - r * 1.35f), 3f * density)
                    paperCircle(col, r, c)
                    paperOval(Color(0xFF66BB6A), Offset(c.x + r * 0.05f, c.y - r * 1.4f), Size(r * 0.6f, r * 0.3f))
                    if (ripe) paperCircle(Color.White.copy(alpha = 0.55f), r * 0.16f, Offset(c.x - r * 0.42f, c.y - r * 0.4f))
                }
            }
        }
        if (!bud) {
            val letters = n.branch == SkillBranch.TALKING
            Text(
                label,
                fontSize = (sizeDp * scale * (if (letters) 0.42f else 0.38f)).sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (ripe) Color.White else Color(0xFF3E2723),
                modifier = Modifier.alpha(if (ripe || grow > 0.15f) 1f else 0.8f),
            )
        }
    }
}
