package org.cloudfoundry.credhub.credentials;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(JsonCredentialVersionData.CREDENTIAL_TYPE)
public class JsonCredentialVersionData extends CredentialVersionData<JsonCredentialVersionData> {

  public static final String CREDENTIAL_TYPE = "json";

  public JsonCredentialVersionData() {
    super();
  }

  public JsonCredentialVersionData(final String name) {
    super(name);
  }

  @Override
  public String getCredentialType() {
    return CREDENTIAL_TYPE;
  }
}
