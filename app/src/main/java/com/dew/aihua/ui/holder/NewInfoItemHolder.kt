package com.dew.aihua.ui.holder

import android.view.ViewGroup
import com.dew.aihua.ui.builder.NewInfoItemBuilder

/**
 *  Created by Edward on 3/2/2019.
 */

abstract class NewInfoItemHolder(
    itemBuilder: NewInfoItemBuilder,
    layoutId: Int,
    parent: ViewGroup
) : GeneralItemHolder<NewInfoItemBuilder>(itemBuilder, layoutId, parent)
