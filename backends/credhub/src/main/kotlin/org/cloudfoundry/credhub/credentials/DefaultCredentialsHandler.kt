package org.cloudfoundry.credhub.credentials

import org.cloudfoundry.credhub.ErrorMessages
import org.cloudfoundry.credhub.PermissionOperation
import org.cloudfoundry.credhub.audit.CEFAuditRecord
import org.cloudfoundry.credhub.auth.UserContextHolder
import org.cloudfoundry.credhub.credential.CertificateCredentialValue
import org.cloudfoundry.credhub.domain.CredentialVersion
import org.cloudfoundry.credhub.exceptions.EntryNotFoundException
import org.cloudfoundry.credhub.exceptions.ParameterizedValidationException
import org.cloudfoundry.credhub.generate.UniversalCredentialGenerator
import org.cloudfoundry.credhub.requests.BaseCredentialGenerateRequest
import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.requests.CertificateSetRequest
import org.cloudfoundry.credhub.services.CertificateAuthorityService
import org.cloudfoundry.credhub.services.CredentialService
import org.cloudfoundry.credhub.services.PermissionCheckingService
import org.cloudfoundry.credhub.utils.CertificateReader
import org.cloudfoundry.credhub.views.CredentialView
import org.cloudfoundry.credhub.views.DataResponse
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DefaultCredentialsHandler(
    private val credentialService: CredentialService,
    private val auditRecord: CEFAuditRecord,
    private val permissionCheckingService: PermissionCheckingService,
    private val userContextHolder: UserContextHolder,
    private val certificateAuthorityService: CertificateAuthorityService,
    private val credentialGenerator: UniversalCredentialGenerator
)
    : CredentialsHandler {
    override fun generateCredential(generateRequest: BaseCredentialGenerateRequest): CredentialView {
        checkPermissionsByName(generateRequest.name, PermissionOperation.WRITE)

        val existingCredentialVersion = credentialService.findMostRecent(generateRequest.name)
        val value = credentialGenerator.generate(generateRequest)

        val credentialVersion = credentialService.save(existingCredentialVersion, value, generateRequest)

        auditRecord.setVersion(credentialVersion)
        auditRecord.setResource(credentialVersion.credential)
        return CredentialView.fromEntity(credentialVersion)
    }

    override fun setCredential(setRequest: BaseCredentialSetRequest<*>): CredentialView {
        checkPermissionsByName(setRequest.name, PermissionOperation.WRITE)
        if (setRequest is CertificateSetRequest) {
            // fill in the ca value if it's one of ours
            val certificateValue = setRequest.certificateValue

            val caName = certificateValue.caName

            if (caName != null) {
                validateCertificateValueIsSignedByCa(certificateValue, caName)
            }
        }

        val existingCredentialVersion = credentialService.findMostRecent(setRequest.name)

        val credentialVersion = credentialService.save(
            existingCredentialVersion,
            setRequest.credentialValue,
            setRequest
        )

        auditRecord.setVersion(credentialVersion)
        auditRecord.setResource(credentialVersion.credential)
        return CredentialView.fromEntity(credentialVersion)    }

    override fun deleteCredential(credentialName: String) {
        checkPermissionsByName(credentialName, PermissionOperation.DELETE)
        val deleteSucceeded = credentialService.delete(credentialName)
        if (!deleteSucceeded) {
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }
    }

    override fun getNCredentialVersions(credentialName: String, numberOfVersions: Int?): DataResponse {
        checkPermissionsByName(credentialName, PermissionOperation.READ)
        val credentialVersions: List<CredentialVersion>
        if (numberOfVersions == null) {
            credentialVersions = credentialService.findAllByName(credentialName)
        } else {
            credentialVersions = credentialService.findNByName(credentialName, numberOfVersions)

            for (credentialVersion in credentialVersions) {
                auditRecord.addVersion(credentialVersion)
                auditRecord.addResource(credentialVersion.credential)
            }
        }

        if (credentialVersions.isEmpty()) {
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }
        return DataResponse.fromEntity(credentialVersions)
    }

    override fun getAllCredentialVersions(credentialName: String): DataResponse {
        checkPermissionsByName(credentialName, PermissionOperation.READ)
        return getNCredentialVersions(credentialName, null)
    }

    override fun getCurrentCredentialVersions(credentialName: String): DataResponse {
        checkPermissionsByName(credentialName, PermissionOperation.READ)
        val credentialVersions = credentialService.findActiveByName(credentialName)

        if (credentialVersions.isEmpty()) {
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }
        return DataResponse.fromEntity(credentialVersions)
    }

    override fun getCredentialVersionByUUID(credentialUUID: String): CredentialView {
        checkPermissionsByUuid(credentialUUID, PermissionOperation.READ)
        return CredentialView.fromEntity(credentialService.findVersionByUuid(credentialUUID))
    }

    private fun validateCertificateValueIsSignedByCa(certificateValue: CertificateCredentialValue, caName: String) {
        checkPermissionsByName(caName, PermissionOperation.READ)
        val caValue = certificateAuthorityService.findActiveVersion(caName).certificate
        certificateValue.ca = caValue

        val certificateReader = CertificateReader(certificateValue.certificate)

        if (!certificateReader.isSignedByCa(caValue)) {
            throw ParameterizedValidationException(ErrorMessages.CERTIFICATE_WAS_NOT_SIGNED_BY_CA_NAME)
        }
    }

    private fun checkPermissionsByName(name: String, permissionOperation: PermissionOperation) {
        if (!permissionCheckingService.hasPermission(
                        userContextHolder.userContext.actor!!,
                        name,
                        permissionOperation
                )) {
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }
    }

    private fun checkPermissionsByUuid(uuid: String, permissionOperation: PermissionOperation) {
        if (!permissionCheckingService.hasPermission(
                        userContextHolder.userContext.actor!!,
                        UUID.fromString(uuid),
                        permissionOperation
                )) {
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }
    }
}
