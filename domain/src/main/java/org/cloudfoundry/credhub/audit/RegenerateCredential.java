package org.cloudfoundry.credhub.audit;

import org.cloudfoundry.credhub.audit.OperationDeviceAction;
import org.cloudfoundry.credhub.audit.RequestDetails;

public class RegenerateCredential implements RequestDetails {
  private String name;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public OperationDeviceAction operation() {
    return OperationDeviceAction.REGENERATE;
  }
}
