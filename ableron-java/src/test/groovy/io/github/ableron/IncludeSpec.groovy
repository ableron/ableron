package io.github.ableron

import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class IncludeSpec extends Specification {

  @Shared
  def config = AbleronConfig.builder()
    .requestTimeout(Duration.ofSeconds(1))
    .build()

  @Shared
  def httpClient = new TransclusionProcessor().getHttpClient()

  FragmentCache cache = new TransclusionProcessor().getFragmentCache()

  @Shared
  def supplyPool = Executors.newFixedThreadPool(4)

  def "constructor should set raw attributes"() {
    given:
    def rawAttributes = ["src": "https://example.com"]

    expect:
    new Include("", rawAttributes).rawAttributes == rawAttributes
  }

  def "constructor should set fallback content"() {
    expect:
    include.fallbackContent == expectedFallbackContent

    where:
    include                             | expectedFallbackContent
    new Include(null)                   | ""
    new Include(null, null, "fallback") | "fallback"
  }

  def "constructor should set raw include tag"() {
    given:
    def rawIncludeTag = '<ableron-include src="https://example.com"/>'

    expect:
    new Include(rawIncludeTag).rawIncludeTag == rawIncludeTag
  }

  def "should handle include id"() {
    expect:
    include.id == expectedId

    where:
    include                                    | expectedId
    new Include("")                            | "0"
    new Include("", ["id": "foo-bar"])         | "foo-bar"
    new Include("", ["id": "FOO-bar%baz__/5"]) | "FOO-barbaz__5"
    new Include("", ["id": "//"])              | "0"
    new Include("zzzzz")                       | "116425210"
    new Include("zzzzzz")                      | "685785664"
  }

  def "should parse src attribute"() {
    expect:
    include.src == expectedSrc

    where:
    include                                         | expectedSrc
    new Include("")                                 | null
    new Include("", ["src": "https://example.com"]) | "https://example.com"
    new Include("", ["SRC": "https://example.com"]) | "https://example.com"
  }

  def "should parse src timeout attribute"() {
    expect:
    include.srcTimeout == expectedSrcTimeout

    where:
    include                                    | expectedSrcTimeout
    new Include("")                            | null
    new Include("", ["src-timeout": "2000"])   | Duration.ofMillis(2000)
    new Include("", ["src-timeout": "2000ms"]) | Duration.ofMillis(2000)
    new Include("", ["src-timeout": "2s"])     | Duration.ofMillis(2000)
    new Include("", ["src-timeout": "2S"])     | null
    new Include("", ["src-timeout": " 2000"])  | null
    new Include("", ["src-timeout": "2000 "])  | null
    new Include("", ["src-timeout": "2m"])     | null
    new Include("", ["src-timeout": "2\ns"])   | null
  }

  def "should parse fallback-src attribute"() {
    expect:
    include.fallbackSrc == expectedFallbackSrc

    where:
    include                                                    | expectedFallbackSrc
    new Include(null)                                          | null
    new Include(null, ["fallback-src": "https://example.com"]) | "https://example.com"
  }

  def "should parse fallback src timeout attribute"() {
    expect:
    include.fallbackSrcTimeout == expectedFallbackSrcTimeout

    where:
    include                                             | expectedFallbackSrcTimeout
    new Include("")                                     | null
    new Include("", ["fallback-src-timeout": "2000"])   | Duration.ofMillis(2000)
    new Include("", ["fallback-src-timeout": "2000ms"]) | Duration.ofMillis(2000)
    new Include("", ["fallback-src-timeout": "2s"])     | Duration.ofMillis(2000)
    new Include("", ["fallback-src-timeout": "2S"])     | null
    new Include("", ["fallback-src-timeout": " 2000"])  | null
    new Include("", ["fallback-src-timeout": "2000 "])  | null
    new Include("", ["fallback-src-timeout": "2m"])     | null
    new Include("", ["fallback-src-timeout": "2\ns"])   | null
  }

  def "should parse primary attribute"() {
    expect:
    include.primary == expectedPrimary

    where:
    include                                   | expectedPrimary
    new Include(null)                         | false
    new Include(null, ["primary": ""])        | true
    new Include(null, ["PRIMARY": ""])        | true
    new Include(null, ["primary": "primary"]) | true
    new Include(null, ["primary": "PRIMARY"]) | true
    new Include(null, ["priMARY": "PRImary"]) | true
    new Include(null, ["primary": "nope"])    | false
  }

  def "should parse headers attribute"() {
    expect:
    include.headersToForward == expectedHeadersToForward

    where:
    include                                                                    | expectedHeadersToForward
    new Include(null)                                                          | []
    new Include(null, ["headers": ""])                                         | []
    new Include(null, ["headers": "test"])                                     | ["test"]
    new Include(null, ["headers": "TEST"])                                     | ["test"]
    new Include(null, ["headers": " test1,test2  ,, TEST3 ,\nTest4,,test4  "]) | ["test1", "test2", "test3", "test4"]
  }

  def "should parse cookies attribute"() {
    expect:
    include.cookiesToForward == expectedCookiesToForward

    where:
    include                                                                    | expectedCookiesToForward
    new Include(null)                                                          | []
    new Include(null, ["cookies": ""])                                         | []
    new Include(null, ["cookies": "test"])                                     | ["test"]
    new Include(null, ["cookies": "TEST"])                                     | ["TEST"]
    new Include(null, ["cookies": " test1,test2  ,, TEST3 ,\nTest4,,test4  "]) | ["test1", "test2", "TEST3", "Test4", "test4"]
  }

  def "should consider include objects with identical include string as equal"() {
    when:
    def include1 = new Include('<ableron-include src="..."></ableron-include>')
    def include2 = new Include('<ableron-include src="..."></ableron-include>', ["foo": "bar"])
    def include3 = new Include('<ableron-include src="..."/>', ["test": "test"], "fallback")

    then:
    include1 == include2
    include1 != include3
    include2 != include3
  }

  def "should resolve with src"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(206)
      .body("response")
      .build())
    mockWebServer.start()

    when:
    def include = new Include("", ["src": mockWebServer.url("/fragment").toString()])
      .resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "response"
    include.resolvedFragment.statusCode == 206
    include.resolvedFragmentSource == "remote src"
    include.resolveTimeMillis > 0
    mockWebServer.takeRequest().url.encodedPath() == "/fragment"

    cleanup:
    mockWebServer.close()
  }

  def "should resolve with fallback-src if src could not be loaded"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(500)
      .body("fragment from src")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment from fallback-src")
      .build())
    mockWebServer.start()

    when:
    def include = new Include("", [
      "src": mockWebServer.url("/src").toString(),
      "fallback-src": mockWebServer.url("/fallback-src").toString()
    ]).resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "fragment from fallback-src"
    include.resolvedFragment.statusCode == 200
    include.resolvedFragmentSource == "remote fallback-src"
    include.resolveTimeMillis > 0

    cleanup:
    mockWebServer.close()
  }

  def "should resolve with fallback content if src and fallback-src could not be loaded"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(500)
      .body("fragment from src")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(500)
      .body("fragment from src")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(500)
      .body("fragment from fallback-src")
      .build())
    mockWebServer.start()

    when:
    def include = new Include("", [
      "src": mockWebServer.url("/src").toString(),
      "fallback-src": mockWebServer.url("/fallback-src").toString()
    ], "fallback content").resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "fallback content"
    include.resolvedFragment.statusCode == 200
    include.resolvedFragmentSource == "fallback content"
    include.resolveTimeMillis > 0
    mockWebServer.takeRequest().url.encodedPath() == "/src"
    mockWebServer.takeRequest().url.encodedPath() == "/fallback-src"

    cleanup:
    mockWebServer.close()
  }

  def "should resolve to empty string if src, fallback src and fallback content are not present"() {
    when:
    def include = new Include(null).resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == ""
    include.resolvedFragment.statusCode == 200
    include.resolvedFragmentSource == "fallback content"
  }

  def "should handle primary include with errored src"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(503)
      .body("fragment from src")
      .build())
    mockWebServer.start()

    when:
    def include = new Include("", [
      "src": mockWebServer.url("/").toString(),
      "primary": "primary"
    ]).resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "fragment from src"
    include.resolvedFragment.statusCode == 503
    include.resolvedFragmentSource == "remote src"
    include.resolveTimeMillis > 0

    cleanup:
    mockWebServer.close()
  }

  def "should handle primary include without src and with errored fallback-src"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(503)
      .body("503")
      .build())
    mockWebServer.start()

    when:
    def include = new Include("", [
      "fallback-src": mockWebServer.url("/").toString(),
      "primary": "primary"
    ]).resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "503"
    include.resolvedFragment.statusCode == 503
    include.resolvedFragmentSource == "remote fallback-src"
    include.resolveTimeMillis > 0

    cleanup:
    mockWebServer.close()
  }

  def "should handle primary include with errored src and successfully resolved fallback-src"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(500)
      .body("src-500")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(206)
      .body("fallback-src-206")
      .build())
    mockWebServer.start()

    when:
    def include = new Include("", [
      "src": mockWebServer.url("/").toString(),
      "fallback-src": mockWebServer.url("/").toString(),
      "primary": ""
    ]).resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "fallback-src-206"
    include.resolvedFragment.statusCode == 206
    include.resolvedFragmentSource == "remote fallback-src"
    include.resolveTimeMillis > 0

    cleanup:
    mockWebServer.close()
  }

  def "should handle primary include with errored src and errored fallback-src"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(503)
      .body("src")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(500)
      .body("fallback")
      .build())
    mockWebServer.start()

    when:
    def include = new Include("", [
      "src": mockWebServer.url("/").toString(),
      "fallback-src": mockWebServer.url("/").toString(),
      "primary": ""
    ]).resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "src"
    include.resolvedFragment.statusCode == 503
    include.resolvedFragmentSource == "remote src"
    include.resolveTimeMillis > 0

    cleanup:
    mockWebServer.close()
  }

  def "should reset errored fragment of primary include for consecutive resolving"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(503)
      .body("src")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(500)
      .body("fallback")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(504)
      .body("src 2nd call")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(500)
      .body("fallback 2nd call")
      .build())
    mockWebServer.start()
    def include = new Include("", [
      "src": mockWebServer.url("/").toString(),
      "fallback-src": mockWebServer.url("/").toString(),
      "primary": ""
    ])

    when:
    include.resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "src"
    include.resolvedFragment.statusCode == 503
    include.resolvedFragmentSource == "remote src"
    include.resolveTimeMillis > 0

    when:
    include.resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "src 2nd call"
    include.resolvedFragment.statusCode == 504
    include.resolvedFragmentSource == "remote src"
    include.resolveTimeMillis > 0

    cleanup:
    mockWebServer.close()
  }

  def "should ignore fallback content and set fragment status code and body of errored src if primary"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(503)
      .body("response")
      .build())
    mockWebServer.start()

    when:
    def include = new Include("", [
      "src": mockWebServer.url("/").toString(),
      "primary": ""
    ], "fallback content").resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "response"
    include.resolvedFragment.statusCode == 503
    include.resolvedFragmentSource == "remote src"
    include.resolveTimeMillis > 0

    cleanup:
    mockWebServer.close()
  }

  def "should not follow redirects when resolving URLs"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(302)
      .setHeader("Location", "foo")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment after redirect")
      .build())
    mockWebServer.start()

    when:
    def include = new Include("", ["src": mockWebServer.url("/test-redirect").toString()], "fallback")
      .resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "fallback"
    include.resolvedFragment.statusCode == 200
    include.resolvedFragmentSource == "fallback content"
    include.resolveTimeMillis > 0

    cleanup:
    mockWebServer.close()
  }

  def "should use cached fragment if not expired"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment from src")
      .build())
    mockWebServer.start()
    def includeSrcUrl = mockWebServer.url("/").toString()

    when:
    cache.set(includeSrcUrl, new Fragment(null, 200, "from cache", expirationTime, [:]))
    sleep(2000)
    def include = new Include("", ["src": includeSrcUrl])
      .resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == expectedFragment
    include.resolvedFragment.statusCode == 200
    include.resolvedFragmentSource == expectedFragmentSource

    cleanup:
    mockWebServer.close()

    where:
    expirationTime                | expectedFragment    | expectedFragmentSource
    Instant.now().plusSeconds(5)  | "from cache"        | "cached src"
    Instant.now().minusSeconds(5) | "fragment from src" | "remote src"
  }

  @Unroll
  def "should cache fragment if status code is defined as cacheable in RFC 7231 - Status #responseStatus"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.start()

    when:
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(responseStatus)
      .setHeader("Cache-Control", "max-age=7200")
      .body(srcFragment)
      .build())
    def includeSrcUrl = mockWebServer.url("/test-caching-" + UUID.randomUUID().toString()).toString()
    def include = new Include("", ["src": includeSrcUrl], ":(")
      .resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == expectedFragment
    include.resolvedFragment.statusCode == expectedFragmentStatusCode
    include.resolvedFragmentSource == (expectedFragment == ':(' ? 'fallback content' : 'remote src')

    if (expectedFragmentCached) {
      assert cache.get(includeSrcUrl).isPresent()
    } else {
      assert cache.get(includeSrcUrl).isEmpty()
    }

    cleanup:
    mockWebServer.close()

    where:
    responseStatus | srcFragment | expectedFragmentCached | expectedFragment | expectedFragmentStatusCode
    100            | "fragment"  | false                  | ":("             | 200
    200            | "fragment"  | true                   | "fragment"       | 200
    202            | "fragment"  | false                  | ":("             | 200
    203            | "fragment"  | true                   | "fragment"       | 203
    204            | ""          | true                   | ""               | 204
    205            | "fragment"  | false                  | ":("             | 200
    206            | "fragment"  | true                   | "fragment"       | 206
    // TODO: Testing status code 300 does not work on Java 11 because HttpClient fails with "IOException: Invalid redirection"
    // 300           | "fragment"  | true                   | ":("             | 200
    302            | "fragment"  | false                  | ":("             | 200
    400            | "fragment"  | false                  | ":("             | 200
    404            | "fragment"  | true                   | ":("             | 200
    405            | "fragment"  | true                   | ":("             | 200
    410            | "fragment"  | true                   | ":("             | 200
    414            | "fragment"  | true                   | ":("             | 200
    500            | "fragment"  | false                  | ":("             | 200
    501            | "fragment"  | true                   | ":("             | 200
    502            | "fragment"  | false                  | ":("             | 200
    503            | "fragment"  | false                  | ":("             | 200
    504            | "fragment"  | false                  | ":("             | 200
    505            | "fragment"  | false                  | ":("             | 200
    506            | "fragment"  | false                  | ":("             | 200
    507            | "fragment"  | false                  | ":("             | 200
    508            | "fragment"  | false                  | ":("             | 200
    509            | "fragment"  | false                  | ":("             | 200
    510            | "fragment"  | false                  | ":("             | 200
    511            | "fragment"  | false                  | ":("             | 200
  }

  def "should cache fragment for s-maxage seconds if directive is present"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment")
      .setHeader("Cache-Control", "max-age=3600, s-maxage=604800 , public")
      .setHeader("Expires", "Wed, 21 Oct 2015 07:28:00 GMT")
      .build())
    mockWebServer.start()
    def includeSrcUrl = mockWebServer.url("/test-s-maxage").toString()

    when:
    def include = new Include("", ["src": includeSrcUrl])
      .resolve(httpClient, [:], cache, config, supplyPool).get()
    def cacheExpirationTime = cache.get(includeSrcUrl).get().expirationTime

    then:
    include.resolved
    include.resolvedFragment.content == "fragment"
    cacheExpirationTime.isBefore(Instant.now().plusSeconds(604800).plusSeconds(1))
    cacheExpirationTime.isAfter(Instant.now().plusSeconds(604800).minusSeconds(1))

    cleanup:
    mockWebServer.close()
  }

  def "should cache fragment for max-age seconds if directive is present"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment")
      .setHeader("Cache-Control", "max-age=3600")
      .setHeader("Expires", "Wed, 21 Oct 2015 07:28:00 GMT")
      .build())
    mockWebServer.start()
    def includeSrcUrl = mockWebServer.url("/-test-max-age").toString()

    when:
    def include = new Include("", ["src": includeSrcUrl])
      .resolve(httpClient, [:], cache, config, supplyPool).get()
    def cacheExpirationTime = cache.get(includeSrcUrl).get().expirationTime

    then:
    include.resolved
    include.resolvedFragment.content == "fragment"
    cacheExpirationTime.isBefore(Instant.now().plusSeconds(3600).plusSeconds(1))
    cacheExpirationTime.isAfter(Instant.now().plusSeconds(3600).minusSeconds(1))

    cleanup:
    mockWebServer.close()
  }

  def "should treat http header names as case insensitive"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment")
      .setHeader("cache-control", "max-age=3600")
      .build())
    mockWebServer.start()
    def includeSrcUrl = mockWebServer.url("/").toString()

    when:
    def include = new Include("", ["src": includeSrcUrl])
      .resolve(httpClient, [:], cache, config, supplyPool).get()
    def cacheExpirationTime = cache.get(includeSrcUrl).get().expirationTime

    then:
    include.resolved
    include.resolvedFragment.content == "fragment"
    cacheExpirationTime.isBefore(Instant.now().plusSeconds(3600).plusSeconds(1))
    cacheExpirationTime.isAfter(Instant.now().plusSeconds(3600).minusSeconds(1))

    cleanup:
    mockWebServer.close()
  }

  def "should cache fragment for max-age seconds minus Age seconds if directive is present and Age header is set"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment")
      .setHeader("Cache-Control", "max-age=3600")
      .setHeader("Age", "600")
      .setHeader("Expires", "Wed, 21 Oct 2015 07:28:00 GMT")
      .build())
    mockWebServer.start()
    def includeSrcUrl = mockWebServer.url("/test-age-header").toString()

    when:
    def include = new Include("", ["src": includeSrcUrl])
      .resolve(httpClient, [:], cache, config, supplyPool).get()
    def cacheExpirationTime = cache.get(includeSrcUrl).get().expirationTime

    then:
    include.resolved
    include.resolvedFragment.content == "fragment"
    cacheExpirationTime.isBefore(Instant.now().plusSeconds(3000).plusSeconds(1))
    cacheExpirationTime.isAfter(Instant.now().plusSeconds(3000).minusSeconds(1))

    cleanup:
    mockWebServer.close()
  }

  def "should use absolute value of Age header for cache expiration calculation"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment")
      .setHeader("Cache-Control", "max-age=3600")
      .setHeader("Age", "-100")
      .setHeader("Expires", "Wed, 21 Oct 2015 07:28:00 GMT")
      .build())
    mockWebServer.start()
    def includeSrcUrl = mockWebServer.url("/test-age-header").toString()

    when:
    def include = new Include("", ["src": includeSrcUrl])
      .resolve(httpClient, [:], cache, config, supplyPool).get()
    def cacheExpirationTime = cache.get(includeSrcUrl).get().expirationTime

    then:
    include.resolved
    include.resolvedFragment.content == "fragment"
    cacheExpirationTime.isBefore(Instant.now().plusSeconds(3500).plusSeconds(1))
    cacheExpirationTime.isAfter(Instant.now().plusSeconds(3500).minusSeconds(1))

    cleanup:
    mockWebServer.close()
  }

  def "should cache fragment based on Expires header and current time if Cache-Control header and Date header are not present"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment")
      .setHeader("Cache-Control", "public")
      .setHeader("Expires", "Wed, 12 Oct 2050 07:28:00 GMT")
      .build())
    mockWebServer.start()
    def includeSrcUrl = mockWebServer.url("/test-expires-header").toString()

    when:
    def include = new Include("", ["src": includeSrcUrl])
      .resolve(httpClient, [:], cache, config, supplyPool).get()
    def cacheExpirationTime = cache.get(includeSrcUrl).get().expirationTime

    then:
    include.resolved
    include.resolvedFragment.content == "fragment"
    cacheExpirationTime == ZonedDateTime.parse("Wed, 12 Oct 2050 07:28:00 GMT", DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()

    cleanup:
    mockWebServer.close()
  }

  def "should handle Expires header with value 0"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment")
      .setHeader("Expires", "0")
      .build())
    mockWebServer.start()
    def includeSrcUrl = mockWebServer.url("/test-expires-header").toString()

    when:
    def include = new Include("", ["src": includeSrcUrl])
      .resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "fragment"
    cache.get(includeSrcUrl).isEmpty()

    cleanup:
    mockWebServer.close()
  }

  def "should cache fragment based on Expires and Date header if Cache-Control header is not present"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment")
      .setHeader("Date", "Wed, 05 Oct 2050 07:28:00 GMT")
      .setHeader("Expires", "Wed, 12 Oct 2050 07:28:00 GMT")
      .build())
    mockWebServer.start()
    def includeSrcUrl = mockWebServer.url("/test-expires-header").toString()

    when:
    def include = new Include("", ["src": includeSrcUrl])
      .resolve(httpClient, [:], cache, config, supplyPool).get()
    def cacheExpirationTime = cache.get(includeSrcUrl).get().expirationTime

    then:
    include.resolved
    include.resolvedFragment.content == "fragment"
    cacheExpirationTime.isBefore(Instant.now().plus(7, ChronoUnit.DAYS).plusSeconds(1))
    cacheExpirationTime.isAfter(Instant.now().plus(7, ChronoUnit.DAYS).minusSeconds(1))

    cleanup:
    mockWebServer.close()
  }

  def "should not cache fragment if Cache-Control header is set but without max-age directives"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment")
      .setHeader("Cache-Control", "no-cache,no-store,must-revalidate")
      .build())
    mockWebServer.start()
    def includeSrcUrl = mockWebServer.url("/test-caching").toString()

    when:
    def include = new Include("", ["src": includeSrcUrl])
      .resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "fragment"
    cache.get(includeSrcUrl).isEmpty()

    cleanup:
    mockWebServer.close()
  }

  def "should not crash when cache headers contain invalid values"() {
    given:
    def mockWebServer = new MockWebServer()

    when:
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment")
      .setHeader(header1Name, header1Value)
      .setHeader(header2Name, header2Value)
      .build())
    mockWebServer.start()
    def include = new Include("", ["src": mockWebServer.url("/test-should-not-crash").toString()])
      .resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolvedFragment.content == "fragment"

    cleanup:
    mockWebServer.close()

    where:
    header1Name     | header1Value                    | header2Name | header2Value
    "Cache-Control" | "s-maxage=not-numeric"          | "X-Dummy"   | "dummy"
    "Cache-Control" | "max-age=not-numeric"           | "X-Dummy"   | "dummy"
    "Cache-Control" | "max-age=3600"                  | "Age"       | "not-numeric"
    "Expires"       | "not-numeric"                   | "X-Dummy"   | "dummy"
    "Expires"       | "Wed, 12 Oct 2050 07:28:00 GMT" | "Date"      | "not-a-date"
  }

  def "should not cache fragment if no expiration time is indicated via response header"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment")
      .build())
    mockWebServer.start()
    def includeSrcUrl = mockWebServer.url("/test-default-cache-duration").toString()

    when:
    def include = new Include("", ["src": includeSrcUrl])
      .resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "fragment"
    cache.get(includeSrcUrl).isEmpty()

    cleanup:
    mockWebServer.close()
  }

  def "should apply request timeout for delayed header"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment from src")
      .headersDelay(2, TimeUnit.SECONDS)
      .build())
    mockWebServer.start()

    when:
    def include = new Include("", ["src": mockWebServer.url("/test-timeout-handling").toString()], "fallback")
      .resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "fallback"

    cleanup:
    mockWebServer.close()
  }

  def "should apply request timeout for delayed body"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment from src")
      .bodyDelay(2, TimeUnit.SECONDS)
      .build())
    mockWebServer.start()

    when:
    def include = new Include("", ["src": mockWebServer.url("/test-timeout-handling").toString()], "fallback")
      .resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolved
    include.resolvedFragment.content == "fallback"

    cleanup:
    mockWebServer.close()
  }

  def "should favor include tag specific request timeout over global one"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .body("fragment")
      .bodyDelay(1500, TimeUnit.MILLISECONDS)
      .build())
    mockWebServer.start()

    when:
    def attributes = new HashMap()
    attributes.put(srcAttributeName, mockWebServer.url("/test-timeout-handling").toString())
    attributes.putAll(timeoutAttribute)

    then:
    new Include("", attributes).resolve(httpClient, [:], cache, config, supplyPool).get()
      .resolvedFragment.content == expectedFragment

    cleanup:
    mockWebServer.close()

    where:
    srcAttributeName | timeoutAttribute               | expectedFragment
    "src"            | [:]                            | ""
    "src"            | ["src-timeout": "2s"]          | "fragment"
    "src"            | ["fallback-src-timeout": "2s"] | ""
    "fallback-src"   | [:]                            | ""
    "fallback-src"   | ["fallback-src-timeout": "2s"] | "fragment"
    "fallback-src"   | ["src-timeout": "2s"]          | ""
  }

  def "should forward headers defined via requestHeadersForward and requestHeadersForwardVary to fragment requests"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder().code(204).build())
    mockWebServer.start()
    def config = AbleronConfig.builder()
      .requestHeadersForward(["X-Header-1", "X-Header-2", "x-hEADEr-3"])
      .requestHeadersForwardVary(["X-HEADER-4", "x-header-5", "x-hEADEr-6"])
      .build()

    when:
    new Include("", ["src": mockWebServer.url("/").toString(), "headers": "x-heaDER-7,"])
      .resolve(httpClient, [
        "x-header-1": ["header1"],
        "X-HEADER-2": ["header2"],
        "X-Header-3": ["header3"],
        "x-header-4": ["header4"],
        "X-Header-5": ["header5"],
        "X-Header-6": ["header6"],
        "X-Header-7": ["header7"]
      ], cache, config, supplyPool).get()
    def fragmentRequest = mockWebServer.takeRequest()

    then:
    fragmentRequest.headers.get("X-header-1") == "header1"
    fragmentRequest.headers.get("X-header-2") == "header2"
    fragmentRequest.headers.get("X-header-3") == "header3"
    fragmentRequest.headers.get("X-header-4") == "header4"
    fragmentRequest.headers.get("X-header-5") == "header5"
    fragmentRequest.headers.get("X-header-6") == "header6"
    fragmentRequest.headers.get("X-header-7") == "header7"
    fragmentRequest.headers.get("X-header-8") == null

    cleanup:
    mockWebServer.close()
  }

  def "should forward headers defined via ableron-include headers-attribute to fragment requests"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder().code(204).build())
    mockWebServer.start()

    when:
    new Include("", ["src": mockWebServer.url("/").toString(), "headers": "X-Header1,X-Header2,x-hEADEr3"])
      .resolve(httpClient, ["X-Header1": ["header1"], "X-Header2": ["header2"], "X-HEADER3": ["header3"]], cache, config, supplyPool).get()
    def fragmentRequest = mockWebServer.takeRequest()

    then:
    fragmentRequest.headers.get("X-header1") == "header1"
    fragmentRequest.headers.get("X-header2") == "header2"
    fragmentRequest.headers.get("X-header3") == "header3"

    cleanup:
    mockWebServer.close()
  }

  def "should forward cookies defined via ableron-include cookies-attribute to fragment requests"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder().code(204).build())
    mockWebServer.start()

    when:
    new Include("", ["src": mockWebServer.url("/").toString(), "cookies": "UID,selected_tab, cID "])
      .resolve(httpClient, ["Cookie": ["foo=bar;  UID=user1 ; Uid=user%3B2; SELECTED_TAB=home; cID = 123"]], cache, config, supplyPool).get()
    def fragmentRequest = mockWebServer.takeRequest()

    then:
    fragmentRequest.headers.get("Cookie") == "UID=user1; cID = 123"

    cleanup:
    mockWebServer.close()
  }

  def "should not forward non-allowed request headers to fragment requests"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder().code(204).build())
    def config = AbleronConfig.builder()
      .requestHeadersForward([])
      .build()
    mockWebServer.start()

    when:
    new Include("", ["src": mockWebServer.url("/").toString()])
      .resolve(httpClient, ["X-Test":["Foo"]], cache, config, supplyPool).get()
    def fragmentRequest = mockWebServer.takeRequest()

    then:
    fragmentRequest.headers.get("X-Test") == null

    cleanup:
    mockWebServer.close()
  }

  def "should pass default User-Agent header to fragment requests"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder().code(204).build())
    mockWebServer.start()

    when:
    new Include("", ["src": mockWebServer.url("/").toString()])
      .resolve(httpClient, [:], cache, config, supplyPool).get()
    def fragmentRequest = mockWebServer.takeRequest()

    then:
    fragmentRequest.headers.get("User-Agent").equals("Ableron/2.0")

    cleanup:
    mockWebServer.close()
  }

  def "should forward provided User-Agent header to fragment requests if enabled via requestHeadersForward"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder().code(204).build())
    def config = AbleronConfig.builder()
      .requestHeadersForward(["User-Agent"])
      .build()
    mockWebServer.start()

    when:
    new Include("", ["src": mockWebServer.url("/").toString()])
      .resolve(httpClient, ["user-AGENT":["test"]], cache, config, supplyPool).get()
    def fragmentRequest = mockWebServer.takeRequest()

    then:
    fragmentRequest.headers.get("User-Agent") == "test"

    cleanup:
    mockWebServer.close()
  }

  def "should forward header with multiple values to fragment requests"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder().code(204).build())
    mockWebServer.start()
    def config = AbleronConfig.builder()
      .requestHeadersForward(["X-Test"])
      .build()

    when:
    new Include("", ["src": mockWebServer.url("/").toString()])
      .resolve(httpClient, ["X-Test":["Foo", "Bar", "Baz"]], cache, config, supplyPool).get()
    def fragmentRequest = mockWebServer.takeRequest()

    then:
    fragmentRequest.headers.values("X-Test") == ["Foo", "Bar", "Baz"]

    cleanup:
    mockWebServer.close()
  }

  def "should forward allowed response headers of primary fragment to transclusion result"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder().code(200)
      .addHeader("X-Test", "Test")
      .build())
    mockWebServer.start()
    def config = AbleronConfig.builder()
      .responseHeadersForward(["X-Test"])
      .build()

    when:
    def include = new Include("", ["src": mockWebServer.url("/").toString(), "primary": ""])
      .resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolvedFragment.responseHeaders == ["x-test": ["Test"]]

    cleanup:
    mockWebServer.close()
  }

  def "should not forward allowed response headers of non-primary fragment to transclusion result"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder().code(200)
      .addHeader("X-Test", "Test")
      .build())
    mockWebServer.start()

    when:
    def include = new Include("", ["src": mockWebServer.url("/").toString(), "primary": "false"])
      .resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolvedFragment.responseHeaders.isEmpty()

    cleanup:
    mockWebServer.close()
  }

  def "should treat fragment response headers allow list as case insensitive"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder().code(200)
      .addHeader("x-test", "Test")
      .build())
    mockWebServer.start()
    def config = AbleronConfig.builder()
      .responseHeadersForward(["X-TeSt"])
      .build()

    when:
    def include = new Include("", ["src": mockWebServer.url("/").toString(), "primary": ""])
      .resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolvedFragment.responseHeaders == ["x-test": ["Test"]]

    cleanup:
    mockWebServer.close()
  }

  def "should forward primary fragment response header with multiple values to transclusion result"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder().code(200)
      .addHeader("X-Test", "Test")
      .addHeader("X-Test", "Test2")
      .build())
    mockWebServer.start()
    def config = AbleronConfig.builder()
      .responseHeadersForward(["X-TEST"])
      .build()

    when:
    def include = new Include("", ["src": mockWebServer.url("/").toString(), "primary": ""])
      .resolve(httpClient, [:], cache, config, supplyPool).get()

    then:
    include.resolvedFragment.responseHeaders.get("x-test") == ["Test", "Test2"]

    cleanup:
    mockWebServer.close()
  }

  def "should consider requestHeadersForwardVary"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .setHeader("Cache-Control", "max-age=30")
      .body("X-AB-Test=A")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .setHeader("Cache-Control", "max-age=30")
      .body("X-AB-Test=B")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .setHeader("Cache-Control", "max-age=30")
      .body("X-AB-Test=omitted")
      .build())
    mockWebServer.start()
    def config = AbleronConfig.builder()
      .requestHeadersForward(["x-ab-TEST"])
      .requestHeadersForwardVary(["x-AB-test"])
      .build()

    when:
    def include1 = new Include("", ["src": mockWebServer.url("/").toString()])
      .resolve(httpClient, ["X-AB-TEST": ["A"]], cache, config, supplyPool).get()
    def include2 = new Include("", ["src": mockWebServer.url("/").toString()])
      .resolve(httpClient, ["X-AB-TEST": ["A"]], cache, config, supplyPool).get()
    def include3 = new Include("", ["src": mockWebServer.url("/").toString()])
      .resolve(httpClient, ["X-AB-TEST": ["B"]], cache, config, supplyPool).get()
    def include4 = new Include("", ["src": mockWebServer.url("/").toString()])
      .resolve(httpClient, ["X-AB-TEST": ["B"], "X-Foo": ["Bar"]], cache, config, supplyPool).get()
    def include5 = new Include("", ["src": mockWebServer.url("/").toString()])
      .resolve(httpClient, [:], cache, config, supplyPool).get()
    def include6 = new Include("", ["src": mockWebServer.url("/").toString()])
      .resolve(httpClient, ["x-ab-test": ["A"]], cache, config, supplyPool).get()

    then:
    include1.resolvedFragment.content == "X-AB-Test=A"
    include2.resolvedFragment.content == "X-AB-Test=A"
    include3.resolvedFragment.content == "X-AB-Test=B"
    include4.resolvedFragment.content == "X-AB-Test=B"
    include5.resolvedFragment.content == "X-AB-Test=omitted"
    include6.resolvedFragment.content == "X-AB-Test=A"

    cleanup:
    mockWebServer.close()
  }

  def "should use consistent order of requestHeadersForwardVary for cache key generation"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .setHeader("Cache-Control", "max-age=30")
      .body("A,B,C")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .setHeader("Cache-Control", "max-age=30")
      .body("A,B,B")
      .build())
    mockWebServer.start()
    def config = AbleronConfig.builder()
      .requestHeadersForward(["x-test-A", "x-test-B", "x-test-C"])
      .requestHeadersForwardVary(["X-Test-A", "X-Test-B", "X-Test-C"])
      .build()

    when:
    def include1 = new Include("", ["src": mockWebServer.url("/").toString()])
      .resolve(httpClient, ["X-TEST-A": ["A"], "X-Test-B": ["B"], "X-Test-C": ["C"]], cache, config, supplyPool).get()
    def include2 = new Include("", ["src": mockWebServer.url("/").toString()])
      .resolve(httpClient, ["X-TEST-B": ["B"], "X-TEST-A": ["A"], "X-Test-C": ["C"]], cache, config, supplyPool).get()
    def include3 = new Include("", ["src": mockWebServer.url("/").toString()])
      .resolve(httpClient, ["X-TEST-C": ["C"], "X-test-B": ["B"], "X-Test-A": ["A"]], cache, config, supplyPool).get()
    def include4 = new Include("", ["src": mockWebServer.url("/").toString()])
      .resolve(httpClient, ["x-test-c": ["B"], "x-test-b": ["B"], "x-test-a": ["A"]], cache, config, supplyPool).get()

    then:
    include1.resolvedFragment.content == "A,B,C"
    include2.resolvedFragment.content == "A,B,C"
    include3.resolvedFragment.content == "A,B,C"
    include4.resolvedFragment.content == "A,B,B"

    cleanup:
    mockWebServer.close()
  }

  def "should consider request headers defined in headers attribute for cache key generation"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .setHeader("Cache-Control", "max-age=30")
      .body("A,B,C")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .setHeader("Cache-Control", "max-age=30")
      .body("B")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .setHeader("Cache-Control", "max-age=30")
      .body("A,B,B")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .setHeader("Cache-Control", "max-age=30")
      .body("B from Cookie")
      .build())
    mockWebServer.start()

    when:
    def include1 = new Include("", ["src": mockWebServer.url("/").toString(), "headers": "X-Test-A,X-Test-B,X-Test-C"])
      .resolve(httpClient, ["X-TEST-B": ["B"], "X-Test-C": ["C"], "X-Test-A": ["A"]], cache, config, supplyPool).get()
    def include2 = new Include("", ["src": mockWebServer.url("/").toString(), "headers": "X-Test-A,X-Test-B,X-Test-C"])
      .resolve(httpClient, ["X-TEST-B": ["B"], "X-TEST-A": ["A"], "X-Test-C": ["C"]], cache, config, supplyPool).get()
    def include3 = new Include("", ["src": mockWebServer.url("/").toString(), "headers": "x-test-b"])
      .resolve(httpClient, ["X-TEST-C": ["C"], "X-test-B": ["B"], "X-Test-A": ["A"]], cache, config, supplyPool).get()
    def include4 = new Include("", ["src": mockWebServer.url("/").toString(), "headers": "X-Test-B,X-Test-C,X-Test-A"])
      .resolve(httpClient, ["x-test-c": ["B"], "x-test-b": ["B"], "x-test-a": ["A"]], cache, config, supplyPool).get()
    def include5 = new Include("", ["src": mockWebServer.url("/").toString(), "headers": "X-TEST-C, X-Test-A,X-Test-B"])
      .resolve(httpClient, ["X-TEST-B": ["B"], "X-Test-C": ["C"], "X-Test-A": ["A"], "X-Test-D": ["D"]], cache, config, supplyPool).get()
    def include6 = new Include("", ["src": mockWebServer.url("/").toString(), "headers": "x-test-b", "cookies": "x-test-b"])
      .resolve(httpClient, ["Cookie": ["x-test-b=B"]], cache, config, supplyPool).get()

    then:
    include1.resolvedFragment.content == "A,B,C"
    include2.resolvedFragment.content == "A,B,C"
    include3.resolvedFragment.content == "B"
    include4.resolvedFragment.content == "A,B,B"
    include5.resolvedFragment.content == "A,B,C"
    include6.resolvedFragment.content == "B from Cookie"

    cleanup:
    mockWebServer.close()
  }

  def "should consider cookies defined in cookies attribute for cache key generation"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .setHeader("Cache-Control", "max-age=30")
      .body("req1")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .setHeader("Cache-Control", "max-age=30")
      .body("req2")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .setHeader("Cache-Control", "max-age=30")
      .body("req3")
      .build())
    mockWebServer.enqueue(new MockResponse.Builder()
      .code(200)
      .setHeader("Cache-Control", "max-age=30")
      .body("req4")
      .build())
    mockWebServer.start()

    when:
    def include1 = new Include("", ["src": mockWebServer.url("/").toString(), "headers": "X-Test-A", "cookies": "UID,ab_test"])
      .resolve(httpClient, ["X-Test-A": ["A"], "Cookie": ["foo=bar;UID=1;ab_test=x"]], cache, config, supplyPool).get()
    def include2 = new Include("", ["src": mockWebServer.url("/").toString(), "headers": "X-Test-A", "cookies": "UID,ab_test"])
      .resolve(httpClient, ["X-Test-A": ["A"], "Cookie": ["foo=bar;ab_test=x"]], cache, config, supplyPool).get()
    def include3 = new Include("", ["src": mockWebServer.url("/").toString(), "headers": "X-Test-A", "cookies": "UID,ab_test"])
      .resolve(httpClient, ["X-Test-A": ["A"], "Cookie": ["foo=bar;UID=2;ab_test=x"]], cache, config, supplyPool).get()
    def include4 = new Include("", ["src": mockWebServer.url("/").toString(), "headers": "X-Test-A", "cookies": "UID,ab_test"])
      .resolve(httpClient, ["x-test-a": ["A"], "Cookie": ["ab_test=x; UID=1"]], cache, config, supplyPool).get()
    def include5 = new Include("", ["src": mockWebServer.url("/").toString(), "headers": "X-Test-A", "cookies": "UID,ab_test"])
      .resolve(httpClient, ["Cookie": ["ab_test=x"]], cache, config, supplyPool).get()
    def include6 = new Include("", ["src": mockWebServer.url("/").toString(), "headers": "X-Test-A", "cookies": "UID,ab_test"])
      .resolve(httpClient, ["X-Test-B": ["B"], "Cookie": ["ab_test=x;a=a"]], cache, config, supplyPool).get()

    then:
    include1.resolvedFragment.content == "req1"
    include2.resolvedFragment.content == "req2"
    include3.resolvedFragment.content == "req3"
    include4.resolvedFragment.content == "req1"
    include5.resolvedFragment.content == "req4"
    include6.resolvedFragment.content == "req4"

    cleanup:
    mockWebServer.close()
  }

  def "should configure auto refresh for cached Fragments"() {
    given:
    def mockWebServer = new MockWebServer()
    mockWebServer.setDispatcher(new Dispatcher() {
      @Override
      MockResponse dispatch(RecordedRequest recordedRequest) throws InterruptedException {
        return new MockResponse.Builder()
          .code(200)
          .setHeader("Cache-Control", "max-age=1")
          .body("fragment")
          .build()
      }
    })
    mockWebServer.start()
    def config = AbleronConfig.builder()
      .cacheAutoRefreshEnabled(true)
      .build()
    def fragmentCache = new FragmentCache(AbleronConfig.builder()
      .cacheAutoRefreshEnabled(true)
      .build())

    when:
    for (def i = 0; i < 4; i++) {
      new Include("", ["src": mockWebServer.url("/").toString()])
        .resolve(httpClient, [:], fragmentCache, config, supplyPool).get()
      sleep(1000)
    }

    then:
    fragmentCache.stats().hitCount() == 3
    fragmentCache.stats().missCount() == 1
    fragmentCache.stats().refreshSuccessCount() >= 3
    fragmentCache.stats().refreshFailureCount() == 0

    cleanup:
    mockWebServer.close()
  }
}
