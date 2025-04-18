package io.github.ableron

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Timeout

import java.time.Instant
import java.util.concurrent.TimeUnit

class TransclusionProcessorSpec extends Specification {

  @Shared
  def transclusionProcessor = new TransclusionProcessor()

  def "should recognize includes of different forms"() {
    expect:
    transclusionProcessor.findIncludes(content).first().rawIncludeTag == expectedRawIncludeTag

    where:
    content                                                         | expectedRawIncludeTag
    "<ableron-include src=\"test\"/>"                               | "<ableron-include src=\"test\"/>"
    "<ableron-include src=\"test\" />"                              | "<ableron-include src=\"test\" />"
    "<ableron-include\nsrc=\"test\" />"                             | "<ableron-include\nsrc=\"test\" />"
    "<ableron-include\tsrc=\"test\"\t\t/>"                          | "<ableron-include\tsrc=\"test\"\t\t/>"
    "<ableron-include src=\"test\"></ableron-include>"              | "<ableron-include src=\"test\"></ableron-include>"
    "<ableron-include src=\"test\"> </ableron-include>"             | "<ableron-include src=\"test\"> </ableron-include>"
    "<ableron-include src=\"test\">foo\nbar\nbaz</ableron-include>" | "<ableron-include src=\"test\">foo\nbar\nbaz</ableron-include>"
    "<ableron-include src=\">>\"/>"                                 | "<ableron-include src=\">>\"/>"
    "<ableron-include src=\"/>\"/>"                                 | "<ableron-include src=\"/>\"/>"
    "\n<ableron-include src=\"...\"/>\n"                            | "<ableron-include src=\"...\"/>"
    "<div><ableron-include src=\"...\"/></div>"                     | "<ableron-include src=\"...\"/>"
    "<ableron-include src=\"...\"  fallback-src=\"...\"/>"          | "<ableron-include src=\"...\"  fallback-src=\"...\"/>"
    "<ableron-include src=\"test\" primary/>"                       | "<ableron-include src=\"test\" primary/>"
    "<ableron-include src=\"test\" primary=\"primary\"/>"           | "<ableron-include src=\"test\" primary=\"primary\"/>"
  }

  def "should not recognize includes with invalid format"() {
    expect:
    transclusionProcessor.findIncludes(content).isEmpty()

    where:
    content << [
      "<ableron-include/>",
      "<ableron-include >",
      "<ableron-include src=\"s\">",
      "<ableron-include src=\"s\" b=\"b\">"
    ]
  }

  def "should accept line breaks in include tag attributes"() {
    when:
    def include = transclusionProcessor.findIncludes("""
      <ableron-include
          src="https://foo.bar/fragment-1"
          fallback-src="https://foo.bar/fragment-1-fallback"/>
      """).first()

    then:
    include.src == "https://foo.bar/fragment-1"
    include.fallbackSrc == "https://foo.bar/fragment-1-fallback"
  }

  def "should parse include tag attributes"() {
    expect:
    transclusionProcessor.findIncludes(content).first().rawAttributes == expectedRawAttributes

    where:
    content                                                          | expectedRawAttributes
    '<ableron-include src="https://example.com"/>'                   | ["src": "https://example.com"]
    '<ableron-include  src="https://example.com"/>'                  | ["src": "https://example.com"]
    '<ableron-include   src="https://example.com"/>'                 | ["src": "https://example.com"]
    '<ableron-include -src="https://example.com"/>'                  | ["-src": "https://example.com"]
    '<ableron-include _src="https://example.com"/>'                  | ["_src": "https://example.com"]
    '<ableron-include 0src="https://example.com"/>'                  | ["0src": "https://example.com"]
    '<ableron-include foo="" src="https://example.com"/>'            | ["foo": "", "src": "https://example.com"]
    '<ableron-include src="source" fallback-src="fallback"/>'        | ["src": "source", "fallback-src": "fallback"]
    '<ableron-include fallback-src="fallback" src="source"/>'        | ["src": "source", "fallback-src": "fallback"]
    '<ableron-include src=">" fallback-src="/>"/>'                   | ["src": ">", "fallback-src": "/>"]
    '<ableron-include src="https://example.com" primary/>'           | ["src": "https://example.com", "primary": ""]
    '<ableron-include primary src="https://example.com"/>'           | ["src": "https://example.com", "primary": ""]
    '<ableron-include src="https://example.com" primary="primary"/>' | ["src": "https://example.com", "primary": "primary"]
    '<ableron-include src="https://example.com" primary="foo"/>'     | ["src": "https://example.com", "primary": "foo"]
  }

