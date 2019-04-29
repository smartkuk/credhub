package org.cloudfoundry.credhub.utils;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import org.cloudfoundry.credhub.auth.OAuth2IssuerService;

@Primary
@Component
public class TestOAuth2IssuerService implements OAuth2IssuerService {
  @Override
  public String getIssuer() {
    return "https://example.com:8443/oauth/token";
  }
}
