package com.fantasma.sudoku.ui.main

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.fantasma.sudoku.R
import com.fantasma.sudoku.databinding.MainFragmentBinding
import com.fantasma.sudoku.game.Cell
import com.fantasma.sudoku.game.SudokuGame
import com.fantasma.sudoku.view.custom.SudokuBoardView

class MainFragment : Fragment(), SudokuBoardView.OnTouchListener {

    companion object {
        fun newInstance() = MainFragment()
    }


    private lateinit var binding: MainFragmentBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.main_fragment,
            container,
            false
        )


        binding.sudokuGame = viewModel.sudokuGame
        binding.gameLayout.registerListener(this)



/*        numberButtons = listOf(oneButton, twoButton, threeButton, fourButton, fiveButton, sixButton,
            sevenButton, eightButton, nineButton)

        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener { viewModel.sudokuGame.handleInput(index + 1) }
        }

        notesButton.setOnClickListener { viewModel.sudokuGame.changeNoteTakingState() }
        deleteButton.setOnClickListener { viewModel.sudokuGame.delete() }*/

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewModel.sudokuGame.selectedCellLiveData.observe(viewLifecycleOwner, Observer { updateSelectedCellUI(it) })

        viewModel.sudokuGame.cellsLiveData.observe(viewLifecycleOwner, Observer { updateCells(it) })

        viewModel.sudokuGame.isTakingNotesLiveData.observe(viewLifecycleOwner, Observer { updateNoteTakingUI(it) })

        viewModel.sudokuGame.highlightedKeysLiveData.observe(viewLifecycleOwner, Observer { updateHighlightedKeys(it) })

        viewModel.sudokuGame.gameWon.observe(viewLifecycleOwner, Observer {gameWon ->
           if(gameWon) {

           }
        })
        viewModel.sudokuGame.createBoard(0)
        super.onViewCreated(view, savedInstanceState)
    }

    private fun updateCells(cells: List<Cell>?) = cells?.let {
        binding.gameLayout.updateCells(cells)
    }

    private fun updateSelectedCellUI(cell: Pair<Int, Int>?) = cell?.let {
        binding.gameLayout.updateSelectedCellUI(cell.first, cell.second)
    }

    private fun updateNoteTakingUI(isNoteTaking: Boolean?) = isNoteTaking?.let {
//        val color = if (it) ContextCompat.getColor(this, R.color.colorPrimary) else Color.LTGRAY
//        notesButton.background.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
    }

    private fun updateHighlightedKeys(set: Set<Int>?) = set?.let {
//        numberButtons.forEachIndexed { index, button ->
//            val color = if (set.contains(index + 1)) ContextCompat.getColor(this, R.color.colorPrimary) else Color.LTGRAY
//            button.background.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
//        }
    }

    override fun onCellTouched(row: Int, col: Int) {
        viewModel.sudokuGame.updateSelectedCell(row, col)
    }
}