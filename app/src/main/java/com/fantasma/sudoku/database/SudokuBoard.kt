package com.fantasma.sudoku.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sudoku_board_table")
data class SudokuBoard(
    @ColumnInfo(name = "board_difficulty")
    val boardDifficulty: Int = -1,

    @ColumnInfo(name = "starting_board")
    val startingBoard: String = "0",

    @ColumnInfo(name = "solved_board")
    val solvedBoard: String = "0",

    @ColumnInfo(name = "users_board") //If does not contain 0 then board is complete
    var usersBoard: String = "0",

    @ColumnInfo(name = "board_used")
    var boardUsed: Boolean = false
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var boardId: Long = 0L

    @ColumnInfo(name = "time_on_board")
    var time: Long = 0L

    @ColumnInfo(name = "created_board")
    var createdBoard: Long = -1L
}