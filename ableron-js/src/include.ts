import Fragment from './fragment.js';
import * as crypto from 'crypto';
import AbleronConfig from './ableron-config.js';
import HttpUtil from './http-util.js';
import { LoggerInterface, NoOpLogger } from './logger.js';
import FragmentCache from './fragment-cache.js';

export default class Include {
  /**
   * Name of the optional attribute which contains the ID of the include - an optional unique name.
   */
  private readonly ATTR_ID: string = 'id';

  /**
   * Name of the optional attribute which contains the source URl to resolve the include to.
   */
  private readonly ATTR_SOURCE: string = 'src';

  /**
   * Name of the optional attribute which contains the timeout for requesting the src URL.
   */
  private readonly ATTR_SOURCE_TIMEOUT: string = 'src-timeout';

  /**
   * Name of the optional attribute which contains the fallback URL to resolve the include to in case the
   * source URL could not be loaded.
   */
  private readonly ATTR_FALLBACK_SOURCE: string = 'fallback-src';

  /**
   * Name of the optional attribute which contains the timeout for requesting the fallback-src URL.
   */
  private readonly ATTR_FALLBACK_SOURCE_TIMEOUT: string = 'fallback-src-timeout';

  /**
   * Name of the optional attribute which denotes a fragment whose response code is set as response code
   * for the page.
   */
  private readonly ATTR_PRIMARY: string = 'primary';

  /**
   * Name of the optional attribute which contains a comma separated list of request headers that shall
   * be forwarded from the parent request to fragment requests.
   */
  private readonly ATTR_HEADERS: string = 'headers';

  /**
   * Name of the optional attribute which contains a comma separated list of HTTP cookie names that shall
   * be passed from the parent request to fragment requests.
   */
  private readonly ATTR_COOKIES: string = 'cookies';

  /**
   * Regular expression for parsing timeouts.<br>
   * <br>
   * Accepts plain numbers and numbers suffixed with either <code>s</code> indicating <code>seconds</code> or
   * <code>ms</code> indicating <code>milliseconds</code>.
   */
  private readonly TIMEOUT_PATTERN: RegExp = /^(\d+)(ms|s)?$/;

  /**
   * HTTP status codes indicating successful and cacheable responses.
   */
  private readonly HTTP_STATUS_CODES_SUCCESS: number[] = [200, 203, 204, 206];

  private readonly logger: LoggerInterface;

  /**
   * Raw include tag.
   */
  private readonly rawIncludeTag: string;

  /**
   * Raw attributes of the include tag.
   */
  private readonly rawAttributes: Map<string, string>;

  /**
   * Fragment ID. Either generated or passed via attribute.
   */
  private readonly id: string;

  /**
   * URL of the fragment to include.
   */
  private readonly src?: string;

  /**
   * Timeout in milliseconds for requesting the src URL.
   */
  private readonly srcTimeoutMillis?: number;

  /**
   * URL of the fragment to include in case the request to the source URL failed.
   */
  private readonly fallbackSrc?: string;

  /**
   * Timeout in milliseconds for requesting the fallback-src URL.
   */
  private readonly fallbackSrcTimeoutMillis?: number;

  /**
   * Whether the include provides the primary fragment and thus sets the response code of the page.
   */
  private readonly primary: boolean;

  /**
   * Request headers, that shall be forwarded from the parent request to fragment requests.
   */
  private readonly headersToForward: string[];

  /**
   * Cookies, that shall be forwarded from the parent request to fragment requests.
   */
  private readonly cookiesToForward: string[];

  /**
   * Fallback content to use in case the include could not be resolved.
   */
  private readonly fallbackContent: string;

  /**
   * Recorded response of the errored primary fragment.
   */
  private erroredPrimaryFragment?: Fragment;
  private erroredPrimaryFragmentSource?: string;

  private resolved: boolean = false;
  private resolvedFragment?: Fragment;
  private resolvedFragmentSource?: string;
  private resolveTimeMillis: number = 0;

  constructor(
    rawIncludeTag: string,
    rawAttributes?: Map<string, string>,
    fallbackContent?: string,
    logger?: LoggerInterface
  ) {
    this.logger = logger || new NoOpLogger();
    this.rawIncludeTag = rawIncludeTag;
    this.rawAttributes = rawAttributes !== undefined ? rawAttributes : new Map<string, string>();
    this.id = this.buildIncludeId(this.rawAttributes.get(this.ATTR_ID));
    this.src = this.rawAttributes.get(this.ATTR_SOURCE);
    this.srcTimeoutMillis = this.parseTimeout(this.rawAttributes.get(this.ATTR_SOURCE_TIMEOUT));
    this.fallbackSrc = this.rawAttributes.get(this.ATTR_FALLBACK_SOURCE);
    this.fallbackSrcTimeoutMillis = this.parseTimeout(this.rawAttributes.get(this.ATTR_FALLBACK_SOURCE_TIMEOUT));
    const primary = this.rawAttributes.get(this.ATTR_PRIMARY);
    this.primary = primary !== undefined && ['', 'primary'].includes(primary.toLowerCase());
    this.headersToForward = this.parseCommaSeparatedList(this.rawAttributes.get(this.ATTR_HEADERS), true);
    this.cookiesToForward = this.parseCommaSeparatedList(this.rawAttributes.get(this.ATTR_COOKIES));
    this.fallbackContent = fallbackContent !== undefined ? fallbackContent : '';
  }

