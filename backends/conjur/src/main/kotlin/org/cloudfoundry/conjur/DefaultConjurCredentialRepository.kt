package org.cloudfoundry.conjur

import org.bouncycastle.asn1.x500.style.RFC4519Style.name
import org.cloudfoundry.credhub.credential.StringCredentialValue
import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.views.CredentialView
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import java.time.Instant
import java.util.Base64
import java.util.UUID

class DefaultConjurCredentialRepository(
    val restOperations: RestOperations,
    val baseUrl: String,
    val basePolicy: String,
    val apiKey: String,
    val accountName: String,
    val userName: String
) : ConjurCredentialRepository {
    override fun getCredential(credentialName: String): CredentialView {
        val token = fetchConjurToken()
        val requestHeaders = HttpHeaders()
        requestHeaders.add(
            "Authorization",
            "Token token=\"$token\""
        )

        val variableNameWithoutPrefix = credentialName.removePrefix("/")

        val credentialValue = getVariableInConjur(variableNameWithoutPrefix, requestHeaders)
        return CredentialView(
            Instant.now(),
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            credentialName,
            "value",
            StringCredentialValue(credentialValue)
        )
    }

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

    private fun getVariableInConjur(variableName: String, requestHeaders: HttpHeaders) : String {
        val response = restOperations.exchange(
            "$baseUrl/secrets/$accountName/variable/$basePolicy/$variableName",
            HttpMethod.GET,
            HttpEntity(null, requestHeaders),
            String::class.java
        )

        return response.body!!.removeSurrounding("\"")
    }
}
