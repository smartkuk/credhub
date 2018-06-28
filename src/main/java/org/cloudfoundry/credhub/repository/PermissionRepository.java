package org.cloudfoundry.credhub.repository;


import org.cloudfoundry.credhub.entity.PermissionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<PermissionData, UUID> {
  List<PermissionData> findAllByPath(String path);

  List<PermissionData> findByActor(String actor);

  PermissionData findByPathAndActor(String path, String actor);

  @Transactional
  long deleteByPathAndActor(String path, String actor);
}
