package com.fantasmaplasma.sudoku.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.fantasmaplasma.sudoku.R
import com.fantasmaplasma.sudoku.database.SudokuBoard
import com.fantasmaplasma.sudoku.database.SudokuBoardDatabase
import com.fantasmaplasma.sudoku.databinding.SudokuFragmentBinding
import com.fantasmaplasma.sudoku.game.Cell
import com.fantasmaplasma.sudoku.ui.viewmodel.MainViewModel
import com.fantasmaplasma.sudoku.util.Constant.EASY
import com.fantasmaplasma.sudoku.util.Constant.EXPERT
import com.fantasmaplasma.sudoku.util.Constant.HARD
import com.fantasmaplasma.sudoku.util.Constant.INTERMEDIATE
import com.fantasmaplasma.sudoku.util.Constant.LOADING
import com.fantasmaplasma.sudoku.util.Constant.RESTART_EXISTING_BOARD
import com.fantasmaplasma.sudoku.util.Constant.SOLVER
import com.fantasmaplasma.sudoku.util.SharedPrefs
import com.fantasmaplasma.sudoku.view.SudokuBoardView

class SudokuFragment : Fragment(), SudokuBoardView.OnTouchListener {

    private lateinit var binding: SudokuFragmentBinding
    private lateinit var numberButtons: Array<View>
    private val viewModel: MainViewModel by activityViewModels()
    private var mode: Int = 0
    private var isTakingNotes = false
    private var header = Array(2) {""}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        mode = SudokuFragmentArgs.fromBundle(requireArguments()).mode
        val database = SudokuBoardDatabase.getInstance(requireContext()).sleepDataBaseDao
        viewModel.setDatabase(database)
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.sudoku_fragment,
            container,
            false
        )
        val setHelpText: (Int) -> Unit = {mode ->
            binding.helpBtnTv.text =
                if(mode == SOLVER)
                    getString(R.string.solve)
                else
                    getString(R.string.hint)
        }
        if(mode >= SOLVER) {
            viewModel.requestBoard(mode)
            setHelpText(mode)
        } else {
            viewModel.postBoard(
                SharedPrefs.getContinueBoardId(requireActivity()),
                mode == RESTART_EXISTING_BOARD,
                setHelpText
            )
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.initAd(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(viewModel.sudokuGame) {
        selectedCellLiveData.observe(viewLifecycleOwner, Observer { updateSelectedCellUI(it) })

        cellsLiveData.observe(viewLifecycleOwner, Observer { updateCells(it) })

        isTakingNotesLiveData.observe(viewLifecycleOwner, Observer { updateNoteTakingUI(it) })

        highlightedKeysLiveData.observe(viewLifecycleOwner, Observer { updateHighlightedKeys(it) })

        undoBtnEnabledLiveData.observe(viewLifecycleOwner, Observer { updateUndoBtn(it) })
        redoBtnEnabledLiveData.observe(viewLifecycleOwner, Observer { updateRedoBtn(it) })

        helpAvailableLiveData.observe(viewLifecycleOwner, Observer { updateHelpBtn(it) })

        deleteBtnEnabledLiveData.observe(viewLifecycleOwner, Observer {updateDeleteBtn(it)})

        gameWonLiveData.observe(viewLifecycleOwner, Observer {gameWon(it)})
        gameWonAnimationLiveData.observe(viewLifecycleOwner, Observer {gameWonParty(it)})

        gameIdLiveData.observe(viewLifecycleOwner, Observer {
            SharedPrefs.updateBoardIdSharedPrefs(
                requireActivity(),
                it)
        })

        headerLabelLiveData.observe(viewLifecycleOwner, Observer { updateHeaderMode(it) })

        timerLabelLiveData.observe(viewLifecycleOwner, Observer { updateTimer(it) })

        showAdLiveData.observe(viewLifecycleOwner, Observer {showAd(it)})
        }
        updateHeaderMode(LOADING)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        viewModel.resumeTimerIfNotSolverMode()
        super.onResume()
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
    }

    private fun updateRedoBtn(enabled: Boolean) {
        binding.redoBtn.isEnabled = enabled
    }

    private fun updateDeleteBtn(enabled: Boolean) {
        binding.deleteBtn.isEnabled = enabled
    }

    private fun updateHelpBtn(enabled: Boolean) {
        binding.helpBtn.isEnabled = enabled
        binding.helpBtnImage.isEnabled = enabled
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
        binding.notesBtnTv.text =
            if(isNoteTaking)
                getString(R.string.back)
            else
                getString(R.string.note)
    }

    private fun updateHighlightedKeys(set: Set<Int>?) = set?.let {
        numberButtons.forEachIndexed { index, button ->
            button.background =
                if (isTakingNotes)
                    if (set.contains(index + 1))
                        ContextCompat.getDrawable(requireContext(), R.drawable.btn_number_note)
                    else
                        ContextCompat.getDrawable(requireContext(), R.drawable.btn_number)
                else
                    ContextCompat.getDrawable(requireContext(), R.drawable.btn_number)
        }
    }

    private fun updateHeaderMode(value: Int = mode) {
        mode = value
        header[0] =
            when(mode) {
                EASY -> getString(R.string.easy)
                INTERMEDIATE -> getString(R.string.intermediate)
                HARD -> getString(R.string.hard)
                EXPERT -> getString(R.string.expert)
                SOLVER -> getString(R.string.sudoku_solver)
                else -> getString(R.string.loading)
            }
        updateHeader()
    }

    private fun updateTimer(timerTxt: String) {
        header[1] = timerTxt
        updateHeader()
    }

    private fun updateHeader(winningString: String = "") {
        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            if(header[1].isNotBlank())
                "${header[0]}   ${header[1]}$winningString"
            else
                "${header[0]}$winningString"//This line is for a solved sudoku solver
    }

    private fun gameWon(won: Boolean) {
        if(won) {
            updateHeader(" \uD83C\uDF1F")
        }
    }

    private fun gameWonParty(animate: Boolean) {
        if(animate) {
            binding.gameLayout.winAnimation()
        }
    }

    private fun showAd(showAd: Boolean) {
        if(!showAd) return
        viewModel.showAd(requireActivity())
        viewModel.initAd(requireActivity())
    }

    override fun onCellTouched(row: Int, col: Int) {
        viewModel.sudokuGame.updateSelectedCell(row, col)
    }
}