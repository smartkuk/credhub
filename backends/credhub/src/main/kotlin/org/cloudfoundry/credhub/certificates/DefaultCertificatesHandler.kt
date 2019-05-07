package org.cloudfoundry.credhub.certificates

import com.google.common.collect.Lists
import org.cloudfoundry.credhub.ErrorMessages
import org.cloudfoundry.credhub.PermissionOperation
import org.cloudfoundry.credhub.PermissionOperation.DELETE
import org.cloudfoundry.credhub.PermissionOperation.READ
import org.cloudfoundry.credhub.PermissionOperation.WRITE
import org.cloudfoundry.credhub.audit.AuditableCredential
import org.cloudfoundry.credhub.audit.CEFAuditRecord
import org.cloudfoundry.credhub.auth.UserContextHolder
import org.cloudfoundry.credhub.credential.CertificateCredentialValue
import org.cloudfoundry.credhub.domain.CertificateCredentialVersion
import org.cloudfoundry.credhub.domain.CredentialVersion
import org.cloudfoundry.credhub.entity.Credential
import org.cloudfoundry.credhub.exceptions.EntryNotFoundException
import org.cloudfoundry.credhub.exceptions.PermissionException
import org.cloudfoundry.credhub.generate.GenerationRequestGenerator
import org.cloudfoundry.credhub.generate.UniversalCredentialGenerator
import org.cloudfoundry.credhub.requests.CertificateRegenerateRequest
import org.cloudfoundry.credhub.requests.CreateVersionRequest
import org.cloudfoundry.credhub.requests.UpdateTransitionalVersionRequest
import org.cloudfoundry.credhub.services.DefaultCertificateService
import org.cloudfoundry.credhub.services.PermissionCheckingService
import org.cloudfoundry.credhub.views.CertificateCredentialView
import org.cloudfoundry.credhub.views.CertificateCredentialsView
import org.cloudfoundry.credhub.views.CertificateVersionView
import org.cloudfoundry.credhub.views.CertificateView
import org.cloudfoundry.credhub.views.CredentialView
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.ArrayList
import java.util.UUID

