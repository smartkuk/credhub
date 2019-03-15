package org.cloudfoundry.conjur

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest

interface ConjurCredentialRepository {
    fun setCredential(baseCredentialSetRequest: BaseCredentialSetRequest<*>)
}

