package dk.wromble.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray

/**
 * Adresse-forslag fra det officielle danske adresseregister (DAWA / Dataforsyningen).
 * Gratis, ingen API-noegle noedvendig. Bruges til "skriv-og-vaelg" adressesoegning
 * i kurven, saa kunden ikke selv skal taste hele adressen korrekt.
 *
 * Dok: https://dawadocs.dataforsyningen.dk/dok/api/autocomplete
 */
data class AddressSuggestion(
    val text: String,   // fx "Noerrebrogade 1, 2200 Koebenhavn N"
    val lat: Double,    // WGS84 (srid=4326)
    val lng: Double
)

object AddressAutocomplete {

    private const val HOST = "https://api.dataforsyningen.dk/adgangsadresser/autocomplete"

    /**
     * Slaar [query] op i DAWA og returnerer op til [limit] adresse-forslag.
     * Kaldes fra IO. Fejl (netvaerk, tomt svar) giver en tom liste - aldrig et crash.
     */
    suspend fun suggest(query: String, limit: Int = 6): List<AddressSuggestion> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val url = HOST.toHttpUrl().newBuilder()
                    .addQueryParameter("q", q)
                    .addQueryParameter("per_side", limit.toString())
                    .addQueryParameter("fuzzy", "")           // tillader smaa stavefejl
                    .addQueryParameter("srid", "4326")        // giv koordinater som lat/lng
                    .build()
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "WrombleApp/1.0 (dk.wromble.app)")
                    .build()
                Http.client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use emptyList()
                    val body = resp.body?.string().orEmpty()
                    if (body.isBlank()) return@use emptyList()
                    val arr = JSONArray(body)
                    val out = ArrayList<AddressSuggestion>(arr.length())
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        val text = obj?.optString("tekst").orEmpty()
                        if (obj == null || text.isBlank()) continue
                        val data = obj.optJSONObject("adgangsadresse") ?: obj.optJSONObject("data")
                        // DAWA srid=4326: x = laengdegrad (lng), y = breddegrad (lat)
                        val lng = data?.optDouble("x", 0.0) ?: 0.0
                        val lat = data?.optDouble("y", 0.0) ?: 0.0
                        out.add(AddressSuggestion(text, lat, lng))
                    }
                    out
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
