package org.cloudfoundry.credhub.requests;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudfoundry.credhub.ErrorMessages;
import org.cloudfoundry.credhub.credential.JsonCredentialValue;
import org.cloudfoundry.credhub.credentials.JsonCredentialValue;

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

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final JsonSetRequest that = (JsonSetRequest) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
