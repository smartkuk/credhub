package org.cloudfoundry.conjur

import org.assertj.core.api.Assertions.assertThat
import org.cloudfoundry.credhub.credential.StringCredentialValue
import org.cloudfoundry.credhub.requests.ValueSetRequest
import org.cloudfoundry.credhub.views.CredentialView
import org.junit.Before
import org.junit.Test
import java.time.Instant

class DefaultConjurCredentialServiceTest {

    lateinit var spyConjurCredentialRepository: SpyConjurCredentialRepository
    lateinit var stubTimeProvider: StubTimeProvider
    lateinit var defaultConjurCredentialService: DefaultConjurCredentialService

    @Before
    fun setUp() {
        spyConjurCredentialRepository = SpyConjurCredentialRepository()
        stubTimeProvider = StubTimeProvider()
        defaultConjurCredentialService = DefaultConjurCredentialService(
            conjurCredentialRepository = spyConjurCredentialRepository,
            timeProvider = stubTimeProvider,
            uuidProvider = ConstantUuidProvider()
        )
    }

    @Test
    fun `should set credential in repository`() {
        val credentialSetRequest = ValueSetRequest()
        credentialSetRequest.value = StringCredentialValue("some-value")
        credentialSetRequest.type = "value"
        credentialSetRequest.name = "some-name"

        stubTimeProvider.getInstantReturn = Instant.ofEpochMilli(2L)

        val expectedCredentialView = CredentialView(
            Instant.ofEpochMilli(2L),
            ConstantUuidProvider().generateUuid(),
            "/some-name",
            "value",
            StringCredentialValue("some-value")
        )

        val actualCredentialView = defaultConjurCredentialService.setCredential(credentialSetRequest)

        assertThat(actualCredentialView).isEqualTo(expectedCredentialView)
        assertThat(spyConjurCredentialRepository.setCredential_calledWith).isEqualTo(credentialSetRequest)
    }

    @Test
    fun `should set another credential in repository`() {
        val credentialSetRequest = ValueSetRequest()
        credentialSetRequest.value = StringCredentialValue("some-other-value")
        credentialSetRequest.type = "value"
        credentialSetRequest.name = "some-other-name"

        stubTimeProvider.getInstantReturn = Instant.ofEpochMilli(2L)

        val expectedCredentialView = CredentialView(
            Instant.ofEpochMilli(2L),
            ConstantUuidProvider().generateUuid(),
            "/some-other-name",
            "value",
            StringCredentialValue("some-other-value")
        )

        val actualCredentialView = defaultConjurCredentialService.setCredential(credentialSetRequest)

        assertThat(actualCredentialView).isEqualTo(expectedCredentialView)
        assertThat(spyConjurCredentialRepository.setCredential_calledWith).isEqualTo(credentialSetRequest)
    }
}
