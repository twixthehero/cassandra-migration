/**
 * File     : CassandraMigrationApiKIT.kt
 * License  :
 *   Original   - Copyright (c) 2015 - 2016 Contrast Security
 *   Derivative - Copyright (c) 2016 - 2018 cassandra-migration Contributors
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.hhandoko.cassandra.migration

import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.hhandoko.cassandra.migration.api.MigrationType
import com.hhandoko.cassandra.migration.internal.info.MigrationInfoDumper
import io.kotlintest.matchers.be
import io.kotlintest.matchers.have
import java.util.*

/**
 * API-based migration integration tests.
 */
class CassandraMigrationApiKIT : BaseKIT() {

    init {

        "Cassandra migration" - {

            "should run successfully" {
                val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                var cm = CassandraMigration()
                cm.locations = scriptsLocations
                cm.keyspaceConfig = getKeyspace()
                cm.migrate()

                var infoService = cm.info()
                println("Initial migration")
                println(MigrationInfoDumper.dumpToAsciiTable(infoService.all()))

                infoService.all().size shouldBe 6

                for (info in infoService.all()) {
                    forAtLeastOne(arrayOf("1.0.0", "1.1.0", "1.2.0", "2.0.0", "3.0", "3.0.1")) { info.version.version shouldBe it }

                    when (info.version.version) {
                        "3.0.1" -> {
                            info.description shouldBe "Three zero one"
                            info.type.name shouldBe MigrationType.JAVA_DRIVER.name
                            info.script should have substring ".java"

                            val select = QueryBuilder.selectFrom("test1").column("value")// QueryBuilder.select().column("value").from("test1")
                                    .whereColumn("space").isEqualTo(QueryBuilder.bindMarker())
                                    .whereColumn("key").isEqualTo(QueryBuilder.bindMarker());
//                            select.where(Relation.column("space").isEqualTo(QueryBuilder.literal("web"))).eq("space", "web")).and(eq("key", "facebook"))
                            val row = getKeyspaceSession().execute(select.build("web","facebook")).one()
                            row?.getString("value") shouldBe "facebook.com"
                        }
                        "3.0" -> {
                            info.description shouldBe "Third"
                            info.type.name shouldBe MigrationType.JAVA_DRIVER.name
                            info.script should have substring ".java"

                            val select = QueryBuilder.selectFrom("test1").column("value")
                                    .whereColumn("space").isEqualTo(QueryBuilder.bindMarker())
                                    .whereColumn("key").isEqualTo(QueryBuilder.bindMarker())

//                            select.where(eq("space", "web")).and(eq("key", "google"))

                            val row = getKeyspaceSession().execute(select.build("web","google")).one()
                            row?.getString("value") shouldBe "google.com"
                        }
                        "2.0.0" -> {
                            info.description shouldBe "Second"
                            info.type.name shouldBe MigrationType.CQL.name
                            info.script should have substring ".cql"

                            val select = QueryBuilder.selectFrom("contents")
                                    .column("title")
                                    .column("message")
                            select.whereColumn("id").isEqualTo(QueryBuilder.literal(1))
                            val row = getKeyspaceSession().execute(select.build()).one()
                            row?.getString("title") shouldBe "foo"
                            row?.getString("message") shouldBe "meh"
                        }
                        "1.2.0" -> {
                            info.description shouldBe "First delete temp"
                            info.type.name shouldBe MigrationType.CQL.name
                            info.script should have substring ".cql"

                            val select = QueryBuilder.selectFrom("test2").all()
                            shouldThrow<InvalidQueryException> { getKeyspaceSession().execute(select.build()) }
                        }
                        "1.1.0" -> {
                            info.description shouldBe "First create temp"
                            info.type.name shouldBe MigrationType.CQL.name
                            info.script should have substring ".cql"

                            val select = QueryBuilder.selectFrom("test2").all()
                            shouldThrow<InvalidQueryException> { getKeyspaceSession().execute(select.build()) }
                        }
                        "1.0.0" -> {
                            info.description shouldBe "First"
                            info.type.name shouldBe MigrationType.CQL.name
                            info.script should have substring ".cql"

                            val select = QueryBuilder.selectFrom("test1").column("value")
                                    .whereColumn("space").isEqualTo(QueryBuilder.bindMarker())
                                    .whereColumn("key").isEqualTo(QueryBuilder.bindMarker())
//                            select.where(eq("space", "foo")).and(eq("key", "blah"))
                            val row = getKeyspaceSession().execute(select.build("foo","blah")).one()
                            row?.getString("value") shouldBe "profit!"
                        }
                    }

                    info.state.isApplied shouldBe true
                    info.installedOn should be a(Date::class)
                }

                // NOTE: Test out of order when out of order is not allowed
                val outOfOrderScriptsLocations = arrayOf("migration/integ_outoforder", "migration/integ/java")
                cm = CassandraMigration()
                cm.locations = outOfOrderScriptsLocations
                cm.keyspaceConfig = getKeyspace()
                cm.migrate()

                infoService = cm.info()
                println("Out of order migration with out-of-order ignored")
                println(MigrationInfoDumper.dumpToAsciiTable(infoService.all()))

                infoService.all().size shouldBe 7

                for (info in infoService.all()) {
                    forAtLeastOne(arrayOf("1.0.0", "1.1.0", "1.2.0", "2.0.0", "3.0", "3.0.1", "1.1.1")) { info.version.version shouldBe it }

                    when (info.version.version) {
                        "1.1.1" -> {
                            info.description shouldBe "Late arrival"
                            info.type.name shouldBe MigrationType.CQL.name
                            info.script should have substring ".cql"
                            info.state.isApplied shouldBe false
                            info.installedOn shouldBe null
                        }
                    }
                }

                // NOTE: Test out of order when out of order is allowed
                val outOfOrder2ScriptsLocations = arrayOf("migration/integ_outoforder2", "migration/integ/java")
                cm = CassandraMigration()
                cm.locations = outOfOrder2ScriptsLocations
                cm.allowOutOfOrder = true
                cm.keyspaceConfig = getKeyspace()
                cm.migrate()

                infoService = cm.info()
                println("Out of order migration with out-of-order allowed")
                println(MigrationInfoDumper.dumpToAsciiTable(infoService.all()))

                infoService.all().size shouldBe 8

                for (info in infoService.all()) {
                    forAtLeastOne(arrayOf("1.0.0", "1.1.0", "1.2.0", "2.0.0", "3.0", "3.0.1", "1.1.1", "1.1.2")) { info.version.version shouldBe it }

                    when (info.version.version) {
                        "1.1.2" -> {
                            info.description shouldBe "Late arrival2"
                            info.type.name shouldBe MigrationType.CQL.name
                            info.script should have substring ".cql"
                            info.state.isApplied shouldBe true
                            info.installedOn should be a(Date::class)
                        }
                    }
                }

                // NOTE: Test out of order when out of order is allowed again
                val outOfOrder3ScriptsLocations = arrayOf("migration/integ_outoforder3", "migration/integ/java")
                cm = CassandraMigration()
                cm.locations = outOfOrder3ScriptsLocations
                cm.allowOutOfOrder = true
                cm.keyspaceConfig = getKeyspace()
                cm.migrate()

                infoService = cm.info()
                println("Out of order migration with out-of-order allowed")
                println(MigrationInfoDumper.dumpToAsciiTable(infoService.all()))

                infoService.all().size shouldBe 9

                for (info in infoService.all()) {
                    forAtLeastOne(arrayOf("1.0.0", "1.1.0", "1.2.0", "2.0.0", "3.0", "3.0.1", "1.1.1", "1.1.2", "1.1.3")) { info.version.version shouldBe it }

                    when (info.version.version) {
                        "1.1.3" -> {
                            info.description shouldBe "Late arrival3"
                            info.type.name shouldBe MigrationType.CQL.name
                            info.script should have substring ".cql"
                            info.state.isApplied shouldBe true
                            info.installedOn should be a(Date::class)
                        }
                    }
                }
            }

        }

    }

}
