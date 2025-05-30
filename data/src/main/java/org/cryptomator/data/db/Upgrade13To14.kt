package org.cryptomator.data.db

import org.greenrobot.greendao.database.Database
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class Upgrade13To14 @Inject constructor() : DatabaseUpgrade(13, 14) {

	override fun internalApplyTo(db: Database, origin: Int) {
		db.beginTransaction()
		try {
			addDeviceIDToVaultSchema(db)
			db.setTransactionSuccessful()
		} finally {
			db.endTransaction()
		}
	}

	private fun addDeviceIDToVaultSchema(db: Database) {
		Sql.alterTable("VAULT_ENTITY").renameTo("VAULT_ENTITY_OLD").executeOn(db)
		Sql.createTable("VAULT_ENTITY") //
			.id() //
			.optionalInt("FOLDER_CLOUD_ID") //
			.optionalText("FOLDER_PATH") //
			.optionalText("FOLDER_NAME") //
			.requiredText("CLOUD_TYPE") //
			.optionalText("PASSWORD") //
			.optionalText("PASSWORD_CRYPTO_MODE") //
			.optionalInt("POSITION") //
			.optionalInt("FORMAT") //
			.optionalInt("SHORTENING_THRESHOLD") //
			.optionalText("DEVICE_ID") //
			.foreignKey("FOLDER_CLOUD_ID", "CLOUD_ENTITY", Sql.SqlCreateTableBuilder.ForeignKeyBehaviour.ON_DELETE_SET_NULL) //
			.executeOn(db)

		// First insert all the data without the DEVICE_ID column
		Sql.insertInto("VAULT_ENTITY") //
			.select("_id", "FOLDER_CLOUD_ID", "FOLDER_PATH", "FOLDER_NAME", "PASSWORD", "PASSWORD_CRYPTO_MODE", "POSITION", "FORMAT", "SHORTENING_THRESHOLD", "CLOUD_ENTITY.TYPE") //
			.columns("_id", "FOLDER_CLOUD_ID", "FOLDER_PATH", "FOLDER_NAME", "PASSWORD", "PASSWORD_CRYPTO_MODE", "POSITION", "FORMAT", "SHORTENING_THRESHOLD", "CLOUD_TYPE") //
			.from("VAULT_ENTITY_OLD") //
			.join("CLOUD_ENTITY", "VAULT_ENTITY_OLD.FOLDER_CLOUD_ID") //
			.executeOn(db)

		// Then update all rows to set DEVICE_ID to NULL
		Sql.update("VAULT_ENTITY") //
			.set("DEVICE_ID", Sql.toNull()) //
			.executeOn(db)

		Sql.dropIndex("IDX_VAULT_ENTITY_FOLDER_PATH_FOLDER_CLOUD_ID").executeOn(db)

		Sql.createUniqueIndex("IDX_VAULT_ENTITY_FOLDER_PATH_FOLDER_CLOUD_ID") //
			.on("VAULT_ENTITY") //
			.asc("FOLDER_PATH") //
			.asc("FOLDER_CLOUD_ID") //
			.executeOn(db)

		Sql.dropTable("VAULT_ENTITY_OLD").executeOn(db)
	}
} 