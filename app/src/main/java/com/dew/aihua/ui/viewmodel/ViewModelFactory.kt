package com.dew.aihua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dew.aihua.data.repository.Repository

/**
 *  Created by Edward on 3/24/2019.
 */
class ViewModelFactory (private val repository: Repository): ViewModelProvider.NewInstanceFactory(){

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SearchFragmentViewModel::class.java) -> SearchFragmentViewModel(repository) as T
            //  ...
            else -> throw IllegalArgumentException("Unknown ViewModel Class")
        }
    }
}
