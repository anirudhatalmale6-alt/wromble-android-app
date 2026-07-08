package dk.wromble.app.data

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
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

    // --- Profile ---
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

    @POST("api/app-driver-deliver.php")
    suspend fun driverDeliver(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    // --- Staff: company ---
    @GET("api/app-company-orders.php")
    suspend fun companyOrders(
        @Query("company_id") companyId: Int,
        @Query("scope") scope: String
    ): CompanyOrdersResponse

    @POST("api/app-company-order-action.php")
    suspend fun companyOrderAction(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    @GET("api/app-company-profile.php")
    suspend fun companyProfile(@Query("company_id") companyId: Int): retrofit2.Response<okhttp3.ResponseBody>

    @POST("api/app-company-profile.php")
    suspend fun saveCompanyProfile(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    @GET("api/app-company-hours.php")
    suspend fun companyHours(@Query("company_id") companyId: Int): retrofit2.Response<okhttp3.ResponseBody>

    @POST("api/app-company-hours.php")
    suspend fun saveCompanyHours(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleResponse

    // --- Jobs / forms ---
    @GET("api/app-jobs.php")
    suspend fun jobs(): JobsResponse

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
        @Part("sender_id") senderId: RequestBody,
        @Part("sender_type") senderType: RequestBody,
        @Part file: MultipartBody.Part
    ): SimpleResponse
}

object Api {
    val service: WrombleApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
        Retrofit.Builder()
            .baseUrl("$BASE_URL/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WrombleApi::class.java)
    }
}