  def "should find all includes in input content"() {
    expect:
    transclusionProcessor.findIncludes("""
      <html>
      <head>
        <ableron-include src="https://foo.bar/baz?test=123" />
        <title>Foo</title>
        <ableron-include foo="bar" src="https://foo.bar/baz?test=456"/>
      </head>
      <body>
        <ableron-include src="https://foo.bar/baz?test=789" fallback-src="https://example.com"/>
        <ableron-include src="https://foo.bar/baz?test=789" fallback-src="https://example.com">fallback</ableron-include>
      </body>
      </html>
    """) == [
      new Include('<ableron-include src="https://foo.bar/baz?test=123" />'),
      new Include('<ableron-include foo="bar" src="https://foo.bar/baz?test=456"/>'),
      new Include('<ableron-include src="https://foo.bar/baz?test=789" fallback-src="https://example.com"/>'),
      new Include('<ableron-include src="https://foo.bar/baz?test=789" fallback-src="https://example.com">fallback</ableron-include>')
    ] as Set
  }

  def "should treat multiple identical includes as one include"() {
    expect:
    transclusionProcessor.findIncludes("""
      <html>
      <head>
        <ableron-include src="https://foo.bar/baz?test=123"/>
        <ableron-include src="https://foo.bar/baz?test=123"/>
        <title>Foo</title>
        <ableron-include foo="bar" src="https://foo.bar/baz?test=456"></ableron-include>
        <ableron-include foo="bar" src="https://foo.bar/baz?test=456"></ableron-include>
      </head>
      <body>
        <ableron-include src="...">...</ableron-include>
        <ableron-include src="...">...</ableron-include>
      </body>
      </html>
    """) == [
      new Include('<ableron-include src="https://foo.bar/baz?test=123"/>'),
      new Include('<ableron-include foo="bar" src="https://foo.bar/baz?test=456"></ableron-include>'),
      new Include('<ableron-include src="...">...</ableron-include>')
    ] as Set
  }

  def "should perform search for includes in big input string"() {
    given:
    def randomStringWithoutIncludes = new Random().ints(32, 127)
      .limit(512 * 1024)
      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
      .toString()
    def randomStringWithIncludeAtTheBeginning = "<ableron-include />" + randomStringWithoutIncludes
    def randomStringWithIncludeAtTheEnd = randomStringWithoutIncludes + "<ableron-include />"
    def randomStringWithIncludeAtTheMiddle = randomStringWithoutIncludes + "<ableron-include />" + randomStringWithoutIncludes

    expect:
    transclusionProcessor.findIncludes(randomStringWithoutIncludes).size() == 0
    transclusionProcessor.findIncludes(randomStringWithIncludeAtTheBeginning).size() == 1
    transclusionProcessor.findIncludes(randomStringWithIncludeAtTheEnd).size() == 1
    transclusionProcessor.findIncludes(randomStringWithIncludeAtTheMiddle).size() == 1
  }

  def "should populate TransclusionResult"() {
    when:
    def result = transclusionProcessor.resolveIncludes("""
      <html>
      <head>
        <ableron-include src="https://foo.bar/baz?test=123"><!-- failed loading 1st include --></ableron-include>
        <title>Foo</title>
        <ableron-include foo="bar" src="https://foo.bar/baz?test=456"><!-- failed loading 2nd include --></ableron-include>
      </head>
      <body>
        <ableron-include src="https://foo.bar/baz?test=789"><!-- failed loading 3rd include --></ableron-include>
      </body>
      </html>
    """, [:])

    then:
    result.processedIncludesCount == 3
    result.processingTimeMillis >= 1
    result.content == """
      <html>
      <head>
        <!-- failed loading 1st include -->
        <title>Foo</title>
        <!-- failed loading 2nd include -->
      </head>
      <body>
        <!-- failed loading 3rd include -->
      </body>
      </html>
    """
  }

