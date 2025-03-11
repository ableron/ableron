package io.github.ableron.springboot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
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
  private Duration fragmentRequestTimeout = Duration.ofSeconds(3);

  /**
   * Request headers that are passed to fragment requests if present.
   */
  private List<String> fragmentRequestHeadersToPass = List.of(
    "Accept-Language",
    "Correlation-ID",
    "Forwarded",
    "Referer",
    "User-Agent",
    "X-Correlation-ID",
    "X-Forwarded-For",
    "X-Forwarded-Proto",
    "X-Forwarded-Host",
    "X-Real-IP",
    "X-Request-ID"
  );

  /**
   * Extends `fragmentRequestHeadersToPass`. Use this property to pass all headers defined in
   * `fragmentRequestHeadersToPass` plus the additional headers defined here. This prevents
   * the need to duplicate `fragmentRequestHeadersToPass` if the only use case is to add
   * additional headers instead of modifying the default ones.
   */
  private List<String> fragmentAdditionalRequestHeadersToPass = List.of();

  /**
   * Response headers of primary fragments to pass to the page response if present.
   */
  private List<String> primaryFragmentResponseHeadersToPass = List.of(
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

  public Duration getFragmentRequestTimeout() {
    return fragmentRequestTimeout;
  }

  public void setFragmentRequestTimeout(Duration fragmentRequestTimeout) {
    this.fragmentRequestTimeout = fragmentRequestTimeout;
  }

  public List<String> getFragmentRequestHeadersToPass() {
    return fragmentRequestHeadersToPass;
  }

  public void setFragmentRequestHeadersToPass(List<String> fragmentRequestHeadersToPass) {
    this.fragmentRequestHeadersToPass = fragmentRequestHeadersToPass;
  }

  public List<String> getFragmentAdditionalRequestHeadersToPass() {
    return fragmentAdditionalRequestHeadersToPass;
  }

  public void setFragmentAdditionalRequestHeadersToPass(List<String> fragmentAdditionalRequestHeadersToPass) {
    this.fragmentAdditionalRequestHeadersToPass = fragmentAdditionalRequestHeadersToPass;
  }

  public List<String> getPrimaryFragmentResponseHeadersToPass() {
    return primaryFragmentResponseHeadersToPass;
  }

  public void setPrimaryFragmentResponseHeadersToPass(List<String> primaryFragmentResponseHeadersToPass) {
    this.primaryFragmentResponseHeadersToPass = primaryFragmentResponseHeadersToPass;
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
     * Fragment request headers which influence the requested fragment aside from its URL.
     */
    private List<String> varyByRequestHeaders = List.of();

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

    public List<String> getVaryByRequestHeaders() {
      return varyByRequestHeaders;
    }

    public void setVaryByRequestHeaders(List<String> varyByRequestHeaders) {
      this.varyByRequestHeaders = varyByRequestHeaders;
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
