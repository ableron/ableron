import Include from './include.js';
import Fragment from './fragment.js';
import HttpUtil from './http-util.js';
import { IncomingHttpHeaders, OutgoingHttpHeaders } from 'http2';
import { LoggerInterface, NoOpLogger } from './logger.js';
import CacheStats from './cache-stats.js';

export default class TransclusionResult {
  private readonly logger: LoggerInterface;
  private content: string;
  private contentExpirationTime?: Date;
  private hasPrimaryInclude: boolean = false;
  private statusCodeOverride?: number;
  private readonly responseHeadersToForward: Headers = new Headers();
  private readonly cacheStats: CacheStats;
  private readonly appendStatsToContent: boolean;
  private readonly exposeFragmentUrl: boolean;
  private processingTimeMillis: number = 0;
  private readonly processedIncludes: Include[] = [];

  constructor(
    content: string,
    cacheStats: CacheStats,
    appendStatsToContent: boolean = false,
    exposeFragmentUrl: boolean = false,
    logger?: LoggerInterface
  ) {
    this.logger = logger || new NoOpLogger();
    this.content = content;
    this.cacheStats = cacheStats;
    this.appendStatsToContent = appendStatsToContent;
    this.exposeFragmentUrl = exposeFragmentUrl;
  }

  getContent(): string {
    return this.appendStatsToContent ? this.content + this.getStatsAsHtmlComment() : this.content;
  }

  getContentExpirationTime(): Date | undefined {
    return this.contentExpirationTime;
  }

  getHasPrimaryInclude(): boolean {
    return this.hasPrimaryInclude;
  }

  getStatusCodeOverride(): number | undefined {
    return this.statusCodeOverride;
  }

  getResponseHeadersToForward(): Headers {
    return this.responseHeadersToForward;
  }

  getProcessedIncludesCount(): number {
    return this.processedIncludes.length;
  }

  getProcessingTimeMillis(): number {
    return this.processingTimeMillis;
  }

  setProcessingTimeMillis(processingTimeMillis: number): void {
    this.processingTimeMillis = processingTimeMillis;
  }

  addResolvedInclude(include: Include): void {
    const fragment: Fragment = include.getResolvedFragment()!;

    if (include.isPrimary()) {
      if (this.hasPrimaryInclude) {
        this.logger.error(
          '[Ableron] Found multiple primary includes in one page. Only treating one of them as primary'
        );
      } else {
        this.hasPrimaryInclude = true;
        this.statusCodeOverride = fragment.statusCode;
        fragment.responseHeaders.forEach((headerValue, headerName) =>
          this.responseHeadersToForward.set(headerName, headerValue)
        );
      }
    }

    if (this.contentExpirationTime === undefined || fragment.expirationTime < this.contentExpirationTime) {
      this.contentExpirationTime = fragment.expirationTime;
    }

    this.content = this.content.replaceAll(include.getRawIncludeTag(), fragment.content);
    this.processedIncludes.push(include);
  }

  /**
   * Calculates the <code>Cache-Control</code> header value based on the fragment with the lowest
   * expiration time and the given page max age.
   *
   * @return The Cache-Control header value. Either "no-store" or "max-age=xxx"
   */
  calculateCacheControlHeaderValue(pageMaxAgeInSeconds?: number) {
    const now = new Date();

    if (
      (this.contentExpirationTime && this.contentExpirationTime < now) ||
      pageMaxAgeInSeconds === undefined ||
      pageMaxAgeInSeconds <= 0
    ) {
      return 'no-store';
    }

    if (
      this.contentExpirationTime &&
      this.contentExpirationTime < new Date(now.getTime() + pageMaxAgeInSeconds * 1000)
    ) {
      return 'max-age=' + Math.ceil(this.contentExpirationTime.getTime() / 1000 - now.getTime() / 1000);
    }

    return 'max-age=' + pageMaxAgeInSeconds;
  }

  /**
   * Calculates the <code>Cache-Control</code> header value based on the fragment with the lowest
   * expiration time and the given response headers which may contain page expiration time.
   *
   * @return The Cache-Control header value. Either "no-store" or "max-age=xxx"
   */
  calculateCacheControlHeaderValueByResponseHeaders(
    headers: Headers | IncomingHttpHeaders | OutgoingHttpHeaders | { [key: string]: string | string[] | number }
  ) {
    const pageExpirationTime: Date = HttpUtil.calculateResponseExpirationTime(headers);
    const pageMaxAge: number =
      pageExpirationTime > new Date() ? Math.ceil((pageExpirationTime.getTime() - Date.now()) / 1000) : 0;
    return this.calculateCacheControlHeaderValue(pageMaxAge);
  }

  getProcessedIncludesLogLine(): string {
    return `Processed ${this.getProcessedIncludesCount()} ${this.getProcessedIncludesCount() === 1 ? 'include' : 'includes'} in ${this.processingTimeMillis}ms`;
  }

  getCacheStatsLogLine(): string {
    return (
      `Cache: ${this.cacheStats.getItemCount()} items` +
      `, ${this.cacheStats.getHitCount()} hits` +
      `, ${this.cacheStats.getMissCount()} misses` +
      `, ${this.cacheStats.getRefreshSuccessCount()} successful refreshs` +
      `, ${this.cacheStats.getRefreshFailureCount()} failed refreshs`
    );
  }

  private getStatsAsHtmlComment(): string {
    return (
      '\n<!-- ' +
      this.getProcessedIncludesLogLine() +
      this.getProcessedIncludesDetails() +
      '\n\n' +
      this.getCacheStatsLogLine() +
      '\n-->'
    );
  }

  private getProcessedIncludesDetails(): string {
    let stats = '';

    if (this.processedIncludes.length) {
      stats +=
        '\n\nTime | Include | Resolved With | Fragment Cacheability' +
        (this.exposeFragmentUrl ? ' | Fragment URL' : '') +
        '\n------------------------------------------------------';
      this.processedIncludes
        .sort((a, b) => new Intl.Collator().compare(a.getId(), b.getId()))
        .forEach((include) => (stats += '\n' + this.getProcessedIncludeStatsRow(include)));
    }

    return stats;
  }

  private getProcessedIncludeStatsRow(include: Include): string {
    return (
      `${include.getResolveTimeMillis()}ms` +
      ` | ${this.getProcessedIncludeStatIncludeId(include)}` +
      ` | ${this.getProcessedIncludeStatFragmentSource(include)}` +
      ` | ${this.getProcessedIncludeStatCacheDetails(include)}` +
      `${this.exposeFragmentUrl ? ' | ' + this.getProcessedIncludeStatFragmentUrl(include) : ''}`
    );
  }

  private getProcessedIncludeStatIncludeId(include: Include): string {
    return include.getId() + (include.isPrimary() ? ' (primary)' : '');
  }

  private getProcessedIncludeStatFragmentSource(include: Include): string {
    return include.getResolvedFragmentSource() || '-';
  }

  private getProcessedIncludeStatCacheDetails(include: Include): string {
    if (!include.getResolvedFragment()?.url) {
      return '-';
    }

    const fragmentExpirationTime = include.getResolvedFragment()?.expirationTime!;

    if (fragmentExpirationTime.getTime() == new Date(0).getTime()) {
      return 'not cacheable';
    }

    return 'expires in ' + Math.ceil((fragmentExpirationTime.getTime() - Date.now()) / 1000) + 's';
  }

  private getProcessedIncludeStatFragmentUrl(include: Include): string {
    return include.getResolvedFragment()?.url || '-';
  }
}
