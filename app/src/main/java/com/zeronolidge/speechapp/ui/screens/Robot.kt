package com.zeronolidge.speechapp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Cell
import com.zeronolidge.speechapp.logic.Cmd
import com.zeronolidge.speechapp.logic.Dir
import com.zeronolidge.speechapp.logic.RobotLevel
import com.zeronolidge.speechapp.logic.RobotLevels
import com.zeronolidge.speechapp.logic.RobotSim
import com.zeronolidge.speechapp.logic.RobotState
import com.zeronolidge.speechapp.logic.StepResult
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.RoundIcon
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_PROGRAM = 12

/**
 * Plan a path, then press Go and watch the robot follow it, one step at a time.
 * Just like a Cubetto: you build the whole plan first, then run it.
 */
@Composable
fun RobotGameScreen(relative: Boolean, back: () -> Unit) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    val levelKey = if (relative) "robot_turn" else "robot_easy"
    val handMade = if (relative) RobotLevels.robot else RobotLevels.easy
    var levelNo by remember { mutableIntStateOf(app.progress.level(levelKey)) }
    val level = remember(levelNo) { handMade.getOrNull(levelNo) ?: RobotLevels.random(relative) }

    val program = remember(levelNo) { mutableStateListOf<Cmd>() }
    var robot by remember(levelNo) { mutableStateOf(RobotState(level.start, level.startFacing)) }
    var runningStep by remember(levelNo) { mutableIntStateOf(-1) }
    var bumpAt by remember(levelNo) { mutableStateOf<Cell?>(null) }
    var won by remember(levelNo) { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val running = runningStep >= 0
    val wobble = remember { Animatable(0f) }

    val palette = if (relative) listOf(Cmd.FORWARD, Cmd.TURN_LEFT, Cmd.TURN_RIGHT)
    else listOf(Cmd.UP, Cmd.DOWN, Cmd.LEFT, Cmd.RIGHT)

    LaunchedEffect(levelNo) {
        app.speaker.say(
            if (relative && levelNo == 0) {
                "Help the robot get the cookie! Forward goes the way the robot is looking. The turn buttons spin the robot. Then press Go!"
            } else if (levelNo == 0) {
                "Help the robot get the cookie! Tap the arrows to make a plan, then press Go!"
            } else {
                "Help the robot get the cookie!"
            },
        )
    }
    DisposableEffect(Unit) { onDispose { job?.cancel() } }

    fun reset() {
        robot = RobotState(level.start, level.startFacing)
        runningStep = -1
        bumpAt = null
    }

    fun nextLevel() {
        val n = levelNo + 1
        app.progress.setLevel(levelKey, n)
        levelNo = n
    }

    fun go() {
        if (program.isEmpty()) {
            app.speaker.say("Tap the arrows to make a plan first!")
            return
        }
        job?.cancel()
        reset()
        job = scope.launch {
            var state = RobotState(level.start, level.startFacing)
            for ((i, cmd) in program.toList().withIndex()) {
                runningStep = i
                app.speaker.say(cmd.spoken)
                delay(450)
                when (val r = RobotSim.step(level, state, cmd)) {
                    is StepResult.Moved -> state = r.state
                    is StepResult.Turned -> state = r.state
                    is StepResult.Bumped -> {
                        robot = r.state
                        bumpAt = r.into
                        app.speaker.say(if (r.into in level.blocks) "Bump! A tree is in the way." else "Bump! That's the edge.")
                        repeat(3) {
                            wobble.animateTo(8f, tween(60))
                            wobble.animateTo(-8f, tween(60))
                        }
                        wobble.animateTo(0f, tween(60))
                        delay(1600)
                        app.speaker.say("Let's fix the plan and try again!")
                        reset()
                        return@launch
                    }
                }
                robot = state
                delay(550)
            }
            runningStep = -1
            if (state.pos == level.goal) {
                won = true
                app.reward(Progress.AREA_ROBOT, "Yum! The robot got the cookie! You did it!")
                delay(3500)
                nextLevel()
            } else {
                app.speaker.say("Hmm, the robot didn't reach the cookie. Let's change the plan!")
                delay(2500)
                reset()
            }
        }
    }

    KidScreen("Robot 🤖  ${levelNo + 1}", Kid.robot, back, "Help the robot get the cookie") {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // The board.
            BoxWithConstraints(
                Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                val cell: Dp = minOf(maxWidth / level.cols, maxHeight / level.rows)
                Board(level, robot, cell, bumpAt, won, Modifier.offset(x = wobble.value.dp))
            }

            // Plan and controls.
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("My plan:", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
                ProgramStrip(program, runningStep, Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    palette.forEach { cmd ->
                        Box(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 80.dp)
                                .shadow(4.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .clickable(enabled = !running && !won) {
                                    if (program.size < MAX_PROGRAM) {
                                        program.add(cmd)
                                        app.speaker.say(cmd.spoken)
                                    } else {
                                        app.speaker.say("The plan is full! Press Go.")
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(cmd.symbol, fontSize = 40.sp)
                                Text(cmd.spoken, fontSize = 14.sp, color = Kid.ink)
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundIcon("🗑️", Kid.soft, size = 64.dp, enabled = !running && program.isNotEmpty()) {
                        program.clear()
                        reset()
                        app.speaker.say("All gone!")
                    }
                    RoundIcon("⌫", Kid.soft, size = 64.dp, enabled = !running && program.isNotEmpty()) {
                        program.removeAt(program.lastIndex)
                        app.speaker.say("Oops, undo.")
                    }
                    RoundIcon("▶️", Kid.good, size = 88.dp, enabled = !running && !won) { go() }
                    RoundIcon("⏭️", Kid.soft, size = 64.dp, enabled = !running) {
                        app.speaker.say("A new puzzle!")
                        job?.cancel()
                        nextLevel()
                    }
                }
            }
        }
    }
}

@Composable
private fun Board(level: RobotLevel, robot: RobotState, cell: Dp, bumpAt: Cell?, won: Boolean, modifier: Modifier) {
    Column(
        modifier
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
    ) {
        for (r in 0 until level.rows) {
            Row {
                for (c in 0 until level.cols) {
                    val here = Cell(r, c)
                    Box(
                        Modifier
                            .size(cell)
                            .background(if ((r + c) % 2 == 0) Kid.grass else Kid.grassDark)
                            .then(if (here == bumpAt) Modifier.border(4.dp, Color(0xFFE53935)) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        val big = (cell.value * 0.55f).sp
                        when {
                            here == robot.pos -> RobotFace(robot.facing, cell, if (won) "🥳" else "🤖")
                            here == level.goal -> Text("🍪", fontSize = big)
                            here in level.blocks -> Text("🌳", fontSize = big)
                        }
                    }
                }
            }
        }
    }
}

/** The robot, with a little arrow showing which way it is looking. */
@Composable
private fun RobotFace(facing: Dir, cell: Dp, face: String) {
    Box(Modifier.size(cell), contentAlignment = Alignment.Center) {
        Text(face, fontSize = (cell.value * 0.5f).sp)
        val align = when (facing) {
            Dir.UP -> Alignment.TopCenter
            Dir.RIGHT -> Alignment.CenterEnd
            Dir.DOWN -> Alignment.BottomCenter
            Dir.LEFT -> Alignment.CenterStart
        }
        Text(
            "▲",
            fontSize = (cell.value * 0.2f).sp,
            color = Color(0xFFD84315),
            modifier = Modifier.align(align).padding(2.dp).rotate(facing.degrees),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgramStrip(program: List<Cmd>, runningStep: Int, modifier: Modifier) {
    FlowRow(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(MAX_PROGRAM) { i ->
            val cmd = program.getOrNull(i)
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            i == runningStep -> Kid.highlight
                            cmd != null -> Color(0xFFE8F5E9)
                            else -> Color(0xFFF5F5F5)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (cmd != null) Text(cmd.symbol, fontSize = 30.sp)
            }
        }
    }
}
