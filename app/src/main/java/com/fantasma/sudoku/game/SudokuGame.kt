package com.fantasma.sudoku.game

import androidx.lifecycle.MutableLiveData
import com.fantasma.sudoku.database.SudokuBoard
import com.fantasma.sudoku.util.Constant.EASY
import com.fantasma.sudoku.util.Constant.SQRT_SIZE
import kotlinx.coroutines.*

class SudokuGame {
    val selectedCellLiveData = MutableLiveData<Pair<Int, Int>>()
    val cellsLiveData = MutableLiveData<List<Cell>>()
    val isTakingNotesLiveData = MutableLiveData<Boolean>()
    val highlightedKeysLiveData = MutableLiveData<Set<Int>>()
    val gameWonLiveData = MutableLiveData<Boolean>()
    val gameIdLiveData = MutableLiveData<Long>()
    val undoBtnEnabledLiveData = MutableLiveData<Boolean>()
    val redoBtnEnabledLiveData = MutableLiveData<Boolean>()
    val deleteBtnEnabledLiveData = MutableLiveData<Boolean>()
    val helpAvailableLiveData = MutableLiveData<Boolean>()
    val headerLabelLiveData = MutableLiveData<Int>()

    private val actions: MutableList<Action> = mutableListOf()
    private var actionIdx = -1

    private var filledIn = 0
    private var selectedRow = -1
    private var selectedCol = -1

    private val board: Board = Board()

    init {
        initGame()
    }

    private fun initGame() {
        selectedRow = -1
        selectedCol = -1

        selectedCellLiveData.postValue(Pair(selectedRow, selectedCol))
        cellsLiveData.postValue(board.cells)
        isTakingNotesLiveData.postValue(false)
        highlightedKeysLiveData.postValue(setOf())
        gameWonLiveData.postValue(false)
        undoBtnEnabledLiveData.postValue(false)
        redoBtnEnabledLiveData.postValue(false)
        deleteBtnEnabledLiveData.postValue(false)
        helpAvailableLiveData.postValue(false)
        actions.clear()
        actionIdx = -1
        filledIn = 0
    }

    fun handleInput(number: Int) {
        if (selectedRow == -1 || selectedCol == -1) return
        val cell = board.getCell(selectedRow, selectedCol)
        if (cell.isStartingCell) return

        if (isTakingNotesLiveData.value!!) {
            if (cell.notes.contains(number))
                cell.notes.remove(number)
            else
                cell.notes.add(number)
            highlightedKeysLiveData.postValue(cell.notes)
        } else {
            val prevValue = cell.value
            if (prevValue != number) {
                if (cell.value == 0) {
                    filledIn++
                }

                cell.value = number
                board.checkConflictingCells(cell, prevValue)

                val cellIdx = board.getIdx(selectedRow, selectedCol)
                if (actionIdx < 0 || actions[actionIdx].idx != cellIdx) {
                    actionIdx++
                    actions.add(actionIdx, Action(prevValue, cellIdx))
                    updateButtonsEnabled()
                }

                if (filledIn == 81) {
                    checkIfBoardComplete()
                }
            } else {
                return
            }
            deleteBtnEnabledLiveData.postValue(true)
        }
        cellsLiveData.postValue(board.cells)
    }

    fun postBoard(sudokuBoard: SudokuBoard, restart: Boolean = false) {
        board.setBoard(sudokuBoard, restart)
        initGame()
        headerLabelLiveData.postValue(sudokuBoard.boardDifficulty)
        gameIdLiveData.postValue(sudokuBoard.boardId)
        board.cells.forEach { if (it.isStartingCell) filledIn++ }
    }

    private fun checkIfBoardComplete() {
        board.cells.forEach { cell ->
            if (cell.conflictingCells != 0) return
        }
        gameWonLiveData.postValue(true)
        gameIdLiveData.postValue(-1L)
    }

