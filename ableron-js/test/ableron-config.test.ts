import { describe, expect, it } from 'vitest';
import { AbleronConfig } from '../src/index.js';

describe('AbleronConfig', () => {
  it('should have default value for each property', () => {
    // when
    const config = new AbleronConfig();

    // then
    expect(config.enabled).toBe(true);
    expect(config.requestTimeoutMs).toBe(3000);
    expect(config.requestHeadersPassThrough).toEqual(['Correlation-ID', 'X-Correlation-ID', 'X-Request-ID']);
    expect(config.requestHeadersPassThroughVary).toEqual([]);
    expect(config.responseHeadersPassThrough).toEqual(['Content-Language', 'Location', 'Refresh']);
    expect(config.cacheMaxItems).toEqual(10000);
    expect(config.cacheAutoRefreshEnabled).toBe(false);
    expect(config.cacheAutoRefreshMaxAttempts).toBe(3);
    expect(config.cacheAutoRefreshInactiveFragmentsMaxRefreshs).toBe(2);
    expect(config.statsAppendToContent).toBe(false);
    expect(config.statsExposeFragmentUrl).toBe(false);
  });

  it('should use values provided via constructor', () => {
    // when
    const config = new AbleronConfig({
      enabled: false,
      requestTimeoutMs: 200,
      requestHeadersPassThrough: ['X-Test-Request-Header', 'X-Test-Request-Header-2'],
      requestHeadersPassThroughVary: ['X-ACME-Test-Groups'],
      responseHeadersPassThrough: ['X-Test-Response-Header', 'X-Test-Response-Header-2'],
      cacheMaxItems: 999,
      cacheAutoRefreshEnabled: true,
      cacheAutoRefreshMaxAttempts: 5,
      cacheAutoRefreshInactiveFragmentsMaxRefreshs: 7,
      statsAppendToContent: true,
      statsExposeFragmentUrl: true
    });

    // then
    expect(config.enabled).toBe(false);
    expect(config.requestTimeoutMs).toBe(200);
    expect(config.requestHeadersPassThrough).toEqual(['X-Test-Request-Header', 'X-Test-Request-Header-2']);
    expect(config.requestHeadersPassThroughVary).toEqual(['X-ACME-Test-Groups']);
    expect(config.responseHeadersPassThrough).toEqual(['X-Test-Response-Header', 'X-Test-Response-Header-2']);
    expect(config.cacheMaxItems).toEqual(999);
    expect(config.cacheAutoRefreshEnabled).toBe(true);
    expect(config.cacheAutoRefreshMaxAttempts).toBe(5);
    expect(config.cacheAutoRefreshInactiveFragmentsMaxRefreshs).toBe(7);
    expect(config.statsAppendToContent).toBe(true);
    expect(config.statsExposeFragmentUrl).toBe(true);
  });
});
