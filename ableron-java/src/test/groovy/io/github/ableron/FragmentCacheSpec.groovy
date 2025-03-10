package io.github.ableron

import spock.lang.Specification

import java.time.Duration
import java.time.Instant

class FragmentCacheSpec extends Specification {

  def fragmentCache = new TransclusionProcessor(AbleronConfig.builder()
    .requestTimeout(Duration.ofSeconds(1))
    .cacheAutoRefreshEnabled(true)
    .build()).getFragmentCache()

  def "should have limited capacity to prevent out of memory problems"() {
    given:
    def fragmentCache = new TransclusionProcessor(AbleronConfig.builder()
      .cacheMaxSizeInBytes(500)
      .build()).getFragmentCache()

    when:
    for (int i = 0; i < 160; i++) {
      // using "9999 - i" as key and "a" as content, we should have 5 bytes per fragment
      fragmentCache.set("" + (9999 - i), new Fragment(null, 200, "a", Instant.now().plusSeconds(5), [:]))
    }
    sleep(25)

    then:
    fragmentCache.stats().itemCount() == 100
  }

  def "should not auto refresh fragments if disabled"() {
    given:
    def fragmentCache = new TransclusionProcessor(AbleronConfig.builder()
      .cacheAutoRefreshEnabled(false)
      .build()).getFragmentCache()
    def newFragment = () -> new Fragment('url', 200, 'fragment', Instant.now().plusSeconds(1), [:])
    fragmentCache.set('cacheKey', newFragment(), () -> newFragment())

    when:
    sleep(1200)

    then:
    fragmentCache.get('cacheKey').isEmpty()
  }

  def "should auto refresh fragments if enabled"() {
    given:
    def newFragment = () -> new Fragment('url', 200, 'fragment', Instant.now().plusSeconds(1), [:])
    fragmentCache.set('cacheKey', newFragment(), () -> newFragment())

    when:
    sleep(1200)

    then:
    fragmentCache.get('cacheKey').isPresent()
  }

  def "should handle cache refresh failure"() {
    given:
    fragmentCache.set('cacheKey', new Fragment('url', 200, 'fragment', Instant.now().plusSeconds(1), [:]), () -> null)

    when:
    sleep(1200)

    then:
    fragmentCache.get('cacheKey').isEmpty()
  }

  def "should use fragment expiration time as cache entry ttl"() {
    given:
    fragmentCache.set('cacheKey', new Fragment('url', 200, 'fragment', Instant.now().plusSeconds(1), [:]))

    expect:
    fragmentCache.get('cacheKey').get().content == 'fragment'

    when:
    sleep(1010)

    then:
    fragmentCache.get('cacheKey').isEmpty()
  }

  def "should not cache expired fragments"() {
    when:
    fragmentCache.set('cacheKey', new Fragment('url', 200, 'fragment', Instant.now(), [:]))

    then:
    fragmentCache.get('cacheKey').isEmpty()
  }

  def "should clear cache"() {
    given:
    def newFragment = () -> new Fragment('url', 200, 'fragment', Instant.now().plusMillis(280), [:])
    fragmentCache.set('cacheKey', newFragment(), () -> newFragment())

    expect:
    fragmentCache.get('cacheKey').isPresent()
    sleep(300)
    fragmentCache.get('cacheKey').isPresent()
    fragmentCache.clear()
    fragmentCache.get('cacheKey').isEmpty()
    sleep(300)
    fragmentCache.get('cacheKey').isEmpty()
  }

  def "should not auto refresh cached fragment when status code is not cacheable"() {
    given:
    def newFragment = (int status) -> new Fragment('url', status, 'fragment', Instant.now().plusMillis(300), [:])
    fragmentCache.set('cacheKey', newFragment(200), () -> newFragment(500))

    expect:
    fragmentCache.get('cacheKey').isPresent()
    sleep(300)
    fragmentCache.get('cacheKey').isEmpty()
  }

  def "should not auto refresh cached fragment when fragment is marked as not cacheable"() {
    given:
    fragmentCache.set('cacheKey', new Fragment('url', 200, 'fragment', Instant.now().plusMillis(250), [:]), () ->
      new Fragment(200, 'fragment')
    )

    expect:
    fragmentCache.get('cacheKey').isPresent()
    sleep(300)
    fragmentCache.get('cacheKey').isEmpty()
  }

