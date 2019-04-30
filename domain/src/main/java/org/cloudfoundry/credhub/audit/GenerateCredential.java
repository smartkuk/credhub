package org.cloudfoundry.credhub.audit;

public class GenerateCredential extends SetCredential {
  @Override
  public OperationDeviceAction operation() {
    return OperationDeviceAction.GENERATE;
  }
}
