package org.cloudfoundry.credhub.data

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.cloudfoundry.credhub.CredhubTestApp
import org.cloudfoundry.credhub.DatabaseProfileResolver
import org.cloudfoundry.credhub.DatabaseUtilities
import org.cloudfoundry.credhub.SpringUtilities
import org.cloudfoundry.credhub.TestHelper
import org.cloudfoundry.credhub.audit.CEFAuditRecord
import org.cloudfoundry.credhub.credentials.DefaultCredentialVersionDataService.toUUID
import org.cloudfoundry.credhub.domain.CertificateCredentialVersion
import org.cloudfoundry.credhub.domain.CredentialVersion
import org.cloudfoundry.credhub.domain.Encryptor
import org.cloudfoundry.credhub.domain.PasswordCredentialVersion
import org.cloudfoundry.credhub.domain.SshCredentialVersion
import org.cloudfoundry.credhub.domain.ValueCredentialVersion
import org.cloudfoundry.credhub.entities.EncryptedValue
import org.cloudfoundry.credhub.entity.CertificateCredentialVersionData
import org.cloudfoundry.credhub.entity.Credential
import org.cloudfoundry.credhub.entity.PasswordCredentialVersionData
import org.cloudfoundry.credhub.entity.SshCredentialVersionData
import org.cloudfoundry.credhub.entity.ValueCredentialVersionData
import org.cloudfoundry.credhub.exceptions.MaximumSizeException
import org.cloudfoundry.credhub.exceptions.ParameterizedValidationException
import org.cloudfoundry.credhub.repositories.CredentialRepository
import org.cloudfoundry.credhub.repositories.CredentialVersionRepository
import org.cloudfoundry.credhub.services.CredentialVersionDataService
import org.cloudfoundry.credhub.services.EncryptionKeySet
import org.cloudfoundry.credhub.util.CurrentTimeProvider
import org.cloudfoundry.credhub.utils.StringUtil
import org.cloudfoundry.credhub.utils.UuidUtil
import org.cloudfoundry.credhub.views.FindCredentialResult
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.hasProperty
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.not
import org.hamcrest.collection.IsIterableContainingInOrder
import org.hamcrest.core.IsCollectionContaining.hasItem
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import java.util.function.Consumer

@RunWith(SpringRunner::class)
@ActiveProfiles(value = "unit-test", resolver = DatabaseProfileResolver::class)
@SpringBootTest(classes = arrayOf(CredhubTestApp::class))
@Transactional
@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Let's refactor this class into kotlin")
class CredentialVersionDataServiceTest {

    @Autowired
    private val credentialVersionRepository: CredentialVersionRepository? = null

    @Autowired
    private val credentialRepository: CredentialRepository? = null

    @Autowired
    private val encryptionKeyCanaryDataService: EncryptionKeyCanaryDataService? = null

    @Autowired
    private val credentialDataService: CredentialDataService? = null

    @Autowired
    private val keySet: EncryptionKeySet? = null

    @MockBean
    private val mockCurrentTimeProvider: CurrentTimeProvider? = null

    @Autowired
    private val subject: CredentialVersionDataService? = null

    @Autowired
    private val cefAuditRecord: CEFAuditRecord? = null

    @Autowired
    private val encryptor: Encryptor? = null

    private var fakeTimeSetter: Consumer<Long>? = null
    private var activeCanaryUuid: UUID? = null
    private var passwordCredential2: PasswordCredentialVersionData? = null
    private var namedPasswordCredential1: PasswordCredentialVersionData? = null
    private var valueCredentialData: ValueCredentialVersionData? = null

    @Before
    fun beforeEach() {
        fakeTimeSetter = TestHelper.mockOutCurrentTimeProvider(mockCurrentTimeProvider)
        fakeTimeSetter!!.accept(345345L)

        activeCanaryUuid = keySet!!.active.uuid
    }

