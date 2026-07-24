package dk.wromble.app.data

import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

const val BASE_URL = "https://wromble.dk"

interface WrombleApi {

    // --- Public catalogue ---
    @GET("api/restaurants.php")
    suspend fun restaurants(): RestaurantsResponse

    @GET("api/home-categories.php")
    suspend fun homeCategories(): CategoriesResponse

    @GET("api/menu.php")
    suspend fun menu(@Query("company_id") companyId: Int): MenuResponse

    // --- Auth ---
    @POST("api/login.php")
    suspend fun login(@Body body: Map<String, @JvmSuppressWildcards Any>): LoginResponse

    @POST("api/register.php")
    suspend fun register(@Body body: Map<String, @JvmSuppressWildcards Any>): LoginResponse

    // --- Orders (customer) ---
    @GET("api/orders.php")
    suspend fun orders(@Query("user_id") userId: Int): OrdersResponse

    @GET("api/order-status.php")
    suspend fun orderStatus(@Query("order_id") orderId: Int): OrderStatus

    @POST("api/place-order.php")
    suspend fun placeOrder(@Body body: Map<String, @JvmSuppressWildcards Any>): PlaceOrderResponse

    @POST("api/register-push-token.php")
    suspend fun registerPushToken(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    @POST("api/app-tip-checkout.php")
    suspend fun tipCheckout(@Body body: Map<String, @JvmSuppressWildcards Any>): TipCheckoutResponse

    // --- Profile (customer) ---
    @GET("api/app-user-profile.php")
    suspend fun userProfile(@Query("user_id") userId: Int): CustomerProfileResponse

    @POST("api/app-user-profile.php")
    suspend fun updateUserProfile(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    @POST("api/delete-account.php")
    suspend fun deleteAccount(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    // --- Staff: driver ---
    @GET("api/app-driver-orders.php")
    suspend fun driverOrders(
        @Query("rider_id") riderId: Int,
        @Query("company_id") companyId: Int
    ): DriverOrdersResponse

    @GET("api/app-driver-history.php")
    suspend fun driverHistory(
        @Query("rider_id") riderId: Int,
        @Query("company_id") companyId: Int,
        @Query("include_failed") includeFailed: Int = 1
    ): DriverOrdersResponse

    @POST("api/app-driver-take.php")
    suspend fun driverTake(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    @POST("api/app-driver-deliver.php")
    suspend fun driverDeliver(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    @POST("api/driver-location.php")
    suspend fun driverLocation(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    // --- Staff: company ---
    @GET("api/app-company-orders.php")
    suspend fun companyOrders(
        @Query("company_id") companyId: Int,
        @Query("scope") scope: String,
        @Query("include_failed") includeFailed: Int = 1
    ): CompanyOrdersResponse

    @POST("api/app-company-order-action.php")
    suspend fun companyOrderAction(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    @GET("api/app-company-busy.php")
    suspend fun companyBusy(@Query("company_id") companyId: Int): BusyResponse

    @POST("api/app-company-busy.php")
    suspend fun setCompanyBusy(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    @GET("api/app-company-autoaccept.php")
    suspend fun companyAutoAccept(@Query("company_id") companyId: Int): AutoAcceptResponse

    @POST("api/app-company-autoaccept.php")
    suspend fun setCompanyAutoAccept(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    @GET("api/app-company-profile.php")
    suspend fun companyProfile(@Query("company_id") companyId: Int): CompanyProfileResponse

    @POST("api/app-company-profile.php")
    suspend fun saveCompanyProfile(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    @GET("api/app-company-hours.php")
    suspend fun companyHours(@Query("company_id") companyId: Int): CompanyHoursResponse

    @POST("api/app-company-hours.php")
    suspend fun saveCompanyHours(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    // --- Menu CRUD (company) ---
    @POST("api/app-menu-category.php")
    suspend fun menuCategory(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    @POST("api/app-menu-item.php")
    suspend fun menuItem(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    // --- Tips / Stripe (driver + company) ---
    @GET("api/app-tips-balance.php")
    suspend fun tipsBalance(
        @Query("type") type: String,
        @Query("id") id: Int
    ): TipsBalanceResponse

    @GET("api/app-stripe-connect.php")
    suspend fun stripeConnectStatus(
        @Query("type") type: String,
        @Query("id") id: Int
    ): StripeConnectResponse

    @POST("api/app-stripe-connect.php")
    suspend fun stripeConnect(@Body body: Map<String, @JvmSuppressWildcards Any>): StripeConnectResponse

    @POST("api/app-tips-payout.php")
    suspend fun tipsPayout(@Body body: Map<String, @JvmSuppressWildcards Any>): PayoutResponse

    // --- Jobs / forms ---
    @GET("api/app-jobs.php")
    suspend fun jobs(): JobsResponse

    @POST("api/app-job-apply.php")
    suspend fun jobApply(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    @POST("api/app-contact.php")
    suspend fun contact(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    @POST("api/app-partner.php")
    suspend fun partner(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    // --- Chat ---
    @POST("api/chat-start.php")
    suspend fun chatStart(@Body body: Map<String, @JvmSuppressWildcards Any>): ChatStartResponse

    @POST("api/chat-send.php")
    suspend fun chatSend(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    @GET("api/chat-poll.php")
    suspend fun chatPoll(
        @Query("conversation_id") conversationId: Int,
        @Query("after") after: Int
    ): ChatPollResponse

    @Multipart
    @POST("api/chat-upload.php")
    suspend fun chatUpload(
        @Part("conversation_id") conversationId: RequestBody,
        @Part("sender_type") senderType: RequestBody,
        @Part("sender_name") senderName: RequestBody,
        @Part file: MultipartBody.Part
    ): SimpleResponse
}

// Retry-with-backoff for the shared host, which returns HTTP 429 under load.
private class RetryInterceptor(private val maxRetries: Int = 2) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var response = chain.proceed(chain.request())
        while (response.code == 429 && attempt < maxRetries) {
            response.close()
            try { Thread.sleep(400L * (attempt + 1)) } catch (_: InterruptedException) {}
            attempt++
            response = chain.proceed(chain.request())
        }
        return response
    }
}

object Http {
    // Shared client with a bounded dispatcher so we never flood the shared host
    // (mirrors the iOS CachedAsyncImage concurrency gate + retry).
    val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val dispatcher = Dispatcher().apply {
            maxRequests = 12
            maxRequestsPerHost = 6
        }
        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(RetryInterceptor())
            .addInterceptor(logging)
            .build()
    }
}

object Api {
    val service: WrombleApi by lazy {
        Retrofit.Builder()
            .baseUrl("$BASE_URL/")
            .client(Http.client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WrombleApi::class.java)
    }
}
