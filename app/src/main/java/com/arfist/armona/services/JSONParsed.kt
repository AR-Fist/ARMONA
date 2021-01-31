package com.arfist.armona.services

data class Direction(
    val geocoded_waypoints: Array<Geocode>?,
    val routes: Array<Route>?,
    val status: String?
    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Direction

        if (geocoded_waypoints != null) {
            if (other.geocoded_waypoints == null) return false
            if (!geocoded_waypoints.contentEquals(other.geocoded_waypoints)) return false
        } else if (other.geocoded_waypoints != null) return false
        if (routes != null) {
            if (other.routes == null) return false
            if (!routes.contentEquals(other.routes)) return false
        } else if (other.routes != null) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = geocoded_waypoints?.contentHashCode() ?: 0
        result = 31 * result + (routes?.contentHashCode() ?: 0)
        result = 31 * result + (status?.hashCode() ?: 0)
        return result
    }
}

data class Geocode(
    val geocoder_status: String?,
    val partial_match: Boolean?,
    val place_id: String?,
    val types: Array<String>?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Geocode

        if (geocoder_status != other.geocoder_status) return false
        if (partial_match != other.partial_match) return false
        if (place_id != other.place_id) return false
        if (types != null) {
            if (other.types == null) return false
            if (!types.contentEquals(other.types)) return false
        } else if (other.types != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = geocoder_status?.hashCode() ?: 0
        result = 31 * result + (partial_match?.hashCode() ?: 0)
        result = 31 * result + (place_id?.hashCode() ?: 0)
        result = 31 * result + (types?.contentHashCode() ?: 0)
        return result
    }
}

data class Route(
    val bounds: Boundary?,
    val copyrights: String?,
    val legs: Array<Leg>?,
    val overview_polyline: Points?,
    val summary: String?,
    val warnings: Array<String>?,
    val waypoint_order: Array<String>?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Route

        if (bounds != other.bounds) return false
        if (copyrights != other.copyrights) return false
        if (legs != null) {
            if (other.legs == null) return false
            if (!legs.contentEquals(other.legs)) return false
        } else if (other.legs != null) return false
        if (overview_polyline != other.overview_polyline) return false
        if (summary != other.summary) return false
        if (warnings != null) {
            if (other.warnings == null) return false
            if (!warnings.contentEquals(other.warnings)) return false
        } else if (other.warnings != null) return false
        if (waypoint_order != null) {
            if (other.waypoint_order == null) return false
            if (!waypoint_order.contentEquals(other.waypoint_order)) return false
        } else if (other.waypoint_order != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bounds?.hashCode() ?: 0
        result = 31 * result + (copyrights?.hashCode() ?: 0)
        result = 31 * result + (legs?.contentHashCode() ?: 0)
        result = 31 * result + (overview_polyline?.hashCode() ?: 0)
        result = 31 * result + (summary?.hashCode() ?: 0)
        result = 31 * result + (warnings?.contentHashCode() ?: 0)
        result = 31 * result + (waypoint_order?.contentHashCode() ?: 0)
        return result
    }
}

data class Leg(
    val distance: Distance?,
    val duration: JSONDuration?,
    val end_address: String?,
    val end_location: JSONLatLng?,
    val start_address: String?,
    val start_location: JSONLatLng?,
    val steps: Array<Step>?,
    val traffic_speed_entry: Array<String>?,
    val via_waypoint: Array<String>?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Leg

        if (distance != other.distance) return false
        if (duration != other.duration) return false
        if (end_address != other.end_address) return false
        if (end_location != other.end_location) return false
        if (start_address != other.start_address) return false
        if (start_location != other.start_location) return false
        if (steps != null) {
            if (other.steps == null) return false
            if (!steps.contentEquals(other.steps)) return false
        } else if (other.steps != null) return false
        if (traffic_speed_entry != null) {
            if (other.traffic_speed_entry == null) return false
            if (!traffic_speed_entry.contentEquals(other.traffic_speed_entry)) return false
        } else if (other.traffic_speed_entry != null) return false
        if (via_waypoint != null) {
            if (other.via_waypoint == null) return false
            if (!via_waypoint.contentEquals(other.via_waypoint)) return false
        } else if (other.via_waypoint != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = distance?.hashCode() ?: 0
        result = 31 * result + (duration?.hashCode() ?: 0)
        result = 31 * result + (end_address?.hashCode() ?: 0)
        result = 31 * result + (end_location?.hashCode() ?: 0)
        result = 31 * result + (start_address?.hashCode() ?: 0)
        result = 31 * result + (start_location?.hashCode() ?: 0)
        result = 31 * result + (steps?.contentHashCode() ?: 0)
        result = 31 * result + (traffic_speed_entry?.contentHashCode() ?: 0)
        result = 31 * result + (via_waypoint?.contentHashCode() ?: 0)
        return result
    }
}

data class Distance(
    val text: String?,
    val value: Int?
)

data class JSONDuration(
    val text: String?,
    val value: Int?
)

data class Step(
    val distance: Distance?,
    val duration: JSONDuration?,
    val end_location: JSONLatLng?,
    val html_instructions: String?,
    val maneuver: String?,
    val polyline: Points?,
    val start_location: JSONLatLng?,
    val travel_mode: String?
)

data class Points(
    val points: String?
)

data class Boundary(
    val northeast: JSONLatLng?,
    val southwest: JSONLatLng?
)
data class JSONLatLng(
    val lat: Double?,
    val lng: Double?
)