    @Test
    fun save_givenANewCredential_savesTheCredential() {
        val passwordCredentialData = PasswordCredentialVersionData("/my-credential")
        passwordCredentialData.encryptedValueData = EncryptedValue(
            activeCanaryUuid,
            "credential-password",
            ""
        )
        val credential = PasswordCredentialVersion(passwordCredentialData)
        credential.setEncryptor(encryptor)
        val savedCredentialVersion: CredentialVersion = subject!!.save(credential)

        assertNotNull(savedCredentialVersion)

        val savedPasswordCredential = subject
            .findMostRecent("/my-credential") as PasswordCredentialVersion?
        val credentialVersionData = credentialVersionRepository!!
            .findOneByUuid(savedCredentialVersion.uuid)

        assertThat(savedPasswordCredential!!.name, equalTo(credential.name))
        assertThat(savedPasswordCredential.uuid, equalTo<UUID>(credential.uuid))

        assertThat(credentialVersionData.credential.name, equalTo("/my-credential"))
        assertThat(credentialVersionData.encryptedValueData.encryptedValue,
            equalTo("credential-password".toByteArray(StringUtil.UTF_8)))
    }

    @Test
    fun save_givenAnExistingCredential_updatesTheCredential() {
        val passwordCredentialData = PasswordCredentialVersionData("/my-credential-2")
        passwordCredentialData.encryptedValueData = EncryptedValue(
            activeCanaryUuid,
            "credential-password",
            "nonce"
        )
        val credential = PasswordCredentialVersion(passwordCredentialData)

        subject!!.save(credential)

        passwordCredentialData.encryptedValueData.encryptedValue = "irynas-ninja-skills".toByteArray(StringUtil.UTF_8)

        subject.save(credential)

        val savedPasswordCredential = subject
            .findMostRecent("/my-credential-2") as PasswordCredentialVersion?
        val credentialVersionData = credentialVersionRepository!!
            .findOneByUuid(savedPasswordCredential!!.uuid)

        assertThat(credentialVersionData.credential.name, equalTo("/my-credential-2"))
        assertThat(credentialVersionData.encryptedValueData.encryptedValue,
            equalTo("irynas-ninja-skills".toByteArray(StringUtil.UTF_8)))
        assertThat(credentialVersionData.uuid, equalTo(credential.uuid))
    }

    @Test(expected = ParameterizedValidationException::class)
    fun save_givenAnExistingCredential_throwsExceptionIfTypeMismatch() {

        val encryptedValueA = EncryptedValue()
        encryptedValueA.encryptionKeyUuid = activeCanaryUuid
        encryptedValueA.encryptedValue = byteArrayOf()
        encryptedValueA.nonce = byteArrayOf()

        val passwordCredentialData = PasswordCredentialVersionData("/my-credential-3")
        passwordCredentialData.encryptedValueData = encryptedValueA
        val credential = PasswordCredentialVersion(passwordCredentialData)

        subject!!.save(credential)

        val encryptedValueB = EncryptedValue()
        encryptedValueB.encryptionKeyUuid = activeCanaryUuid
        encryptedValueB.encryptedValue = "some value".toByteArray(StringUtil.UTF_8)

        val newCredentialData = ValueCredentialVersionData()
        newCredentialData.encryptedValueData = encryptedValueB
        newCredentialData.credential = passwordCredentialData.credential
        val newCredential = ValueCredentialVersion(newCredentialData)

        subject.save(newCredential)
    }

    @Test
    fun save_givenANewCredential_generatesTheUuid() {
        val credential = SshCredentialVersion("/my-credential-2")
        credential.setEncryptor(encryptor)
        credential.privateKey = "privatekey"
        credential.publicKey = "fake-public-key"
        var savedCredential = subject!!.save(credential) as SshCredentialVersion

        val generatedUuid = savedCredential.uuid
        assertNotNull(generatedUuid)

        savedCredential.publicKey = "updated-fake-public-key"
        savedCredential = subject.save(savedCredential) as SshCredentialVersion

        assertThat(savedCredential.uuid, equalTo<UUID>(generatedUuid))
    }

    @Test
    fun save_givenACredentialWithALeadingSlash_savesWithTheLeadingSlash() {
        val passwordCredentialData = PasswordCredentialVersionData("/my/credential")
        val credentialWithLeadingSlash = PasswordCredentialVersion(passwordCredentialData)

        subject!!.save(credentialWithLeadingSlash)

        val savedCredentialVersion = subject.findMostRecent("/my/credential")
        assertThat(savedCredentialVersion!!.credential.name, equalTo("/my/credential"))
    }

