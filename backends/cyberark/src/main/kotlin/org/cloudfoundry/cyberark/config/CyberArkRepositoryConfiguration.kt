package org.cloudfoundry.cyberark.config

import org.cloudfoundry.cyberark.CyberArkCredentialRepository
import org.cloudfoundry.cyberark.DefaultCyberArkCredentialRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestOperations

@Configuration
class CyberArkRepositoryConfiguration {

    @Value("\${cyberark.base-url}")
    private lateinit var baseUrl: String

    @Value("\${cyberark.base-policy}")
    private lateinit var basePolicy: String

    @Value("\${cyberark.api-key}")
    private lateinit var apiKey: String

    @Value("\${cyberark.account-name}")
    private lateinit var accountName: String

    @Value("\${cyberark.user-name}")
    private lateinit var userName: String

    @Bean
    fun getCyberArkCredentialRepository(restOperations: RestOperations): CyberArkCredentialRepository {
        return DefaultCyberArkCredentialRepository(
            restOperations = restOperations,
            baseUrl = baseUrl,
            basePolicy = basePolicy,
            apiKey = apiKey,
            accountName = accountName,
            userName = userName
        )
    }
}
