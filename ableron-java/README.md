# Ableron Java Library

[![Build Status](https://github.com/ableron/ableron/actions/workflows/ableron-java.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-java.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.ableron/ableron/badge.svg)](https://mvnrepository.com/artifact/io.github.ableron/ableron)
[![Java Version](https://img.shields.io/badge/Java-11+-4EB1BA.svg)](https://docs.oracle.com/en/java/javase/11/)

Java implementation of Ableron Decentralized Server-Side UI Composition.

## Installation
Gradle:
```groovy
implementation 'io.github.ableron:ableron:2.0.0'
```

Maven:
```xml
<dependency>
  <groupId>io.github.ableron</groupId>
  <artifactId>ableron</artifactId>
  <version>2.0.0</version>
</dependency>
```

## Usage

Normally, you do not want to use `ableron-java` directly, because intercepting and modifying
the response body within your service may be tricky. Instead, you may want to use an existing
framework integration, which uses `ableron-java` under the hood, e.g.
* [ableron-spring-boot-2](https://github.com/ableron/ableron/tree/main/ableron-spring-boot-2)
* [ableron-spring-boot-3](https://github.com/ableron/ableron/tree/main/ableron-spring-boot-3)

To use `ableron-java` directly, follow these steps:

1. Init ableron
   ```java
   var ableron = new Ableron(AbleronConfig.builder()
     .cacheMaxSizeInBytes(1024 * 1024 * 100)
     .build());
   ```
2. Use includes in response body
   ```html
   <html>
     <head>
       <ableron-include src="https://head-fragment" />
     </head>
     <body>
       <ableron-include src="https://body-fragment" fallback-src="https://fallback-body-fragment"><!-- Static fallback fragment goes here --></ableron-include>
     </body>
   </html>
   ```
3. Apply transclusion to response if applicable (HTTP status 2xx, 4xx or 5xx; Response content type is non-binary, ...)
   ```java
   // perform transclusion based on unprocessed response body and request headers from e.g. HttpServletRequest
   TransclusionResult transclusionResult = ableron.resolveIncludes(getOriginalResponseBody(), getRequestHeaders());
   // set body to the processed one
   setResponseBody(transclusionResult.getContent());
   // override response status code when primary include was present
   transclusionResult.getStatusCodeOverride().ifPresent(statusCode -> setResponseStatusCode(statusCode));
   // add response headers when primary include was present
   addResponseHeaders(transclusionResult.getResponseHeadersToForward());
   // set cache-control header
   getResponse().setHeader(CACHE_CONTROL, transclusionResult.calculateCacheControlHeaderValue(getResponseHeaders()));
   ```

### Configuration

- `enabled`
  - Default: `true`
  - Whether to enable UI composition.
- `requestTimeout`
  - Default: `3 seconds`
  - Timeout for requesting fragments.
- `requestHeadersForward`
  - Default: `[Correlation-ID, X-Correlation-ID, X-Request-ID]`
  - Request headers that are forwarded to fragment requests, if present.<br>
    These request headers are not considered to influence the response and thus will not influence caching.
- `requestHeadersForwardVary`
  - Default: `empty list`
  - Request headers that are forwarded to fragment requests, if present and that influence the requested fragment
    aside from its URL.<br>
    These request headers are considered to influence the response and thus influence caching.
- `responseHeadersForward`
  - Default: `[Content-Language, Location, Refresh]`
  - Response headers of primary fragments to forward to the page response, if present.
- `cacheMaxSizeInBytes`
  - Default: `1024 * 1024 * 50` (`50 MiB`)
  - Maximum size in bytes the fragment cache may have.
- `cacheAutoRefreshEnabled`
  - Default: `false`
  - Whether to enable auto-refreshing of cached fragments, before they expire.<br>
    If set to `true`, cached fragments are getting asynchronously refreshed before they expire. This reduces the cache miss
    rate and thus has a positive impact on latency. On the other hand, additional traffic is introduced, because the cached
    fragments are loaded again even before their actual expiration time.<br>
    Fragments are tried to be refreshed when only 15% of their initial time to live remains. In case of failure, refresh is
    repeated three times with a static delay of one second.
- `cacheAutoRefreshMaxAttempts`
  - Default: `3`
  - Maximum number of attempts to refresh a cached fragment.
- `cacheAutoRefreshInactiveFragmentsMaxRefreshs`
  - Default: `2`
  - Maximum number of consecutive refreshs of inactive cached fragments.<br>
    Fragments are considered inactive, if they have not been read from cache between writing to cache and a refresh attempt.
- `statsAppendToContent`
  - Default: `false`
  - Whether to append UI composition stats as HTML comment to the content.
- `statsExposeFragmentUrl`
  - Default: `false`
  - Whether to expose fragment URLs in the stats appended to the content.
