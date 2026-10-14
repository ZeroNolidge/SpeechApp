package com.zeronolidge.speechapp.logic

import kotlin.random.Random

/** Screen directions. Rows grow downwards, like the grid on screen. */
enum class Dir(val dRow: Int, val dCol: Int, val degrees: Float) {
    UP(-1, 0, 0f), RIGHT(0, 1, 90f), DOWN(1, 0, 180f), LEFT(0, -1, 270f);

    fun turnLeft(): Dir = entries[(ordinal + 3) % 4]
    fun turnRight(): Dir = entries[(ordinal + 1) % 4]
}

data class Cell(val row: Int, val col: Int) {
    fun step(d: Dir) = Cell(row + d.dRow, col + d.dCol)
}

/**
 * Commands a child can place in the program line.
 * "Arrow" commands move in a screen direction (easy mode).
 * FORWARD / TURN_LEFT / TURN_RIGHT are relative to the robot, like the Cubetto toy.
 */
enum class Cmd(val symbol: String, val spoken: String) {
    UP("⬆️", "up"), DOWN("⬇️", "down"), LEFT("⬅️", "left"), RIGHT("➡️", "right"),
    FORWARD("⬆️", "forward"), TURN_LEFT("↩️", "turn left"), TURN_RIGHT("↪️", "turn right"),
}

data class RobotState(val pos: Cell, val facing: Dir)

data class RobotLevel(
    val rows: Int,
    val cols: Int,
    val start: Cell,
    val startFacing: Dir,
    val goal: Cell,
    val blocks: Set<Cell> = emptySet(),
    /** True for the Cubetto-style forward/turn controls, false for screen arrows. */
    val relative: Boolean = false,
) {
    fun inside(c: Cell) = c.row in 0 until rows && c.col in 0 until cols
    fun open(c: Cell) = inside(c) && c !in blocks
}

sealed interface StepResult {
    data class Moved(val state: RobotState) : StepResult
    data class Turned(val state: RobotState) : StepResult
    /** Tried to walk off the board or into a tree. State is unchanged. */
    data class Bumped(val state: RobotState, val into: Cell) : StepResult
}

object RobotSim {
    fun step(level: RobotLevel, s: RobotState, cmd: Cmd): StepResult {
        fun walk(d: Dir): StepResult {
            val next = s.pos.step(d)
            val faced = s.copy(facing = d)
            return if (level.open(next)) StepResult.Moved(RobotState(next, d))
            else StepResult.Bumped(faced, next)
        }
        return when (cmd) {
            Cmd.UP -> walk(Dir.UP)
            Cmd.DOWN -> walk(Dir.DOWN)
            Cmd.LEFT -> walk(Dir.LEFT)
            Cmd.RIGHT -> walk(Dir.RIGHT)
            Cmd.FORWARD -> walk(s.facing)
            Cmd.TURN_LEFT -> StepResult.Turned(s.copy(facing = s.facing.turnLeft()))
            Cmd.TURN_RIGHT -> StepResult.Turned(s.copy(facing = s.facing.turnRight()))
        }
    }

    /** Runs a whole program; returns every intermediate state and whether the goal was reached. */
    fun run(level: RobotLevel, program: List<Cmd>): Pair<List<StepResult>, Boolean> {
        var s = RobotState(level.start, level.startFacing)
        val out = mutableListOf<StepResult>()
        for (c in program) {
            val r = step(level, s, c)
            out += r
            if (r is StepResult.Bumped) return out to false
            s = when (r) {
                is StepResult.Moved -> r.state
                is StepResult.Turned -> r.state
                is StepResult.Bumped -> r.state
            }
        }
        return out to (s.pos == level.goal)
    }

    /** The commands a child can use on this level. */
    fun palette(level: RobotLevel) = if (level.relative) listOf(Cmd.FORWARD, Cmd.TURN_LEFT, Cmd.TURN_RIGHT)
    else listOf(Cmd.UP, Cmd.DOWN, Cmd.LEFT, Cmd.RIGHT)

    /** Where the robot ends up after [program], or null if it bumps into something on the way. */
    fun stateAfter(level: RobotLevel, program: List<Cmd>): RobotState? {
        var s = RobotState(level.start, level.startFacing)
        for (c in program) {
            s = when (val r = step(level, s, c)) {
                is StepResult.Moved -> r.state
                is StepResult.Turned -> r.state
                is StepResult.Bumped -> return null
            }
        }
        return s
    }