  def "should continuously refresh cache"() {
    given:
    def newFragment = () -> new Fragment('url', 200, 'fragment', Instant.now().plusMillis(200), [:])
    fragmentCache.set('cacheKey', newFragment(), () -> newFragment())

    expect:
    fragmentCache.get('cacheKey').isPresent()
    sleep(250)
    fragmentCache.get('cacheKey').isPresent()
    sleep(250)
    fragmentCache.get('cacheKey').isPresent()
    sleep(250)
    fragmentCache.get('cacheKey').isPresent()
  }

  def "should make 3 attempts to refresh cached fragments"() {
    given:
    def counter = 0
    def newFragment = () -> {
      counter++

      switch (counter) {
        case 1:
        case 4:
        case 8:
          return new Fragment('url', 200, 'fragment', Instant.now().plusSeconds(1), [:])
        default:
          throw new Exception('Refresh failed!')
      }
    }
    fragmentCache.set('cacheKey', newFragment(), () -> newFragment())

    expect:
    fragmentCache.get('cacheKey').isPresent()
    sleep(1200)
    fragmentCache.get('cacheKey').isEmpty()
    sleep(1000)
    fragmentCache.get('cacheKey').isEmpty()
    sleep(1000)
    fragmentCache.get('cacheKey').isPresent()

    sleep(1200)
    fragmentCache.get('cacheKey').isEmpty()
    sleep(1000)
    fragmentCache.get('cacheKey').isEmpty()
    sleep(1000)
    fragmentCache.get('cacheKey').isEmpty()
    sleep(1000)
    fragmentCache.get('cacheKey').isEmpty()
  }

  def "should not pollute stats when refreshing cache"() {
    given:
    def newFragment = () -> new Fragment('url', 200, 'fragment', Instant.now().plusMillis(200), [:])
    def fragmentCache = new FragmentCache(AbleronConfig.builder()
      .cacheAutoRefreshEnabled(true)
      .cacheAutoRefreshInactiveFragmentsMaxRefreshs(4)
      .build())
    fragmentCache.set('testShouldNotPolluteStats', newFragment(), () -> newFragment())

    expect:
    fragmentCache.stats().hitCount() == 0
    fragmentCache.stats().missCount() == 0
    fragmentCache.stats().refreshSuccessCount() == 0
    fragmentCache.stats().refreshFailureCount() == 0
    sleep(750)
    fragmentCache.stats().hitCount() == 0
    fragmentCache.stats().missCount() == 0
    fragmentCache.stats().refreshSuccessCount() == 4
    fragmentCache.stats().refreshFailureCount() == 0
  }

  def "should stop refreshing unused fragments with cacheAutoRefreshInactiveFragmentsMaxRefreshs=1"() {
    given:
    def newFragment = () -> new Fragment('url', 200, 'fragment', Instant.now().plusMillis(200), [:])
    def fragmentCache = new FragmentCache(AbleronConfig.builder()
      .cacheAutoRefreshEnabled(true)
      .cacheAutoRefreshInactiveFragmentsMaxRefreshs(1)
      .build())

    when:
    fragmentCache.set('key', newFragment(), () -> newFragment())
    sleep(400)

    then:
    fragmentCache.stats().refreshSuccessCount() == 1

    when:
    fragmentCache.set('key', newFragment(), () -> newFragment())
    fragmentCache.get('key')
    sleep(600)

    then:
    fragmentCache.stats().refreshSuccessCount() == 3
  }

  def "should stop refreshing unused fragments with cacheAutoRefreshInactiveFragmentsMaxRefreshs=0"() {
    given:
    def newFragment = () -> new Fragment('url', 200, 'fragment', Instant.now().plusMillis(200), [:])
    def fragmentCache = new FragmentCache(AbleronConfig.builder()
      .cacheAutoRefreshEnabled(true)
      .cacheAutoRefreshInactiveFragmentsMaxRefreshs(0)
      .build())

    when:
    fragmentCache.set('key', newFragment(), () -> newFragment())
    sleep(400)

    then:
    fragmentCache.stats().refreshSuccessCount() == 0

    when:
    fragmentCache.set('key', newFragment(), () -> newFragment())
    fragmentCache.get('key')
    sleep(600)

    then:
    fragmentCache.stats().refreshSuccessCount() == 1
  }
}