    @Test
    fun save_whenTheCredentialSavedWithEncryptedValueSet_setsTheMasterEncryptionKeyUuid() {
        val sshCredentialData = SshCredentialVersionData("/my-credential")
        val credential = SshCredentialVersion(sshCredentialData)
        credential.setEncryptor(encryptor)
        credential.privateKey = "private-key"
        credential.publicKey = "fake-public-key"
        subject!!.save(credential)

        assertThat(sshCredentialData.encryptionKeyUuid, equalTo<UUID>(activeCanaryUuid))
    }

    @Test
    fun save_whenTheCredentialSavedWithoutEncryptedValueSet_doesNotSetTheMasterEncryptionKeyUuid() {
        val sshCredentialData = SshCredentialVersionData("/my-credential")
        val credential = SshCredentialVersion(sshCredentialData)
        credential.setEncryptor(encryptor)
        credential.publicKey = "fake-public-key"
        subject!!.save(credential)

        assertThat(sshCredentialData.encryptionKeyUuid, nullValue())
    }

    @Test
    fun delete_onAnExistingCredential_returnsTrue() {
        credentialDataService!!.save(Credential("/my-credential"))

        assertThat(subject!!.delete("/my-credential"), equalTo(true))
    }

    @Test
    fun delete_onACredentialName_deletesAllCredentialsWithTheName() {
        val credential = credentialDataService!!
            .save(Credential("/my-credential"))

        val encryptedValueA = EncryptedValue()
        encryptedValueA.encryptionKeyUuid = activeCanaryUuid
        encryptedValueA.encryptedValue = "credential-password".toByteArray(StringUtil.UTF_8)
        encryptedValueA.nonce = "nonce".toByteArray(StringUtil.UTF_8)

        val credentialDataA = PasswordCredentialVersionData()
        credentialDataA.credential = credential
        credentialDataA.encryptedValueData = encryptedValueA
        subject!!.save(credentialDataA)

        val encryptedValueB = EncryptedValue()
        encryptedValueB.encryptionKeyUuid = activeCanaryUuid
        encryptedValueB.encryptedValue = "another password".toByteArray(StringUtil.UTF_8)
        encryptedValueB.nonce = "nonce".toByteArray(StringUtil.UTF_8)

        val credentialDataB = PasswordCredentialVersionData("/my-credential")
        credentialDataB.credential = credential
        credentialDataB.encryptedValueData = encryptedValueB
        subject.save(credentialDataB)

        assertThat(subject.findAllByName("/my-credential"), hasSize(2))

        subject.delete("/my-credential")

        assertThat(subject.findAllByName("/my-credential"), hasSize(0))
        assertNull(credentialDataService.find("/my-credential"))
    }

    @Test
    fun delete_givenACredentialNameCasedDifferentlyFromTheActual_shouldBeCaseInsensitive() {
        val credentialName = credentialDataService!!
            .save(Credential("/my-credential"))

        val encryptedValueA = EncryptedValue()
        encryptedValueA.encryptionKeyUuid = activeCanaryUuid
        encryptedValueA.encryptedValue = "credential-password".toByteArray(StringUtil.UTF_8)
        encryptedValueA.nonce = byteArrayOf()

        var credential = PasswordCredentialVersionData()
        credential.credential = credentialName
        credential.encryptedValueData = encryptedValueA
        subject!!.save(credential)

        val encryptedValueB = EncryptedValue()
        encryptedValueB.encryptionKeyUuid = activeCanaryUuid
        encryptedValueB.encryptedValue = "another password".toByteArray(StringUtil.UTF_8)
        encryptedValueB.nonce = byteArrayOf()

        credential = PasswordCredentialVersionData()
        credential.credential = credentialName
        credential.encryptedValueData = encryptedValueB

        subject.save(credential)

        assertThat(subject.findAllByName("/my-credential"), hasSize(2))

        subject.delete("/MY-CREDENTIAL")

        assertThat(subject.findAllByName("/my-credential"), empty())
    }

    @Test
    fun delete_givenANonExistentCredentialName_returnsFalse() {
        assertThat(subject!!.delete("/does/not/exist"), equalTo(false))
    }

