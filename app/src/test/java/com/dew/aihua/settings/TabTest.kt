package com.dew.aihua.settings

import com.dew.aihua.settings.tabs.Tab
import org.junit.Assert
import org.junit.Test
import java.util.HashSet

/**
 *  Created by Edward on 2/23/2019.
 */

class TabTest {
    @Test
    fun checkIdDuplication() {
        val usedIds = HashSet<Int>()

        for (type in Tab.Type.values()) {
            val added = usedIds.add(type.tabId)
            Assert.assertTrue("Id was already used: " + type.tabId, added)
        }
    }
}