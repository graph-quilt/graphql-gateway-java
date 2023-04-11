package com.intuit.graphql.gateway;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import com.intuit.graphql.gateway.registry.ServiceRegistrationException;
import java.util.function.Predicate;
import org.junit.Test;

public class PredicatesTests {

  private static final String PATH_PREFIX = "graphql-gateway/e2e/registrations/1.0.0";
  private Predicate<String> isMainResourcePredicate = Predicates.isMainResourcePredicate(PATH_PREFIX);

  @Test
  public void testEmptyKey() {
    assertFalse(isMainResourcePredicate.test(""));
  }

  @Test
  public void testKeyWithUnexpectedPathPrefix() {
    assertFalse(isMainResourcePredicate.test("someprefix/test/file.txt"));
  }

  @Test
  public void testPrefixAsKey() {
    assertFalse(isMainResourcePredicate.test(PATH_PREFIX));
  }

  @Test
  public void testPrefixAsKeyWithTrailingSlash() {
    String keyStr = PATH_PREFIX + "/";
    assertFalse(isMainResourcePredicate.test(keyStr));
  }

  @Test
  public void testkeyWithAppId() {
    String keyStr = PATH_PREFIX + "/appId";
    assertFalse(isMainResourcePredicate.test(keyStr));
  }

  @Test
  public void testkeyWithAppIdWithTrailingslash() {
    String keyStr = PATH_PREFIX + "/appId/";
    assertFalse(isMainResourcePredicate.test(keyStr));
  }

  @Test
  public void testkeyWithMainFolder() {
    String keyStr = PATH_PREFIX + "/appId/main";
    assertTrue(isMainResourcePredicate.test(keyStr));
  }

  @Test
  public void testkeyWithMainFolderWithTrailingSlash() {
    String keyStr = PATH_PREFIX + "/appId/main/";
    assertTrue(isMainResourcePredicate.test(keyStr));
  }

  @Test
  public void testkeyWithMainFolderWithConfigJson() {
    String keyStr = PATH_PREFIX + "/appId/main/config.json";
    assertTrue(isMainResourcePredicate.test(keyStr));
  }

  @Test
  public void testkeyWithMainFolderWithSubdir() {
    String keyStr = PATH_PREFIX + "/appId/main/dir";
    assertTrue(isMainResourcePredicate.test(keyStr));
  }

  @Test
  public void testkeyWithMainFolderWithSubdirWithTrailingSlash() {
    String keyStr = PATH_PREFIX + "/appId/main/dir/";
    assertTrue(isMainResourcePredicate.test(keyStr));
  }

  @Test
  public void testkeyWithTestFolder() {
    String keyStr = PATH_PREFIX + "/appId/test";
    assertFalse(isMainResourcePredicate.test(keyStr));
  }

  @Test
  public void testkeyWithTestFolderWithTrailingSlash() {
    String keyStr = PATH_PREFIX + "/appId/test/";
    assertFalse(isMainResourcePredicate.test(keyStr));
  }

  @Test
  public void testkeyWithTestFolderWithSomeTestFile() {
    String keyStr = PATH_PREFIX + "/appId/test/some-karate-test.feature";
    assertFalse(isMainResourcePredicate.test(keyStr));
  }

  @Test
  public void testkeyWithTestFolderWithSubdir() {
    String keyStr = PATH_PREFIX + "/appId/test/dir";
    assertFalse(isMainResourcePredicate.test(keyStr));
  }

  @Test
  public void testkeyWithTestFolderWithSubdirWithTrailingSlash() {
    String keyStr = PATH_PREFIX + "/appId/test/dir/";
    assertFalse(isMainResourcePredicate.test(keyStr));
  }

  @Test
  public void testSDLEndsWithGraphl() {
    String filename = "/app/test/dir/schema.graphqls";
    assertTrue(Predicates.isSDLFile.test(filename));
  }

  @Test
  public void testSDLEndsWithGraphls() {
    String filename = "/app/test/dir/schema.graphql";
    assertTrue(Predicates.isSDLFile.test(filename));
  }

  @Test
  public void notSDLTest() {
    String filename = "/app/test/dir/schema.txt";
    assertFalse(Predicates.isSDLFile.test(filename));
  }

  @Test
  public void isRegistrationErrorTest() {
    ServiceRegistrationException exception = new ServiceRegistrationException("error");
    Predicate<Throwable> p = Predicates.isSkippableRegistrationError(true);
    assertTrue(p.test(exception));
  }


  @Test
  public void isRegistrationSuccessTest() {
    ServiceRegistrationException exception = new ServiceRegistrationException("error");
    Predicate<Throwable> p = Predicates.isSkippableRegistrationError(false);
    assertFalse(p.test(exception));
  }

  @Test
  public void isNotRegistrationErrorTest() {
    NullPointerException nullPointerException = new NullPointerException("error");
    Predicate<Throwable> p = Predicates.isSkippableRegistrationError(false);
    assertFalse(p.test(nullPointerException));
  }

  @Test
  public void isNotRegistrationErrorNullTest() {
    Predicate<Throwable> p = Predicates.isSkippableRegistrationError(false);
    assertFalse(p.test(null));
  }
}
