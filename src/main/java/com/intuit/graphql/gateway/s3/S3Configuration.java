package com.intuit.graphql.gateway.s3;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * Hold configuration information used to contact the S3 bucket.
 */
@Configuration
@ConfigurationProperties("aws.s3")
@RefreshScope
@Getter
@Setter
@Slf4j
public class S3Configuration {

  /**
   * Unique application name. This appName should be unique amongst other orchestration apps. Used to build S3
   * prefixes.
   */
  private String appName;

  /**
   * Environment subpath. Used to build S3 prefixes.
   */
  private String env;

  /**
   * AWS region override. Used to determine instance region as well.
   */
  private Region region = Region.US_WEST_2;

  /**
   * Version subpath. Used to build S3 prefixes.
   */
  private String version;

  /**
   * Setting this field to false at start-time <b>MUST</b> not use s3 registry.
   */
  private boolean enabled = false;

  /**
   * Map of region vs S3 bucket properties
   */
  private Map<String, S3Bucket> buckets;
  private S3Upload upload = new S3Upload();
  private S3Polling polling = new S3Polling();

  /**
   * AWS profile file path for credentials <b>(local development ONLY)</b>. This defaults to {@code
   * ${user.home}/.aws/credentials}.
   */
  private Path profileFilePath = Paths.get(System.getProperty("user.home"), ".aws/credentials");

  @Getter
  @Setter
  public static class S3Bucket {

    private String bucketName;
    private String endpoint;
    private String region;
  }

  @Getter
  @Setter
  public static class S3Upload {

    /**
     * List of Environments to upload registration files
     */
    private List<String> environments;

    /**
     * Maximum number of retry attempts for delete errors before aborting.
     */
    private int maxDeleteRetries = 5;
  }

  @Getter
  @Setter
  public static class S3Polling {

    /**
     * Maximum number of retry attempts for non-5xx errors before aborting.
     */
    private int maxRetryAttempts = 3;

    /**
     * S3 polling endpoint override.
     */
    private URI endpoint;

    /**
     * S3 bucket name to poll registrations from.
     */
    private String bucketname;

    /**
     * Setting this field to false at start-time <b>MUST</b> not poll s3 for changes. Setting this field to false at
     * run-time <b>MUST</b> stop future polling attempts.
     */
    private boolean enabled = false;

    /**
     * Period of delay before downloading latest content from S3.
     */
    private Duration syncDelay = Duration.ofSeconds(300);

    /**
     * S3 polling period.
     */
    private Duration period = Duration.ofSeconds(30);
  }

  public enum Region {
    US_WEST_2, US_EAST_2;

    public static Region fromValue(String value) {
      return valueOf(StringUtils.replaceChars(
          StringUtils.upperCase(value), '-', '_'));
    }
  }
}
