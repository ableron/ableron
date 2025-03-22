# Contributing

## Quick Start
* Compile/test/package
  ```console
  ./mvnw clean install
  ```
* Check for outdated dependencies via [Versions Maven Plugin](https://www.mojohaus.org/versions/versions-maven-plugin/index.html)
  ```console
  ./mvnw versions:display-dependency-updates
  ```
* Update maven wrapper to newer version
   ```console
   ./mvnw wrapper:wrapper -Dmaven=<version, e.g. 3.9.0>
   ```

## Tooling
* See `io.github.ableron:ableron-spring-boot-starter` in [MvnRepository](https://mvnrepository.com/artifact/io.github.ableron/ableron-spring-boot-starter)
* See Artifacts in [nexus repository manager](https://s01.oss.sonatype.org/index.html#nexus-search;gav~io.github.ableron~ableron-spring-boot*~~~)
