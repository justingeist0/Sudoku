package com.fantasma.sudoku.game

import android.util.Log
import com.fantasma.sudoku.database.SudokuBoard
import com.fantasma.sudoku.util.Constant
import com.fantasma.sudoku.util.Constant.GRID_SIZE
import com.fantasma.sudoku.util.Constant.SOLVER

class Board() {
    val cells = List(GRID_SIZE * GRID_SIZE) { i -> Cell(i / GRID_SIZE, i % GRID_SIZE, 0) }
    private var currentBoard: SudokuBoard? = null

    fun getCell(row: Int, col: Int) = cells[getIdx(row, col)]

    fun setBoard(newBoard: SudokuBoard, restart: Boolean = false) {
        currentBoard = newBoard

        if (newBoard.usersBoard.length < GRID_SIZE || newBoard.boardDifficulty == SOLVER) {
            //Solver mode (Empty sudoku board)
            cells.forEach {
                it.value = 0
                it.isStartingCell = false
                it.notes.clear()
            }
            return
        }

        if(newBoard.startingBoard.length == GRID_SIZE* GRID_SIZE) {
            val startingBoard = Array(cells.size) { i ->
                Character.getNumericValue(newBoard.startingBoard[i])
            }
            for (i in cells.indices) {
                cells[i].value = startingBoard[i]
                cells[i].isStartingCell = startingBoard[i] != 0
                cells[i].notes.clear()
            }
        }

        if(restart) return

        val userBoard = Array(cells.size) { i ->
            Character.getNumericValue(newBoard.usersBoard[i])
        }
        for (i in cells.indices) {
            if(cells[i].value == 0 && userBoard[i] > 0) {
                cells[i].value = userBoard[i]
                checkConflictingCells(cells[i], 0)
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
        //Calculate conflicts with cells
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

}