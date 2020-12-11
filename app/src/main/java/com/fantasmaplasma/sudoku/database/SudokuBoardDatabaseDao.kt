package com.fantasmaplasma.sudoku.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SudokuBoardDatabaseDao {

    @Insert
    fun insert(board: SudokuBoard)

    @Update
    fun update(board: SudokuBoard)

    @Query("DELETE from sudoku_board_table WHERE _id = :key")
    fun deleteById(key: Long)

    @Query("SELECT * from sudoku_board_table WHERE _id = :key")
    fun get(key: Long): SudokuBoard?

    @Query("SELECT COUNT(*) from sudoku_board_table WHERE board_difficulty = :difficulty and board_used = 0")
    fun getNumberOfBoards(difficulty: Int): Int

    @Query("SELECT * from sudoku_board_table WHERE board_used = 1 ORDER BY created_board DESC limit 1")
    fun getLastCreatedBoard(): SudokuBoard?

    @Query("SELECT * from sudoku_board_table WHERE board_used = 1 ORDER BY created_board DESC")
    fun getAllBoards(): LiveData<List<SudokuBoard>>
    //Live data so boards are updated automatically

    @Query("DELETE from sudoku_board_table")
    fun nukeTable()

    @Query("SELECT * from sudoku_board_table WHERE board_difficulty = :difficulty and board_used = 0 limit 1")
    fun newGameOfDifficulty(difficulty: Int) : SudokuBoard?

    @Query("SELECT * from sudoku_board_table ORDER BY _id DESC limit 1")
    fun getLastBoard() : SudokuBoard?

}