package org.cloudfoundry.credhub.auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.oauth2.provider.error.DefaultWebResponseExceptionTranslator;
import org.springframework.security.oauth2.provider.error.WebResponseExceptionTranslator;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkTokenStore;

@Configuration
@Profile({
  "prod",
  "dev",
})
@ConditionalOnProperty("security.oauth2.enabled")
public class OAuth2Configuration {

  @Bean
  public ResourceServerProperties resourceServerProperties() {
    return new ResourceServerProperties();
  }

  @Bean
  public JwtAccessTokenConverter jwtAccessTokenConverter() throws Exception {
    final DefaultAccessTokenConverter defaultAccessTokenConverter = new DefaultAccessTokenConverter();
    defaultAccessTokenConverter.setIncludeGrantType(true);
    final JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
    jwtAccessTokenConverter.setAccessTokenConverter(defaultAccessTokenConverter);
    jwtAccessTokenConverter.afterPropertiesSet();
    return jwtAccessTokenConverter;
  }

  @Bean
  public JwkTokenStore jwkTokenStore(final OAuthProperties oAuthProperties) throws Exception {
    return new JwkTokenStore(oAuthProperties.getJwkKeysPath(), jwtAccessTokenConverter());
  }

  @Bean
  public ResourceServerTokenServices resourceServerTokenServices(final JwkTokenStore tokenStore) {
    final DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
    defaultTokenServices.setTokenStore(tokenStore);
    return defaultTokenServices;
  }

  @Bean
  public AuthenticationManagerBuilder authenticationManagerBuilder() {
    final ObjectPostProcessor<Object> objectPostProcessor = new ObjectPostProcessor<Object>() {
      @Override
      public <O extends Object> O postProcess(final O object) {
        return object;
      }
    };
    final AuthenticationManagerBuilder authenticationManagerBuilder =
      new AuthenticationManagerBuilder(objectPostProcessor);
    authenticationManagerBuilder.parentAuthenticationManager(authenticationManager());
    return authenticationManagerBuilder;
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    return authentication -> authentication;
  }

  @Bean
  public WebResponseExceptionTranslator webResponseExceptionTranslator() {
    return new DefaultWebResponseExceptionTranslator();
  }
}
