package com.dew.aihua.repository.database.subscription

import androidx.room.*
import com.dew.aihua.repository.database.BasicDAO
import com.dew.aihua.repository.database.subscription.SubscriptionEntity.Companion.SUBSCRIPTION_SERVICE_ID
import com.dew.aihua.repository.database.subscription.SubscriptionEntity.Companion.SUBSCRIPTION_TABLE
import com.dew.aihua.repository.database.subscription.SubscriptionEntity.Companion.SUBSCRIPTION_UID
import com.dew.aihua.repository.database.subscription.SubscriptionEntity.Companion.SUBSCRIPTION_URL
import io.reactivex.Flowable

/**
 *  Created by Edward on 2/23/2019.
 */

@Dao
abstract class SubscriptionDAO : BasicDAO<SubscriptionEntity> {
    @get:Query("SELECT * FROM $SUBSCRIPTION_TABLE")
    abstract override val all: Flowable<List<SubscriptionEntity>>

    @Query("DELETE FROM $SUBSCRIPTION_TABLE")
    abstract override fun deleteAll(): Int

    @Query("SELECT * FROM $SUBSCRIPTION_TABLE WHERE $SUBSCRIPTION_SERVICE_ID = :serviceId")
    abstract override fun listByService(serviceId: Int): Flowable<List<SubscriptionEntity>>

    @Query("SELECT * FROM $SUBSCRIPTION_TABLE WHERE $SUBSCRIPTION_URL LIKE :url AND $SUBSCRIPTION_SERVICE_ID = :serviceId")
    abstract fun getSubscription(serviceId: Int, url: String): Flowable<List<SubscriptionEntity>>

    @Query("SELECT $SUBSCRIPTION_UID FROM $SUBSCRIPTION_TABLE WHERE $SUBSCRIPTION_URL LIKE :url AND $SUBSCRIPTION_SERVICE_ID = :serviceId")
    abstract fun getSubscriptionIdInternal(serviceId: Int, url: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertInternal(entities: SubscriptionEntity): Long?

    @Transaction
    open fun upsertAll(entities: List<SubscriptionEntity>): List<SubscriptionEntity> {
        for (entity in entities) {
            var uid = insertInternal(entity)

            if (uid != -1L) {
                entity.uid = uid!!
                continue
            }

            uid = getSubscriptionIdInternal(entity.serviceId, entity.url!!)
            entity.uid = uid!!

            if (uid == -1L) {
                throw IllegalStateException("Invalid subscription id (-1)")
            }

            update(entity)
        }

        return entities
    }
}
