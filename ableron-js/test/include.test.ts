import { beforeEach, afterEach, describe, expect, it } from 'vitest';
import Include from '../src/include.js';
import Fastify, { FastifyInstance } from 'fastify';
import { AbleronConfig } from '../src/index.js';
import TransclusionProcessor from '../src/transclusion-processor.js';
import Fragment from '../src/fragment.js';
import { NoOpLogger } from '../src/logger.js';
import FragmentCache from '../src/fragment-cache.js';
import { IncomingHttpHeaders } from 'http2';

const sleep = (delay: number) => new Promise((resolve) => setTimeout(resolve, delay));

let server: FastifyInstance | undefined;
const config = new AbleronConfig({
  requestTimeoutMs: 1000
});
const fragmentCache = new TransclusionProcessor(config, new NoOpLogger()).getFragmentCache();

beforeEach(() => {
  server = undefined;
  fragmentCache.clear();
});

afterEach(async () => {
  if (server) {
    await server.close();
  }
});

function serverAddress(path: string): string {
  if (server) {
    const address = ['127.0.0.1', '::1'].includes(server.addresses()[0].address)
      ? 'localhost'
      : server.addresses()[0].address;
    return `http://${address}:${server.addresses()[0].port}/${path.replace(/^\//, '')}`;
  }

  return 'undefined';
}

