package com.dew.aihua.player.resolver

/**
 *  Created by Edward on 2/23/2019.
 */

interface
Resolver<Source, Product> {
    fun resolve(source: Source): Product?
}
