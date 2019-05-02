package org.cloudfoundry.credhub.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class RsaGenerateRequest extends BaseCredentialGenerateRequest {
  @JsonProperty("parameters")
  private RsaGenerationParameters generationParameters;

  @Override
  @JsonIgnore
  public GenerationParameters getGenerationParameters() {
    if (generationParameters == null) {
      generationParameters = new RsaGenerationParameters();
    }
    return generationParameters;
  }

  public void setGenerationParameters(final RsaGenerationParameters generationParameters) {
    this.generationParameters = generationParameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RsaGenerateRequest that = (RsaGenerateRequest) o;
    return Objects.equals(generationParameters, that.generationParameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(generationParameters);
  }
}
