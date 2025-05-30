# @ableron/ableron

[![Build Status](https://github.com/ableron/ableron/actions/workflows/ableron-js.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-js.yml)
[![npm version](https://badge.fury.io/js/@ableron%2Fableron.svg)](https://badge.fury.io/js/@ableron%2Fableron)
[![Node.js Version](https://img.shields.io/badge/Node.js-18+-4EB1BA.svg)](https://nodejs.org/docs/latest-v18.x/api/)

JavaScript implementation of Ableron Decentralized Server-Side UI Composition.

## Installation

```shell
npm i @ableron/ableron
```

## Usage

Normally, you do not want to use `ableron-js` directly, because intercepting and modifying
the response body within your service may be tricky. Instead, you may want to use an existing
framework integration, which uses `ableron-js` under the hood, e.g.

- [ableron-express](https://github.com/ableron/ableron/tree/main/ableron-express)
- [ableron-fastify](https://github.com/ableron/ableron/tree/main/ableron-fastify)

To use `ableron-js` directly, do something like this:

```ts
import { Ableron } from '@ableron/ableron';

const ableron = new Ableron(
  /* optional configuration */
  {
    statsAppendToContent: true
    // ...
  },
  // optional logger
  pinoWinstonMorganOrWhateverYouMayHave() || console
);
const rawResponseBody = buildRawResponseBody();
const req = yourNodeJsRequestObject();
const res = yourNodeJsResponseObject();

try {
  ableron
    .resolveIncludes(rawResponseBody, req.headers)
    .then((transclusionResult) => {
      transclusionResult
        .getResponseHeadersToForward()
        .forEach((headerValue, headerName) => res.setHeader(headerName, headerValue));
      res.setHeader(
        'Cache-Control',
        transclusionResult.calculateCacheControlHeaderValueByResponseHeaders(res.getHeaders())
      );
      res.setHeader('Content-Length', Buffer.byteLength(transclusionResult.getContent()));
      res.status(transclusionResult.getStatusCodeOverride() || res.statusCode);
      setFinalResponseBody(transclusionResult.getContent());
    })
    .catch((e) => {
      logger.error(`[Ableron] Unable to perform UI composition: ${e.stack || e.message}`);
    });
} catch (e) {
  logger.error(`[Ableron] Unable to perform UI composition: ${e.stack || e.message}`);
}
```

### Configuration

- `enabled`
  - Default: `true`
  - Whether to enable UI composition.
- `requestTimeoutMs`
  - Default: `3000`
  - Timeout in milliseconds for requesting fragments.
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
- `cacheMaxItems`
  - Default: `10000`
  - Maximum number of items, the fragment cache may hold.
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
