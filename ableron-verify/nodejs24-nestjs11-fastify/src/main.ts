import { NestFactory } from '@nestjs/core';
import { FastifyAdapter, NestFastifyApplication } from '@nestjs/platform-fastify';
import { AppModule } from './app.module';
import ableron from '@ableron/fastify';

async function bootstrap() {
  const app = await NestFactory.create<NestFastifyApplication>(
    AppModule,
    new FastifyAdapter(),
    {
      rawBody: true,
      logger: console
    }
  );
  app.useBodyParser('text/plain', { bodyLimit: 5 * 1024 * 1024 });
  await app.register(ableron, {
    ableron: {
      requestHeadersForwardVary: ['Accept-Language'],
      logger: console
    }
  });
  await app.listen(8080, '0.0.0.0');
}
bootstrap();
