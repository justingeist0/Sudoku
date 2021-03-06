package com.fantasmaplasma.sudoku.ui.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import com.fantasmaplasma.sudoku.database.SudokuBoard
import com.fantasmaplasma.sudoku.database.SudokuBoardDatabaseDao
import com.fantasmaplasma.sudoku.game.SudokuBoardGenerator
import com.fantasmaplasma.sudoku.game.SudokuGame
import com.fantasmaplasma.sudoku.util.Constant.BOARD_BACKLOG
import com.fantasmaplasma.sudoku.util.Constant.EASY
import com.fantasmaplasma.sudoku.util.Constant.EXPERT
import com.fantasmaplasma.sudoku.util.Constant.HARD
import com.fantasmaplasma.sudoku.util.Constant.INTERMEDIATE
import com.fantasmaplasma.sudoku.util.Constant.SOLVER
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdCallback
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.*

class MainViewModel : ViewModel() {
    val sudokuGame = SudokuGame()
    private lateinit var database: SudokuBoardDatabaseDao
    private var generatorJob: Job? = null
    private val uiJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + uiJob)
    private lateinit var rewardedAd: RewardedAd

    fun setDatabase(databaseDao: SudokuBoardDatabaseDao) {
        database = databaseDao
        uiScope.launch {
           beginGeneratingBoards()
        }
    }

    private suspend fun beginGeneratingBoards() {
        if(generatorJob?.isActive == true) return
        generatorJob = GlobalScope.launch(Dispatchers.Default) {
            val sudokuBoardGenerator = SudokuBoardGenerator()

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

    fun requestBoard(mode: Int) {
        uiScope.launch {
            sudokuGame.postBoard(
                when(mode) {
                    SOLVER -> getEmptyBoard()
                    else -> getNewBoard(mode)
                }
            )
            resumeTimerIfNotSolverMode()
        }
    }

    fun postBoard(id: Long, restart: Boolean, setHelpText: (Int) -> Unit){
        uiScope.launch {
            val board = boardOfId(id)
            setHelpText(board?.boardDifficulty ?: SOLVER)
            sudokuGame.postBoard(board ?: getEmptyBoard(), restart)
            resumeTimerIfNotSolverMode()
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
            database.getLastCreatedBoard()?.createdBoard ?: 0L
        }
    }

    private suspend fun boardOfId(id: Long) : SudokuBoard? {
        return withContext(Dispatchers.IO) {
            database.get(id)
        }
    }

    private suspend fun getEmptyBoard() : SudokuBoard {
        insertBoard(SudokuBoard(boardDifficulty = SOLVER))
        return getNewBoard(SOLVER)
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
                delay(1000)
                board = getNewGameFromDatabase(difficulty)
            }
            board
        }!!
    }

    fun resumeTimerIfNotSolverMode() {
        val board = sudokuGame.getSudokuBoard() ?: return
        if(board.boardDifficulty > SOLVER)
            sudokuGame.startTimer()
    }

    private val adLoadCallback = object: RewardedAdLoadCallback(){}
    private val adCallback = object: RewardedAdCallback() {
        override fun onUserEarnedReward(p0: RewardItem) {
            adFinished()
        }
        override fun onRewardedAdFailedToShow(p0: AdError?) {
            adFinished()
        }
    }

    fun initAd(activity: Activity) {
        rewardedAd = RewardedAd(activity, "ca-app-pub-1475221072762577/1769151783")
        //rewardedAd = RewardedAd(activity, "ca-app-pub-3940256099942544/5224354917") //Test ad
        rewardedAd.loadAd(AdRequest.Builder().build(), adLoadCallback)
    }

    fun showAd(activity: Activity) {
        rewardedAd.show(activity, adCallback)
    }

    fun adFinished() {
        sudokuGame.adShown = true
        sudokuGame.setAnswerForSelected()
    }
}