  getRawIncludeTag(): string {
    return this.rawIncludeTag;
  }

  getRawAttributes(): Map<string, string> {
    return this.rawAttributes;
  }

  getId(): string {
    return this.id;
  }

  getSrc(): string | undefined {
    return this.src;
  }

  getSrcTimeoutMillis(): number | undefined {
    return this.srcTimeoutMillis;
  }

  getFallbackSrc(): string | undefined {
    return this.fallbackSrc;
  }

  getFallbackSrcTimeoutMillis(): number | undefined {
    return this.fallbackSrcTimeoutMillis;
  }

  isPrimary(): boolean {
    return this.primary;
  }

  getHeadersToForward(): string[] {
    return this.headersToForward;
  }

  getCookiesToForward(): string[] {
    return this.cookiesToForward;
  }

  getFallbackContent(): string {
    return this.fallbackContent;
  }

  isResolved(): boolean {
    return this.resolved;
  }

  getResolvedFragment(): Fragment | undefined {
    return this.resolvedFragment;
  }

  getResolvedFragmentSource(): string | undefined {
    return this.resolvedFragmentSource;
  }

  getResolveTimeMillis(): number {
    return this.resolveTimeMillis;
  }

  resolve(config: AbleronConfig, fragmentCache: FragmentCache, parentRequestHeaders: Headers): Promise<Include> {
    const resolveStartTime = Date.now();
    const requestHeaders = this.buildRequestHeaders(parentRequestHeaders, config);
    this.erroredPrimaryFragment = undefined;

    return this.load(
      this.src,
      requestHeaders,
      this.getRequestTimeout(this.srcTimeoutMillis, config),
      fragmentCache,
      config,
      this.ATTR_SOURCE
    )
      .then(
        (fragment) =>
          fragment ||
          this.load(
            this.fallbackSrc,
            requestHeaders,
            this.getRequestTimeout(this.fallbackSrcTimeoutMillis, config),
            fragmentCache,
            config,
            this.ATTR_FALLBACK_SOURCE
          )
      )
      .then((fragment) => {
        if (fragment) {
          return fragment;
        }

        this.resolvedFragmentSource = this.erroredPrimaryFragmentSource;
        return this.erroredPrimaryFragment;
      })
      .then((fragment) => {
        if (fragment) {
          return fragment;
        }

        this.resolvedFragmentSource = 'fallback content';
        return new Fragment(200, this.fallbackContent);
      })
      .then((fragment) => this.resolveWith(fragment, Date.now() - resolveStartTime, this.resolvedFragmentSource));
  }

  /**
   * Resolves this Include with the given Fragment.
   *
   * @param fragment The Fragment to resolve this Include with
   * @param resolveTimeMillis The time in milliseconds it took to resolve the Include
   * @param resolvedFragmentSource Source of the fragment
   */
  resolveWith(
    fragment: Fragment,
    resolveTimeMillis?: number,
    resolvedFragmentSource: string = 'fallback content'
  ): Include {
    this.resolved = true;
    this.resolvedFragment = fragment;
    this.resolvedFragmentSource = resolvedFragmentSource;
    this.resolveTimeMillis = resolveTimeMillis || 0;
    this.logger.debug("[Ableron] Resolved include '%s' in %dms", this.id, this.resolveTimeMillis);
    return this;
  }

  private buildRequestHeaders(parentRequestHeaders: Headers, config: AbleronConfig): Headers {
    const requestHeaders = this.filterHeaders(parentRequestHeaders, [
      ...config.requestHeadersForward,
      ...config.requestHeadersForwardVary,
      ...this.headersToForward
    ]);
    const cookieHeaderValue = HttpUtil.getCookieHeaderValue(parentRequestHeaders, this.cookiesToForward);

    if (cookieHeaderValue !== null) {
      requestHeaders.set(HttpUtil.HEADER_COOKIE, cookieHeaderValue);
    }

    return requestHeaders;
  }

