package org.cryptomator.data.db

import org.greenrobot.greendao.database.Database
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class Upgrade19To20 @Inject constructor() : DatabaseUpgrade(19, 20) {

    override fun internalApplyTo(db: Database, origin: Int) {
        db.beginTransaction()
        try {
            addSizeToVaultSchema(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun addSizeToVaultSchema(db: Database) {
        // Step 1: Rename old table
        Sql.alterTable("VAULT_ENTITY").renameTo("VAULT_ENTITY_OLD").executeOn(db)

        // Step 2: Create new table with SIZE column
        Sql.createTable("VAULT_ENTITY") //
            .id() //
            .optionalInt("FOLDER_CLOUD_ID") //
            .optionalText("FOLDER_PATH") //
            .optionalText("FOLDER_NAME") //
            .optionalInt("FORMAT") //
            .requiredText("CLOUD_TYPE") //
            .optionalText("PASSWORD") //
            .optionalText("PASSWORD_CRYPTO_MODE") //
            .optionalInt("POSITION") //
            .optionalInt("SHORTENING_THRESHOLD") //
            .optionalText("DEVICE_ID") //
            .optionalText("CREATED_BY") //
            .optionalText("DESCRIPTION") //
            .optionalText("ETAG") //
            .optionalText("STATUS") //
            .optionalText("FULL_LOCAL_PATH") //
            .optionalInt("SIZE") //
            .foreignKey("FOLDER_CLOUD_ID", "CLOUD_ENTITY", Sql.SqlCreateTableBuilder.ForeignKeyBehaviour.ON_DELETE_SET_NULL) //
            .executeOn(db)

        // Step 3: Copy data from old table to new table
        db.execSQL("""
            INSERT INTO VAULT_ENTITY (
                _id, FOLDER_CLOUD_ID, FOLDER_PATH, FOLDER_NAME, FORMAT, 
                CLOUD_TYPE, PASSWORD, PASSWORD_CRYPTO_MODE, POSITION, 
                SHORTENING_THRESHOLD, DEVICE_ID, CREATED_BY, DESCRIPTION, 
                ETAG, STATUS, FULL_LOCAL_PATH, SIZE
            )
            SELECT 
                VAULT_ENTITY_OLD._id, VAULT_ENTITY_OLD.FOLDER_CLOUD_ID, 
                VAULT_ENTITY_OLD.FOLDER_PATH, VAULT_ENTITY_OLD.FOLDER_NAME, 
                VAULT_ENTITY_OLD.FORMAT, CLOUD_ENTITY.TYPE, 
                VAULT_ENTITY_OLD.PASSWORD, VAULT_ENTITY_OLD.PASSWORD_CRYPTO_MODE, 
                VAULT_ENTITY_OLD.POSITION, VAULT_ENTITY_OLD.SHORTENING_THRESHOLD, 
                VAULT_ENTITY_OLD.DEVICE_ID, VAULT_ENTITY_OLD.CREATED_BY, 
                VAULT_ENTITY_OLD.DESCRIPTION, VAULT_ENTITY_OLD.ETAG, 
                VAULT_ENTITY_OLD.STATUS, VAULT_ENTITY_OLD.FULL_LOCAL_PATH, 
                0
            FROM VAULT_ENTITY_OLD
            JOIN CLOUD_ENTITY ON VAULT_ENTITY_OLD.FOLDER_CLOUD_ID = CLOUD_ENTITY._id
        """.trimIndent())

        // Step 4: Drop old index
        Sql.dropIndex("IDX_VAULT_ENTITY_FOLDER_PATH_FOLDER_CLOUD_ID").executeOn(db)

        // Step 5: Create new index
        Sql.createUniqueIndex("IDX_VAULT_ENTITY_FOLDER_PATH_FOLDER_CLOUD_ID") //
            .on("VAULT_ENTITY") //
            .asc("FOLDER_PATH") //
            .asc("FOLDER_CLOUD_ID") //
            .executeOn(db)

        // Step 6: Drop old table
        Sql.dropTable("VAULT_ENTITY_OLD").executeOn(db)
    }
} 