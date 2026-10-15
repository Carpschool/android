package ca.carpschool.data

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ContractTest {
    @Test fun schoolRegistryPreservesExternalOrigin() {
        val school = Net.json.decodeFromJsonElement<School>(Net.json.parseToJsonElement("""{"_id":"synthetic","schoolCode":"sandbox","name":"QA School","baseUrl":"https://school.example.invalid"}"""))
        assertEquals("sandbox", school.schoolCode)
        assertEquals("https://school.example.invalid", school.baseUrl)
    }
    @Test fun driverAndRiderGatesUseExplicitRole() {
        assertTrue(Me(role = "driver").isDriver)
        assertFalse(Me(role = "rider").isDriver)
        assertFalse(Me().isDriver)
    }
    @Test fun pickupCoordinatesAreLongitudeThenLatitude() {
        val p = Point.of(49.25, -123.01)
        assertEquals(listOf(-123.01, 49.25), p.coordinates)
        assertEquals(49.25, p.lat, 0.0)
    }
    @Test fun recurringWeekdaysAndSpecificDatesRender() {
        assertEquals("Weekdays", schedule(Commute("qa", days = listOf(5, 1, 4, 2, 3))))
        assertEquals("Every Sun, Sat", schedule(Commute("qa", days = listOf(6, 0))))
        assertTrue(schedule(Commute("qa", dates = listOf("2026-10-14"))).isNotBlank())
    }
    @Test fun driveResponseKeepsSeatAndPassengerState() {
        val c = Net.json.decodeFromJsonElement<Commute>(Net.json.parseToJsonElement("""{"_id":"android-drive","seats":3,"availableSeats":2,"passengers":[{"rider":"android-rider","pickup":{"coordinates":[-123.01,49.25]}}]}"""))
        assertEquals(2, c.availableSeats)
        assertEquals("android-rider", c.passengers.single().rider)
    }
    @Test fun acceptedProposalPreservesAuthorAndStatus() {
        val p = Net.json.decodeFromJsonElement<Proposal>(Net.json.parseToJsonElement("""{"_id":"android-proposal","author":"android-driver","status":"accepted","pickup":{"coordinates":[-123.01,49.25]},"time":"07:45"}"""))
        assertEquals("accepted", p.status)
        assertEquals("android-driver", p.author)
    }
}
