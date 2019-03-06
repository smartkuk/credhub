package org.cloudfoundry.cyberark.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate

@Configuration
class RestOperationsConfig {

    @Bean
    public fun getRestOperations(): RestOperations {
        return RestTemplate()
    }
}