    @Test
    fun findMostRecent_givenACredentialNameWithoutVersions_returnsNull() {
        credentialDataService!!.save(Credential("/my-unused-CREDENTIAL"))

        assertNull(subject!!.findMostRecent("/my-unused-CREDENTIAL"))
    }

    @Test
    fun findMostRecent_givenACredentialName_returnsMostRecentCredentialWithoutCaseSensitivity() {
        setupTestFixtureForFindMostRecent()

        val passwordCredential = subject!!.findMostRecent("/my-credential") as PasswordCredentialVersion?

        assertThat(passwordCredential!!.name, equalTo("/my-CREDENTIAL"))
        assertThat(passwordCredential2!!.encryptedValueData.encryptedValue, equalTo("/my-new-password".toByteArray(StringUtil.UTF_8)))
    }

    @Test
    fun findByUuid_givenAUuid_findsTheCredential() {

        val encryptedValue = EncryptedValue()
        encryptedValue.encryptionKeyUuid = activeCanaryUuid
        encryptedValue.encryptedValue = "credential-password".toByteArray(StringUtil.UTF_8)
        encryptedValue.nonce = "nonce".toByteArray(StringUtil.UTF_8)

        val passwordCredentialData = PasswordCredentialVersionData("/my-credential")
        passwordCredentialData.encryptedValueData = encryptedValue
        val credential = PasswordCredentialVersion(passwordCredentialData)
        val savedCredential = subject!!.save(credential) as PasswordCredentialVersion

        assertNotNull(savedCredential.uuid)
        val oneByUuid = subject
            .findByUuid(savedCredential.uuid!!.toString()) as PasswordCredentialVersion?
        assertThat(oneByUuid!!.name, equalTo("/my-credential"))
        assertThat(passwordCredentialData.encryptedValueData.encryptedValue,
            equalTo("credential-password".toByteArray(StringUtil.UTF_8)))
    }

    @Test
    fun findContainingName_givenACredentialName_returnsCredentialsInReverseChronologicalOrder() {
        val valueName = "/value.Credential"
        val passwordName = "/password/Credential"
        val certificateName = "/certif/ic/atecredential"

        setupTestFixturesForFindContainingName(valueName, passwordName, certificateName)

        assertThat(subject!!.findContainingName("CREDENTIAL"), IsIterableContainingInOrder.contains(
            hasProperty("name", equalTo(certificateName)),
            hasProperty("name", equalTo(valueName)),
            hasProperty("name", equalTo(passwordName))))

        val encryptedValue = EncryptedValue()
        encryptedValue.encryptionKeyUuid = activeCanaryUuid
        encryptedValue.encryptedValue = "new-encrypted-value".toByteArray(StringUtil.UTF_8)
        encryptedValue.nonce = "nonce".toByteArray(StringUtil.UTF_8)

        val valueCredential = subject.findMostRecent("/value.Credential") as ValueCredentialVersion?
        valueCredentialData!!.encryptedValueData = encryptedValue
        subject.save(valueCredential!!)

        assertThat("The credentials are ordered by versionCreatedAt",
            subject.findContainingName("CREDENTIAL"), IsIterableContainingInOrder.contains(
            hasProperty("name", equalTo(certificateName)),
            hasProperty("name", equalTo(valueName)),
            hasProperty("name", equalTo(passwordName))
        ))
    }

    @Test
    fun findContainingName_whenThereAreMultipleVerionsOfACredential() {
        savePassword(2000000000123L, "/foo/DUPLICATE")
        savePassword(1000000000123L, "/foo/DUPLICATE")
        savePassword(3000000000123L, "/bar/duplicate")
        savePassword(4000000000123L, "/bar/duplicate")

        val credentials = subject!!.findContainingName("DUP")
        assertThat("should only return unique credential names", credentials.size, equalTo(2))

        var credential = credentials[0]
        assertThat(credential.name, equalTo("/bar/duplicate"))
        assertThat("should return the most recently created version",
            credential.versionCreatedAt, equalTo(Instant.ofEpochMilli(4000000000123L)))

        credential = credentials[1]
        assertThat(credential.name, equalTo("/foo/DUPLICATE"))
        assertThat("should return the most recently created version",
            credential.versionCreatedAt, equalTo(Instant.ofEpochMilli(2000000000123L)))
    }

