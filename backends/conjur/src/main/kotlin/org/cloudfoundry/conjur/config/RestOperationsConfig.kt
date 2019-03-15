package org.cloudfoundry.conjur.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate

@Profile("conjur")
@Configuration
class RestOperationsConfig {

    @Bean
    fun getRestOperations(): RestOperations {
        return RestTemplate()
    }
}
