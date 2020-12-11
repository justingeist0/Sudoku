package com.fantasmaplasma.sudoku.ui.fragment

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.fantasmaplasma.sudoku.R
import com.fantasmaplasma.sudoku.database.SudokuBoardDatabase
import com.fantasmaplasma.sudoku.databinding.MainMenuFragmentBinding
import com.fantasmaplasma.sudoku.ui.viewmodel.MainViewModel
import com.fantasmaplasma.sudoku.util.Constant
import com.fantasmaplasma.sudoku.util.Constant.EASY
import com.fantasmaplasma.sudoku.util.Constant.EXISTING_BOARD
import com.fantasmaplasma.sudoku.util.Constant.EXPERT
import com.fantasmaplasma.sudoku.util.Constant.HARD
import com.fantasmaplasma.sudoku.util.Constant.INTERMEDIATE
import com.fantasmaplasma.sudoku.util.Constant.KEY_CONTINUE
import com.fantasmaplasma.sudoku.util.Constant.KEY_PREFERENCES
import com.fantasmaplasma.sudoku.util.Constant.SOLVER
import com.fantasmaplasma.sudoku.util.Constant.THEME_DARK
import com.fantasmaplasma.sudoku.util.Constant.THEME_LIGHT
import com.fantasmaplasma.sudoku.util.SharedPrefs

class MainMenuFragment : Fragment() {

    private lateinit var binding: MainMenuFragmentBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var showMainMenu = true
    private lateinit var buttons: Array<Button>
    private lateinit var buttonsTxt: Array<Pair<String, String>>
    lateinit var themeButtonTxt: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = SudokuBoardDatabase
            .getInstance(requireContext())
            .sleepDataBaseDao
        viewModel.setDatabase(database)
        activity?.onBackPressedDispatcher?.addCallback(this,
            object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(showingMainMenu()) {
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
        })
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
        buttons = arrayOf (
            binding.newGameBtn,
            binding.continueGameBtn,
            binding.solverBtn,
            binding.createdBoardsBtn,
            binding.themesBtn
        )
        themeButtonTxt = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> {
                getString(R.string.light_theme)
            }
            else -> {
                getString(R.string.dark_theme)
            }
        }
        binding.themesBtn.text = themeButtonTxt
        buttonsTxt = arrayOf(
            Pair(getString(R.string.new_game), getString(R.string.main_menu)),
            Pair(getString(R.string.continue_txt), getString(R.string.easy)),
            Pair(getString(R.string.created_boards), getString(R.string.intermediate)),
            Pair(getString(R.string.sudoku_solver), getString(R.string.hard)),
            Pair(themeButtonTxt, getString(R.string.expert))
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val desiredTheme = SharedPrefs.getThemePref(requireActivity())
        if((themeButtonTxt == getString(R.string.dark_theme) && desiredTheme == THEME_DARK) ||
            (themeButtonTxt == getString(R.string.light_theme) && desiredTheme == THEME_LIGHT)) {
            themesOrExpertPressed()
        }
        super.onViewCreated(view, savedInstanceState)
    }

    private fun showingMainMenu() : Boolean {
        if(!showMainMenu) {
            showMainMenu = !showMainMenu
            animateButtons()
            return false
        }
        return true
    }

    fun continueOrEasyPressed() {
        if (showMainMenu) {
            val sharedPrefs =
                requireActivity().getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
            val continueBoardId = sharedPrefs.getLong(KEY_CONTINUE, -1L)
            if(continueBoardId == -1L)
                Toast.makeText(requireContext(), getString(R.string.no_board_to_continue), Toast.LENGTH_SHORT).show()
            else
                navigateToSudokuFragment(EXISTING_BOARD)
        } else {
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