    @Test
    fun findStartingWithPath_whenProvidedAPath_returnsTheListOfOrderedCredentials() {
        setupTestFixtureForFindStartingWithPath()

        var credentials = subject!!.findStartingWithPath("Credential/")

        assertThat(credentials.size, equalTo(3))
        assertThat(credentials, IsIterableContainingInOrder.contains(
            hasProperty("name", equalTo("/Credential/2")),
            hasProperty("name", equalTo("/credential/1")),
            hasProperty("name", equalTo("/CREDENTIAL/3"))
        ))
        assertThat(
            "should return a list of credentials in chronological order that start with a given string",
            credentials, not(contains(hasProperty<FindCredentialResult>("notSoSecret"))))

        val passwordCredential = subject
            .findMostRecent("/credential/1") as PasswordCredentialVersion?
        passwordCredential!!.setPasswordAndGenerationParameters("new-password", null)
        subject.save(passwordCredential)
        credentials = subject.findStartingWithPath("Credential/")
        assertThat("should return credentials in order by version_created_at, not updated_at",
            credentials, IsIterableContainingInOrder.contains(
            hasProperty("name", equalTo("/Credential/2")),
            hasProperty("name", equalTo("/credential/1")),
            hasProperty("name", equalTo("/CREDENTIAL/3"))
        ))

    }

    @Test
    fun findStartingWithPath_givenMultipleVersionsOfACredential() {
        savePassword(2000000000123L, "/DupSecret/1")
        savePassword(3000000000123L, "/DupSecret/1")
        savePassword(1000000000123L, "/DupSecret/1")

        val credentials = subject!!.findStartingWithPath("/dupsecret/")
        assertThat("should not return duplicate credential names",
            credentials.size, equalTo(1))

        val credential = credentials[0]
        assertThat("should return the most recent credential",
            credential.versionCreatedAt, equalTo(Instant.ofEpochMilli(3000000000123L)))
    }

    @Test
    fun findStartingWithPath_givenAPath_matchesFromTheStart() {
        setupTestFixtureForFindStartingWithPath()

        val credentials = subject!!.findStartingWithPath("Credential")

        assertThat(credentials.size, equalTo(3))
        assertThat(credentials, not(contains(hasProperty<FindCredentialResult>("name", equalTo("/not/So/Credential")))))

        assertThat("appends trailing slash to path", credentials,
            not(contains(hasProperty<FindCredentialResult>("name", equalTo("/CREDENTIALnotrailingslash")))))

        assertThat("appends trailing slash to path", credentials[0].name.toLowerCase(),
            containsString("/credential/"))
    }

    @Test
    fun findAllPaths_returnsCompleteDirectoryStructure() {
        val valueOther = "/fubario"
        val valueName = "/value/Credential"
        val passwordName = "/password/Credential"
        val certificateName = "/certif/ic/ateCredential"

        var valueCredentialData = ValueCredentialVersionData(valueOther)
        var valueCredential = ValueCredentialVersion(valueCredentialData)
        subject!!.save(valueCredential)

        valueCredentialData = ValueCredentialVersionData(valueName)
        valueCredential = ValueCredentialVersion(valueCredentialData)
        subject.save(valueCredential)

        val passwordCredentialData = PasswordCredentialVersionData(passwordName)
        val passwordCredential = PasswordCredentialVersion(passwordCredentialData)
        subject.save(passwordCredential)

        val certificateCredentialData = CertificateCredentialVersionData(certificateName)
        val certificateCredential = CertificateCredentialVersion(
            certificateCredentialData)
        subject.save(certificateCredential)

    }

    @Test
    fun findAllByName_whenProvidedAName_findsAllMatchingCredentials() {
        val credential1 = savePassword(2000000000123L, "/secret1")
        val credential2 = savePassword(4000000000123L, "/seCret1")
        savePassword(3000000000123L, "/Secret2")

        val credentialVersions = subject!!.findAllByName("/Secret1")
        assertThat(credentialVersions, containsInAnyOrder(hasProperty("uuid", equalTo<UUID>(credential1.uuid)),
            hasProperty("uuid", equalTo<UUID>(credential2.uuid))))

        assertThat("returns empty list when no credential matches",
            subject.findAllByName("does/NOT/exist"), empty())
    }

