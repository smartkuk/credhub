package org.cloudfoundry.conjur.config

import org.cloudfoundry.conjur.ConjurCredentialRepository
import org.cloudfoundry.conjur.DefaultConjurCredentialRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestOperations

@Profile("conjur")
@Configuration
class ConjurRepositoryConfiguration {

    @Value("\${conjur.base-url}")
    private lateinit var baseUrl: String

    @Value("\${conjur.base-policy}")
    private lateinit var basePolicy: String

    @Value("\${conjur.api-key}")
    private lateinit var apiKey: String

    @Value("\${conjur.account-name}")
    private lateinit var accountName: String

    @Value("\${conjur.user-name}")
    private lateinit var userName: String

    @Bean
    fun getConjurCredentialRepository(restOperations: RestOperations): ConjurCredentialRepository {
        return DefaultConjurCredentialRepository(
            restOperations = restOperations,
            baseUrl = baseUrl,
            basePolicy = basePolicy,
            apiKey = apiKey,
            accountName = accountName,
            userName = userName
        )
    }
}
