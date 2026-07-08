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
    val role: String = ""
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
    val total: Double = 0.0
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
    val mine: Boolean = false
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
    val items: List<CompanyOrderItem> = emptyList()
)

data class JobPost(
    val id: Int,
    val title: String = "",
    val company: String? = null,
    val location: String? = null,
    val description: String? = null,
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
data class JobsResponse(val jobs: List<JobPost> = emptyList())
data class ChatStartResponse(
    @SerializedName("conversation_id") val conversationId: Int? = null,
    val error: String? = null
)
data class ChatPollResponse(val messages: List<ChatMessage> = emptyList())
data class SimpleResponse(val success: Boolean = false, val error: String? = null, val message: String? = null)

// Cart (client-side only)
data class CartItem(
    val id: Int,
    val name: String,
    val price: Double,
    var quantity: Int
)
