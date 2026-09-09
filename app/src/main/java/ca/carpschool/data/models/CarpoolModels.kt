package ca.carpschool.data.models

data class School(
    val schoolCode: String,
    val officialName: String,
    val allowedEmailDomains: List<String>,
    val baseUrl: String,
    val isTrusted: Boolean
)

data class FederationTicket(
    val ticket: String,
    val schoolBaseUrl: String,
    val isTrusted: Boolean,
    val expiresAt: String
)

data class UserHome(
    val _id: String?,
    val label: String,
    val address: String,
    val walkingRadiusMeters: Int, // 10m to 200m
    val location: GeoPoint
)

data class GeoPoint(
    val type: String = "Point",
    val coordinates: List<Double> // [lng, lat]
)

enum class CommuteDirection {
    HOME_TO_SCHOOL,
    SCHOOL_TO_HOME
}

data class Proposal(
    val proposalId: String,
    val pickupPointName: String,
    val pickupCoordinates: List<Double>,
    val proposedTime: String,
    val status: String // PENDING, CONFIRMED, DENIED
)
