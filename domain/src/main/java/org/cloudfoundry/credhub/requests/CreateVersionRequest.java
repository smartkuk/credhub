package org.cloudfoundry.credhub.requests;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudfoundry.credhub.credentials.CertificateCredentialValue;
import org.cloudfoundry.credhub.exceptions.ErrorMessages;

public class CreateVersionRequest {

  @NotNull(message = ErrorMessages.MISSING_VALUE)
  @Valid
  @JsonProperty("value")
  private CertificateCredentialValue value;
  @JsonProperty("transitional")
  private boolean transitional;

  public CreateVersionRequest() {
    super();
    /* this needs to be there for jackson to be happy */
  }

  public CreateVersionRequest(final CertificateCredentialValue value, final boolean transitional) {
    super();
    this.value = value;
    this.transitional = transitional;
  }

  public boolean isTransitional() {
    return transitional;
  }

  public void setTransitional(final boolean transitional) {
    this.transitional = transitional;
  }

  public CertificateCredentialValue getValue() {
    return value;
  }

  public void setValue(final CertificateCredentialValue value) {
    this.value = value;
  }
}
