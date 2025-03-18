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
    "ableron.request-timeout=5000",
    "ableron.request-headers-forward=X-Test-Foo,X-Test-Bar,X-Test-Baz",
    "ableron.request-headers-forward-vary=X-Foo,X-Bar",
    "ableron.response-headers-forward=X-Correlation-ID",
    "ableron.cache.max-size=2MB",
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
    assertEquals(Duration.ofMillis(5000), ableronConfig.getRequestTimeout());
    assertEquals(List.of(
      "X-Test-Foo",
      "X-Test-Bar",
      "X-Test-Baz"
    ), ableronConfig.getRequestHeadersForward());
    assertEquals(List.of(
      "X-Foo",
      "X-Bar"
    ), ableronConfig.getRequestHeadersForwardVary());
    assertEquals(List.of("X-Correlation-ID"), ableronConfig.getResponseHeadersForward());
    assertEquals(2097152, ableronConfig.getCacheMaxSizeInBytes());
    assertTrue(ableronConfig.cacheAutoRefreshEnabled());
    assertEquals(5, ableronConfig.getCacheAutoRefreshMaxAttempts());
    assertEquals(6, ableronConfig.getCacheAutoRefreshInactiveFragmentsMaxRefreshs());
    assertTrue(ableronConfig.statsAppendToContent());
    assertTrue(ableronConfig.statsExposeFragmentUrl());
  }
}
