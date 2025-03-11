package io.github.ableron.springboot.autoconfigure;

import io.github.ableron.AbleronConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
  classes = { AbleronAutoConfiguration.class },
  properties = {
    "ableron.enabled=true",
    "ableron.fragment-request-timeout=5000",
    "ableron.fragment-request-headers-to-pass=X-Test-Foo,X-Test-Bar,X-Test-Baz",
    "ableron.primary-fragment-response-headers-to-pass=X-Correlation-ID",
    "ableron.cache.max-size=2MB",
    "ableron.cache.vary-by-request-headers=X-Foo,X-Bar",
    "ableron.cache.auto-refresh-enabled=true",
    "ableron.cache.auto-refresh-max-attempts=5",
    "ableron.cache.auto-refresh-inactive-fragments-max-refreshs=6",
    "ableron.stats.append-to-content=true",
    "ableron.stats.expose-fragment-url=true"
  }
)
public class AbleronPropertiesTest {

  @Autowired
  private AbleronConfig ableronConfig;

  @Test
  public void shouldCoverWholeAbleronJavaConfig() {
    assertTrue(ableronConfig.isEnabled());
    assertEquals(Duration.ofMillis(5000), ableronConfig.getFragmentRequestTimeout());
    assertEquals(List.of(
      "X-Test-Foo",
      "X-Test-Bar",
      "X-Test-Baz"
    ), ableronConfig.getFragmentRequestHeadersToPass());
    assertEquals(List.of("X-Correlation-ID"), ableronConfig.getPrimaryFragmentResponseHeadersToPass());
    assertEquals(2097152, ableronConfig.getCacheMaxSizeInBytes());
    assertEquals(List.of("X-Foo", "X-Bar"), ableronConfig.getCacheVaryByRequestHeaders());
    assertTrue(ableronConfig.cacheAutoRefreshEnabled());
    assertEquals(5, ableronConfig.getCacheAutoRefreshMaxAttempts());
    assertEquals(6, ableronConfig.getCacheAutoRefreshInactiveFragmentsMaxRefreshs());
    assertTrue(ableronConfig.statsAppendToContent());
    assertTrue(ableronConfig.statsExposeFragmentUrl());
  }
}