    @Test
    fun findNByName_whenProvidedANameAndCount_findsCountMatchingCredentials() {
        val credential1 = savePassword(2000000000125L, "/secret1")
        val credential2 = savePassword(2000000000124L, "/seCret1")
        savePassword(2000000000123L, "/secret1")
        savePassword(3000000000123L, "/Secret2")

        val credentialVersions = subject!!.findNByName("/Secret1", 2)
        assertThat(
            credentialVersions,
            containsInAnyOrder(
                hasProperty("uuid", equalTo<UUID>(credential1.uuid)),
                hasProperty("uuid", equalTo<UUID>(credential2.uuid))
            )
        )

        assertThat("returns empty list when no credential matches",
            subject.findNByName("does/NOT/exist", 12), empty())
    }

    @Test
    fun findNByName_whenAskedForTooManyVersions_returnsAllVersions() {
        val credential1 = savePassword(2000000000123L, "/secret1")

        val credentialVersions = subject!!.findNByName("/Secret1", 2)

        assertThat(credentialVersions.size, equalTo(1))
        assertThat(credentialVersions[0].uuid, equalTo<UUID>(credential1.uuid))
    }

    @Test(expected = IllegalArgumentException::class)
    fun findNByName_whenAskedForANegativeNumberOfVersions_throws() {
        savePassword(2000000000123L, "/secret1")

        val credentialVersions = subject!!.findNByName("/Secret1", -2)

        assertThat(credentialVersions.size, equalTo(0))
    }

    @Test
    @Throws(Exception::class)
    fun findActiveByName_whenAskedForCertificate_returnsTransitionalValueInAddition() {
        saveCertificate(2000000000123L, "/some-certificate")
        val version2 = saveTransitionalCertificate(2000000000123L, "/some-certificate")
        val version3 = saveCertificate(2000000000229L, "/some-certificate")

        val credentialVersions = subject!!.findActiveByName("/some-certificate")

        assertThat(credentialVersions!!.size, equalTo(2))
        assertThat(credentialVersions,
            containsInAnyOrder(
                hasProperty("uuid", equalTo<UUID>(version2.uuid)),
                hasProperty("uuid", equalTo<UUID>(version3.uuid))
            ))
    }

    @Test
    @Throws(Exception::class)
    fun findActiveByName_whenAskedNonCertificateType_returnsOneCredentialValue() {
        savePassword(2000000000123L, "/test/password")
        savePassword(3000000000123L, "/test/password")
        val password3 = savePassword(4000000000123L, "/test/password")

        val credentialVersions = subject!!.findActiveByName("/test/password")

        assertThat(credentialVersions!!.size, equalTo(1))
        assertThat(credentialVersions, contains(
            hasProperty("uuid", equalTo<UUID>(password3.uuid))))
    }


    @Test
    fun findAllCertificateCredentialsByCaName_returnsCertificatesSignedByTheCa() {
        saveCertificate(2000000000123L, "/ca-cert")
        saveCertificateByCa(2000000000125L, "/cert1", "/ca-cert")
        saveCertificateByCa(2000000000126L, "/cert2", "/ca-cert")

        saveCertificate(2000000000124L, "/ca-cert2")
        saveCertificateByCa(2000000000127L, "/cert3", "/ca-cert2")

        var certificates = subject!!.findAllCertificateCredentialsByCaName("/ca-cert")
        assertThat(certificates, containsInAnyOrder(equalTo("/cert1"),
            equalTo("/cert2")))
        assertThat(certificates, not<Iterable<*>>(hasItem("/cert3")))
        certificates = subject.findAllCertificateCredentialsByCaName("/ca-cert2")
        assertThat(certificates, hasItem("/cert3"))
        assertThat(certificates, not<Iterable<*>>(hasItem("/cert1")))
        assertThat(certificates, not<Iterable<*>>(hasItem("/cert2")))
    }

