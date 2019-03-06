package org.cloudfoundry.cyberark

import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.util.TimeProvider
import org.cloudfoundry.credhub.views.CredentialView

class DefaultCyberArkCredentialService(
    val cyberArkCredentialRepository: CyberArkCredentialRepository,
    val timeProvider: TimeProvider,
    val uuidProvider: UuidProvider
) : CyberArkCredentialService {

    override fun setCredential(baseCredentialSetRequest: BaseCredentialSetRequest<*>): CredentialView {
        cyberArkCredentialRepository.setCredential(baseCredentialSetRequest)
        return CredentialView(
            timeProvider.getInstant(),
            uuidProvider.generateUuid(),
            baseCredentialSetRequest.name,
            baseCredentialSetRequest.type,
            baseCredentialSetRequest.credentialValue
        )
    }
}
