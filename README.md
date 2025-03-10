## <p align="center">Ableron - Distributed Server Side UI Composition</p>
<p align="center">
  <a href="https://github.com/ableron/ableron/ableron-js" target="_blank"><img src="https://github.com/ableron/ableron/actions/workflows/ableron-js-verify.yml/badge.svg" alt="ableron-js Status" /></a>
  <a href="https://github.com/ableron/ableron/ableron-express" target="_blank"><img src="https://github.com/ableron/ableron/actions/workflows/ableron-express-verify.yml/badge.svg" alt="ableron-express Status" /></a>
  <a href="https://github.com/ableron/ableron/ableron-java" target="_blank"><img src="https://github.com/ableron/ableron/actions/workflows/ableron-java.yml/badge.svg" alt="ableron-java Status" /></a>
</p>

## Description

Ableron is a library (or a set of libraries) which glues together your micro frontends using distributed
server side UI composition.<br>
It does the job via intercepting the output of your microservice and resolving <code>&lt;ableron-include&gt;</code>-tags
used to load content from remote sources and substitute the <code>&lt;ableron-include&gt;</code>-tags with
the remote content.

This leads to
- **Lean Infrastructure**: No need to route all your traffic through the infrastructure component which is performing the UI composition
- **Easy Local Development**: Includes are resolved directly in your service. This works fine for local development and increases developer experience.
- **Easy Configuration**: Your can easily define different timeouts, fallback fragments or static fallback content for each of your includes.
- **Local Caching**: All fragments are cached in memory directly within your service according to the cache policy of the fragments. This saves HTTP calls and thus money as well as latency.

## Getting Started

Find documentation at [ableron.github.io](http://ableron.github.io/)

The technical libs you may want to start with, are:
- the [Java Library](https://github.com/ableron/ableron/tree/main/ableron-java)
- the [JavaScript Library](https://github.com/ableron/ableron/tree/main/ableron-js)
- the [Spring Boot Plugin](https://github.com/ableron/ableron-spring-boot)
- the [express Plugin](https://github.com/ableron/ableron/tree/main/ableron-express)
- the [fastify Plugin](https://github.com/ableron/ableron-fastify)

### End-to-End Test Suite Status

* [![ableron-java @ Java 11 Status](https://github.com/ableron/ableron/actions/workflows/ableron-verify-java11.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-verify-java11.yml)
* [![ableron-java @ Java 17 Status](https://github.com/ableron/ableron/actions/workflows/ableron-verify-java17.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-verify-java17.yml)
* [![ableron-java @ Java 21 Status](https://github.com/ableron/ableron/actions/workflows/ableron-verify-java21.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-verify-java21.yml)
* [![ableron-spring-boot @ Java 11 & Spring Boot 2 Status](https://github.com/ableron/ableron/actions/workflows/ableron-verify-java11-spring-boot-2.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-verify-java11-spring-boot-2.yml)
* [![ableron-spring-boot @ Java 21 & Spring Boot 2 Status](https://github.com/ableron/ableron/actions/workflows/ableron-verify-java21-spring-boot-2.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-verify-java21-spring-boot-2.yml)
* [![ableron-spring-boot @ Java 17 & Spring Boot 3 Status](https://github.com/ableron/ableron/actions/workflows/ableron-verify-java17-spring-boot-3.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-verify-java17-spring-boot-3.yml)
* [![ableron-spring-boot @ Java 21 & Spring Boot 3 Status](https://github.com/ableron/ableron/actions/workflows/ableron-verify-java21-spring-boot-3.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-verify-java21-spring-boot-3.yml)
* [![ableron-js @ Node.js 22 CommonJS Status](https://github.com/ableron/ableron/actions/workflows/ableron-verify-nodejs22-cjs.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-verify-nodejs22-cjs.yml)
* [![ableron-js @ Node.js 22 ES Modules Status](https://github.com/ableron/ableron/actions/workflows/ableron-verify-nodejs22-esm.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-verify-nodejs22-esm.yml)
* [![ableron-express @ Node.js 22 & Express 4 Status](https://github.com/ableron/ableron/actions/workflows/ableron-verify-nodejs22-express4.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-verify-nodejs22-express4.yml)
* [![ableron-express @ Node.js 22 & Express 4 & CJS Status](https://github.com/ableron/ableron/actions/workflows/ableron-verify-nodejs22-express4-cjs.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-verify-nodejs22-express4-cjs.yml)
* [![ableron-fastify @ Node.js 22 & Fastify 4 Status](https://github.com/ableron/ableron/actions/workflows/ableron-verify-nodejs22-fastify4.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-verify-nodejs22-fastify4.yml)

### Contributing

All contributions are greatly appreciated. Be it pull requests, feature requests or bug reports. See
[ableron.github.io](https://ableron.github.io/) for details.

### License

Licensed under [MIT License](./LICENSE).
