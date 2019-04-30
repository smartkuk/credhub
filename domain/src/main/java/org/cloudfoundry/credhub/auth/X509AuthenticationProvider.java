package org.cloudfoundry.credhub.auth;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import org.bouncycastle.asn1.x509.KeyPurposeId;

@Component
public class X509AuthenticationProvider extends PreAuthenticatedAuthenticationProvider {

  public static final String CLIENT_AUTH_EXTENDED_KEY_USAGE = KeyPurposeId.id_kp_clientAuth.getId();
  // Spring's access assertion language's hasRole() takes
  // {@link ROLE_MTLS_USER} without "ROLE_" prefix
  public static final String MTLS_USER = "MTLS_USER";
  private static final String ROLE_MTLS_USER = "ROLE_MTLS_USER";

  public X509AuthenticationProvider() {
    super();
    setPreAuthenticatedUserDetailsService(x509v3ExtService());
  }

  private AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> x509v3ExtService() {
    return token -> new User(token.getName(), "", AuthorityUtils.createAuthorityList(ROLE_MTLS_USER));
  }

  @Override
  public Authentication authenticate(final Authentication authentication) {
    final Authentication result = super.authenticate(authentication);

    if (result != null && authentication.getCredentials() instanceof X509Certificate) {
      final X509Certificate certificate = (X509Certificate) authentication.getCredentials();

      /*
        The following exceptions are wrapped in
        InternalAuthenticationServiceException to avoid the logic in
        org.springframework.security.authentication.ProviderManager
        from allowing another provider an
        attempt after this failure.
       */

      try {
        final List<String> extKeyUsage = certificate.getExtendedKeyUsage();
        if (extKeyUsage == null || !extKeyUsage.contains(CLIENT_AUTH_EXTENDED_KEY_USAGE)) {
          final BadCredentialsException throwable =
            new BadCredentialsException("");

          throw new InternalAuthenticationServiceException("Certificate does not contain: " + CLIENT_AUTH_EXTENDED_KEY_USAGE, throwable);
        }
      } catch (final CertificateParsingException e) {
        final BadCredentialsException throwable =
          new BadCredentialsException("");

        throw new InternalAuthenticationServiceException("Certificate Extended Key Usage unreadable", throwable);
      }
    }

    return result;
  }

}
