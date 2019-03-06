package org.cloudfoundry.cyberark

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest

class SpyCyberArkCredentialRepository : CyberArkCredentialRepository {
    lateinit var setCredential_calledWith: BaseCredentialSetRequest<*>

    override fun setCredential(baseCredentialSetRequest: BaseCredentialSetRequest<*>) {
        setCredential_calledWith = baseCredentialSetRequest
    }

}
