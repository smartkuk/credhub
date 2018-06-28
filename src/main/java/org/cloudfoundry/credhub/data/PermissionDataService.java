package org.cloudfoundry.credhub.data;

import org.cloudfoundry.credhub.audit.CEFAuditRecord;
import org.cloudfoundry.credhub.entity.PermissionData;
import org.cloudfoundry.credhub.entity.Credential;
import org.cloudfoundry.credhub.repository.PermissionRepository;
import org.cloudfoundry.credhub.request.PermissionEntry;
import org.cloudfoundry.credhub.request.PermissionOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

@Component
public class PermissionDataService {

  private PermissionRepository permissionRepository;
  private final CredentialDataService credentialDataService;
  private CEFAuditRecord auditRecord;

  @Autowired
  public PermissionDataService(
      PermissionRepository permissionRepository,
      CredentialDataService credentialDataService,
      CEFAuditRecord auditRecord
  ) {
    this.permissionRepository = permissionRepository;
    this.credentialDataService = credentialDataService;
    this.auditRecord = auditRecord;
  }

  public List<PermissionEntry> getPermissions(Credential credential) {
    return createViewsFromPermissionsFor(credential);
  }

  public void savePermissions(List<PermissionEntry> permissions) {
    for (PermissionEntry permission : permissions) {
      String path = permission.getPath();
      List<PermissionData> existingPermissions = permissionRepository.findAllByPath(path);
      upsertPermissions(path, existingPermissions, permission.getActor(),
          permission.getAllowedOperations());
    }
  }

  public List<PermissionOperation> getAllowedOperations(String name, String actor) {
    List<PermissionOperation> operations = newArrayList();
    PermissionData permissionData = permissionRepository.findByPathAndActor(name, actor);

    if (permissionData != null) {
      if (permissionData.hasReadPermission()) {
        operations.add(PermissionOperation.READ);
      }
      if (permissionData.hasWritePermission()) {
        operations.add(PermissionOperation.WRITE);
      }
      if (permissionData.hasDeletePermission()) {
        operations.add(PermissionOperation.DELETE);
      }
      if (permissionData.hasReadAclPermission()) {
        operations.add(PermissionOperation.READ_ACL);
      }
      if (permissionData.hasWriteAclPermission()) {
        operations.add(PermissionOperation.WRITE_ACL);
      }
    }

    return operations;
  }

  public boolean deletePermissions(String name, String actor) {
    Credential credential = credentialDataService.find(name);
    auditRecord.setResource(credential); // TODO auditRecord should take a permission ID or name
    return permissionRepository.deleteByPathAndActor(name, actor) > 0;
  }

  public boolean hasNoDefinedAccessControl(String name) {
    Credential credential = credentialDataService.find(name);
    if (credential == null) {
      return false;
    }
    return (permissionRepository.findAllByPath(name).size() == 0);
  }

  public boolean hasPermission(String user, String name, PermissionOperation requiredPermission) {
    final PermissionData permissionData =
        permissionRepository.findByPathAndActor(name, user);
    return permissionData != null && permissionData.hasPermission(requiredPermission);
  }

  private void upsertPermissions(String path,
                                 List<PermissionData> accessEntries, String actor, List<PermissionOperation> operations) {
    PermissionData entry = findAccessEntryForActor(accessEntries, actor);

    if (entry == null) {
      entry = new PermissionData(path, actor);
    }

    entry.enableOperations(operations);
    permissionRepository.saveAndFlush(entry);
  }

  private PermissionEntry createViewFor(PermissionData data) {
    if (data == null) {
      return null;
    }
    PermissionEntry entry = new PermissionEntry();
    List<PermissionOperation> operations = data.generateAccessControlOperations();
    entry.setAllowedOperations(operations);
//    entry.setPath(data.getPath());
    entry.setActor(data.getActor());
    return entry;
  }

  private List<PermissionEntry> createViewsFromPermissionsFor(Credential credential) {
    return permissionRepository.findAllByPath(credential.getName())
        .stream()
        .map(this::createViewFor)
        .collect(Collectors.toList());
  }

  private PermissionData findAccessEntryForActor(List<PermissionData> accessEntries,
                                                 String actor) {
    Optional<PermissionData> temp = accessEntries.stream()
        .filter(permissionData -> permissionData.getActor().equals(actor))
        .findFirst();
    return temp.orElse(null);
  }
}
