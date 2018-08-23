package org.cloudfoundry.credhub.repository;

import org.cloudfoundry.credhub.entity.CertificateCredentialVersionData;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.persistence.Table;
import java.util.List;
import java.util.UUID;

@Table(name = "certificate_credential")
public interface CertificateCredentialRepository extends JpaRepository<CertificateCredentialVersionData, UUID> {

  CertificateCredentialVersionData findOneByUuid(UUID uuid);
  List<CertificateCredentialVersionData> findAllByExpiryDate();
}