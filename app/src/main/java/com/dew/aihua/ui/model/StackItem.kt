package com.dew.aihua.ui.model

import java.io.Serializable

/**
 *  Created by Edward on 3/2/2019.
 */
data class StackItem(val serviceId: Int, val url: String, var title: String) : Serializable {

    override fun toString(): String {
        return "${serviceId.toString()}:$url > $title"
    }
}
