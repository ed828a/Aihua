package com.dew.aihua.ui.contract

/**
 *  Created by Edward on 2/23/2019.
 */

interface ViewContract<I> {
    fun showLoading()
    fun hideLoading()
    fun showEmptyState()
    fun showError(message: String, showRetryButton: Boolean)

    fun handleResult(result: I)
}
