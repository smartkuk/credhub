package org.cloudfoundry.credhub.credentials;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.credhub.audit.CEFAuditRecord;
import org.cloudfoundry.credhub.audit.RequestDetails;
import org.cloudfoundry.credhub.audit.entities.DeleteCredential;
import org.cloudfoundry.credhub.audit.entities.FindCredential;
import org.cloudfoundry.credhub.audit.entities.GetCredential;
import org.cloudfoundry.credhub.audit.entities.SetCredential;
import org.cloudfoundry.credhub.exceptions.InvalidQueryParameterException;
import org.cloudfoundry.credhub.handlers.CredentialsHandler;
import org.cloudfoundry.credhub.handlers.LegacyGenerationHandler;
import org.cloudfoundry.credhub.handlers.SetHandler;
import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest;
import org.cloudfoundry.credhub.services.PermissionedCredentialService;
import org.cloudfoundry.credhub.views.CredentialView;
import org.cloudfoundry.credhub.views.DataResponse;
import org.cloudfoundry.credhub.views.FindCredentialResults;

@RestController
@RequestMapping(
  path = CredentialsController.ENDPOINT,
  produces = MediaType.APPLICATION_JSON_UTF8_VALUE
)
public class CredentialsController {

  public static final String ENDPOINT = "/api/v1/data";

  @Autowired
  private DiscoveryClient discoveryClient;

  private final PermissionedCredentialService permissionedCredentialService;
  private final SetHandler setHandler;
  private final CredentialsHandler credentialsHandler;
  private final LegacyGenerationHandler legacyGenerationHandler;
  private final CEFAuditRecord auditRecord;

  @Autowired
  public CredentialsController(
    final PermissionedCredentialService permissionedCredentialService,
    final CredentialsHandler credentialsHandler,
    final SetHandler setHandler,
    final LegacyGenerationHandler legacyGenerationHandler,
    final CEFAuditRecord auditRecord
  ) {
    super();
    this.permissionedCredentialService = permissionedCredentialService;
    this.credentialsHandler = credentialsHandler;
    this.setHandler = setHandler;
    this.legacyGenerationHandler = legacyGenerationHandler;
    this.auditRecord = auditRecord;
  }

  @RequestMapping("/service-instance-strings/{applicationName}")
  public List<String> serviceInstancesStrings(
    @PathVariable String applicationName) {
    List<String> rval = new ArrayList<>();
    List<ServiceInstance> credhubServiceInstances = discoveryClient.getInstances(applicationName);
    for(ServiceInstance serviceInstance : credhubServiceInstances) {
      rval.add(serviceInstance.getHost());
    }
    return rval;
  }

  @RequestMapping("/service-instances/{applicationName}")
  public String serviceInstancesByApplicationName(
    @RequestHeader("X-Authorization") String auth,
    @PathVariable String applicationName) {
    // I am credhubA (running locally)
    List<ServiceInstance> credhubServiceInstances = discoveryClient.getInstances(applicationName);
    String host = credhubServiceInstances.get(0).getHost();

    ResponseEntity<String> responseEntity;

    try {
      RestTemplate rest = new RestTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.add("Accept", "application/json");
      headers.add("Authorization", auth);
      HttpEntity<String> requestEntity = new HttpEntity<String>("", headers);
      responseEntity = rest.exchange(
        String.format("https://%s:8844/api/v1/data?name-like=/", host),
        HttpMethod.GET,
        requestEntity,
        String.class);
    }catch(Exception e) {
      return "some exception: " + e.toString() + "\n token: " + auth;
    }

    return responseEntity.getBody();

//    ServiceInstance credhubB = discoveryClient.getInstances(applicationName).get(0);
//    URI credhubBUri = credhubB.getUri();
//    HttpClient client = new HttpClient(credhubBUri);
//    Creds creds = client.get("/api/v1/data?name-like=/");
//    System.out.println(creds);
//    return this.discoveryClient.getInstances(applicationName);
  }

  @RequestMapping(path = "", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public synchronized CredentialView generate(final InputStream inputStream) throws IOException {
    final InputStream requestInputStream = new ByteArrayInputStream(ByteStreams.toByteArray(inputStream));
    return legacyGenerationHandler.auditedHandlePostRequest(requestInputStream);
  }

  @RequestMapping(path = "", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  public synchronized CredentialView set(@RequestBody final BaseCredentialSetRequest requestBody) {
    requestBody.validate();
    return auditedHandlePutRequest(requestBody);
  }

  @RequestMapping(path = "", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@RequestParam("name") final String credentialName) {
    if (StringUtils.isEmpty(credentialName)) {
      throw new InvalidQueryParameterException("error.missing_query_parameter", "name");
    }

    final String credentialNameWithPrependedSlash = StringUtils.prependIfMissing(credentialName, "/");

    final RequestDetails requestDetails = new DeleteCredential(credentialNameWithPrependedSlash);
    auditRecord.setRequestDetails(requestDetails);

    credentialsHandler.deleteCredential(credentialNameWithPrependedSlash);
  }

  @RequestMapping(path = "/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public CredentialView getCredentialById(@PathVariable final String id) {
    return credentialsHandler.getCredentialVersionByUUID(id);
  }

  @GetMapping(path = "")
  @ResponseStatus(HttpStatus.OK)
  public DataResponse getCredential(
    @RequestParam("name") final String credentialName,
    @RequestParam(value = "versions", required = false) final Integer numberOfVersions,
    @RequestParam(value = "current", required = false, defaultValue = "false") final boolean current
  ) {
    if (StringUtils.isEmpty(credentialName)) {
      throw new InvalidQueryParameterException("error.missing_query_parameter", "name");
    }

    if (current && numberOfVersions != null) {
      throw new InvalidQueryParameterException("error.cant_use_versions_and_current", "name");
    }

    final String credentialNameWithPrependedSlash = StringUtils.prependIfMissing(credentialName, "/");

    auditRecord.setRequestDetails(new GetCredential(credentialName, numberOfVersions, current));

    if (current) {
      return credentialsHandler.getCurrentCredentialVersions(credentialNameWithPrependedSlash);
    } else {
      return credentialsHandler.getNCredentialVersions(credentialNameWithPrependedSlash, numberOfVersions);
    }
  }

  @RequestMapping(path = "", params = "path", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public FindCredentialResults findByPath(
    @RequestParam("path") final String path,
    @RequestParam(value = "expires-within-days", required = false, defaultValue = "") final String expiresWithinDays
  ) {
    final FindCredential findCredential = new FindCredential();
    findCredential.setPath(path);
    findCredential.setExpiresWithinDays(expiresWithinDays);
    auditRecord.setRequestDetails(findCredential);

    return new FindCredentialResults(permissionedCredentialService.findStartingWithPath(path, expiresWithinDays));
  }

  @RequestMapping(path = "", params = "name-like", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public FindCredentialResults findByNameLike(
    @RequestParam("name-like") final String nameLike,
    @RequestParam(value = "expires-within-days", required = false, defaultValue = "") final String expiresWithinDays
  ) {
    final FindCredential findCredential = new FindCredential();
    findCredential.setNameLike(nameLike);
    findCredential.setExpiresWithinDays(expiresWithinDays);
    auditRecord.setRequestDetails(findCredential);

    return new FindCredentialResults(permissionedCredentialService.findContainingName(nameLike, expiresWithinDays));
  }

  private CredentialView auditedHandlePutRequest(@RequestBody final BaseCredentialSetRequest requestBody) {
    auditRecord.setRequestDetails(new SetCredential(requestBody.getName(), requestBody.getType()));
    return setHandler.handle(requestBody);
  }
}