  def "should populate TransclusionResult with primary include status code"() {
    given:
    def mockWebServer = new MockWebServer()
    def baseUrl = mockWebServer.url("/").toString()
    mockWebServer.setDispatcher(new Dispatcher() {
      @Override
      MockResponse dispatch(RecordedRequest recordedRequest) throws InterruptedException {
        switch (recordedRequest.getPath()) {
          case "/header":
            return new MockResponse()
              .setResponseCode(200)
              .setBody("header-fragment")
          case "/footer":
            return new MockResponse()
              .setResponseCode(200)
              .setBody("footer-fragment")
          case "/main":
            return new MockResponse()
              .setResponseCode(301)
              .addHeader("Location", "/foobar")
              .setBody("main-fragment")
        }
        return new MockResponse().setResponseCode(500)
      }
    })

    when:
    def result = transclusionProcessor.resolveIncludes("""
      <ableron-include src="${baseUrl}header" />
      <ableron-include src="${baseUrl}main" primary="primary"><!-- failure --></ableron-include>
      <ableron-include src="${baseUrl}footer" />
    """, [:])

    then:
    result.content == """
      header-fragment
      main-fragment
      footer-fragment
    """
    result.hasPrimaryInclude()
    result.getStatusCodeOverride().get() == 301
    result.getResponseHeadersToForward() == ["location": ["/foobar"]]

    cleanup:
    mockWebServer.shutdown()
  }

  def "should set content expiration time to lowest fragment expiration time"() {
    given:
    def mockWebServer = new MockWebServer()
    def baseUrl = mockWebServer.url("/").toString()
    mockWebServer.setDispatcher(new Dispatcher() {
      @Override
      MockResponse dispatch(RecordedRequest recordedRequest) throws InterruptedException {
        switch (recordedRequest.getPath()) {
          case "/header":
            return new MockResponse()
              .setResponseCode(200)
              .setHeader("Cache-Control", "max-age=120")
              .setBody("header-fragment")
          case "/footer":
            return new MockResponse()
              .setResponseCode(200)
              .setHeader("Cache-Control", "max-age=60")
              .setBody("footer-fragment")
          case "/main":
            return new MockResponse()
              .setResponseCode(200)
              .setHeader("Cache-Control", "max-age=30")
              .setBody("main-fragment")
        }
        return new MockResponse().setResponseCode(500)
      }
    })

    when:
    def result = transclusionProcessor.resolveIncludes("""
      <ableron-include src="${baseUrl}header"/>
      <ableron-include src="${baseUrl}main"/>
      <ableron-include src="${baseUrl}footer"/>
    """, [:])

    then:
    result.content == """
      header-fragment
      main-fragment
      footer-fragment
    """
    with (result.contentExpirationTime.get()) {
      isBefore(now() + 31)
      isAfter(now() + 27)
    }

    cleanup:
    mockWebServer.shutdown()
  }

