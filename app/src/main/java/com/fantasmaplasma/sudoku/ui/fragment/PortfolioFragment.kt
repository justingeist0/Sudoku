package com.fantasmaplasma.sudoku.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.fantasmaplasma.sudoku.databinding.PortfolioFragmentBinding
import com.fantasmaplasma.sudoku.R
import com.fantasmaplasma.sudoku.adapter.CreatedBoardAdapter
import com.fantasmaplasma.sudoku.database.SudokuBoardDatabase
import com.fantasmaplasma.sudoku.ui.viewmodel.PortfolioViewModel
import com.fantasmaplasma.sudoku.util.Constant.DELETE_BOARD
import com.fantasmaplasma.sudoku.util.SharedPrefs
import kotlinx.coroutines.launch

class PortfolioFragment : Fragment() {
    private val viewModel: PortfolioViewModel by viewModels()
    private lateinit var binding: PortfolioFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.portfolio_fragment,
            container,
            false
        )

        val clickListener = CreatedBoardAdapter.ClickListener { boardId, action ->
            when(action) {
                DELETE_BOARD -> {
                    viewModel.deleteBoard(boardId)
                }
                else -> navigateToBoard(boardId, action)
            }
        }

        val adapter = CreatedBoardAdapter(clickListener)
        binding.createdBoardsList.adapter = adapter

        binding.createdBoardsList.setHasFixedSize(true)

        viewModel.uiScope.launch {
            val database = SudokuBoardDatabase.getInstance(requireContext()).sleepDataBaseDao
            viewModel.setDatabase(database) // <- This suspend function initializes boardLiveData

            viewModel.boardLiveData.observe(viewLifecycleOwner, Observer {
                adapter.boards = it
            })
        }

        return binding.root
    }

    private fun navigateToBoard(boardId: Long, action: Int) {
        SharedPrefs.updateBoardIdSharedPrefs(requireActivity(), boardId)
        findNavController().navigate(PortfolioFragmentDirections.actionPortfolioFragmentToSudokuFragment(action))
    }

}