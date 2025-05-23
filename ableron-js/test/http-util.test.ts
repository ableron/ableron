import { describe, expect, it } from 'vitest';
import HttpUtil from '../src/http-util.js';

describe('HttpUtil.calculateResponseExpirationTime', () => {
  it('should calculate response expiration time based on s-maxage', () => {
    // when
    const expirationTime = HttpUtil.calculateResponseExpirationTime(
      new Headers([
        ['Cache-Control', 'max-age=3600, s-maxage=604800 , public'],
        ['Expires', 'Wed, 21 Oct 2015 07:28:00 GMT']
      ])
    );

    // then
    expect(expirationTime < new Date(Date.now() + 604800000 + 1000)).toBe(true);
    expect(expirationTime > new Date(Date.now() + 604800000 - 1000)).toBe(true);
  });

  it.each([
    [
      new Headers([
        ['Cache-Control', 'max-age=3600'],
        ['Expires', 'Wed, 21 Oct 2015 07:28:00 GMT']
      ]),
      3600
    ],
    [
      new Headers([
        ['cache-control', 'MAX-AGE=3600'],
        ['Expires', 'Wed, 21 Oct 2015 07:28:00 GMT']
      ]),
      3600
    ],
    [
      new Headers([
        ['Cache-Control', 'max-age=3600'],
        ['Age', '600'],
        ['Expires', 'Wed, 21 Oct 2015 07:28:00 GMT']
      ]),
      3000
    ],
    [
      new Headers([
        ['cache-control', 'MAX-AGE=3600'],
        ['age', '600'],
        ['Expires', 'Wed, 21 Oct 2015 07:28:00 GMT']
      ]),
      3000
    ],
    [
      new Headers([
        ['Cache-Control', 'max-age=3600'],
        ['Age', '-100'],
        ['Expires', 'Wed, 21 Oct 2015 07:28:00 GMT']
      ]),
      3500
    ]
  ])(
    'should calculate response expiration time based on max-age and optional Age',
    (responseHeaders: Headers, expectedExpirationTimeSeconds: number) => {
      // when
      const expirationTime = HttpUtil.calculateResponseExpirationTime(responseHeaders);

      // then
      expect(expirationTime.getTime()).toBeLessThan(
        new Date(Date.now() + expectedExpirationTimeSeconds * 1000 + 1000).getTime()
      );
      expect(expirationTime.getTime()).toBeGreaterThan(
        new Date(Date.now() + expectedExpirationTimeSeconds * 1000 - 1000).getTime()
      );
    }
  );

  it('should calculate response expiration time based on Expires header and current time if Cache-Control header and Date header are not present', () => {
    // when
    const expirationTime = HttpUtil.calculateResponseExpirationTime(
      new Headers([
        ['Cache-Control', 'public'],
        ['Expires', 'Wed, 12 Oct 2050 07:28:00 GMT']
      ])
    );

    // then
    expect(expirationTime.toUTCString()).toBe('Wed, 12 Oct 2050 07:28:00 GMT');
  });

  it('should calculate response expiration time based on Expires and Date header if Cache-Control header is not present', () => {
    // when
    const expirationTime = HttpUtil.calculateResponseExpirationTime(
      new Headers([
        ['Date', 'Wed, 05 Oct 2050 07:28:00 GMT'],
        ['Expires', 'Wed, 12 Oct 2050 07:28:00 GMT']
      ])
    );

    // then
    const SEVEN_DAYS_IN_MILLISECONDS = 7 * 24 * 60 * 60 * 1000;
    expect(expirationTime.getTime()).toBeLessThan(new Date(Date.now() + SEVEN_DAYS_IN_MILLISECONDS + 1000).getTime());
    expect(expirationTime.getTime()).toBeGreaterThan(
      new Date(Date.now() + SEVEN_DAYS_IN_MILLISECONDS - 1000).getTime()
    );
  });

  it('should calculate response expiration time based on Expires=0', () => {
    expect(HttpUtil.calculateResponseExpirationTime(new Headers([['Expires', '0']]))).toEqual(new Date(0));
  });

  it('should calculate response expiration time if Cache-Control header is set but without max-age directives', () => {
    expect(
      HttpUtil.calculateResponseExpirationTime(new Headers([['Cache-Control', 'no-cache,no-store,must-revalidate']]))
    ).toEqual(new Date(0));
  });

  it.each([
    [new Headers([['Cache-Control', 's-maxage=not-numeric']])],
    [new Headers([['Cache-Control', 'max-age=not-numeric']])],
    [
      new Headers([
        ['Cache-Control', 'max-age=3600'],
        ['Age', 'not-numeric']
      ])
    ],
    [new Headers([['Expires', 'not-a-date']])],
    [
      new Headers([
        ['Expires', 'Wed, 12 Oct 2050 07:28:00 GMT'],
        ['Date', 'not-a-date']
      ])
    ]
  ])('should not crash on invalid header values', (responseHeaders: Headers) => {
    expect(HttpUtil.calculateResponseExpirationTime(responseHeaders)).toEqual(new Date(0));
  });

  it('should calculate expiration time in the past if no expiration time is indicated via response header', () => {
    expect(HttpUtil.calculateResponseExpirationTime(new Headers())).toEqual(new Date(0));
  });
});

