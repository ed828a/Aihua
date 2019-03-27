package com.dew.aihua.data.model

/**
 *  Created by Edward on 3/2/2019.
 */

data class SuggestionItem(val fromHistory: Boolean, val query: String?) {

    override fun toString(): String {
        return "[$fromHistory→$query]"
    }
}
