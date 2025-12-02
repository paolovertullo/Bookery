package com.bookery.ui.library

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bookery.data.model.Book

class LibraryViewModel : ViewModel() {

    private val _books = MutableLiveData<List<Book>>(emptyList())
    val books: LiveData<List<Book>> = _books

    fun setBooks(list: List<Book>) {
        _books.value = list
    }

    fun addBook(book: Book) {
        val current = _books.value.orEmpty()
        _books.value = current + book
    }
}
