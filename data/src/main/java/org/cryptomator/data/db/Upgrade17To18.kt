package org.cryptomator.data.db

import org.greenrobot.greendao.database.Database
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class Upgrade17To18 @Inject constructor() : DatabaseUpgrade(17, 18) {

	override fun internalApplyTo(db: Database, origin: Int) {
		db.beginTransaction()
		try {
			addEtagToVaultSchema(db)
			db.setTransactionSuccessful()
		} finally {
			db.endTransaction()
		}
	}

	private fun addEtagToVaultSchema(db: Database) {
		// Step 1: Rename old table
		Sql.alterTable("VAULT_ENTITY").renameTo("VAULT_ENTITY_OLD").executeOn(db)

		// Step 2: Create new table with ETAG column
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
			.foreignKey("FOLDER_CLOUD_ID", "CLOUD_ENTITY", Sql.SqlCreateTableBuilder.ForeignKeyBehaviour.ON_DELETE_SET_NULL) //
			.executeOn(db)

		// Step 3: Copy data from old table to new table
		Sql.insertInto("VAULT_ENTITY") //
			.select("_id", "FOLDER_CLOUD_ID", "FOLDER_PATH", "FOLDER_NAME", "FORMAT", "CLOUD_ENTITY.TYPE", "PASSWORD", "PASSWORD_CRYPTO_MODE", "POSITION", "SHORTENING_THRESHOLD", "DEVICE_ID", "CREATED_BY", "DESCRIPTION","ETAG") //
			.columns("_id", "FOLDER_CLOUD_ID", "FOLDER_PATH", "FOLDER_NAME", "FORMAT", "CLOUD_TYPE", "PASSWORD", "PASSWORD_CRYPTO_MODE", "POSITION", "SHORTENING_THRESHOLD", "DEVICE_ID", "CREATED_BY", "DESCRIPTION","ETAG") //
			.from("VAULT_ENTITY_OLD") //
			.join("CLOUD_ENTITY", "VAULT_ENTITY_OLD.FOLDER_CLOUD_ID") //
			.executeOn(db)

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