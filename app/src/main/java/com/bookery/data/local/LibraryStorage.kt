package com.bookery.data.local

import android.content.Context
import com.bookery.data.model.Book
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val LIBRARY_FILE = "library.json"

class LibraryStorage(private val context: Context) {

    fun loadBooks(): List<Book> {
        val file = File(context.filesDir, LIBRARY_FILE)
        if (!file.exists()) return emptyList()

        return try {
            val json = file.readText()
            val array = JSONArray(json)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                Book(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    author = o.getString("author"),
                    language = o.getString("language"),
                    coverUrl = o.optString("coverUrl", null)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveBooks(books: List<Book>) {
        val array = JSONArray()
        books.forEach { b ->
            val o = JSONObject()
            o.put("id", b.id)
            o.put("title", b.title)
            o.put("author", b.author)
            o.put("language", b.language)
            o.put("coverUrl", b.coverUrl)
            array.put(o)
        }
        val file = File(context.filesDir, LIBRARY_FILE)
        file.writeText(array.toString())
    }
}
