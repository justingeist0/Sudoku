package com.fantasma.sudoku.ui.main

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.fantasma.sudoku.R
import com.fantasma.sudoku.database.SudokuBoard
import com.fantasma.sudoku.databinding.MainFragmentBinding
import com.fantasma.sudoku.game.Cell
import com.fantasma.sudoku.util.Constant.DISABLED_BTN_ALPHA
import com.fantasma.sudoku.util.Constant.EASY
import com.fantasma.sudoku.util.Constant.EXPERT
import com.fantasma.sudoku.util.Constant.HARD
import com.fantasma.sudoku.util.Constant.INTERMEDIATE
import com.fantasma.sudoku.util.Constant.LOADING
import com.fantasma.sudoku.util.Constant.RESTART_EXISTING_BOARD
import com.fantasma.sudoku.util.Constant.SOLVER
import com.fantasma.sudoku.util.SharedPrefs
import com.fantasma.sudoku.view.custom.SudokuBoardView

class SudokuFragment : Fragment(), SudokuBoardView.OnTouchListener {

    private lateinit var binding: MainFragmentBinding
    private lateinit var numberButtons: Array<View>
    private val viewModel: MainViewModel by activityViewModels()
    private var mode: Int = 0
    private var isTakingNotes = false
    private var headerText = Array(2) {""}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        mode = SudokuFragmentArgs.fromBundle(requireArguments()).mode

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.main_fragment,
            container,
            false
        )

        Log.i("Sudoku:", "$mode")

        if(mode < SOLVER) {
            viewModel.postBoard(
                SharedPrefs.getContinueBoardId(requireActivity()),
                mode == RESTART_EXISTING_BOARD
            )
        } else {
            viewModel.requestBoard(mode)
        }

        binding.sudokuGame = viewModel.sudokuGame
        binding.gameLayout.registerListener(this)

        numberButtons = arrayOf(
            binding.numberBtn1, binding.numberBtn2, binding.numberBtn3,
            binding.numberBtn4, binding.numberBtn5, binding.numberBtn6,
            binding.numberBtn7, binding.numberBtn8, binding.numberBtn9
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.sudokuGame.selectedCellLiveData.observe(viewLifecycleOwner, Observer { updateSelectedCellUI(it) })

        viewModel.sudokuGame.cellsLiveData.observe(viewLifecycleOwner, Observer { updateCells(it) })

        viewModel.sudokuGame.isTakingNotesLiveData.observe(viewLifecycleOwner, Observer { updateNoteTakingUI(it) })

        viewModel.sudokuGame.highlightedKeysLiveData.observe(viewLifecycleOwner, Observer { updateHighlightedKeys(it) })

        viewModel.sudokuGame.undoBtnEnabledLiveData.observe(viewLifecycleOwner, Observer { updateUndoBtn(it) })
        viewModel.sudokuGame.redoBtnEnabledLiveData.observe(viewLifecycleOwner, Observer { updateRedoBtn(it) })

        viewModel.sudokuGame.helpAvailableLiveData.observe(viewLifecycleOwner, Observer { updateHelpBtn(it) })

        viewModel.sudokuGame.deleteBtnEnabledLiveData.observe(viewLifecycleOwner, Observer {updateDeleteBtn(it)})

        viewModel.sudokuGame.gameWonLiveData.observe(viewLifecycleOwner, Observer {})

        viewModel.sudokuGame.gameIdLiveData.observe(viewLifecycleOwner, Observer { SharedPrefs.updateBoardIdSharedPrefs(requireActivity(), it)})

        viewModel.sudokuGame.headerLabelLiveData.observe(viewLifecycleOwner, Observer { updateHeaderMode(it) })

        updateHeaderMode(LOADING)

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        updateHeaderMode(

        )
    }



    override fun onPause() {
        viewModel.saveBoard()
        super.onPause()
    }

    override fun onDestroy() {
        viewModel.sudokuGame.postBoard(SudokuBoard())
        super.onDestroy()
    }

    private fun updateUndoBtn(enabled: Boolean) {
        binding.undoBtn.isEnabled = enabled
        binding.undoBtn.alpha = if (enabled) 1f else DISABLED_BTN_ALPHA
    }

    private fun updateRedoBtn(enabled: Boolean) {
        binding.redoBtn.isEnabled = enabled
        binding.redoBtn.alpha = if (enabled) 1f else DISABLED_BTN_ALPHA
    }

    private fun updateDeleteBtn(enabled: Boolean) {
        binding.deleteBtn.isEnabled = enabled
        binding.deleteBtn.alpha = if (enabled) 1f else DISABLED_BTN_ALPHA
    }

    private fun updateHelpBtn(enabled: Boolean) {
        binding.helpBtn.isEnabled = enabled
        binding.helpBtn.alpha = if (enabled) 1f else DISABLED_BTN_ALPHA
    }

    private fun updateCells(cells: List<Cell>?) = cells?.let {
        binding.gameLayout.updateCells(cells)
    }

    private fun updateSelectedCellUI(cell: Pair<Int, Int>?) = cell?.let {
        binding.gameLayout.updateSelectedCellUI(cell.first, cell.second)
    }

    private fun updateNoteTakingUI(isNoteTaking: Boolean) {
        isTakingNotes = isNoteTaking
        binding.notesBtn.background =
            if(isNoteTaking)
                ContextCompat.getDrawable(requireContext(), R.drawable.btn_number_note)
            else
                ContextCompat.getDrawable(requireContext(), R.drawable.btn_number)
    }

    private fun updateHighlightedKeys(set: Set<Int>?) = set?.let {
        numberButtons.forEachIndexed { index, button ->
            button.background =
                if (isTakingNotes)
                    if (set.contains(index + 1))
                        ContextCompat.getDrawable(requireContext(), R.drawable.btn_number)
                    else
                        ContextCompat.getDrawable(requireContext(), R.drawable.btn_number_note)
                else
                    ContextCompat.getDrawable(requireContext(), R.drawable.btn_number)
        }
    }

    private fun updateHeaderMode(value: Int = mode) {
        mode = value
        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            when(mode) {
                EASY -> getString(R.string.easy)
                INTERMEDIATE -> getString(R.string.intermediate)
                HARD -> getString(R.string.hard)
                EXPERT -> getString(R.string.expert)
                SOLVER -> getString(R.string.sudoku_solver)
                else -> getString(R.string.loading)
            }
    }

    override fun onCellTouched(row: Int, col: Int) {
        viewModel.sudokuGame.updateSelectedCell(row, col)
    }
}