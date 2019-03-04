package org.cloudfoundry.credhub.requests;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.cloudfoundry.credhub.credential.StringCredentialValue;

public class ValueSetRequest extends BaseCredentialSetRequest<StringCredentialValue> {

  @NotNull(message = "error.missing_value")
  @Valid
  @JsonProperty("value")
  private StringCredentialValue value;

  public StringCredentialValue getValue() {
    return value;
  }

  public void setValue(final StringCredentialValue value) {
    this.value = value;
  }

  @Override
  public StringCredentialValue getCredentialValue() {
    return value;
  }

  @Override
  public GenerationParameters getGenerationParameters() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ValueSetRequest that = (ValueSetRequest) o;

    return new EqualsBuilder()
      .appendSuper(super.equals(o))
      .append(value, that.value)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .appendSuper(super.hashCode())
      .append(value)
      .toHashCode();
  }
}
