package com.dew.aihua.ui.model

/**
 *  Created by Edward on 2/23/2019.
 */

data class SuggestionItem(val fromHistory: Boolean, val query: String?) {

    override fun toString(): String {
        return "[$fromHistory→$query]"
    }
}
