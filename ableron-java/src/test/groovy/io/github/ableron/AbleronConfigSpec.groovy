package io.github.ableron

import spock.lang.Specification

import java.time.Duration

class AbleronConfigSpec extends Specification {

  def "should have default value for each property"() {
    when:
    def config = AbleronConfig.builder().build()

    then:
    with(config) {
      enabled
      requestTimeout == Duration.ofSeconds(3)
      requestHeadersPassThrough == [
        "Correlation-ID",
        "X-Correlation-ID",
        "X-Request-ID"
      ]
      requestHeadersPassThroughVary == []
      responseHeadersPassThrough == [
        "Content-Language",
        "Location",
        "Refresh"
      ]
      cacheMaxSizeInBytes == 1024 * 1024 * 50
      !cacheAutoRefreshEnabled()
      cacheAutoRefreshMaxAttempts == 3
      cacheAutoRefreshInactiveFragmentsMaxRefreshs == 2
      !statsAppendToContent()
      !statsExposeFragmentUrl()
    }
  }

  def "should use values provided via builder"() {
    when:
    def config = AbleronConfig.builder()
      .enabled(false)
      .requestTimeout(Duration.ofMillis(200))
      .requestHeadersPassThrough(["X-Test-Request-Header", "X-Test-Request-Header-2"])
      .requestHeadersPassThroughVary(["X-Test-Groups", "X-ACME-Country"])
      .responseHeadersPassThrough(["X-Test-Response-Header", "X-Test-Response-Header-2"])
      .cacheMaxSizeInBytes(1024 * 100)
      .cacheAutoRefreshEnabled(true)
      .cacheAutoRefreshMaxAttempts(5)
      .cacheAutoRefreshInactiveFragmentsMaxRefreshs(4)
      .statsAppendToContent(true)
      .statsExposeFragmentUrl(true)
      .build()

    then:
    with(config) {
      !enabled
      requestTimeout == Duration.ofMillis(200)
      requestHeadersPassThrough == ["X-Test-Request-Header", "X-Test-Request-Header-2"]
      requestHeadersPassThroughVary == ["X-Test-Groups", "X-ACME-Country"]
      responseHeadersPassThrough == ["X-Test-Response-Header", "X-Test-Response-Header-2"]
      cacheMaxSizeInBytes == 1024 * 100
      cacheAutoRefreshEnabled()
      cacheAutoRefreshMaxAttempts == 5
      cacheAutoRefreshInactiveFragmentsMaxRefreshs == 4
      statsAppendToContent()
      statsExposeFragmentUrl()
    }
  }

  def "should throw exception if requestTimeout is tried to be set to null"() {
    when:
    AbleronConfig.builder()
      .requestTimeout(null)
      .build()

    then:
    def exception = thrown(NullPointerException)
    exception.message == "requestTimeout must not be null"
  }

  def "should throw exception if requestHeadersPassThrough is tried to be set to null"() {
    when:
    AbleronConfig.builder()
      .requestHeadersPassThrough(null)
      .build()

    then:
    def exception = thrown(NullPointerException)
    exception.message == "requestHeadersPassThrough must not be null"
  }

  def "should throw exception if requestHeadersPassThroughVary is tried to be set to null"() {
    when:
    AbleronConfig.builder()
      .requestHeadersPassThroughVary(null)
      .build()

    then:
    def exception = thrown(NullPointerException)
    exception.message == "requestHeadersPassThroughVary must not be null"
  }

  def "should throw exception if responseHeadersPassThrough is tried to be set to null"() {
    when:
    AbleronConfig.builder()
      .responseHeadersPassThrough(null)
      .build()

    then:
    def exception = thrown(NullPointerException)
    exception.message == "responseHeadersPassThrough must not be null"
  }

  def "should expose only immutable collections - default values"() {
    given:
    def config = AbleronConfig.builder().build()

    when:
    config.getRequestHeadersPassThrough().add("Not-Allowed")

    then:
    thrown(UnsupportedOperationException)

    when:
    config.getRequestHeadersPassThroughVary().add("Not-Allowed")

    then:
    thrown(UnsupportedOperationException)

    when:
    config.getResponseHeadersPassThrough().add("Not-Allowed")

    then:
    thrown(UnsupportedOperationException)
  }

  def "should expose only immutable collections - provided values"() {
    given:
    def config = AbleronConfig.builder()
      .requestHeadersPassThrough(new ArrayList())
      .requestHeadersPassThroughVary(new ArrayList())
      .responseHeadersPassThrough(new ArrayList())
      .build()

    when:
    config.getRequestHeadersPassThrough().add("Not-Allowed")

    then:
    thrown(UnsupportedOperationException)

    when:
    config.getRequestHeadersPassThroughVary().add("Not-Allowed")

    then:
    thrown(UnsupportedOperationException)

    when:
    config.getResponseHeadersPassThrough().add("Not-Allowed")

    then:
    thrown(UnsupportedOperationException)
  }
}
