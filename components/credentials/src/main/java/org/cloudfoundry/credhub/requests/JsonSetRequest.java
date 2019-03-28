package org.cloudfoundry.credhub.requests;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudfoundry.credhub.ErrorMessages;
import org.cloudfoundry.credhub.credential.JsonCredentialValue;

public class JsonSetRequest extends BaseCredentialSetRequest<JsonCredentialValue> {

  @NotNull(message = ErrorMessages.MISSING_VALUE)
  @Valid
  @JsonProperty("value")
  private JsonCredentialValue value;

  public JsonCredentialValue getValue() {
    return value;
  }

  public void setValue(final JsonCredentialValue value) {
    this.value = value;
  }

  @Override
  public JsonCredentialValue getCredentialValue() {
    return value;
  }

  @Override
  public GenerationParameters getGenerationParameters() {
    return null;
  }
}
