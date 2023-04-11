package com.intuit.graphql.gateway.registry;

import com.intuit.graphql.gateway.s3.AWSRegion;
import lombok.Builder;
import lombok.Getter;
import reactor.util.context.Context;

@Builder
@Getter
public class RegistrationContextData {

  private ServiceRegistration incomingServiceRegistration;
  private ServiceRegistration incomingServiceRegistrationS3PathResolved;
  private ServiceRegistration cacheServiceRegistration;
  private byte[] fileInBytes;
  private String serviceAppId;
  private boolean isValidateOnly;
  private AWSRegion awsRegion;
  private Context context;

}