  def "should set content expiration time to past if a fragment must not be cached"() {
    given:
    def mockWebServer = new MockWebServer()
    def baseUrl = mockWebServer.url("/").toString()
    mockWebServer.setDispatcher(new Dispatcher() {
      @Override
      MockResponse dispatch(RecordedRequest recordedRequest) throws InterruptedException {
        switch (recordedRequest.getPath()) {
          case "/header":
            return new MockResponse()
              .setResponseCode(200)
              .setHeader("Cache-Control", "max-age=120")
              .setBody("header-fragment")
          case "/footer":
            return new MockResponse()
              .setResponseCode(200)
              .setHeader("Cache-Control", "max-age=60")
              .setBody("footer-fragment")
          case "/main":
            return new MockResponse()
              .setResponseCode(200)
              .setHeader("Cache-Control", "no-store, no-cache, must-revalidate")
              .setBody("main-fragment")
        }
        return new MockResponse().setResponseCode(500)
      }
    })

    when:
    def result = transclusionProcessor.resolveIncludes("""
      <ableron-include src="${baseUrl}header"/>
      <ableron-include src="${baseUrl}main"/>
      <ableron-include src="${baseUrl}footer"/>
    """, [:])

    then:
    result.content == """
      header-fragment
      main-fragment
      footer-fragment
    """
    result.contentExpirationTime.get() == Instant.EPOCH

    cleanup:
    mockWebServer.shutdown()
  }

  def "should prevent caching if no fragment provides explicit caching information"() {
    given:
    def mockWebServer = new MockWebServer()
    def baseUrl = mockWebServer.url("/").toString()
    mockWebServer.setDispatcher(new Dispatcher() {
      @Override
      MockResponse dispatch(RecordedRequest recordedRequest) throws InterruptedException {
        switch (recordedRequest.getPath()) {
          case "/header":
            return new MockResponse()
              .setResponseCode(200)
              .setBody("header-fragment")
          case "/footer":
            return new MockResponse()
              .setResponseCode(200)
              .setBody("footer-fragment")
          case "/main":
            return new MockResponse()
              .setResponseCode(200)
              .setBody("main-fragment")
        }
        return new MockResponse().setResponseCode(500)
      }
    })

    when:
    def result = transclusionProcessor.resolveIncludes("""
      <ableron-include src="${baseUrl}header"/>
      <ableron-include src="${baseUrl}main"/>
      <ableron-include src="${baseUrl}footer"/>
    """, [:])

    then:
    result.content == """
      header-fragment
      main-fragment
      footer-fragment
    """
    result.contentExpirationTime.get() == Instant.EPOCH

    cleanup:
    mockWebServer.shutdown()
  }

  def "should replace identical includes"() {
    when:
    def result = transclusionProcessor.resolveIncludes("""
      <ableron-include src="foo-bar"><!-- #1 --></ableron-include>
      <ableron-include src="foo-bar"><!-- #1 --></ableron-include>
      <ableron-include src="foo-bar"><!-- #1 --></ableron-include>
      <ableron-include src="foo-bar"><!-- #2 --></ableron-include>
    """, [:])

    then:
    result.content == """
      <!-- #1 -->
      <!-- #1 -->
      <!-- #1 -->
      <!-- #2 -->
    """
  }

  def "should not crash due to include tag #scenarioName"() {
    when:
    def result = transclusionProcessor
      .resolveIncludes("<ableron-include >before</ableron-include>" + includeTag + "<ableron-include >after</ableron-include>", [:])

    then:
    result.content == "before" + expectedResult + "after"

    where:
    scenarioName                   | includeTag                                                               | expectedResult
    "invalid src url"              | '<ableron-include src=",._">fallback</ableron-include>'                  | "fallback"
    "invalid src timeout"          | '<ableron-include src-timeout="5 seconds">fallback</ableron-include>'    | "fallback"
    "invalid fallback-src timeout" | '<ableron-include fallback-src-timeout=" 5s">fallback</ableron-include>' | "fallback"
  }

