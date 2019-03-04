package org.cloudfoundry.cyberark

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.views.CredentialView

interface CyberArkCredentialService {
    fun setCredential(baseCredentialSetRequest: BaseCredentialSetRequest<*>) : CredentialView
}
