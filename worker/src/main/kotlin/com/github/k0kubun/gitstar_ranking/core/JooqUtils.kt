package com.github.k0kubun.gitstar_ranking.core

import org.jooq.Record
import org.jooq.Table
import org.jooq.TableField
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal
import org.jooq.impl.SQLDataType
import org.jooq.impl.TableImpl

fun table(name: String, primaryKey: String? = null): Table<Record> {
    return if (primaryKey == null) {
        DSL.table(name)
    } else {
        object : TableImpl<Record>(DSL.name(name)) {
            private val pkeyTable = object : TableImpl<Record>(DSL.name(name)) {
                val primaryKey: TableField<Record, Long> = createField(DSL.name(primaryKey), SQLDataType.BIGINT)
            }
            override fun getPrimaryKey(): UniqueKey<Record>? = Internal.createUniqueKey(pkeyTable, "", pkeyTable.primaryKey)
        }
    }
}
