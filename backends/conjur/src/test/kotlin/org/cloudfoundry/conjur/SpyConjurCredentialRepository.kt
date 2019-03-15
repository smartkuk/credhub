package org.cloudfoundry.conjur

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest

class SpyConjurCredentialRepository : ConjurCredentialRepository {
    lateinit var setCredential_calledWith: BaseCredentialSetRequest<*>

    override fun setCredential(baseCredentialSetRequest: BaseCredentialSetRequest<*>) {
        setCredential_calledWith = baseCredentialSetRequest
    }

}
