package org.cloudfoundry.credhub.entity;

import java.util.UUID;

public class KeyUsageCount {
  private Long count;
  private UUID uuid;

  public KeyUsageCount(Long count, UUID uuid) {
    this.count = count;
    this.uuid = uuid;
  }


  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }
}
