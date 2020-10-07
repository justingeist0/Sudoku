package com.fantasmaplasma.sudoku.game
import com.fantasmaplasma.sudoku.database.SudokuBoard
import com.fantasmaplasma.sudoku.util.Constant
import com.fantasmaplasma.sudoku.util.Constant.GRID_SIZE

class Board {
    val cells = List(GRID_SIZE * GRID_SIZE) { i -> Cell(i / GRID_SIZE, i % GRID_SIZE, 0) }
    private var currentBoard: SudokuBoard? = null

    fun getCell(row: Int, col: Int) = cells[getIdx(row, col)]

    fun setBoard(newBoard: SudokuBoard, restart: Boolean = false) {
        currentBoard = newBoard

        cells.forEach {
            it.value = 0
            it.isStartingCell = false
            it.notes.clear()
            it.conflictingCells = 0
        }

        if(newBoard.startingBoard.length == GRID_SIZE * GRID_SIZE) {
            val startingBoard = Array(cells.size) { i ->
                Character.getNumericValue(newBoard.startingBoard[i])
            }
            for (i in cells.indices) {
                cells[i].value = startingBoard[i]
                cells[i].isStartingCell = startingBoard[i] != 0
                cells[i].notes.clear()
            }
        }

        if(restart) {
            newBoard.time = 0L
            return
        }

        if(newBoard.usersBoard.length == GRID_SIZE * GRID_SIZE) {
            val userBoard = Array(cells.size) { i ->
                Character.getNumericValue(newBoard.usersBoard[i])
            }
            for (i in cells.indices) {
                if (cells[i].value == 0 && userBoard[i] > 0) {
                    cells[i].value = userBoard[i]
                    checkConflictingCells(cells[i], 0)
                }
            }
        }
    }

    fun getIdx(row: Int, col: Int) = row * GRID_SIZE + col

    fun getSudokuBoard(): SudokuBoard? {
        val usersBoardArray = Array(81) { i -> cells[i].value }
        currentBoard?.boardUsed = true
        currentBoard?.usersBoard = usersBoardArray.joinToString("")
        return currentBoard //Possible null because it is possible board is not yet generated/set
    }

    fun checkConflictingCells(cell: Cell, prevValue: Int) {
        cell.conflictingCells = 0
        cells.forEach { conflictingCell ->
            val r = conflictingCell.row
            val c = conflictingCell.col

            if (r == cell.row || c == cell.col || r / Constant.SQRT_SIZE == cell.row / Constant.SQRT_SIZE && c / Constant.SQRT_SIZE == cell.col / Constant.SQRT_SIZE) {
                if (conflictingCell.value == cell.value) { //Will ALWAYS execute for selected cell
                    conflictingCell.conflictingCells++
                    cell.conflictingCells++
                } else if (conflictingCell.value == prevValue) {
                    conflictingCell.conflictingCells--
                }
            }
        }
        cell.conflictingCells -= 2 //Selected cell does not conflict with itself!
    }

    fun difficulty() : Int? {
        return currentBoard?.boardDifficulty
    }

    fun setCorrectNumber(selectedRow: Int, selectedCol: Int) : Boolean {
        val idx = getIdx(selectedRow, selectedCol)
        val correctValueChar: Char = currentBoard?.solvedBoard?.get(idx) ?: return false
        val prevValue = cells[idx].value
        cells[idx].value =
            Character.getNumericValue(correctValueChar)
        checkConflictingCells(cells[idx], prevValue)
        return prevValue == 0
    }

    fun isNotComplete() : Boolean {
        cells.forEach { cell ->
            if (cell.conflictingCells != 0 || cell.value == 0) return true
        }
        return false
    }

    fun isComplete() : Boolean = !isNotComplete()

    fun setTime(time: Long) {
        currentBoard?.time = time
    }
}