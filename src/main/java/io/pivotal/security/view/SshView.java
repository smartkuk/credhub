package io.pivotal.security.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.pivotal.security.entity.NamedSshSecret;

import java.time.Instant;
import java.util.UUID;

public class SshView extends SecretView {
  @JsonProperty("value")
  private SshBody sshBody;

  public SshView(Instant versionCreatedAt, UUID uuid, String name, String publicKey, String privateKey) {
    super(versionCreatedAt, uuid, name);
    setSshBody(new SshBody(publicKey, privateKey));
  }

  public SshView(NamedSshSecret namedSshSecret) {
    this(
        namedSshSecret.getVersionCreatedAt(),
        namedSshSecret.getUuid(),
        namedSshSecret.getName(),
        namedSshSecret.getPublicKey(),
        namedSshSecret.getPrivateKey()
    );
  }

  @Override
  public String getType() {
    return NamedSshSecret.SECRET_TYPE;
  }

  public SshBody getSshBody() {
    return sshBody;
  }

  public SshView setSshBody(SshBody sshBody) {
    this.sshBody = sshBody;
    return this;
  }
}