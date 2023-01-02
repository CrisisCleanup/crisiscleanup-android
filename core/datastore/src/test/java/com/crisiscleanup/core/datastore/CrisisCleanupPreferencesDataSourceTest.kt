package com.crisiscleanup.core.datastore

import com.crisiscleanup.core.datastore.test.testUserPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrisisCleanupPreferencesDataSourceTest {
    private lateinit var subject: CrisisCleanupPreferencesDataSource

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Before
    fun setup() {
        subject = CrisisCleanupPreferencesDataSource(
            tmpFolder.testUserPreferencesDataStore()
        )
    }

    @Test
    fun shouldHideOnboardingIsFalseByDefault() = runTest {
        assertFalse(subject.userData.first().shouldHideOnboarding)
    }

    @Test
    fun userShouldHideOnboardingIsTrueWhenSet() = runTest {
        subject.setShouldHideOnboarding(true)
        assertTrue(subject.userData.first().shouldHideOnboarding)
    }

//    @Test
//    fun userShouldHideOnboarding_unfollowsLastTopic_shouldHideOnboardingIsFalse() = runTest {
//
//        // Given: user completes onboarding by selecting a single topic.
//        subject.toggleFollowedTopicId("1", true)
//        subject.setShouldHideOnboarding(true)
//
//        // When: they unfollow that topic.
//        subject.toggleFollowedTopicId("1", false)
//
//        // Then: onboarding should be shown again
//        assertFalse(subject.userData.first().shouldHideOnboarding)
//    }

//    @Test
//    fun userShouldHideOnboarding_unfollowsAllTopics_shouldHideOnboardingIsFalse() = runTest {
//
//        // Given: user completes onboarding by selecting several topics.
//        subject.setFollowedTopicIds(setOf("1", "2"))
//        subject.setShouldHideOnboarding(true)
//
//        // When: they unfollow those topics.
//        subject.setFollowedTopicIds(emptySet())
//
//        // Then: onboarding should be shown again
//        assertFalse(subject.userData.first().shouldHideOnboarding)
//    }
}
