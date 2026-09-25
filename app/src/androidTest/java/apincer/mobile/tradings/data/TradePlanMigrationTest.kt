package apincer.mobile.tradings.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TradePlanMigrationTest {
    @Test fun migrationClearsMislabelledGrowthAndSeparatesFundamentalFreshness() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "fundamental_freshness_migration_test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(32) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE stock_cache (symbol TEXT NOT NULL PRIMARY KEY, profitGrowth3Y REAL, lastUpdated TEXT)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build())
        try {
            val db = helper.writableDatabase
            db.execSQL("INSERT INTO stock_cache VALUES ('PTT', 12.5, '2026-09-25 10:00:00')")
            StockDatabase.MIGRATION_32_33.migrate(db)
            db.query("SELECT profitGrowth3Y, fundamentalsUpdatedAt, lastUpdated FROM stock_cache WHERE symbol='PTT'").use {
                assertTrue(it.moveToFirst())
                assertTrue(it.isNull(0))
                assertTrue(it.isNull(1))
                assertEquals("2026-09-25 10:00:00", it.getString(2))
            }
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun migrationRetainsLegacyHoldingAndAddsEmptyPlan() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "trade_plan_migration_test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(30) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE portfolio (symbol TEXT NOT NULL PRIMARY KEY, cost REAL NOT NULL, quantity INTEGER NOT NULL, stopLoss REAL NOT NULL)")
                        db.execSQL("CREATE TABLE stock_signal (symbol TEXT NOT NULL PRIMARY KEY)")
                        db.execSQL("CREATE TABLE trade_history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, symbol TEXT NOT NULL)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        try {
            val db = helper.writableDatabase
            db.execSQL("INSERT INTO portfolio (symbol, cost, quantity, stopLoss) VALUES ('PTT', 100.0, 100, 95.0)")
            db.execSQL("INSERT INTO trade_history (symbol) VALUES ('PTT')")
            StockDatabase.MIGRATION_30_31.migrate(db)
            StockDatabase.MIGRATION_31_32.migrate(db)
            db.query("SELECT cost, quantity, stopLoss, targetPrice, planId, exitPolicy FROM portfolio WHERE symbol = 'PTT'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(100.0, cursor.getDouble(0), 0.0)
                assertEquals(100, cursor.getInt(1))
                assertEquals(95.0, cursor.getDouble(2), 0.0)
                assertEquals(0.0, cursor.getDouble(3), 0.0)
                assertEquals("", cursor.getString(4))
                assertEquals("LEGACY", cursor.getString(5))
            }
            db.query("SELECT COUNT(*) FROM advice_event").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT planId, targetPrice, sellFees, peakPrice, playbookNote FROM trade_history WHERE symbol = 'PTT'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("", cursor.getString(0))
                assertEquals(0.0, cursor.getDouble(1), 0.0)
                assertEquals(0.0, cursor.getDouble(2), 0.0)
                assertEquals(0.0, cursor.getDouble(3), 0.0)
                assertEquals("", cursor.getString(4))
            }
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }
}
