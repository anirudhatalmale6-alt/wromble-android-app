package dk.wromble.app.data

import com.google.gson.annotations.SerializedName

// MARK: - Core models (mirror iOS ContentView.swift)

data class UserProfile(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    val type: String = "customer",
    @SerializedName("company_id") val companyId: Int = 0,
    val role: String = "",
    // Sikkerheds-token udstedt af serveren ved login. Sendes med hver forespoergsel
    // (Authorization: Bearer) saa serveren selv kan afgoere hvem kalderen er.
    val token: String? = null
)

data class Restaurant(
    val id: Int,
    val name: String,
    val alias: String = "",
    val type: Int = 0,
    @SerializedName("type_label") val typeLabel: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val image: String? = null,
    val logo: String? = null,
    val categories: Int = 0,
    val items: Int = 0
)

data class MenuCategory(
    val id: Int,
    val name: String,
    val products: List<MenuItem> = emptyList()
)

data class MenuItem(
    val id: Int,
    val name: String,
    val description: String? = null,
    val price: Double = 0.0,
    val image: String? = null,
    @SerializedName("extra_images") val extraImages: List<String>? = null
)

data class OrderItem(
    val name: String = "Ukendt",
    val quantity: Int = 1,
    val price: Double = 0.0
)

data class Order(
    val id: Int,
    @SerializedName("company_name") val companyName: String = "Ukendt",
    val date: String = "",
    val total: Double = 0.0,
    val status: String = "pending",
    val items: List<OrderItem> = emptyList()
)

// Home product categories (api/home-categories.php)
data class ProductCatCompany(val id: Int, val product: String? = null)

data class CatProduct(
    val id: Int,
    val name: String,
    val price: Double? = null,
    val image: String? = null,
    @SerializedName("company_id") val companyId: Int = 0,
    @SerializedName("company_name") val companyName: String? = null
)

data class ProductCat(
    val key: String,
    val name: String,
    val image: String? = null,
    val companies: List<ProductCatCompany> = emptyList(),
    val products: List<CatProduct>? = null
)

// Order tracking (api/order-status.php)
data class OrderStatus(
    val stage: Int = 0,          // -1 afvist, 0 modtaget, 1 bekraeftet, 2 paa vej, 3 leveret
    val label: String = "",
    val description: String = "",
    @SerializedName("company_name") val companyName: String = "",
    val total: Double = 0.0,
    @SerializedName("is_delivery") val isDelivery: Boolean = false,
    @SerializedName("company_lat") val companyLat: Double = 0.0,
    @SerializedName("company_lng") val companyLng: Double = 0.0,
    @SerializedName("company_address") val companyAddress: String = "",
    @SerializedName("customer_lat") val customerLat: Double = 0.0,
    @SerializedName("customer_lng") val customerLng: Double = 0.0,
    @SerializedName("rider_lat") val riderLat: Double = 0.0,
    @SerializedName("rider_lng") val riderLng: Double = 0.0,
    @SerializedName("rider_live") val riderLive: Boolean = false,
    @SerializedName("eta_text") val etaText: String = "",
    @SerializedName("eta_minutes") val etaMinutes: Int = 0,
    @SerializedName("rider_distance_m") val riderDistanceM: Int = 0,
    // "Leveres af" ved levering, "Afhentes hos" ved afhentning (kommer fra serveren).
    @SerializedName("pickup_label") val pickupLabel: String = ""
)

// Chat
data class ChatMessage(
    val id: Int,
    @SerializedName("sender_type") val senderType: String = "",
    @SerializedName("sender_name") val senderName: String = "",
    val message: String = "",
    @SerializedName("file_url") val fileUrl: String? = null,
    @SerializedName("file_type") val fileType: String? = null,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("created_at") val createdAt: String = ""
)

// Staff models (match api/app-driver-orders.php + app-company-orders.php)
data class DriverOrder(
    val id: Int,
    val customer: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val amount: Double = 0.0,
    val company: String = "",
    val delivery: Boolean = true,
    val mine: Boolean = false,
    val failed: Boolean = false,
    @SerializedName("order_time") val orderTime: String = "",
    @SerializedName("deliver_time") val deliverTime: String = "",
    @SerializedName("date_label") val dateLabel: String = "",
    @SerializedName("eta_text") val etaText: String? = null,
    val items: List<CompanyOrderItem> = emptyList()
)

data class CompanyOrderItem(
    val name: String = "",
    val qty: Int = 1,
    val price: Double = 0.0
)

data class CompanyOrder(
    val id: Int,
    val customer: String = "",
    val phone: String = "",
    val address: String = "",
    val amount: Double = 0.0,
    val delivery: Boolean = false,
    val payment: Int = 0,
    val table: Boolean = false,
    @SerializedName("is_new") val isNew: Boolean = false,
    val delivered: Boolean = false,
    val status: String = "",
    val items: List<CompanyOrderItem> = emptyList(),
    val date: Long = 0,
    @SerializedName("wanted_time") val wantedTime: String? = null,
    val overdue: Boolean = false,
    @SerializedName("eta_text") val etaText: String? = null,
    @SerializedName("rider_name") val riderName: String? = null,
    @SerializedName("rider_phone") val riderPhone: String? = null
)