    /** A shortest list of commands from [from] to the goal (breadth first search), or null. */
    fun solve(level: RobotLevel, from: RobotState = RobotState(level.start, level.startFacing)): List<Cmd>? {
        val cmds = palette(level)
        // Facing doesn't matter for arrows, so ignore it to keep the search small.
        fun key(s: RobotState) = if (level.relative) s else s.copy(facing = Dir.UP)
        val prev = HashMap<RobotState, Pair<RobotState, Cmd>?>()
        prev[key(from)] = null
        var frontier = listOf(from)
        while (frontier.isNotEmpty()) {
            val next = mutableListOf<RobotState>()
            for (s in frontier) {
                if (s.pos == level.goal) {
                    val path = mutableListOf<Cmd>()
                    var k = key(s)
                    while (true) {
                        val p = prev[k] ?: break
                        path += p.second
                        k = key(p.first)
                    }
                    return path.reversed()
                }
                for (c in cmds) {
                    val r = step(level, s, c)
                    val ns = when (r) {
                        is StepResult.Moved -> r.state
                        is StepResult.Turned -> r.state
                        is StepResult.Bumped -> continue
                    }
                    if (key(ns) !in prev) {
                        prev[key(ns)] = s to c
                        next += ns
                    }
                }
            }
            frontier = next
        }
        return null
    }

    /** Shortest number of moves using screen arrows (breadth first search), or null if unreachable. */
    fun shortestArrowPath(level: RobotLevel): Int? {
        val seen = mutableSetOf(level.start)
        var frontier = listOf(level.start)
        var depth = 0
        while (frontier.isNotEmpty()) {
            if (level.goal in frontier) return depth
            depth++
            frontier = frontier.flatMap { c -> Dir.entries.map { c.step(it) } }
                .filter { level.open(it) && seen.add(it) }
        }
        return null
    }
}

object RobotLevels {
    private val e = emptySet<Cell>()

    /** Hand-made levels that grow slowly in difficulty. */
    val easy: List<RobotLevel> = listOf(
        RobotLevel(3, 3, Cell(1, 0), Dir.RIGHT, Cell(1, 2), e),
        RobotLevel(3, 3, Cell(2, 1), Dir.UP, Cell(0, 1), e),
        RobotLevel(3, 3, Cell(2, 0), Dir.UP, Cell(0, 2), e),
        RobotLevel(4, 4, Cell(0, 0), Dir.RIGHT, Cell(3, 3), setOf(Cell(1, 1), Cell(2, 2))),
        RobotLevel(4, 4, Cell(3, 0), Dir.UP, Cell(0, 3), setOf(Cell(2, 0), Cell(1, 2), Cell(3, 2))),
        RobotLevel(4, 5, Cell(1, 0), Dir.RIGHT, Cell(1, 4), setOf(Cell(1, 2), Cell(0, 2))),
    )

    val robot: List<RobotLevel> = listOf(
        RobotLevel(3, 3, Cell(1, 0), Dir.RIGHT, Cell(1, 2), e, relative = true),
        RobotLevel(3, 3, Cell(2, 0), Dir.UP, Cell(0, 0), e, relative = true),
        RobotLevel(3, 3, Cell(2, 0), Dir.UP, Cell(0, 2), e, relative = true),
        RobotLevel(3, 3, Cell(0, 0), Dir.RIGHT, Cell(2, 2), setOf(Cell(1, 1)), relative = true),
        RobotLevel(4, 4, Cell(3, 0), Dir.RIGHT, Cell(0, 3), setOf(Cell(3, 2), Cell(1, 1)), relative = true),
        RobotLevel(4, 4, Cell(0, 0), Dir.DOWN, Cell(0, 3), setOf(Cell(0, 1), Cell(1, 1), Cell(0, 2)), relative = true),
    )

    /** A random solvable board, used once the hand-made levels are finished. */
    fun random(relative: Boolean, rnd: Random = Random.Default): RobotLevel {
        while (true) {
            val rows = 4
            val cols = 5
            val all = (0 until rows).flatMap { r -> (0 until cols).map { c -> Cell(r, c) } }.shuffled(rnd)
            val start = all[0]
            val goal = all.drop(1).first { (kotlin.math.abs(it.row - start.row) + kotlin.math.abs(it.col - start.col)) >= 3 }
            val blocks = all.filter { it != start && it != goal }.take(rnd.nextInt(2, 5)).toSet()
            val lvl = RobotLevel(rows, cols, start, Dir.entries[rnd.nextInt(4)], goal, blocks, relative)
            val len = RobotSim.shortestArrowPath(lvl)
            if (len != null && len in 3..6) return lvl
        }
    }
}
