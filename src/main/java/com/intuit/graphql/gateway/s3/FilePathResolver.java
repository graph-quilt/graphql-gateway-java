package com.intuit.graphql.gateway.s3;

import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
public interface FilePathResolver {

  String PATH = "%s/%s/registrations/%s/%s/%s";

  @Parameter
  String env();

  @Parameter
  String version();

  @Parameter
  String appName();

  @Derived
  default String getResolvedPath(String fileNameInZip, String appId) {
    return String
        .format(PATH,
            appName(), env(), version(), appId, fileNameInZip);
  }

}