  def "should perform only one request per URL"() {
    given:
    def mockWebServer = new MockWebServer()
    def baseUrl = mockWebServer.url("/").toString()
    mockWebServer.setDispatcher(new Dispatcher() {
      @Override
      MockResponse dispatch(RecordedRequest recordedRequest) throws InterruptedException {
        switch (recordedRequest.getPath()) {
          case "/1":
            return new MockResponse()
              .setResponseCode(200)
              .setBody("fragment-1")
              .setHeadersDelay(200, TimeUnit.MILLISECONDS)
        }
        return new MockResponse()
          .setResponseCode(404)
          .setBody("404")
      }
    })

    when:
    def result = transclusionProcessor.resolveIncludes("""
      <html>
      <head>
        <ableron-include src="${baseUrl}1"><!-- failed loading 1st fragment --></ableron-include>
        <title>Foo</title>
        <ableron-include src="${baseUrl}1"><!-- failed loading 2nd fragment --></ableron-include>
      </head>
      <body>
        <ableron-include src="${baseUrl}1"><!-- failed loading 3rd fragment --></ableron-include>
        <ableron-include src="${baseUrl}expect-404"><!-- failed loading 4th fragment --></ableron-include>
      </body>
      </html>
    """, [:])

    then:
    result.content == """
      <html>
      <head>
        fragment-1
        <title>Foo</title>
        fragment-1
      </head>
      <body>
        fragment-1
        <!-- failed loading 4th fragment -->
      </body>
      </html>
    """

    cleanup:
    mockWebServer.shutdown()
  }

  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  def "should resolve includes in parallel"() {
    given:
    def mockWebServer = new MockWebServer()
    def baseUrl = mockWebServer.url("/").toString()
    mockWebServer.setDispatcher(new Dispatcher() {
      @Override
      MockResponse dispatch(RecordedRequest recordedRequest) throws InterruptedException {
        switch (recordedRequest.getPath()) {
          case "/503-route":
            return new MockResponse()
              .setResponseCode(503)
              .setBody("fragment-1")
              .setHeadersDelay(2000, TimeUnit.MILLISECONDS)
          case "/1000ms-delay-route":
            return new MockResponse()
              .setResponseCode(200)
              .setBody("fragment-2")
              .setHeadersDelay(1000, TimeUnit.MILLISECONDS)
          case "/2000ms-delay-route":
            return new MockResponse()
              .setResponseCode(200)
              .setBody("fragment-3")
              .setHeadersDelay(2000, TimeUnit.MILLISECONDS)
          case "/2100ms-delay-route":
            return new MockResponse()
              .setResponseCode(200)
              .setBody("fragment-4")
              .setHeadersDelay(2100, TimeUnit.MILLISECONDS)
          case "/2200ms-delay-route":
            return new MockResponse()
              .setResponseCode(200)
              .setBody("fragment-5")
              .setHeadersDelay(2200, TimeUnit.MILLISECONDS)
        }
        return new MockResponse().setResponseCode(404)
      }
    })

    when:
    def result = transclusionProcessor.resolveIncludes("""
      <html>
      <head>
        <ableron-include src="${baseUrl}503-route"><!-- failed loading fragment #1 --></ableron-include>
        <title>Foo</title>
        <ableron-include src="${baseUrl}1000ms-delay-route"><!-- failed loading fragment #2 --></ableron-include>
      </head>
      <body>
        <ableron-include src="${baseUrl}2000ms-delay-route"><!-- failed loading fragment #3 --></ableron-include>
        <ableron-include src="${baseUrl}2100ms-delay-route"><!-- failed loading fragment #4 --></ableron-include>
        <ableron-include src="${baseUrl}2200ms-delay-route"><!-- failed loading fragment #5 --></ableron-include>
        <ableron-include src="${baseUrl}expect-404"><!-- failed loading fragment #6 --></ableron-include>
      </body>
      </html>
    """, [:])

    then:
    result.content == """
      <html>
      <head>
        <!-- failed loading fragment #1 -->
        <title>Foo</title>
        fragment-2
      </head>
      <body>
        fragment-3
        fragment-4
        fragment-5
        <!-- failed loading fragment #6 -->
      </body>
      </html>
    """

    cleanup:
    mockWebServer.shutdown()
  }

  def "should handle unresolvable include"() {
    given:
    def transclusionProcessor = new TransclusionProcessor(AbleronConfig.builder().statsAppendToContent(true).build())
    def parentRequestHeaders = Mock(Map)

    when:
    def result = transclusionProcessor.resolveIncludes(
      "<ableron-include ><!-- fallback content --></ableron-include>",
      parentRequestHeaders
    )

    then:
    result.content.matches("(?s)<!-- fallback content -->\\n<!-- Processed 1 include in \\d+ms.+")
    result.content.matches("(?s).+\\d+ms \\| 125703905 \\| fallback content \\| -\\n.+")
    result.contentExpirationTime.get() <= Instant.now().plusSeconds(60)
    result.contentExpirationTime.get() >= Instant.now().plusSeconds(58)
  }

