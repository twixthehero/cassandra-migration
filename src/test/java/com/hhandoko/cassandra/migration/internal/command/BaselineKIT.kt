/**
 * File     : BaselineKIT.kt
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
package com.hhandoko.cassandra.migration.internal.command

import com.hhandoko.cassandra.migration.BaseKIT
import com.hhandoko.cassandra.migration.CassandraMigration
import com.hhandoko.cassandra.migration.api.CassandraMigrationException
import com.hhandoko.cassandra.migration.api.MigrationVersion
import com.hhandoko.cassandra.migration.internal.dbsupport.SchemaVersionDAO

/**
 * Baseline command unit tests.
 */
class BaselineKIT : BaseKIT() {

    init {

        "Baseline command API" - {

            "should mark at first migration script" - {

                "with default table prefix" - {

                    "for session and keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.baseline()

                        val schemaVersionDAO = SchemaVersionDAO(getSession(), getKeyspace(), MigrationVersion.CURRENT.table)
                        val baselineMarker = schemaVersionDAO.baselineMarker

                        baselineMarker?.version shouldBe MigrationVersion.fromVersion("1")
                    }

                    "for session and keyspace, version and description setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.baselineVersion = MigrationVersion.fromVersion("0.0.1")
                        cm.baselineDescription = "Baseline test"
                        cm.baseline()

                        val schemaVersionDAO = SchemaVersionDAO(getSession(), getKeyspace(), MigrationVersion.CURRENT.table)
                        val baselineMarker = schemaVersionDAO.baselineMarker

                        baselineMarker?.version shouldBe MigrationVersion.fromVersion("0.0.1")
                    }

                    "for external session, but keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getKeyspaceSession()
                        val cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.baseline(session)

                        val schemaVersionDAO = SchemaVersionDAO(getSession(), getKeyspace(), MigrationVersion.CURRENT.table)
                        val baselineMarker = schemaVersionDAO.baselineMarker

                        baselineMarker?.version shouldBe MigrationVersion.fromVersion("1")
                    }

                    "for external session and defaulted keyspace" {
                        /* session with defaulted keyspace doesnt work, seems
                        cassandra 4.* allows setting keyspace only
                        at creation time, so provided keyspace */
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getSession(CASSANDRA_USERNAME,CASSANDRA_PASSWORD)
                        val cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.baseline(session)

                        val schemaVersionDAO = SchemaVersionDAO(getSession(), getKeyspace(), MigrationVersion.CURRENT.table)
                        val baselineMarker = schemaVersionDAO.baselineMarker

                        baselineMarker?.version shouldBe MigrationVersion.fromVersion("1")
                    }
                }

                "with user-defined table prefix" - {

                    "for session and keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"
                        cm.baseline()

                        val schemaVersionDAO = SchemaVersionDAO(getSession(), getKeyspace(), cm.tablePrefix + MigrationVersion.CURRENT.table)
                        val baselineMarker = schemaVersionDAO.baselineMarker

                        baselineMarker?.version shouldBe MigrationVersion.fromVersion("1")
                    }

                    "for session and keyspace, version and description setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"
                        cm.baselineVersion = MigrationVersion.fromVersion("0.0.1")
                        cm.baselineDescription = "Baseline test"
                        cm.baseline()

                        val schemaVersionDAO = SchemaVersionDAO(getSession(), getKeyspace(), cm.tablePrefix + MigrationVersion.CURRENT.table)
                        val baselineMarker = schemaVersionDAO.baselineMarker

                        baselineMarker?.version shouldBe MigrationVersion.fromVersion("0.0.1")
                    }

                    "for external session, but keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getKeyspaceSession()
                        val cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"
                        cm.baseline(session)

                        val schemaVersionDAO = SchemaVersionDAO(getSession(), getKeyspace(), cm.tablePrefix + MigrationVersion.CURRENT.table)
                        val baselineMarker = schemaVersionDAO.baselineMarker

                        baselineMarker?.version shouldBe MigrationVersion.fromVersion("1")
                    }

                    "for external session and defaulted keyspace" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getKeyspaceSession()
                        val cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"
                        cm.baseline(session)

                        val schemaVersionDAO = SchemaVersionDAO(getSession(), getKeyspace(), cm.tablePrefix + MigrationVersion.CURRENT.table)
                        val baselineMarker = schemaVersionDAO.baselineMarker

                        baselineMarker?.version shouldBe MigrationVersion.fromVersion("1")
                    }

                }

            }

            "should throw exception when baselining after successful migration" - {

                "with default table prefix" - {

                    "for session and keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        var cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.migrate()

                        cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()

                        shouldThrow<CassandraMigrationException> { cm.baseline() }
                    }

                    "for session and keyspace, version and description setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        var cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.migrate()

                        cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.baselineVersion = MigrationVersion.fromVersion("0.0.1")
                        cm.baselineDescription = "Baseline test"

                        shouldThrow<CassandraMigrationException> { cm.baseline() }
                    }

                    "for external session, but keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getKeyspaceSession()
                        var cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.migrate(session)

                        cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()

                        shouldThrow<CassandraMigrationException> { cm.baseline(session) }
                    }

                    "for external session and defaulted keyspace" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getKeyspaceSession()
                        var cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.migrate(session)

                        cm = CassandraMigration()
                        cm.locations = scriptsLocations

                        shouldThrow<CassandraMigrationException> { cm.baseline(session) }
                    }

                }

                "with user-defined table prefix" - {

                    "for session and keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        var cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"
                        cm.migrate()

                        cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"

                        shouldThrow<CassandraMigrationException> { cm.baseline() }
                    }

                    "for session and keyspace, version and description setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        var cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"
                        cm.migrate()

                        cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"
                        cm.baselineVersion = MigrationVersion.fromVersion("0.0.1")
                        cm.baselineDescription = "Baseline test"

                        shouldThrow<CassandraMigrationException> { cm.baseline() }
                    }

                    "for external session, but keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getKeyspaceSession()
                        var cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"
                        cm.migrate(session)

                        cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"

                        shouldThrow<CassandraMigrationException> { cm.baseline(session) }
                    }

                    "for external session and defaulted keyspace" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getKeyspaceSession()
                        var cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.tablePrefix = "test1_"
                        cm.keyspaceConfig = getKeyspace()
                        cm.migrate(session)

                        cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.tablePrefix = "test1_"

                        shouldThrow<CassandraMigrationException> { cm.baseline(session) }
                    }

                }

            }

        }

    }

}
