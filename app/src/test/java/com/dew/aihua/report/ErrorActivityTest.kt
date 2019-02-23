package com.dew.aihua.report

import android.app.Activity
import com.dew.aihua.ui.activity.MainActivity
import com.dew.aihua.ui.activity.RouterActivity
import com.dew.aihua.ui.fragment.VideoDetailFragment
import org.junit.Assert
import org.junit.Test

/**
 *  Created by Edward on 2/23/2019.
 ***
 * Unit tests for [ErrorActivity]
 */
class ErrorActivityTest {
    @Test
    fun getReturnActivity() {
        var returnActivity: Class<out Activity>? = ErrorActivity.getReturnActivity(MainActivity::class.java)
        Assert.assertEquals(MainActivity::class.java, returnActivity)

        returnActivity = ErrorActivity.getReturnActivity(RouterActivity::class.java)
        Assert.assertEquals(RouterActivity::class.java, returnActivity)

        returnActivity = ErrorActivity.getReturnActivity(null)
        Assert.assertNull(returnActivity)

        returnActivity = ErrorActivity.getReturnActivity(Int::class.java)
        Assert.assertEquals(MainActivity::class.java, returnActivity)

        returnActivity = ErrorActivity.getReturnActivity(VideoDetailFragment::class.java)
        Assert.assertEquals(MainActivity::class.java, returnActivity)
    }


}