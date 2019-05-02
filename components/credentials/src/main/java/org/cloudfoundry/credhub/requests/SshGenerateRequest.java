package org.cloudfoundry.credhub.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class SshGenerateRequest extends BaseCredentialGenerateRequest {
  @JsonProperty("parameters")
  private SshGenerationParameters generationParameters;

  @Override
  @JsonIgnore
  public GenerationParameters getGenerationParameters() {
    if (generationParameters == null) {
      generationParameters = new SshGenerationParameters();
    }
    return generationParameters;
  }

  public void setGenerationParameters(final SshGenerationParameters generationParameters) {
    this.generationParameters = generationParameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SshGenerateRequest that = (SshGenerateRequest) o;
    return Objects.equals(generationParameters, that.generationParameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(generationParameters);
  }
}
