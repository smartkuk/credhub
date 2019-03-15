package org.cloudfoundry.conjur

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.views.CredentialView

interface ConjurCredentialService {
    fun setCredential(baseCredentialSetRequest: BaseCredentialSetRequest<*>) : CredentialView
}
