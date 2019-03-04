package org.cloudfoundry.cyberark

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.views.CredentialView

class SpyCyberArkCredentialService : CyberArkCredentialService {

    lateinit var setCredentialReturn: CredentialView
    lateinit var setCredential_calledWith: BaseCredentialSetRequest<*>

    override fun setCredential(baseCredentialSetRequest: BaseCredentialSetRequest<*>): CredentialView {
        setCredential_calledWith = baseCredentialSetRequest

        return setCredentialReturn
    }
}
