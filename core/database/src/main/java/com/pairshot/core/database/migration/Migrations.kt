package com.pairshot.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=OFF")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS photo_pairs_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    beforePhotoUri TEXT,
                    afterPhotoUri TEXT,
                    beforeTimestamp INTEGER NOT NULL,
                    afterTimestamp INTEGER,
                    status TEXT NOT NULL,
                    zoomLevel REAL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO photo_pairs_new (id, beforePhotoUri, afterPhotoUri, beforeTimestamp, afterTimestamp, status, zoomLevel)
                SELECT id, beforePhotoUri, afterPhotoUri, beforeTimestamp, afterTimestamp, status, zoomLevel
                FROM photo_pairs
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE photo_pairs")
            db.execSQL("ALTER TABLE photo_pairs_new RENAME TO photo_pairs")
            db.execSQL("PRAGMA foreign_key_check")
            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }

@Suppress("MagicNumber")
val MIGRATION_2_3: Migration =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE photo_pairs ADD COLUMN aspectRatio TEXT")
        }
    }
