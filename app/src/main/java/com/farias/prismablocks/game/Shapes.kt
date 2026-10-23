package com.farias.prismablocks.game

/** A single cell coordinate (row, column). */
data class Cell(val r: Int, val c: Int)

/** A block shape made of cells, normalized so the top-left of its bounding box is (0, 0). */
class Shape(val id: Int, val cells: List<Cell>, val weight: Int) {
    val rows: Int = cells.maxOf { it.r } + 1
    val cols: Int = cells.maxOf { it.c } + 1
    val size: Int get() = cells.size
}

/** A shape with a color (1..7) — what the player drags. */
data class Piece(val shape: Shape, val color: Int)

object Shapes {

    private val defs: List<Pair<Int, Array<String>>> = listOf(
        // singles and bars
        2 to arrayOf("#"),
        3 to arrayOf("##"),
        3 to arrayOf("#", "#"),
        3 to arrayOf("###"),
        3 to arrayOf("#", "#", "#"),
        2 to arrayOf("####"),
        2 to arrayOf("#", "#", "#", "#"),
        1 to arrayOf("#####"),
        1 to arrayOf("#", "#", "#", "#", "#"),
        // squares and rectangles
        4 to arrayOf("##", "##"),
        1 to arrayOf("###", "###", "###"),
        1 to arrayOf("###", "###"),
        1 to arrayOf("##", "##", "##"),
        // small corners (3 cells)
        2 to arrayOf("##", "#."),
        2 to arrayOf("##", ".#"),
        2 to arrayOf("#.", "##"),
        2 to arrayOf(".#", "##"),
        // L pieces (4 cells)
        1 to arrayOf("#.", "#.", "##"),
        1 to arrayOf(".#", ".#", "##"),
        1 to arrayOf("##", "#.", "#."),
        1 to arrayOf("##", ".#", ".#"),
        1 to arrayOf("###", "#.."),
        1 to arrayOf("###", "..#"),
        1 to arrayOf("#..", "###"),
        1 to arrayOf("..#", "###"),
        // T pieces
        1 to arrayOf("###", ".#."),
        1 to arrayOf(".#.", "###"),
        1 to arrayOf("#.", "##", "#."),
        1 to arrayOf(".#", "##", ".#"),
        // S / Z pieces
        1 to arrayOf(".##", "##."),
        1 to arrayOf("##.", ".##"),
        1 to arrayOf("#.", "##", ".#"),
        1 to arrayOf(".#", "##", "#."),
        // big corners (5 cells)
        1 to arrayOf("###", "#..", "#.."),
        1 to arrayOf("###", "..#", "..#"),
        1 to arrayOf("#..", "#..", "###"),
        1 to arrayOf("..#", "..#", "###"),
        // diagonals
        1 to arrayOf("#.", ".#"),
        1 to arrayOf(".#", "#."),
    )

    val all: List<Shape> = defs.mapIndexed { index, (weight, rows) ->
        val cells = rows.flatMapIndexed { r, line ->
            line.mapIndexedNotNull { c, ch -> if (ch == '#') Cell(r, c) else null }
        }
        Shape(index, cells, weight)
    }

    fun byId(id: Int): Shape? = all.getOrNull(id)
}
