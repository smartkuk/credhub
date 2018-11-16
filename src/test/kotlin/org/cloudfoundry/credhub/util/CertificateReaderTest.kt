package org.cloudfoundry.credhub.util

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.cloudfoundry.credhub.exceptions.MalformedCertificateException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.security.Security
import java.util.Arrays.asList
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bouncycastle.asn1.x509.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.core.IsEqual.equalTo

@RunWith(JUnit4::class)
class CertificateReaderTest {

    @Before
    fun beforeEach() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    @Test
    fun `when the ca basic constraint is true, isCa() returns true`() {
        val certificateReader = CertificateReader(CertificateStringConstants.SELF_SIGNED_CA_CERT)

        assertThat(certificateReader.isCa(), equalTo(true))
    }

    @Test
    fun `when the ca basic constraint is false, isCa() returns false`() {
        val certificateReader = CertificateReader(CertificateStringConstants.SIMPLE_SELF_SIGNED_TEST_CERT)
        assertThat(certificateReader.isCa(), equalTo(false))
    }

    @Test
    fun `when the certificate is X509V3 and does not have basic constraints, isCa() returns false`() {
        val certificateReader = CertificateReader(CertificateStringConstants.V3_CERT_WITHOUT_BASIC_CONSTRAINTS)

        assertThat(certificateReader.isCa(), equalTo(false))
    }

    @Test
    fun `when certificate is valid, certificateReader does not throw exception`() {
        CertificateReader(CertificateStringConstants.SIMPLE_SELF_SIGNED_TEST_CERT)
        CertificateReader(CertificateStringConstants.V3_CERT_WITHOUT_BASIC_CONSTRAINTS)
        CertificateReader(CertificateStringConstants.SELF_SIGNED_CA_CERT)
        CertificateReader(CertificateStringConstants.BIG_TEST_CERT)
    }

    @Test
    fun `when certificate is invalid, certificateReader throws exception`() {
        assertThatThrownBy {
            CertificateReader("penguin")
        }.isInstanceOf(MalformedCertificateException::class.java)

        assertThatThrownBy {
            CertificateReader("")
        }.isInstanceOf(MalformedCertificateException::class.java)
    }

    @Test
    fun `given a self signed certificate, certificateReader sets certificate fields correctly`() {
        val distinguishedName = "O=test-org, ST=Jupiter, C=MilkyWay, CN=test-common-name, OU=test-org-unit, L=Europa"

        val generalNames = GeneralNames(
                GeneralName(GeneralName.dNSName, "SolarSystem")
        )

        val certificateReader = CertificateReader(CertificateStringConstants.BIG_TEST_CERT)

        assertThat(certificateReader.getSubjectName().toString(), equalTo(distinguishedName))
        assertThat(certificateReader.getKeyLength(), equalTo(4096))
        assertThat<GeneralNames>(certificateReader.getAlternativeNames(), equalTo(generalNames))
        assertThat(
                asList(*certificateReader.getExtendedKeyUsage()!!.usages),
                containsInAnyOrder(KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth)
        )
        assertThat(
                certificateReader.getKeyUsage()!!.hasUsages(KeyUsage.digitalSignature),
                equalTo(true)
        )
        assertThat(certificateReader.getDurationDays(), equalTo(30))
        assertThat(certificateReader.isSelfSigned(), equalTo(false))
        assertThat(certificateReader.isCa(), equalTo(false))
    }

    @Test
    fun `given a simple self signed certificate, certificateReader sets certificate fields correctly`() {
        val certificateReader = CertificateReader(CertificateStringConstants.SIMPLE_SELF_SIGNED_TEST_CERT)

        assertThat(
                certificateReader.getSubjectName().toString(),
                equalTo(
                "CN=test.example.com, OU=app:b67446e5-b2b0-4648-a0d0-772d3d399dcb, L=exampletown"
                )
        )
        assertThat(certificateReader.getKeyLength(), equalTo(2048))
        assertThat<GeneralNames>(certificateReader.getAlternativeNames(), equalTo<GeneralNames>(null))
        assertThat<ExtendedKeyUsage>(certificateReader.getExtendedKeyUsage(), equalTo<ExtendedKeyUsage>(null))
        assertThat<KeyUsage>(certificateReader.getKeyUsage(), equalTo<KeyUsage>(null))
        assertThat(certificateReader.getDurationDays(), equalTo(3650))
        assertThat(certificateReader.isSelfSigned(), equalTo(true))
        assertThat(certificateReader.isCa(), equalTo(false))
    }

    @Test
    fun `given a deceptive and not self signed certificate, certificateReader sets certificate fields correctly`() {
        val certificateReader = CertificateReader(CertificateStringConstants.MISLEADING_CERT)

        assertThat(certificateReader.getSubjectName().toString(), equalTo("CN=trickster"))
        assertThat(certificateReader.getKeyLength(), equalTo(2048))
        assertThat<GeneralNames>(certificateReader.getAlternativeNames(), equalTo<GeneralNames>(null))
        assertThat<ExtendedKeyUsage>(certificateReader.getExtendedKeyUsage(), equalTo<ExtendedKeyUsage>(null))
        assertThat<KeyUsage>(certificateReader.getKeyUsage(), equalTo<KeyUsage>(null))
        assertThat(certificateReader.getDurationDays(), equalTo(365))
        assertThat(certificateReader.isSelfSigned(), equalTo(false))
        assertThat(certificateReader.isCa(), equalTo(false))
    }

    @Test
    fun `given a certificate authority, certificateReader sets certificate fields correctly`() {
        val certificateReader = CertificateReader(CertificateStringConstants.SELF_SIGNED_CA_CERT)

        assertThat(certificateReader.getSubjectName().toString(), equalTo("CN=foo.com"))
        assertThat(certificateReader.getKeyLength(), equalTo(2048))
        assertThat<GeneralNames>(certificateReader.getAlternativeNames(), equalTo<GeneralNames>(null))
        assertThat<ExtendedKeyUsage>(certificateReader.getExtendedKeyUsage(), equalTo<ExtendedKeyUsage>(null))
        assertThat<KeyUsage>(certificateReader.getKeyUsage(), equalTo<KeyUsage>(null))
        assertThat(certificateReader.getDurationDays(), equalTo(365))
        assertThat(certificateReader.isSelfSigned(), equalTo(true))
        assertThat(certificateReader.isCa(), equalTo(true))
    }

    @Test
    fun `certificateReader returns parameters correctly`() {
        val distinguishedName = "O=test-org, ST=Jupiter, C=MilkyWay, CN=test-common-name, OU=test-org-unit, L=Europa"
        val generalNames = GeneralNames(
                GeneralName(GeneralName.dNSName, "SolarSystem")
        )

        val certificateReader = CertificateReader(CertificateStringConstants.BIG_TEST_CERT)

        assertThat<GeneralNames>(certificateReader.getAlternativeNames(), equalTo(generalNames))
        assertThat(
                asList(*certificateReader.getExtendedKeyUsage()!!.usages),
                containsInAnyOrder(KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth)
        )
        assertThat(
                certificateReader.getKeyUsage()!!.hasUsages(KeyUsage.digitalSignature),
                equalTo(true)
        )
        assertThat(certificateReader.getSubjectName().toString(), equalTo(distinguishedName))
    }
}
