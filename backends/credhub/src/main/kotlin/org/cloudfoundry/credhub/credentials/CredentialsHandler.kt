package org.cloudfoundry.credhub.credentials

import org.cloudfoundry.credhub.requests.BaseCredentialGenerateRequest
import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.views.CredentialView
import org.cloudfoundry.credhub.views.DataResponse
import java.io.InputStream

interface CredentialsHandler {
    fun deleteCredential(credentialName: String)
    fun getNCredentialVersions(credentialName: String, numberOfVersions: Int?): DataResponse
    fun getAllCredentialVersions(credentialName: String): DataResponse
    fun getCurrentCredentialVersions(credentialName: String): DataResponse
    fun getCredentialVersionByUUID(credentialUUID: String): CredentialView
    fun setCredential(setRequest: BaseCredentialSetRequest<*>): CredentialView
    fun generateCredential(generateRequest: BaseCredentialGenerateRequest): CredentialView

}
