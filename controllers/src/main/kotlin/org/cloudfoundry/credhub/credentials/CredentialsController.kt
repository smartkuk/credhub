package org.cloudfoundry.credhub.credentials

import com.google.common.io.ByteStreams
import org.apache.commons.lang3.StringUtils
import org.cloudfoundry.credhub.ErrorMessages
import org.cloudfoundry.credhub.audit.CEFAuditRecord
import org.cloudfoundry.credhub.audit.entities.DeleteCredential
import org.cloudfoundry.credhub.audit.entities.FindCredential
import org.cloudfoundry.credhub.audit.entities.GetCredential
import org.cloudfoundry.credhub.audit.entities.SetCredential
import org.cloudfoundry.credhub.exceptions.InvalidQueryParameterException
import org.cloudfoundry.credhub.generate.CredentialsHandler
import org.cloudfoundry.credhub.generate.LegacyGenerationHandler
import org.cloudfoundry.credhub.generate.SetHandler
import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.services.PermissionedCredentialService
import org.cloudfoundry.credhub.views.CredentialView
import org.cloudfoundry.credhub.views.DataResponse
import org.cloudfoundry.credhub.views.FindCredentialResults
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

@RestController
@RequestMapping(path = [CredentialsController.ENDPOINT], produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
class CredentialsController(
    private val permissionedCredentialService: PermissionedCredentialService,
    private val credentialsHandler: CredentialsHandler,
    private val setHandler: SetHandler,
    private val legacyGenerationHandler: LegacyGenerationHandler,
    private val auditRecord: CEFAuditRecord
) {

    companion object {
        const val ENDPOINT = "/api/v1/data"
    }

    @RequestMapping(path = [""], method = [RequestMethod.POST])
    @ResponseStatus(HttpStatus.OK)
    @Synchronized
    @Throws(IOException::class)
    fun generate(inputStream: InputStream): CredentialView {
        val requestInputStream = ByteArrayInputStream(ByteStreams.toByteArray(inputStream))
        return legacyGenerationHandler.auditedHandlePostRequest(requestInputStream)
    }

    @RequestMapping(path = [""], method = [RequestMethod.PUT])
    @ResponseStatus(HttpStatus.OK)
    @Synchronized
    fun set(@RequestBody requestBody: BaseCredentialSetRequest<*>): CredentialView {
        requestBody.validate()
        return auditedHandlePutRequest(requestBody)
    }

    @RequestMapping(path = [""], method = [RequestMethod.DELETE])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@RequestParam("name") credentialName: String) {
        if (StringUtils.isEmpty(credentialName)) {
            throw InvalidQueryParameterException(ErrorMessages.MISSING_QUERY_PARAMETER, "name")
        }

        val credentialNameWithPrependedSlash = StringUtils.prependIfMissing(credentialName, "/")

        val requestDetails = DeleteCredential(credentialNameWithPrependedSlash)
        auditRecord.requestDetails = requestDetails

        credentialsHandler.deleteCredential(credentialNameWithPrependedSlash)
    }

    @RequestMapping(path = ["/{id}"], method = [RequestMethod.GET])
    @ResponseStatus(HttpStatus.OK)
    fun getById(@PathVariable id: String): CredentialView {
        return credentialsHandler.getCredentialVersionByUUID(id)
    }

    @GetMapping(path = [""])
    @ResponseStatus(HttpStatus.OK)
    fun getByName(
        @RequestParam("name") credentialName: String,
        @RequestParam(value = "versions", required = false) numberOfVersions: Int?,
        @RequestParam(value = "current", required = false, defaultValue = "false") current: Boolean
    ): DataResponse {
        if (StringUtils.isEmpty(credentialName)) {
            throw InvalidQueryParameterException(ErrorMessages.MISSING_QUERY_PARAMETER, "name")
        }

        if (current && numberOfVersions != null) {
            throw InvalidQueryParameterException(ErrorMessages.CANT_USE_VERSIONS_AND_CURRENT, "name")
        }

        val credentialNameWithPrependedSlash = StringUtils.prependIfMissing(credentialName, "/")

        auditRecord.requestDetails = GetCredential(credentialName, numberOfVersions, current)

        return if (current) {
            credentialsHandler.getCurrentCredentialVersions(credentialNameWithPrependedSlash)
        } else {
            credentialsHandler.getNCredentialVersions(credentialNameWithPrependedSlash, numberOfVersions)
        }
    }

    @RequestMapping(path = [""], params = ["path"], method = [RequestMethod.GET])
    @ResponseStatus(HttpStatus.OK)
    fun findByPath(
        @RequestParam("path") path: String,
        @RequestParam(value = "expires-within-days", required = false, defaultValue = "") expiresWithinDays: String
    ): FindCredentialResults {
        val findCredential = FindCredential()
        findCredential.path = path
        findCredential.expiresWithinDays = expiresWithinDays
        auditRecord.requestDetails = findCredential

        return FindCredentialResults(permissionedCredentialService.findStartingWithPath(path, expiresWithinDays))
    }

    @RequestMapping(path = [""], params = ["name-like"], method = [RequestMethod.GET])
    @ResponseStatus(HttpStatus.OK)
    fun findByNameLike(
        @RequestParam("name-like") nameLike: String,
        @RequestParam(value = "expires-within-days", required = false, defaultValue = "") expiresWithinDays: String
    ): FindCredentialResults {
        val findCredential = FindCredential()
        findCredential.nameLike = nameLike
        findCredential.expiresWithinDays = expiresWithinDays
        auditRecord.requestDetails = findCredential

        return FindCredentialResults(permissionedCredentialService.findContainingName(nameLike, expiresWithinDays))
    }

    private fun auditedHandlePutRequest(@RequestBody requestBody: BaseCredentialSetRequest<*>): CredentialView {
        auditRecord.requestDetails = SetCredential(requestBody.name, requestBody.type)
        return setHandler.handle(requestBody)
    }
}
