package org.cloudfoundry.credhub.controllers

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.views.CredentialView
import org.cloudfoundry.credhub.views.DataResponse
import org.cloudfoundry.credhub.views.FindCredentialResults
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import java.io.IOException
import java.io.InputStream

interface CredentialsController {

    @Throws(IOException::class)
    fun generate(inputStream: InputStream): CredentialView

    fun set(@RequestBody requestBody: BaseCredentialSetRequest<*>): CredentialView

    fun delete(@RequestParam("name") credentialName: String)

    fun getCredentialById(@PathVariable id: String): CredentialView

    fun getCredential(
        @RequestParam("name") credentialName: String,
        @RequestParam(value = "versions", required = false) numberOfVersions: Int?,
        @RequestParam(value = "current", required = false, defaultValue = "false") current: Boolean
    ): DataResponse

    fun findByPath(
        @RequestParam("path") path: String,
        @RequestParam(value = "expires-within-days", required = false, defaultValue = "") expiresWithinDays: String
    ): FindCredentialResults

    fun findByNameLike(
        @RequestParam("name-like") nameLike: String,
        @RequestParam(value = "expires-within-days", required = false, defaultValue = "") expiresWithinDays: String
    ): FindCredentialResults
}
