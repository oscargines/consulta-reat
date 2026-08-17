package com.oscar.consultareat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oscar.consultareat.RgtRepository

class ConsultaViewModelFactory(
    private val repository: RgtRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ConsultaViewModel(repository) as T
    }
}
