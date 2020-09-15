package com.fantasma.sudoku.game

import com.fantasma.sudoku.database.SudokuBoard
import com.fantasma.sudoku.util.Constant.EASY
import com.fantasma.sudoku.util.Constant.EXPERT
import com.fantasma.sudoku.util.Constant.GRID_SIZE
import com.fantasma.sudoku.util.Constant.HARD
import com.fantasma.sudoku.util.Constant.INTERMEDIATE
import java.util.Random

class SudokuBoardGenerator {
    private val board: Array<Int> = Array(GRID_SIZE * GRID_SIZE) {0}

    fun generateBoard(difficulty: Int) : SudokuBoard {
        val offset = random.nextInt(4)
        val emptyCells = when (difficulty) {
            EASY -> 25 + offset
            INTERMEDIATE -> 35 + offset
            HARD -> 45 + offset
            EXPERT -> 64 //Creates TRUE sudoku board (Min amount of hints for 1 solution)
            else -> 0
        }

        val solution = createSudokuSolution()//~1 in 6x10^21 chance of creating same board
        removeThisManyCells(emptyCells)//Remove X number of values from generated board board

        //Create string version to store in database
        val unsolvedBoard = board.joinToString("")
        val solvedBoard = solution.joinToString("")

        printBoard(solution)
        printBoard(board)

        //Return board to put in database which will be sent to SudokuGame through LiveData
        return SudokuBoard(difficulty, unsolvedBoard, solvedBoard, unsolvedBoard)
    }

    private fun createSudokuSolution() : Array<Int> {
        val availableDigits = mutableListOf<Int>()
        availableDigits.addAll(DIGITS_RANGE)

        for(i in board.indices) board[i] = 0

        //Fill up diagonal boxes randomly
        var start = 0
        for(n in 0 until 3) {
            for (i in start until start + 3) {
                for(j in 0..GRID_SIZE*2 step GRID_SIZE) {
                    board[i+j] = availableDigits.removeAt(random.nextInt(availableDigits.size))
                }
            }
            availableDigits.addAll(DIGITS_RANGE)
            start+=30//Next top left of box
        }

        //Create valid sudoku solution
        solve(board)

        //Return instance of solved puzzle
        return board.clone()
    }

    private fun removeThisManyCells(amount: Int) {
        var removed = 0
        var randomIdx = random.nextInt(GRID_SIZE*GRID_SIZE)
        var invalidCounter = 0
        val invalidCells = mutableSetOf<Int>()
        while(removed < amount) {
            if(randomIdx >= 81) randomIdx -= 81
            while(board[randomIdx] == 0 || invalidCells.contains(randomIdx)) {
                randomIdx++
                invalidCounter++
                if(invalidCounter > GRID_SIZE * GRID_SIZE) return
                if(randomIdx == 81) randomIdx = 0
            }
            val prevValue = board[randomIdx]
            board[randomIdx] = 0
            if(containsUniqueSolution(board)) {
                removed += 1
                randomIdx += random.nextInt(GRID_SIZE)+1
                invalidCells.add(randomIdx)
                invalidCounter = 0
            } else {
                board[randomIdx] = prevValue
                randomIdx++
            }
        }
    }


    companion object Solver {
        val random = Random()
        val DIGITS_RANGE = 1..9

        fun solve(board: Array<Int>, start: Int = 0) : Boolean {
            for (i in start until board.size) {
                if (board[i] == 0) {
                    val availableDigits = getAvailableDigits(i, board)
                    for (possibleAnswer in availableDigits) {
                        board[i] = possibleAnswer
                        if (solve(board, i + 1)) {
                            return true
                        }
                        board[i] = 0
                    }
                    return false
                }
            }
            return true
        }

        fun solve(board: Array<Int>, start: Int = 0,  updateDisplay: (idx: Int) -> Unit): Boolean {
            for (i in start until board.size) {
                if (board[i] == 0) {
                    val availableDigits = getAvailableDigits(i, board)
                    for (j in availableDigits) {
                        board[i] = j
                        updateDisplay(i)
                        if (solve(board, i + 1, updateDisplay)) {
                            return true
                        }
                        board[i] = 0
                        updateDisplay(i)
                    }
                    return false
                }
            }
            return true
        }

        private fun containsUniqueSolution(board: Array<Int>) : Boolean {
            for(i in board.indices) {
                if(board[i] == 0) {
                    val possibleDigits = getAvailableDigits(i, board)
                    if(possibleDigits.count() <= 1) continue
                    var solutions = 0
                    for(p in possibleDigits) {
                        val boardCopy = board.clone()
                        boardCopy[i] = p
                        if(solve(boardCopy)) {
                            solutions++
                            if(solutions > 1) {
                                return false
                            }
                        }
                    }
                }
            }
            return true
        }

        private fun getAvailableDigits(idx: Int, board: Array<Int>) : Iterable<Int> {
            val availableDigits = mutableSetOf<Int>()
            availableDigits.addAll(DIGITS_RANGE)

            truncateDigitsUsedInRow(idx, availableDigits, board)

            if(availableDigits.size > 0)
                truncateDigitsUsedInColumn(idx, availableDigits, board)

            if(availableDigits.size > 0)
                truncateDigitsUsedInBox(idx, availableDigits, board)

            return availableDigits.asIterable().shuffled()
        }

        private fun truncateDigitsUsedInRow(idx: Int, availableDigits: MutableSet<Int>, board: Array<Int>) {
            val start = (idx / 9) * 9
            for (i in start until start+9) {
                if(board[i] != 0) {
                    availableDigits.remove(board[i])
                }
            }
        }

        private fun truncateDigitsUsedInColumn(idx: Int, availableDigits: MutableSet<Int>, board: Array<Int>) {
            val start = idx % 9
            for (i in start until GRID_SIZE*GRID_SIZE step(9)) {
                if(board[i] != 0) {
                    availableDigits.remove(board[i])
                }
            }
        }

        private fun truncateDigitsUsedInBox(idx: Int, availableDigits: MutableSet<Int>, board: Array<Int>) {
            val start = ((idx / 3) * 3) - ((idx / 9) % 3) * 9 //Top left index of 3x3 grid

            for (i in start until start+3) {
                for(j in 0..GRID_SIZE*2 step(GRID_SIZE)) {
                    if (board[i+j] != 0) {
                        availableDigits.remove(board[i+j])
                    }
                }
            }
        }

        fun printBoard(board: Array<Int>) {
            for (i in board.indices) {
                if (i % 9 == 0) println("")
                print("${board[i]} ")
            }
            print("\n-=-=-=-=-=-=-=-=-")
        }
    }
}

