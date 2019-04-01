package org.cloudfoundry.conjur

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.views.CredentialView

class SpyConjurCredentialService : ConjurCredentialService {

    lateinit var setCredentialReturn: CredentialView
    lateinit var getCredentialReturn: CredentialView
    lateinit var setCredential_calledWith: BaseCredentialSetRequest<*>
    lateinit var getCredential_calledWithCredentialName: String

    override fun setCredential(baseCredentialSetRequest: BaseCredentialSetRequest<*>): CredentialView {
        setCredential_calledWith = baseCredentialSetRequest

        return setCredentialReturn
    }

    override fun getCredential(credentialName: String): CredentialView {
        getCredential_calledWithCredentialName = credentialName
        return getCredentialReturn
    }
}
