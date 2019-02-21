package com.dew.aihua

import android.os.Parcel
import androidx.test.InstrumentationRegistry
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import com.dew.aihua.report.ErrorInfo
import com.dew.aihua.report.UserAction
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [ErrorInfo]
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ErrorInfoTest {
    private val context = InstrumentationRegistry.getContext()

    @Test
    fun errorInfo_testParcelable() {
        val info = ErrorInfo.make(UserAction.USER_REPORT, "youtube", "request", R.string.general_error)
        // Obtain a Parcel object and write the parcelable object to it:
        val parcel = Parcel.obtain()
        info.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val infoFromParcel = ErrorInfo.CREATOR.createFromParcel(parcel)

        Assert.assertEquals(UserAction.USER_REPORT, infoFromParcel.userAction)
        Assert.assertEquals("youtube", infoFromParcel.serviceName)
        Assert.assertEquals("request", infoFromParcel.request)
        Assert.assertEquals(R.string.general_error.toLong(), infoFromParcel.message.toLong())

        parcel.recycle()
    }
}