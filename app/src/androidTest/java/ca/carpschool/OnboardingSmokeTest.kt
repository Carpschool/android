package ca.carpschool

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real app entry and Clerk navigation. Does not replace authenticated flow QA. */
@RunWith(AndroidJUnit4::class)
class OnboardingSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun signedOutLaunchShowsWorkingSignInNavigation() {
        compose.waitUntil(timeoutMillis = 60000) {
            compose.onAllNodesWithText("Sign in").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("CarpSchool").assertIsDisplayed()
        compose.onNodeWithText("Share rides with classmates.").assertIsDisplayed()
        compose.onNodeWithText("Sign in").assertHasClickAction().performClick()
        compose.onNodeWithText("Back").assertIsDisplayed().performClick()
        compose.onNodeWithText("CarpSchool").assertIsDisplayed()
    }
}
