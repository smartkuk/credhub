package io.pivotal.security;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import io.pivotal.security.config.AuthServerProperties;
import io.pivotal.security.entity.JpaAuditingHandlerRegistrar;
import io.pivotal.security.util.CurrentTimeProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ofPattern;

@SpringBootApplication
@EnableConfigurationProperties({ AuthServerProperties.class })
@EnableJpaAuditing(dateTimeProviderRef = "currentTimeProvider")
@Import(JpaAuditingHandlerRegistrar.class)
public class CredentialManagerApp {

  private final DateTimeFormatter TIMESTAMP_FORMAT = ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  public static void main(String[] args) {
    SpringApplication.run(CredentialManagerApp.class, args);
  }

  @Bean
  Configuration getConfiguration() {
    return Configuration.defaultConfiguration()
        .addOptions(Option.SUPPRESS_EXCEPTIONS);
  }

  @Bean(name = "currentTimeProvider")
  public CurrentTimeProvider currentTimeProvider() {
    CurrentTimeProvider currentTimeProvider = new CurrentTimeProvider();
    return currentTimeProvider;
  }

  @Bean
  public Module javaTimeModule() {
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(Instant.class, new JsonSerializer<Instant>() {

      @Override
      public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(ZonedDateTime.ofInstant(value, ZoneId.of("UTC")).format(TIMESTAMP_FORMAT));
      }
    });
    return javaTimeModule;
  }

  @Bean
  public Jackson2ObjectMapperBuilder jacksonBuilder(Module javaTimeModule) {
    Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
    builder.modules(javaTimeModule);
    return builder;
  }
}