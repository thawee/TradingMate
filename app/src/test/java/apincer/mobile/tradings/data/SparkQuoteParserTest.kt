package apincer.mobile.tradings.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SparkQuoteParserTest {

    @Test
    fun parsesRealSparkResponse() {
        // Captured 2026-10-01 from v7/finance/spark?symbols=TDEX.BK,PTT.BK,XXXXNOTREAL.BK (unknown symbol omitted by Yahoo).
        val body = javaClass.classLoader!!.getResource("spark_sample.json").readText()
        val quotes = SetScraper.parseSparkQuotes(JSONObject(body), "ts").associateBy { it.symbol }

        assertEquals(setOf("TDEX", "PTT"), quotes.keys)
        val tdex = quotes.getValue("TDEX")
        assertEquals(10.33, tdex.lastPrice, 1e-9)
        assertEquals(-0.02, tdex.change, 1e-9)
        assertEquals(-0.193, tdex.percentChange, 1e-9)
        assertEquals("ThaiDEX SET50", tdex.name)
        assertEquals(93_080L, tdex.volume)
        // Spark has no fundamentals; null lets refresh keep cached values.
        assertNull(tdex.pe)
        assertNull(tdex.dividendYield)
    }

    @Test
    fun derivesChangeFromPreviousCloseWhenMissing() {
        val json = JSONObject("""
            {"spark":{"result":[{"symbol":"ABC.BK","response":[{"meta":{
              "symbol":"ABC.BK","regularMarketPrice":11.0,"chartPreviousClose":10.0,"shortName":"ABC"}}]}]}}
        """.trimIndent())
        val q = SetScraper.parseSparkQuotes(json, "ts").single()
        assertEquals(1.0, q.change, 1e-9)
        assertEquals(10.0, q.percentChange, 1e-9)
        assertEquals("ABC", q.name)
    }

    @Test
    fun skipsMissingOrZeroPrice() {
        val json = JSONObject("""
            {"spark":{"result":[
              {"symbol":"A.BK","response":[{"meta":{"symbol":"A.BK","regularMarketPrice":0}}]},
              {"symbol":"B.BK","response":[]},
              {"symbol":"C.BK"}
            ]}}
        """.trimIndent())
        assertTrue(SetScraper.parseSparkQuotes(json, "ts").isEmpty())
    }

    @Test
    fun errorResponseYieldsEmpty() {
        val json = JSONObject("""{"spark":{"result":null,"error":{"code":"Bad Request"}}}""")
        assertTrue(SetScraper.parseSparkQuotes(json, "ts").isEmpty())
    }
}
