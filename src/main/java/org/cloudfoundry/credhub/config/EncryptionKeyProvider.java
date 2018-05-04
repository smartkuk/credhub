package org.cloudfoundry.credhub.config;

import java.util.List;

@SuppressWarnings("unused")
public class EncryptionKeyProvider {
  private String providerName, host;
  private Integer port;
  private ProviderType providerType;
  private List<EncryptionKeyMetadata> keys;

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public ProviderType getProviderType() {
    return providerType;
  }

  public void setProviderType(ProviderType providerType) {
    this.providerType = providerType;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }


  public List<EncryptionKeyMetadata> getKeys() {
    return keys;
  }

  public void setKeys(List<EncryptionKeyMetadata> keys) {
    this.keys = keys;
  }

  @Override
  public String toString() {
    return "EncryptionKeyProvider{" +
        "providerName='" + providerName + '\'' +
        ", host='" + host + '\'' +
        ", port=" + port +
        ", providerType=" + providerType +
        ", keys=" + keys +
        '}';
  }
}
