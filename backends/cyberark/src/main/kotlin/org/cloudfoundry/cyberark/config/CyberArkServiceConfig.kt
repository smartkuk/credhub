package org.cloudfoundry.cyberark.config

import org.cloudfoundry.credhub.util.CurrentTimeProvider
import org.cloudfoundry.cyberark.ConstantUuidProvider
import org.cloudfoundry.cyberark.CyberArkCredentialRepository
import org.cloudfoundry.cyberark.CyberArkCredentialService
import org.cloudfoundry.cyberark.DefaultCyberArkCredentialService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CyberArkServiceConfig {
    @Bean
    fun getCyberArkCredentialService(cyberArkCredentialRepository: CyberArkCredentialRepository): CyberArkCredentialService {
        return DefaultCyberArkCredentialService(
            cyberArkCredentialRepository = cyberArkCredentialRepository,
            timeProvider = CurrentTimeProvider(),
            uuidProvider = ConstantUuidProvider()
        )
    }
}
