package com.fantasmaplasma.sudoku.game

class Cell(
    val row: Int,
    val col: Int,
    var value: Int,
    var isStartingCell: Boolean = false,
    var conflictingCells: Int = 0,
    var notes: MutableSet<Int> = mutableSetOf()
)