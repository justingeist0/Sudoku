package com.fantasmaplasma.sudoku.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SudokuBoard::class], version = 7, exportSchema = false)
abstract class SudokuBoardDatabase : RoomDatabase() {

    abstract val sleepDataBaseDao: SudokuBoardDatabaseDao

    companion object {

        @Volatile
        private var INSTANCE: SudokuBoardDatabase? = null

        fun getInstance(context: Context) : SudokuBoardDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        SudokuBoardDatabase::class.java,
                        "sudoku_board_table"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }

                return instance
            }
        }
    }
}