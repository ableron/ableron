# Ableron Verification Test Suite

Test suite to verify all implementations of ableron provide a common feature set.

## Quick Start
* Run tests
   ```console
   $ ./gradlew clean test
   ```
* Check for outdated dependencies via [Gradle Versions Plugin](https://github.com/ben-manes/gradle-versions-plugin)
   ```console
   $ ./gradlew dependencyUpdates -Drevision=release
   ```

## How to add new spec
* New runnable application which shall be verified
   * Create folder `./<language>-<framework>`, e.g. `/java21-spring-boot-3`
   * Application must run on port `8080`
* New test which tests the created application
   * Create file `./src/test/groovy/io/github/ableron/<SpecName>Spec.groovy` (just copy existing spec and adjust path to application)
* New GitHub workflow which runs the new test
   * Create file `/.github/workflows/<SpecName>.yml` (just copy existing one and adjust test to execute)
* New badge in `/README.md` which shows the status of the spec
