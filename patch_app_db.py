with open('app/src/main/java/apincer/mobile/tradings/TradingMateApp.kt', 'r') as f:
    app_content = f.read()

new_app = """class TradingMateApp : Application() {

    private var _database: apincer.mobile.tradings.data.StockDatabase? = null
    private var _repository: apincer.mobile.tradings.data.StockRepository? = null

    val repository: apincer.mobile.tradings.data.StockRepository
        get() {
            val db = apincer.mobile.tradings.data.StockDatabase.getDatabase(this)
            if (_database !== db) {
                _database = db
                _repository = apincer.mobile.tradings.data.StockRepository(
                    database = db,
                    stockDao = db.stockDao(),
                    tradeDao = db.tradeDao(),
                    cashDao = db.cashDao(),
                    focusDao = db.focusDao(),
                    checklistDao = db.checklistDao(),
                    dividendDao = db.dividendDao(),
                    portfolioSnapshotDao = db.portfolioSnapshotDao(),
                    cashTransactionDao = db.cashTransactionDao(),
                    adviceEventDao = db.adviceEventDao()
                )
            }
            return _repository!!
        }
}"""
import re
app_content = re.sub(r'class TradingMateApp.*}', new_app, app_content, flags=re.DOTALL)
with open('app/src/main/java/apincer/mobile/tradings/TradingMateApp.kt', 'w') as f:
    f.write(app_content)

with open('app/src/main/java/apincer/mobile/tradings/data/RoomModels.kt', 'r') as f:
    db_content = f.read()

old_get_db = """        fun getDatabase(context: android.content.Context): StockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "stock_database"
                )
                .addMigrations(
                    MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, 
                    MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, 
                    MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
                    MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24,
                    MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33
                )
                .build()
                INSTANCE = instance
                instance
            }
        }"""

new_get_db = """        fun getDatabase(context: android.content.Context): StockDatabase {
            val prefs = context.getSharedPreferences("app_sandbox", android.content.Context.MODE_PRIVATE)
            val isSandbox = prefs.getBoolean("is_sandbox_mode", false)
            val dbName = if (isSandbox) "stock_database_sandbox" else "stock_database"
            
            return INSTANCE?.takeIf { it.openHelper.databaseName == dbName } ?: synchronized(this) {
                val currentInstance = INSTANCE
                if (currentInstance != null && currentInstance.openHelper.databaseName == dbName) {
                    currentInstance
                } else {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        StockDatabase::class.java,
                        dbName
                    )
                    .addMigrations(
                        MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, 
                        MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, 
                        MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
                        MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24,
                        MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33
                    )
                    .build()
                    INSTANCE = instance
                    instance
                }
            }
        }"""

db_content = db_content.replace(old_get_db, new_get_db)
with open('app/src/main/java/apincer/mobile/tradings/data/RoomModels.kt', 'w') as f:
    f.write(db_content)

