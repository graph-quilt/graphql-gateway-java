package com.intuit.graphql.gateway;

import com.intuit.graphql.gateway.registry.ServiceRegistrationException;
import java.util.Objects;
import java.util.function.Predicate;
import org.apache.commons.lang.StringUtils;
import software.amazon.awssdk.services.s3.model.S3Object;

public final class Predicates {

  private static final String MAIN_FOLDER_NAME = "main";

  private Predicates() {
  }

  //todo these are better suited as plain methods.
  public static final Predicate<String> isMainConfigJson = str -> StringUtils.endsWith(str, "main/config.json");
  public static final Predicate<String> isConfigYml = str -> StringUtils.endsWith(str, "config.yml");
  public static final Predicate<String> isFlowFile = str -> StringUtils.endsWith(str, "flow/service.flow");
  public static final Predicate<String> isSDLFile = str -> StringUtils.endsWith(str, ".graphql") || StringUtils
      .endsWith(str, ".graphqls");
  public static final Predicate<String> isRuleFile = str -> StringUtils.endsWith(str, ".rules");
  public static final Predicate<String> isRegistrationFile = isMainConfigJson.or(isFlowFile).or(isSDLFile);
  public static final Predicate<String> isMainFolder = str -> StringUtils.startsWith(str, MAIN_FOLDER_NAME);

  /**
   * Creates a {@link Predicate} that tests if a given {@link S3Object} is a main resource or not. A main resource
   * includes the main folder itself and the files under the main folder.
   *
   * @param pathPrefix expected prefix of s3Object key
   * @return {@link Predicate} created
   */
  public static final Predicate<String> isMainResourcePredicate(final String pathPrefix) {
    return str -> StringUtils.startsWith(
        StringUtils.substringAfter(StringUtils.substringAfter(str, pathPrefix + "/"), "/"),
        MAIN_FOLDER_NAME);
  }

  public static final Predicate<Throwable> isSkippableRegistrationError(final boolean isS3Registration) {
    return throwable -> isS3Registration && Objects.nonNull(throwable)
            && throwable instanceof ServiceRegistrationException;
  }
}
