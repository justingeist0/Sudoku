package com.fantasma.sudoku.ui.fragment

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.fantasma.sudoku.R
import com.fantasma.sudoku.database.SudokuBoardDatabase
import com.fantasma.sudoku.databinding.MainMenuFragmentBinding
import com.fantasma.sudoku.ui.viewmodel.MainViewModel
import com.fantasma.sudoku.util.Constant
import com.fantasma.sudoku.util.Constant.DISABLED_BTN_ALPHA
import com.fantasma.sudoku.util.Constant.EASY
import com.fantasma.sudoku.util.Constant.EXISTING_BOARD
import com.fantasma.sudoku.util.Constant.EXPERT
import com.fantasma.sudoku.util.Constant.HARD
import com.fantasma.sudoku.util.Constant.INTERMEDIATE
import com.fantasma.sudoku.util.Constant.KEY_CONTINUE
import com.fantasma.sudoku.util.Constant.KEY_PREFERENCES
import com.fantasma.sudoku.util.Constant.SOLVER
import com.fantasma.sudoku.util.Constant.THEME_DARK
import com.fantasma.sudoku.util.Constant.THEME_LIGHT
import com.fantasma.sudoku.util.SharedPrefs

class MainMenuFragment : Fragment() {

    private lateinit var binding: MainMenuFragmentBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var showMainMenu = true
    private lateinit var buttons: Array<Button>
    private lateinit var buttonsTxt: Array<Pair<String, String>>
    lateinit var themeButtonTxt: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = SudokuBoardDatabase.getInstance(requireContext()).sleepDataBaseDao
        viewModel.setDatabase(database)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.main_menu_fragment,
            container,
            false
        )

        binding.menu = this
        binding.constant = Constant

        buttons = arrayOf(
            binding.newGameBtn,
            binding.continueGameBtn,
            binding.solverBtn,
            binding.createdBoardsBtn,
            binding.themesBtn
        )

        themeButtonTxt = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> {
                getString(R.string.lightTheme)
            }
            else -> {
                getString(R.string.darkTheme)
            }
        }

        binding.themesBtn.text = themeButtonTxt

        buttonsTxt = arrayOf(
            Pair(getString(R.string.new_game), getString(R.string.back)),
            Pair(getString(R.string.continue_txt), getString(R.string.easy)),
            Pair(getString(R.string.created_boards), getString(R.string.intermediate)),
            Pair(getString(R.string.sudoku_solver), getString(R.string.hard)),
            Pair(themeButtonTxt, getString(R.string.expert))
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val desiredTheme = SharedPrefs.getThemePref(requireActivity())
        if(themeButtonTxt == getString(R.string.darkTheme) && desiredTheme == THEME_DARK) {
            themesOrExpertPressed()
        }

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        updateContinueBtn()
        super.onResume()
    }

    private fun updateContinueBtn() {
        val sharedPrefs =
            requireActivity().getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
        val continueBoardId = sharedPrefs.getLong(KEY_CONTINUE, -1L)
        enableContinueBtn(continueBoardId != -1L)
    }

    private fun enableContinueBtn(enabled: Boolean) {
        binding.continueGameBtn.isEnabled = enabled
        binding.continueGameBtn.alpha = if (enabled) 1f else DISABLED_BTN_ALPHA
    }

    fun continueOrEasyPressed() {
        if (showMainMenu) {
            navigateToSudokuFragment(EXISTING_BOARD)
        } else {
            //EASY BUTTON PRESSED
            navigateToSudokuFragment(EASY)
        }
    }

    fun createdBoardOrIntermediatePressed() {
        if (showMainMenu) {
            findNavController().navigate(MainMenuFragmentDirections.actionMainMenuToPortfolioFragment())
        } else {
            navigateToSudokuFragment(INTERMEDIATE)
        }
    }

    fun solverOrHardPressed() {
        navigateToSudokuFragment(
            if (showMainMenu)
                SOLVER
            else
                HARD
        )
    }

    fun themesOrExpertPressed() {
        if (showMainMenu) {
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    SharedPrefs.updateThemePrefs(requireActivity(), THEME_LIGHT)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    SharedPrefs.updateThemePrefs(requireActivity(), THEME_DARK)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }
        } else {
            navigateToSudokuFragment(EXPERT)
        }
    }

    private fun navigateToSudokuFragment(mode: Int) {
        setDefaultButtons()
        findNavController().navigate(MainMenuFragmentDirections.actionMainMenuToSudokuFragment(mode))
    }

    fun newGameBtnPressed() {
        showMainMenu = !showMainMenu
        animateButtons()
    }

    private fun animateButtons() {
        for (i in buttons.indices) {
            buttons[i].isEnabled = false
            buttons[i].animate().apply {
                startDelay = 100L * (buttons.size - i - 1)
                duration = 250
                scaleX(1f - (i / (buttons.size.toFloat() - 1) + 0.1f))
                scaleY(0f)
            }.withEndAction {
                buttons[i].text =
                    if (showMainMenu)
                        buttonsTxt[i].first
                    else
                        buttonsTxt[i].second
                buttons[i].animate().apply {
                    startDelay = 200L * i
                    duration = 300
                    scaleX(1f)
                    scaleY(1f)
                }.withEndAction {
                    buttons[i].isEnabled = true
                }
            }
        }
    }

    private fun setDefaultButtons() {
        if (!showMainMenu) {
            newGameBtnPressed()
        }
    }
}