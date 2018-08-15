package org.cloudfoundry.credhub.service;

import org.cloudfoundry.credhub.entity.Credential;
import org.cloudfoundry.credhub.entity.PermissionData;
import org.cloudfoundry.credhub.request.PermissionEntry;
import org.cloudfoundry.credhub.request.PermissionOperation;
import org.cloudfoundry.credhub.request.PermissionsV2Request;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PermissionProvider {

  List<PermissionEntry> getPermissions(Credential credential);

  PermissionData getPermission(UUID guid);

  List<PermissionData> savePermissionsWithLogging(List<PermissionEntry> permissions);

  List<PermissionData> savePermissions(List<PermissionEntry> permissions);

  List<PermissionOperation> getAllowedOperations(String name, String actor);

  boolean deletePermissions(String name, String actor);

  Set<String> findAllPathsByActor(String actor);

  boolean hasPermission(String user, String path, PermissionOperation requiredPermission);

  PermissionData putPermissions(String guid, PermissionsV2Request permissionsRequest);

  PermissionData patchPermissions(String guid, List<PermissionOperation> operations);

  PermissionData saveV2Permissions(PermissionsV2Request permissionsRequest);

  PermissionData deletePermissions(UUID guid);

  // TODO can we remove these?

  boolean hasNoDefinedAccessControl(String name);

  boolean permissionExists(String user, String path);
}
