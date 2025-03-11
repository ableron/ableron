package io.github.ableron.springboot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

@ConfigurationProperties(prefix = "ableron")
public class AbleronProperties {

  /**
   * Whether Ableron UI composition is enabled.
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

  private final Cache cache = new Cache();

  private final Stats stats = new Stats();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public Collection<String> getRequestHeadersForward() {
    return requestHeadersForward;
  }

  public void setRequestHeadersForward(Collection<String> requestHeadersForward) {
    this.requestHeadersForward = requestHeadersForward;
  }

  public Collection<String> getRequestHeadersForwardVary() {
    return requestHeadersForwardVary;
  }

  public void setRequestHeadersForwardVary(Collection<String> requestHeadersForwardVary) {
    this.requestHeadersForwardVary = requestHeadersForwardVary;
  }

  public Collection<String> getResponseHeadersForward() {
    return responseHeadersForward;
  }

  public void setResponseHeadersForward(Collection<String> responseHeadersForward) {
    this.responseHeadersForward = responseHeadersForward;
  }

  public Cache getCache() {
    return cache;
  }

  public Stats getStats() {
    return stats;
  }

  public static class Cache {

    /**
     * Maximum size, the fragment cache may have.
     */
    private DataSize maxSize = DataSize.ofMegabytes(50);

    /**
     * Whether to enable auto-refreshing of cached fragments, before they expire.
     */
    private boolean autoRefreshEnabled = false;

    /**
     * Maximum number of attempts to refresh a cached fragment.
     */
    private int autoRefreshMaxAttempts = 3;

    /**
     * Maximum number of consecutive refreshs of inactive cached fragments.
     */
    private int autoRefreshInactiveFragmentsMaxRefreshs = 2;

    public DataSize getMaxSize() {
      return maxSize;
    }

    public void setMaxSize(DataSize maxSize) {
      this.maxSize = maxSize;
    }

    public boolean isAutoRefreshEnabled() {
      return autoRefreshEnabled;
    }

    public void setAutoRefreshEnabled(boolean autoRefreshEnabled) {
      this.autoRefreshEnabled = autoRefreshEnabled;
    }

    public int getAutoRefreshMaxAttempts() {
      return autoRefreshMaxAttempts;
    }

    public void setAutoRefreshMaxAttempts(int autoRefreshMaxAttempts) {
      this.autoRefreshMaxAttempts = autoRefreshMaxAttempts;
    }

    public int getAutoRefreshInactiveFragmentsMaxRefreshs() {
      return autoRefreshInactiveFragmentsMaxRefreshs;
    }

    public void setAutoRefreshInactiveFragmentsMaxRefreshs(int autoRefreshInactiveFragmentsMaxRefreshs) {
      this.autoRefreshInactiveFragmentsMaxRefreshs = autoRefreshInactiveFragmentsMaxRefreshs;
    }
  }

  public static class Stats {

    /**
     * Whether to append UI composition stats as HTML comment to the content.
     */
    private boolean appendToContent = false;

    /**
     * Whether to expose fragment URLs in the stats appended to the content.
     */
    private boolean exposeFragmentUrl = false;

    public boolean isAppendToContent() {
      return appendToContent;
    }

    public void setAppendToContent(boolean appendToContent) {
      this.appendToContent = appendToContent;
    }

    public boolean isExposeFragmentUrl() {
      return exposeFragmentUrl;
    }

    public void setExposeFragmentUrl(boolean exposeFragmentUrl) {
      this.exposeFragmentUrl = exposeFragmentUrl;
    }
  }
}
