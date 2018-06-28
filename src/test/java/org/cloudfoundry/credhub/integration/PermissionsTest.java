package org.cloudfoundry.credhub.integration;

import org.cloudfoundry.credhub.CredentialManagerApp;
import org.cloudfoundry.credhub.request.PermissionOperation;
import org.cloudfoundry.credhub.service.permissions.PermissionCheckingService;
import org.cloudfoundry.credhub.util.DatabaseProfileResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CredentialManagerApp.class)
@ActiveProfiles(value = {"unit-test", "unit-test-permissions"}, resolver = DatabaseProfileResolver.class)
@Transactional
public class PermissionsTest {
  @Autowired
  private PermissionCheckingService subject;

  private static final String USERA = "uaa-user:user-a";
  private static final String USERB = "uaa-user:user-b";
  private static final String USERC = "uaa-user:user-c";
  private static final String PATH = "/my/credential";

  @Test
  public void testPermissionsWithoutWildcard(){
    assertThat(subject.hasPermission(USERA, PATH, PermissionOperation.READ), is(true));
    assertThat(subject.hasPermission(USERB, PATH, PermissionOperation.READ), is(false));
  }

  @Test
  public void testPermissionsWithWildcard(){
    assertThat(subject.hasPermission(USERB, PATH, PermissionOperation.READ), is(true));
  }

  @Test
  public void testUnauthorizedPermissions(){
    assertThat(subject.hasPermission(USERB, PATH, PermissionOperation.READ), is(false));
  }
}
