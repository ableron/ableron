import { describe, expect, it } from 'vitest';
import ableron from '../src';
import Fastify, { FastifyRequest } from 'fastify';
import request from 'supertest';
import { LoggerInterface } from '@ableron/ableron';

describe('Ableron Fastify Plugin', () => {
  it('should apply transclusion', async () => {
    // given
    const app = appWithAbleronPlugin();
    app.get('/', (request, reply) => {
      reply
        .type('text/html; charset=utf-8')
        .send(`<ableron-include src="${getFragmentBaseUrl(request)}/fragment">fallback</ableron-include>`);
    });
    app.get('/fragment', (request, reply) => {
      reply.type('text/html; charset=utf-8').send('fragment');
    });
    await app.ready();

    // when
    const response = await request(app.server).get('/');

    // then
    expect(response.headers['content-type']).toBe('text/html; charset=utf-8');
    expect(response.headers['content-length']).toBe(String('fragment'.length));
    expect(response.status).toBe(200);
    expect(response.text).toBe('fragment');
  });

  it('should check content-type text/html case insensitive', async () => {
    // given
    const app = appWithAbleronPlugin();
    app.get('/', (request, reply) => {
      reply
        .type('TEXT/HTML')
        .send(`<ableron-include src="${getFragmentBaseUrl(request)}/fragment">fallback</ableron-include>`);
    });
    app.get('/fragment', (request, reply) => {
      reply.type('TEXT/HTML; charset=utf-8').send('fragment');
    });
    await app.ready();

    // when
    const response = await request(app.server).get('/');

    // then
    expect(response.headers['content-type']).toBe('TEXT/HTML');
    expect(response.headers['content-length']).toBe(String('fragment'.length));
    expect(response.status).toBe(200);
    expect(response.text).toBe('fragment');
  });

  it('should skip transclusion when content-type is not text/html', async () => {
    // given
    const originalBody = `<ableron-include id="test">fallback</ableron-include>`;
    const app = appWithAbleronPlugin();
    app.get('/', (request, reply) => {
      reply.type('text/plain').send(originalBody);
    });
    await app.ready();

    // when
    const response = await request(app.server).get('/');

    // then
    expect(response.headers['content-type']).toBe('text/plain');
    expect(response.headers['content-length']).toBe(String(originalBody.length));
    expect(response.status).toBe(200);
    expect(response.text).toBe(originalBody);
  });

  it('should skip transclusion when status code is 3xx', async () => {
    // given
    const originalBody = `<ableron-include id="test">fallback</ableron-include>`;
    const app = appWithAbleronPlugin();
    app.get('/', (request, reply) => {
      reply.code(301).send(originalBody);
    });
    await app.ready();

    // when
    const response = await request(app.server).get('/');

    // then
    expect(response.headers['content-type']).toBe('text/plain; charset=utf-8');
    expect(response.headers['content-length']).toBe(String(originalBody.length));
    expect(response.status).toBe(301);
    expect(response.text).toBe(originalBody);
  });

  it('should handle multibyte characters', async () => {
    // given
    const app = appWithAbleronPlugin();
    app.get('/', (request, reply) => {
      reply.type('text/html; charset=utf-8').send(`<ableron-include id="test">☺</ableron-include>`);
    });
    await app.ready();

    // when
    const response = await request(app.server).get('/');

    // then
    expect(response.headers['content-type']).toBe('text/html; charset=utf-8');
    expect(response.headers['content-length']).toBe('3');
    expect(response.status).toBe(200);
    expect(response.text).toBe('☺');
  });

  it('should pass request headers to resolveIncludes()', async () => {
    // given
    const app = appWithAbleronPlugin();
    app.get('/', (request, reply) => {
      reply
        .type('text/html; charset=utf-8')
        .send(
          `<ableron-include src="${getFragmentBaseUrl(request)}/fragment" headers="X-Test">fallback</ableron-include>`
        );
    });
    app.get('/fragment', (request, reply) => {
      reply.type('text/html; charset=utf-8').send(request.headers['x-test']);
    });
    await app.ready();

    // when
    const response = await request(app.server).get('/').set('X-TEST', 'test');

    // then
    expect(response.text).toBe('test');
  });

  it.each([
    [true, '[Ableron] Skipping UI composition (response status: 301, content-type: text/plain; charset=utf-8)'],
    [false, null]
  ])(
    'should not hook into response when Ableron is disabled',
    async (ableronEnabled: boolean, expectedLogMessage?: string) => {
      // given
      let ableronSkippingUiCompositionLogMessage = null;
      const catchDebugMessageLogger = {
        debug: (msg) => {
          ableronSkippingUiCompositionLogMessage = msg;
        }
      } as LoggerInterface;
      const app = Fastify({ logger: true });
      app.register(ableron, {
        ableron: {
          enabled: ableronEnabled,
          logger: catchDebugMessageLogger
        }
      });
      app.get('/', (request, reply) => {
        reply.code(301).send('<ableron-include id="test">fallback</ableron-include>');
      });
      await app.ready();

      // when
      await request(app.server).get('/');

      // then
      expect(ableronSkippingUiCompositionLogMessage).toBe(expectedLogMessage);
    }
  );

  function appWithAbleronPlugin() {
    const app = Fastify({ logger: true });
    app.register(ableron, {
      ableron: {
        logger: app.log
      }
    });
    return app;
  }

  function getFragmentBaseUrl(request: FastifyRequest): string {
    return `${request.protocol}://${request.hostname}:${request.port}`;
  }
});