// Company back-office
data class CompanyProfile(
    val id: Int = 0,
    val companyname: String = "",
    val email: String = "",
    val companytype: String = "",
    @SerializedName("firstname_contact") val firstnameContact: String = "",
    @SerializedName("lastname_contact") val lastnameContact: String = "",
    val adress: String = "",
    val zipcode: String = "",
    val city: String = "",
    @SerializedName("phone_mobile") val phoneMobile: String = "",
    val specialities: String = "",
    val website: String = "",
    val description: String = "",
    val briefdescription: String = "",
    @SerializedName("shop_status") val shopStatus: String = "",
    @SerializedName("com_delivery") val comDelivery: Int = 0,
    @SerializedName("com_delivery_price") val comDeliveryPrice: String = "",
    @SerializedName("com_delivery_time") val comDeliveryTime: String = "",
    val logo: String? = null
)

data class CompanyHourDay(
    val weekday: String = "",
    @SerializedName("store_open") val storeOpen: String = "",
    @SerializedName("store_close") val storeClose: String = "",
    @SerializedName("bring_open") val bringOpen: String = "",
    @SerializedName("bring_close") val bringClose: String = ""
)

// Customer profile edit (api/app-user-profile.php)
data class CustomerProfileData(
    val id: Int = 0,
    val firstname: String = "",
    val lastname: String = "",
    val email: String = "",
    val adress: String = "",
    val zipcode: String = "",
    val city: String = "",
    val phone: String = ""
)

// Stripe tips / earnings
data class StripeTipRow(
    val id: Int = 0,
    val amount: Double = 0.0,
    @SerializedName("order_id") val orderId: Int = 0,
    val date: String = ""
)

data class JobPost(
    val id: Int,
    val title: String = "",
    val company: String? = null,
    val location: String? = null,
    val description: String? = null,
    val body: String? = null,
    val hours: String? = null,
    val deadline: String? = null,
    val type: String? = null
)

// MARK: - API response envelopes
data class RestaurantsResponse(val restaurants: List<Restaurant> = emptyList())
data class CategoriesResponse(val categories: List<ProductCat> = emptyList())
data class MenuResponse(val company: Restaurant? = null, val categories: List<MenuCategory> = emptyList())
data class OrdersResponse(val orders: List<Order> = emptyList())
data class LoginResponse(val user: UserProfile? = null, val error: String? = null)
data class PlaceOrderResponse(
    @SerializedName("order_id") val orderId: Int? = null,
    val error: String? = null
)
data class DriverOrdersResponse(val orders: List<DriverOrder> = emptyList())
data class CompanyOrdersResponse(val orders: List<CompanyOrder> = emptyList())
data class DriverPhoneResponse(val success: Boolean = false, val phone: String = "")
data class JobsResponse(val jobs: List<JobPost> = emptyList())

// Back-office / profile / tips envelopes
data class CompanyProfileResponse(val profile: CompanyProfile? = null, val error: String? = null)
data class CompanyHoursResponse(
    val days: List<CompanyHourDay> = emptyList(),
    @SerializedName("shop_status") val shopStatus: String? = null
)
data class CustomerProfileResponse(val profile: CustomerProfileData? = null, val error: String? = null)
data class MenuAdminResponse(
    val company: Restaurant? = null,
    val categories: List<MenuCategory> = emptyList()
)
data class TipsBalanceResponse(
    val balance: Double = 0.0,
    val earned: Double = 0.0,
    @SerializedName("paid_out") val paidOut: Double = 0.0,
    val tips: List<StripeTipRow> = emptyList()
)
data class StripeConnectResponse(
    @SerializedName("payouts_enabled") val payoutsEnabled: Boolean = false,
    @SerializedName("details_submitted") val detailsSubmitted: Boolean = false,
    @SerializedName("onboarding_url") val onboardingUrl: String? = null,
    @SerializedName("connect_disabled") val connectDisabled: Boolean = false,
    val error: String? = null
)
data class TipCheckoutResponse(
    @SerializedName("checkout_url") val checkoutUrl: String? = null,
    val error: String? = null
)
data class BusyResponse(val busy: Int = 0)
data class AutoAcceptResponse(@SerializedName("auto_accept") val autoAccept: Int = 0)
data class AlarmResponse(@SerializedName("alarm_seconds") val alarmSeconds: Int = 5)
data class PayoutResponse(val success: Boolean = false, val amount: Double = 0.0, val error: String? = null)
data class ChatStartResponse(
    @SerializedName("conversation_id") val conversationId: Int? = null,
    val error: String? = null
)
data class ChatPollResponse(
    val status: String = "open",
    val messages: List<ChatMessage> = emptyList()
)
data class SimpleResponse(val success: Boolean = false, val error: String? = null, val message: String? = null)

// Forudprintet QR-kode opslag (api/qr-resolve.php)
data class QrResolveResponse(
    val linked: Boolean = false,
    val alias: String? = null,
    @SerializedName("company_id") val companyId: Int = 0,
    @SerializedName("company_name") val companyName: String? = null,
    @SerializedName("table_no") val tableNo: Int? = null
)

// Cart (client-side only)
data class CartItem(
    val id: Int,
    val name: String,
    val price: Double,
    var quantity: Int
)
