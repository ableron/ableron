# Contributing

## Quick Start
* `npm test` to build and test all projects
* `npm run test:ableron-java` to build and test `ableron-java`
* `npm run test:ableron-js` to build and test `ableron-js`
* `npm run test:ableron-express` to build and test `ableron-express`
* `npm run test:ableron-fastify` to build and test `ableron-fastify`
* `npm run test:ableron-spring-boot-2` to build and test `ableron-spring-boot` for Spring Boot 2
* `npm run test:ableron-spring-boot-3` to build and test `ableron-spring-boot` for Spring Boot 3
* `npm run ableron-verify` to test all projects against the common `ableron-verify` test base

## Tooling
* Java Components
  * Find available `ableron-java` SNAPSHOT versions [here](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/io/github/ableron/ableron/)

## Perform Release
1. Release `ableron-js`
   1. Make sure, `package.json` → `version` reflects the version to be published
   2. Run `ableron-js-publish` workflow in GitHub Actions to release current main branch (using the version set in `package.json`)
   3. Set version in `package.json` to next version via new commit (do not forget to update `package-lock.json` via `npm install`)
2. Release `ableron-java`
   1. Set release version in `pom.xml` (remove `-SNAPSHOT`)
   2. Update version in `README.md` maven and gradle dependency declaration code snippets
   3. Update `ableron-verify` projects to match version in `ableron-java`
   4. Push code changes  - Release and deploy to Maven Central is performed automatically
   5. Set artifact version in `pom.xml` in `main` branch to next `-SNAPSHOT` version via new commit
   6. Update `ableron-verify` projects to match version in `ableron-java`
3. Release `ableron-spring-boot-2` and `ableron-spring-boot-3`
   1. Set release version in `pom.xml` (remove `-SNAPSHOT`)
   2. Update version in `README.md` maven and gradle dependency declaration code snippets
   3. Update `ableron` version in `pom.xml` (`<properties>` -> `<ableron.version>`) to the `ableron-java` version released one step before (remove `-SNAPSHOT`)
   4. Update `ableron-verify` projects to match versions in `ableron-spring-boot-2` and `ableron-spring-boot-3`
   5. Push code changes  - Release and deploy to Maven Central is performed automatically
   6. Set artifact version in `pom.xml` in `main` branch to next `-SNAPSHOT` version
   7. Set `ableron` version in `pom.xml` (`<properties>` -> `<ableron.version>`) to current `ableron-java` `-SNAPSHOT`-version
   8. Push code with new `-SNAPSHOT` versions and update `ableron-verify` projects to point to this new version
4. Release `ableron-express` and  `ableron-fastify`
   1. Make sure, `package.json` → `version` reflects the version to be published
   2. Update `@ableron/ableron` dependency in `package.json` to latest released `ableron-js` version
      1. Test locally via `npm i && npm test` to validate whether `@ableron/ableron` version is set correctly
   3. Update `package-lock.json` via `npm i` and push changes to `main`, if necessary
   4. Run `ableron-express-publish` and `ableron-fastify-publish` workflow in GitHub Actions to release current main branch (i.e. the version set in `package.json`)
   5. Set version in `package.json` to next version via new commit (do not forget to update `package-lock.json` via `npm i`) and update `ableron-verify` projects to point to this new version
5. Manually create [GitHub Release](https://github.com/ableron/ableron/releases/new)
   1. Set tag name to the released version e.g. `v1.0.0`
   2. Set release title to the release version, e.g. `1.0.0`
   3. Set release notes
   4. Publish release
