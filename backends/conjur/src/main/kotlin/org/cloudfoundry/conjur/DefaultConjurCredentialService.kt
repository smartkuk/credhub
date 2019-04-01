package org.cloudfoundry.conjur

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.util.TimeProvider
import org.cloudfoundry.credhub.views.CredentialView

class DefaultConjurCredentialService(
    val conjurCredentialRepository: ConjurCredentialRepository,
    val timeProvider: TimeProvider,
    val uuidProvider: UuidProvider
) : ConjurCredentialService {

    override fun setCredential(baseCredentialSetRequest: BaseCredentialSetRequest<*>): CredentialView {
        conjurCredentialRepository.setCredential(baseCredentialSetRequest)
        return CredentialView(
            timeProvider.getInstant(),
            uuidProvider.generateUuid(),
            baseCredentialSetRequest.name,
            baseCredentialSetRequest.type,
            baseCredentialSetRequest.credentialValue
        )
    }

    override fun getCredential(credentialName: String): CredentialView {
        return conjurCredentialRepository.getCredential(credentialName)
    }
}
