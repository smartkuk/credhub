package org.cloudfoundry.credhub.utils;

import org.springframework.stereotype.Component;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;

@Component
public class JsonContextFactory {
  public ParseContext getParseContext() {
    final Configuration configuration = Configuration.defaultConfiguration()
      .addOptions(Option.SUPPRESS_EXCEPTIONS);
    return JsonPath.using(configuration);
  }
}
