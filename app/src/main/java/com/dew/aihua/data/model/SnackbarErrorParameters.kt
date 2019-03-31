package com.dew.aihua.data.model

import androidx.annotation.StringRes
import com.dew.aihua.report.UserAction

/**
 *  Created by Edward on 3/24/2019.
 */
data class SnackbarErrorParameters(
    val exception: List<Throwable>,
    val userAction: UserAction,
    val serviceName: String,
    val request: String,
    @StringRes val errorId: Int
)