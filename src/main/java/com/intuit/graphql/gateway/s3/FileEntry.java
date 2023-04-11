package com.intuit.graphql.gateway.s3;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
public interface FileEntry {

  @Parameter
  String filename();

  @Parameter
  byte[] contentInBytes();
}
