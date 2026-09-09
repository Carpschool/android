package ca.carpschool.data.network

import ca.carpschool.data.models.*

/**
 * NetworkService
 * 
 * Manages HTTP communication with Central Authority Server and local School Server.
 */
class NetworkService {
    // TODO: Configure Retrofit instance for Central Server
    // TODO: Configure Retrofit instance for School Server with Bearer Federation Ticket

    suspend fun getSchools(): List<School> {
        // TODO: Call GET /api/v1/schools
        return emptyList()
    }

    suspend fun getFederationTicket(schoolCode: String, customBaseUrl: String?): FederationTicket {
        // TODO: Call POST /api/v1/schools/ticket
        return FederationTicket("", "", true, "")
    }

    suspend fun getHomes(): List<UserHome> {
        // TODO: Call GET /api/v1/homes
        return emptyList()
    }

    suspend fun saveHome(label: String, address: String, lat: Double, lng: Double, radius: Int): UserHome {
        // TODO: Call POST /api/v1/homes
        return UserHome(null, label, address, radius, GeoPoint("Point", listOf(lng, lat)))
    }

    suspend fun submitBoardingPin(carpoolId: String, riderId: String, pin: String, lat: Double, lng: Double): Boolean {
        // TODO: Call POST /api/v1/carpools/:id/board-passenger with single GPS read
        return true
    }

    suspend fun endRide(carpoolId: String, lat: Double, lng: Double): Boolean {
        // TODO: Call POST /api/v1/carpools/:id/end-ride with single GPS read
        return true
    }
}
