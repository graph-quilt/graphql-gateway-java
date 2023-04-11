# Contribution Guide

* Please follow the [Code of Conduct](./CODE_OF_CONDUCT.md).
* Please create a bug report or feature request on the issue section.
* Discuss your issue and get the high level design approved from one of the code owners.
* Please read the Code of Conduct before contributing to this project.
* Please link your issues to corresponding Pull Requests, especially for larger changes.
* All code changes should have a new/edited test!

        
## Working on the Code

* Fork and clone this repository


### Keeping your fork up-to-date
* Grab the latest code for your master branch.
```bash
git checkout master
git pull upstream master
```

### Workflow, branching, and commits
This is the workflow suggested for all developers to follow when contributing code:
```
YourFork/branch -> graphql-gateway-java/master
```

* When doing any new work, run the following command to create a feature branch that is synced with and tracking `graphql-gateway-java/master`
```
git fetch upstream; git checkout -b <branch_name> upstream/master

//or if your local repository is up to date...
git checkout -b <branch_name>
```
* A message indicating the work done is a must :-). 
    * `add new graphql feature`
* Always do work on your feature branch(es). You **should not** need to commit to your `master` branch.

### Code format
This project follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). Please format Pull Requests to conform to the guide. Here's how to perform the one-time setup for auto-formatting in IntelliJ:

1. Download Google's IntelliJ auto-format config [file](https://raw.githubusercontent.com/google/styleguide/gh-pages/intellij-java-google-style.xml).
2. In IntelliJ, navigate to `IntelliJ IDEA -> Preferences -> Editor -> Code Style -> Java`.
3. Click on `Manage...` then `Import...`. Select `IntelliJ IDEA code style XML` and click `OK`.
4. Navigate to and select the downloaded Google auto-format config file in the resulting Finder dialog.
5. Leave the default selections in the next dialog and click `OK`. You should now have a new Code Style Scheme called `GoogleStyle`. Click `Close`.
6. Back on the `...Code Style -> Java` window, select `GoogleStyle` from the `Scheme:` dropdown. Click `OK`.
7. In IntelliJ, navigate to `IntelliJ IDEA -> Preferences -> Editor -> General -> AutoImport ` and check `Java Optimize imports on the fly` click `OK`.

When making changes to this project, please run `Code -> Reformat Code` on the changed files. **NOTE:** Please isolate any formatting changes in commits separate from your actual logical code changes in order to make the Pull Request easier to review.

### Coding recommendations
* Method parameters that are immutable are marked `final`
* Method parameters that should **NOT** be null are annotated `@NonNull` (lombok.nonNull) OR Preconditions.assertNotNull() is used in the method.
* Local variables that are immutable should be marked `final`
```java
public void someMethod(@NonNull final List<String> immutableList, List<String> mutableList) {
 //immutableList = new ArrayList<>(); ILLEGAL!
 mutableList = new ArrayList<>(); //legal
}
```
* Java8 features are preferred. IntelliJ will let you know of these optimizations by highlighting your line of code yellow
    * (MAC) To address these optimizations, put your typing cursor on the yellow statement then press `option + enter`. IntelliJ will show you the optimization option, if available.
* Use dependency injection where possible
* Do not define static variables at the class level. Static constants are OK.
* Argument checking should utilize guava's [Preconditions](https://github.com/google/guava/wiki/PreconditionsExplained)
* Method composition is preferred
* Primitives are preferred over Objects (`int` instead of `Integer`)

### Documentation
* Javadocs should be created for new classes and methods
* Self-documenting code is highly encouraged; use non-javadoc comments sparingly
```java
/**
 * Example class that demonstrates javadoc documentation
 */
public class Example {

  private A a;
  private B b;

  public Example(A a, B b) {
    this.a = a;
    this.b = b;
  }

  /**
   * Computes a class C
   *
   * @return computed value of A transformed with B
   */
  C compute() {
    return A.transformWith(B);
  }
}
```

### Naming
* Acronyms for classes, variables, and methods are not recommended. Abbreviations are ok as long as it still makes sense.
    * If you must use an acronym because its not feasible to shorten, treat it like a word (`TdesClient` not `TDESClient`)
    * Document the full name of the acronym where it is defined. If it is a file name, use the full name in your javadoc
* Long method names or classes that are referenced many times in a file should be statically imported

