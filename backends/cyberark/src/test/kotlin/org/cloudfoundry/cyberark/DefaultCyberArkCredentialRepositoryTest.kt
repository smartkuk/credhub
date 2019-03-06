package org.cloudfoundry.cyberark

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.cloudfoundry.credhub.credential.StringCredentialValue
import org.cloudfoundry.credhub.requests.ValueSetRequest
import org.junit.Rule
import org.junit.Test
import org.springframework.web.client.RestTemplate
import java.util.Base64

class DefaultCyberArkCredentialRepositoryTest {
    @get: Rule
    val wireMockRule = WireMockRule(wireMockConfig().dynamicPort())

    @Test
    fun `setCredential should set a credential`() {
        val wireMockServer = WireMockServer()
        wireMockServer.start()

        val apiKey = "some-api-key"
        val accountName = "some-account-name"
        val userName = "some-user-name"
        val policyName = "some-policy-name"
        val token = "some-token"
        val variableName = "some-variable-name"
        val base64Token = Base64.getEncoder().encodeToString(token.toByteArray())
        val value = "some-value"

        val getTokenUrl = "/authn/$accountName/$userName/authenticate"
        setupWireMockForFetchTokenStubServer(
            getTokenUrl = getTokenUrl,
            apiKey = apiKey,
            token = token
        )

        val createPolicyVariableUrl = "/policies/$accountName/policy/$policyName"
        setupWireMockForCreateVariableStubServer(
            createPolicyVariableUrl = createPolicyVariableUrl,
            variableName =  variableName,
            base64Token =  base64Token
        )

        val setVariableUrl = "/secrets/$accountName/variable/$policyName/$variableName"
        setupWireMockForSetVariableStubServer(
            setVariableUrl = setVariableUrl,
            value = value,
            base64Token = base64Token
        )

        val defaultCyberArkCredentialRepository = DefaultCyberArkCredentialRepository(
            restOperations = RestTemplate(),
            baseUrl = wireMockRule.baseUrl(),
            basePolicy = policyName,
            apiKey = apiKey,
            accountName = accountName,
            userName = userName
        )
        defaultCyberArkCredentialRepository.setCredential(
            {
                val valueSetRequest = ValueSetRequest()
                valueSetRequest.setType("value")
                valueSetRequest.value = StringCredentialValue(value)
                valueSetRequest.name = variableName

                valueSetRequest
            }()
        )

        wireMockServer.stop()

        verify(postRequestedFor(urlEqualTo(getTokenUrl)))
        verify(putRequestedFor(urlEqualTo(createPolicyVariableUrl)))
        verify(postRequestedFor(urlEqualTo(setVariableUrl)))
    }

    private fun setupWireMockForSetVariableStubServer(setVariableUrl: String, value: String, base64Token: String) {
        stubFor(
            post(urlPathMatching(setVariableUrl))
                .withRequestBody(containing(value))
                .withHeader("Authorization", equalTo("Token token=\"$base64Token\""))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                )
        )
    }

    private fun setupWireMockForCreateVariableStubServer(createPolicyVariableUrl: String, variableName: String, base64Token: String) {
        stubFor(
            put(urlPathMatching(createPolicyVariableUrl))
                .withRequestBody(equalTo("- !variable $variableName"))
                .withHeader("Authorization", equalTo("Token token=\"$base64Token\""))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                )
        )
    }

    private fun setupWireMockForFetchTokenStubServer(getTokenUrl: String, apiKey: String, token: String) {
        stubFor(
            post(urlPathMatching(getTokenUrl))
                .withRequestBody(equalTo(apiKey))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(token)
                )
        )
    }
}
