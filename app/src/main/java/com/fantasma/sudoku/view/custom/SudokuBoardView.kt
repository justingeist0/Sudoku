package com.fantasma.sudoku.view.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.fantasma.sudoku.R
import com.fantasma.sudoku.game.Cell
import com.fantasma.sudoku.util.Constant.SIZE
import com.fantasma.sudoku.util.Constant.SQRT_SIZE
import kotlin.math.min

class SudokuBoardView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    // these are set in onDraw
    private var cellSizePixels = 0F
    private var noteSizePixels = 0F

    private var width = 0F
    private var height = 0F
    private var startX = 0F
    private var startY = 0F
    private lateinit var boardRectangle: List<Float>

    private var selectedRow = 0
    private var selectedCol = 0

    private var listener: SudokuBoardView.OnTouchListener? = null

    private var cells: List<Cell>? = null

    private val thickLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 8F
    }

    private val thinLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 2F
    }

    private val conflictingCellPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(getContext(), R.color.conflictingCellBackground)
    }

    private val textPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.BLACK
    }

    private val startingCellTextPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.BLACK
        typeface = Typeface.DEFAULT_BOLD
    }

    private val invalidCellTextPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(getContext(), R.color.invalidCellText)
    }

    private val noteTextPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.BLACK
    }

    private val startingCellPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(getContext(), R.color.selectedCellBackground)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        width = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        height = MeasureSpec.getSize(heightMeasureSpec).toFloat()

        updateMeasurements(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        fillCells(canvas)
        drawLines(canvas)
        drawText(canvas)
    }

    private fun updateMeasurements(width: Float, height: Float) {
        cellSizePixels = min(width, height) / SIZE.toFloat()

        val boardSize = cellSizePixels*SIZE
        if(width < height) {
            startY = (height - boardSize) / 2
        } else if(height < width) {
            startX = (width - boardSize) / 2
        }

        noteSizePixels = cellSizePixels / SQRT_SIZE.toFloat()
        noteTextPaint.textSize = cellSizePixels / SQRT_SIZE.toFloat()
        textPaint.textSize = cellSizePixels / 1.5F
        invalidCellTextPaint.textSize = textPaint.textSize
        startingCellTextPaint.textSize = cellSizePixels / 1.5F
        boardRectangle = listOf(startX+thickLinePaint.strokeWidth/2, startY+thickLinePaint.strokeWidth/2, startX+boardSize-thickLinePaint.strokeWidth/2, startY+boardSize-thickLinePaint.strokeWidth/2)
    }

    private fun fillCells(canvas: Canvas) {
        if (selectedRow == -1 || selectedCol == -1) {
            return
        }

        cells?.forEach { cell ->
            val r = cell.row
            val c = cell.col

            if (r == selectedRow && c == selectedCol) {
                fillCell(canvas, r, c, startingCellPaint)
            } else if (r == selectedRow || c == selectedCol) {
                fillCell(canvas, r, c, conflictingCellPaint)
            } else if (r / SQRT_SIZE == selectedRow / SQRT_SIZE && c / SQRT_SIZE == selectedCol / SQRT_SIZE) {
                fillCell(canvas, r, c, conflictingCellPaint)
            }
        }
    }

    private fun fillCell(canvas: Canvas, r: Int, c: Int, paint: Paint) {
        canvas.drawRect(startX + c * cellSizePixels, startY + r * cellSizePixels, startX + (c + 1) * cellSizePixels, startY + (r + 1) * cellSizePixels, paint)
    }


    private fun drawLines(canvas: Canvas) {
        canvas.drawRect(boardRectangle[0], boardRectangle[1], boardRectangle[2], boardRectangle[3], thickLinePaint)

        for (i in 1 until SIZE) {
            val paintToUse = when (i % SQRT_SIZE) {
                0 -> thickLinePaint
                else -> thinLinePaint
            }

            val columnX = startX + i * cellSizePixels
            canvas.drawLine(
                columnX,
                startY,
                columnX,
                height-startY,
                paintToUse
            )

            val rowY = startY + i * cellSizePixels
            canvas.drawLine(
                startX,
                rowY,
                width-startX,
                rowY,
                paintToUse
            )
        }
    }

    private fun drawText(canvas: Canvas) {
        cells?.forEach { cell ->
            val value = cell.value

            if (value == 0) {
                // draw notes
                cell.notes.forEach { note ->
                    val rowInCell = (note - 1) / SQRT_SIZE
                    val colInCell = (note - 1) % SQRT_SIZE
                    val valueString = note.toString()

                    val textBounds = Rect()
                    noteTextPaint.getTextBounds(valueString, 0, valueString.length, textBounds)
                    val textWidth = noteTextPaint.measureText(valueString)
                    val textHeight = textBounds.height()
                    canvas.drawText(
                        valueString,
                        startX + (cell.col * cellSizePixels) + (colInCell * noteSizePixels) + noteSizePixels / 2 - textWidth / 2f,
                        startY + (cell.row * cellSizePixels) + (rowInCell * noteSizePixels) + noteSizePixels / 2 + textHeight / 2f,
                        noteTextPaint
                    )
                }
            } else {
                val row = cell.row
                val col = cell.col
                val valueString = cell.value.toString()

                val paintToUse = if (cell.isStartingCell) startingCellTextPaint else if(cell.conflictingCells == 0) textPaint else invalidCellTextPaint
                val textBounds = Rect()
                paintToUse.getTextBounds(valueString, 0, valueString.length, textBounds)
                val textWidth = paintToUse.measureText(valueString)
                val textHeight = textBounds.height()

                canvas.drawText(valueString, startX+(col * cellSizePixels) + cellSizePixels / 2 - textWidth / 2,
                    startY+(row * cellSizePixels) + cellSizePixels / 2 + textHeight / 2, paintToUse)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                return handleTouchEvent(event.x, event.y)
            }
            else -> false
        }
    }

    private fun handleTouchEvent(x: Float, y: Float) : Boolean {
        if (inGameBounds(x,y)) {
            val selectedRow = ((y - startY) / cellSizePixels).toInt()
            val selectedCol = ((x - startX) / cellSizePixels).toInt()
            listener?.onCellTouched(selectedRow, selectedCol)
        } else {
            listener?.onCellTouched(-1,-1)
            invalidate()
            return false
        }
        return true
    }

    private fun inGameBounds(x: Float, y: Float) : Boolean = startX < x && width-startX > x
                                                             && startY < y && height-startY > y

    fun updateSelectedCellUI(row: Int, col: Int) {
        selectedRow = row
        selectedCol = col
        invalidate()
    }

    fun updateCells(cells: List<Cell>) {
        this.cells = cells
        invalidate()
    }

    fun registerListener(listener: OnTouchListener) {
        this.listener = listener
    }

    interface OnTouchListener {
        fun onCellTouched(row: Int, col: Int)
    }
}
