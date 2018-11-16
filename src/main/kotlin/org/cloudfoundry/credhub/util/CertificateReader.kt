package org.cloudfoundry.credhub.util

import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.cloudfoundry.credhub.exceptions.MalformedCertificateException
import org.cloudfoundry.credhub.exceptions.UnreadableCertificateException
import org.cloudfoundry.credhub.util.StringUtil.UTF_8
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.StringReader
import java.lang.Math.toIntExact
import java.security.InvalidKeyException
import java.security.SignatureException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import javax.security.auth.x500.X500Principal

class CertificateReader {
    val certificate: X509Certificate
    private val certificateHolder: X509CertificateHolder

    constructor(pemString: String) {
        try {
            certificate = parseStringIntoCertificate(pemString)
            certificateHolder = PEMParser(StringReader(pemString))
                    .readObject() as X509CertificateHolder
        } catch (e: IOException) {
            throw UnreadableCertificateException()
        } catch (e: CertificateException) {
            throw MalformedCertificateException()
        }
    }

    private fun parseStringIntoCertificate(pemString: String): X509Certificate {
        return try {
            CertificateFactory
                    .getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME)
                    .generateCertificate(ByteArrayInputStream(pemString.toByteArray(UTF_8))) as X509Certificate
        } catch (e: Exception) {
            throw MalformedCertificateException()
        }
    }

    fun getAlternativeNames(): GeneralNames? {
        val encodedAlternativeNames = certificateHolder.getExtension(Extension.subjectAlternativeName)
        return when (encodedAlternativeNames) {
            null -> null
            else -> GeneralNames.getInstance(encodedAlternativeNames.parsedValue)
        }
    }

    fun getDurationDays(): Int {
        return toIntExact(DAYS.between(
                certificate.notBefore.toInstant(),
                certificate.notAfter.toInstant()
        ))
    }

    fun getExtendedKeyUsage(): ExtendedKeyUsage? {
        return ExtendedKeyUsage.fromExtensions(certificateHolder.extensions)
    }

    fun getSubjectName(): X500Principal {
        return X500Principal(certificate.subjectDN.name)
    }

    fun isSignedByCa(caValue: String): Boolean {
        try {
            val ca = parseStringIntoCertificate(caValue)
            certificate.verify(ca.publicKey)
            return true
        } catch (e: SignatureException) {
            return false
        } catch (e: InvalidKeyException) {
            return false
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun getKeyLength(): Int {
        return (certificate.publicKey as RSAPublicKey).modulus.bitLength()
    }

    fun getKeyUsage(): KeyUsage? {
        return KeyUsage.fromExtensions(certificateHolder.extensions)
    }

    fun isSelfSigned(): Boolean {
        val issuerName = certificate.issuerDN.name

        if (issuerName != certificate.subjectDN.toString()) {
            return false
        } else {
            try {
                certificate.verify(certificate.publicKey)
                return true
            } catch (e: SignatureException) {
                return false
            } catch (e: InvalidKeyException) {
                return false
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    fun isCa(): Boolean {
        val extensions = certificateHolder.extensions

        if (extensions != null) {
            val basicConstraints = BasicConstraints
                    .fromExtensions(Extensions.getInstance(extensions))

            return basicConstraints != null && basicConstraints.isCA
        }

        return false
    }

    fun getNotAfter(): Instant? {
        if (certificate.notAfter == null) {
            return null
        } else {
            return certificate.notAfter.toInstant()
        }
    }
}