export default class AbleronConfig {
  /**
   * Whether UI composition is enabled.
   */
  readonly enabled: boolean = true;

  /**
   * Timeout in milliseconds for requesting fragments.
   */
  readonly requestTimeoutMs: number = 3000;

  /**
   * Request headers that are forwarded to fragment requests, if present.
   * These request headers are not considered to influence the response and thus will not influence caching.
   */
  readonly requestHeadersForward: string[] = ['Correlation-ID', 'X-Correlation-ID', 'X-Request-ID'];

  /**
   * Request headers that are forwarded to fragment requests, if present and that influence the
   * requested fragment aside from its URL.
   * These request headers are considered to influence the response and thus influence caching.
   */
  readonly requestHeadersForwardVary: string[] = [];

  /**
   * Response headers of primary fragments to forward to the page response, if present.
   */
  readonly responseHeadersForward: string[] = ['Content-Language', 'Location', 'Refresh'];

  /**
   * Maximum number of items, the fragment cache may hold.
   */
  readonly cacheMaxItems: number = 10000;

  /**
   * Whether to enable auto-refreshing of cached fragments.
   */
  readonly cacheAutoRefreshEnabled: boolean = false;

  /**
   * Maximum number of attempts to refresh a cached fragment.
   */
  readonly cacheAutoRefreshMaxAttempts: number = 3;

  /**
   * Maximum number of consecutive refreshs of inactive cached fragments.<br>
   * Fragments are considered inactive, if they have not been read from cache
   * between writing to cache and a refresh attempt.
   */
  readonly cacheAutoRefreshInactiveFragmentsMaxRefreshs: number = 2;

  /**
   * Whether to append UI composition stats as HTML comment to the content.
   */
  readonly statsAppendToContent: boolean = false;

  /**
   * Whether to expose fragment URLs in the stats appended to the content.
   */
  readonly statsExposeFragmentUrl: boolean = false;

  constructor(init?: Partial<AbleronConfig>) {
    Object.assign(this, init);
  }
}