    fun updateSelectedCell(row: Int, col: Int) {
        if (row == -1 || col == -1) {
            selectedRow = row
            selectedCol = col
            selectedCellLiveData.postValue(Pair(row, col))
            deleteBtnEnabledLiveData.postValue(false)
            helpAvailableLiveData.postValue(false)
            return
        }

        val cell = board.getCell(row, col)

        selectedRow = row
        selectedCol = col
        selectedCellLiveData.postValue(Pair(row, col))
        deleteBtnEnabledLiveData.postValue(!cell.isStartingCell && cell.value > 0)
        helpAvailableLiveData.postValue(!cell.isStartingCell)

        if (!cell.isStartingCell && isTakingNotesLiveData.value!!) {
            highlightedKeysLiveData.postValue(cell.notes)
        } else {
            highlightedKeysLiveData.postValue(setOf())
        }
    }

    fun undo() {
        val undoAction = actions[actionIdx]
        val cell = board.cells[undoAction.idx]
        val prevValue = cell.value

        cell.value = undoAction.valuePosted
        undoAction.valuePosted = prevValue

        board.checkConflictingCells(cell, prevValue)

        actionIdx--

        cellsLiveData.postValue(board.cells)
        updateButtonsEnabled()
    }

    fun redo() {
        actionIdx++

        val redoAction = actions[actionIdx]
        val cell = board.cells[redoAction.idx]
        val prevValue = cell.value

        cell.value = redoAction.valuePosted
        redoAction.valuePosted = prevValue

        board.checkConflictingCells(cell, prevValue)

        cellsLiveData.postValue(board.cells)
        updateButtonsEnabled()
    }

    fun changeNoteTakingState() {
        val isNoteTaking = !isTakingNotesLiveData.value!!
        isTakingNotesLiveData.postValue(isNoteTaking)

        if (isNoteTaking && selectedCol != -1 && selectedRow != -1) {
            val currentNotes = board.getCell(selectedRow, selectedCol).notes
            highlightedKeysLiveData.postValue(currentNotes)
        } else {
            highlightedKeysLiveData.postValue(setOf())
        }
    }

    fun delete() {
        val cellIdx = board.getIdx(selectedRow, selectedCol)
        val cell = board.cells[cellIdx]

        actionIdx++
        actions.add(actionIdx, Action(cell.value, cellIdx))

        board.cells.forEach { conflictingCell ->
            val r = conflictingCell.row
            val c = conflictingCell.col

            if (r == selectedRow || c == selectedCol || r / SQRT_SIZE == selectedRow / SQRT_SIZE && c / SQRT_SIZE == selectedCol / SQRT_SIZE) {
                if (conflictingCell.value == cell.value) { //Check if this delete makes less conflicting cells
                    conflictingCell.conflictingCells--
                }
            }
        }
        cell.conflictingCells = 0
        cell.value = 0

        cellsLiveData.postValue(board.cells)
        updateButtonsEnabled()
    }

    fun setAnswerForSelected() {

    }

    fun solveInStyle() {
        val boardValues = Array(81) { i -> board.cells[i].value }

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            SudokuBoardGenerator.solve(boardValues) { idx ->
                board.cells[idx].apply {
                    value = boardValues[idx]
                    conflictingCells = 0
                }
                cellsLiveData.postValue(board.cells)
                Thread.sleep(20)
            }
        }

    }

    fun getSudokuBoard(): SudokuBoard? {
        return board.getSudokuBoard()
    }

    private fun updateButtonsEnabled() {
        undoBtnEnabledLiveData.postValue(actionIdx >= 0)
        redoBtnEnabledLiveData.postValue(actionIdx < actions.size - 1)

        val cell = getSelectedCell()
        val canDelete = cell != null && !cell.isStartingCell && cell.value > 0
        deleteBtnEnabledLiveData.postValue(canDelete)
    }

    private fun getSelectedCell(): Cell? {
        return if (selectedRow == -1 || selectedCol == -1) null
        else board.getCell(selectedRow, selectedCol)
    }

    inner class Action(var valuePosted: Int, val idx: Int)
}
