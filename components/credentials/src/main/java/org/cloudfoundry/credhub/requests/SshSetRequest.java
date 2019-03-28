package org.cloudfoundry.credhub.requests;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudfoundry.credhub.ErrorMessages;
import org.cloudfoundry.credhub.credential.SshCredentialValue;

public class SshSetRequest extends BaseCredentialSetRequest<SshCredentialValue> {

  @NotNull(message = ErrorMessages.MISSING_VALUE)
  @Valid
  @JsonProperty("value")
  private SshCredentialValue sshKeyValue;

  public SshCredentialValue getSshKeyValue() {
    return sshKeyValue;
  }

  public void setSshKeyValue(final SshCredentialValue sshKeyValue) {
    this.sshKeyValue = sshKeyValue;
  }

  @Override
  public SshCredentialValue getCredentialValue() {
    return sshKeyValue;
  }

  @Override
  public GenerationParameters getGenerationParameters() {
    return null;
  }
}
