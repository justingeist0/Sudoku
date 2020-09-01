package com.fantasma.sudoku.ui.main

import androidx.lifecycle.ViewModel
import com.fantasma.sudoku.game.SudokuGame

class MainViewModel : ViewModel() {
    val sudokuGame = SudokuGame()
}