package com.fantasmaplasma.sudoku.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.fantasmaplasma.sudoku.database.SudokuBoard
import com.fantasmaplasma.sudoku.database.SudokuBoardDatabaseDao
import kotlinx.coroutines.*

class PortfolioViewModel : ViewModel() {
    private lateinit var database: SudokuBoardDatabaseDao
    lateinit var boardLiveData: LiveData<List<SudokuBoard>>

    val uiScope = CoroutineScope(Dispatchers.Main)

    suspend fun setDatabase(database: SudokuBoardDatabaseDao) {
        this.database = database
        boardLiveData = getBoardsLiveData()
    }

    private suspend fun getBoardsLiveData() : LiveData<List<SudokuBoard>>{
        return withContext(Dispatchers.IO) {
            database.getAllBoards()
        }
    }

    fun deleteBoard(boardId: Long) {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                database.deleteById(boardId)
            }
        }
    }

}