    @Test
    fun findAllCertificateCredentialsByCaName_isCaseInsensitive() {
        saveCertificate(2000000000123L, "/ca-cert")
        saveCertificateByCa(2000000000125L, "/cert1", "/ca-cert")
        saveCertificateByCa(2000000000126L, "/cert2", "/ca-cert")

        val certificates = subject!!.findAllCertificateCredentialsByCaName("/ca-CERT")
        assertThat(certificates, containsInAnyOrder(equalTo("/cert1"),
            equalTo("/cert2")))
    }

    @Test
    fun `toUUID converts byte array to UUID`(){
        val uuid = UUID.randomUUID()
        val uuidBytes = UuidUtil.uuidToByteArray(uuid)

        val convertedUUID = toUUID(uuidBytes)
        assertThat(convertedUUID).isEqualTo(uuid)
    }

    @Test
    fun `toUUID returns UUID when UUID is passed in`(){
        val uuid = UUID.randomUUID()
        val convertedUUID = toUUID(uuid)
        assertThat(convertedUUID).isEqualTo(uuid)
    }

    @Test
    fun `toUUID throws error when object is not byte array or UUID`(){
        assertThatThrownBy {
            toUUID("Some uuid")
        }.hasMessageStartingWith("Expected byte[] or UUID type. Received")
    }

    private fun savePassword(timeMillis: Long, name: String, canaryUuid: UUID?): PasswordCredentialVersion {
        fakeTimeSetter!!.accept(timeMillis)
        var credential: Credential? = credentialDataService!!.find(name)
        if (credential == null) {
            credential = credentialDataService.save(Credential(name))
        }

        val encryptedValue = EncryptedValue()
        encryptedValue.encryptionKeyUuid = canaryUuid
        encryptedValue.encryptedValue = byteArrayOf()
        encryptedValue.nonce = byteArrayOf()

        val credentialObject = PasswordCredentialVersionData()
        credentialObject.credential = credential
        credentialObject.encryptedValueData = encryptedValue
        return subject!!.save(credentialObject) as PasswordCredentialVersion
    }

    private fun savePassword(timeMillis: Long, credentialName: String): PasswordCredentialVersion {
        return savePassword(timeMillis, credentialName, activeCanaryUuid)
    }

    private fun saveCertificate(timeMillis: Long, name: String, caName: String?, canaryUuid: UUID?,
                                transitional: Boolean): CertificateCredentialVersion {
        fakeTimeSetter!!.accept(timeMillis)
        var credential: Credential? = credentialDataService!!.find(name)
        if (credential == null) {
            credential = credentialDataService.save(Credential(name))
        }

        val encryptedValue = EncryptedValue()
        encryptedValue.encryptionKeyUuid = canaryUuid
        encryptedValue.encryptedValue = byteArrayOf()
        encryptedValue.nonce = byteArrayOf()

        val credentialObject = CertificateCredentialVersionData()
        credentialObject.credential = credential
        credentialObject.encryptedValueData = encryptedValue
        if (caName != null) {
            credentialObject.caName = caName
        }
        credentialObject.isTransitional = transitional
        return subject!!.save(credentialObject) as CertificateCredentialVersion
    }

    private fun saveCertificate(timeMillis: Long, credentialName: String): CertificateCredentialVersion {
        return saveCertificate(timeMillis, credentialName, null, activeCanaryUuid, false)
    }

    private fun saveTransitionalCertificate(timeMillis: Long, credentialName: String): CertificateCredentialVersion {
        return saveCertificate(timeMillis, credentialName, null, activeCanaryUuid, true)
    }

    private fun saveCertificateByCa(
        timeMillis: Long, credentialName: String, caName: String): CertificateCredentialVersion {
        return saveCertificate(timeMillis, credentialName, caName, activeCanaryUuid, false)
    }


