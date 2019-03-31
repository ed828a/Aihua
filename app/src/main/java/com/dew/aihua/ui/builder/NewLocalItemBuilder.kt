package com.dew.aihua.ui.builder

import android.content.Context
import com.dew.aihua.data.local.database.LocalItem
import com.dew.aihua.util.OnClickGesture

/**
 *  Created by Edward on 3/2/2019.
 */
class NewLocalItemBuilder(context: Context) : GeneralItemBuilder(context){

    var onItemSelectedListener: OnClickGesture<LocalItem>? = null

}
