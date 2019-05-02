package org.cloudfoundry.credhub.handlers;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.credhub.ErrorMessages;
import org.cloudfoundry.credhub.PermissionOperation;
import org.cloudfoundry.credhub.TestHelper;
import org.cloudfoundry.credhub.audit.CEFAuditRecord;
import org.cloudfoundry.credhub.auth.UserContext;
import org.cloudfoundry.credhub.auth.UserContextHolder;
import org.cloudfoundry.credhub.credential.CertificateCredentialValue;
import org.cloudfoundry.credhub.credential.CredentialValue;
import org.cloudfoundry.credhub.credential.StringCredentialValue;
import org.cloudfoundry.credhub.credential.UserCredentialValue;
import org.cloudfoundry.credhub.credentials.DefaultCredentialsHandler;
import org.cloudfoundry.credhub.domain.CredentialVersion;
import org.cloudfoundry.credhub.domain.Encryptor;
import org.cloudfoundry.credhub.domain.PasswordCredentialVersion;
import org.cloudfoundry.credhub.domain.SshCredentialVersion;
import org.cloudfoundry.credhub.entity.Credential;
import org.cloudfoundry.credhub.exceptions.EntryNotFoundException;
import org.cloudfoundry.credhub.generate.UniversalCredentialGenerator;
import org.cloudfoundry.credhub.requests.CertificateSetRequest;
import org.cloudfoundry.credhub.requests.PasswordGenerateRequest;
import org.cloudfoundry.credhub.requests.PasswordSetRequest;
import org.cloudfoundry.credhub.requests.StringGenerationParameters;
import org.cloudfoundry.credhub.requests.UserSetRequest;
import org.cloudfoundry.credhub.services.CertificateAuthorityService;
import org.cloudfoundry.credhub.services.DefaultCredentialService;
import org.cloudfoundry.credhub.services.PermissionCheckingService;
import org.cloudfoundry.credhub.utils.TestConstants;
import org.cloudfoundry.credhub.views.CredentialView;
import org.cloudfoundry.credhub.views.DataResponse;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.VerificationModeFactory;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Java6Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class DefaultCredentialsHandlerTest {
  private static final String CREDENTIAL_NAME = "/test/credential";
  private static final Instant VERSION1_CREATED_AT = Instant.ofEpochMilli(555555555);
  private static final Instant VERSION2_CREATED_AT = Instant.ofEpochMilli(777777777);
  private static final String UUID_STRING = UUID.randomUUID().toString();
  private static final String USER = "darth-sirius";

  private DefaultCredentialsHandler subject;
  private DefaultCredentialService credentialService;
  private CEFAuditRecord auditRecord;
  private PermissionCheckingService permissionCheckingService;
  private CertificateAuthorityService certificateAuthorityService;

  private SshCredentialVersion version1;
  private SshCredentialVersion version2;
  private StringGenerationParameters generationParameters;
  private CredentialVersion credentialVersion;
  private UniversalCredentialGenerator universalCredentialGenerator;

  @Before
  public void beforeEach() {
    TestHelper.getBouncyCastleFipsProvider();
    final Encryptor encryptor = mock(Encryptor.class);

    credentialService = mock(DefaultCredentialService.class);
    auditRecord = new CEFAuditRecord();
    permissionCheckingService = mock(PermissionCheckingService.class);
    UserContextHolder userContextHolder = mock(UserContextHolder.class);
    certificateAuthorityService = mock(CertificateAuthorityService.class);
    universalCredentialGenerator = mock(UniversalCredentialGenerator.class);

    subject = new DefaultCredentialsHandler(
      credentialService,
      auditRecord,
      permissionCheckingService,
      userContextHolder,
      certificateAuthorityService,
      universalCredentialGenerator);


    generationParameters = new StringGenerationParameters();
    UserContext userContext = mock(UserContext.class);
    when(userContext.getActor()).thenReturn(USER);
    when(userContextHolder.getUserContext()).thenReturn(userContext);

    version1 = new SshCredentialVersion(CREDENTIAL_NAME);
    version1.setVersionCreatedAt(VERSION1_CREATED_AT);
    version1.setEncryptor(encryptor);

    version2 = new SshCredentialVersion(CREDENTIAL_NAME);
    version2.setVersionCreatedAt(VERSION2_CREATED_AT);
    version2.setEncryptor(encryptor);

    final Credential cred = new Credential("federation");
    cred.setUuid(UUID.fromString(UUID_STRING));

    credentialVersion = mock(PasswordCredentialVersion.class);
    when(credentialVersion.getCredential()).thenReturn(cred);
    when(credentialVersion.getName()).thenReturn(cred.getName());
    when(credentialVersion.getUuid()).thenReturn(cred.getUuid());
    when(credentialService.save(any(), any(), any())).thenReturn(credentialVersion);

  }

  @Test
  public void deleteCredential_whenTheDeletionSucceeds_deletesTheCredential() {
    when(credentialService.delete(eq(CREDENTIAL_NAME))).thenReturn(true);
    when(permissionCheckingService.hasPermission(USER, CREDENTIAL_NAME, PermissionOperation.DELETE))
      .thenReturn(true);

    subject.deleteCredential(CREDENTIAL_NAME);

    verify(credentialService, times(1)).delete(eq(CREDENTIAL_NAME));
  }

  @Test
  public void deleteCredential_whenTheCredentialIsNotDeleted_throwsAnException() {
    when(permissionCheckingService.hasPermission(USER, CREDENTIAL_NAME, PermissionOperation.DELETE))
      .thenReturn(true);
    when(credentialService.delete(eq(CREDENTIAL_NAME))).thenReturn(false);

    try {
      subject.deleteCredential(CREDENTIAL_NAME);
      fail("Should throw exception");
    } catch (final EntryNotFoundException e) {
      assertThat(e.getMessage(), equalTo(ErrorMessages.Credential.INVALID_ACCESS));
    }
  }

  @Test
  public void getAllCredentialVersions_whenTheCredentialExists_returnsADataResponse() {
    final List<CredentialVersion> credentials = newArrayList(version1, version2);
    when(credentialService.findAllByName(eq(CREDENTIAL_NAME)))
      .thenReturn(credentials);
    when(permissionCheckingService.hasPermission(USER, CREDENTIAL_NAME, PermissionOperation.READ))
      .thenReturn(true);

    final DataResponse credentialVersions = subject.getAllCredentialVersions(CREDENTIAL_NAME);

    final List<CredentialView> credentialViews = credentialVersions.getData();
    assertThat(credentialViews, hasSize(2));
    assertThat(credentialViews.get(0).getName(), equalTo(CREDENTIAL_NAME));
    assertThat(credentialViews.get(0).getVersionCreatedAt(), equalTo(VERSION1_CREATED_AT));
    assertThat(credentialViews.get(1).getName(), equalTo(CREDENTIAL_NAME));
    assertThat(credentialViews.get(1).getVersionCreatedAt(), equalTo(VERSION2_CREATED_AT));
  }

  @Test
  public void getAllCredentialVersions_whenTheCredentialDoesNotExist_throwsException() {
    when(credentialService.findAllByName(eq(CREDENTIAL_NAME)))
      .thenReturn(emptyList());
    when(permissionCheckingService.hasPermission(USER, CREDENTIAL_NAME, PermissionOperation.READ))
      .thenReturn(true);

    try {
      subject.getAllCredentialVersions(CREDENTIAL_NAME
      );
      fail("should throw exception");
    } catch (final EntryNotFoundException e) {
      assertThat(e.getMessage(), equalTo(ErrorMessages.Credential.INVALID_ACCESS));
    }
  }

  @Test
  public void getMostRecentCredentialVersion_whenTheCredentialExists_returnsDataResponse() {
    when(credentialService.findActiveByName(eq(CREDENTIAL_NAME)))
      .thenReturn(Collections.singletonList(version1));
    when(permissionCheckingService.hasPermission(USER, CREDENTIAL_NAME, PermissionOperation.READ))
      .thenReturn(true);

    final DataResponse dataResponse = subject.getCurrentCredentialVersions(
      CREDENTIAL_NAME
    );
    final CredentialView credentialView = dataResponse.getData().get(0);
    assertThat(credentialView.getName(), equalTo(CREDENTIAL_NAME));
    assertThat(credentialView.getVersionCreatedAt(), equalTo(VERSION1_CREATED_AT));
  }

  @Test
  public void getMostRecentCredentialVersion_whenTheCredentialDoesNotExist_throwsException() {
    try {
      subject.getCurrentCredentialVersions(CREDENTIAL_NAME);
      fail("should throw exception");
    } catch (final EntryNotFoundException e) {
      assertThat(e.getMessage(), equalTo(ErrorMessages.Credential.INVALID_ACCESS));
    }
  }

  @Test
  public void getCurrentCredentialVersion_whenTheUserLacksPermission_throwsException() {
    when(permissionCheckingService.hasPermission(USER, CREDENTIAL_NAME, PermissionOperation.READ))
      .thenReturn(false);

    try {
      subject.getCurrentCredentialVersions(CREDENTIAL_NAME);
      fail("should throw exception");
    } catch (final EntryNotFoundException e) {
      assertThat(e.getMessage(), equalTo(ErrorMessages.Credential.INVALID_ACCESS));
    }
    verify(credentialService, times(0)).findActiveByName(any());

  }

  @Test
  public void getCredentialVersionByUUID_whenTheUserLacksPermission_throwsException() {
    when(permissionCheckingService.hasPermission(USER, UUID.fromString(UUID_STRING), PermissionOperation.READ))
      .thenReturn(false);

    try {
      subject.getCredentialVersionByUUID(UUID_STRING);
      fail("should throw exception");
    } catch (final EntryNotFoundException e) {
      assertThat(e.getMessage(), equalTo(ErrorMessages.Credential.INVALID_ACCESS));
    }
    verify(credentialService, times(0)).findVersionByUuid(any());

  }

  @Test
  public void getNCredentialVersions_whenTheUserLacksPermission_throwsException() {
    when(permissionCheckingService.hasPermission(USER, CREDENTIAL_NAME, PermissionOperation.READ))
      .thenReturn(false);

    try {
      subject.getNCredentialVersions(CREDENTIAL_NAME, null);
      fail("should throw exception");
    } catch (final EntryNotFoundException e) {
      assertThat(e.getMessage(), equalTo(ErrorMessages.Credential.INVALID_ACCESS));
    }
    verify(credentialService, times(0)).findAllByName(any());

  }

  @Test
  public void deleteCredential_whenTheUserLacksPermission_throwsException() {
    when(permissionCheckingService.hasPermission(USER, CREDENTIAL_NAME, PermissionOperation.DELETE))
      .thenReturn(false);
    when(credentialService.delete(CREDENTIAL_NAME))
      .thenReturn(true);

    try {
      subject.deleteCredential(CREDENTIAL_NAME);
      fail("should throw exception");
    } catch (final EntryNotFoundException e) {
      assertThat(e.getMessage(), equalTo(ErrorMessages.Credential.INVALID_ACCESS));
    }
    verify(credentialService, times(0)).delete(any());
  }

  @Test
  public void getAllCredentialVersion_whenTheUserLacksPermission_throwsException() {
    when(permissionCheckingService.hasPermission(USER, CREDENTIAL_NAME, PermissionOperation.READ))
      .thenReturn(false);

    try {
      subject.getAllCredentialVersions(CREDENTIAL_NAME);
      fail("should throw exception");
    } catch (final EntryNotFoundException e) {
      assertThat(e.getMessage(), equalTo(ErrorMessages.Credential.INVALID_ACCESS));
    }
    verify(credentialService, times(0)).findAllByName(any());
  }

  @Test
  public void getCredentialVersion_whenTheVersionExists_returnsDataResponse() {
    when(credentialService.findVersionByUuid(eq(UUID_STRING)))
      .thenReturn(version1);
    when(permissionCheckingService.hasPermission(USER, UUID.fromString(UUID_STRING), PermissionOperation.READ))
      .thenReturn(true);

    final CredentialView credentialVersion = subject.getCredentialVersionByUUID(UUID_STRING);
    assertThat(credentialVersion.getName(), equalTo(CREDENTIAL_NAME));
    assertThat(credentialVersion.getVersionCreatedAt(), equalTo(VERSION1_CREATED_AT));
  }

  @Test
  public void getNCredentialVersions_whenTheCredentialExists_addsToAuditRecord() {
    final List<CredentialVersion> credentials = newArrayList(version1, version2);
    when(credentialService.findNByName(eq(CREDENTIAL_NAME), eq(2)))
      .thenReturn(credentials);
    when(permissionCheckingService.hasPermission(USER, CREDENTIAL_NAME, PermissionOperation.READ))
      .thenReturn(true);

    subject.getNCredentialVersions(CREDENTIAL_NAME, 2);

    verify(auditRecord, times(2)).addVersion(any(CredentialVersion.class));
    verify(auditRecord, times(2)).addResource(any(Credential.class));
  }

  @Test
  public void handleSetRequest_AddsTheCredentialNameToTheAuditRecord() {
    final StringCredentialValue password = new StringCredentialValue("federation");
    final PasswordSetRequest setRequest = new PasswordSetRequest();

    setRequest.setType("password");
    setRequest.setGenerationParameters(generationParameters);
    setRequest.setPassword(password);
    setRequest.setName(CREDENTIAL_NAME);

    when(permissionCheckingService.hasPermission(USER, setRequest.getName(), PermissionOperation.WRITE))
      .thenReturn(true);

    subject.setCredential(setRequest);


    verify(credentialService).save(null, password, setRequest);
    assertThat(auditRecord.getResourceName(), Matchers.equalTo("federation"));
    assertThat(auditRecord.getResourceUUID(), Matchers.equalTo(UUID_STRING));
    assertThat(auditRecord.getVersionUUID(), Matchers.equalTo(credentialVersion.getUuid().toString()));
  }

  @Test
  public void handleSetRequest_whenPasswordSetRequest_passesCorrectParametersIncludingGeneration() {
    final StringCredentialValue password = new StringCredentialValue("federation");
    final PasswordSetRequest setRequest = new PasswordSetRequest();

    setRequest.setType("password");
    setRequest.setGenerationParameters(generationParameters);
    setRequest.setPassword(password);
    setRequest.setName(CREDENTIAL_NAME);

    when(permissionCheckingService.hasPermission(USER, setRequest.getName(), PermissionOperation.WRITE))
      .thenReturn(true);

    subject.setCredential(setRequest);

    verify(credentialService).save(null, password, setRequest);
  }

  @Test
  public void handleSetRequest_whenNonPasswordSetRequest_passesCorrectParametersWithNullGeneration() {
    final UserSetRequest setRequest = new UserSetRequest();
    final UserCredentialValue userCredentialValue = new UserCredentialValue(
      "Picard",
      "Enterprise",
      "salt");

    setRequest.setType("user");
    setRequest.setName(CREDENTIAL_NAME);
    setRequest.setUserValue(userCredentialValue);

    when(permissionCheckingService.hasPermission(USER, setRequest.getName(), PermissionOperation.WRITE))
      .thenReturn(true);

    subject.setCredential(setRequest);

    verify(credentialService).save(null, userCredentialValue, setRequest);
  }

  @Test
  public void handleSetRequest_withACertificateSetRequest_andNoCaName_usesCorrectParameters() {
    final CertificateSetRequest setRequest = new CertificateSetRequest();
    final CertificateCredentialValue certificateValue = new CertificateCredentialValue(
      null,
      "Picard",
      "Enterprise",
      null);

    setRequest.setType("certificate");
    setRequest.setName(CREDENTIAL_NAME);
    setRequest.setCertificateValue(certificateValue);

    when(permissionCheckingService.hasPermission(USER, setRequest.getName(), PermissionOperation.WRITE))
      .thenReturn(true);

    subject.setCredential(setRequest);

    verify(credentialService).save(null, certificateValue, setRequest);
  }

  @Test
  public void handleSetRequest_withACertificateSetRequest_andACaName_providesCaCertificate() {
    final CertificateCredentialValue cerificateAuthority = new CertificateCredentialValue(
      null,
      TestConstants.TEST_CA,
      null,
      null
    );
    when(permissionCheckingService.hasPermission(USER, "/test-ca-name", PermissionOperation.READ))
      .thenReturn(true);

    when(certificateAuthorityService.findActiveVersion("/test-ca-name"))
      .thenReturn(cerificateAuthority);

    final CertificateSetRequest setRequest = new CertificateSetRequest();
    final CertificateCredentialValue credentialValue = new CertificateCredentialValue(
      null,
      TestConstants.TEST_CERTIFICATE,
      "Enterprise",
      "test-ca-name");

    setRequest.setType("certificate");
    setRequest.setName("/captain");
    setRequest.setCertificateValue(credentialValue);

    final CertificateCredentialValue expectedCredentialValue = new CertificateCredentialValue(
      TestConstants.TEST_CA,
      TestConstants.TEST_CERTIFICATE,
      "Enterprise",
      "/test-ca-name"
    );
    final ArgumentCaptor<CredentialValue> credentialValueArgumentCaptor = ArgumentCaptor.forClass(CredentialValue.class);

    when(permissionCheckingService.hasPermission(USER, setRequest.getName(), PermissionOperation.WRITE))
      .thenReturn(true);

    subject.setCredential(setRequest);

    verify(credentialService).save(eq(null), credentialValueArgumentCaptor.capture(), eq(setRequest));
    assertThat(credentialValueArgumentCaptor.getValue(), samePropertyValuesAs(expectedCredentialValue));
  }

  @Test
  public void handleGenerateRequest_whenPasswordGenerateRequest_passesCorrectParametersIncludingGeneration() {
    final PasswordGenerateRequest generateRequest = new PasswordGenerateRequest();

    generateRequest.setType("password");
    generateRequest.setGenerationParameters(generationParameters);
    generateRequest.setName("/captain");
    generateRequest.setOverwrite(false);

    when(permissionCheckingService.hasPermission(USER, generateRequest.getName(), PermissionOperation.WRITE))
      .thenReturn(true);

    subject.generateCredential(generateRequest);

    verify(credentialService).save(null, null, generateRequest);
  }

  @Test
  public void handleGenerateRequest_addsToCEFAuditRecord() {
    final PasswordGenerateRequest generateRequest = new PasswordGenerateRequest();

    generateRequest.setType("password");
    generateRequest.setGenerationParameters(generationParameters);
    generateRequest.setName("/captain");
    generateRequest.setOverwrite(false);

    when(permissionCheckingService.hasPermission(USER, generateRequest.getName(), PermissionOperation.WRITE))
      .thenReturn(true);

    subject.generateCredential(generateRequest);
    verify(auditRecord, times(1)).addVersion(any(CredentialVersion.class));
    verify(auditRecord, times(1)).addResource(any(Credential.class));
  }



}
