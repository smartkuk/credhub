package org.cloudfoundry.conjur.config

import org.cloudfoundry.conjur.ConjurCredentialRepository
import org.cloudfoundry.conjur.ConjurCredentialService
import org.cloudfoundry.conjur.ConstantUuidProvider
import org.cloudfoundry.conjur.DefaultConjurCredentialService
import org.cloudfoundry.credhub.util.CurrentTimeProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("conjur")
@Configuration
class ConjurServiceConfig {
    @Bean
    fun getConjurCredentialService(conjurCredentialRepository: ConjurCredentialRepository): ConjurCredentialService {
        return DefaultConjurCredentialService(
            conjurCredentialRepository = conjurCredentialRepository,
            timeProvider = CurrentTimeProvider(),
            uuidProvider = ConstantUuidProvider()
        )
    }
}
