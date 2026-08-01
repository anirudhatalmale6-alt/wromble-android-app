package dk.wromble.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Resolve an image path the way iOS does: full URLs pass through, otherwise
// prefix with /uploads/. (api returns some full https urls and some filenames.)
fun imageUrl(p: String?): String? {
    if (p.isNullOrBlank()) return null
    return if (p.startsWith("http")) p else "$BASE_URL/uploads/$p"
}

// Session persistence
object Session {
    private const val PREFS = "wromble_prefs"
    var user: UserProfile? = null

    // Nuvaerende login-token (bruges af AuthInterceptor til at signere hver forespoergsel).
    val token: String? get() = user?.token

    fun save(ctx: Context, u: UserProfile) {
        user = u
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("id", u.id).putString("name", u.name).putString("email", u.email)
            .putString("phone", u.phone).putString("type", u.type)
            .putInt("company_id", u.companyId).putString("role", u.role)
            .putString("token", u.token)
            .apply()
    }

    fun load(ctx: Context): UserProfile? {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = p.getInt("id", 0)
        if (id == 0) return null
        user = UserProfile(
            id = id,
            name = p.getString("name", "") ?: "",
            email = p.getString("email", "") ?: "",
            phone = p.getString("phone", null),
            type = p.getString("type", "customer") ?: "customer",
            companyId = p.getInt("company_id", 0),
            role = p.getString("role", "") ?: "",
            token = p.getString("token", null)
        )
        return user
    }

    fun clear(ctx: Context) {
        user = null
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    // Sidste kunde-login huskes ogsaa EFTER log ud (i en separat prefs-fil, saa
    // clear() ovenfor ikke sletter den), saa "Velkommen tilbage"-skaermen kan vises.
    private const val LAST_PREFS = "wromble_lastlogin"
    fun saveLastLogin(ctx: Context, name: String, email: String, method: String) {
        if (email.isBlank()) return
        ctx.getSharedPreferences(LAST_PREFS, Context.MODE_PRIVATE).edit()
            .putString("name", name).putString("email", email).putString("method", method).apply()
    }
    // Returnerer (navn, email, metode) hvis der er et tidligere kunde-login, ellers null.
    fun lastLogin(ctx: Context): Triple<String, String, String>? {
        val p = ctx.getSharedPreferences(LAST_PREFS, Context.MODE_PRIVATE)
        val email = p.getString("email", null) ?: return null
        return Triple(p.getString("name", "") ?: "", email, p.getString("method", "email") ?: "email")
    }
    fun clearLastLogin(ctx: Context) {
        ctx.getSharedPreferences(LAST_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

// Cart manager (client-side, mirrors iOS CartManager)
object Cart {
    val items = mutableStateListOf<CartItem>()
    var restaurantId by mutableStateOf(0)
    var restaurantName by mutableStateOf("")

    val itemCount: Int get() = items.sumOf { it.quantity }
    val total: Double get() = items.sumOf { it.price * it.quantity }

    fun add(item: MenuItem, forRestaurant: Int, name: String) {
        if (restaurantId != forRestaurant) {
            items.clear()
            restaurantId = forRestaurant
            restaurantName = name
        }
        val idx = items.indexOfFirst { it.id == item.id }
        if (idx >= 0) items[idx] = items[idx].copy(quantity = items[idx].quantity + 1)
        else items.add(CartItem(item.id, item.name, item.price, 1))
    }

    fun setQuantity(id: Int, qty: Int) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx < 0) return
        if (qty <= 0) items.removeAt(idx)
        else items[idx] = items[idx].copy(quantity = qty)
    }

    fun clear() { items.clear(); restaurantId = 0; restaurantName = "" }
}

// Favorites manager (persisted set of restaurant ids)
object Favorites {
    private const val PREFS = "wromble_favs"
    val ids = mutableStateListOf<Int>()

    fun load(ctx: Context) {
        ids.clear()
        val saved = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet("ids", emptySet()) ?: emptySet()
        ids.addAll(saved.mapNotNull { it.toIntOrNull() })
    }

    fun isFavorite(id: Int) = ids.contains(id)

    fun toggle(ctx: Context, id: Int) {
        if (ids.contains(id)) ids.remove(id) else ids.add(id)
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet("ids", ids.map { it.toString() }.toSet()).apply()
    }
}
