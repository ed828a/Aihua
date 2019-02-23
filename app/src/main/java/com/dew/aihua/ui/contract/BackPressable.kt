package com.dew.aihua.ui.contract

/**
 *  Created by Edward on 2/23/2019.
 ***
 * Indicates that the current fragment can handle back presses
 */
interface BackPressable {
    /**
     * A back press was delegated to this fragment
     *
     * @return if the back press was handled
     */
    fun onBackPressed(): Boolean
}
