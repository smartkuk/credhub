package org.cloudfoundry.credhub.credentials;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(ValueCredentialVersionData.CREDENTIAL_TYPE)
public class ValueCredentialVersionData extends CredentialVersionData<ValueCredentialVersionData> {

  public static final String CREDENTIAL_TYPE = "value";

  public ValueCredentialVersionData() {
    super();
  }

  public ValueCredentialVersionData(final String name) {
    super(name);
  }

  @Override
  public String getCredentialType() {
    return CREDENTIAL_TYPE;
  }
}
