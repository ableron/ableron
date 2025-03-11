import fastifyPlugin from 'fastify-plugin';
import { Ableron } from '@ableron/ableron';

async function ableron(app, opts) {
  const shouldPerformUiComposition = (reply) =>
    !(reply.statusCode >= 100 && reply.statusCode <= 199) &&
    !(reply.statusCode >= 300 && reply.statusCode <= 399) &&
    (!reply.getHeader('content-type') || /^text\/html/i.test(String(reply.getHeader('content-type'))));
  const ableron = new Ableron(opts?.ableron || {}, opts?.ableron?.logger);

  ableron.getConfig().enabled &&
    app.addHook('onSend', async (request, reply, payload) => {
      if (!shouldPerformUiComposition(reply)) {
        ableron
          .getLogger()
          .debug(
            `[Ableron] Skipping UI composition (response status: ${reply.statusCode}, content-type: ${reply.getHeader(
              'content-type'
            )})`
          );
        return payload;
      }

      try {
        return ableron
          .resolveIncludes(payload, request.headers)
          .then((transclusionResult) => {
            transclusionResult
              .getResponseHeadersToForward()
              .forEach((headerValue, headerName) => reply.header(headerName, headerValue));
            reply.header(
              'Cache-Control',
              transclusionResult.calculateCacheControlHeaderValueByResponseHeaders(reply.getHeaders())
            );

            if (transclusionResult.getStatusCodeOverride()) {
              reply.code(transclusionResult.getStatusCodeOverride());
            }

            return transclusionResult.getContent();
          })
          .catch((e) => {
            ableron.getLogger().error(`[Ableron] Unable to perform UI composition: ${e.stack || e.message}`);
            return payload;
          });
      } catch (e: any) {
        ableron.getLogger().error(`[Ableron] Unable to perform UI composition: ${e.stack || e.message}`);
        return payload;
      }
    });
}

export default fastifyPlugin(ableron);
