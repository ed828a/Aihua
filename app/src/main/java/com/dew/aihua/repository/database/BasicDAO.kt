package com.dew.aihua.repository.database

import androidx.room.*
import io.reactivex.Flowable

/**
 *  Created by Edward on 2/23/2019.
 */


@Dao
interface BasicDAO<Entity> {

    /* Searches */
    val all: Flowable<List<Entity>>

    /* Inserts */
    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insert(entity: Entity): Long

    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insertAll(vararg entities: Entity): List<Long>

    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insertAll(entities: Collection<Entity>): List<Long>

    fun listByService(serviceId: Int): Flowable<List<Entity>>

    /* Deletes */
    @Delete
    fun delete(entity: Entity)

    @Delete
    fun delete(entities: Collection<Entity>): Int

    fun deleteAll(): Int

    /* Updates */
    @Update
    fun update(entity: Entity): Int

    @Update
    fun update(entities: Collection<Entity>)
}
