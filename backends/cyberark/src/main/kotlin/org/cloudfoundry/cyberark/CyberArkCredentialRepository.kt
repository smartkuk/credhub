package org.cloudfoundry.cyberark

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest

interface CyberArkCredentialRepository {
    fun setCredential(baseCredentialSetRequest: BaseCredentialSetRequest<*>)
}

