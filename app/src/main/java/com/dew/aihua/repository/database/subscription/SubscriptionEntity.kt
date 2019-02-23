package com.dew.aihua.repository.database.subscription

import androidx.room.*
import com.dew.aihua.repository.database.subscription.SubscriptionEntity.Companion.SUBSCRIPTION_SERVICE_ID
import com.dew.aihua.repository.database.subscription.SubscriptionEntity.Companion.SUBSCRIPTION_TABLE
import com.dew.aihua.repository.database.subscription.SubscriptionEntity.Companion.SUBSCRIPTION_URL
import com.dew.aihua.util.Constants
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelInfoItem

/**
 *  Created by Edward on 2/23/2019.
 */

@Entity(tableName = SUBSCRIPTION_TABLE, indices = [Index(value = arrayOf(SUBSCRIPTION_SERVICE_ID, SUBSCRIPTION_URL), unique = true)])
class SubscriptionEntity(
    @ColumnInfo(name = SUBSCRIPTION_SERVICE_ID) var serviceId: Int = Constants.NO_SERVICE_ID,

    @ColumnInfo(name = SUBSCRIPTION_URL) var url: String? = null,

    @ColumnInfo(name = SUBSCRIPTION_NAME) var name: String? = null,

    @ColumnInfo(name = SUBSCRIPTION_AVATAR_URL) var avatarUrl: String? = null,

    @ColumnInfo(name = SUBSCRIPTION_SUBSCRIBER_COUNT) var subscriberCount: Long? = null,

    @ColumnInfo(name = SUBSCRIPTION_DESCRIPTION) var description: String? = null
) {

    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0

    @Ignore
    fun setData(name: String,
                avatarUrl: String,
                description: String,
                subscriberCount: Long?) {
        this.name = name
        this.avatarUrl = avatarUrl
        this.description = description
        this.subscriberCount = subscriberCount
    }

    @Ignore
    fun toChannelInfoItem(): ChannelInfoItem {
        val item = ChannelInfoItem(serviceId, url, name)
        item.thumbnailUrl = avatarUrl
        item.subscriberCount = subscriberCount!!
        item.description = description
        return item
    }

    companion object {

        internal const val SUBSCRIPTION_UID = "uid"
        internal const val SUBSCRIPTION_TABLE = "subscriptions"
        internal const val SUBSCRIPTION_SERVICE_ID = "service_id"
        internal const val SUBSCRIPTION_URL = "url"
        internal const val SUBSCRIPTION_NAME = "name"
        internal const val SUBSCRIPTION_AVATAR_URL = "avatar_url"
        internal const val SUBSCRIPTION_SUBSCRIBER_COUNT = "subscriber_count"
        internal const val SUBSCRIPTION_DESCRIPTION = "description"

        @Ignore
        fun from(info: ChannelInfo): SubscriptionEntity =
            with(info) {
                SubscriptionEntity(
                    serviceId,
                    url,
                    name,
                    avatarUrl,
                    subscriberCount,
                    description
                )
            }
    }
}
