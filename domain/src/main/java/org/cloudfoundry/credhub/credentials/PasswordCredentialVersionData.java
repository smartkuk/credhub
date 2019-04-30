package org.cloudfoundry.credhub.credentials;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;

import org.cloudfoundry.credhub.entities.EncryptedValue;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Entity
@DiscriminatorValue(PasswordCredentialVersionData.CREDENTIAL_TYPE)
@SecondaryTable(
  name = PasswordCredentialVersionData.TABLE_NAME,
  pkJoinColumns = @PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
)
public class PasswordCredentialVersionData extends CredentialVersionData<PasswordCredentialVersionData> {

  public static final String CREDENTIAL_TYPE = "password";
  public static final String TABLE_NAME = "password_credential";

  @OneToOne(cascade = CascadeType.ALL)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumn(table = PasswordCredentialVersionData.TABLE_NAME, name = "password_parameters_uuid")
  private EncryptedValue encryptedGenerationParameters;

  @SuppressWarnings("unused")
  public PasswordCredentialVersionData() {
    super();
  }

  public PasswordCredentialVersionData(final String name) {
    super(name);
  }

  public EncryptedValue getEncryptedGenerationParameters() {
    return encryptedGenerationParameters;
  }

  public void setEncryptedGenerationParameters(final EncryptedValue encryptedGenerationParameters) {
    this.encryptedGenerationParameters = encryptedGenerationParameters;
  }

  @Override
  public String getCredentialType() {
    return CREDENTIAL_TYPE;
  }
}
