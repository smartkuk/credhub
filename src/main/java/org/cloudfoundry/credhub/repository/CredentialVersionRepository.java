package org.cloudfoundry.credhub.repository;

import org.cloudfoundry.credhub.entity.CredentialVersionData;
import org.cloudfoundry.credhub.entity.KeyUsageCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CredentialVersionRepository extends JpaRepository<CredentialVersionData, UUID> {

  int BATCH_SIZE = 50;

  CredentialVersionData findOneByUuid(UUID uuid);

  @Query(value = "select * from credential_version "
      + "left join certificate_credential on credential_version.uuid = certificate_credential.uuid "
      + "where credential_uuid = ?1 "
      + "and certificate_credential.transitional = false "
      + "order by version_created_at desc "
      + "limit 1", nativeQuery = true)
  CredentialVersionData findLatestNonTransitionalCertificateVersion(UUID credentialUUID);

  @Query(value = "select * from credential_version "
      + "left join certificate_credential on credential_version.uuid = certificate_credential.uuid "
      + "where credential_uuid = ?1 "
      + "and certificate_credential.transitional = true "
      + "order by version_created_at desc "
      + "limit 1", nativeQuery = true)
  CredentialVersionData findTransitionalCertificateVersion(UUID credentialUUID);

  Long countByEncryptedCredentialValueEncryptionKeyUuidNot(UUID encryptionKeyUuid);

  Long countByEncryptedCredentialValueEncryptionKeyUuidIn(Collection<UUID> encryptionKeyUuids);

  List<CredentialVersionData> findByEncryptedCredentialValueEncryptionKeyUuidIn(List<UUID> encryptionKeyUuids);

  List<CredentialVersionData> findAllByCredentialUuidOrderByVersionCreatedAtDesc(UUID uuid);

  List<CredentialVersionData> findAllByCredentialUuidAndTypeOrderByVersionCreatedAtDesc(UUID uuid, String credentialType);

  CredentialVersionData findFirstByCredentialUuidOrderByVersionCreatedAtDesc(UUID uuid);

  @Query(value = "SELECT new org.cloudfoundry.credhub.entity.KeyUsageCount(count(*), encrypted_value.encryption_key_uuid)"
      + " FROM credential_version " +
      "LEFT JOIN encrypted_value ON credential_version.encrypted_value_uuid = encrypted_value.uuid " +
      "GROUP BY encrypted_value.encryption_key_uuid", nativeQuery = true)
  List<KeyUsageCount> countKeyUsage(); // TODO or do LIST<Object>
}

// see https://stackoverflow.com/questions/36328063/how-to-return-a-custom-object-from-a-spring-data-jpa-group-by-query
