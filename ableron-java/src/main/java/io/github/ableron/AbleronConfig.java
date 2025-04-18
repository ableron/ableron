package io.github.ableron;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AbleronConfig {

  /**
   * Whether UI composition is enabled.
   * Defaults to true.
   */
  private boolean enabled = true;

  /**
   * Timeout for requesting fragments.
   */
  private Duration requestTimeout = Duration.ofSeconds(3);

  /**
   * Request headers that are forwarded to fragment requests, if present.
   * These request headers are not considered to influence the response and thus will not influence caching.
   */
  private Collection<String> requestHeadersForward = List.of(
    "Correlation-ID",
    "X-Correlation-ID",
    "X-Request-ID"
  );

  /**
   * Request headers that are forwarded to fragment requests, if present and that influence the
   * requested fragment aside from its URL.
   * These request headers are considered to influence the response and thus influence caching.
   */
  private Collection<String> requestHeadersForwardVary = List.of();

  /**
   * Response headers of primary fragments to forward to the page response, if present.
   */
  private Collection<String> responseHeadersForward = List.of(
    "Content-Language",
    "Location",
    "Refresh"
  );

  /**
   * Maximum size in bytes the fragment cache may have.
   * Defaults to 50 MB.
   */
  private long cacheMaxSizeInBytes = 1024 * 1024 * 50;

  /**
   * Whether to enable auto-refreshing of cached fragments.
   */
  private boolean cacheAutoRefreshEnabled = false;

  /**
   * Maximum number of attempts to refresh a cached fragment.
   */
  private int cacheAutoRefreshMaxAttempts = 3;

  /**
   * Maximum number of consecutive refreshs of inactive cached fragments.<br>
   * Fragments are considered inactive, if they have not been read from cache
   * between writing to cache and a refresh attempt.
   */
  private int cacheAutoRefreshInactiveFragmentsMaxRefreshs = 2;

  /**
   * Whether to append UI composition stats as HTML comment to the content.
   * Defaults to false.
   */
  private boolean statsAppendToContent = false;

  /**
   * Whether to expose fragment URLs in the stats appended to the content.
   * Defaults to false.
   */
  private boolean statsExposeFragmentUrl = false;

  private AbleronConfig() {}

  public static Builder builder() {
    return new Builder();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public Collection<String> getRequestHeadersForward() {
    return requestHeadersForward;
  }

  public Collection<String> getRequestHeadersForwardVary() {
    return requestHeadersForwardVary;
  }

  public Collection<String> getResponseHeadersForward() {
    return responseHeadersForward;
  }

  public long getCacheMaxSizeInBytes() {
    return cacheMaxSizeInBytes;
  }

  public boolean cacheAutoRefreshEnabled() {
    return cacheAutoRefreshEnabled;
  }

  public int getCacheAutoRefreshMaxAttempts() {
    return cacheAutoRefreshMaxAttempts;
  }

  public int getCacheAutoRefreshInactiveFragmentsMaxRefreshs() {
    return cacheAutoRefreshInactiveFragmentsMaxRefreshs;
  }

  public boolean statsAppendToContent() {
    return statsAppendToContent;
  }

  public boolean statsExposeFragmentUrl() {
    return statsExposeFragmentUrl;
  }

  public static class Builder {

    private final AbleronConfig ableronConfig = new AbleronConfig();

    public Builder enabled(boolean enabled) {
      ableronConfig.enabled = enabled;
      return this;
    }

    public Builder requestTimeout(Duration requestTimeout) {
      ableronConfig.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
      return this;
    }

    public Builder requestHeadersForward(Collection<String> requestHeadersForward) {
      Objects.requireNonNull(requestHeadersForward, "requestHeadersForward must not be null");
      ableronConfig.requestHeadersForward = requestHeadersForward.stream().collect(Collectors.toUnmodifiableList());
      return this;
    }

    public Builder requestHeadersForwardVary(Collection<String> requestHeadersForwardVary) {
      Objects.requireNonNull(requestHeadersForwardVary, "requestHeadersForwardVary must not be null");
      ableronConfig.requestHeadersForwardVary = requestHeadersForwardVary.stream().collect(Collectors.toUnmodifiableList());
      return this;
    }

    public Builder responseHeadersForward(Collection<String> responseHeadersForward) {
      Objects.requireNonNull(responseHeadersForward, "responseHeadersForward must not be null");
      ableronConfig.responseHeadersForward = responseHeadersForward.stream().collect(Collectors.toUnmodifiableList());
      return this;
    }

    public Builder cacheMaxSizeInBytes(long cacheMaxSizeInBytes) {
      ableronConfig.cacheMaxSizeInBytes = cacheMaxSizeInBytes;
      return this;
    }

    public Builder statsAppendToContent(boolean statsAppendToContent) {
      ableronConfig.statsAppendToContent = statsAppendToContent;
      return this;
    }

    public Builder cacheAutoRefreshEnabled(boolean cacheAutoRefreshEnabled) {
      ableronConfig.cacheAutoRefreshEnabled = cacheAutoRefreshEnabled;
      return this;
    }

    public Builder cacheAutoRefreshMaxAttempts(int cacheAutoRefreshMaxAttempts) {
      ableronConfig.cacheAutoRefreshMaxAttempts = cacheAutoRefreshMaxAttempts;
      return this;
    }

    public Builder cacheAutoRefreshInactiveFragmentsMaxRefreshs(int cacheAutoRefreshInactiveFragmentsMaxRefreshs) {
      ableronConfig.cacheAutoRefreshInactiveFragmentsMaxRefreshs = cacheAutoRefreshInactiveFragmentsMaxRefreshs;
      return this;
    }

    public Builder statsExposeFragmentUrl(boolean statsExposeFragmentUrl) {
      ableronConfig.statsExposeFragmentUrl = statsExposeFragmentUrl;
      return this;
    }

    public AbleronConfig build() {
      return ableronConfig;
    }
  }
}
