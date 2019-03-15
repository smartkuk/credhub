package org.cloudfoundry.conjur

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestOperations
import java.util.Base64

class DefaultConjurCredentialRepository(
    val restOperations: RestOperations,
    val baseUrl: String,
    val basePolicy: String,
    val apiKey: String,
    val accountName: String,
    val userName: String
) : ConjurCredentialRepository {

    override fun setCredential(baseCredentialSetRequest: BaseCredentialSetRequest<*>) {
        val token = fetchConjurToken()
        val requestHeaders = HttpHeaders()
        requestHeaders.add(
            "Authorization",
            "Token token=\"$token\""
        )

        val variableNameWithoutPrefix = baseCredentialSetRequest.name.removePrefix("/")
        val policyYml = "- !variable $variableNameWithoutPrefix"

        createVariableInConjur(policyYml, requestHeaders)
        setVariableInConjur(variableNameWithoutPrefix, baseCredentialSetRequest, requestHeaders)
    }

    private fun fetchConjurToken(): String {
        val conjurTokenResponse = restOperations.exchange(
            "$baseUrl/authn/$accountName/$userName/authenticate",
            HttpMethod.POST,
            HttpEntity(apiKey),
            String::class.java
        )

        return Base64.getEncoder().encodeToString(conjurTokenResponse.body!!.toByteArray())
    }

    private fun createVariableInConjur(policyYml: String, requestHeaders: HttpHeaders) {
        restOperations.put(
            "$baseUrl/policies/$accountName/policy/$basePolicy",
            HttpEntity(policyYml, requestHeaders)
        )
    }

    private fun setVariableInConjur(variableName: String, baseCredentialSetRequest: BaseCredentialSetRequest<*>, requestHeaders: HttpHeaders) {
        restOperations.exchange(
            "$baseUrl/secrets/$accountName/variable/$basePolicy/$variableName",
            HttpMethod.POST,
            HttpEntity(baseCredentialSetRequest.credentialValue, requestHeaders),
            String::class.java
        )
    }
}
