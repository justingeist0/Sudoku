package com.fantasma.sudoku.game

import androidx.lifecycle.MutableLiveData
import com.fantasma.sudoku.util.Constant.SQRT_SIZE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SudokuGame {

    val selectedCellLiveData = MutableLiveData<Pair<Int, Int>>()
    val cellsLiveData = MutableLiveData<List<Cell>>()
    val isTakingNotesLiveData = MutableLiveData<Boolean>()
    val highlightedKeysLiveData = MutableLiveData<Set<Int>>()
    val gameWon = MutableLiveData<Boolean>(false)

    private var filledIn = 0
    private var selectedRow = -1
    private var selectedCol = -1
    private var isTakingNotes = false

    private val board: Board

    init {
        val cells = List(9 * 9) {i ->
            println(i%9)
            Cell(i / 9, i % 9, 0)}
        board = Board(9, cells)

        selectedCellLiveData.postValue(Pair(selectedRow, selectedCol))
        cellsLiveData.postValue(board.cells)
        isTakingNotesLiveData.postValue(isTakingNotes)
    }

    fun handleInput(number: Int) {
        if (selectedRow == -1 || selectedCol == -1) return
        val cell = board.getCell(selectedRow, selectedCol)
        if (cell.isStartingCell) return

        if (isTakingNotes) {
            if (cell.notes.contains(number)) {
                cell.notes.remove(number)
            } else {
                cell.notes.add(number)
            }
            highlightedKeysLiveData.postValue(cell.notes)
        } else {
            val prevValue = cell.value
            if(prevValue != number) {
                if(cell.value == 0) {
                    filledIn++
                }
                cell.value = number
                cell.conflictingCells = 0
                //Calculate conflicts with cells
                board.cells.forEach { conflictingCell ->
                    val r = conflictingCell.row
                    val c = conflictingCell.col

                    if (r == selectedRow || c == selectedCol || r / SQRT_SIZE == selectedRow / SQRT_SIZE && c / SQRT_SIZE == selectedCol / SQRT_SIZE) {
                        if(conflictingCell.value == number) { //Will always execute for selected cell
                            conflictingCell.conflictingCells++
                            cell.conflictingCells++
                        } else if (conflictingCell.value == prevValue) {
                            conflictingCell.conflictingCells--
                        }
                    }
                }
                cell.conflictingCells -= 2 //Selected cell does not conflict with itself

                if(filledIn == 81) {
                    checkIfBoardComplete()
                }
            } else {
                return
            }
        }
        cellsLiveData.postValue(board.cells)
    }

    private fun checkIfBoardComplete() {
        board.cells.forEach {cell ->
            if(cell.conflictingCells != 0) return
        }
        gameWon.postValue(true)
    }

    fun updateSelectedCell(row: Int, col: Int) {

        if(row == -1 || col == -1) {
            selectedRow = row
            selectedCol = col
            selectedCellLiveData.postValue(Pair(row, col))
            return
        }

        val cell = board.getCell(row, col)

        if (!cell.isStartingCell) {
            selectedRow = row
            selectedCol = col
            selectedCellLiveData.postValue(Pair(row, col))

            if (isTakingNotes) {
                highlightedKeysLiveData.postValue(cell.notes)
            }
        }
    }

    fun changeNoteTakingState() {
        isTakingNotes = !isTakingNotes
        isTakingNotesLiveData.postValue(isTakingNotes)

        val curNotes = if (isTakingNotes) {
            board.getCell(selectedRow, selectedCol).notes
        } else {
            setOf<Int>()
        }
        highlightedKeysLiveData.postValue(curNotes)
    }

    fun delete() {
        val cell = board.getCell(selectedRow, selectedCol)
        if (isTakingNotes) {
            cell.notes.clear()
            highlightedKeysLiveData.postValue(setOf())
        } else {
            cell.value = 0
        }
        cellsLiveData.postValue(board.cells)
    }

    fun createBoard(difficulty: Int) {
        val generator = SudokuBoardGenerator()

        val scope = CoroutineScope(Job())
        scope.launch(Dispatchers.Default) {
            generator.generateBoard(difficulty)
        }
    }

    fun getValue(row: Int, column: Int) : Int = if(row == -1 || column == -1) 0 else board.getCell(row, column).value


}
