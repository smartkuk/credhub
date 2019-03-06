package org.cloudfoundry.cyberark

import org.assertj.core.api.Assertions.assertThat
import org.cloudfoundry.credhub.credential.StringCredentialValue
import org.cloudfoundry.credhub.requests.ValueSetRequest
import org.cloudfoundry.credhub.utils.TestConstants
import org.cloudfoundry.credhub.views.CredentialView
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.util.UUID

class CyberArkCredentialsControllerTest {

    lateinit var spyCyberArkCredentialService: SpyCyberArkCredentialService
    lateinit var cyberArkCredentialsController: CyberArkCredentialsController
    lateinit var mockMvc: MockMvc

    @Before
    fun setUp() {
        spyCyberArkCredentialService = SpyCyberArkCredentialService()
        cyberArkCredentialsController = CyberArkCredentialsController(spyCyberArkCredentialService)

        mockMvc = MockMvcBuilders
            .standaloneSetup(cyberArkCredentialsController)
            .build()
    }

    @Test
    fun `should set a cyberark credential`() {
        spyCyberArkCredentialService.setCredentialReturn = CredentialView(
            Instant.ofEpochMilli(2L),
            UUID.fromString(TestConstants.TEST_UUID_STRING),
            "some-name",
            "value",
            StringCredentialValue("some-value")
        )
        val responseBody = mockMvc.perform(
            put(CyberArkCredentialsController.ENDPOINT)
                //language=json
                .content(
                    """
                        {
                            "name": "some-name",
                            "type": "value",
                            "value": "some-value"
                        }
                    """.trimIndent()
                )
                .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().is2xxSuccessful)
        .andReturn().response.contentAsString

        val expectedBaseCredentialSetRequest = ValueSetRequest()
        expectedBaseCredentialSetRequest.value = StringCredentialValue("some-value")
        expectedBaseCredentialSetRequest.type = "value"
        expectedBaseCredentialSetRequest.name = "some-name"

        assertThat(spyCyberArkCredentialService.setCredential_calledWith).isEqualToComparingFieldByField(expectedBaseCredentialSetRequest)

        JSONAssert.assertEquals(
            responseBody,
            //language=json
            """
                {
                    "version_created_at": 0.002,
                    "id": "${UUID.fromString(TestConstants.TEST_UUID_STRING)}",
                    "name": "some-name",
                    "type": "value",
                    "value": "some-value"
                }
            """.trimIndent(),
            true
        )
    }

    @Test
    fun `should set another cyberark credential`() {
        spyCyberArkCredentialService.setCredentialReturn = CredentialView(
            Instant.ofEpochMilli(2L),
            UUID.fromString(TestConstants.TEST_UUID_STRING),
            "some-other-name",
            "value",
            StringCredentialValue("some-other-value")
        )
        val responseBody = mockMvc.perform(
            put(CyberArkCredentialsController.ENDPOINT)
                //language=json
                .content(
                    """
                        {
                            "name": "some-other-name",
                            "type": "value",
                            "value": "some-other-value"
                        }
                    """.trimIndent()
                )
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().is2xxSuccessful)
            .andReturn().response.contentAsString

        val expectedBaseCredentialSetRequest = ValueSetRequest()
        expectedBaseCredentialSetRequest.value = StringCredentialValue("some-other-value")
        expectedBaseCredentialSetRequest.type = "value"
        expectedBaseCredentialSetRequest.name = "some-other-name"

        assertThat(spyCyberArkCredentialService.setCredential_calledWith).isEqualToComparingFieldByField(expectedBaseCredentialSetRequest)

        JSONAssert.assertEquals(
            responseBody,
            //language=json
            """
                {
                    "version_created_at": 0.002,
                    "id": "${UUID.fromString(TestConstants.TEST_UUID_STRING)}",
                    "name": "some-other-name",
                    "type": "value",
                    "value": "some-other-value"
                }
            """.trimIndent(),
            true
        )
    }
}
