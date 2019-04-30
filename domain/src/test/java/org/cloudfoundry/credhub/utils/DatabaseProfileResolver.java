package org.cloudfoundry.credhub.utils;

import org.springframework.test.context.support.DefaultActiveProfilesResolver;

import org.apache.commons.lang3.ArrayUtils;

public class DatabaseProfileResolver extends DefaultActiveProfilesResolver {

  @Override
  public String[] resolve(final Class<?> testClass) {
    return (String[]) ArrayUtils.addAll(new String[]{System.getProperty(SpringUtilities.activeProfilesString)},
      super.resolve(testClass));
  }
}
