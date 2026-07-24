package dk.wromble.app.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// Shared osmdroid setup so every map surface looks and behaves the same.
fun newMapView(ctx: Context): MapView = MapView(ctx).apply {
    setTileSource(TileSourceFactory.MAPNIK)
    setMultiTouchControls(true)
    setUseDataConnection(true)
    controller.setZoom(14.0)
    controller.setCenter(GeoPoint(55.6761, 12.5683)) // Copenhagen default
}

// A simple round coloured pin so we don't need bundled marker assets.
fun dotMarker(ctx: Context, color: Int, sizeDp: Int = 26): GradientDrawable {
    val px = (sizeDp * ctx.resources.displayMetrics.density).toInt()
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke((2 * ctx.resources.displayMetrics.density).toInt(), AndroidColor.WHITE)
        setSize(px, px)
    }
}

fun MapView.addPin(
    lat: Double,
    lng: Double,
    title: String?,
    color: Int,
    onClick: (() -> Unit)? = null
): Marker? {
    if (lat == 0.0 && lng == 0.0) return null
    val m = Marker(this)
    m.position = GeoPoint(lat, lng)
    m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    m.icon = dotMarker(context, color)
    m.title = title
    if (onClick != null) {
        m.setOnMarkerClickListener { _, _ -> onClick(); true }
    }
    overlays.add(m)
    return m
}

val WrombleRedInt = AndroidColor.rgb(226, 15, 30)
val BlueInt = AndroidColor.rgb(37, 99, 235)
