package com.pairshot.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pairshot.core.database.migration.MIGRATION_1_2
import com.pairshot.core.database.migration.MIGRATION_2_3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            PairShotDatabase::class.java,
        )

    /**
     * Catches the v1.1.5 class of bug — any schema mismatch (column type / nullability /
     * affinity / pk position) between the migrated DB and the entity-bundle expectation
     * makes runMigrationsAndValidate throw.
     */
    @Test
    fun migrate_1_to_2_validatesSchema() {
        helper.createDatabase(dbName, 1).close()
        helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2).close()
    }

    /**
     * Verifies photo_pairs rows survive the table-rebuild migration, including the
     * formerly-NOT-NULL beforePhotoUri (which must remain readable) and the nullable
     * afterPhotoUri/zoomLevel/afterTimestamp.
     */
    @Test
    fun migrate_1_to_2_preservesPhotoPairs() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO photo_pairs (id, beforePhotoUri, afterPhotoUri, beforeTimestamp, afterTimestamp, status, zoomLevel)
                VALUES
                    (1, 'content://media/1', 'content://media/2', 1700000000000, 1700000060000, 'PAIRED', 1.0),
                    (2, 'content://media/3', NULL, 1700000010000, NULL, 'BEFORE_ONLY', NULL)
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2).use { db ->
            db.query(
                "SELECT id, beforePhotoUri, afterPhotoUri, status, zoomLevel FROM photo_pairs ORDER BY id",
            ).use { c ->
                assertTrue(c.moveToNext())
                assertEquals(1L, c.getLong(0))
                assertEquals("content://media/1", c.getString(1))
                assertEquals("content://media/2", c.getString(2))
                assertEquals("PAIRED", c.getString(3))
                assertEquals(1.0, c.getDouble(4), 0.0001)

                assertTrue(c.moveToNext())
                assertEquals(2L, c.getLong(0))
                assertEquals("content://media/3", c.getString(1))
                assertTrue(c.isNull(2))
                assertEquals("BEFORE_ONLY", c.getString(3))
                assertTrue(c.isNull(4))

                assertFalse(c.moveToNext())
            }
        }
    }

    /**
     * Verifies the FK-referencing tables (pair_album_cross_ref, export_history) keep
     * their rows pointing at the rebuilt photo_pairs by id.  Catches regressions where
     * DROP+RENAME accidentally cascade-deletes dependent rows.
     */
    @Test
    fun migrate_1_to_2_preservesForeignKeyRows() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO photo_pairs (id, beforePhotoUri, afterPhotoUri, beforeTimestamp, afterTimestamp, status, zoomLevel)
                VALUES (10, 'content://media/x', NULL, 1700000000000, NULL, 'BEFORE_ONLY', NULL)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO albums (id, name, address, latitude, longitude, createdAt, updatedAt)
                VALUES (5, 'Album A', NULL, NULL, NULL, 1700000000000, 1700000000000)
                """.trimIndent(),
            )
            db.execSQL("INSERT INTO pair_album_cross_ref (pairId, albumId) VALUES (10, 5)")
            db.execSQL(
                """
                INSERT INTO export_history (id, pairId, mediaStoreUri, kind, createdAt)
                VALUES (1, 10, 'content://media/exported', 'COMBINED', 1700000050000)
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT count(*) FROM pair_album_cross_ref WHERE pairId = 10").use { c ->
                c.moveToFirst()
                assertEquals(1, c.getInt(0))
            }
            db.query("SELECT count(*) FROM export_history WHERE pairId = 10").use { c ->
                c.moveToFirst()
                assertEquals(1, c.getInt(0))
            }
        }
    }

    /**
     * v2 -> v3 adds nullable aspectRatio column. Schema validation must pass and existing
     * rows must remain readable with aspectRatio defaulting to NULL.
     */
    @Test
    fun migrate_2_to_3_validatesSchema() {
        helper.createDatabase(dbName, 2).close()
        helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3).close()
    }

    @Test
    fun migrate_2_to_3_preservesRowsWithNullAspectRatio() {
        helper.createDatabase(dbName, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO photo_pairs (id, beforePhotoUri, afterPhotoUri, beforeTimestamp, afterTimestamp, status, zoomLevel)
                VALUES (1, 'content://media/1', 'content://media/2', 1700000000000, 1700000060000, 'PAIRED', 1.0)
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3).use { db ->
            db.query("SELECT id, beforePhotoUri, aspectRatio FROM photo_pairs WHERE id = 1").use { c ->
                assertTrue(c.moveToNext())
                assertEquals(1L, c.getLong(0))
                assertEquals("content://media/1", c.getString(1))
                assertNull(c.getString(2))
                assertFalse(c.moveToNext())
            }
        }
    }
}
