package org.cloudfoundry.credhub.services;

import org.cloudfoundry.credhub.ErrorMessages;
import org.cloudfoundry.credhub.auth.UserContext;
import org.cloudfoundry.credhub.auth.UserContextHolder;
import org.cloudfoundry.credhub.credential.CertificateCredentialValue;
import org.cloudfoundry.credhub.domain.CertificateCredentialVersion;
import org.cloudfoundry.credhub.domain.PasswordCredentialVersion;
import org.cloudfoundry.credhub.exceptions.EntryNotFoundException;
import org.cloudfoundry.credhub.exceptions.ParameterizedValidationException;
import org.cloudfoundry.credhub.utils.CertificateReader;
import org.cloudfoundry.credhub.utils.CertificateStringConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class CertificateAuthorityServiceTest {

  private static final String CREDENTIAL_NAME = "/expectedCredential";
  private static final String USER_NAME = "expectedUser";
  private CertificateAuthorityService certificateAuthorityService;
  private DefaultCertificateVersionDataService certificateVersionDataService;
  private CertificateCredentialValue certificate;
  private CertificateCredentialVersion certificateCredential;
  private UserContext userContext;

  @Before
  public void beforeEach() {
    certificate = new CertificateCredentialValue(null, CertificateStringConstants.SELF_SIGNED_CA_CERT, "my-key", null);
    certificateCredential = mock(CertificateCredentialVersion.class);

    userContext = mock(UserContext.class);
    when(userContext.getActor()).thenReturn(USER_NAME);
    when(certificateCredential.getName()).thenReturn(CREDENTIAL_NAME);

    certificateVersionDataService = mock(DefaultCertificateVersionDataService.class);
    final UserContextHolder userContextHolder = new UserContextHolder();
    userContextHolder.setUserContext(userContext);
    certificateAuthorityService = new CertificateAuthorityService(certificateVersionDataService);
  }

  @Test
  public void findActiveVersion_whenACaDoesNotExist_throwsException() {
    when(certificateVersionDataService.findActive(any(String.class))).thenReturn(null);

    try {
      certificateAuthorityService.findActiveVersion("any ca name");
    } catch (final EntryNotFoundException pe) {
      assertThat(pe.getMessage(), equalTo(ErrorMessages.Credential.INVALID_ACCESS));
    }
  }

  @Test
  public void findActiveVersion_whenCaNameRefersToNonCa_throwsException() {
    when(certificateVersionDataService.findActive(any(String.class))).thenReturn(mock(PasswordCredentialVersion.class));

    try {
      certificateAuthorityService.findActiveVersion("any non-ca name");
    } catch (final ParameterizedValidationException pe) {
      assertThat(pe.getMessage(), equalTo(ErrorMessages.NOT_A_CA_NAME));
    }
  }

  @Test
  public void findActiveVersion_givenExistingCa_returnsTheCa() {
    final CertificateReader certificateReader = mock(CertificateReader.class);
    when(certificateVersionDataService.findActive(CREDENTIAL_NAME)).thenReturn(certificateCredential);
    when(certificateCredential.getPrivateKey()).thenReturn("my-key");
    when(certificateCredential.getParsedCertificate()).thenReturn(certificateReader);
    when(certificateReader.isCa()).thenReturn(true);
    when(certificateCredential.getCertificate()).thenReturn(CertificateStringConstants.SELF_SIGNED_CA_CERT);

    assertThat(certificateAuthorityService.findActiveVersion(CREDENTIAL_NAME),
      samePropertyValuesAs(certificate));
  }

  @Test
  public void findActiveVersion_whenCredentialIsNotACa_throwsException() {
    when(certificateVersionDataService.findActive("actually-a-password"))
      .thenReturn(new PasswordCredentialVersion());

    try {
      certificateAuthorityService.findActiveVersion("actually-a-password");
    } catch (final ParameterizedValidationException pe) {
      assertThat(pe.getMessage(), equalTo(ErrorMessages.NOT_A_CA_NAME));
    } catch (final Exception e) {
      fail("expected EntryNotFoundException, but got " + e.getClass() );
    }
  }

  @Test
  public void findActiveVersion_whenCertificateIsNotACa_throwsException() {
    final CertificateCredentialVersion notACertificateAuthority = mock(CertificateCredentialVersion.class);
    when(notACertificateAuthority.getParsedCertificate()).thenReturn(mock(CertificateReader.class));
    when(notACertificateAuthority.getCertificate()).thenReturn(CertificateStringConstants.SIMPLE_SELF_SIGNED_TEST_CERT);
    when(certificateVersionDataService.findActive(CREDENTIAL_NAME))
      .thenReturn(notACertificateAuthority);

    try {
      certificateAuthorityService.findActiveVersion(CREDENTIAL_NAME);
    } catch (final ParameterizedValidationException pe) {
      assertThat(pe.getMessage(), equalTo(ErrorMessages.CERT_NOT_CA));
    }
  }
}
