package com.fantasma.sudoku.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.fantasma.sudoku.database.SudokuBoard
import com.fantasma.sudoku.database.SudokuBoardDatabaseDao
import com.fantasma.sudoku.game.SudokuBoardGenerator
import com.fantasma.sudoku.game.SudokuGame
import com.fantasma.sudoku.util.Constant.BOARD_BACKLOG
import com.fantasma.sudoku.util.Constant.EASY
import com.fantasma.sudoku.util.Constant.EXISTING_BOARD
import com.fantasma.sudoku.util.Constant.EXPERT
import com.fantasma.sudoku.util.Constant.HARD
import com.fantasma.sudoku.util.Constant.INTERMEDIATE
import com.fantasma.sudoku.util.Constant.SOLVER
import kotlinx.coroutines.*
import java.lang.Exception

class MainViewModel : ViewModel() {
    val sudokuGame = SudokuGame()

    private lateinit var database: SudokuBoardDatabaseDao

    private val generatorJob = Job()
    private val generatorScope = CoroutineScope(Dispatchers.Default + generatorJob)

    private val uiJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + uiJob)

    fun setDatabase(databaseDao: SudokuBoardDatabaseDao) {
        database = databaseDao
        uiScope.launch {
           // nuke()
           beginGeneratingBoards()
        }

    }

    private suspend fun beginGeneratingBoards() {
        val sudokuBoardGenerator = SudokuBoardGenerator()
        generatorScope.launch {
            var difficulty = EASY
            var standByEasyBoardsCreated = getNumberOfBoards(EASY)
            var standByIntermediateBoardsCreated = getNumberOfBoards(INTERMEDIATE)
            var standByHardBoardsCreated = getNumberOfBoards(HARD)
            var standByExpertBoardsCreated = getNumberOfBoards(EXPERT)

            while(standByEasyBoardsCreated < BOARD_BACKLOG ||
                standByIntermediateBoardsCreated < BOARD_BACKLOG ||
                standByHardBoardsCreated < BOARD_BACKLOG ||
                standByExpertBoardsCreated < BOARD_BACKLOG
            ) {
                val startTime = System.currentTimeMillis()
                val shouldContinue = when (difficulty) {
                    EASY -> standByEasyBoardsCreated >= BOARD_BACKLOG
                    INTERMEDIATE -> standByIntermediateBoardsCreated >= BOARD_BACKLOG
                    HARD -> standByHardBoardsCreated >= BOARD_BACKLOG
                    EXPERT -> standByExpertBoardsCreated >= BOARD_BACKLOG
                    else -> true
                }
                if(shouldContinue) {
                    difficulty++
                    if(difficulty > EXPERT) difficulty = EASY
                    continue
                }
                val board = sudokuBoardGenerator.generateBoard(difficulty)
                insertBoard(board)
                when(difficulty) {
                    EASY -> standByEasyBoardsCreated++
                    INTERMEDIATE -> standByIntermediateBoardsCreated++
                    HARD -> standByHardBoardsCreated++
                    EXPERT -> standByExpertBoardsCreated++
                }
                val endTime = System.currentTimeMillis() - startTime
                Log.i("SUDOKU", "$difficulty <- difficulty created in $endTime ms")
                difficulty++
            }
        }
    }

    private suspend fun insertBoard(board: SudokuBoard) {
        withContext(Dispatchers.IO) {
            database.insert(board)
        }
    }

    private suspend fun getNumberOfBoards(difficulty: Int) : Int {
        return withContext(Dispatchers.IO) {
            database.getNumberOfBoards(difficulty)
        }
    }

    private suspend fun nuke() {
        withContext(Dispatchers.IO) {
            database.nukeTable()
        }
    }

    fun requestBoard(mode: Int) {
        uiScope.launch {
            sudokuGame.postBoard(
                when(mode) {
                    SOLVER -> getEmptyBoard()
                    else -> getNewBoard(mode)
                }
            )
        }
    }

    fun postBoard(id: Long, restart: Boolean) {
        uiScope.launch {
            val board = boardOfId(id)
            sudokuGame.postBoard(board ?: getEmptyBoard(), restart)
        }
    }

    fun saveBoard() {
        val board = sudokuGame.getSudokuBoard() ?: return
        uiScope.launch {
            if(board.createdBoard == -1L)
                board.createdBoard = getTotalCreatedBoards() + 1L
            updateBoard(board)
        }
    }

    private suspend fun updateBoard(board: SudokuBoard) {
        withContext(Dispatchers.IO) {
            database.update(board)
        }
    }

    private suspend fun getTotalCreatedBoards() : Long {
        return withContext(Dispatchers.IO) {
            database.getNumberOfCreatedBoards()
        }
    }

    private suspend fun boardOfId(id: Long) : SudokuBoard? {
        return withContext(Dispatchers.IO) {
            database.get(id)
        }
    }

    private suspend fun getEmptyBoard() : SudokuBoard {
        insertBoard(SudokuBoard())
        return getNewBoard(SOLVER)
    }

    private suspend fun getLastBoard() : SudokuBoard? {
        return withContext(Dispatchers.IO) {
            database.getLastBoard()
        }
    }

    private suspend fun getNewBoard(difficulty: Int) : SudokuBoard {
        return getNewGameFromDatabase(difficulty) ?: boardOfDifficulty(difficulty)
    }

    private suspend fun getNewGameFromDatabase(difficulty: Int) : SudokuBoard? {
        return withContext(Dispatchers.IO) {
            database.newGameOfDifficulty(difficulty)
        }
    }

    private suspend fun boardOfDifficulty(difficulty: Int) : SudokuBoard {
        return withContext(Dispatchers.Default) {
            var board = getNewGameFromDatabase(difficulty)
            while(board == null) {
                Thread.sleep(1000)
                board = getNewGameFromDatabase(difficulty)
            }
            board!!
        }
    }

}