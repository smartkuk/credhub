package org.cloudfoundry.credhub.credentials;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.cloudfoundry.credhub.constants.UuidConstants;
import org.cloudfoundry.credhub.entities.EncryptedValue;
import org.cloudfoundry.credhub.util.InstantMillisecondsConverter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Entity
@Table(name = "credential_version")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@EntityListeners(AuditingEntityListener.class)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class CredentialVersionData<Z extends CredentialVersionData> {

  // Use VARBINARY to make all 3 DB types happy.
  // H2 doesn't distinguish between "binary" and "varbinary" - see
  // https://hibernate.atlassian.net/browse/HHH-9835 and
  // https://github.com/h2database/h2database/issues/345
  @Id
  @Column(length = UuidConstants.UUID_BYTES, columnDefinition = "VARBINARY")
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  private UUID uuid;

  @OneToOne(cascade = CascadeType.ALL)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumn(name = "encrypted_value_uuid")
  private EncryptedValue encryptedCredentialValue;

  @Convert(converter = InstantMillisecondsConverter.class)
  @Column(nullable = false, columnDefinition = "BIGINT NOT NULL")
  @CreatedDate
  private Instant versionCreatedAt;

  @ManyToOne
  @JoinColumn(name = "credential_uuid", nullable = false)
  private Credential credential;

  //this is mapped with updatable and insertable false since it's managed by the DiscriminatorColumn annotation
  //surfacing property here lets us use it in JPA queries
  @Column(name = "type", insertable = false, updatable = false)
  @SuppressWarnings("unused")
  private String type;

  public CredentialVersionData(final Credential name) {
    super();
    if (this.credential != null) {
      this.credential.setName(name.getName());
    } else {
      setCredential(name);
    }
  }

  public CredentialVersionData(final String name) {
    this(new Credential(name));
  }

  public CredentialVersionData() {
    this((String) null);
  }

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(final UUID uuid) {
    this.uuid = uuid;
  }

  public Credential getCredential() {
    return credential;
  }

  public void setCredential(final Credential credential) {
    this.credential = credential;
  }

  public EncryptedValue getEncryptedValueData() {
    return encryptedCredentialValue;
  }

  public void setEncryptedValueData(final EncryptedValue encryptedValue) {
    encryptedCredentialValue = encryptedValue;
  }

  public byte[] getNonce() {
    return encryptedCredentialValue != null ? this.encryptedCredentialValue.getNonce() : null;
  }

  public abstract String getCredentialType();

  public UUID getEncryptionKeyUuid() {
    return encryptedCredentialValue != null ? encryptedCredentialValue.getEncryptionKeyUuid() : null;
  }

  public Instant getVersionCreatedAt() {
    return versionCreatedAt;
  }

  public void setVersionCreatedAt(final Instant versionCreatedAt) {
    this.versionCreatedAt = versionCreatedAt;
  }
}
