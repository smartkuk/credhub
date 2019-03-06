package org.cloudfoundry.cyberark

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestOperations
import java.util.Base64

class DefaultCyberArkCredentialRepository(
    val restOperations: RestOperations,
    val baseUrl: String,
    val basePolicy: String,
    val apiKey: String,
    val accountName: String,
    val userName: String
) : CyberArkCredentialRepository {

    override fun setCredential(baseCredentialSetRequest: BaseCredentialSetRequest<*>) {
        val token = fetchCyberArkToken()
        val requestHeaders = HttpHeaders()
        requestHeaders.add(
            "Authorization",
            "Token token=\"$token\""
        )

        val variableNameWithoutPrefix = baseCredentialSetRequest.name.removePrefix("/")
        val policyYml = "- !variable $variableNameWithoutPrefix"

        createVariableInCyberArk(policyYml, requestHeaders)
        setVariableInCyberArk(variableNameWithoutPrefix, baseCredentialSetRequest, requestHeaders)
    }

    private fun fetchCyberArkToken(): String {
        val cyberArkTokenResponse = restOperations.exchange(
            "$baseUrl/authn/$accountName/$userName/authenticate",
            HttpMethod.POST,
            HttpEntity(apiKey),
            String::class.java
        )

        return Base64.getEncoder().encodeToString(cyberArkTokenResponse.body!!.toByteArray())
    }

    private fun setVariableInCyberArk(variableName: String, baseCredentialSetRequest: BaseCredentialSetRequest<*>, requestHeaders: HttpHeaders) {
        restOperations.exchange(
            "$baseUrl/secrets/$accountName/variable/$basePolicy/$variableName",
            HttpMethod.POST,
            HttpEntity(baseCredentialSetRequest.credentialValue, requestHeaders),
            String::class.java
        )
    }

    private fun createVariableInCyberArk(policyYml: String, requestHeaders: HttpHeaders) {
        restOperations.put(
            "$baseUrl/policies/$accountName/policy/$basePolicy",
            HttpEntity(policyYml, requestHeaders)
        )
    }
}
