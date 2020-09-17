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
import com.fantasma.sudoku.util.Constant.GRID_SIZE
import com.fantasma.sudoku.util.Constant.SQRT_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    private var selectedRow = -1
    private var selectedCol = -1

    private var listener: OnTouchListener? = null

    private var cells: List<Cell>? = null
    private var animate = false
    var createdBoardView = false

    private val thickLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(getContext(), R.color.text)
        strokeWidth = 8F
    }

    private val thinLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(getContext(), R.color.text)
        strokeWidth = 2F
    }

    private val conflictingCellPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(getContext(), R.color.conflictingCellBackground)
    }

    private val conflictingValidCellPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(getContext(), R.color.conflictingValidCellBackground)
    }

    private val textPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(getContext(), R.color.text)
    }

    private val startingCellTextPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(getContext(), R.color.text)
        typeface = Typeface.DEFAULT_BOLD
    }

    private val invalidCellTextPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(getContext(), R.color.invalidCellText)
    }

    private val noteTextPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(getContext(), R.color.text)
    }

    private val startingCellPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(getContext(), R.color.selectedCellBackground)
    }

    private val startingValidCellPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(getContext(), R.color.selectedValidCellBackground)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        width = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        height = MeasureSpec.getSize(heightMeasureSpec).toFloat()

        updateMeasurements(width, height)

        val sizePixels = min(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(sizePixels, sizePixels)
    }

    override fun onDraw(canvas: Canvas) {
        if(animate)
            animationStep(canvas)
        else
            fillCells(canvas)
        drawLines(canvas)
        drawText(canvas)
    }

    fun winAnimation() {
        if(animate) return

        animate = true
        GlobalScope.launch(Dispatchers.Default) {
            spiralTraverse = true
            var startRow = 0
            var endRow = GRID_SIZE - 1
            var startCol = 0
            var endCol = GRID_SIZE - 1

            while(startRow <= endRow && startCol <= endCol) {
                for(col in startCol..endCol) {
                    drawSpiralAnimationPosition(startRow, col)
                }
                for (row in startRow+1..endRow) {
                    drawSpiralAnimationPosition(row, endCol)
                }
                for (col in endCol-1 downTo startCol) {
                    if (startRow == endRow) break
                    drawSpiralAnimationPosition(endRow, col)
                }
                for (row in endRow-1 downTo startRow + 1) {
                    if(startCol == endCol) break
                    drawSpiralAnimationPosition(row, startCol)
                }
                startRow++
                endRow--
                startCol++
                endCol--
            }

            drawSpiralAnimationPosition(animationRow, animationCol) //Center Solid

            spiralTraverse = false
            var delay = 50L
            for(i in 0 until 5) {
                animationCol = GRID_SIZE/2
                animationCol2 = animationCol
                animationRow = animationCol
                animationRow2 = animationCol

                while (animationCol >= 0) {
                    animationCol--
                    animationRow--
                    animationCol2++
                    animationRow2++
                    invalidate()
                    delay(delay)
                }
                delay += 20L
            }

            selectedCol = -1
            selectedRow = -1
            animate = false
            invalidate()
        }
    }

    private suspend fun drawSpiralAnimationPosition(row: Int, col: Int) {
        animationCol2 = animationCol
        animationRow2 = animationRow
        animationRow = row
        animationCol = col
        invalidate()
        delay(20)
    }

    var animationRow2 = 0
    var animationCol2 = 0
    var animationRow = 0
    var animationCol = 0
    var spiralTraverse = true

    private fun animationStep(canvas: Canvas) {
        if(spiralTraverse) { //Spiral traverse animation
            fillCell(canvas, animationRow2, animationCol2, conflictingValidCellPaint)
            fillCell(canvas, animationRow, animationCol, startingValidCellPaint)
        } else { //middle out traverse animation
            for (col in animationCol+1 until animationCol2) {
                fillCell(canvas, animationRow+1, col, conflictingValidCellPaint)
                fillCell(canvas, animationRow2-1, col, conflictingValidCellPaint)
            }
            for (row in animationRow+2 until animationRow2-1) {
                fillCell(canvas, row, animationCol+1, conflictingValidCellPaint)
                fillCell(canvas, row, animationCol2-1, conflictingValidCellPaint)
            }

            if(animationCol >= 0) {
                for (col in animationCol..animationCol2) {
                    fillCell(canvas, animationRow, col, startingValidCellPaint)
                    fillCell(canvas, animationRow2, col, startingValidCellPaint)
                }
                for (row in animationRow + 1 until animationRow2) {
                    fillCell(canvas, row, animationCol, startingValidCellPaint)
                    fillCell(canvas, row, animationCol2, startingValidCellPaint)
                }
            }
        }
    }

    private fun updateMeasurements(width: Float, height: Float) {
        cellSizePixels = min(width, height) / GRID_SIZE.toFloat()

        val boardSize: Float = cellSizePixels*GRID_SIZE
        when {
            width < height -> {
                startY = (height - boardSize) / 2
                startX = 0f
            }
            height < width -> {
                startX = (width - boardSize) / 2
                startY = 0f
            }
            else -> {
                startX = 0f
                startY = 0f
            }
        }

        noteSizePixels = cellSizePixels / SQRT_SIZE.toFloat()
        noteTextPaint.textSize = (cellSizePixels / SQRT_SIZE.toFloat())
        textPaint.textSize = cellSizePixels / 1.5F
        invalidCellTextPaint.textSize = textPaint.textSize
        startingCellTextPaint.textSize = cellSizePixels / 1.5F

        boardRectangle = listOf(
            startX+thickLinePaint.strokeWidth/2,
            startY+thickLinePaint.strokeWidth/2,
            startX+boardSize-thickLinePaint.strokeWidth/2,
            startY+boardSize-thickLinePaint.strokeWidth/2
        )
    }

    private fun fillCells(canvas: Canvas) {
        if (selectedRow == -1 || selectedCol == -1) {
            if(createdBoardView) {
                cells?.forEach { cell ->
                    if(cell.isStartingCell)
                        fillCell(canvas, cell.row, cell.col, conflictingValidCellPaint)
                }
            }
            return
        }

        val cellSelectedIsStartingCell = cells?.get(selectedRow * GRID_SIZE + selectedCol)?.isStartingCell ?: false

        cells?.forEach { cell ->
            val r = cell.row
            val c = cell.col

            if (r == selectedRow && c == selectedCol) {
                val paint =
                    if (cell.isStartingCell)
                        startingValidCellPaint
                    else
                        startingCellPaint
                fillCell(canvas, r, c, paint)
            } else if (r == selectedRow || c == selectedCol ||
                (r / SQRT_SIZE == selectedRow / SQRT_SIZE && c / SQRT_SIZE == selectedCol / SQRT_SIZE)) {
                val paint =
                    if (cellSelectedIsStartingCell && cell.isStartingCell)
                        conflictingValidCellPaint
                    else
                        conflictingCellPaint
                fillCell(canvas, r, c, paint)
            }
        }
    }

    private fun fillCell(canvas: Canvas, r: Int, c: Int, paint: Paint) {
        canvas.drawRect(startX + c * cellSizePixels, startY + r * cellSizePixels, startX + (c + 1) * cellSizePixels, startY + (r + 1) * cellSizePixels, paint)
    }


    private fun drawLines(canvas: Canvas) {
        canvas.drawRect(boardRectangle[0], boardRectangle[1], boardRectangle[2], boardRectangle[3], thickLinePaint)

        for (i in 1 until GRID_SIZE) {
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
            if(this.selectedCol == selectedCol && this.selectedRow == selectedRow) {
                listener?.onCellTouched(-1,-1)
                invalidate()
                return true
            }
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
