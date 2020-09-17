package com.fantasma.sudoku.adapter

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.fantasma.sudoku.R
import com.fantasma.sudoku.database.SudokuBoard
import com.fantasma.sudoku.databinding.ItemBoardBinding
import com.fantasma.sudoku.game.Board
import com.fantasma.sudoku.util.Constant.DELETE_BOARD
import com.fantasma.sudoku.util.Constant.EASY
import com.fantasma.sudoku.util.Constant.EXISTING_BOARD
import com.fantasma.sudoku.util.Constant.EXPERT
import com.fantasma.sudoku.util.Constant.HARD
import com.fantasma.sudoku.util.Constant.INTERMEDIATE
import com.fantasma.sudoku.util.Constant.RESTART_EXISTING_BOARD
import kotlinx.android.synthetic.main.confirm_dialog.*

class CreatedBoardAdapter(val listener: ClickListener) : RecyclerView.Adapter<CreatedBoardAdapter.BoardViewHolder>() {

    var boards = listOf<SudokuBoard>()
        set(value) {
            if(itemRemoved == -1) {
                field = value
                notifyDataSetChanged()
            } else {
                field = value
                notifyItemRemoved(itemRemoved)
                notifyItemRangeChanged(itemRemoved, field.size)
                itemRemoved = -1
            }
        }

    private var itemRemoved = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        return BoardViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_board, parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return boards.size
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        holder.bind(boards[position], listener) {
            itemRemoved = position
        }
    }

    class BoardViewHolder(private val binding: ItemBoardBinding) : RecyclerView.ViewHolder(binding.root) {
        private val boardValues = Board()

        init {

        }

        fun bind(board: SudokuBoard, listener: ClickListener, notifyDelete: () -> Unit) {
            val getString: (Int) -> String = {id ->
                binding.root.context.getString(id)
            }

            boardValues.setBoard(board)
            val complete = boardValues.isComplete()
            with(binding) {
                boardView.createdBoardView = true
                boardView.updateCells(boardValues.cells)
                boardTypeTxt.text =
                    when(board.boardDifficulty) {
                        EASY -> getString(R.string.easy)
                        INTERMEDIATE -> getString(R.string.intermediate)
                        HARD -> getString(R.string.hard)
                        EXPERT -> getString(R.string.expert)
                        else -> getString(R.string.solver)
                    }

                val time = if(complete)
                    "⌛️" + formatTime(board.time) + " \uD83C\uDF1F"
                else
                    "⏳" + formatTime(board.time)

                timeTxt.text = time

                resumeBtn.setOnClickListener {
                    listener.onClick(board, EXISTING_BOARD)
                }

                resumeBtn.text =
                    if(complete)
                        getString(R.string.view)
                    else
                        getString(R.string.resume)

                restartBtn.setOnClickListener {
                    showConfirmDialog(binding.root.context, getString(R.string.restart_board)) {
                        listener.onClick(board, RESTART_EXISTING_BOARD)
                    }
                }

                itemDeleteBtn.setOnClickListener {
                    showConfirmDialog(binding.root.context, getString(R.string.delete_board)) {
                        listener.onClick(board, DELETE_BOARD)
                        notifyDelete()
                    }
                }

            }
        }

        private fun showConfirmDialog(context: Context, headerTxt: String, confirm: () -> Unit) {
            val confirmDialog = Dialog(context)
            confirmDialog.setCancelable(true)
            confirmDialog.setContentView(R.layout.confirm_dialog)
            confirmDialog.confirm_btn.setOnClickListener {
                confirm()
                confirmDialog.cancel()
            }
            confirmDialog.cancel_btn.setOnClickListener {
                confirmDialog.cancel()
            }
            confirmDialog.header_txt.text = headerTxt
            confirmDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            confirmDialog.window?.attributes?.windowAnimations = R.style.dialogAnimation
            confirmDialog.show()
        }

        private fun formatTime(time: Long) : String =
            "${time / 60}:${if((time%60)/10 >= 1) "" else "0"}${time % 60}"

    }

    class ClickListener(val listener: (boardId: Long, action: Int) -> Unit) {
        fun onClick(board: SudokuBoard, action: Int) = listener(board.boardId, action)
    }

}