package com.fantasmaplasma.sudoku.adapter

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.fantasmaplasma.sudoku.R
import com.fantasmaplasma.sudoku.database.SudokuBoard
import com.fantasmaplasma.sudoku.databinding.ItemBoardBinding
import com.fantasmaplasma.sudoku.game.Board
import com.fantasmaplasma.sudoku.util.Constant.DELETE_BOARD
import com.fantasmaplasma.sudoku.util.Constant.EASY
import com.fantasmaplasma.sudoku.util.Constant.EXISTING_BOARD
import com.fantasmaplasma.sudoku.util.Constant.EXPERT
import com.fantasmaplasma.sudoku.util.Constant.HARD
import com.fantasmaplasma.sudoku.util.Constant.INTERMEDIATE
import com.fantasmaplasma.sudoku.util.Constant.RESTART_EXISTING_BOARD
import kotlinx.android.synthetic.main.confirm_dialog.*

class CreatedBoardAdapter(private val listener: ClickListener) : RecyclerView.Adapter<CreatedBoardAdapter.BoardViewHolder>() {

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

        fun bind(board: SudokuBoard, listener: ClickListener, notifyDelete: () -> Unit) {
            val getStringID: (Int) -> String = { id ->
                binding.root.context.getString(id)
            }

            boardValues.setBoard(board)
            val complete = boardValues.isComplete()
            with(binding) {
                boardView.createdBoardView = true
                boardView.updateCells(boardValues.cells)
                boardTypeTxt.text =
                    when(board.boardDifficulty) {
                        EASY -> getStringID(R.string.easy)
                        INTERMEDIATE -> getStringID(R.string.intermediate)
                        HARD -> getStringID(R.string.hard)
                        EXPERT -> getStringID(R.string.expert)
                        else -> getStringID(R.string.solver)
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
                        getStringID(R.string.view)
                    else
                        getStringID(R.string.resume)

                restartBtn.setOnClickListener {
                    showConfirmDialog(binding.root.context, getStringID(R.string.restart_board)) {
                        listener.onClick(board, RESTART_EXISTING_BOARD)
                    }
                }

                itemDeleteBtn.setOnClickListener {
                    showConfirmDialog(binding.root.context, getStringID(R.string.delete_board)) {
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