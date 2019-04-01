package org.cloudfoundry.conjur

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.views.CredentialView

class SpyConjurCredentialRepository : ConjurCredentialRepository {
    lateinit var setCredential_calledWith: BaseCredentialSetRequest<*>

    lateinit var getCredential_returnValue: CredentialView
    lateinit var getCredential_calledWithName: String

    override fun setCredential(baseCredentialSetRequest: BaseCredentialSetRequest<*>) {
        setCredential_calledWith = baseCredentialSetRequest
    }

    override fun getCredential(credentialName: String): CredentialView {
        getCredential_calledWithName = credentialName
        return getCredential_returnValue
    }

}