  private async load(
    url: string | undefined,
    requestHeaders: Headers,
    requestTimeoutMs: number,
    fragmentCache: FragmentCache,
    config: AbleronConfig,
    urlSource: string
  ): Promise<Fragment | null> {
    if (!url) {
      return null;
    }

    const fragmentCacheKey = this.buildFragmentCacheKey(url, requestHeaders, config.requestHeadersForwardVary);
    const fragmentFromCache = fragmentCache.get(fragmentCacheKey);
    const fragmentSource = (fragmentFromCache ? 'cached ' : 'remote ') + urlSource;
    const maybeFragment: Promise<Fragment | null> = fragmentFromCache
      ? Promise.resolve(fragmentFromCache)
      : HttpUtil.loadUrl(url, requestHeaders, requestTimeoutMs, this.logger)
          .then(async (response: Response | null) => {
            if (!response) {
              return null;
            }

            if (!HttpUtil.HTTP_STATUS_CODES_CACHEABLE.includes(response.status)) {
              this.logger.error(`[Ableron] Fragment '${this.id}' returned status code ${response.status}`);
              this.recordErroredPrimaryFragment(
                await this.toFragment(response, url, config.responseHeadersForward, true),
                fragmentSource
              );
              return null;
            }

            return this.toFragment(response, url, config.responseHeadersForward);
          })
          .then((fragment) => {
            if (fragment) {
              fragmentCache.set(fragmentCacheKey, fragment, () =>
                HttpUtil.loadUrl(url, requestHeaders, requestTimeoutMs, this.logger).then(
                  async (response: Response | null) => {
                    return response ? this.toFragment(response, url, config.responseHeadersForward) : null;
                  }
                )
              );
            }

            return fragment;
          });

    return maybeFragment.then((fragment) => {
      if (fragment && !this.HTTP_STATUS_CODES_SUCCESS.includes(fragment.statusCode)) {
        this.logger.error(`[Ableron] Fragment '${this.id}' returned status code ${fragment.statusCode}`);
        this.recordErroredPrimaryFragment(fragment, fragmentSource);
        return null;
      }

      this.resolvedFragmentSource = fragmentSource;
      return fragment;
    });
  }

  private async toFragment(
    response: Response,
    url: string,
    responseHeadersForward: string[],
    preventCaching: boolean = false
  ): Promise<Fragment> {
    return response
      .text()
      .then(
        (responseBody) =>
          new Fragment(
            response.status,
            responseBody,
            url,
            preventCaching ? undefined : HttpUtil.calculateResponseExpirationTime(response.headers),
            this.filterHeaders(response.headers, responseHeadersForward)
          )
      );
  }

  private recordErroredPrimaryFragment(fragment: Fragment, fragmentSource: string): void {
    if (this.primary && !this.erroredPrimaryFragment) {
      this.erroredPrimaryFragment = fragment;
      this.erroredPrimaryFragmentSource = fragmentSource;
    }
  }

  private filterHeaders(headersToFilter: Headers, allowedHeaders: string[]): Headers {
    const filteredHeaders = new Headers();
    allowedHeaders.forEach((allowedHeaderName) => {
      if (headersToFilter.has(allowedHeaderName)) {
        filteredHeaders.set(allowedHeaderName, headersToFilter.get(allowedHeaderName) as string);
      }
    });
    return filteredHeaders;
  }

  private parseTimeout(timeoutAsString?: string): number | undefined {
    if (timeoutAsString !== undefined) {
      const match = timeoutAsString.match(this.TIMEOUT_PATTERN);

      if (match !== null) {
        const amount = Number(match[1]);
        const unit = match[2];

        if (unit === 's') {
          return amount * 1000;
        }

        return amount;
      }

      this.logger.error(`[Ableron] Invalid request timeout: '${timeoutAsString}'`);
    }

    return undefined;
  }

  private parseCommaSeparatedList(entriesAsString?: string, toLowerCase: boolean = false): string[] {
    return entriesAsString !== undefined
      ? [
          ...new Set(
            entriesAsString
              .split(/,/)
              .map((entry) => entry.trim())
              .map((entry) => (toLowerCase ? entry.toLowerCase() : entry))
              .filter((entry) => entry.length > 0)
          )
        ]
      : [];
  }

  private getRequestTimeout(localTimeout: number | undefined, config: AbleronConfig): number {
    return localTimeout ? localTimeout : config.requestTimeoutMs;
  }

  private buildIncludeId(providedId?: string): string {
    if (providedId !== undefined) {
      const sanitizedId = providedId.replaceAll(/[^A-Za-z0-9_-]/g, '');

      if (sanitizedId !== '') {
        return sanitizedId;
      }
    }

    return crypto.createHash('sha1').update(this.rawIncludeTag).digest('hex').substring(0, 7);
  }

  private buildFragmentCacheKey(
    fragmentUrl: string,
    requestHeaders: Headers,
    requestHeadersForwardVary: string[]
  ): string {
    const headersRelevantForCaching = [...requestHeadersForwardVary, ...this.headersToForward];
    let headersCacheKey = '';

    headersRelevantForCaching.sort().forEach((headerName) => {
      const headerValue = requestHeaders.get(headerName);

      if (headerValue) {
        headersCacheKey += '\nh:' + headerName + '=' + headerValue;
      }
    });

    let cookiesCacheKey = '';
    const cookieHeaderValue = HttpUtil.getCookieHeaderValue(requestHeaders, this.cookiesToForward);

    if (cookieHeaderValue !== null) {
      cookiesCacheKey = cookieHeaderValue
        .split(';')
        .map((cookie) => cookie.trim())
        .sort()
        .map((cookie) => '\nc:' + cookie)
        .join();
    }

    return fragmentUrl + headersCacheKey + cookiesCacheKey;
  }
}
