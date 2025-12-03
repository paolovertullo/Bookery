package com.bookery.data.local

import android.content.Context

object BookProgressStore {
    private const val PREF_NAME = "book_progress"

    fun getProgress(context: Context, bookId: String): Float {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(bookId, 0f) // 0 = libro non ancora letto
    }

    fun setProgress(context: Context, bookId: String, progress: Float) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(bookId, progress.coerceIn(0f, 1f))
            .apply()
    }
}
