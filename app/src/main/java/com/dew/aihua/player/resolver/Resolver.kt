package com.dew.aihua.player.resolver

/**
 *  Created by Edward on 3/2/2019.
 */

interface Resolver<Source, Product> {
    fun resolve(source: Source): Product?
}
