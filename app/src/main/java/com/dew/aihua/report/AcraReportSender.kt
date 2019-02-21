package com.dew.aihua.report

import android.content.Context
import com.dew.aihua.R
import org.acra.collector.CrashReportData
import org.acra.sender.ReportSender

class AcraReportSender : ReportSender {

    override fun send(context: Context, report: CrashReportData) {
        ErrorActivity.reportError(context, report,
            ErrorInfo.make(UserAction.UI_ERROR, "none",
                "App crash, UI failure", R.string.app_ui_crash))
    }
}