describe('HttpUtil.normalizeHeaders', () => {
  it('should handle input headers of type Headers', () => {
    // given
    const inputHeaders = new Headers([
      ['X-Foo', 'foo'],
      ['X-Bar', 'bar'],
      ['X-Baz', ''],
      ['Set-Cookie', 'keya=valuea'],
      ['Set-Cookie', 'keyb=valueb']
    ]);

    // when
    const result = HttpUtil.normalizeHeaders(inputHeaders);

    // then
    // @ts-ignore
    expect(Array.from(result)).toEqual(
      expect.arrayContaining([
        ['x-foo', 'foo'],
        ['x-bar', 'bar'],
        ['x-baz', ''],
        ['set-cookie', 'keya=valuea'],
        ['set-cookie', 'keyb=valueb']
      ])
    );
  });

  it('should handle input headers of type IncomingHttpHeaders', () => {
    // when
    const result = HttpUtil.normalizeHeaders({
      'X-Foo': 'foo',
      'X-Bar': 'bar',
      'X-Baz': '',
      'Set-Cookie': ['keya=valuea', 'keyb=valueb']
    });

    // then
    // @ts-ignore
    expect(Array.from(result)).toEqual(
      expect.arrayContaining([
        ['x-foo', 'foo'],
        ['x-bar', 'bar'],
        ['set-cookie', 'keya=valuea'],
        ['set-cookie', 'keyb=valueb']
      ])
    );
  });
});

describe('HttpUtil.getCookieHeaderValue', () => {
  it.each([
    [null, null, null],
    [new Headers([['Cookie', 'uid']]), null, null],
    [null, ['uid'], null],
    [new Headers([['cookie', 'uid']]), [], null],
    [new Headers(), ['uid'], null],
    [new Headers([['Cookie', 'uid=1;TEST=A; Foo=Bar']]), ['test'], null],
    [new Headers([['cookie', 'uid=1;TEST=A; Foo=Bar']]), ['TEST'], 'TEST=A'],
    [new Headers([['Cookie', ' uid=1  ;TEST=A;  Foo=Bar  ; ']]), ['TEST', 'Foo', 'uid'], 'uid=1; TEST=A; Foo=Bar']
  ])(
    'should extract cookie header value',
    (headers: Headers, cookieNameAllowlist: string[], expectedResult: string | null) => {
      expect(HttpUtil.getCookieHeaderValue(headers, cookieNameAllowlist)).toBe(expectedResult);
    }
  );
});
