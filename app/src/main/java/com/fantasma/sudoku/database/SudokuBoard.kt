package com.fantasma.sudoku.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sudoku_board_table")
data class SudokuBoard (
    @PrimaryKey(autoGenerate = true)
    var boardId: Long = 0L,

    @ColumnInfo(name = "board_difficulty")
    val boardDifficulty: Int,

    @ColumnInfo(name = "board")
    val board: String
    //162 characters first 81 ex: "900600301..." loads top left to bottom right zeros are editable non-zeros are starting cells
    //Next 81 is solution ex: "987654321..."
)