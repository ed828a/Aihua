package com.dew.aihua.report


/**
 * The user actions that can cause an error.
 */
enum class UserAction(val message: String) {
    USER_REPORT("user report"),
    UI_ERROR("ui error"),
    SUBSCRIPTION("subscription"),
    LOAD_IMAGE("load image"),
    SOMETHING_ELSE("something else"),
    SEARCHED("searched"),
    GET_SUGGESTIONS("get suggestions"),
    REQUESTED_STREAM("requested stream"),
    REQUESTED_CHANNEL("requested channel"),
    REQUESTED_PLAYLIST("requested playlist"),
    REQUESTED_KIOSK("requested kiosk"),
    DELETE_FROM_HISTORY("delete getTabFrom history"),
    PLAY_STREAM("Play stream")
}
