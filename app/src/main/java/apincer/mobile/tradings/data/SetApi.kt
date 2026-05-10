package apincer.mobile.tradings.data

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface SetApi {
    @GET("api/set/stock/{symbol}/info")
    suspend fun getStockInfo(
        @Path("symbol") symbol: String,
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        @Header("Referer") referer: String = "https://www.set.or.th/",
        @Header("Accept") accept: String = "application/json, text/plain, */*",
        @Header("Accept-Language") lang: String = "en-US,en;q=0.9,th;q=0.8",
        @Header("Connection") connection: String = "keep-alive"
    ): StockInfoResponse

    @GET("api/set/stock/{symbol}/historical-trading")
    suspend fun getHistoricalTrading(
        @Path("symbol") symbol: String,
        @Query("limit") limit: Int = 100,
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        @Header("Referer") referer: String = "https://www.set.or.th/",
        @Header("Accept") accept: String = "application/json, text/plain, */*",
        @Header("Accept-Language") lang: String = "en-US,en;q=0.9,th;q=0.8",
        @Header("Connection") connection: String = "keep-alive"
    ): HistoricalTradingResponse
}
