# Ableron Spring Boot
[![Build Status](https://github.com/ableron/ableron/actions/workflows/ableron-spring-boot-2.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-spring-boot-2.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.ableron/ableron-spring-boot/badge.svg)](https://mvnrepository.com/artifact/io.github.ableron/ableron-spring-boot)

**Spring Boot Support for Ableron Server Side UI Composition**
1. Spring Boot 3.x project: Please use ableron-spring-boot-starter 3.x (Java 17+, spring-webmvc only)
2. Spring Boot 2.x (< 2.1) project: Please use ableron-spring-boot-starter 2.x (Java 11+, spring-webmvc only)

## Installation
Gradle:
```groovy
implementation 'io.github.ableron:ableron-spring-boot-starter:2.8.0'
```

Maven:
```xml
<dependency>
  <groupId>io.github.ableron</groupId>
  <artifactId>ableron-spring-boot-starter</artifactId>
  <version>2.8.0</version>
</dependency>
```

## Usage
Use includes in response body
```html
<ableron-include src="https://your-fragment-url" />
```

### Configuration

#### `ableron.enabled`

Default: `true`

Whether UI composition is enabled.

#### `ableron.fragment-request-timeout`

Default: `3s`

Timeout for requesting fragments. Interpreted as milliseconds, if no unit is provided.

#### `ableron.fragment-request-headers-to-pass`

Default:

```java
List.of(
    "Accept-Language",
    "Correlation-ID",
    "Forwarded",
    "Referer",
    "User-Agent",
    "X-Correlation-ID",
    "X-Forwarded-For",
    "X-Forwarded-Proto",
    "X-Forwarded-Host",
    "X-Real-IP",
    "X-Request-ID"
);
```

Request headers that are passed to fragment requests, if present.

#### `ableron.fragment-additional-request-headers-to-pass`

Default: `empty list`

Extends `ableron.fragment-request-headers-to-pass`. Use this property to pass all headers defined in
`ableron.fragment-request-headers-to-pass` plus the additional headers defined here. This prevents the
need to duplicate `ableron.fragment-request-headers-to-pass` if the only  use case is to add additional
headers instead of modifying the default ones.

#### `ableron.primary-fragment-response-headers-to-pass`

```java
List.of(
    "Content-Language",
    "Location",
    "Refresh"
);
```

Response headers of primary fragments to pass to the page response, if present.

#### `ableron.cache.max-size`

Default: `50MB`

Maximum size, the fragment cache may have.

#### `ableron.cache.vary-by-request-headers`

Default: `empty list`

Fragment request headers which influence the requested fragment aside from its URL. Used to create fragment cache keys.
Must be a subset of `ableron.fragment-request-headers-to-pass`. Common example are headers used for steering A/B-tests.

#### `ableron.cache.auto-refresh-enabled`

Default: `false`

Whether to enable auto-refreshing of cached fragments, before they expire.
If set to `true`, cached fragments are getting asynchronously refreshed before they expire. This reduces the cache miss
rate and thus have a positive impact on latency. On the other hand, additional traffic is introduced, because the cached
fragments are loaded again even before their actual expiration time.
Fragments are tried to be refreshed when only 15% of their initial time to live remains. In case of failure, refresh is
repeated three times with a static delay of one second.

#### `ableron.cache.auto-refresh-max-attempts`

Default: `3`

Maximum number of attempts to refresh a cached fragment.

#### `ableron.cache.auto-refresh-inactive-fragments-max-refreshs`

Default: `2`

Maximum number of consecutive refreshs of inactive cached fragments.
Fragments are considered inactive, if they have not been read from cache between writing to cache and a refresh attempt.

#### `ableron.stats.append-to-content`

Default: `false`

Whether to append UI composition stats as HTML comment to the content.

#### `ableron.stats.expose-fragment-url`

Default: `false`

Whether to expose fragment URLs in the stats appended to the content.
