package com.dew.aihua.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dew.aihua.R

/**
 *  Created by Edward on 3/2/2019.
 */
class BlankFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setTitle(TITLE)
        return inflater.inflate(R.layout.fragment_blank, container, false)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        setTitle(TITLE)

    }
    companion object {
        const val TITLE = "Aihua"
    }
}