describe('Include', () => {
  it('should set raw attributes in constructor', () => {
    // given
    const rawAttributes = new Map([['src', 'https://example.com']]);

    // expect
    expect(new Include('', rawAttributes).getRawAttributes()).toEqual(rawAttributes);
  });

  it.each([
    [new Include(''), ''],
    [new Include('', undefined, 'fallback'), 'fallback']
  ])('should set fallback content in constructor', (include: Include, expectedFallbackContent: string) => {
    expect(include.getFallbackContent()).toBe(expectedFallbackContent);
  });

  it('should set raw include tag in constructor', () => {
    // given
    const rawIncludeTag = '<ableron-include src="https://example.com"/>';

    // expect
    expect(new Include(rawIncludeTag).getRawIncludeTag()).toBe(rawIncludeTag);
  });

  it.each([
    [new Include('', new Map()), 'da39a3e'],
    [new Include('', new Map([['id', 'foo-bar']])), 'foo-bar'],
    [new Include('', new Map([['id', 'FOO-bar%baz__/5']])), 'FOO-barbaz__5'],
    [new Include('', new Map([['id', '//']])), 'da39a3e'],
    [new Include('zzzzz'), 'a2b7cad'],
    [new Include('zzzzzz'), '984ff6e']
  ])('should handle include id', (include: Include, expectedId: string) => {
    expect(include.getId()).toBe(expectedId);
  });

  it.each([
    [new Include(''), undefined],
    [new Include('', new Map([['src', 'https://example.com']])), 'https://example.com']
  ])('should parse src attribute', (include: Include, expectedSrc?: string) => {
    expect(include.getSrc()).toBe(expectedSrc);
  });

  it.each([
    [new Include(''), undefined],
    [new Include('', new Map([['src-timeout', '2000']])), 2000],
    [new Include('', new Map([['src-timeout', '2000ms']])), 2000],
    [new Include('', new Map([['src-timeout', '2s']])), 2000],
    [new Include('', new Map([['src-timeout', '2S']])), undefined],
    [new Include('', new Map([['src-timeout', ' 2000']])), undefined],
    [new Include('', new Map([['src-timeout', '2000 ']])), undefined],
    [new Include('', new Map([['src-timeout', '2m']])), undefined],
    [new Include('', new Map([['src-timeout', '2\ns']])), undefined]
  ])('should parse src timeout attribute', (include: Include, expectedSrcTimeout?: number) => {
    expect(include.getSrcTimeoutMillis()).toBe(expectedSrcTimeout);
  });

  it.each([
    [new Include(''), undefined],
    [new Include('', new Map([['fallback-src', 'https://example.com']])), 'https://example.com']
  ])('should parse fallback-src attribute', (include: Include, expectedFallbackSrc?: string) => {
    expect(include.getFallbackSrc()).toBe(expectedFallbackSrc);
  });

  it.each([
    [new Include(''), undefined],
    [new Include('', new Map([['fallback-src-timeout', '2000']])), 2000],
    [new Include('', new Map([['fallback-src-timeout', '2000ms']])), 2000],
    [new Include('', new Map([['fallback-src-timeout', '2s']])), 2000],
    [new Include('', new Map([['fallback-src-timeout', '2S']])), undefined],
    [new Include('', new Map([['fallback-src-timeout', ' 2000']])), undefined],
    [new Include('', new Map([['fallback-src-timeout', '2000 ']])), undefined],
    [new Include('', new Map([['fallback-src-timeout', '2m']])), undefined],
    [new Include('', new Map([['fallback-src-timeout', '2\ns']])), undefined]
  ])('should parse fallback-src timeout attribute', (include: Include, expectedFallbackSrcTimeout?: number) => {
    expect(include.getFallbackSrcTimeoutMillis()).toBe(expectedFallbackSrcTimeout);
  });

  it.each([
    [new Include(''), false],
    [new Include('', new Map([['primary', '']])), true],
    [new Include('', new Map([['primary', 'primary']])), true],
    [new Include('', new Map([['primary', 'PRIMARY']])), true],
    [new Include('', new Map([['primary', 'nope']])), false]
  ])('should parse primary attribute', (include: Include, expectedPrimary: boolean) => {
    expect(include.isPrimary()).toBe(expectedPrimary);
  });

  it.each([
    [new Include(''), []],
    [new Include('', new Map([['headers', '']])), []],
    [new Include('', new Map([['headers', 'test']])), ['test']],
    [new Include('', new Map([['headers', 'TEST']])), ['test']],
    [
      new Include('', new Map([['headers', ' test1,test2  ,, TEST3 ,\nTest4,,test4  ']])),
      ['test1', 'test2', 'test3', 'test4']
    ]
  ])('should parse headers attribute', (include: Include, expectedHeadersToForward: string[]) => {
    expect(include.getHeadersToForward()).toStrictEqual(expectedHeadersToForward);
  });

  it.each([
    [new Include(''), []],
    [new Include('', new Map([['cookies', '']])), []],
    [new Include('', new Map([['cookies', 'test']])), ['test']],
    [new Include('', new Map([['cookies', 'TEST']])), ['TEST']],
    [
      new Include('', new Map([['cookies', ' test1,test2  ,, TEST3 ,\nTest4,,test4  ']])),
      ['test1', 'test2', 'TEST3', 'Test4', 'test4']
    ]
  ])('should parse cookies attribute', (include: Include, expectedCookiesToForward: string[]) => {
    expect(include.getCookiesToForward()).toStrictEqual(expectedCookiesToForward);
  });

  it('should resolve with src', async () => {
    // given
    server = Fastify();
    server.get('/', function (request, reply) {
      reply.status(206).send('response');
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/')]])).resolve(
      config,
      fragmentCache,
      new Headers()
    );

    // then
    expect(include.isResolved()).toBe(true);
    expect(include.getResolvedFragment()?.content).toBe('response');
    expect(include.getResolvedFragment()?.statusCode).toBe(206);
    expect(include.getResolvedFragmentSource()).toBe('remote src');
    expect(include.getResolveTimeMillis()).toBeGreaterThan(0);
  });

  it('should resolve with fallback-src if src could not be loaded', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply.status(500).send('fragment from src');
    });
    server.get('/fallback-src', function (request, reply) {
      reply.status(200).send('fragment from fallback-src');
    });
    await server.listen();

    // when
    const include = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['fallback-src', serverAddress('/fallback-src')]
      ])
    ).resolve(config, fragmentCache, new Headers());

    // then
    expect(include.isResolved()).toBe(true);
    expect(include.getResolvedFragment()?.content).toBe('fragment from fallback-src');
    expect(include.getResolvedFragment()?.statusCode).toBe(200);
    expect(include.getResolvedFragmentSource()).toBe('remote fallback-src');
    expect(include.getResolveTimeMillis()).toBeGreaterThan(0);
  });

  it('should resolve with fallback content if src and fallback-src could not be loaded', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply.status(500).send('fragment from src');
    });
    server.get('/fallback-src', function (request, reply) {
      reply.status(500).send('fragment from fallback-src');
    });
    await server.listen();

    // when
    const include = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['fallback-src', serverAddress('/fallback-src')]
      ]),
      'fallback content'
    ).resolve(config, fragmentCache, new Headers());

    // then
    expect(include.isResolved()).toBe(true);
    expect(include.getResolvedFragment()?.content).toBe('fallback content');
    expect(include.getResolvedFragment()?.statusCode).toBe(200);
    expect(include.getResolvedFragmentSource()).toBe('fallback content');
    expect(include.getResolveTimeMillis()).toBeGreaterThan(0);
  });

  it('should resolve to empty string if src, fallback src and fallback content are not present', async () => {
    // when
    const include = await new Include('').resolve(config, fragmentCache, new Headers());

    // then
    expect(include.isResolved()).toBe(true);
    expect(include.getResolvedFragment()?.content).toBe('');
    expect(include.getResolvedFragment()?.statusCode).toBe(200);
    expect(include.getResolvedFragmentSource()).toBe('fallback content');
  });

  it('should handle primary include with errored src', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply.status(503).send('fragment from src');
    });
    await server.listen();

    // when
    const include = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['primary', 'primary']
      ])
    ).resolve(config, fragmentCache, new Headers());

    // then
    expect(include.isResolved()).toBe(true);
    expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    expect(include.getResolvedFragment()?.statusCode).toBe(503);
    expect(include.getResolvedFragmentSource()).toBe('remote src');
    expect(include.getResolveTimeMillis()).toBeGreaterThan(0);
  });

  it('should handle primary include without src and with errored fallback-src', async () => {
    // given
    server = Fastify();
    server.get('/fallback-src-503', function (request, reply) {
      reply.status(503).send('fragment from fallback-src-503');
    });
    await server.listen();

    // when
    const include = await new Include(
      '',
      new Map([
        ['fallback-src', serverAddress('/fallback-src-503')],
        ['primary', 'primary']
      ])
    ).resolve(config, fragmentCache, new Headers());

    // then
    expect(include.isResolved()).toBe(true);
    expect(include.getResolvedFragment()?.content).toBe('fragment from fallback-src-503');
    expect(include.getResolvedFragment()?.statusCode).toBe(503);
    expect(include.getResolvedFragmentSource()).toBe('remote fallback-src');
    expect(include.getResolveTimeMillis()).toBeGreaterThan(0);
  });

  it('should handle primary include with errored src and successfully resolved fallback-src', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply.status(500).send('fragment from src');
    });
    server.get('/fallback-src', function (request, reply) {
      reply.status(206).send('fragment from fallback-src');
    });
    await server.listen();

    // when
    const include = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['fallback-src', serverAddress('/fallback-src')],
        ['primary', '']
      ])
    ).resolve(config, fragmentCache, new Headers());

    // then
    expect(include.isResolved()).toBe(true);
    expect(include.getResolvedFragment()?.content).toBe('fragment from fallback-src');
    expect(include.getResolvedFragment()?.statusCode).toBe(206);
    expect(include.getResolvedFragmentSource()).toBe('remote fallback-src');
    expect(include.getResolveTimeMillis()).toBeGreaterThan(0);
  });

  it('should handle primary include with errored src and errored fallback-src', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply.status(503).send('fragment from src');
    });
    server.get('/fallback-src', function (request, reply) {
      reply.status(500).send('fragment from fallback_src');
    });
    await server.listen();

    // when
    const include = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['fallback-src', serverAddress('/fallback-src')],
        ['primary', '']
      ])
    ).resolve(config, fragmentCache, new Headers());

    // then
    expect(include.isResolved()).toBe(true);
    expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    expect(include.getResolvedFragment()?.statusCode).toBe(503);
    expect(include.getResolvedFragmentSource()).toBe('remote src');
    expect(include.getResolveTimeMillis()).toBeGreaterThan(0);
  });

  it('should reset errored fragment of primary include for consecutive resolving', async () => {
    // given
    server = Fastify();
    let reqCounter = 0;
    server.get('/src', function (request, reply) {
      if (reqCounter++ === 0) {
        reply.status(503).send('fragment from src');
      } else {
        reply.status(504).send('fragment from src 2nd call');
      }
    });
    server.get('/fallback-src', function (request, reply) {
      reply.status(500).send('fragment from fallback-src');
    });
    await server.listen();
    const include = new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['fallback-src', serverAddress('/fallback-src')],
        ['primary', '']
      ])
    );

    // when
    await include.resolve(config, fragmentCache, new Headers());

    // then
    expect(include.isResolved()).toBe(true);
    expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    expect(include.getResolvedFragment()?.statusCode).toBe(503);
    expect(include.getResolvedFragmentSource()).toBe('remote src');
    expect(include.getResolveTimeMillis()).toBeGreaterThan(0);

    // when
    await include.resolve(config, fragmentCache, new Headers());

    // then
    expect(include.isResolved()).toBe(true);
    expect(include.getResolvedFragment()?.content).toBe('fragment from src 2nd call');
    expect(include.getResolvedFragment()?.statusCode).toBe(504);
    expect(include.getResolvedFragmentSource()).toBe('remote src');
  });

  it('should ignore fallback content and set fragment status code and body of errored src if primary', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply.status(503).send('fragment from src');
    });
    await server.listen();

    // when
    const include = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['primary', '']
      ]),
      'fallback content'
    ).resolve(config, fragmentCache, new Headers());

    // then
    expect(include.isResolved()).toBe(true);
    expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    expect(include.getResolvedFragment()?.statusCode).toBe(503);
    expect(include.getResolvedFragmentSource()).toBe('remote src');
    expect(include.getResolveTimeMillis()).toBeGreaterThan(0);
  });

  it('should not follow redirects when resolving URLs', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply.status(302).header('Location', serverAddress('/src-after-redirect')).send();
    });
    server.get('/src-after-redirect', function (request, reply) {
      reply.status(200).send('fragment from src after redirect');
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/src')]]), 'fallback content').resolve(
      config,
      fragmentCache,
      new Headers()
    );

    // then
    expect(include.isResolved()).toBe(true);
    expect(include.getResolvedFragment()?.content).toBe('fallback content');
    expect(include.getResolvedFragment()?.statusCode).toBe(200);
    expect(include.getResolvedFragmentSource()).toBe('fallback content');
    expect(include.getResolveTimeMillis()).toBeGreaterThan(0);
  });

  it.each([
    [new Date(Date.now() + 5000), 'fragment from cache', 'cached src'],
    [new Date(Date.now() - 5000), 'fragment from src', 'remote src']
  ])(
    'should use cached fragment if not expired',
    async (expirationTime: Date, expectedFragmentContent: string, expectedFragmentSource: string) => {
      // given
      server = Fastify();
      server.get('/src', function (request, reply) {
        reply.status(200).send('fragment from src');
      });
      await server.listen();

      // when
      fragmentCache.set(serverAddress('/src'), new Fragment(200, 'fragment from cache', undefined, expirationTime));
      await sleep(2);
      const include = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
        config,
        fragmentCache,
        new Headers()
      );

      // then
      expect(include.isResolved()).toBe(true);
      expect(include.getResolvedFragment()?.content).toBe(expectedFragmentContent);
      expect(include.getResolvedFragment()?.statusCode).toBe(200);
      expect(include.getResolvedFragmentSource()).toBe(expectedFragmentSource);
    }
  );

  it.each([
    [100, 'fragment', false, ':(', 200],
    [200, 'fragment', true, 'fragment', 200],
    [202, 'fragment', false, ':(', 200],
    [203, 'fragment', true, 'fragment', 203],
    [204, '', true, '', 204],
    [205, 'fragment', false, ':(', 200],
    [206, 'fragment', true, 'fragment', 206],
    [300, 'fragment', true, ':(', 200],
    [302, 'fragment', false, ':(', 200],
    [400, 'fragment', false, ':(', 200],
    [404, 'fragment', true, ':(', 200],
    [405, 'fragment', true, ':(', 200],
    [410, 'fragment', true, ':(', 200],
    [414, 'fragment', true, ':(', 200],
    [500, 'fragment', false, ':(', 200],
    [501, 'fragment', true, ':(', 200],
    [502, 'fragment', false, ':(', 200],
    [503, 'fragment', false, ':(', 200],
    [504, 'fragment', false, ':(', 200],
    [505, 'fragment', false, ':(', 200],
    [506, 'fragment', false, ':(', 200],
    [507, 'fragment', false, ':(', 200],
    [508, 'fragment', false, ':(', 200],
    [509, 'fragment', false, ':(', 200],
    [510, 'fragment', false, ':(', 200],
    [511, 'fragment', false, ':(', 200]
  ])(
    'should cache fragment if status code is defined as cacheable in RFC 7231 - Status %i',
    async (
      responseStatus: number,
      srcFragment: string,
      expectedFragmentCached: boolean,
      expectedFragment: string,
      expectedFragmentStatusCode: number
    ) => {
      // given
      server = Fastify();
      server.get('/src', function (request, reply) {
        reply.status(responseStatus).header('Cache-Control', 'max-age=7200').send(srcFragment);
      });
      await server.listen();

      // when
      const include = await new Include('', new Map([['src', serverAddress('/src')]]), ':(').resolve(
        config,
        fragmentCache,
        new Headers()
      );

      // then
      expect(include.isResolved()).toBe(true);
      expect(include.getResolvedFragment()?.content).toBe(expectedFragment);
      expect(include.getResolvedFragment()?.statusCode).toBe(expectedFragmentStatusCode);
      expect(include.getResolvedFragmentSource()).toBe(expectedFragment == ':(' ? 'fallback content' : 'remote src');

      if (expectedFragmentCached) {
        expect(fragmentCache.get(serverAddress('/src'))).toBeDefined();
      } else {
        expect(fragmentCache.get(serverAddress('/src'))).toBeUndefined();
      }
    }
  );

  it('should cache fragment for s-maxage seconds if directive is present', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply
        .status(200)
        .header('Cache-Control', 'max-age=3600, s-maxage=604800 , public')
        .header('Expires', 'Wed, 21 Oct 2015 07:28:00 GMT')
        .send('fragment from src');
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers()
    );
    const cachedFragment = fragmentCache.get(serverAddress('/src')) as Fragment;

    // then
    expect(include.isResolved()).toBe(true);
    expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    expect(cachedFragment).toBeDefined();
    expect(cachedFragment.expirationTime < new Date(Date.now() + 604800000 + 1000)).toBe(true);
    expect(cachedFragment.expirationTime > new Date(Date.now() + 604800000 - 1000)).toBe(true);
  });

  it('should cache fragment for max-age seconds if directive is present', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply
        .status(200)
        .header('Cache-Control', 'max-age=3600')
        .header('Expires', 'Wed, 21 Oct 2015 07:28:00 GMT')
        .send('fragment from src');
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers()
    );
    const cachedFragment = fragmentCache.get(serverAddress('/src')) as Fragment;

    // then
    expect(include.isResolved()).toBe(true);
    expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    expect(cachedFragment).toBeDefined();
    expect(cachedFragment.expirationTime < new Date(Date.now() + 3600000 + 1000)).toBe(true);
    expect(cachedFragment.expirationTime > new Date(Date.now() + 3600000 - 1000)).toBe(true);
  });

  it('should treat http header names as case insensitive', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply.status(200).header('cache-control', 'max-age=3600').send('fragment from src');
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers()
    );
    const cachedFragment = fragmentCache.get(serverAddress('/src')) as Fragment;

    // then
    expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    expect(cachedFragment).toBeDefined();
    expect(cachedFragment.expirationTime < new Date(Date.now() + 3600000 + 1000)).toBe(true);
    expect(cachedFragment.expirationTime > new Date(Date.now() + 3600000 - 1000)).toBe(true);
  });

  it('should cache fragment for max-age seconds minus Age seconds if directive is present and Age header is set', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply
        .status(200)
        .header('Cache-Control', 'max-age=3600')
        .header('Age', '600')
        .header('Expires', 'Wed, 21 Oct 2015 07:28:00 GMT')
        .send('fragment from src');
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers()
    );
    const cachedFragment = fragmentCache.get(serverAddress('/src')) as Fragment;

    // then
    expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    expect(cachedFragment).toBeDefined();
    expect(cachedFragment.expirationTime < new Date(Date.now() + 3000000 + 1000)).toBe(true);
    expect(cachedFragment.expirationTime > new Date(Date.now() + 3000000 - 1000)).toBe(true);
  });

  it('should use absolute value of Age header for cache expiration calculation', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply
        .status(200)
        .header('Cache-Control', 'max-age=3600')
        .header('Age', '-100')
        .header('Expires', 'Wed, 21 Oct 2015 07:28:00 GMT')
        .send('fragment from src');
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers()
    );
    const cachedFragment = fragmentCache.get(serverAddress('/src')) as Fragment;

    // then
    expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    expect(cachedFragment).toBeDefined();
    expect(cachedFragment.expirationTime < new Date(Date.now() + 3500000 + 1000)).toBe(true);
    expect(cachedFragment.expirationTime > new Date(Date.now() + 3500000 - 1000)).toBe(true);
  });

  it('should cache fragment based on Expires header and current time if Cache-Control header and Date header are not present', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply
        .status(200)
        .header('Cache-Control', 'public')
        .header('Expires', 'Wed, 12 Oct 2050 07:28:00 GMT')
        .send('fragment from src');
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers()
    );
    const cachedFragment = fragmentCache.get(serverAddress('/src')) as Fragment;

    // then
    expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    expect(cachedFragment).toBeDefined();
    expect(cachedFragment.expirationTime.toUTCString()).toBe('Wed, 12 Oct 2050 07:28:00 GMT');
  });

  it('should handle Expires header with value 0', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply.status(200).header('Expires', '0').send('fragment from src');
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers()
    );

    // then
    expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    expect(fragmentCache.get(serverAddress('/src'))).toBeUndefined();
  });

  it('should cache fragment based on Expires and Date header if Cache-Control header is not present', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply
        .status(200)
        .header('Date', 'Wed, 05 Oct 2050 07:28:00 GMT')
        .header('Expires', 'Wed, 12 Oct 2050 07:28:00 GMT')
        .send('fragment from src');
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers()
    );
    const cachedFragment = fragmentCache.get(serverAddress('/src')) as Fragment;

    // then
    expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    expect(cachedFragment).toBeDefined();
    expect(cachedFragment.expirationTime < new Date(Date.now() + 7 * 24 * 60 * 60 * 1000 + 1000)).toBe(true);
    expect(cachedFragment.expirationTime > new Date(Date.now() + 7 * 24 * 60 * 60 * 1000 - 1000)).toBe(true);
  });

  it('should not cache fragment if Cache-Control header is set but without max-age directives', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply.status(200).header('Cache-Control', 'no-cache,no-store,must-revalidate').send('fragment from src');
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers()
    );

    // then
    expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    expect(fragmentCache.get(serverAddress('/src'))).toBeUndefined();
  });

  it.each([
    ['Cache-Control', 's-maxage=not-numeric', 'X-Dummy', 'dummy'],
    ['Cache-Control', 'max-age=not-numeric', 'X-Dummy', 'dummy'],
    ['Cache-Control', 'max-age=3600', 'Age', 'not-numeric'],
    ['Expires', 'not-numeric', 'X-Dummy', 'dummy'],
    ['Expires', 'Wed, 12 Oct 2050 07:28:00 GMT', 'Date', 'not-a-date']
  ])(
    'should not crash when cache headers contain invalid values',
    async (header1Name: string, header1Value: string, header2Name: string, header2Value: string) => {
      // given
      server = Fastify();
      server.get('/src', function (request, reply) {
        reply.status(200).header(header1Name, header1Value).header(header2Name, header2Value).send('fragment from src');
      });
      await server.listen();

      // when
      const include = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
        config,
        fragmentCache,
        new Headers()
      );

      // then
      expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    }
  );

  it('should not cache fragment if no expiration time is indicated via response header', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply.status(200).send('fragment from src');
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers()
    );

    // then
    expect(include.getResolvedFragment()?.content).toBe('fragment from src');
    expect(fragmentCache.get(serverAddress('/src'))).toBeUndefined();
  });

  it('should apply request timeout', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      sleep(2000).then(() => reply.status(200).send('fragment from src'));
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/src')]]), 'fallback content').resolve(
      config,
      fragmentCache,
      new Headers()
    );

    // then
    expect(include.getResolvedFragment()?.content).toBe('fallback content');
  });

  it.each([
    ['src', new Map(), ''],
    ['src', new Map([['src-timeout', '2000']]), 'fragment'],
    ['src', new Map([['fallback-src-timeout', '2000']]), ''],
    ['fallback-src', new Map(), ''],
    ['fallback-src', new Map([['fallback-src-timeout', '2000']]), 'fragment'],
    ['fallback-src', new Map([['src-timeout', '2000']]), '']
  ])(
    'should favor include tag specific request timeout over global one - %s, %o',
    async (srcAttributeName: string, timeoutAttribute: Map<string, string>, expectedFragmentContent: string) => {
      // given
      server = Fastify();
      server.get('/', function (request, reply) {
        sleep(1200).then(() => reply.status(200).send('fragment'));
      });
      await server.listen();

      // when
      const rawAttributes = new Map([[srcAttributeName, serverAddress('/')]]);
      timeoutAttribute.forEach((value, key) => rawAttributes.set(key, value));
      const include = await new Include('', rawAttributes).resolve(config, fragmentCache, new Headers());

      // then
      expect(include.getResolvedFragment()?.content).toBe(expectedFragmentContent);
    }
  );

  it('should forward headers defined via requestHeadersForward and requestHeadersForwardVary to fragment requests', async () => {
    // given
    server = Fastify();
    let lastRecordedRequestHeaders: IncomingHttpHeaders = {};
    server.get('/src', function (request, reply) {
      lastRecordedRequestHeaders = request.headers;
      reply.status(204).send();
    });
    await server.listen();

    // when
    await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['headers', 'x-heaDER-7,']
      ])
    ).resolve(
      new AbleronConfig({
        requestHeadersForward: ['X-Header-1', 'X-Header-2', 'x-hEADEr-3'],
        requestHeadersForwardVary: ['X-HEADER-4', 'x-header-5', 'x-hEADEr-6']
      }),
      fragmentCache,
      new Headers([
        ['x-header-1', 'header1'],
        ['X-HEADER-2', 'header2'],
        ['X-Header-3', 'header3'],
        ['x-header-4', 'header4'],
        ['X-Header-5', 'header5'],
        ['X-Header-6', 'header6'],
        ['X-Header-7', 'header7'],
        ['X-Header-8', 'header8']
      ])
    );

    // then
    expect(lastRecordedRequestHeaders['x-header-1']).toBe('header1');
    expect(lastRecordedRequestHeaders['x-header-2']).toBe('header2');
    expect(lastRecordedRequestHeaders['x-header-3']).toBe('header3');
    expect(lastRecordedRequestHeaders['x-header-4']).toBe('header4');
    expect(lastRecordedRequestHeaders['x-header-5']).toBe('header5');
    expect(lastRecordedRequestHeaders['x-header-6']).toBe('header6');
    expect(lastRecordedRequestHeaders['x-header-7']).toBe('header7');
    expect(lastRecordedRequestHeaders['x-header-8']).toBeUndefined();
  });

  it('should forward headers defined via ableron-include headers-attribute to fragment requests', async () => {
    // given
    server = Fastify();
    let lastRecordedRequestHeaders: IncomingHttpHeaders = {};
    server.get('/src', function (request, reply) {
      lastRecordedRequestHeaders = request.headers;
      reply.status(204).send();
    });
    await server.listen();

    // when
    await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['headers', 'X-Header1,X-Header2,x-hEADEr3']
      ])
    ).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-Header1', 'header1'],
        ['X-Header2', 'header2'],
        ['X-HEADER3', 'header3']
      ])
    );

    // then
    expect(lastRecordedRequestHeaders['x-header1']).toBe('header1');
    expect(lastRecordedRequestHeaders['x-header2']).toBe('header2');
    expect(lastRecordedRequestHeaders['x-header3']).toBe('header3');
  });

  it('should forward cookies defined via ableron-include cookies-attribute to fragment requests', async () => {
    // given
    server = Fastify();
    let lastRecordedRequestHeaders: IncomingHttpHeaders = {};
    server.get('/src', function (request, reply) {
      lastRecordedRequestHeaders = request.headers;
      reply.status(204).send();
    });
    await server.listen();

    // when
    await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['cookies', 'UID,selected_tab, cID ']
      ])
    ).resolve(
      config,
      fragmentCache,
      new Headers([['Cookie', 'foo=bar;  UID=user1 ; Uid=user%3B2; SELECTED_TAB=home; cID = 123']])
    );

    // then
    expect(lastRecordedRequestHeaders['cookie']).toBe('UID=user1; cID = 123');
  });

  it('should not forward non-allowed request headers to fragment requests', async () => {
    // given
    server = Fastify();
    let lastRecordedRequestHeaders: IncomingHttpHeaders = {};
    server.get('/src', function (request, reply) {
      lastRecordedRequestHeaders = request.headers;
      reply.status(204).send();
    });
    await server.listen();

    // when
    await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      new AbleronConfig({ requestHeadersForward: [] }),
      fragmentCache,
      new Headers([['X-Test', 'Foo']])
    );

    // then
    expect(lastRecordedRequestHeaders['x-test']).toBeUndefined();
  });

  it('should pass default User-Agent header to fragment requests', async () => {
    // given
    server = Fastify();
    let lastRecordedRequestHeaders: IncomingHttpHeaders = {};
    server.get('/src', function (request, reply) {
      lastRecordedRequestHeaders = request.headers;
      reply.status(204).send();
    });
    await server.listen();

    // when
    await new Include('', new Map([['src', serverAddress('/src')]])).resolve(config, fragmentCache, new Headers());

    // then
    expect(lastRecordedRequestHeaders['user-agent']).toBe('Ableron/2.0');
  });

  it('should forward provided User-Agent header to fragment requests if enabled via requestHeadersForward', async () => {
    // given
    server = Fastify();
    let lastRecordedRequestHeaders: IncomingHttpHeaders = {};
    server.get('/src', function (request, reply) {
      lastRecordedRequestHeaders = request.headers;
      reply.status(204).send();
    });
    await server.listen();

    // when
    await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      new AbleronConfig({ requestHeadersForward: ['User-Agent'] }),
      fragmentCache,
      new Headers([['user-AGENT', 'test']])
    );

    // then
    expect(lastRecordedRequestHeaders['user-agent']).toBe('test');
  });

  it('should forward allowed response headers of primary fragment to transclusion result', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply.status(200).header('X-Test', 'Test').send();
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      new AbleronConfig({ responseHeadersForward: ['X-Test'] }),
      fragmentCache,
      new Headers()
    );

    // then
    expect(include.getResolvedFragment()?.responseHeaders).toEqual(new Headers([['x-test', 'Test']]));
  });

  it('should not forward allowed response headers of non-primary fragment to transclusion result', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply.status(200).header('X-Test', 'Test').send();
    });
    await server.listen();

    // when
    const include = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers()
    );

    // then
    expect(include.getResolvedFragment()?.responseHeaders).toEqual(new Headers());
  });

  it('should treat fragment response headers allow list as case insensitive', async () => {
    // given
    server = Fastify();
    server.get('/src', function (request, reply) {
      reply.status(200).header('x-test', 'Test').send();
    });
    await server.listen();

    // when
    const include = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['primary', '']
      ])
    ).resolve(new AbleronConfig({ responseHeadersForward: ['X-TeSt'] }), fragmentCache, new Headers());

    // then
    expect(include.getResolvedFragment()?.responseHeaders.get('x-test')).toBe('Test');
  });

  it('should not collapse requests', async () => {
    // given
    server = Fastify();
    let reqCounter = 0;
    server.get('/src', function (request, reply) {
      sleep(200).then(() =>
        reply
          .status(200)
          .header('Cache-Control', 'max-age=30')
          .send('request ' + ++reqCounter)
      );
    });
    await server.listen();
    const include = new Include('', new Map([['src', serverAddress('/src')]]));

    // when
    const fragment1 = include.resolve(config, fragmentCache, new Headers());
    const fragment2 = include.resolve(config, fragmentCache, new Headers());
    const fragment3 = include.resolve(config, fragmentCache, new Headers());
    const fragment4 = new Include('', new Map([['src', serverAddress('/404')]]), '404 not found').resolve(
      config,
      fragmentCache,
      new Headers()
    );

    // and
    await Promise.all([fragment1, fragment2, fragment3, fragment4]);

    // then
    expect(reqCounter).toBe(3);
  });

  it('should consider requestHeadersForwardVary', async () => {
    // given
    server = Fastify();
    let reqCounter = 0;
    server.get('/src', function (request, reply) {
      reply
        .status(200)
        .header('Cache-Control', 'max-age=30')
        .send('request X-AB-Test=' + request.headers['x-ab-test'] + ' | ' + ++reqCounter);
    });
    await server.listen();
    const config = new AbleronConfig({
      requestHeadersForward: ['x-ab-TEST', 'x-ab-TEST-1', 'x-ab-TEST-2'],
      requestHeadersForwardVary: ['x-AB-test', 'x-AB-test-1', 'x-AB-test-2']
    });

    // when
    const resolvedInclude1 = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-AB-TEST', 'A'],
        ['X-AB-TEST-2', 'A'],
        ['X-AB-TEST-1', 'A']
      ])
    );
    const resolvedInclude2 = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-AB-TEST', 'A'],
        ['X-AB-TEST-1', 'A'],
        ['X-AB-TEST-2', 'A']
      ])
    );
    const resolvedInclude3 = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers([['X-AB-TEST', 'B']])
    );
    const resolvedInclude4 = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-AB-TEST', 'B'],
        ['X-Foo', 'Bar']
      ])
    );
    const resolvedInclude5 = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers()
    );
    const resolvedInclude6 = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-AB-TEST-2', 'A'],
        ['X-AB-TEST-1', 'A'],
        ['X-AB-TEST', 'A']
      ])
    );
    const resolvedInclude7 = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-AB-TEST-2', ''],
        ['X-AB-TEST-1', 'A'],
        ['X-AB-TEST', 'A']
      ])
    );
    const resolvedInclude8 = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-AB-TEST-1', 'A'],
        ['X-AB-TEST', 'A']
      ])
    );

    // then
    expect(resolvedInclude1.getResolvedFragment()?.content).toBe('request X-AB-Test=A | 1');
    expect(resolvedInclude2.getResolvedFragment()?.content).toBe('request X-AB-Test=A | 1');
    expect(resolvedInclude3.getResolvedFragment()?.content).toBe('request X-AB-Test=B | 2');
    expect(resolvedInclude4.getResolvedFragment()?.content).toBe('request X-AB-Test=B | 2');
    expect(resolvedInclude5.getResolvedFragment()?.content).toBe('request X-AB-Test=undefined | 3');
    expect(resolvedInclude6.getResolvedFragment()?.content).toBe('request X-AB-Test=A | 1');
    expect(resolvedInclude7.getResolvedFragment()?.content).toBe('request X-AB-Test=A | 4');
    expect(resolvedInclude8.getResolvedFragment()?.content).toBe('request X-AB-Test=A | 4');
  });

  it('should use consistent order of requestHeadersForwardVary for cache key generation', async () => {
    // given
    server = Fastify();
    let reqCounter = 0;
    server.get('/src', function (request, reply) {
      reply
        .status(200)
        .header('Cache-Control', 'max-age=30')
        .send('request ' + ++reqCounter);
    });
    await server.listen();
    const config = new AbleronConfig({
      requestHeadersForward: ['X-Test-A', 'X-Test-B', 'X-Test-C'],
      requestHeadersForwardVary: ['X-Test-A', 'X-Test-B', 'X-Test-C']
    });

    // when
    const resolvedInclude1 = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-TEST-A', 'A'],
        ['X-Test-B', 'B'],
        ['X-Test-C', 'C']
      ])
    );
    const resolvedInclude2 = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-TEST-B', 'B'],
        ['X-Test-A', 'A'],
        ['X-Test-C', 'C']
      ])
    );
    const resolvedInclude3 = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-TEST-C', 'C'],
        ['X-Test-B', 'B'],
        ['X-Test-A', 'A']
      ])
    );
    const resolvedInclude4 = await new Include('', new Map([['src', serverAddress('/src')]])).resolve(
      config,
      fragmentCache,
      new Headers([
        ['x-test-c', 'B'],
        ['x-test-b', 'B'],
        ['x-test-a', 'A']
      ])
    );

    // then
    expect(resolvedInclude1.getResolvedFragment()?.content).toBe('request 1');
    expect(resolvedInclude2.getResolvedFragment()?.content).toBe('request 1');
    expect(resolvedInclude3.getResolvedFragment()?.content).toBe('request 1');
    expect(resolvedInclude4.getResolvedFragment()?.content).toBe('request 2');
  });

  it('should consider request headers defined in headers attribute for cache key generation', async () => {
    // given
    server = Fastify();
    let reqCounter = 0;
    server.get('/src', function (request, reply) {
      reply
        .status(200)
        .header('Cache-Control', 'max-age=30')
        .send('request ' + ++reqCounter);
    });
    await server.listen();

    // when
    const resolvedInclude1 = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['headers', 'X-Test-A,X-Test-B,X-Test-C']
      ])
    ).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-TEST-B', 'B'],
        ['X-Test-C', 'C'],
        ['X-Test-A', 'A']
      ])
    );
    const resolvedInclude2 = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['headers', 'X-Test-A,X-Test-B,X-Test-C']
      ])
    ).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-TEST-B', 'B'],
        ['X-TEST-A', 'A'],
        ['X-Test-C', 'C']
      ])
    );
    const resolvedInclude3 = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['headers', 'x-test-b']
      ])
    ).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-TEST-C', 'C'],
        ['X-test-B', 'B'],
        ['X-Test-A', 'A']
      ])
    );
    const resolvedInclude4 = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['headers', 'X-Test-B,X-Test-C,X-Test-A']
      ])
    ).resolve(
      config,
      fragmentCache,
      new Headers([
        ['x-test-c', 'B'],
        ['x-test-b', 'B'],
        ['x-test-a', 'A']
      ])
    );
    const resolvedInclude5 = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['headers', 'X-TEST-C, X-Test-A,X-Test-B']
      ])
    ).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-TEST-B', 'B'],
        ['X-Test-C', 'C'],
        ['X-Test-A', 'A'],
        ['X-Test-D', 'D']
      ])
    );
    const resolvedInclude6 = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['headers', 'x-test-b'],
        ['cookies', 'x-test-b']
      ])
    ).resolve(config, fragmentCache, new Headers([['Cookie', 'x-test-b=B']]));

    // then
    expect(resolvedInclude1.getResolvedFragment()?.content).toBe('request 1');
    expect(resolvedInclude2.getResolvedFragment()?.content).toBe('request 1');
    expect(resolvedInclude3.getResolvedFragment()?.content).toBe('request 2');
    expect(resolvedInclude4.getResolvedFragment()?.content).toBe('request 3');
    expect(resolvedInclude5.getResolvedFragment()?.content).toBe('request 1');
    expect(resolvedInclude6.getResolvedFragment()?.content).toBe('request 4');
  });

  it('should consider cookies defined in cookies attribute for cache key generation', async () => {
    // given
    server = Fastify();
    let reqCounter = 0;
    server.get('/src', function (request, reply) {
      reply
        .status(200)
        .header('Cache-Control', 'max-age=30')
        .send('request ' + ++reqCounter);
    });
    await server.listen();

    // when
    const resolvedInclude1 = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['headers', 'X-Test-A'],
        ['cookies', 'UID,ab_test']
      ])
    ).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-Test-A', 'A'],
        ['Cookie', 'foo=bar;UID=1;ab_test=x']
      ])
    );
    const resolvedInclude2 = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['headers', 'X-Test-A'],
        ['cookies', 'UID,ab_test']
      ])
    ).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-Test-A', 'A'],
        ['Cookie', 'foo=bar;ab_test=x']
      ])
    );
    const resolvedInclude3 = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['headers', 'X-Test-A'],
        ['cookies', 'UID,ab_test']
      ])
    ).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-Test-A', 'A'],
        ['Cookie', 'foo=bar;UID=2;ab_test=x']
      ])
    );
    const resolvedInclude4 = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['headers', 'X-Test-A'],
        ['cookies', 'UID,ab_test']
      ])
    ).resolve(
      config,
      fragmentCache,
      new Headers([
        ['x-test-a', 'A'],
        ['Cookie', 'ab_test=x; UID=1']
      ])
    );
    const resolvedInclude5 = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['headers', 'X-Test-A'],
        ['cookies', 'UID,ab_test']
      ])
    ).resolve(config, fragmentCache, new Headers([['Cookie', 'ab_test=x']]));
    const resolvedInclude6 = await new Include(
      '',
      new Map([
        ['src', serverAddress('/src')],
        ['headers', 'X-Test-A'],
        ['cookies', 'UID,ab_test']
      ])
    ).resolve(
      config,
      fragmentCache,
      new Headers([
        ['X-Test-B', 'B'],
        ['Cookie', 'ab_test=x;a=a']
      ])
    );

    // then
    expect(resolvedInclude1.getResolvedFragment()?.content).toBe('request 1');
    expect(resolvedInclude2.getResolvedFragment()?.content).toBe('request 2');
    expect(resolvedInclude3.getResolvedFragment()?.content).toBe('request 3');
    expect(resolvedInclude4.getResolvedFragment()?.content).toBe('request 1');
    expect(resolvedInclude5.getResolvedFragment()?.content).toBe('request 4');
    expect(resolvedInclude6.getResolvedFragment()?.content).toBe('request 4');
  });

  it('should configure auto refresh for cached Fragments', async () => {
    // given
    server = Fastify();
    server.get('/', function (request, reply) {
      reply.status(200).header('Cache-Control', 'max-age=1').send('fragment');
    });
    await server.listen();
    const fragmentCache = new FragmentCache(
      new AbleronConfig({ cacheMaxItems: 10, cacheAutoRefreshEnabled: true }),
      new NoOpLogger()
    );

    // when
    for (let i = 0; i < 4; i++) {
      await new Include('', new Map([['src', serverAddress('/')]]))
        .resolve(new AbleronConfig({ cacheAutoRefreshEnabled: true }), fragmentCache, new Headers())
        .then(() => sleep(1000));
    }

    // then
    expect(fragmentCache.getStats().getHitCount()).toBe(3);
    expect(fragmentCache.getStats().getMissCount()).toBe(1);
    expect(fragmentCache.getStats().getRefreshSuccessCount()).toBe(4);
    expect(fragmentCache.getStats().getRefreshFailureCount()).toBe(0);
  });
});
