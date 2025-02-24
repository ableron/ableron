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
      requestHeadersForward == [
        "Correlation-ID",
        "X-Correlation-ID",
        "X-Request-ID"
      ]
      requestHeadersForwardVary == []
      responseHeadersForward == [
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
      .requestHeadersForward(["X-Test-Request-Header", "X-Test-Request-Header-2"])
      .requestHeadersForwardVary(["X-Test-Groups", "X-ACME-Country"])
      .responseHeadersForward(["X-Test-Response-Header", "X-Test-Response-Header-2"])
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
      requestHeadersForward == ["X-Test-Request-Header", "X-Test-Request-Header-2"]
      requestHeadersForwardVary == ["X-Test-Groups", "X-ACME-Country"]
      responseHeadersForward == ["X-Test-Response-Header", "X-Test-Response-Header-2"]
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

  def "should throw exception if requestHeadersForward is tried to be set to null"() {
    when:
    AbleronConfig.builder()
      .requestHeadersForward(null)
      .build()

    then:
    def exception = thrown(NullPointerException)
    exception.message == "requestHeadersForward must not be null"
  }

  def "should throw exception if requestHeadersForwardVary is tried to be set to null"() {
    when:
    AbleronConfig.builder()
      .requestHeadersForwardVary(null)
      .build()

    then:
    def exception = thrown(NullPointerException)
    exception.message == "requestHeadersForwardVary must not be null"
  }

  def "should throw exception if responseHeadersForward is tried to be set to null"() {
    when:
    AbleronConfig.builder()
      .responseHeadersForward(null)
      .build()

    then:
    def exception = thrown(NullPointerException)
    exception.message == "responseHeadersForward must not be null"
  }

  def "should expose only immutable collections - default values"() {
    given:
    def config = AbleronConfig.builder().build()

    when:
    config.getRequestHeadersForward().add("Not-Allowed")

    then:
    thrown(UnsupportedOperationException)

    when:
    config.getRequestHeadersForwardVary().add("Not-Allowed")

    then:
    thrown(UnsupportedOperationException)

    when:
    config.getResponseHeadersForward().add("Not-Allowed")

    then:
    thrown(UnsupportedOperationException)
  }

  def "should expose only immutable collections - provided values"() {
    given:
    def config = AbleronConfig.builder()
      .requestHeadersForward(new ArrayList())
      .requestHeadersForwardVary(new ArrayList())
      .responseHeadersForward(new ArrayList())
      .build()

    when:
    config.getRequestHeadersForward().add("Not-Allowed")

    then:
    thrown(UnsupportedOperationException)

    when:
    config.getRequestHeadersForwardVary().add("Not-Allowed")

    then:
    thrown(UnsupportedOperationException)

    when:
    config.getResponseHeadersForward().add("Not-Allowed")

    then:
    thrown(UnsupportedOperationException)
  }
}
