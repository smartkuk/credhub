package org.cloudfoundry.conjur

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.views.CredentialView

interface ConjurCredentialRepository {
    fun setCredential(baseCredentialSetRequest: BaseCredentialSetRequest<*>)

    fun getCredential(credentialName: String) : CredentialView
}

