package com.fantasma.sudoku.util

import android.app.Activity
import android.content.Context

object SharedPrefs {
    fun updateBoardIdSharedPrefs(activity: Activity, id: Long) {
        val prefs = activity.getSharedPreferences(Constant.KEY_PREFERENCES, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putLong(Constant.KEY_CONTINUE, id)
        editor.apply()
    }

    fun getContinueBoardId(activity: Activity) : Long {
        val prefs = activity.getSharedPreferences(Constant.KEY_PREFERENCES, Context.MODE_PRIVATE)
        return prefs.getLong(Constant.KEY_CONTINUE, -1L)
    }
}