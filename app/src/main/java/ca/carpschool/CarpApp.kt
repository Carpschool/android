package ca.carpschool

import android.app.Application
import ca.carpschool.data.Carp
import com.clerk.api.Clerk
import com.google.android.libraries.places.api.Places

class CarpApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Clerk.initialize(this, BuildConfig.CLERK_PUBLISHABLE_KEY)
        if (BuildConfig.MAPS_API_KEY.isNotBlank() && !Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.MAPS_API_KEY)
        }
        Carp.init(this)
    }
}
