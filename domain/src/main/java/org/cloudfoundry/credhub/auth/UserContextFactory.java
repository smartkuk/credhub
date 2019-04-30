package org.cloudfoundry.credhub.auth;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class UserContextFactory {
  @Autowired(required = false)
  private ResourceServerTokenServices resourceServerTokenServices;

  /*
   * The "iat" and "exp" claims are parsed by Jackson as integers,
   * because JWT defines these as seconds since Epoch
   * (https://tools.ietf.org/html/rfc7519#section-2). That means it has a
   * Year-2038 bug. To adapt to our local model, hoping JWT will some day be improved,
   * this function returns a numeric value as long.
   */
  private static long claimValueAsLong(final Map<String, Object> additionalInformation) {
    return ((Number) additionalInformation.get("iat")).longValue();
  }

  public UserContext createUserContext(final Authentication authentication) {
    if (authentication instanceof PreAuthenticatedAuthenticationToken) {
      return createUserContext((PreAuthenticatedAuthenticationToken) authentication);
    } else {
      return createUserContext((OAuth2Authentication) authentication, null);
    }
  }

  public UserContext createUserContext(final OAuth2Authentication authentication, final String maybeToken) {
    final OAuth2Request oauth2Request = authentication.getOAuth2Request();
    final String clientId = oauth2Request.getClientId();
    final String grantType = oauth2Request.getGrantType();
    String userId = null;
    String userName = null;
    String issuer = null;
    long validFrom = 0;
    long validUntil = 0;
    String scope = null;

    String token = maybeToken;

    if (maybeToken == null) {
      final OAuth2AuthenticationDetails authDetails = (OAuth2AuthenticationDetails) authentication
        .getDetails();
      token = authDetails.getTokenValue();
    }

    final OAuth2AccessToken accessToken;
    accessToken = resourceServerTokenServices.readAccessToken(token);


    if (accessToken != null) {
      final Set<String> scopes = accessToken.getScope();
      scope = scopes == null ? null : String.join(",", scopes);

      final Map<String, Object> additionalInformation = accessToken.getAdditionalInformation();
      userName = (String) additionalInformation.get("user_name");
      userId = (String) additionalInformation.get("user_id");
      issuer = (String) additionalInformation.get("iss");
      validFrom = claimValueAsLong(additionalInformation);
      validUntil = accessToken.getExpiration().toInstant().getEpochSecond();
    }

    return new UserContext(
      userId,
      userName,
      issuer,
      validFrom,
      validUntil,
      clientId,
      scope,
      grantType,
      UserContext.AUTH_METHOD_UAA
    );
  }

  private UserContext createUserContext(final PreAuthenticatedAuthenticationToken authentication) {
    final X509Certificate certificate = (X509Certificate) authentication.getCredentials();

    return new UserContext(
      certificate.getNotBefore().toInstant().getEpochSecond(),
      certificate.getNotAfter().toInstant().getEpochSecond(),
      certificate.getSubjectDN().getName(),
      UserContext.AUTH_METHOD_MUTUAL_TLS
    );
  }
}