### Logging
* Graphql Gateway uses `EventLogger` class for logging. We require `TransactionContext` to be passed as a parameter in methods where logging is expected.
* Keep your logs short, concise, and to the point. `Resource failed to close` or `Request to TKE Failed`, not `Resource failed to close, ignoring this because we always ignore when we fail at closing resources`.
* Understand the impact of the area of code you want to log, and understand the levels of logging that you can utilize
    * `debug`: log any useful information used for debugging.
    * `info`: log important application state. Good Example: `Nav Algorithm found next page: somePage`. Do not log: `Entered X method`.
    * `warn`: log any unexpected behavior that should be considered, but not immediately acted upon.
    * `error`: log any unexpected behavior that should result in immediate action.
* Use `LogNameValuePair` when logging properties and attributes.
```java
//do
EventLogger.info(log, transactionContext, "Request Succeeded", LogNameValuePair.of("response", response));

//don't
EventLogger.info(log, transactionContext, "Request Succeeded, response: " + response);
```
* Statically import EventLogger methods
* Use the `@Slf4j` annotation above your class (lombok.extern.slf4j.Slf4j). This will inject a logger into your class that can be referenced with `log`
    * Download the Lombok plugin for IntelliJ: click on `IntelliJ Idea -> Preferences -> Plugins -> Browse Repositories...` enter `Lombok Plugin`
```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SomeClass {
  public void someMethod(@NonNull final TransactionContext transactionContext) {
    try {
      //SomeImportantMethodCall
    } catch(Exception e) {
      error(log, transactionContext, "SomeImportantMethodCall failed", e);
    }
  }
}
```

### Deprecation
* Do not delete public classes, constants, and methods without knowing the impact for consumers. Deprecate them instead.
```java

@Deprecated("Use somePublicApi(String, String)")
public String somePublicApi(String someRequest) {}

public String somePublicApi(String someRequest, String otherData) {}

@Obsolete("This is no longer needed because somePublicApi(String, String) performs this method")
public String somePreparationApi(String someRequest) {}
```

### Unit testing
* Make sure your code does not decrease code coverage. We will make sure, in your code review, that your tests meet this criteria.
    * Look in `/target/` when you build to see the code coverage report (JaCoCo)
* `AssertJ` is the library we use to do all of our unit test assertions (most of the assertions start with `assertThat`)
* statically import all `AssertJ` methods
```java
import static org.assertj.core.api.Assertions.assertThat;

import org.junit;

public class TestClass {
  
  @Test
  public void testBestBurger() {
    //asserting strings
    final String bestBurger = someClass.getBestBurger();
    assertThat(bestBurger)
      .isEqualTo("in-n-out")
      .as("The best burger should be in-n-out");
  }
  
  @Test
  public void testBurgerIngredients() {
    //burger ingredients is a List<String>
    assertThat(burgerIngredients)
      .hasSize(8)
      .contains("grilled onions", "cheese")
      .doesNotContain("milkshake");
  }
}
```

### Integration Testing

Integration tests should be created under `com.intuit.graphql.gateway.integration` package.  Our integration tests 
are written using Junit as described in the Unit Test section but uses `@SpringBootTest` annotation. This will 
start the spring boot application with the `/graphql` and `/register` endpoint.  For mocking downstream provider
responses, we use wiremock.  You may explore the existing integration tests to learn more.  And don't forget 
to consult a data api team member if needed.

## Creating a pull request
Please make pull requests to `graphql-gateway-java/master`
* Tag any appropriate parties in the description of your pull request, then tag a core contributor to review your pull request.

## After submitting the Pull Request
Your reviewer(s) will perform the following tasks
* Ensure codecov does not report a decrease in code coverage
* ensure that the code committed is related to the issue (sorry no sneak-ins!)
* Does the PR follow the guidelines outlined in this document

We typically reply with comments and requests within one or two business days.
**Note** that comments are a critical part of the review process. We write comments to keep a history of conversation about the work, and to ensure that our contribution guide is effective at communicating code requirements.

When your pull request is ready to be merged, we will ask you to rebase your branch with `upstream/master` if we see excessive merge commits..like these:
* `Merge remote-tracking branch 'upstream/master'`

Follow these steps:
```
git checkout <branch_name>
git fetch upstream;
git rebase upstream/master;
// if you have conflicts, follow these steps:
// 1) resolve any conflicts
// 2) git add -A;
// 3) git rebase --continue;
// repeat steps above until rebasing is done
mvn clean install; //make sure it builds!!
git push --force-with-lease origin <branch_name>;
```

[Here's a more in-detail guide about rebasing](https://www.atlassian.com/git/tutorials/rewriting-history#git-rebase).