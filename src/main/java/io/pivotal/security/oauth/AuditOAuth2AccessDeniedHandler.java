package io.pivotal.security.oauth;

import io.pivotal.security.entity.OperationAuditRecord;
import io.pivotal.security.repository.AuditRecordRepository;
import io.pivotal.security.service.AuditRecordParameters;
import io.pivotal.security.util.InstantFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuditOAuth2AccessDeniedHandler extends OAuth2AccessDeniedHandler {
  @Autowired
  ResourceServerTokenServices tokenServices;

  @Autowired
  JwtTokenStore tokenStore;

  @Autowired
  InstantFactoryBean instantFactoryBean;

  @Autowired
  AuditRecordRepository operationAuditRecordRepository;

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException authException) throws IOException, ServletException {
    try {
      super.handle(request, response, authException);
    } finally {
      logAuthFailureToDb(request, new AuditRecordParameters(request, null), response.getStatus());
    }
  }

  private void logAuthFailureToDb(HttpServletRequest request, AuditRecordParameters auditRecordParameters, int status) {
    String token = (String) request.getAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE);

    OAuth2Authentication authentication = tokenStore.readAuthentication(token);
    OAuth2Request oAuth2Request = authentication.getOAuth2Request();

    OAuth2AccessToken accessToken = tokenServices.readAccessToken(token);

    String path = auditRecordParameters.getPath();
    String method = auditRecordParameters.getMethod();
    RequestToOperationTranslator requestToOperationTranslator = new RequestToOperationTranslator(path).setMethod(method);

    final Instant now;
    try {
      now = instantFactoryBean.getObject();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    Set<String> scopes = accessToken.getScope();
    String scope = scopes == null ? null : String.join(",", scopes);

    OperationAuditRecord operationAuditRecord = new OperationAuditRecord(
        now,
        requestToOperationTranslator.translate(),
        (String) accessToken.getAdditionalInformation().get("user_id"),
        (String) accessToken.getAdditionalInformation().get("user_name"),
        (String) accessToken.getAdditionalInformation().get("iss"),
        ((Number) accessToken.getAdditionalInformation().get("iat")).longValue(),
        accessToken.getExpiration().toInstant().getEpochSecond(),
        auditRecordParameters.getHostName(),
        method,
        path,
        auditRecordParameters.getRequesterIp(),
        auditRecordParameters.getXForwardedFor(),
        oAuth2Request.getClientId(),
        scope,
        oAuth2Request.getGrantType()
    );

    operationAuditRecord.setStatusCode(status);
    operationAuditRecord.setFailed();

    operationAuditRecordRepository.save(operationAuditRecord);
  }
}