    private fun setupTestFixtureForFindMostRecent() {
        val credential = credentialDataService!!
            .save(Credential("/my-CREDENTIAL"))

        val encryptedValueA = EncryptedValue()
        encryptedValueA.encryptionKeyUuid = activeCanaryUuid
        encryptedValueA.encryptedValue = "/my-old-password".toByteArray(StringUtil.UTF_8)
        encryptedValueA.nonce = byteArrayOf()

        namedPasswordCredential1 = PasswordCredentialVersionData()
        namedPasswordCredential1!!.credential = credential
        namedPasswordCredential1!!.encryptedValueData = encryptedValueA

        val encryptedValueB = EncryptedValue()
        encryptedValueB.encryptionKeyUuid = activeCanaryUuid
        encryptedValueB.encryptedValue = "/my-new-password".toByteArray(StringUtil.UTF_8)
        encryptedValueB.nonce = byteArrayOf()

        passwordCredential2 = PasswordCredentialVersionData()
        passwordCredential2!!.credential = credential
        passwordCredential2!!.encryptedValueData = encryptedValueB

        subject!!.save(namedPasswordCredential1!!)
        fakeTimeSetter!!.accept(345346L) // 1 second later
        subject.save(passwordCredential2!!)
    }

    private fun setupTestFixturesForFindContainingName(
        valueName: String,
        passwordName: String,
        certificateName: String
    ) {

        val encryptedValueA = EncryptedValue()
        encryptedValueA.encryptionKeyUuid = activeCanaryUuid
        encryptedValueA.encryptedValue = "value".toByteArray(StringUtil.UTF_8)
        encryptedValueA.nonce = byteArrayOf()

        fakeTimeSetter!!.accept(2000000000123L)
        valueCredentialData = ValueCredentialVersionData(valueName)
        valueCredentialData!!.encryptedValueData = encryptedValueA
        val namedValueCredential = ValueCredentialVersion(valueCredentialData)
        namedValueCredential.setEncryptor(encryptor)
        subject!!.save(namedValueCredential)

        var passwordCredentialData = PasswordCredentialVersionData("/mySe.cret")
        passwordCredentialData.encryptedValueData = EncryptedValue(activeCanaryUuid, "", "")
        PasswordCredentialVersion(passwordCredentialData)
        val namedPasswordCredential: PasswordCredentialVersion
        subject.save(namedValueCredential)

        val encryptedValueB = EncryptedValue()
        encryptedValueB.encryptionKeyUuid = activeCanaryUuid
        encryptedValueB.encryptedValue = "password".toByteArray(StringUtil.UTF_8)
        encryptedValueB.nonce = byteArrayOf()

        fakeTimeSetter!!.accept(1000000000123L)
        passwordCredentialData = PasswordCredentialVersionData(passwordName)
        passwordCredentialData.encryptedValueData = encryptedValueB
        namedPasswordCredential = PasswordCredentialVersion(passwordCredentialData)
        subject.save(namedPasswordCredential)

        var certificateCredentialData = CertificateCredentialVersionData(
            "/myseecret")
        var certificateCredential = CertificateCredentialVersion(
            certificateCredentialData)
        subject.save(certificateCredential)

        fakeTimeSetter!!.accept(3000000000123L)
        certificateCredentialData = CertificateCredentialVersionData(
            certificateName)
        certificateCredential = CertificateCredentialVersion(certificateCredentialData)
        subject.save(certificateCredential)
    }

    @Test
    fun shouldThrowAnMaximumSizeException_whenDataExceedsMaximumSize() {
        if (System.getProperty(SpringUtilities.activeProfilesString).contains(SpringUtilities.unitTestPostgresProfile)) {
            return
        }

        val exceedsMaxBlobStoreValue = DatabaseUtilities.getExceedsMaxBlobStoreSizeBytes()

        val credentialName = "some_name"
        val entity = ValueCredentialVersionData()
        val credential = credentialRepository!!.save(Credential(credentialName))

        val encryptedValue = EncryptedValue()
        encryptedValue.encryptedValue = exceedsMaxBlobStoreValue
        encryptedValue.encryptionKeyUuid = activeCanaryUuid
        encryptedValue.nonce = "nonce".toByteArray(StringUtil.UTF_8)

        entity.credential = credential
        entity.encryptedValueData = encryptedValue

        assertThatThrownBy { subject!!.save(entity) }.isInstanceOf(MaximumSizeException::class.java)
    }

    private fun setupTestFixtureForFindStartingWithPath() {
        savePassword(2000000000123L, "/credential/1")
        savePassword(3000000000123L, "/Credential/2")
        savePassword(1000000000123L, "/CREDENTIAL/3")
        savePassword(1000000000123L, "/not/So/Credential")
        savePassword(1000000000123L, "/CREDENTIALnotrailingslash")
    }
}