@Service
class DefaultCertificatesHandler(
    private val certificateService: DefaultCertificateService,
    private val credentialGenerator: UniversalCredentialGenerator,
    private val generationRequestGenerator: GenerationRequestGenerator,
    private val auditRecord: CEFAuditRecord,
    private val permissionCheckingService: PermissionCheckingService,
    private val userContextHolder: UserContextHolder,
    @Value("\${security.authorization.acls.enabled}") private val enforcePermissions: Boolean
) : CertificatesHandler {

    override fun handleRegenerate(
        credentialUuid: String,
        request: CertificateRegenerateRequest
    ): CredentialView {

        checkPermissionsByUuid(credentialUuid, WRITE)

        val existingCredentialVersion = certificateService
            .findByCredentialUuid(credentialUuid)

        val generateRequest = generationRequestGenerator
            .createGenerateRequest(existingCredentialVersion)
        val credentialValue = credentialGenerator
            .generate(generateRequest) as CertificateCredentialValue

        credentialValue.isTransitional = request.isTransitional

        val credentialVersion = certificateService
            .save(
                existingCredentialVersion,
                credentialValue,
                generateRequest
            ) as CertificateCredentialVersion

        auditRecord.setVersion(credentialVersion)

        return CertificateView(credentialVersion)
    }

    override fun handleGetAllRequest(): CertificateCredentialsView {
        val credentialList = filterPermissions(certificateService.getAll())
        val list = convertCertificateCredentialsToCertificateCredentialViews(credentialList)

        auditRecord.addAllCredentials(Lists.newArrayList<AuditableCredential>(credentialList))

        return CertificateCredentialsView(list)
    }

    override fun handleGetByNameRequest(name: String): CertificateCredentialsView {
        checkPermissionsByName(name, READ)

        val credentialList = certificateService.getByName(name)
        val list = convertCertificateCredentialsToCertificateCredentialViews(credentialList)

        return CertificateCredentialsView(list)
    }

    override fun handleGetAllVersionsRequest(uuidString: String, current: Boolean): List<CertificateView> {
        checkPermissionsByUuid(uuidString, READ)

        val uuid: UUID
        try {
            uuid = UUID.fromString(uuidString)
        } catch (e: IllegalArgumentException) {
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }

        val credentialList = certificateService.getVersions(uuid, current)

        return credentialList.map { credential -> CertificateView(credential as CertificateCredentialVersion) }
    }

    override fun handleDeleteVersionRequest(certificateId: String, versionId: String): CertificateView {
        checkPermissionsByUuid(versionId, DELETE)

        val deletedVersion = certificateService
            .deleteVersion(UUID.fromString(certificateId), UUID.fromString(versionId))
        return CertificateView(deletedVersion)
    }

    override fun handleUpdateTransitionalVersion(
        certificateId: String,
        requestBody: UpdateTransitionalVersionRequest
    ): List<CertificateView> {
        checkPermissionsByUuid(certificateId, WRITE)
        var versionUUID: UUID? = null

        if (requestBody.versionUuid != null) {
            versionUUID = UUID.fromString(requestBody.versionUuid)
        }

        val credentialList: List<CredentialVersion>
        credentialList = certificateService
            .updateTransitionalVersion(UUID.fromString(certificateId), versionUUID)

        return credentialList
            .map { credential -> CertificateView(credential as CertificateCredentialVersion) }
    }

    override fun handleCreateVersionsRequest(certificateId: String, requestBody: CreateVersionRequest): CertificateView {
        checkPermissionsByUuid(certificateId, WRITE)

        val certificateCredentialValue = requestBody.value
        certificateCredentialValue.isTransitional = requestBody.isTransitional
        val credentialVersion = certificateService.set(
            UUID.fromString(certificateId),
            certificateCredentialValue
        )

        return CertificateView(credentialVersion)
    }

    private fun convertCertificateCredentialsToCertificateCredentialViews(certificateCredentialList: List<Credential>): List<CertificateCredentialView> {
        return certificateCredentialList.map { credential ->
            val certificateVersions = certificateService.getAllValidVersions(credential.uuid!!) as List<CertificateCredentialVersion>

            var signedBy = ""
            if (certificateVersions.isNotEmpty()) {
                if (certificateVersions.first().caName != null) {
                    signedBy = certificateVersions.first().caName
                } else if (certificateVersions.first().isSelfSigned) {
                    signedBy = credential.name!!
                }
            }

            val signedCertificates = if (credential.name != null) {
                certificateService.findSignedCertificates(credential.name!!)
            } else {
                emptyList()
            }

            val certificateVersionViews = certificateVersions.map { certificateVersion ->
                CertificateVersionView(
                    id = certificateVersion.uuid!!,
                    expiryDate = certificateVersion.expiryDate,
                    transitional = certificateVersion.isVersionTransitional
                )
            }

            CertificateCredentialView(credential.name, credential.uuid, certificateVersionViews, signedBy, signedCertificates)
        }
    }

    private fun checkPermissionsByName(name: String, permissionOperation: PermissionOperation) {
        if (!enforcePermissions) return

        if (!permissionCheckingService.hasPermission(
                userContextHolder.userContext.actor!!,
                name,
                permissionOperation
            )) {
            if (permissionOperation == WRITE) {
                throw PermissionException(ErrorMessages.Credential.INVALID_ACCESS)
            } else {
                throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
            }
        }
    }

    private fun checkPermissionsByUuid(uuid: String, permissionOperation: PermissionOperation) {
        if (!enforcePermissions) return

        val credential = certificateService.findByCredentialUuid(uuid)

        if (!permissionCheckingService.hasPermission(
                userContextHolder.userContext.actor!!,
                credential.name,
                permissionOperation
            )) {
            if (permissionOperation == WRITE) {
                throw PermissionException(ErrorMessages.Credential.INVALID_ACCESS)
            } else {
                throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
            }
        }
    }

    private fun filterPermissions(unfilteredCredentials: List<Credential>): List<Credential> {
        if (!enforcePermissions) {
            return unfilteredCredentials
        }
        val actor = userContextHolder.userContext.actor
        val paths = permissionCheckingService.findAllPathsByActor(actor)

        if (paths.contains("/*")) return unfilteredCredentials
        if (paths.isEmpty()) return ArrayList()

        val filteredCredentials = ArrayList<Credential>()

        for (credential in unfilteredCredentials) {
            val credentialName = credential.name
            if (paths.contains(credentialName)) {
                filteredCredentials.add(credential)
            }

            val result = ArrayList<String>()

            for (i in 1 until credentialName!!.length) {
                if (credentialName[i] == '/') {
                    result.add(credentialName.substring(0, i) + "/*")
                }
            }

            for (credentialPath in result) {
                if (paths.contains(credentialPath)) {
                    filteredCredentials.add(credential)
                    break
                }
            }
        }
        return filteredCredentials
    }
}
