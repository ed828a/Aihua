package com.dew.aihua.ui.contract

/**
 *  Created by Edward on 3/2/2019.
 */
interface ListViewContract<I, N> : ViewContract<I> {
    fun showListFooter(show: Boolean)

    fun handleNextItems(result: N)
}
