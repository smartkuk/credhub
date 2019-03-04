package org.cloudfoundry.credhub.credential;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class StringCredentialValue implements CredentialValue {

  @NotEmpty(message = "error.missing_value")
  private final String string;

  public StringCredentialValue(final String password) {
    super();
    this.string = password;
  }

  @JsonValue
  public String getStringCredential() {
    return string;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    StringCredentialValue that = (StringCredentialValue) o;

    return new EqualsBuilder()
      .append(string, that.string)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(string)
      .toHashCode();
  }
}
