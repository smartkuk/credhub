package org.cloudfoundry.credhub.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

import org.cloudfoundry.credhub.ManagementInterceptor
import org.cloudfoundry.credhub.interceptors.AuditInterceptor
import org.cloudfoundry.credhub.interceptors.UserContextInterceptor

@Configuration
class WebMvcConfiguration(
    private val auditInterceptor: AuditInterceptor,
    private val userContextInterceptor: UserContextInterceptor,
    private val managementInterceptor: ManagementInterceptor
) : WebMvcConfigurer {

    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer.favorPathExtension(false)
    }

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.setUseSuffixPatternMatch(false)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(auditInterceptor).excludePathPatterns(
            "/info",
            "/health",
            "/**/key-usage",
            "/version",
            "/docs/index.html"
        )
        registry.addInterceptor(managementInterceptor)
        registry.addInterceptor(userContextInterceptor).excludePathPatterns(
            "/info",
            "/health",
            "/**/key-usage",
            "/management",
            "/docs/index.html"
        )
    }
}