  def "should record stats"() {
    given:
    def transclusionProcessor = new TransclusionProcessor(AbleronConfig.builder()
      .statsAppendToContent(true)
      .build())
    def mockWebServer = new MockWebServer()
    def baseUrl = mockWebServer.url("/").toString()
    mockWebServer.setDispatcher(new Dispatcher() {
      @Override
      MockResponse dispatch(RecordedRequest recordedRequest) throws InterruptedException {
        switch (recordedRequest.getPath()) {
          case "/200-cacheable":
            return new MockResponse()
              .setResponseCode(200)
              .setHeader("Cache-Control", "max-age=60")
              .setBody("200-cacheable")
          case "/200-not-cacheable":
            return new MockResponse()
              .setResponseCode(200)
              .setBody("200-not-cacheable")
          case "/404":
            return new MockResponse()
              .setResponseCode(404)
              .setBody("404")
          case "/503":
            return new MockResponse()
              .setResponseCode(503)
              .setBody("503")
        }
        return new MockResponse().setResponseCode(404)
      }
    })

    when:
    transclusionProcessor.resolveIncludes("""
      <ableron-include src="${baseUrl}200-cacheable"/>
      <ableron-include src="${baseUrl}200-not-cacheable"/>
      <ableron-include src="${baseUrl}404"/>
      <ableron-include src="${baseUrl}503"/>
    """, [:])
    transclusionProcessor.resolveIncludes("""
      <ableron-include src="${baseUrl}200-cacheable"/>
      <ableron-include src="${baseUrl}200-not-cacheable"/>
      <ableron-include src="${baseUrl}404" primary/>
      <ableron-include src="${baseUrl}503" fallback-src="${baseUrl}404" primary/>
    """, [:])
    transclusionProcessor.resolveIncludes("""
      <ableron-include src="${baseUrl}200-cacheable"/>
      <ableron-include src="${baseUrl}200-not-cacheable"/>
      <ableron-include src="${baseUrl}404" fallback-src="${baseUrl}200-cacheable" primary/>
      <ableron-include src="${baseUrl}404" fallback-src="${baseUrl}503"/>
    """, [:])
    def result = transclusionProcessor.resolveIncludes("""
      <ableron-include id="5" src="${baseUrl}200-cacheable"/>
      <ableron-include id="e" src="${baseUrl}200-not-cacheable"/>
      <ableron-include id="4" src="${baseUrl}404"><!-- error --></ableron-include>
      <ableron-include id="h" src="${baseUrl}404" fallback-src="${baseUrl}503" primary><!-- error --></ableron-include>
      <ableron-include id="z" fallback-src="${baseUrl}200-not-cacheable"/>
      <ableron-include id="b" src="${baseUrl}503" fallback-src="${baseUrl}200-cacheable"><!-- error --></ableron-include>""", [:])

    then:
    result.content.replaceAll("\\d+ms", "XXXms") == """
      200-cacheable
      200-not-cacheable
      <!-- error -->
      404
      200-not-cacheable
      200-cacheable
<!-- Processed 6 includes in XXXms

Time | Include | Resolved With | Fragment Cacheability
------------------------------------------------------
XXXms | 4 | fallback content | -
XXXms | 5 | cached src | expires in 60s
XXXms | b | cached fallback-src | expires in 60s
XXXms | e | remote src | not cacheable
XXXms | h (primary) | remote src | not cacheable
XXXms | z | remote fallback-src | not cacheable

Cache: 3 items, 5 hits, 18 misses, 0 successful refreshs, 0 failed refreshs
-->"""

    cleanup:
    mockWebServer.shutdown()
  }
}
