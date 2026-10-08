package ca.carpschool.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Point(val type: String = "Point", val coordinates: List<Double>) {
    val lng get() = coordinates[0]
    val lat get() = coordinates[1]
    companion object {
        fun of(lat: Double, lng: Double) = Point(coordinates = listOf(round6(lng), round6(lat)))
        private fun round6(v: Double) = Math.round(v * 1e6) / 1e6
    }
}

@Serializable
data class School(
    @SerialName("_id") val id: String,
    val schoolCode: String,
    val name: String,
    val domains: List<String> = emptyList(),
    val baseUrl: String,
    val lastHeartbeat: String? = null,
    val trusted: Boolean? = null,
)

@Serializable data class Campus(val coordinates: List<Double>)
@Serializable data class Limits(val homes: Int = 3, val seats: Int = 4)
@Serializable data class SchoolMeta(val schoolCode: String = "", val name: String = "", val campus: Campus? = null, val limits: Limits = Limits())

@Serializable data class Car(val make: String = "", val color: String = "", val plate: String = "")

@Serializable
data class Me(
    @SerialName("_id") val id: String = "",
    val sub: String = "",
    val verified: Boolean = false,
    val eduEmail: String? = null,
    val role: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val avatar: String? = null,
    val personalEmail: String? = null,
    val car: Car? = null,
    val licenseConfirmed: Boolean? = null,
    val banned: Boolean? = null,
) { val isDriver get() = role == "driver" }

@Serializable
data class Home(
    @SerialName("_id") val id: String,
    val label: String,
    val location: Point,
    val walkingRadius: Int,
)

@Serializable
data class Passenger(
    @SerialName("_id") val id: String = "",
    val rider: String,
    val negotiationId: String = "",
    val pickup: Point,
    val time: String = "",
    val status: String = "",
)

@Serializable data class Route(val coordinates: List<List<Double>> = emptyList())

@Serializable
data class Commute(
    @SerialName("_id") val id: String,
    val homeId: String = "",
    val direction: String = "to-school",
    val dates: List<String> = emptyList(),
    val days: List<Int> = emptyList(),
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "",
    val createdAt: String = "",
    // drive-only fields
    val route: Route? = null,
    val seats: Int = 0,
    val availableSeats: Int = 0,
    val owner: String = "",
    val passengers: List<Passenger> = emptyList(),
)

@Serializable data class Match(val requestId: String, val rider: String, val distanceMeters: Double = 0.0, val startTime: String = "", val endTime: String = "")
@Serializable data class Offer(val negotiationId: String, val driveId: String = "", val requestId: String = "")

@Serializable
data class Negotiation(
    @SerialName("_id") val id: String,
    val driver: String,
    val rider: String,
    val driveId: String = "",
    val requestId: String = "",
    val status: String = "open",
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable data class Message(@SerialName("_id") val id: String, val negotiationId: String = "", val author: String = "", val text: String = "", val createdAt: String = "")
@Serializable data class Proposal(@SerialName("_id") val id: String, val negotiationId: String = "", val author: String = "", val pickup: Point, val time: String = "", val status: String = "pending", val createdAt: String = "")


// request bodies
@Serializable data class CommuteBody(val homeId: String, val direction: String, val dates: List<String>, val days: List<Int>, val startTime: String, val endTime: String)
@Serializable data class DriveBody(val commute: CommuteBody, val route: List<List<Double>>, val seats: Int)
@Serializable data class HomeBody(val label: String, val location: Point, val walkingRadius: Int)
