package org.cloudfoundry.credhub.requests;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PasswordGenerateRequest extends BaseCredentialGenerateRequest {
  @JsonProperty("parameters")
  private StringGenerationParameters generationParameters;

  @Override
  @JsonIgnore
  public GenerationParameters getGenerationParameters() {
    if (generationParameters == null) {
      generationParameters = new StringGenerationParameters();
    }
    return generationParameters;
  }

  public void setGenerationParameters(final StringGenerationParameters generationParameters) {
    this.generationParameters = generationParameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PasswordGenerateRequest that = (PasswordGenerateRequest) o;
    return Objects.equals(generationParameters, that.generationParameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(generationParameters);
  }
}
