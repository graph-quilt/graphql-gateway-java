package com.intuit.graphql.gateway.utils;

import org.apache.commons.lang.StringUtils;

public class S3RegistryPathUtil {

  private S3RegistryPathUtil() {}

  private static final char PATH_SEPARATOR = '/';

  private static final int ENV_INDEX_IN_PATH = 1;

  public static String toEnvSpecificPath(String path, String env) {
    String[] pathElements = StringUtils.split(path, PATH_SEPARATOR);
    String pathEnv = pathElements[ENV_INDEX_IN_PATH];
    return StringUtils.replaceOnce(path, pathEnv, env);
  }

}
